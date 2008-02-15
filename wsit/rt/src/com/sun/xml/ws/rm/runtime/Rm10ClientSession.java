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
package com.sun.xml.ws.rm.runtime;

import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.rm.CreateSequenceException;
import com.sun.xml.ws.rm.MessageNumberRolloverException;
import com.sun.xml.ws.rm.RmException;
import com.sun.xml.ws.rm.TerminateSequenceException;
import com.sun.xml.ws.rm.localization.RmLogger;
import com.sun.xml.ws.rm.policy.Configuration;
import com.sun.xml.ws.rm.runtime.Sequence.AckRange;
import com.sun.xml.ws.rm.v200502.AcceptType;
import com.sun.xml.ws.rm.v200502.AckRequestedElement;
import com.sun.xml.ws.rm.v200502.CreateSequenceElement;
import com.sun.xml.ws.rm.v200502.CreateSequenceResponseElement;
import com.sun.xml.ws.rm.v200502.Identifier;
import com.sun.xml.ws.rm.v200502.OfferType;
import com.sun.xml.ws.rm.v200502.SequenceAcknowledgementElement;
import com.sun.xml.ws.rm.v200502.SequenceElement;
import com.sun.xml.ws.rm.v200502.SequenceElement.LastMessage;
import com.sun.xml.ws.rm.v200502.TerminateSequenceElement;
import com.sun.xml.ws.rm.v200702.TerminateSequenceResponseElement;
import com.sun.xml.ws.security.secext10.SecurityTokenReferenceType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

/**
 *
 * @author Marek Potociar (marek.potociar at sun.com)
 */
final class Rm10ClientSession extends ClientSession {

    private static final RmLogger LOGGER = RmLogger.getLogger(Rm10ClientSession.class);

    public Rm10ClientSession(WSDLPort wsdlPort, ProtocolCommunicator communicator, Configuration configuration) {
        super(wsdlPort, communicator, configuration);
    }

    @Override
    protected void openRmSession(String offerInboundSequenceId, SecurityTokenReferenceType strType) throws RmException {
        CreateSequenceElement csElement = new CreateSequenceElement();

        if (configuration.getAddressingVersion() == AddressingVersion.W3C) {
            csElement.setAcksTo(new W3CEndpointReference(AddressingVersion.W3C.anonymousEpr.asSource("AcksTo")));
        } else {
            // TODO L10N
            throw LOGGER.logSevereException(new IllegalStateException("Unsupported addressing version [" + configuration.getAddressingVersion() + "]"));
        }

        if (offerInboundSequenceId != null) {
            Identifier offerIdentifier = new Identifier();
            offerIdentifier.setValue(offerInboundSequenceId);

            OfferType offer = new OfferType();
            offer.setIdentifier(offerIdentifier);
            csElement.setOffer(offer);
        }
        if (strType != null) {
            csElement.setSecurityTokenReference(strType);
        }

        Message csMessage = Messages.create(configuration.getRmVersion().jaxbContext, csElement, configuration.getSoapVersion());

        Message csResponseMessage = communicator.send(csMessage, configuration.getRmVersion().createSequenceAction);
        if (csResponseMessage == null) {
            // TODO L10N
            throw LOGGER.logSevereException(new CreateSequenceException("CreateSequenceResponse was 'null'"));
        }
        if (csResponseMessage.isFault()) {
            // TODO L10N
            throw LOGGER.logSevereException(new CreateSequenceException("CreateSequence was refused by the RMDestination", csResponseMessage));
        }

        CreateSequenceResponseElement csrElement = unmarshallResponse(csResponseMessage);
        outboundSequenceId = csrElement.getIdentifier().getValue();

        long expirationTime = Configuration.UNSPECIFIED;
        if (csrElement.getExpires() != null && !"PT0S".equals(csrElement.getExpires().getValue().toString())) {
            expirationTime = csrElement.getExpires().getValue().getTimeInMillis(Calendar.getInstance()) + System.currentTimeMillis();
        }
        sequenceManager.createOutboudSequence(outboundSequenceId, expirationTime);


        if (offerInboundSequenceId != null) {
            AcceptType accept = csrElement.getAccept();
            // TODO : make sure we are sending acknowledgements to the proper acksTo endpoint
//            if (!configuration.getAddressingVersion().anonymousEpr.equals(accept.getAcksTo())) {
//                // TODO L10N
//                throw new CreateSequenceException("Addressable \"AcksTo\" endpoint not supported for inbound sequence", inboundSequenceId);
//            }
            inboundSequenceId = offerInboundSequenceId;
            sequenceManager.createInboundSequence(inboundSequenceId, Configuration.UNSPECIFIED);
        }
    }

