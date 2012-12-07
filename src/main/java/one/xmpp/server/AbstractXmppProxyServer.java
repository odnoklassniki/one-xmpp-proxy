package one.xmpp.server;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Component;

import one.ejb.NotNullByDefault;
import one.xmpp.server.errors.stanza.InternalServerErrorCondition;
import one.xmpp.server.errors.stanza.XmppStanzaError;
import one.xmpp.server.errors.stream.BadFormatError;
import one.xmpp.server.errors.stream.InternalServerError;
import one.xmpp.server.errors.stream.SaslNotAuthorizedError;
import one.xmpp.server.errors.stream.XmppStreamError;
import one.xmpp.server.network.AbstractServer;
import one.xmpp.server.network.SocketCloseOperation;
import one.xmpp.server.network.SslAcceptor;
import one.xmpp.utils.CharSequenceUtils;
import one.xmpp.xml.StanzaListener;
import one.xmpp.xml.XmlConstructor;
import one.xmpp.xml.XmlElement;
import one.xmpp.xml.XmlParser;
import one.xmpp.xml.XmlParsingErrorHandler;

/**
 * Extension of {@link AbstractServer} with simple XMPP-XML parsing support and error handling.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
@Component
@NotNullByDefault
public abstract class AbstractXmppProxyServer extends AbstractServer {

    private static final Log log = LogFactory.getLog(AbstractXmppProxyServer.class);

    private static final ByteBuffer MESSAGE_GOODBYE = CharSequenceUtils.encodeToUtf8("</"
            + XmppConstants.Prefixes.STREAM + ":" + XmppConstants.Elements.STREAM_STREAM + ">");

    private final AtomicLong receivedStanzas = new AtomicLong(0);

    @Autowired
    private XmppProxyConfiguration xmppServiceConfiguration;

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getReceivedStanzas() {
        return receivedStanzas.longValue();
    }

    /**
     * Handles newly-accepted socket. One can override the method to implement additional checks,
     * like IP-blacklist check
     */
    @Override
    protected void handleAccepted(AbstractSocket socket) {
        // no op -- waiting for client first packet
        addToReadPoll(socket);
    }

    @Override
    protected void handleRead(AbstractSocket socket, final ByteBuffer readBuffer) {
        AbstractXmppProxySocket xmppSocket = (AbstractXmppProxySocket) socket;

        try {
            xmppSocket.xmlParser.parse(readBuffer);
        } catch (Throwable exc) {
            log.error(exc, exc);

            new InternalServerError().queueWrite(xmppSocket);
            xmppSocket.queueGoodbuy();
            return;
        }
    }

    public void handleStanzaError(AbstractXmppProxySocket xmppSocket, XmlElement stanza, Exception exc) {

        if (exc instanceof XmppStreamError) {
            if (exc instanceof SaslNotAuthorizedError) {
                log.info("Auth problem for " + xmppSocket + ": " + exc);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("XMPP Exception occured dusing processing " + stanza.getQName() + " stranza for "
                            + xmppSocket + ": " + exc, exc);
                }
            }

            ((XmppStreamError) exc).queueWrite(xmppSocket);
            xmppSocket.queueGoodbuy();
            return;
        }

        if (exc instanceof XmppStanzaError) {
            if (log.isDebugEnabled()) {
                log.debug("XMPP stanza error condition occured dusing processing " + stanza.getQName()
                        + " stranza for " + xmppSocket + ": " + exc, exc);
            }

            xmppSocket.queueWrite(((XmppStanzaError) exc).toXmlElement(xmppSocket, stanza));
            return;
        }

        log.error("Unknown error processing packet from " + xmppSocket + ": " + exc, exc);
        xmppSocket.queueWrite(new InternalServerErrorCondition().toXmlElement(xmppSocket, stanza));
    }

    /**
     * Handles client stranzas. Core method to implement.
     */
    protected abstract void handleStranza(AbstractXmppProxySocket xmppSocket, XmlElement xmlElement);

    /**
     * Handles receiving stream:stream element from client. Usually replies with stream:stream
     * element and additional stream features element.
     */
    protected abstract void handleStreamBegin(AbstractXmppProxySocket xmppSocket, XmlElement streamElement);

    /**
     * Log error if server unable to encode queued data into UTF-8 encoding
     */
    protected void logEncodeError(CharSequence data, Throwable exc) {
        log.error("Unable to encode '" + data + "' to UTF-8: " + exc, exc);
    }

    /**
     * Log error if server unable to decode client data as XML
     */
    protected void logParsingError(AbstractXmppProxySocket xmppSocket, BadFormatError exc) {
        if (log.isDebugEnabled()) {
            log.debug("XML parsing error from " + xmppSocket + ": " + exc, exc);
        } else if (log.isInfoEnabled()) {
            log.info("XML parsing error from " + xmppSocket + ": " + exc.getMessage());
        }
    }

    protected abstract void logStanzaProcessed(long spentTime);

    @Override
    protected abstract AbstractXmppProxySocket newISocket(long clientSocketPointer, boolean secured) throws Exception;

    public abstract class AbstractXmppProxySocket extends AbstractSocket implements StanzaListener, XmlParsingErrorHandler {

        private boolean secured;

        private final XmlConstructor xmlConstructor;

        private final XmlParser xmlParser;

        protected AbstractXmppProxySocket(long clientSock, boolean secured) throws Exception {
            super(clientSock);

            this.secured = secured;
            this.xmlConstructor = new XmlConstructor(this);
            this.xmlParser = new XmlParser(xmlConstructor, this);
        }

        @Override
        protected void dumpImpl(StringBuilder stringBuilder) {
            super.dumpImpl(stringBuilder);

            stringBuilder.append("\tSecured: \t").append(secured).append('\n');
        }

        public abstract String getBareJid();

        public abstract String getJid();

        /**
         * @return <tt>true</tt> if socket was accepted by {@link SslAcceptor} or encryption was
         *         enabled later by TLS Start processor
         */
        public boolean isSecured() {
            return secured;
        }

        @Override
        public void onStanza(XmlElement xmlElement) {
            receivedStanzas.incrementAndGet();

            final long startTime = System.currentTimeMillis();
            try {
                AbstractXmppProxyServer.this.handleStranza(this, xmlElement);
            } finally {
                logStanzaProcessed(System.currentTimeMillis() - startTime);
            }
        }

        @Override
        public void onStreamBegin(XmlElement xmlElement) {
            receivedStanzas.incrementAndGet();
            handleStreamBegin(this, xmlElement);
        }

        @Override
        public void onStreamEnd(QName qName) {
            receivedStanzas.incrementAndGet();
            assertNotClosed();
            queueGoodbuy();
        }

        @Override
        @NotNullByDefault(false)
        public void onXmlParsingError(BadFormatError exc) {
            logParsingError(this, exc);
            exc.queueWrite(this);
            queueGoodbuy();
        }

        public void queueGoodbuy() {
            queueWrite(MESSAGE_GOODBYE);
            queue(SocketCloseOperation.INSTANCE);
        }

        public void queueRestart() {
            queue(RestartStreamOperation.INSTANCE);
        }

        public void queueWrite(CharSequence data) {
            assertNotClosed();
            final ByteBuffer encoded;
            try {
                encoded = CharSequenceUtils.encodeToUtf8(data);
            } catch (RuntimeException exc) {
                logEncodeError(data, exc);
                throw exc;
            }
            queueWriteImpl(encoded);
        }

        public void queueWrite(XmlElement data) {
            final CharSequence xml = data.toXml();

            if (log.isTraceEnabled()) {
                log.trace("Adding XML '" + xml + "' to write queue of " + this);
            }

            queueWrite(xml);
        }

        void restart() {
            if (log.isDebugEnabled()) {
                log.debug("Restarting stream parsing for " + this);
            }

            xmlParser.restart();
        }

        public void setSecured(boolean secured) {
            this.secured = secured;
        }
    }
}
