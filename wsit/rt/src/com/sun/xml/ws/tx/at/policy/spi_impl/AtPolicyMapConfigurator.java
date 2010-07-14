/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.xml.ws.tx.at.policy.spi_impl;

import com.sun.istack.logging.Logger;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.policy.AssertionSet;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.policy.PolicySubject;
import com.sun.xml.ws.policy.SimpleAssertion;
import com.sun.xml.ws.policy.jaxws.spi.PolicyMapConfigurator;
import com.sun.xml.ws.policy.sourcemodel.AssertionData;
import com.sun.xml.ws.policy.subject.WsdlBindingSubject;
import com.sun.xml.ws.tx.at.api.WsatNamespace;
import com.sun.xml.ws.tx.at.localization.LocalizationMessages;
import com.sun.xml.ws.tx.at.policy.spi_impl.EjbTransactionAnnotationProcessor.TransactionAttributeType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * From CMT EJB methods generate wsdl:binding/wsdl:operations with semantically equivalent WS-AT Policy Assertion(s).
 * <p/>
 * Known limitation: not accounting for EJB deployment descriptor, only working off of TransactionAttribute annotations.
 */
public class AtPolicyMapConfigurator implements PolicyMapConfigurator {

    private static final Logger LOGGER = Logger.getLogger(AtPolicyMapConfigurator.class);

    private static boolean NON_JAVAEE_CONTAINER = false;

