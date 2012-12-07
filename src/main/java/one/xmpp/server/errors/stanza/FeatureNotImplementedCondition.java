package one.xmpp.server.errors.stanza;

/**
 * The feature requested is not implemented by the recipient or server and therefore cannot be
 * processed.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class FeatureNotImplementedCondition extends XmppStanzaError {

    private static final long serialVersionUID = 1L;

    public FeatureNotImplementedCondition() {
        super("feature-not-implemented", StanzaErrorType.cancel);
    }

    public FeatureNotImplementedCondition(Exception exc) {
        super(exc, "feature-not-implemented", StanzaErrorType.cancel);
    }

    public FeatureNotImplementedCondition(String message) {
        super(message, "feature-not-implemented", StanzaErrorType.cancel);
    }

}
