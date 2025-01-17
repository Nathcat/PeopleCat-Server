package com.nathcat.AuthCat;

import org.json.simple.*;

/**
 * Contains interpreted data from an authentication request to AuthCat
 * @author Nathan Baines
 */
public class AuthResult {
    /**
     * Whether or not the authentication request was successful
     */
    public final boolean result;
    /**
     * The user data returned by the service, if result = true, otherwise this field is null
     */
    public final JSONObject user;

    public AuthResult(JSONObject user) {
        this.result = true;
        this.user = user;
    }

    public AuthResult() {
        this.result = false;
        this.user = null;
    }

    @Override
    public String toString() {
        return (result ? "Successful" : "Unsuccessful") + " authentication result.";
    }
}
