/*
 * $Id: EncryptedKeyHeaderBlock.java,v 1.1 2006-05-03 22:57:31 arungupta Exp $
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

package com.sun.xml.wss.core;

import com.sun.xml.wss.impl.MessageConstants;
import com.sun.xml.wss.impl.XMLUtil;
import com.sun.xml.wss.XWSSecurityException;
import java.util.Iterator;
import java.util.logging.Level;

import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import org.w3c.dom.Document;

import com.sun.xml.wss.impl.misc.SecurityHeaderBlockImpl;

/**
 * The schema definition of EncryptedKey element is as follows:
 * <xmp>
 * <element name='EncryptedKey' type='xenc:EncryptedKeyType'/>
 * <complexType name='EncryptedKeyType'>
 *     <complexContent>
 *         <extension base='xenc:EncryptedType'>
 *             <sequence>
 *                 <element ref='xenc:ReferenceList' minOccurs='0'/>
 *                 <element name='CarriedKeyName' type='string' minOccurs='0'/>
 *             </sequence>
 *             <attribute name='Recipient' type='string' use='optional'/>
 *         </extension>
 *     </complexContent>
 * </complexType>
 * </xmp>
 *
 * @author Vishal Mahajan
 */
public class EncryptedKeyHeaderBlock extends EncryptedTypeHeaderBlock {

    ReferenceListHeaderBlock referenceList;

    SOAPElement carriedKeyName;

    /**
     * Create an empty EncryptedKey element.
     *
     * @throws XWSSecurityException
     *     If there is problem creating an EncryptedKey element.
     */
    public EncryptedKeyHeaderBlock() throws XWSSecurityException {
        try {
            setSOAPElement(
                getSoapFactory().createElement(
                    MessageConstants.XENC_ENCRYPTED_KEY_LNAME,
                    MessageConstants.XENC_PREFIX,
                    MessageConstants.XENC_NS));
            addNamespaceDeclaration(
                MessageConstants.XENC_PREFIX,
                MessageConstants.XENC_NS);
        } catch (SOAPException e) {
            log.log(Level.SEVERE, "WSS0348.error.creating.ekhb", e.getMessage()); 
            throw new XWSSecurityException(e);
        }
    }

    /**
     * Create an empty EncryptedKey element whose owner document is given.
     *
     * @throws XWSSecurityException
     *     If there is problem creating an EncryptedKey element
     */
    public EncryptedKeyHeaderBlock(Document doc) throws XWSSecurityException {
        try {
            setSOAPElement(
                (SOAPElement) doc.createElementNS(
                    MessageConstants.XENC_NS,
                    MessageConstants.XENC_ENCRYPTED_KEY_QNAME));
            addNamespaceDeclaration(
                MessageConstants.XENC_PREFIX,
                MessageConstants.XENC_NS);
        } catch (Exception e) {
            log.log(Level.SEVERE, "WSS0348.error.creating.ekhb", e.getMessage());
            throw new XWSSecurityException(e);
        }
    }

    /**
     * @throws XWSSecurityException
     *     If there is problem in initializing EncryptedKey element.
     */
    public EncryptedKeyHeaderBlock(SOAPElement element)
        throws XWSSecurityException {

        try {

            setSOAPElement(element);

            if (!(element.getLocalName().equals(
                      MessageConstants.XENC_ENCRYPTED_KEY_LNAME) &&
                  XMLUtil.inEncryptionNS(element))) {
                log.log(Level.SEVERE, "WSS0349.error.creating.ekhb", element.getTagName());  
                throw new XWSSecurityException("Invalid EncryptedKey passed");
            }

            initializeEncryptedType(element);

            Iterator referenceLists =
                getChildElements(
                    getSoapFactory().createName(
                        "ReferenceList",
                        MessageConstants.XENC_PREFIX,
                        MessageConstants.XENC_NS));
            if (referenceLists.hasNext())
                this.referenceList =
                    new ReferenceListHeaderBlock(
                        (SOAPElement) referenceLists.next());

        
            Iterator carriedKeyNames =
                getChildElements(
                    getSoapFactory().createName(
                        "CarriedKeyName",
                        MessageConstants.XENC_PREFIX,
                        MessageConstants.XENC_NS));
            if (carriedKeyNames.hasNext())
                this.carriedKeyName = (SOAPElement) carriedKeyNames.next(); 

        } catch (Exception e) {
            log.log(Level.SEVERE, "WSS0348.error.creating.ekhb", e.getMessage());
            throw new XWSSecurityException(e);
        }
    }

