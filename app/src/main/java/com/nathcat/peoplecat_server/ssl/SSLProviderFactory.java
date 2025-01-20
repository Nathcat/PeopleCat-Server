package com.nathcat.peoplecat_server.ssl;

import java.lang.reflect.InvocationTargetException;

import org.json.simple.JSONObject;

public class SSLProviderFactory {
    public static ISSLProvider getProvider(String providerName, JSONObject config) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        return ISSLProvider.class.cast(Class.forName("com.nathcat.peoplecat_server.ssl." + providerName).getDeclaredConstructor(JSONObject.class).newInstance(config));
    }
}
