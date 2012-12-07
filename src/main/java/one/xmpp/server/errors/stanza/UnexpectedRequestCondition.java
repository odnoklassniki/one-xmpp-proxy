package one.xmpp.server.errors.stanza;

/**
 * The recipient or server understood the request but was not expecting it at this time (e.g., the
 * request was out of order).
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class UnexpectedRequestCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public UnexpectedRequestCondition() {
        super("unexpected-request", StanzaErrorType.wait);
    }

}
