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
 * SequenceElement.java
 *
 * @author Mike Grogan
 * Created on October 23, 2005, 9:34 AM
 *
 */

package com.sun.xml.ws.rm.protocol;


import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SequenceElement is based on a JAXB Schema Compiler generated class that serializes
 * and deserialized the <code>SequenceType</code> defined in the WS-RM schema.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SequenceType")
@XmlRootElement(name = "Sequence",namespace="http://schemas.xmlsoap.org/ws/2005/02/rm")
public class SequenceElement   {


    @XmlElement(name = "Identifier", namespace = "http://schemas.xmlsoap.org/ws/2005/02/rm")
    protected Identifier identifier;

    @XmlElement(name = "MessageNumber", namespace = "http://schemas.xmlsoap.org/ws/2005/02/rm")
    protected Integer messageNumber;

    @XmlElement(name = "LastMessage", namespace = "http://schemas.xmlsoap.org/ws/2005/02/rm")
    protected LastMessage lastMessage;

    @XmlAnyElement(lax = true)
    protected List<Object> any;

    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();


    public SequenceElement(){
        
    }


    public String getLocalPart(){
        return  "Sequence";
    }


    /**
     * Mutator for the Id property.  Maps to the Identifier property in the underlying
     * JAXB class.
     * 
     * @param id The new value.
     */
    public void setId(String id) {
        Identifier identifier = new Identifier();
        identifier.setValue(id);
        setIdentifier(identifier);
    }

    /**
     * Accessor for the Id property.  Maps to the Identifier property in the underlying
     * JAXB class
     * @return The sequence id
     */
    public String getId() {
        return getIdentifier().getValue();
    }

    /**
     * Mutator for the Number property which maps to the MessageNumber property in
     * the underlying JAXB class.
     * 
     * @param l The Message number.
     */
    public void setNumber(int l) {
        setMessageNumber(l);
    }

    /**
     * Accessor for the Number property which maps to the MessageNumber property in
     * the underlying JAXB class.
     * 
     * @return The Message number.
     */
    public int getNumber() {
        return getMessageNumber();
    }

    /**
     * Mutator for the Last property that maps to the LastMessage property in the
     * underlying JAXB class
     *
     * @param last The value of the property.
     */
    public void setLast(boolean  last) {
        if (last) {
            setLastMessage(new LastMessage());
        } else {
            setLastMessage(null);
        }
    }


    /**
     * Accessor for the Last property that maps to the LastMessage property in the
     * underlying JAXB class
     *
     * @return The value of the property.
     */
    public boolean getLast() {
        return getLastMessage() != null;
    }

    /**
     * Gets the value of the identifier property.
     * 
     * @return The property value
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the identifier property.
     * 
     * @param value The new value.
     */
    public void setIdentifier(Identifier value) {
        this.identifier = value;
    }

    /**
     * Gets the value of the messageNumber property.
     * 
     * @return The value of the property.
     *     
     */
    public Integer getMessageNumber() {
        return messageNumber;
    }

    /**
     * Sets the value of the messageNumber property.
     * 
     * @param value The new value.
     *     
     */
    public void setMessageNumber(Integer value) {
        this.messageNumber = value;
    }

    /**
     * Gets the value of the lastMessage property.
     * 
     * @return The value of the property
     *          non-null indicates that a Last child will be serialized on
     *          the Sequence element.
     *     
     */
    public LastMessage getLastMessage() {
        return lastMessage;
    }

    /**
     * Sets the value of the lastMessage property.
     * 
     * @param value The new value.  Either null or a member
     * of the placeholder inner LastMessage class.
     *  
     *     
     */
    public void setLastMessage(LastMessage value) {
        this.lastMessage = value;
    }

    /**
     * Gets the value of the any property.
     * 
     * @return The value of the property.
     * 
     * 
     */
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * @return The map of attributes.
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }


    public String toString() {
        String ret =  "SequenceElement:\n";
               ret += "\tid = " + getId() + "\n";
               ret += "\tnumber = " + getNumber() + "\n";
               ret += "\tlast = ";
        if (getLast()) {
            ret += "true\n";
        } else {
            ret += "false\n";
        }
        return ret;
    }
    
    /**
     * <p>Java class for anonymous complex type.  That acts as a
     * placeholder in the <code>lastMessage</code> field.
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class LastMessage {
    }

}

