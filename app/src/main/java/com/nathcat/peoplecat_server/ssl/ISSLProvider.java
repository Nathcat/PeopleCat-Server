package com.nathcat.peoplecat_server.ssl;

import javax.net.ssl.SSLContext;

public interface ISSLProvider {
    SSLContext getContext();
}
