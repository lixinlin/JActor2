package org.agilewiki.jactor2.core.mt.mtRequests;

import org.agilewiki.jactor2.core.impl.reactorsImpl.ReactorImpl;
import org.agilewiki.jactor2.core.impl.requestsImpl.RequestImpl;

/**
 * A source of requests, and which must be able to handle a response.
 */
public interface RequestSource {

    /**
     * Process an incoming response.
     *
     * @param message        The response.
     * @param responseSource The targetReactor returning the response.
     */
    void incomingResponse(final RequestImpl message, final ReactorImpl responseSource);
}