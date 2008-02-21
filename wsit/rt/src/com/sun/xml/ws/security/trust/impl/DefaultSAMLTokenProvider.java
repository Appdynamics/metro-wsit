/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.ws.security.trust.impl;

import com.sun.xml.ws.api.security.trust.STSAttributeProvider;
import com.sun.xml.ws.api.security.trust.STSTokenProvider;
import com.sun.xml.ws.api.security.trust.WSTrustException;
import com.sun.xml.ws.security.IssuedTokenContext;
import com.sun.xml.ws.security.trust.GenericToken;
import com.sun.xml.ws.security.trust.WSTrustConstants;
import com.sun.xml.ws.security.trust.util.WSTrustUtil;
import com.sun.xml.ws.security.trust.WSTrustVersion;
import com.sun.xml.ws.security.trust.elements.RequestedAttachedReference;
import com.sun.xml.ws.security.trust.elements.RequestedUnattachedReference;
import com.sun.xml.ws.security.trust.elements.str.SecurityTokenReference;

import com.sun.xml.wss.XWSSecurityException;
import com.sun.org.apache.xml.internal.security.keys.KeyInfo;
import com.sun.xml.wss.impl.MessageConstants;
import com.sun.xml.wss.saml.Advice;
import com.sun.xml.wss.saml.Assertion;
import com.sun.xml.wss.saml.Attribute;
import com.sun.xml.wss.saml.AttributeStatement;
import com.sun.xml.wss.saml.AudienceRestriction;
import com.sun.xml.wss.saml.AudienceRestrictionCondition;
import com.sun.xml.wss.saml.AuthenticationStatement;
import com.sun.xml.wss.saml.AuthnContext;
import com.sun.xml.wss.saml.AuthnStatement;
import com.sun.xml.wss.saml.Conditions;
import com.sun.xml.wss.saml.NameID;
import com.sun.xml.wss.saml.NameIdentifier;
import com.sun.xml.wss.saml.SAMLAssertionFactory;
import com.sun.xml.wss.saml.SAMLException;
import com.sun.xml.wss.saml.SubjectConfirmation;
import com.sun.xml.wss.saml.KeyInfoConfirmationData;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.xml.ws.security.trust.logging.LogDomainConstants;
import com.sun.xml.ws.security.trust.logging.LogStringsMessages;

import java.security.cert.X509Certificate;
import java.util.UUID;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.org.apache.xml.internal.security.keys.KeyInfo;
import com.sun.org.apache.xml.internal.security.encryption.EncryptedKey;
import com.sun.org.apache.xml.internal.security.keys.content.X509Data;


import com.sun.xml.ws.security.trust.WSTrustElementFactory;
import java.security.PrivateKey;

/**
 *
 * @author Jiandong Guo
 */
public class DefaultSAMLTokenProvider implements STSTokenProvider {
    private static final Logger log =
            Logger.getLogger(
            LogDomainConstants.TRUST_IMPL_DOMAIN,
            LogDomainConstants.TRUST_IMPL_DOMAIN_BUNDLE);

    protected static final String SAML_HOLDER_OF_KEY_1_0 = "urn:oasis:names:tc:SAML:1.0:cm:holder-of-key";
    protected static final String SAML_HOLDER_OF_KEY_2_0 = "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key";
    protected static final String SAML_BEARER_1_0 = "urn:oasis:names:tc:SAML:1.0:cm:bearer";
    protected static final String SAML_BEARER_2_0 = "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    protected static final String SAML_SENDER_VOUCHES_1_0 = "urn:oasis:names:tc:SAML:1.0:cm::sender-vouches";
    protected static final String SAML_SENDER_VOUCHES_2_0 = "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches";
    
