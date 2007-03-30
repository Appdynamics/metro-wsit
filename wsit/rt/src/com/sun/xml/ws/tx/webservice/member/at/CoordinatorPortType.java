/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.tx.webservice.member.at;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.1-hudson-812-EA2
 * Generated source version: 2.0
 */
@WebService(name = "CoordinatorPortType", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/10/wsat",
        wsdlLocation = "WEB-INF/wsdl/wsat.wsdl")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
        com.sun.xml.ws.tx.webservice.member.at.ObjectFactory.class
        })
public interface CoordinatorPortType {


    /**
     * @param parameters
     */
    @WebMethod(operationName = "PreparedOperation", action = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Prepared")
    @Oneway
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Prepared")
    public void preparedOperation(
            @WebParam(name = "Prepared", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/10/wsat", partName = "parameters") Notification parameters);

    /**
     * @param parameters
     */
    @WebMethod(operationName = "AbortedOperation", action = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Aborted")
    @Oneway
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Aborted")
    public void abortedOperation(
            @WebParam(name = "Aborted", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/10/wsat", partName = "parameters") Notification parameters);

    /**
     * @param parameters
     */
    @WebMethod(operationName = "ReadOnlyOperation", action = "http://schemas.xmlsoap.org/ws/2004/10/wsat/ReadOnly")
    @Oneway
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/10/wsat/ReadOnly")
    public void readOnlyOperation(
            @WebParam(name = "ReadOnly", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/10/wsat", partName = "parameters") Notification parameters);

    /**
     * @param parameters
     */
    @WebMethod(operationName = "CommittedOperation", action = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Committed")
    @Oneway
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Committed")
    public void committedOperation(
            @WebParam(name = "Committed", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/10/wsat", partName = "parameters") Notification parameters);

    /**
     * @param parameters
     */
    @WebMethod(operationName = "ReplayOperation", action = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Replay")
    @Oneway
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/10/wsat/Replay")
    public void replayOperation(
            @WebParam(name = "Replay", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/10/wsat", partName = "parameters") Notification parameters);

}
