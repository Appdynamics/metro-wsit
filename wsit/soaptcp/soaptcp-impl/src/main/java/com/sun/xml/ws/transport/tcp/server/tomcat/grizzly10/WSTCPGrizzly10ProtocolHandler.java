/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.transport.tcp.server.tomcat.grizzly10;

import com.sun.enterprise.web.connector.grizzly.SelectorThread;
import com.sun.enterprise.web.portunif.PortUnificationPipeline;
import com.sun.enterprise.web.portunif.TlsProtocolFinder;
import com.sun.xml.ws.transport.tcp.grizzly.WSTCPProtocolFinder;
import com.sun.xml.ws.transport.tcp.grizzly.WSTCPProtocolHandler;
import com.sun.xml.ws.transport.tcp.server.IncomeMessageProcessor;
import com.sun.xml.ws.transport.tcp.server.tomcat.WSTCPTomcatRegistry;
import com.sun.xml.ws.transport.tcp.server.tomcat.WSTCPTomcatProtocolHandlerBase;
import java.io.IOException;

/**
 * @author Alexey Stashok
 */
public class WSTCPGrizzly10ProtocolHandler extends WSTCPTomcatProtocolHandlerBase {
    private SelectorThread grizzlySelectorThread;
    
    @Override
    public void init() throws Exception {
        super.init();
        
        try {
            grizzlySelectorThread = createSelectorThread();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void run() {
        try {
            grizzlySelectorThread.startEndpoint();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void destroy() throws Exception {
        if (grizzlySelectorThread != null) {
            grizzlySelectorThread.stopEndpoint();
        }
    }
    
    private SelectorThread createSelectorThread() throws IOException, InstantiationException {
        final IncomeMessageProcessor messageProcessor = IncomeMessageProcessor.registerListener(port,
                (WSTCPTomcatRegistry) WSTCPTomcatRegistry.getInstance(), null);
        
        final SelectorThread selectorThread = new SelectorThread() {
            @Override
            protected void rampUpProcessorTask() {}
            @Override
            protected void registerComponents() {}
        };
        
        selectorThread.setPort(port);
        if (readThreadsCount > 0) {
            selectorThread.setSelectorReadThreadsCount(readThreadsCount);
        }
        
        if (maxWorkerThreadsCount >= 0) {
            selectorThread.setMaxThreads(maxWorkerThreadsCount);
        }
        
        if (minWorkerThreadsCount >= 0) {
            selectorThread.setMinThreads(minWorkerThreadsCount);
        }
        
        selectorThread.setPipelineClassName(PortUnificationPipeline.class.getName());
        selectorThread.initEndpoint();
        
        PortUnificationPipeline puPipeline = (PortUnificationPipeline) selectorThread.getProcessorPipeline();
        puPipeline.addProtocolFinder(new WSTCPProtocolFinder());
        puPipeline.addProtocolFinder(new TlsProtocolFinder());
        puPipeline.addProtocolFinder(new HttpRedirectorProtocolFinder());
        
        WSTCPProtocolHandler protocolHandler = new WSTCPProtocolHandler();
        WSTCPProtocolHandler.setIncomingMessageProcessor(messageProcessor);
        puPipeline.addProtocolHandler(protocolHandler);
        
        puPipeline.addProtocolHandler(new HttpRedirectorProtocolHandler(redirectHttpPort));
        
        return selectorThread;
    }
}