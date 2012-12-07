package one.xmpp.server.errors.stanza;

/**
 * The sender has sent XML that is malformed or that cannot be processed (e.g., an IQ stanza that
 * includes an unrecognized value of the 'type' attribute).
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class BadRequestCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public BadRequestCondition() {
        super("bad-request", StanzaErrorType.modify);
    }

    public BadRequestCondition(String message) {
        super(message, "bad-request", StanzaErrorType.modify);
    }

}
