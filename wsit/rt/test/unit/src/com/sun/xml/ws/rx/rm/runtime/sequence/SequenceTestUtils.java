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
package com.sun.xml.ws.rx.rm.runtime.sequence;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.rx.RxConfiguration;
import com.sun.xml.ws.rx.mc.McVersion;
import com.sun.xml.ws.rx.rm.ReliableMessagingFeature.BackoffAlgorithm;
import com.sun.xml.ws.rx.rm.ReliableMessagingFeature.DeliveryAssurance;
import com.sun.xml.ws.rx.rm.ReliableMessagingFeature.SecurityBinding;
import com.sun.xml.ws.rx.rm.RmVersion;
import com.sun.xml.ws.rx.rm.runtime.ApplicationMessage;
import com.sun.xml.ws.rx.rm.runtime.delivery.DeliveryQueueBuilder;
import com.sun.xml.ws.rx.rm.runtime.delivery.Postman;
import com.sun.xml.ws.rx.rm.runtime.delivery.PostmanPool;
import java.util.LinkedList;
import java.util.List;
import org.glassfish.gmbal.ManagedObjectManager;

/**
 *
 * @author Marek Potociar <marek.potociar at sun.com>
 */
final class SequenceTestUtils  {
    private SequenceTestUtils() {}

    static final DeliveryQueueBuilder getDeliveryQueueBuilder() {
        Tube tubeline = new AbstractFilterTubeImpl(null) {

            @Override
            public AbstractTubeImpl copy(TubeCloner cloner) {
                return this;
            }
        };
        RxConfiguration config = new RxConfiguration() {

            public boolean isReliableMessagingEnabled() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isMakeConnectionSupportEnabled() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public RmVersion getRmVersion() {
                return RmVersion.WSRM200702;
            }

            public McVersion getMcVersion() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public SOAPVersion getSoapVersion() {
                return SOAPVersion.SOAP_12;
            }

            public AddressingVersion getAddressingVersion() {
                return AddressingVersion.W3C;
            }

            public boolean requestResponseOperationsDetected() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public long getSequenceInactivityTimeout() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public SecurityBinding getSecurityBinding() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public DeliveryAssurance getDeliveryAssurance() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isOrderedDeliveryEnabled() {
                return false;
            }

            public long getDestinationBufferQuota() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public long getMessageRetransmissionInterval() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public BackoffAlgorithm getRetransmissionBackoffAlgorithm() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public long getAcknowledgementRequestInterval() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public long getCloseSequenceOperationTimeout() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public ManagedObjectManager getManagedObjectManager() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };

        return DeliveryQueueBuilder.getBuilder(config, PostmanPool.INSTANCE.getPostman(), new Postman.Callback() {

            public void deliver(ApplicationMessage message) {
            }
        });
    }

    static final List<Sequence.AckRange> createAckRanges(long... msgNumbers) {
        List<Sequence.AckRange> ackList = new LinkedList<Sequence.AckRange>();

        if (msgNumbers.length > 0) {
            long lower = msgNumbers[0];
            long upper = msgNumbers[0] - 1;
            for (long number : msgNumbers) {
                if (number == upper + 1) {
                    upper = number;
                } else {
                    ackList.add(new Sequence.AckRange(lower, upper));
                    lower = upper = number;
                }
            }
            ackList.add(new Sequence.AckRange(lower, upper));
        }
        return ackList;
    }

}
