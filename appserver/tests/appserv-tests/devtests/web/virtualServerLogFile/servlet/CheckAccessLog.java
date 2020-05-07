/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class CheckAccessLog extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        String location = req.getParameter("location");
        String[] files = new File(location + "/domains/domain1/logs/vslogs").list();
        System.out.println("Checking file location "+location+"/domains/domain1/logs/vslogs");
        if (files != null && files.length == 2) {
            resp.getWriter().println("SUCCESS!");
        }    
    }
}