    public void setCipherData(SOAPElement cipherData) {
        this.cipherData = cipherData;
        updateRequired = true;
    }

    public void setCipherValue(String cipherValue)
        throws XWSSecurityException {

        try {
            cipherData =
                getSoapFactory().createElement(
                    "CipherData",
                    MessageConstants.XENC_PREFIX,
                    MessageConstants.XENC_NS);
            cipherData.addNamespaceDeclaration(
                MessageConstants.XENC_PREFIX,
                MessageConstants.XENC_NS);

            cipherData.addTextNode("\n    ");
            SOAPElement cipherValueElement =
                cipherData.addChildElement(
                    "CipherValue", MessageConstants.XENC_PREFIX);
            cipherData.addTextNode("\n    ");

            cipherValueElement.addTextNode(cipherValue);

            cipherData.removeNamespaceDeclaration(
                MessageConstants.XENC_PREFIX);
        } catch (SOAPException e) {
            log.log(Level.SEVERE, "WSS0350.error.setting.ciphervalue", e.getMessage());
            throw new XWSSecurityException(e);
        }
        updateRequired = true;
    }

    public ReferenceListHeaderBlock getReferenceList() {
        return referenceList;
    }

    public void setReferenceList(ReferenceListHeaderBlock referenceList) {
        this.referenceList = referenceList;
        updateRequired = true;
    }

    /**
     * Returns null if Recipient attr is not present
     */
    public String getRecipient() {
        String recipient = getAttribute("Recipient");
        if(recipient.equals(""))
            return null;
        return recipient;
    }

    public void setRecipient(String recipient) {
        setAttribute("Recipient", recipient);
    }

    public SOAPElement getCarriedKeyName() {
        return carriedKeyName;
    }

    public void setCarriedKeyName(SOAPElement carriedKeyName) {
        this.carriedKeyName = carriedKeyName;
        updateRequired = true;
    }

    public static SecurityHeaderBlock fromSoapElement(SOAPElement element)
        throws XWSSecurityException {
        return SecurityHeaderBlockImpl.fromSoapElement(
            element, EncryptedKeyHeaderBlock.class);
    }

    public SOAPElement getAsSoapElement() throws XWSSecurityException {
        if (updateRequired) {
            removeContents();
            try {
                addTextNode("\n    ");
                if (encryptionMethod != null) {
                    addChildElement(encryptionMethod);
                    addTextNode("\n    ");
                }
                if (keyInfo != null) {
                    addChildElement(keyInfo.getAsSoapElement());
                    addTextNode("\n    ");
                }
                if (cipherData == null) {
                    log.log(Level.SEVERE, 
                            "WSS0347.missing.cipher.data");
                    throw new XWSSecurityException(
                        "CipherData is not present inside EncryptedType");
                }
                addChildElement(cipherData);
                addTextNode("\n    ");
                if (encryptionProperties != null) {
                    addChildElement(encryptionProperties);
                    addTextNode("\n    ");
                }
                if (referenceList != null) {
                    addChildElement(referenceList.getAsSoapElement());
                    addTextNode("\n    ");
                }
                if (carriedKeyName != null) {
                    addChildElement(carriedKeyName);
                    addTextNode("\n    ");
                }
            } catch (SOAPException e) {
                log.log(Level.SEVERE, "WSS0348.error.creating.ekhb", e.getMessage());
                throw new XWSSecurityException(e);
            }
        }
        return super.getAsSoapElement();
    }
}