    /**
     * Update policy map with operation scope of correct WS-AT policy assertions.
     * <p/>
     * Only looking for this for Java to WSDL at tool time.
     *
     * @param policyMapMutator
     * @param policyMap
     * @param model
     * @param wsBinding
     */
    public Collection<PolicySubject> update(final PolicyMap policyMap, final SEIModel model, final WSBinding wsBinding) throws PolicyException {
        final Collection<PolicySubject> subjects = new ArrayList<PolicySubject>();

        if (NON_JAVAEE_CONTAINER) {
            return subjects;
        }

        // For each method of a CMT EJB, map its effective javax.ejb.TransactionAttribute to semantically equivalent 
        // ws-at policy assertion.
        if (model != null) {
            final Collection<? extends JavaMethod> methods = model.getJavaMethods();
            Class CMTEJB = null;
            TransactionAttributeType classDefaultTxnAttr = null;
            for (JavaMethod method : methods) {

                if (CMTEJB == null) {
                    boolean isCMTEJB = false;
                    final Class theClass = method.getSEIMethod().getDeclaringClass();
                    try {
                        isCMTEJB = EjbTransactionAnnotationProcessor.isContainerManagedEJB(theClass);
                    } catch (NoClassDefFoundError e) {
                        // running in a container that does not support EJBs; terminate processing of EJB annotations
                        NON_JAVAEE_CONTAINER = true;
                        LOGGER.fine(LocalizationMessages.WSAT_1001_NON_EE_CONTAINER("NoClassDefFoundError: " + e.getLocalizedMessage()));
                        return subjects;
                    }
                    if (isCMTEJB) {
                        // perform class level caching of info
                        CMTEJB = theClass;
                        classDefaultTxnAttr = EjbTransactionAnnotationProcessor.getTransactionAttributeDefault(theClass);
                    } else {
                        // not a CMT EJB, no transaction attributes to look for; just return
                        return subjects;
                    }
                }

                // we have a CMT EJB. Map its transaction attribute to proper ws-at policy assertion.

                final TransactionAttributeType txnAttr =
                        EjbTransactionAnnotationProcessor.getEffectiveTransactionAttribute(method.getSEIMethod(), classDefaultTxnAttr);
                final String policyId = model.getBoundPortTypeName().getLocalPart() + "_" + method.getOperationName() + "_WSAT_Policy";
                final Policy policy = mapTransactionAttribute2WSATPolicy(policyId, txnAttr);
                if (policy != null) {
                    // attach ws-at policy assertion to binding/operation
                    final WsdlBindingSubject wsdlSubject = WsdlBindingSubject.createBindingOperationSubject(model.getBoundPortTypeName(),
                                                                                                            new QName(model.getTargetNamespace(), method.getOperationName()));
                    final PolicySubject generatedWsatPolicySubject = new PolicySubject(wsdlSubject, policy);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(LocalizationMessages.WSAT_1002_ADD_AT_POLICY_ASSERTION(
                                model.getPortName().toString(),
                                method.getOperationName(),
                                policy.toString(),
                                txnAttr.toString(),
                                CMTEJB.getName(),
                                method.getMethod().getName()));
                    } else {
                        LOGGER.info(LocalizationMessages.WSAT_1002_ADD_AT_POLICY_ASSERTION(
                                model.getPortName().toString(),
                                method.getOperationName(),
                                policy.getId(),
                                txnAttr.toString(),
                                CMTEJB.getName(),
                                method.getMethod().getName()));
                    }
                    subjects.add(generatedWsatPolicySubject);
                }
            } // for each method in CMT EJB
        }
        return subjects;
    }
    
    static class WsatPolicyAssertion extends SimpleAssertion {
        /**
         * patch for wsit 419
         */
        private static final QName WSP2002_OPTIONAL = new QName("http://schemas.xmlsoap.org/ws/2002/12/policy", "Optional");
        //
        private static AssertionData createAssertionData(final QName assertionQName, final boolean isOptional) {
            final AssertionData result = AssertionData.createAssertionData(assertionQName);
            result.setOptionalAttribute(isOptional);
            if (isOptional) {
                // patch for wsit 419
                result.setAttribute(WSP2002_OPTIONAL, "true");
            }
            return result;
        }

        WsatPolicyAssertion(final QName wsatPolicyAssertionName, final boolean isOptional) {
            super(createAssertionData(wsatPolicyAssertionName, isOptional), null);
        }
    }
    
    private static final QName AT_ASSERTION = WsatNamespace.WSAT200410.createFqn("ATAssertion");
    private static final WsatPolicyAssertion AT_ASSERTION_OPTIONAL = new WsatPolicyAssertion(AT_ASSERTION, true);
    private static final WsatPolicyAssertion AT_ASSERTION_REQUIRED = new WsatPolicyAssertion(AT_ASSERTION, false);
    private static final WsatPolicyAssertion AT_ALWAYS_CAPABILITY_PA = new WsatPolicyAssertion(WsatNamespace.WSAT200410.createFqn("ATAllwaysCapability"), false);
    
    /**
     * Pass in what the effective transaction attribute for a given Container Manager Transaction EJB method and return the
     * semantically closest WS-AT policy assertion.
     * <p/>
     * This is best match between Java EE Transaction Attribute and WS-AT Policy Assertion.
     * There are a number of differences between them.
     */
    private Policy mapTransactionAttribute2WSATPolicy(final String id, final TransactionAttributeType txnAttr) {

        switch (txnAttr) {
            case NOT_SUPPORTED:
            case NEVER:          // ws-at does not require exception thrown if txn propagated with no assertion.
                // no ws-at policy assertion on wsdl:binding/wsdl:operation is equivalent of no
                // claim.
                return null;

            case MANDATORY:
                return createATPolicy(id, AT_ASSERTION_REQUIRED);

            case SUPPORTS:
                return createATPolicy(id, AT_ASSERTION_OPTIONAL);

            case REQUIRES_NEW:
                return createATPolicy(id, AT_ALWAYS_CAPABILITY_PA);

            case REQUIRED:
                return createATPolicy(id, AT_ASSERTION_OPTIONAL, AT_ALWAYS_CAPABILITY_PA);

            default:
                return null;
        }
    }

    private static Policy createATPolicy(final String id, final WsatPolicyAssertion... assertions) {
        assert assertions != null && assertions.length > 0;

        final List<AssertionSet> assertionSets = new ArrayList<AssertionSet>(1);
        assertionSets.add(AssertionSet.createAssertionSet(Arrays.asList(assertions)));

        return Policy.createPolicy("", id, assertionSets);
    }
}
