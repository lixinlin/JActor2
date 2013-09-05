package org.agilewiki.jactor2.osgi;

import org.agilewiki.jactor2.core.messaging.Request;
import org.agilewiki.jactor2.core.messaging.Transport;
import org.agilewiki.jactor2.core.processing.MessageProcessor;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import java.util.Map;

/**
 * Locates (or waits for) a service.
 */
public class LocateService<T> implements ServiceChangeReceiver<T> {

    /**
     * The processing.
     */
    private MessageProcessor messageProcessor;

    /**
     * The service tracker actor.
     */
    private JAServiceTracker<T> tracker;

    /**
     * The transport for returning the service.
     */
    private Transport<T> transport;

    /**
     * Create a LocateService actor.
     *
     * @param _messageProcessor The actor processing.
     * @param clazz             Class name of the desired service.
     */
    public LocateService(MessageProcessor _messageProcessor, String clazz) throws Exception {
        messageProcessor = _messageProcessor;
        tracker = new JAServiceTracker(messageProcessor, clazz);
    }

    /**
     * Returns a request to locate the service.
     *
     * @return The request.
     */
    public Request<T> getReq() {
        return new Request<T>(messageProcessor) {
            @Override
            public void processRequest(final Transport<T> _transport) throws Exception {
                tracker.start(LocateService.this);
                transport = _transport;
            }
        };
    }

    @Override
    public void serviceChange(ServiceEvent _event,
                              Map<ServiceReference, T> _tracked)
            throws Exception {
        if (_tracked.size() > 0 && transport != null) {
            T service = _tracked.values().iterator().next();
            transport.processResponse(service);
            transport = null;
            tracker.close();
            tracker = null;
        }
    }

    @Override
    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }
}