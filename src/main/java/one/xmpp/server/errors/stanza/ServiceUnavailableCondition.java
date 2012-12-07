package one.xmpp.server.errors.stanza;

/**
 * The server or recipient does not currently provide the requested service.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class ServiceUnavailableCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public ServiceUnavailableCondition() {
        super("service-unavailable", StanzaErrorType.cancel);
    }

    public ServiceUnavailableCondition(Exception exc) {
        super(exc, "service-unavailable", StanzaErrorType.cancel);
    }

}
