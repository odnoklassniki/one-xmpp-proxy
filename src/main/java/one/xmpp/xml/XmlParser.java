package one.xmpp.xml;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import one.xmpp.server.errors.stream.BadFormatError;
import one.xmpp.server.errors.stream.RestrictedXmlError;
import one.xmpp.server.errors.stream.XmlNotWellFormedError;
import one.xmpp.utils.CharSequenceUtils;
import one.xmpp.xml.XmlTokenizer.TokenListener;

/**
 * Based on original version of XmlParser from the Apache MINA Project (dev@mina.apache.org)
 */
public class XmlParser implements TokenListener {

    private static final char[] CHARS_NAME_BEGIN;

    private static final char[] CHARS_NAME_CONTINUE;

    static {
        List<Character> list = new ArrayList<Character>();
        list.add(Character.valueOf(':'));
        addToCharsList(list, 'A', 'Z');
        list.add(Character.valueOf('_'));
        addToCharsList(list, 'a', 'z');
        addToCharsList(list, '\u00C0', '\u00D6');
        addToCharsList(list, '\u00D8', '\u00F6');
        addToCharsList(list, '\u00F8', '\u02FF');
        addToCharsList(list, '\u0370', '\u037D');
        addToCharsList(list, '\u037F', '\u1FFF');
        addToCharsList(list, '\u200C', '\u200D');
        addToCharsList(list, '\u2070', '\u218F');
        addToCharsList(list, '\u2C00', '\u2FEF');
        addToCharsList(list, '\u3001', '\uD7FF');
        addToCharsList(list, '\uF900', '\uFDCF');
        addToCharsList(list, '\uFDF0', '\uFFFD');
        Collections.sort(list);
        CHARS_NAME_BEGIN = ArrayUtils.toPrimitive(list.toArray(new Character[list.size()]));

        list.add(Character.valueOf('-'));
        addToCharsList(list, '0', '9');
        list.add(Character.valueOf('\u00B7'));
        addToCharsList(list, '\u0300', '\u036F');
        addToCharsList(list, '\u203F', '\u2040');
        Collections.sort(list);
        CHARS_NAME_CONTINUE = ArrayUtils.toPrimitive(list.toArray(new Character[list.size()]));
    }

    private static void addToCharsList(List<Character> list, final char begin, final char end) {
        for (char c = begin; c <= end; c++) {
            list.add(Character.valueOf(c));
        }
    }

    private static String extractLocalName(CharSequence qname) {
        return CharSequenceUtils.substringAfter(qname, ':').toString();
    }

    private static String extractNsPrefix(CharSequence qname) {
        return CharSequenceUtils.substringBefore(qname, ':').toString();
    }

    private static boolean isValidName(CharSequence name) {
        final int length = name.length();

        // element names must only contain valid characters
        if (length == 0) {
            return false;
        }

        final char char0 = name.charAt(0);
        if (Arrays.binarySearch(CHARS_NAME_BEGIN, char0) < 0) {
            return false;
        }

        for (int i = 1; i < length; i++) {
            if (Arrays.binarySearch(CHARS_NAME_CONTINUE, name.charAt(i)) < 0) {
                return false;
            }
        }

        // element names must not begin with "xml" in any casing
        if (length >= 3) {
            final char char1 = name.charAt(1);
            final char char2 = name.charAt(2);

            if ((char0 == 'x' || char0 == 'X') && (char1 == 'm' || char1 == 'M') && (char2 == 'l' || char2 == 'L')) {
                return false;
            }
        }

        return true;
    }

    private CharSequence attributeName;

    // qname/value map
    private Map<CharSequence, CharSequence> attributes;

    private Stack<QName> elements = new Stack<QName>();

    private XmlParsingErrorHandler errorHandler;

    private Log log = LogFactory.getLog(XmlParser.class);

    private NamespaceResolver nsResolver = new NamespaceResolver();

    private CharSequence qname;

    private boolean sentStartDocument = false;

    private State state = State.START;

    private XmlTokenizer tokenizer;

    private XmlConstructor xmlConstructor;