    @Override
    protected void appendSequenceHeader(Message outboundMessage) throws UnknownSequenceException, MessageNumberRolloverException {
        long messageNumber = sequenceManager.getSequence(outboundSequenceId).getNextMessageId();

        SequenceElement sequenceHeaderElement = new SequenceElement();
        sequenceHeaderElement.setNumber(messageNumber);
        sequenceHeaderElement.setId(outboundSequenceId);

        outboundMessage.getHeaders().add(createHeader(sequenceHeaderElement));
    }

    @Override
    protected void appendAckRequestedHeader(Message outboundMessage) {
        AckRequestedElement ackRequestedElement = new AckRequestedElement();
        ackRequestedElement.setId(outboundSequenceId);

        outboundMessage.getHeaders().add(createHeader(ackRequestedElement));
    }

    @Override
    protected void appendSequenceAcknowledgementHeader(Message outboundMessage) throws UnknownSequenceException {
        SequenceAcknowledgementElement ackElement = new SequenceAcknowledgementElement();
        Identifier identifier = new Identifier();
        identifier.setValue(inboundSequenceId);
        ackElement.setIdentifier(identifier);

        final List<AckRange> acknowledgedIndexes = sequenceManager.getSequence(inboundSequenceId).getAcknowledgedMessageIds();
        if (acknowledgedIndexes != null && !acknowledgedIndexes.isEmpty()) {
            for (Sequence.AckRange range : acknowledgedIndexes) {
                ackElement.addAckRange(range.lower, range.upper);
            }
        } else {
            ackElement.addAckRange(0, 0); // we don't have any ack ranges => we have not received any message yet
        }

// TODO move this to server side - we don't have a buffer support on the client side
//        if (configuration.getDestinationBufferQuota() != Configuration.UNSPECIFIED) {
//            ackElement.setBufferRemaining(-1/*calculate remaining quota*/);
//        }

        outboundMessage.getHeaders().add(createHeader(ackElement));
    }

    @Override
    protected void closeOutboundSequence() throws RmException {
        Message lastMessage = Messages.createEmpty(configuration.getSoapVersion());

        appendSequenceAcknowledgementHeader(lastMessage);

        SequenceElement sequenceElement = new SequenceElement();
        sequenceElement.setId(outboundSequenceId);
        sequenceElement.setNumber(sequenceManager.getSequence(outboundSequenceId).getLastMessageId());
        sequenceElement.setLastMessage(new LastMessage());

        lastMessage.getHeaders().add(Headers.create(configuration.getRmVersion().jaxbContext, sequenceElement));
        Message response = null;
        try {
            response = communicator.send(lastMessage, configuration.getRmVersion().lastAction);
            if (response != null) {
                processInboundMessageHeaders(response.getHeaders(), false);
                if (response.isFault()) {
                    // TODO L10N
                    throw new RmException("Error sending Last message", response);
                }
            }
        } finally {
            if (response != null) {
                response.consume(); // we need to consume message as we didn't read it (e.g. as JAXB bean)
            }
        }
    }

