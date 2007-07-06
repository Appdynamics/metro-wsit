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

package com.sun.xml.ws.security.impl.policyconv;

import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.security.impl.policy.PolicyUtil;
import com.sun.xml.ws.security.policy.Binding;
import com.sun.xml.ws.security.policy.EncryptedElements;
import com.sun.xml.ws.security.policy.EncryptedParts;
import com.sun.xml.ws.security.policy.SamlToken;
import com.sun.xml.ws.security.policy.SecureConversationToken;
import com.sun.xml.ws.security.policy.SignedElements;
import com.sun.xml.ws.security.policy.SignedParts;
import com.sun.xml.ws.security.policy.SymmetricBinding;
import com.sun.xml.ws.security.policy.Token;
import com.sun.xml.ws.security.policy.X509Token;
import com.sun.xml.wss.impl.MessageConstants;
import com.sun.xml.wss.impl.PolicyTypeUtil;
import com.sun.xml.wss.impl.policy.mls.AuthenticationTokenPolicy;
import com.sun.xml.wss.impl.policy.mls.DerivedTokenKeyBinding;
import com.sun.xml.wss.impl.policy.mls.EncryptionPolicy;
import com.sun.xml.wss.impl.policy.mls.IssuedTokenKeyBinding;
import com.sun.xml.wss.impl.policy.mls.SecureConversationTokenKeyBinding;
import com.sun.xml.wss.impl.policy.mls.SignaturePolicy;
import com.sun.xml.wss.impl.policy.mls.TimestampPolicy;
import com.sun.xml.wss.impl.policy.mls.WSSPolicy;
import java.util.Vector;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import com.sun.xml.ws.security.policy.IssuedToken;
/**
 *
 * @author K.Venugopal@sun.com
 */
public class SymmetricBindingProcessor extends BindingProcessor{
    private SymmetricBinding binding = null;
    /** Creates a new instance of SymmetricBindingProcessor */
    public SymmetricBindingProcessor(SymmetricBinding binding,XWSSPolicyContainer container,
            boolean isServer,boolean isIncoming,Vector<SignedParts> signedParts,Vector<EncryptedParts> encryptedParts,
            Vector<SignedElements> signedElements,Vector<EncryptedElements> encryptedElements) {
        this.binding = binding;
        this.container = container;
        this.isServer = isServer;
        this.isIncoming = isIncoming;
        protectionOrder = binding.getProtectionOrder();
        tokenProcessor = new TokenProcessor(isServer,isIncoming,pid);
        iAP = new IntegrityAssertionProcessor(binding.getAlgorithmSuite(),binding.isSignContent());
        eAP = new EncryptionAssertionProcessor(binding.getAlgorithmSuite(),false);
        this.signedParts = signedParts;
        this.signedElements = signedElements;
        this.encryptedElements = encryptedElements;
        this.encryptedParts = encryptedParts;
    }
    
    
    public void process()throws PolicyException{
        
        Token pt = binding.getProtectionToken();
        Token st = null;
        Token et = null;
        
        if(pt == null ){
            st = binding.getSignatureToken();
            et = binding.getEncryptionToken();
            
            if(et != null){
                primaryEP = new EncryptionPolicy();
                primaryEP.setUUID(pid.generateID());
                addSymmetricKeyBinding(primaryEP,et);
            }
            
            if(st != null){
                primarySP = new SignaturePolicy();
                primarySP.setUUID(pid.generateID());
                
                SignaturePolicy.FeatureBinding spFB = (com.sun.xml.wss.impl.policy.mls.SignaturePolicy.FeatureBinding)
                        primarySP.getFeatureBinding();
                //spFB.setCanonicalizationAlgorithm(CanonicalizationMethod.EXCLUSIVE);
                SecurityPolicyUtil.setCanonicalizationMethod(spFB, binding.getAlgorithmSuite());
                spFB.isPrimarySignature(true);
                addSymmetricKeyBinding(primarySP,st);
            }
        }else{
            primarySP = new SignaturePolicy();
            primarySP.setUUID(pid.generateID());
            primaryEP = new EncryptionPolicy();
            primaryEP.setUUID(pid.generateID());
            addSymmetricKeyBinding(primarySP,pt);
            addSymmetricKeyBinding(primaryEP,pt);
            
            SignaturePolicy.FeatureBinding spFB = (com.sun.xml.wss.impl.policy.mls.SignaturePolicy.FeatureBinding)
                    primarySP.getFeatureBinding();
            //spFB.setCanonicalizationAlgorithm(CanonicalizationMethod.EXCLUSIVE);
            SecurityPolicyUtil.setCanonicalizationMethod(spFB, binding.getAlgorithmSuite());
            spFB.isPrimarySignature(true);
        }
        
        if(protectionOrder == Binding.SIGN_ENCRYPT){
            container.insert(primarySP);
            // container.insert(primaryEP);
        }else{
            container.insert(primaryEP);
            container.insert(primarySP);
            if(primaryEP != null){
                EncryptionPolicy.FeatureBinding efp = (EncryptionPolicy.FeatureBinding) primaryEP.getFeatureBinding();
                efp.setUseStandAloneRefList(true);
            }
            
        }
        addPrimaryTargets();
        
        
        if(foundEncryptTargets && binding.getSignatureProtection()){
            protectPrimarySignature();
        }
        if(binding.isIncludeTimeStamp()){
            TimestampPolicy tp = new TimestampPolicy();
            tp.setUUID(pid.generateID());
            container.insert(tp);
            if(!binding.isDisableTimestampSigning()){
                protectTimestamp(tp);
            }
        }
        if(binding.getTokenProtection()){
            WSSPolicy policy = (WSSPolicy) primarySP.getKeyBinding();
            if(PolicyTypeUtil.derivedTokenKeyBinding(policy)){
                protectToken(policy,true);
            }else{
                protectToken((WSSPolicy) policy.getKeyBinding(),true);
            }
        }
        
    }
    
