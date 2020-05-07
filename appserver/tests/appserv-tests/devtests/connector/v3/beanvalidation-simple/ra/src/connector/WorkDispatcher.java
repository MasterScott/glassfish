/*
 * Copyright (c) 2002, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package connector;

import java.lang.reflect.Method;
import java.util.Iterator;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkManager;
import jakarta.resource.spi.work.ExecutionContext;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

/**
 *
 * @author	Qingqing Ouyang
 */
public class WorkDispatcher implements Work {

    protected boolean stop = false;
    protected String id;
    protected MessageEndpointFactory factory;
    protected BootstrapContext ctx;
    protected ActivationSpec spec;
    protected WorkManager wm;
    protected XATerminator xa;
    
    public WorkDispatcher(
            String id,
            BootstrapContext ctx,
            MessageEndpointFactory factory, 
            ActivationSpec spec) {
        this.id      = id;
        this.ctx     = ctx;
        this.factory = factory;
        this.spec    = spec;
        this.wm      = ctx.getWorkManager();
        this.xa      = ctx.getXATerminator();
    }

    public void run() {

        debug("ENTER...");
        
        try {
            synchronized (Controls.readyLock) {
                debug("WAIT...");
                Controls.readyLock.wait();

                if (stop) {
                    return;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        debug("Running...");

            try {

                Method onMessage = getOnMessageMethod();
                System.out.println("isDeliveryTransacted = " +
                        factory.isDeliveryTransacted(onMessage));

                if (!factory.isDeliveryTransacted(onMessage)) {
                    //MessageEndpoint ep = factory.createEndpoint(null);
                    //DeliveryWork d = new DeliveryWork("NO_TX", ep);
                    //wm.doWork(d, 0, null, null);
                } else {

                    //MessageEndpoint ep = factory.createEndpoint(null);
                    MessageEndpoint ep = factory.createEndpoint(new FakeXAResource());
                    int numOfMessages = 5;

                    //importing transaction

                    //write/commit
                    ExecutionContext ec = startTx();
                    debug("Start TX - " + ec.getXid());

                    DeliveryWork w =
                        new DeliveryWork(ep, numOfMessages, "WRITE");
                    wm.doWork(w, 0, ec, null);
                    xa.commit(ec.getXid(), true);

                    debug("DONE WRITE TO DB");
                    Controls.expectedResults = numOfMessages;
                    notifyAndWait();

                    //delete all.
                    ec = startTx();
                    debug("Start TX - " + ec.getXid());
                    
                    w = new DeliveryWork(ep, 1, "DELETE_ALL");
                    wm.doWork(w, 0, ec, null);
                    xa.commit(ec.getXid(), true);
                    
                    debug("DONE DELETE ALL FROM DB");
                    Controls.expectedResults = 0;
                    notifyAndWait();
                    
                    done();
                }

            } catch (UnavailableException ex) {
                //ex.printStackTrace();
                System.out.println("WorkDispatcher["+id+"] Endpoint Unavailable");
                try {
                    Thread.currentThread().sleep(3*1000); //3 seconds
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (XAException ex) {
                ex.printStackTrace();
                System.out.println("ERROR CODE = " + ex.errorCode);
                done();
            }  catch (Exception ex) {
                System.out.println("Exception while executing work");
                ex.printStackTrace();
                done();
            }finally{
                done();
            }

        debug("LEAVE...");
    }

    public void release() {}

    public void stop() {
        this.stop = true;
    }

    public String toString() {
       return id;
    }

    public Method getOnMessageMethod() {
        
        Method onMessageMethod = null;
        try {
            Class msgListenerClass = connector.MyMessageListener.class;
            Class[] paramTypes = { java.lang.String.class };
            onMessageMethod = 
                msgListenerClass.getMethod("onMessage", paramTypes);
            
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        return onMessageMethod;
    }

    protected ExecutionContext startTx() {
        ExecutionContext ec = new ExecutionContext();
        try {
            Xid xid = new XID();
            ec.setXid(xid);
            ec.setTransactionTimeout(5*1000); //5 seconds
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ec;
    }

    protected void notifyAndWait() {
        try {
            //Sleep for 5 seconds
            //Thread.currentThread().sleep(5*1000);
            
            synchronized(Controls.readyLock) {
                //Notify the client to check the results
                Controls.readyLock.notifyAll(); 

                //Wait until results are verified by the client
                Controls.readyLock.wait(); 

                if (stop) {
                    return;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void done() {
        try {

            synchronized(Controls.readyLock) {
                Controls.done = true;
                //Notify the client to check the results
                Controls.readyLock.notifyAll();

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void debug(String mesg) {
        System.out.println("Dispatcher[" + id + "] --> " + mesg);
    }
}