    @Override
    protected void terminateOutboundSequence() throws RmException {
        final Message request = Messages.create(
                configuration.getRmVersion().jaxbContext,
                new TerminateSequenceElement(outboundSequenceId),
                configuration.getSoapVersion());

        appendSequenceAcknowledgementHeader(request);

        Message response = null;
        try {
            response = communicator.send(request, configuration.getRmVersion().terminateSequenceAction);
            if (response == null) {
                // TODO L10N
                throw new TerminateSequenceException("TerminateSequenceResponse was 'null'");
            }

            processInboundMessageHeaders(response.getHeaders(), false);

            if (response.isFault()) {
                // TODO L10N
                throw new TerminateSequenceException("There was an error during the sequence termination", response);
            }

            String responseAction = communicator.getAction(response);
            if (configuration.getRmVersion().terminateSequenceAction.equals(responseAction)) {
                TerminateSequenceElement tsElement = unmarshallResponse(response);
                response = null; // marking response as consumed...
                sequenceManager.terminateSequence(tsElement.getIdentifier().toString());
            } else if (configuration.getRmVersion().terminateSequenceResponseAction.equals(responseAction)) {
                TerminateSequenceResponseElement tsrElement = unmarshallResponse(response);
                response = null; // marking response as consumed...
                if (!outboundSequenceId.equals(tsrElement.getIdentifier().getValue())) {
                    // TODO L10N
                    throw new TerminateSequenceException("The sequence identifier in the terminate sequence response message [" + tsrElement.getIdentifier().getValue() + "]" +
                            " does not correspond to the terminating outbound sequence identifier [" + outboundSequenceId + "]");
                }
            }
        } finally {
            if (response != null) {
                response.consume();
            }
        }
    }

    @Override
    protected void processSequenceHeader(HeaderList inboundMessageHeaders) throws RmException {
        SequenceElement sequenceElement = readHeaderAsUnderstood(inboundMessageHeaders, "Sequence");
        if (sequenceElement != null) {
            assertSequenceIdInInboundHeader(inboundSequenceId, sequenceElement.getId());
            sequenceManager.getSequence(sequenceElement.getId()).acknowledgeMessageId(sequenceElement.getMessageNumber());
        } else {
            // TODO L10N
            throw new RmException("Mandatory <Sequence ... /> header not present on the response message");
        }
    }

    @Override
    protected void processAcknowledgementHeader(HeaderList inboundMessageHeaders) throws RmException {
        SequenceAcknowledgementElement ackElement = readHeaderAsUnderstood(inboundMessageHeaders, "SequenceAcknowledgement");

        if (ackElement != null) {
            assertSequenceIdInInboundHeader(outboundSequenceId, ackElement.getId());

            List<Sequence.AckRange> ranges = new LinkedList<Sequence.AckRange>();
            if (!ackElement.getNack().isEmpty()) {
                List<BigInteger> nacks = new ArrayList<BigInteger>(ackElement.getNack());
                Collections.sort(nacks);
                long lastLowerBound = 1;
                for (BigInteger nackId : nacks) {
                    if (lastLowerBound == nackId.longValue()) {
                        lastLowerBound++;
                    } else {
                        ranges.add(new Sequence.AckRange(lastLowerBound, nackId.longValue() - 1));
                        lastLowerBound =
                                nackId.longValue() + 1;
                    }

                }

                long lastMessageId = sequenceManager.getSequence(outboundSequenceId).getLastMessageId();
                if (lastLowerBound <= lastMessageId) {
                    ranges.add(new Sequence.AckRange(lastLowerBound, lastMessageId));
                }

            } else if (ackElement.getAcknowledgementRange() != null && !ackElement.getAcknowledgementRange().isEmpty()) {
                for (SequenceAcknowledgementElement.AcknowledgementRange rangeElement : ackElement.getAcknowledgementRange()) {
                    ranges.add(new Sequence.AckRange(rangeElement.getLower().longValue(), rangeElement.getUpper().longValue()));
                }

            }

            sequenceManager.getSequence(outboundSequenceId).acknowledgeMessageIds(ranges);

        // TODO handle other stuff in the header
        // ackElement.getBufferRemaining();
        }
    }

    @Override
    protected void processAckRequestedHeader(HeaderList inboundMessageHeaders) throws RmException {
        AckRequestedElement ackRequestedElement = readHeaderAsUnderstood(inboundMessageHeaders, "AckRequested");
        if (ackRequestedElement != null) {
            String sequenceId = ackRequestedElement.getId();
            assertSequenceIdInInboundHeader(inboundSequenceId, sequenceId);
            sequenceManager.getSequence(sequenceId).setAckRequestedFlag();
        }
    }
}