    public XmlParser(XmlConstructor xmlConstructor, XmlParsingErrorHandler errorHandler) {
        this.xmlConstructor = xmlConstructor;
        this.errorHandler = errorHandler;

        this.tokenizer = new XmlTokenizer(this);
    }

    private void characters(CharSequence s) throws XmlNotWellFormedError {
        // text only allowed in element
        if (!elements.isEmpty()) {
            CharSequence unescaped;
            try {
                unescaped = CharSequenceUtils.unescapeXml(s);
            } catch (IllegalArgumentException exc) {
                throw new XmlNotWellFormedError("Unable to unescape XML chars sequence (escaped): '"
                        + StringEscapeUtils.escapeJava("" + s) + "'", exc);
            }

            if (log.isTraceEnabled()) {
                log.trace("Parser emitting characters \"" + unescaped + "\"");
            }

            xmlConstructor.characters(unescaped);
        } else if (!CharSequenceUtils.isEmptyTrimmed(s)) {
            // must start document, even that document is not wellformed
            startDocument();
            throw new XmlNotWellFormedError("Text only allowed in element");
        } else {
            // ignorable whitespace
            startDocument();
        }
    }

    private void endElement() throws XmlNotWellFormedError {
        if (log.isTraceEnabled()) {
            log.trace("EndElement " + qname);
        }

        if (state == State.CLOSED)
            return;

        String prefix = extractNsPrefix(qname);
        String uri = nsResolver.resolveUri(prefix);
        if (uri == null) {
            if (prefix.length() > 0) {
                throw new XmlNotWellFormedError("Undeclared namespace prefix: " + prefix);
            }

            uri = "";
        }

        nsResolver.pop();

        String localName = extractLocalName(qname);

        QName fqn = elements.pop();
        final QName endElementQName = new QName(uri, localName, prefix);
        if (!fqn.equals(endElementQName)) {
            throw new XmlNotWellFormedError("Invalid element name " + qname);
        }

        xmlConstructor.endElement(endElementQName);

        if (elements.isEmpty()) {
            xmlConstructor.endDocument();
            state = State.CLOSED;
        }
    }

    private void fatalError(BadFormatError exc) {
        if (log.isDebugEnabled()) {
            log.debug("Fatal error: " + exc.getMessage(), exc);
        }

        state = State.CLOSED;
        tokenizer.close();

        // make sure we send a start document event
        startDocument();

        errorHandler.onXmlParsingError(exc);
    }

    private boolean needsRestart() {
        return elements.size() > 0;
    }

    public void parse(ByteBuffer byteBuffer) {
        if (state == State.CLOSED)
            throw new IllegalStateException("Parser is closed");

        try {
            tokenizer.parse(byteBuffer);
        } catch (BadFormatError exc) {
            fatalError(exc);
        } catch (RuntimeException e) {
            fatalError(new BadFormatError(e.getMessage(), e));
        }
    }

    public void restart() {
        log.trace("Restarting XML stream");

        elements.clear();
        nsResolver = new NamespaceResolver();
        sentStartDocument = false;
        tokenizer.restart();
        xmlConstructor.restart();
    }

    private void startDocument() {
        if (!sentStartDocument) {
            xmlConstructor.startDocument();
            sentStartDocument = true;
        }
    }

