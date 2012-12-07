package one.xmpp.server.errors.stanza;

public class InternalServerErrorCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public InternalServerErrorCondition() {
        super("internal-server-error", StanzaErrorType.wait);
    }

    public InternalServerErrorCondition(Throwable cause) {
        super(cause, "internal-server-error", StanzaErrorType.wait);
    }

}
