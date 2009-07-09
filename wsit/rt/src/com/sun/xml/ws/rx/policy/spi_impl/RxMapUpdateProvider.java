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
package com.sun.xml.ws.rx.policy.spi_impl;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.istack.logging.Logger;
import com.sun.xml.ws.policy.AssertionSet;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.policy.PolicyMapKey;
import com.sun.xml.ws.policy.PolicySubject;
import com.sun.xml.ws.policy.jaxws.spi.PolicyMapConfigurator;
import com.sun.xml.ws.policy.subject.WsdlBindingSubject;
import com.sun.xml.ws.rx.mc.MakeConnectionSupportedFeature;
import com.sun.xml.ws.rx.policy.assertion.MakeConnectionSupportedAssertion;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import javax.xml.namespace.QName;

/**
 *
 * @author Marek Potociar <marek.potociar at sun.com>
 */
public class RxMapUpdateProvider implements PolicyMapConfigurator {

    private static final Logger LOGGER = Logger.getLogger(RxMapUpdateProvider.class);

    public Collection<PolicySubject> update(PolicyMap policyMap, SEIModel model, WSBinding wsBinding) throws PolicyException {
        final Collection<PolicySubject> subjects = new LinkedList<PolicySubject>();

        try {
            LOGGER.entering(policyMap, model, wsBinding);

            updateMakeConnectionSettings(subjects, wsBinding, model, policyMap);

            return subjects;

            // TODO : update map with RM policy based on RM feature

        } finally {
            LOGGER.exiting(subjects);
        }
    }

    /**
     * Create a policy with an MCSupported assertion.
     *
     * @param bindingName The wsdl:binding element name. Used to generate a (locally) unique ID for the policy.
     * @return A policy that contains one policy assertion that corresponds to the given assertion name.
     */
    private Policy createMakeConnectionPolicy(final QName bindingName) {
        return Policy.createPolicy(null, bindingName.getLocalPart() + "_MCSupported_Policy", Arrays.asList(new AssertionSet[]{
                    AssertionSet.createAssertionSet(Arrays.asList(new PolicyAssertion[]{new MakeConnectionSupportedAssertion()}))
                }));
    }

    private void updateMakeConnectionSettings(Collection<PolicySubject> subjects, WSBinding wsBinding, SEIModel model, PolicyMap policyMap) throws PolicyException, IllegalArgumentException {
        final MakeConnectionSupportedFeature mcFeature = wsBinding.getFeature(MakeConnectionSupportedFeature.class);
        if (mcFeature == null || !mcFeature.isEnabled()) {
            return;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            // TODO L10N
            LOGGER.finest(String.format("Make connection feature enabled on service '%s', port '%s'", model.getServiceQName(), model.getPortName()));
        }

        final PolicyMapKey endpointKey = PolicyMap.createWsdlEndpointScopeKey(model.getServiceQName(), model.getPortName());
        final Policy existingPolicy = (policyMap != null) ? policyMap.getEndpointEffectivePolicy(endpointKey) : null;
        if ((existingPolicy == null) || !existingPolicy.contains(MakeConnectionSupportedAssertion.NAME)) {
            final Policy mcPolicy = createMakeConnectionPolicy(model.getBoundPortTypeName());
            final WsdlBindingSubject wsdlSubject = WsdlBindingSubject.createBindingSubject(model.getBoundPortTypeName());
            final PolicySubject subject = new PolicySubject(wsdlSubject, mcPolicy);
            subjects.add(subject);
            if (LOGGER.isLoggable(Level.FINE)) {
                // TODO L10N
                LOGGER.fine(String.format("Added make connection policy with ID '%s' to binding element '%s'", mcPolicy.getIdOrName(), model.getBoundPortTypeName()));
            }
        } else if (LOGGER.isLoggable(Level.FINE)) {
            // TODO L10N
            LOGGER.fine("Make connection assertion is already present in the endpoint policy");
        }
    }
}
