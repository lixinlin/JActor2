package org.agilewiki.jactor2.core.messaging;

/**
 * A ResponseProcessor is an application callback to handle the response from a request.
 *
 * @param <RESPONSE_TYPE> The type of response.
 */
public interface ResponseProcessor<RESPONSE_TYPE> {
    /**
     * The processResponse method accepts the response of a request.
     * <p>
     * This method need not be thread-safe, as it
     * is always invoked from the same light-weight thread (message processor) that passed the
     * AsyncRequest and ResponseProcessor objects.
     * </p>
     *
     * @param _response The response to a request.
     */
    public void processResponse(final RESPONSE_TYPE _response) throws Exception;
}
