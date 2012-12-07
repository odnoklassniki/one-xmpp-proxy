package one.xmpp.server.errors.stanza;

/**
 * The sending entity has provided or communicated an XMPP address (e.g., a value of the 'to'
 * attribute) or aspect thereof (e.g., a resource identifier) that does not adhere to the syntax
 * defined in Addressing Scheme (Section 3);
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class JidMalformedCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public JidMalformedCondition() {
        super("jid-malformed", StanzaErrorType.modify);
    }

    public JidMalformedCondition(String message) {
        super(message, "jid-malformed", StanzaErrorType.modify);
    }

    public JidMalformedCondition(Throwable cause) {
        super(cause, "jid-malformed", StanzaErrorType.modify);
    }

}
