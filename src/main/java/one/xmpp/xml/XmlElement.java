package one.xmpp.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import one.xmpp.server.XmppConstants;
import one.xmpp.utils.CharSequenceUtils;

public class XmlElement implements Serializable {

    private static final long serialVersionUID = 1L;

    private static void appendNamespaceDefinition(StringBuilder stringBuilder, final String prefix,
            final String namespaceUri) {
        stringBuilder.append(' ');
        stringBuilder.append(XMLConstants.XMLNS_ATTRIBUTE);
        if (!StringUtils.isEmpty(prefix)) {
            stringBuilder.append(':');
            stringBuilder.append(prefix);
        }
        stringBuilder.append("='");
        CharSequenceUtils.escapeXml(stringBuilder, namespaceUri, true);
        stringBuilder.append('\'');
    }

    static void appendQName(StringBuilder stringBuilder, NamespaceResolver namespaceResolver, final QName qName) {
        final String prefix = namespaceResolver.resolvePrefix(qName.getNamespaceURI());
        if (!StringUtils.isEmpty(prefix)) {
            stringBuilder.append(prefix);
            stringBuilder.append(':');
        }
        stringBuilder.append(qName.getLocalPart());
    }

    private List<XmlAttribute> attributes = null;

    private List<XmlElement> children = null;

    /* Not using thread sync, becase all pregenerations must be done on startup time */
    private String pregenerated = null;

    private final QName qName;

    private CharSequence value = null;

    public XmlElement(QName qName) {
        this.qName = qName;
        this.attributes = null;
    }

    public XmlElement(QName qName, CharSequence value) {
        this.qName = qName;
        this.attributes = Collections.<XmlAttribute> emptyList();
        this.children = null;
        this.value = value;
    }

    public XmlElement(QName qName, List<XmlAttribute> attributes) {
        this.qName = qName;
        this.attributes = attributes;
    }

    public XmlElement(QName qName, XmlAttribute... attributes) {
        this.qName = qName;

        if (attributes != null && attributes.length > 0) {
            this.attributes = new ArrayList<XmlAttribute>(Arrays.asList(attributes));
        } else {
            this.attributes = null;
        }
    }

    public XmlElement(QName qName, XmlElement... children) {
        this.qName = qName;
        this.attributes = null;

        if (children != null && children.length > 0) {
            this.children = new ArrayList<XmlElement>(Arrays.asList(children));
        } else {
            this.children = null;
        }
    }

    public void appendCharacters(CharSequence charSequence) {
        if (pregenerated != null) {
            throw new IllegalStateException("Element is read-only");
        }

        if (value == null) {
            value = charSequence;
            return;
        }

        if (value instanceof StringBuilder) {
            ((StringBuilder) value).append(charSequence);
        } else {
            StringBuilder newValue = new StringBuilder(value.length() + charSequence.length());
            newValue.append(value);
            newValue.append(charSequence);
            this.value = newValue;
        }
    }

    public void appendChild(XmlElement child) {
        if (pregenerated != null) {
            throw new IllegalStateException("Element is read-only");
        }

        if (children == null) {
            children = new LinkedList<XmlElement>();
        }
        children.add(child);
    }

    public String getAttributeValue(QName qName) {
        if (attributes == null) {
            return null;
        }

        for (XmlAttribute xmlAttribute : attributes) {
            if (qName.equals(xmlAttribute.getQName())) {
                final CharSequence value = xmlAttribute.getValue();
                if (value == null) {
                    return null;
                }
                if (value instanceof String) {
                    return (String) value;
                }
                // to prevent recreate later
                final String string = value.toString();
                xmlAttribute.setValue(string);
                return string;
            }
        }
        return null;
    }

    public XmlElement getChild() {
        if (children == null || children.isEmpty())
            return null;

        return children.get(0);
    }

    public XmlElement getChild(QName qName) {
        if (children == null) {
            return null;
        }

        for (XmlElement xmlElement : children) {
            if (qName.equals(xmlElement.getQName())) {
                return xmlElement;
            }
        }
        return null;
    }

    public XmlElement getChildByNamespace(String namespaceUri) {
        if (children == null) {
            return null;
        }

        for (XmlElement xmlElement : children) {
            if (namespaceUri.equals(xmlElement.getQName().getNamespaceURI())) {
                return xmlElement;
            }
        }
        return null;
    }

    public List<XmlElement> getChildren() {
        return children;
    }

    public List<XmlElement> getChildrenByQName(QName childName) {
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }

