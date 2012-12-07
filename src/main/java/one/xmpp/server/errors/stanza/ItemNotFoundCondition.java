package one.xmpp.server.errors.stanza;

/**
 * The addressed JID or item requested cannot be found.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class ItemNotFoundCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public ItemNotFoundCondition() {
        super("item-not-found", StanzaErrorType.cancel);
    }

    public ItemNotFoundCondition(String message) {
        super(message, "item-not-found", StanzaErrorType.cancel);
    }

}
