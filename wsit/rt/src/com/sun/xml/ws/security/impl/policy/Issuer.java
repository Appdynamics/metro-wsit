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

/*
 * Issuer.java
 *
 * Created on February 22, 2006, 5:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.xml.ws.security.impl.policy;

import com.sun.xml.ws.addressing.policy.Address;
import com.sun.xml.ws.policy.AssertionSet;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.sourcemodel.AssertionData;
import java.util.Collection;
import java.util.Iterator;
import com.sun.xml.ws.security.policy.SecurityAssertionValidator;
/**
 *
 * @author Abhijit Das
 */
public class Issuer extends PolicyAssertion implements com.sun.xml.ws.security.policy.Issuer, SecurityAssertionValidator {
    private AssertionFitness fitness = AssertionFitness.IS_VALID;
    private Address address;
    private boolean populated = false;
    private PolicyAssertion refProps = null;
    private PolicyAssertion refParams = null;
    private PolicyAssertion serviceName = null;
    private String portType = null;
    
    /**
     * Creates a new instance of Issuer
     */
    public Issuer() {
    }
    
    public Issuer(AssertionData name,Collection<PolicyAssertion> nestedAssertions, AssertionSet nestedAlternative) {
        super(name,nestedAssertions,nestedAlternative);
    }
    
    public AssertionFitness validate(boolean isServer) {
        return populate(isServer);
    }
    private void populate(){
        populate(false);
    }
    
    private synchronized AssertionFitness populate(boolean isServer) {
        if(!populated){
            if ( this.hasNestedAssertions() ) {
                Iterator <PolicyAssertion> it = this.getNestedAssertionsIterator();
                while ( it.hasNext() ) {
                    PolicyAssertion assertion = it.next();
                    if ( PolicyUtil.isAddress(assertion)) {
                        this.address = (Address) assertion;
                    } else if(PolicyUtil.isPortType(assertion)){
                        this.portType = assertion.getValue();
                    } else if(PolicyUtil.isReferenceParameters(assertion)){
                        this.refParams = assertion;
                    } else if(PolicyUtil.isReferenceProperties(assertion)){
                        this.refProps = assertion;
                    } else if(PolicyUtil.isServiceName(assertion)){
                        this.serviceName = assertion;
                    }
                }
            }
            populated = true;
        }
        return fitness;
    }
    
    public Address getAddress() {
        populate();
        return (Address) address;
    }
    
    public String getPortType(){
        populate();
        return portType;
    }
    
    public PolicyAssertion getReferenceParameters(){
        populate();
        return refParams;
    }
    
    public PolicyAssertion getReferenceProperties(){
        populate();
        return refProps;
    }
    
    public PolicyAssertion getServiceName(){
        populate();
        return serviceName;
    }
}
