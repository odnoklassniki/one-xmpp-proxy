package one.xmpp.server.errors.stanza;

/**
 * The sender must provide proper credentials before being allowed to perform the action, or has
 * provided improper credentials.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class NotAuthorizedCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public NotAuthorizedCondition() {
        super("not-authorized", StanzaErrorType.auth);
    }

    public NotAuthorizedCondition(String message) {
        super(message, "not-authorized", StanzaErrorType.auth);
    }
}
