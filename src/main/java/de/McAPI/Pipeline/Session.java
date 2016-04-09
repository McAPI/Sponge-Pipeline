package de.McAPI.Pipeline;

import io.netty.util.AttributeKey;

public class Session {

    public final static AttributeKey<Session> SESSION_ATTRIBUTE_KEY = AttributeKey.valueOf("key_pipeline_session");

    private final String key;
    private final String debugKey;

    public Session() {
        this.key = Token.generateToken();
        this.debugKey = Token.pseudo();
    }

    /**
     * This method returns the related key.
     * @return
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Returns the debug key.
     * @return
     */
    public String getDebugKey() {
        return this.debugKey;
    }

}
