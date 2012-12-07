package one.xmpp.server.errors.stanza;

/**
 * The requesting entity does not possess the required permissions to perform the action; the
 * associated error type SHOULD be "auth".
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class ForbiddenCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public ForbiddenCondition() {
        super("forbidden", StanzaErrorType.auth);
    }

    public ForbiddenCondition(Exception exc) {
        super(exc, "forbidden", StanzaErrorType.auth);
    }

}