    protected void addSymmetricKeyBinding(WSSPolicy policy, Token token) throws PolicyException{
        com.sun.xml.wss.impl.policy.mls.SymmetricKeyBinding skb =
                new com.sun.xml.wss.impl.policy.mls.SymmetricKeyBinding();
        //skb.setKeyAlgorithm(_binding.getAlgorithmSuite().getSymmetricKeyAlgorithm());
        // policy.setKeyBinding(skb);
        PolicyAssertion tokenAssertion = (PolicyAssertion)token;
        if(PolicyUtil.isX509Token(tokenAssertion)){
            AuthenticationTokenPolicy.X509CertificateBinding x509CB =new AuthenticationTokenPolicy.X509CertificateBinding();
            //        (AuthenticationTokenPolicy.X509CertificateBinding)policy.newX509CertificateKeyBinding();
            x509CB.setUUID(token.getTokenId());
            tokenProcessor.setTokenValueType(x509CB, tokenAssertion);
            tokenProcessor.setTokenInclusion(x509CB,(Token) tokenAssertion);
            //x509CB.setPolicyToken((Token) tokenAssertion);
            tokenProcessor.setX509TokenRefType(x509CB, (X509Token) token);
            
            if(((X509Token)token).isRequireDerivedKeys()){
                DerivedTokenKeyBinding dtKB =  new DerivedTokenKeyBinding();
                skb.setKeyBinding(x509CB);
                policy.setKeyBinding(dtKB);
                dtKB.setOriginalKeyBinding(skb);
                dtKB.setUUID(pid.generateID());
            }else{
                skb.setKeyBinding(x509CB);
                policy.setKeyBinding(skb);
            }
        }else if(PolicyUtil.isSamlToken(tokenAssertion)){
            AuthenticationTokenPolicy.SAMLAssertionBinding sab = new AuthenticationTokenPolicy.SAMLAssertionBinding();
            sab.setUUID(token.getTokenId());
            sab.setReferenceType(MessageConstants.DIRECT_REFERENCE_TYPE);
            tokenProcessor.setTokenInclusion(sab,(Token) tokenAssertion);
            //sab.setPolicyToken((Token) tokenAssertion);
            if(((SamlToken)token).isRequireDerivedKeys()){
                DerivedTokenKeyBinding dtKB =  new DerivedTokenKeyBinding();
                dtKB.setOriginalKeyBinding(sab);
                policy.setKeyBinding(dtKB);
                dtKB.setUUID(pid.generateID());
            }else{
                policy.setKeyBinding(sab);
            }
        }else if(PolicyUtil.isIssuedToken(tokenAssertion)){
            IssuedTokenKeyBinding itkb = new IssuedTokenKeyBinding();
            tokenProcessor.setTokenInclusion(itkb,(Token) tokenAssertion);
            //itkb.setPolicyToken((Token) tokenAssertion);
            itkb.setUUID(((Token)tokenAssertion).getTokenId());
            IssuedToken it = (IssuedToken)tokenAssertion;
            if(it.isRequireDerivedKeys()){
                DerivedTokenKeyBinding dtKB =  new DerivedTokenKeyBinding();
                dtKB.setOriginalKeyBinding(itkb);
                policy.setKeyBinding(dtKB);
                dtKB.setUUID(pid.generateID());
            }else{
                policy.setKeyBinding(itkb);
            }
        }else if(PolicyUtil.isSecureConversationToken(tokenAssertion)){
            SecureConversationTokenKeyBinding sct = new SecureConversationTokenKeyBinding();
            SecureConversationToken sctPolicy = (SecureConversationToken)tokenAssertion;
            if(sctPolicy.isRequireDerivedKeys()){
                DerivedTokenKeyBinding dtKB =  new DerivedTokenKeyBinding();
                dtKB.setOriginalKeyBinding(sct);
                policy.setKeyBinding(dtKB);
                dtKB.setUUID(pid.generateID());
            }else{
                policy.setKeyBinding(sct);
            }
            tokenProcessor.setTokenInclusion(sct,(Token) tokenAssertion);
            //sct.setPolicyToken((Token) tokenAssertion);
            sct.setUUID(((Token)tokenAssertion).getTokenId());
        }else{
            throw new UnsupportedOperationException("addKeyBinding for "+ token + "is not supported");
        }
    }
    
    protected Binding getBinding(){
        return binding;
    }
    
    protected EncryptionPolicy getSecondaryEncryptionPolicy() throws PolicyException {
        if(sEncPolicy == null){
            sEncPolicy  = new EncryptionPolicy();
            sEncPolicy.setUUID(pid.generateID());
            Token token = null;
            token = binding.getProtectionToken();
            if( token== null){
                token = binding.getEncryptionToken();
            }
            addSymmetricKeyBinding(sEncPolicy,token);
            container.insert(sEncPolicy);
        }
        return sEncPolicy;
    }
    
    protected void close(){
        if(protectionOrder == Binding.SIGN_ENCRYPT){
            container.insert(primaryEP);
        }
    }
}