    private void startElement() throws BadFormatError {
        if (log.isTraceEnabled()) {
            log.trace("StartElement " + qname);
        }

        if (elements.isEmpty()) {
            startDocument();
        }

        // find all namespace declarations so we can populate the NS resolver
        if (attributes != null) {
            Map<String, String> nsDeclarations = new TreeMap<String, String>();
            for (Entry<CharSequence, CharSequence> attribute : attributes.entrySet()) {
                final CharSequence qName = attribute.getKey();
                final String namespace = attribute.getValue().toString();

                if (CharSequenceUtils.equals(qName, XMLConstants.XMLNS_ATTRIBUTE)) {
                    // is namespace attribute
                    nsDeclarations.put(XMLConstants.DEFAULT_NS_PREFIX, namespace);
                } else if (CharSequenceUtils.startsWith(qName, "xmlns:")) {
                    final String nsPrefix = qName.subSequence(6, qName.length()).toString();
                    nsDeclarations.put(nsPrefix, namespace);
                }
            }
            nsResolver.push(nsDeclarations.isEmpty() ? Collections.<String, String> emptyMap() : nsDeclarations);
        } else {
            nsResolver.push(Collections.<String, String> emptyMap());
        }

        // find all non-namespace attributes
        List<XmlAttribute> nonNsAttributes = null;
        if (attributes != null) {
            for (Entry<CharSequence, CharSequence> attribute : attributes.entrySet()) {
                CharSequence attQname = attribute.getKey();

                if (!CharSequenceUtils.equals(attQname, "xmlns") && !CharSequenceUtils.startsWith(attQname, "xmlns:")) {
                    String attLocalName = extractLocalName(attQname);
                    String attPrefix = extractNsPrefix(attQname);
                    String attUri;
                    if (attPrefix.length() > 0) {
                        attUri = nsResolver.resolveUri(attPrefix);
                        if (attUri == null) {
                            if (attPrefix.length() > 0) {
                                throw new BadFormatError("Undeclared namespace prefix: " + attPrefix);
                            }
                            attUri = XMLConstants.NULL_NS_URI;
                        }
                    } else {
                        // by default, attributes are in the empty namespace
                        attUri = XMLConstants.NULL_NS_URI;
                    }

                    if (nonNsAttributes == null) {
                        nonNsAttributes = new LinkedList<XmlAttribute>();
                    }
                    nonNsAttributes.add(new XmlAttribute(new QName(attUri, attLocalName, attPrefix), attribute
                            .getValue()));
                }
            }
        }

        String prefix = extractNsPrefix(qname);
        String uri = nsResolver.resolveUri(prefix);
        if (uri == null) {
            if (prefix.length() > 0) {
                throw new BadFormatError("Undeclared namespace prefix: " + prefix);
            }
            uri = "";
        }

        String localName = extractLocalName(qname);

        final QName fullyQualifiedName = new QName(uri, localName, prefix);
        elements.add(fullyQualifiedName);

        xmlConstructor.startElement(fullyQualifiedName, nonNsAttributes);
    }

