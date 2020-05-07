/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.jersey_cdi.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
@Path("/jcdibean/singleton")
@ApplicationScoped
public class JCDIBeanSingletonResource {

    private @Resource(name="injectedResource") int injectedResource = 0;

    // TODO: this should be using proxiable injection support
    private @Context UriInfo uiFieldInjectProvider;

    // TODO: this should be using proxiable injection support
    private @Context ResourceContext rcProvider;

    private UriInfo uiMethodInject;

    @Context
    public void set(UriInfo ui) {
        this.uiMethodInject = ui;
    }

    @PostConstruct
    public void postConstruct() {
        Logger.getLogger(JCDIBeanSingletonResource.class.getName()).log(Level.INFO,
                "In post construct " + this);

        if (uiFieldInjectProvider == null || uiMethodInject == null || rcProvider == null) {
            throw new IllegalStateException();
        }
    }

    @GET
    @Produces("text/plain")
    public String getMessage() {
        Logger.getLogger(JCDIBeanSingletonResource.class.getName()).log(Level.INFO,
                "In getMessage " + this +
                "; uiFieldInject: " + uiFieldInjectProvider + "; uiMethodInject: " + uiMethodInject);

        if (uiFieldInjectProvider == null || uiMethodInject == null || rcProvider == null) {
            throw new IllegalStateException();
        }

        return Integer.toString(injectedResource++);
    }

    @Path("exception")
    public String getException() {
        throw new JDCIBeanException();
    }

    @PreDestroy
    public void preDestroy() {
        Logger.getLogger(JCDIBeanSingletonResource.class.getName()).log(Level.INFO, "In pre destroy " + this);
    }
}
