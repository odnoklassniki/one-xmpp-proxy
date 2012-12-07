package one.xmpp.server.network;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public interface SslConfiguration {

    /**
     * Set SSL Context parameters, including chain file path, certificate file path, accepted
     * protocols, password (if required)
     */
    public void setupSslContext(long contextPointer) throws Exception;
}
