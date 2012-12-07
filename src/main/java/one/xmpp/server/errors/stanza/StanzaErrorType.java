package one.xmpp.server.errors.stanza;

public enum StanzaErrorType {

    /**
     * retry after providing credentials
     */
    auth("auth"),

    /**
     * do not retry (the error is unrecoverable)
     */
    cancel("cancel"),

    /**
     * proceed (the condition was only a warning)
     */
    continue_type("continue"),

    /**
     * retry after changing the data sent
     */
    modify("modify"),

    /**
     * retry after waiting (the error is temporary)
     */
    wait("modify");

    private final String type;

    private StanzaErrorType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return getType();
    }
}
