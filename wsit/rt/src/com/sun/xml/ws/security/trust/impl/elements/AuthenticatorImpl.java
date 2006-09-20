/*
 * $Id: AuthenticatorImpl.java,v 1.2 2006-09-20 23:58:47 manveen Exp $
 */

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

package com.sun.xml.ws.security.trust.impl.elements;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.sun.xml.ws.security.trust.elements.Authenticator;
import com.sun.xml.ws.security.trust.impl.bindings.AuthenticatorType;

import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.xml.ws.security.trust.logging.LogDomainConstants;

/**
 * Provides verification (authentication) of a computed hash.
 *
 * @author Manveen Kaur
 */

public class AuthenticatorImpl extends AuthenticatorType implements Authenticator {

    private static Logger log =
            Logger.getLogger(
            LogDomainConstants.TRUST_IMPL_DOMAIN,
            LogDomainConstants.TRUST_IMPL_DOMAIN_BUNDLE);

    public AuthenticatorImpl() {
        // empty constructor
    }
    
    public AuthenticatorImpl(AuthenticatorType aType) throws Exception{
        //ToDo
    }
    
    public AuthenticatorImpl(byte[] hash) {
        setRawCombinedHash(hash);
    }
    
    public List<Object> getAny() {
        return super.getAny();
    }
    
    public byte[] getRawCombinedHash() {
        return getCombinedHash();
    }
    
    public void setRawCombinedHash(byte[] rawCombinedHash) {
        setCombinedHash(rawCombinedHash);
    }
    
    public String getTextCombinedHash() {
        return Base64.encode(getRawCombinedHash());
    }
    
    public void setTextCombinedHash(String encodedCombinedHash) {
        try {
            setRawCombinedHash(Base64.decode(encodedCombinedHash));
        } catch (Base64DecodingException de) {
            log.log(Level.SEVERE,"WST0020.error.decoding", new Object[] {encodedCombinedHash});
            throw new RuntimeException("Error while decoding combineHash" +
                    de.getMessage());
        }
    }
    
}
