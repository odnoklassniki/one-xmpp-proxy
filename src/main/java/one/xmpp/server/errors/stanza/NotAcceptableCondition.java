package one.xmpp.server.errors.stanza;

/**
 * The recipient or server understands the request but cannot process it because the request does
 * not meet criteria defined by the recipient or server (e.g., a request to subscribe to information
 * that does not simultaneously include configuration parameters needed by the recipient).
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class NotAcceptableCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public NotAcceptableCondition() {
        super("not-acceptable", StanzaErrorType.modify);
    }

}
