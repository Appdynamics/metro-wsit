/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.ws.api.config.management;

import com.sun.istack.logging.Logger;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.FiberContextSwitchInterceptor;
import com.sun.xml.ws.api.pipe.ServerTubeAssemblerContext;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.EndpointComponent;
import com.sun.xml.ws.api.server.ServiceDefinition;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WSEndpoint.CompletionCallback;
import com.sun.xml.ws.api.server.WSEndpoint.PipeHead;
import com.sun.xml.ws.config.management.ManagementMessages;
import com.sun.xml.ws.policy.PolicyMap;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;

import org.glassfish.gmbal.ManagedObjectManager;

/**
 * Wraps an existing WSEndpoint instance and provides a method to swap the
 * WSEndpoint instance. This class also brings up the management communication
 * interfaces when it is instantiated.
 *
 * This class forwards all method invocations to the wrapped WSEndpoint instance.
 *
 * @author Fabian Ritzmann
 */
public class ManagedEndpoint<T> extends WSEndpoint<T> implements EndpointStarter {

    public static final String ENDPOINT_ID_PARAMETER_NAME = "ENDPOINT_ID";
    public static final String ENDPOINT_INSTANCE_PARAMETER_NAME = "ENDPOINT_INSTANCE";
    public static final String CREATION_ATTRIBUTES_PARAMETER_NAME = "CREATION_ATTRIBUTES";
    public static final String CLASS_LOADER_PARAMETER_NAME = "CLASS_LOADER";
    public static final String ENDPOINT_STARTER_PARAMETER_NAME = "ENDPOINT_STARTER";

    private static final Logger LOGGER = Logger.getLogger(ManagedEndpoint.class);

    private final String id;
    private final EndpointCreationAttributes creationAttributes;
    private WSEndpoint<T> endpointDelegate;

    private final CountDownLatch startSignal = new CountDownLatch(1);

    private final Collection<CommunicationServer> commInterfaces;

    /**
     * Initializes this endpoint.
     *
     * @param id A unique ID of the managed endpoint.
     * @param endpoint The wrapped WSEndpoint instance.
     * @param attributes Several attributes that were used to create the original WSEndpoint
     *   instance and that cannot be queried from WSEndpoint itself. This is used by
     *   the communication API to recreate WSEndpoint instances with the same parameters.
     */
    public ManagedEndpoint(final String id, final WSEndpoint<T> endpoint, final EndpointCreationAttributes attributes) {
        try {
            this.id = id;
            this.creationAttributes = attributes;
            this.endpointDelegate = endpoint;

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }

            final NamedParameters parameters = new NamedParameters()
                    .put(ENDPOINT_ID_PARAMETER_NAME, this.id)
                    .put(ENDPOINT_INSTANCE_PARAMETER_NAME, this)
                    .put(CREATION_ATTRIBUTES_PARAMETER_NAME, this.creationAttributes)
                    .put(CLASS_LOADER_PARAMETER_NAME, classLoader)
                    .put(ENDPOINT_STARTER_PARAMETER_NAME, this);
            this.commInterfaces = ManagementFactory.createCommunicationImpls(parameters);
            for (CommunicationServer commInterface : commInterfaces) {
                commInterface.start();
            }

            // TODO log that we are blocking
            // block until we receive a start signal
            startSignal.await();
        } catch (InterruptedException e) {
            // TODO add error message
            throw LOGGER.logSevereException(new WebServiceException(e));
        }
    }

    public void startEndpoint() {
        this.startSignal.countDown();
    }

    /**
     * Return the ID of this managed endpoint.
     *
     * @return The ID of the managed endpoint.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets a new WSEndpoint instance to which method calls will be forwarded from
     * then on.
     *
     * @param endpoint The WSEndpoint instance. May not be null.
     */
    synchronized public void swapEndpointDelegate(final WSEndpoint<T> endpoint) {
        // Plug in code that regenerates WSDL when the endpoint was reconfigured
        final Set<EndpointComponent> endpointComponents = endpoint.getComponentRegistry();
        endpointComponents.add(new ManagedHttpMetadataPublisher());

        this.endpointDelegate = endpoint;
        LOGGER.info(ManagementMessages.WSM_5004_RECONFIGURED_ENDPOINT(this.id));
    }

    @Override
    public Codec createCodec() {
        return this.endpointDelegate.createCodec();
    }

    @Override
    public QName getServiceName() {
        return this.endpointDelegate.getServiceName();
    }

    @Override
    public QName getPortName() {
        return this.endpointDelegate.getPortName();
    }

    @Override
    public Class<T> getImplementationClass() {
        return this.endpointDelegate.getImplementationClass();
    }

    @Override
    public WSBinding getBinding() {
        return this.endpointDelegate.getBinding();
    }

    @Override
    public Container getContainer() {
        return this.endpointDelegate.getContainer();
    }

    @Override
    public WSDLPort getPort() {
        return this.endpointDelegate.getPort();
    }

    @Override
    public void setExecutor(Executor exec) {
        this.endpointDelegate.setExecutor(exec);
    }

    @Override
    public void schedule(Packet request, CompletionCallback callback, FiberContextSwitchInterceptor interceptor) {
        this.endpointDelegate.schedule(request, callback, interceptor);
    }

    @Override
    public PipeHead createPipeHead() {
        return this.endpointDelegate.createPipeHead();
    }

    @Override
    public void dispose() {
        for (CommunicationServer commInterface: this.commInterfaces) {
            commInterface.stop();
        }
        if (this.endpointDelegate != null) {
            this.endpointDelegate.dispose();
        }
    }

    @Override
    public ServiceDefinition getServiceDefinition() {
        return this.endpointDelegate.getServiceDefinition();
    }

    @Override
    public Set<EndpointComponent> getComponentRegistry() {
        return this.endpointDelegate.getComponentRegistry();
    }

    @Override
    public SEIModel getSEIModel() {
        return this.endpointDelegate.getSEIModel();
    }

    @Override
    public PolicyMap getPolicyMap() {
        return this.endpointDelegate.getPolicyMap();
    }

    @Override
    public ManagedObjectManager getManagedObjectManager() {
        return this.endpointDelegate.getManagedObjectManager();
    }

    @Override
    public ServerTubeAssemblerContext getAssemblerContext() {
        return this.endpointDelegate.getAssemblerContext();
    }

}