    public void generateToken(IssuedTokenContext ctx) throws WSTrustException {
           
        String issuer = ctx.getTokenIssuer();
        String appliesTo = ctx.getAppliesTo();
        String tokenType = ctx.getTokenType(); 
        String keyType = ctx.getKeyType();
        int tokenLifeSpan = (int)(ctx.getExpirationTime().getTime() - ctx.getCreationTime().getTime());
        String confirMethod = (String)ctx.getOtherProperties().get(IssuedTokenContext.CONFIRMATION_METHOD);
        Map<QName, List<String>> claimedAttrs = (Map<QName, List<String>>) ctx.getOtherProperties().get(IssuedTokenContext.CLAIMED_ATTRUBUTES);
        WSTrustVersion wstVer = (WSTrustVersion)ctx.getOtherProperties().get(IssuedTokenContext.WS_TRUST_VERSION);
        WSTrustElementFactory eleFac = WSTrustElementFactory.newInstance(wstVer);
        
        // Create the KeyInfo for SubjectConfirmation
        final KeyInfo keyInfo = createKeyInfo(ctx);
        
        // Create AssertionID
        final String assertionId = "uuid-" + UUID.randomUUID().toString();
        
        // Create SAML assertion
        Assertion assertion = null;
        
        if (WSTrustConstants.SAML10_ASSERTION_TOKEN_TYPE.equals(tokenType)||
            WSTrustConstants.SAML11_ASSERTION_TOKEN_TYPE.equals(tokenType)){
            assertion = createSAML11Assertion(wstVer, tokenLifeSpan, confirMethod, assertionId, issuer, appliesTo, keyInfo, claimedAttrs, keyType);
        } else if (WSTrustConstants.SAML20_ASSERTION_TOKEN_TYPE.equals(tokenType)){
            assertion = createSAML20Assertion(wstVer, tokenLifeSpan, confirMethod, assertionId, issuer, appliesTo, keyInfo, claimedAttrs, keyType);
        } else{
            log.log(Level.SEVERE, LogStringsMessages.WST_0031_UNSUPPORTED_TOKEN_TYPE(tokenType, appliesTo));
            throw new WSTrustException(LogStringsMessages.WST_0031_UNSUPPORTED_TOKEN_TYPE(tokenType, appliesTo));
        }
            
        // Get the STS's certificate and private key
        final X509Certificate stsCert = (X509Certificate)ctx.getOtherProperties().get(IssuedTokenContext.STS_CERTIFICATE);
        final PrivateKey stsPrivKey = (PrivateKey)ctx.getOtherProperties().get(IssuedTokenContext.STS_PRIVATE_KEY);
            
        // Sign the assertion with STS's private key
        Element signedAssertion = null;
        try{
            signedAssertion = assertion.sign(stsCert, stsPrivKey, true);
            //signedAssertion = assertion.sign(stsCert, stsPrivKey);
        }catch (SAMLException ex){
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
            throw new WSTrustException(
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
        }
            
        ctx.setSecurityToken(new GenericToken(signedAssertion));
        
        // Create References
        String valueType = null;
        if (WSTrustConstants.SAML10_ASSERTION_TOKEN_TYPE.equals(tokenType)||
            WSTrustConstants.SAML11_ASSERTION_TOKEN_TYPE.equals(tokenType)){
            valueType = MessageConstants.WSSE_SAML_KEY_IDENTIFIER_VALUE_TYPE;
        } else if (WSTrustConstants.SAML20_ASSERTION_TOKEN_TYPE.equals(tokenType)){
            valueType = MessageConstants.WSSE_SAML_v2_0_KEY_IDENTIFIER_VALUE_TYPE;
        }
        final SecurityTokenReference samlReference = WSTrustUtil.createSecurityTokenReference(assertionId, valueType);
        final RequestedAttachedReference raRef =  eleFac.createRequestedAttachedReference(samlReference);
        final RequestedUnattachedReference ruRef =  eleFac.createRequestedUnattachedReference(samlReference);
        ctx.setAttachedSecurityTokenReference(samlReference);
        ctx.setUnAttachedSecurityTokenReference(samlReference);
    }

    public void isValideToken(IssuedTokenContext ctx) throws WSTrustException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void renewToken(IssuedTokenContext ctx) throws WSTrustException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void invalidateToken(IssuedTokenContext ctx) throws WSTrustException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    protected Assertion createSAML11Assertion(final WSTrustVersion wstVer, final int lifeSpan, String confirMethod, final String assertionId, final String issuer, final String appliesTo, final KeyInfo keyInfo, final Map<QName, List<String>> claimedAttrs, String keyType) throws WSTrustException{
        Assertion assertion = null;
        try{
            final SAMLAssertionFactory samlFac = SAMLAssertionFactory.newInstance(SAMLAssertionFactory.SAML1_1);
            
            final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
            final GregorianCalendar issuerInst = new GregorianCalendar(utcTimeZone);
            final GregorianCalendar notOnOrAfter = new GregorianCalendar(utcTimeZone);
            notOnOrAfter.add(Calendar.MILLISECOND, lifeSpan);
            
            List<AudienceRestrictionCondition> arc = null;
            final List<String> confirmMethods = new ArrayList<String>();
            if (confirMethod == null){
                if (keyType.equals(wstVer.getBearerKeyTypeURI())){
                     confirMethod = SAML_BEARER_1_0;
                     if (appliesTo != null){
                         arc = new ArrayList<AudienceRestrictionCondition>();
                         List<String> au = new ArrayList<String>();
                         au.add(appliesTo);
                         arc.add(samlFac.createAudienceRestrictionCondition(au));
                     }
                }else{
                    confirMethod = SAML_HOLDER_OF_KEY_1_0;
                }
            }
            
            Element keyInfoEle = null;
            if (keyInfo != null && !wstVer.getBearerKeyTypeURI().equals(keyType)){
                keyInfoEle = keyInfo.getElement();
            }
            confirmMethods.add(confirMethod);
            
            final SubjectConfirmation subjectConfirm = samlFac.createSubjectConfirmation(
                    confirmMethods, null, keyInfoEle);
            final Conditions conditions =
                    samlFac.createConditions(issuerInst, notOnOrAfter, null, arc, null);
            final Advice advice = samlFac.createAdvice(null, null, null);
            
            com.sun.xml.wss.saml.Subject subj = null;
            final List<Attribute> attrs = new ArrayList<Attribute>();
            final Set<Map.Entry<QName, List<String>>> entries = claimedAttrs.entrySet();
            for(Map.Entry<QName, List<String>> entry : entries){
                final QName attrKey = entry.getKey();
                final List<String> values = entry.getValue();
                if (values != null && values.size() > 0){
                    if (STSAttributeProvider.NAME_IDENTIFIER.equals(attrKey.getLocalPart()) && subj == null){
                        final NameIdentifier nameId = samlFac.createNameIdentifier(values.get(0), attrKey.getNamespaceURI(), null);
                        subj = samlFac.createSubject(nameId, subjectConfirm);
                    }else{
                        final Attribute attr = samlFac.createAttribute(attrKey.getLocalPart(), attrKey.getNamespaceURI(), values);
                        attrs.add(attr);
                    }
                }
            }
            
            final List<Object> statements = new ArrayList<Object>();
            if (attrs.isEmpty()){
                final AuthenticationStatement statement = samlFac.createAuthenticationStatement(null, issuerInst, subj, null, null);
                statements.add(statement); 
            }else{
                final AttributeStatement statement = samlFac.createAttributeStatement(subj, attrs);
                statements.add(statement);
            }
            assertion =
                    samlFac.createAssertion(assertionId, issuer, issuerInst, conditions, advice, statements);
        }catch(SAMLException ex){
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
            throw new WSTrustException(
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
        }catch(XWSSecurityException ex){
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
            throw new WSTrustException(
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
        }
        
        return assertion;
    }
    
    protected Assertion createSAML20Assertion(final WSTrustVersion wstVer, final int lifeSpan, String confirMethod, final String assertionId, final String issuer, final String appliesTo, final KeyInfo keyInfo, final  Map<QName, List<String>> claimedAttrs, String keyType) throws WSTrustException{
        Assertion assertion = null;
        try{
            final SAMLAssertionFactory samlFac = SAMLAssertionFactory.newInstance(SAMLAssertionFactory.SAML2_0);
            
            // Create Conditions
            final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
            final GregorianCalendar issueInst = new GregorianCalendar(utcTimeZone);
            final GregorianCalendar notOnOrAfter = new GregorianCalendar(utcTimeZone);
            notOnOrAfter.add(Calendar.MILLISECOND, lifeSpan);
            
            List<AudienceRestriction> arc = null;
            KeyInfoConfirmationData keyInfoConfData = null;
            if (confirMethod == null){
                if (keyType.equals(wstVer.getBearerKeyTypeURI())){
                     confirMethod = SAML_BEARER_2_0;
                     if (appliesTo != null){
                         arc = new ArrayList<AudienceRestriction>();
                         List<String> au = new ArrayList<String>();
                         au.add(appliesTo);
                         arc.add(samlFac.createAudienceRestriction(au));
                     }

                }else{
                    confirMethod = SAML_HOLDER_OF_KEY_2_0;
                    if (keyInfo != null){
                        keyInfoConfData = samlFac.createKeyInfoConfirmationData(keyInfo.getElement());
                    }
                }
            }
            
            final Conditions conditions = samlFac.createConditions(issueInst, notOnOrAfter, null, arc, null, null);
               
            final SubjectConfirmation subjectConfirm = samlFac.createSubjectConfirmation(
                    null, keyInfoConfData, confirMethod);
            
            com.sun.xml.wss.saml.Subject subj = null;
            final List<Attribute> attrs = new ArrayList<Attribute>();
            final Set<Map.Entry<QName, List<String>>> entries = claimedAttrs.entrySet();
            for(Map.Entry<QName, List<String>> entry : entries){
                final QName attrKey = entry.getKey();
                final List<String> values = entry.getValue();
                if (values != null && values.size() > 0){
                    if (STSAttributeProvider.NAME_IDENTIFIER.equals(attrKey.getLocalPart()) && subj == null){
                        final NameID nameId = samlFac.createNameID(values.get(0), attrKey.getNamespaceURI(), null);
                        subj = samlFac.createSubject(nameId, subjectConfirm);
                    }
                    else{
                        final Attribute attr = samlFac.createAttribute(attrKey.getLocalPart(), values);
                        attrs.add(attr);
                    }
                }
            }
        
            final List<Object> statements = new ArrayList<Object>();
            if (attrs.isEmpty()){
                // To Do: create AuthnContext with proper content. Currently what 
                // we have is a place holder.
                AuthnContext ctx = samlFac.createAuthnContext();
                final AuthnStatement statement = samlFac.createAuthnStatement(issueInst, null, ctx);
                statements.add(statement); 
            }else{
                final AttributeStatement statement = samlFac.createAttributeStatement(attrs);
                statements.add(statement);
            }
            
            final NameID issuerID = samlFac.createNameID(issuer, null, null);
            
            // Create Assertion
            assertion =
                    samlFac.createAssertion(assertionId, issuerID, issueInst, conditions, null, subj, statements);
        }catch(SAMLException ex){
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
            throw new WSTrustException(
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
        }catch(XWSSecurityException ex){
            log.log(Level.SEVERE,
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
            throw new WSTrustException(
                    LogStringsMessages.WST_0032_ERROR_CREATING_SAML_ASSERTION(), ex);
        }
        
        return assertion;
    }
     
    private KeyInfo createKeyInfo(final IssuedTokenContext ctx)throws WSTrustException{
        Element kiEle = (Element)ctx.getOtherProperties().get("ConfirmationKeyInfo");
        if (kiEle != null){
            try{
                return new KeyInfo(kiEle, null);
            }catch(com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException ex){
                log.log(Level.SEVERE, LogStringsMessages.WST_0034_UNABLE_GET_CLIENT_CERT(), ex);
                throw new WSTrustException(LogStringsMessages.WST_0034_UNABLE_GET_CLIENT_CERT(), ex);
            }
        }
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        Document doc = null;
        try{
            doc = docFactory.newDocumentBuilder().newDocument();
        }catch(ParserConfigurationException ex){
            log.log(Level.SEVERE, 
                    LogStringsMessages.WST_0039_ERROR_CREATING_DOCFACTORY(), ex);
            throw new WSTrustException(LogStringsMessages.WST_0039_ERROR_CREATING_DOCFACTORY(), ex);
        }
        
        final String appliesTo = ctx.getAppliesTo();
        final KeyInfo keyInfo = new KeyInfo(doc);
        String keyType = ctx.getKeyType();
        WSTrustVersion wstVer = (WSTrustVersion)ctx.getOtherProperties().get(IssuedTokenContext.WS_TRUST_VERSION);
        if (wstVer.getSymmetricKeyTypeURI().equals(keyType)){
            final byte[] key = ctx.getProofKey();
            try{
                final EncryptedKey encKey = WSTrustUtil.encryptKey(doc, key, (X509Certificate)ctx.getOtherProperties().get(IssuedTokenContext.TARGET_SERVICE_CERTIFICATE));
                 keyInfo.add(encKey);
            } catch (Exception ex) {
                 log.log(Level.SEVERE,
                            LogStringsMessages.WST_0040_ERROR_ENCRYPT_PROOFKEY(appliesTo), ex);
                 throw new WSTrustException(LogStringsMessages.WST_0040_ERROR_ENCRYPT_PROOFKEY(appliesTo), ex);
            }
        }else if(wstVer.getPublicKeyTypeURI().equals(keyType)){
            final X509Data x509data = new X509Data(doc);
            try{
                x509data.addCertificate(ctx.getRequestorCertificate());
            }catch(com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException ex){
                log.log(Level.SEVERE, LogStringsMessages.WST_0034_UNABLE_GET_CLIENT_CERT(), ex);
                throw new WSTrustException(LogStringsMessages.WST_0034_UNABLE_GET_CLIENT_CERT(), ex);
            }
            keyInfo.add(x509data);
        }
        
        return keyInfo;
    }
}