    @Override
    public void token(char c, CharSequence token) throws BadFormatError {
        if (log.isTraceEnabled()) {
            CharSequence s = (token == null) ? Character.toString(c) : token;
            log.trace("Parser got token " + s + " in state " + state);
        }

        switch (state) {
        case START:
            if (c == '<') {
                state = State.IN_TAG;
                attributes = null;
            } else {
                characters(token);
            }
            break;
        case IN_TAG:
            // token must be element name or / for a end tag
            if (c == '/') {
                state = State.IN_END_TAG;
            } else if (c == '?') {
                state = State.IN_DECLARATION;
                xmlDeclaration();
            } else if (c == '!') {
                throw new RestrictedXmlError("Comments are not allowed");
                // state = State.AFTER_COMMENT_BANG;
            } else {
                if (token != null && isValidName(token)) {
                    qname = token;
                    state = State.AFTER_START_NAME;
                } else {
                    if (token != null) {
                        throw new XmlNotWellFormedError("Invalid element name: " + qname);
                    }

                    throw new XmlNotWellFormedError("Not well-formed start tag");
                }
            }
            break;
        case IN_END_TAG:
            // token must be element name
            qname = token;
            state = State.AFTER_END_NAME;
            break;
        case AFTER_START_NAME:
            // token must be attribute name or > or /
            if (c == '>') {
                // end of start or end tag
                if (state == State.AFTER_START_NAME) {
                    startElement();
                    state = State.START;
                    attributes = null;
                } else if (state == State.AFTER_END_NAME) {
                    state = State.START;
                    endElement();
                }
            } else if (c == '/') {
                state = State.IN_EMPTY_TAG;
            } else {
                // must be attribute name
                attributeName = token;
                state = State.AFTER_ATTRIBUTE_NAME;
            }
            break;
        case AFTER_ATTRIBUTE_NAME:
            // token must be =
            if (c == '=') {
                state = State.AFTER_ATTRIBUTE_EQUALS;
            } else {
                throw new XmlNotWellFormedError("Not wellformed");
            }
            break;
        case AFTER_ATTRIBUTE_EQUALS:
            // token must be " or '
            if (c == '"' || c == '\'') {
                state = State.AFTER_ATTRIBUTE_FIRST_QUOTE;
            }
            break;
        case AFTER_ATTRIBUTE_FIRST_QUOTE:
            // token must be attribute value
            if (attributes == null) {
                attributes = new TreeMap<CharSequence, CharSequence>(CharSequenceUtils.COMPARATOR);
            }
            attributes.put(attributeName, CharSequenceUtils.unescapeXml(token));
            state = State.AFTER_ATTRIBUTE_VALUE;
            break;
        case AFTER_ATTRIBUTE_VALUE:
            // token must be " or '
            if (c == '"' || c == '\'') {
                state = State.AFTER_START_NAME;
            } else {
                throw new XmlNotWellFormedError("Not wellformed");
            }
            break;
        case AFTER_END_NAME:
            // token must be >
            if (c == '>') {
                state = State.START;
                endElement();
            }
            break;
        case IN_EMPTY_TAG:
            // token must be >
            if (c == '>') {
                startElement();
                attributes = null;

                if (state != State.CLOSED) {
                    state = State.START;
                    endElement();
                }
            }
            break;
        case AFTER_COMMENT_BANG:
            // token must be -
            if (c == '-') {
                state = State.AFTER_COMMENT_DASH1;
            } else {
                throw new XmlNotWellFormedError("Comment not wellformed");
            }
            break;
        case AFTER_COMMENT_DASH1:
            // token must be -
            if (c == '-') {
                state = State.AFTER_COMMENT_DASH2;
            } else {
                throw new XmlNotWellFormedError("Comment not wellformed");
            }
            break;
        case AFTER_COMMENT_DASH2:
            // we should now get the comment content, ignore
            if (c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH1;
            } else {
                state = State.AFTER_COMMENT;
            }
            break;
        case AFTER_COMMENT:
            // token must be - or some text
            if (c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH1;
            } else if (c == '>') {
                throw new XmlNotWellFormedError("Comment not wellformed");
            } else {
                // ignore
            }
            break;
        case AFTER_COMMENT_CLOSING_DASH1:
            // token must be -
            if (c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH2;
            } else {
                throw new XmlNotWellFormedError("Comment not wellformed");
            }
            break;
        case AFTER_COMMENT_CLOSING_DASH2:
            // token must be >
            if (c == '>') {
                state = State.START;
            } else {
                throw new XmlNotWellFormedError("Comment not wellformed");
            }
            break;
        case IN_DECLARATION:
            // wait for >
            if (c == '>') {
                state = State.START;
            }
            break;
        }
    }

    private void xmlDeclaration() throws BadFormatError {
        // we got an XML declaration, should we restart stream?
        // TODO could also be a PI, if we want to support PIs, this code needs further attention
        if (needsRestart()) {
            // restarts not allowed, fail
            throw new XmlNotWellFormedError("Another unexpected XML declaration found");
        }
    }

    private static enum State {
        AFTER_ATTRIBUTE_EQUALS,
        AFTER_ATTRIBUTE_FIRST_QUOTE,
        AFTER_ATTRIBUTE_NAME,
        AFTER_ATTRIBUTE_VALUE,
        AFTER_COMMENT,
        AFTER_COMMENT_BANG,
        AFTER_COMMENT_CLOSING_DASH1,
        AFTER_COMMENT_CLOSING_DASH2,
        AFTER_COMMENT_DASH1,
        AFTER_COMMENT_DASH2,
        AFTER_COMMENT_ENDING_DASH1,
        AFTER_COMMENT_ENDING_DASH2,
        AFTER_END_NAME,
        AFTER_START_NAME,
        CLOSED,
        IN_DECLARATION,
        IN_EMPTY_TAG,
        IN_END_TAG,
        IN_TAG,
        START
    }
}
