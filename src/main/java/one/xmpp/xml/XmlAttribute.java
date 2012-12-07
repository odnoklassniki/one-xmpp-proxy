/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package one.xmpp.xml;

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import one.xmpp.utils.CharSequenceUtils;

public class XmlAttribute implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pregeneraged = null;

    private QName qName;

    private CharSequence value;

    public XmlAttribute(final QName qName, final CharSequence value) {
        if (qName == null) {
            throw new NullArgumentException("qName");
        }
        if (value == null) {
            throw new NullArgumentException("value");
        }

        this.qName = qName;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XmlAttribute other = (XmlAttribute) obj;

        if (qName == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;

        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!StringUtils.equals(String.valueOf(value), String.valueOf(other.value)))
            return false;

        return true;
    }

    public String getLocalPart() {
        return qName.getLocalPart();
    }

    public String getNamespaceURI() {
        return qName.getNamespaceURI();
    }

    public String getPrefix() {
        return qName.getPrefix();
    }

    public QName getQName() {
        return qName;
    }

    public CharSequence getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((qName == null) ? 0 : qName.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    void pregenerate() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("='");
        CharSequenceUtils.escapeXml(stringBuilder, getValue(), true);
        stringBuilder.append('\'');
        this.pregeneraged = stringBuilder.toString();
    }

    void setValue(CharSequence value) {
        if (pregeneraged != null) {
            throw new IllegalArgumentException("This is read-only attribute");
        }

        this.value = value;
    }

    void toXml(StringBuilder stringBuilder, NamespaceResolver namespaceResolver) {
        stringBuilder.append(' ');
        final QName attributeName = getQName();
        XmlElement.appendQName(stringBuilder, namespaceResolver, attributeName);

        if (pregeneraged != null) {
            stringBuilder.append(pregeneraged);
            return;
        }

        stringBuilder.append("='");
        CharSequenceUtils.escapeXml(stringBuilder, getValue(), true);
        stringBuilder.append('\'');
    }

}