        List<XmlElement> result = new ArrayList<XmlElement>(children.size());
        for (XmlElement child : children) {
            if (childName.equals(child.getQName())) {
                result.add(child);
            }
        }
        return result;
    }

    public QName getQName() {
        return qName;
    }

    public String getValue() {
        return value != null ? value.toString() : null;
    }

    public boolean hasValue() {
        return value != null && value.length() != 0;
    }

    /**
     * Pregenerates XML string. Must be used ONLY when element is top one among all elements with
     * same namespace.
     */
    void pregenerate() {
        this.pregenerated = this.toXml().toString();
    }

    public void setAttributeValue(QName qName, String value) {
        if (attributes == null) {
            this.attributes = new ArrayList<XmlAttribute>(1);
        }

        this.attributes.add(new XmlAttribute(qName, value));
    }

    public CharSequence toXml() {

        if (pregenerated != null) {
            return pregenerated;
        }

        NamespaceResolver parserNamespaceResolver = new NamespaceResolver();

        // defined in stream:stream opening element
        Map<String, String> streamDefaultNamespaces = new TreeMap<String, String>();
        streamDefaultNamespaces.put(XmppConstants.Prefixes.CLIENT, XmppConstants.Namespaces.CLIENT);
        streamDefaultNamespaces.put(XmppConstants.Prefixes.STREAM, XmppConstants.Namespaces.STREAM);
        parserNamespaceResolver.push(streamDefaultNamespaces);

        StringBuilder stringBuilder = new StringBuilder();
        this.toXml(stringBuilder, parserNamespaceResolver);
        return stringBuilder;
    }

    protected void toXml(StringBuilder stringBuilder, NamespaceResolver namespaceResolver) {

        if (pregenerated != null) {
            // if we have pregenerated string -- use it
            stringBuilder.append(pregenerated);
            return;
        }

        Map<String, String> newlyDefined = new TreeMap<String, String>();

        {
            final String elementPrefix = qName.getPrefix();
            if (!StringUtils.equals(qName.getNamespaceURI(), namespaceResolver.resolveUri(elementPrefix))) {
                newlyDefined.put(elementPrefix, qName.getNamespaceURI());
            }
        }

        if (attributes != null && !attributes.isEmpty()) {
            for (XmlAttribute xmlAttribute : attributes) {
                final String attributePrefix = xmlAttribute.getPrefix();
                if (!StringUtils.isEmpty(attributePrefix)
                        && !newlyDefined.containsKey(attributePrefix)
                        && !StringUtils.equals(xmlAttribute.getNamespaceURI(),
                                namespaceResolver.resolveUri(attributePrefix))) {
                    newlyDefined.put(attributePrefix, xmlAttribute.getNamespaceURI());
                }
            }
        }

        final boolean updateNamespacesDefinition = !newlyDefined.isEmpty();
        if (updateNamespacesDefinition) {
            namespaceResolver.push(newlyDefined);
        }
        try {
            stringBuilder.append('<');
            appendQName(stringBuilder, namespaceResolver, qName);

            for (Map.Entry<String, String> entry : newlyDefined.entrySet()) {
                appendNamespaceDefinition(stringBuilder, entry.getKey(), entry.getValue());
            }

            if (attributes != null && !attributes.isEmpty()) {
                for (XmlAttribute xmlAttribute : attributes) {
                    xmlAttribute.toXml(stringBuilder, namespaceResolver);
                }
            }

            final boolean hasChildren = children != null && !children.isEmpty();
            final boolean hasValue = value != null;
            if (hasChildren || hasValue) {
                stringBuilder.append('>');

                boolean ignoreValue = false;

                if (hasChildren && hasValue) {
                    if (StringUtils.trimToNull(value.toString()) == null) {
                        ignoreValue = true;
                    } else {
                        throw new UnsupportedOperationException("Can't output element with both value and children");
                    }
                }

                if (hasChildren) {
                    for (XmlElement child : children) {
                        child.toXml(stringBuilder, namespaceResolver);
                    }
                }

                if (hasValue && !ignoreValue) {
                    CharSequenceUtils.escapeXml(stringBuilder, value, false);
                }

                stringBuilder.append("</");
                appendQName(stringBuilder, namespaceResolver, qName);
                stringBuilder.append('>');
            } else {
                stringBuilder.append("/>");
            }

        } finally {
            if (updateNamespacesDefinition) {
                namespaceResolver.pop();
            }
        }
    }

    public CharSequence toXmlSafe() {
        try {
            return toXml();
        } catch (Exception exc) {
            return "<<XML serialization error>>";
        }
    }
}
