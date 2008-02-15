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

/**
 *
 * @author Marek Potociar (marek.potociar at sun.com)
 */
public interface SequenceManager {

    /**
     * Creates a new outbound sequence object with a given Id. It is assumed that RM handshake has been alrady established,
     * thus no RM handshake is performed.
     * 
     * @param sequenceId identifier of the new sequence
     * 
     * @param expirationTime expiration time of the sequence in milliseconds; value of {@link com.sun.xml.ws.rm.policy.Configuration#UNSPECIFIED}
     * means that this sequence never expires.
     */
    public Sequence createOutboudSequence(String sequenceId, long expirationTime) throws DuplicateSequenceException;

    /**
     * Creates a new inbound sequence object
     * 
     * @param sequenceId identifier of the new sequence
     * 
     * @param expirationTime expiration time of the sequence in milliseconds; value of {@link com.sun.xml.ws.rm.policy.Configuration#UNSPECIFIED}
     * means that this sequence never expires.
     */
    public Sequence createInboundSequence(String sequenceId, long expirationTime) throws DuplicateSequenceException;

    /**
     * Retrieves an existing sequence from the internal sequence storage
     * 
     * @param sequenceId the unique sequence identifier
     * 
     * @return sequence identified with the {@code sequenceId} identifier
     * 
     * @exception UnknownSequenceExceptio in case no such sequence is registered within the sequence manager
     */
    public Sequence getSequence(String sequenceId) throws UnknownSequenceException;

    /**
     * Provides information on whether the sequence identifier is a valid identifier that belongs to an existing 
     * sequence registered with the sequence manager.
     * 
     * @param sequenceId sequence identifier to be checked
     * 
     * @return {@code true} in case the sequence identifier is valid, {@code false} otherwise
     */
    public boolean isValid(String sequenceId);
    
    /**
     * Closes an existing sequence. The closed sequence is still kept in the internal sequence storage
     * 
     * @param sequenceId the unique sequence identifier
     */
    public void closeSequence(String sequenceId) throws UnknownSequenceException;
    
    /**
     * Terminates an existing sequence by calling the {@link Sequence#preDestroy()} method. In addition to this, the terminated
     * sequence is removed from the internal sequence storage
     * 
     * @param sequenceId the unique sequence identifier
     */
    public void terminateSequence(String sequenceId) throws UnknownSequenceException;
    
    /**
     * Generates a unique identifier of a sequence
     * 
     * @return new unique sequence identifier which can be used to construct a new sequence.
     */
    public String generateSequenceUID();
}
