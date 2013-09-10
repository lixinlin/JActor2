package org.agilewiki.jactor2.core.messaging;

import org.agilewiki.jactor2.core.processing.MessageProcessor;
import org.agilewiki.jactor2.core.processing.MessageProcessorBase;
import org.agilewiki.jactor2.core.threading.ModuleContext;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public abstract class RequestBase<RESPONSE_TYPE> implements Message {

    /**
     * Assigned to current time when ModuleContext.DEBUG.
     */
    private Long debugTimestamp;

    /**
     * A request can only be used once.
     */
    protected boolean used;

    /**
     * The message processor where this Request Object is passed for processing. The thread
     * owned by this message processor will process the Request.
     */
    protected final MessageProcessorBase messageProcessor;

    /**
     * True when the result is to be returned via a message processor with a context
     * that differs from the context of the target message processor.
     */
    protected boolean foreign;

    /**
     * The source message processor or pender that will receive the results.
     */
    protected MessageSource messageSource;

    /**
     * The message targeted to the source message processor which, when processed,
     * resulted in this message.
     */
    protected Message oldMessage;

    /**
     * The exception handler that was active in the source message processor at the time
     * when this message was created.
     */
    protected ExceptionHandler sourceExceptionHandler;

    /**
     * The application object that will process the results.
     */
    protected AsyncResponseProcessor responseProcessor;

    /**
     * True when a response to this message has not yet been determined.
     */
    protected boolean responsePending = true;

    /**
     * The response created when this message is applied to the target actor.
     */
    protected Object response;

    /**
     * Create a RequestBase.
     *
     * @param _targetMessageProcessor The message processor where this Request Objects is passed for processing.
     *                                The thread owned by this message processor will process this Request.
     */
    public RequestBase(final MessageProcessor _targetMessageProcessor) {
        if (_targetMessageProcessor == null) {
            throw new NullPointerException("targetMessageProcessor");
        }
        messageProcessor = (MessageProcessorBase) _targetMessageProcessor;
    }

    /**
     * Returns the MessageProcessor to which this Request is bound and to which this Request is to be passed.
     *
     * @return The target MessageProcessor.
     */
    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    /**
     * Marks the request as having been used, or throws an
     * exception if the request was already used.
     */
    protected void use() {
        if (used)
            throw new IllegalStateException("Already used");
        used = true;
    }

    /**
     * Passes this Request to the target MessageProcessor without any result being passed back.
     * I.E. The signal method results in a 1-way message being passed.
     * If an exception is thrown while processing this Request,
     * that exception is simply logged as a warning.
     */
    public void signal() throws Exception {
        use();
        responseProcessor = SignalResponseProcessor.SINGLETON;
        messageProcessor.unbufferedAddMessage(this, false);
    }

    /**
     * Passes this Request together with the AsyncResponseProcessor to the target MessageProcessor.
     * Responses are passed back via the message processor of the source actor and processed by the
     * provided AsyncResponseProcessor and any exceptions
     * raised while processing the request are processed by the exception handler active when
     * the send method was called.
     *
     * @param _source            The message processor on whose thread this method was invoked and which
     *                           will buffer this Request and subsequently receive the result for
     *                           processing on the same thread.
     * @param _responseProcessor Passed with this request and then returned with the result, the
     *                           AsyncResponseProcessor is used to process the result on the same thread
     *                           that originally invoked this method. If null, then no response is returned.
     */
    public void send(final MessageProcessor _source,
                     final AsyncResponseProcessor<RESPONSE_TYPE> _responseProcessor) throws Exception {
        MessageProcessorBase source = (MessageProcessorBase) _source;
        if (!source.isRunning())
            throw new IllegalStateException(
                    "A valid source message processor can not be idle");
        use();
        AsyncResponseProcessor<RESPONSE_TYPE> rp = _responseProcessor;
        if (rp == null)
            rp = (AsyncResponseProcessor<RESPONSE_TYPE>) SignalResponseProcessor.SINGLETON;
        else
            addDebugPending();
        foreign = source.getModuleContext() != messageProcessor.getModuleContext();
        messageSource = source;
        oldMessage = source.getCurrentMessage();
        sourceExceptionHandler = source.getExceptionHandler();
        responseProcessor = rp;
        boolean local = messageProcessor == source;
        if (local || !source.buffer(this, messageProcessor))
            messageProcessor.unbufferedAddMessage(this, local);
    }

    /**
     * Passes this Request to the target MessageProcessor and blocks the current thread until
     * a result is returned. The call method sends the message directly without buffering,
     * as there is no message processor. The response message is buffered, though thread migration is
     * not possible.
     *
     * @return The result from applying this Request to the target actor.
     * @throws Exception If the result is an exception, it is thrown rather than being returned.
     */
    public RESPONSE_TYPE call() throws Exception {
        use();
        addDebugPending();
        foreign = true;
        messageSource = new Pender();
        responseProcessor = CallResponseProcessor.SINGLETON;
        messageProcessor.unbufferedAddMessage(this, false);
        return (RESPONSE_TYPE) ((Pender) messageSource).pend();
    }

    /**
     * track pending requests.
     */
    private void addDebugPending() {
        if (ModuleContext.DEBUG) {
            debugTimestamp = System.nanoTime();
            ModuleContext targetModuleContext = messageProcessor.getModuleContext();
            Map<Long, Set<RequestBase>> pendingRequests = targetModuleContext.pendingRequests;
            Set<RequestBase> nanoSet = pendingRequests.get(debugTimestamp);
            if (nanoSet == null) {
                nanoSet = Collections.newSetFromMap(new ConcurrentHashMap<RequestBase, Boolean>());
            }
            pendingRequests.put(debugTimestamp, nanoSet);
        }
    }

    /**
     * Assigns a response to the request.
     *
     * @param _response the response being returned
     */
    protected void setResponse(final Object _response, final MessageProcessor _activeMessageProcessor) {
        ((MessageProcessorBase) _activeMessageProcessor).requestEnd();
        responsePending = false;
        response = _response;
        if (ModuleContext.DEBUG) {
            ModuleContext targetModuleContext = messageProcessor.getModuleContext();
            Map<Long, Set<RequestBase>> pendingRequests = targetModuleContext.pendingRequests;
            Set<RequestBase> nanoSet = pendingRequests.get(debugTimestamp);
            if (nanoSet != null) {
                nanoSet.remove(this);
                if (nanoSet.isEmpty()) {
                    pendingRequests.remove(debugTimestamp);
                }
            }
        }
    }

    /**
     * The processObjectResponse method accepts the response of a request.
     * <p>
     * This method need not be thread-safe, as it
     * is always invoked from the same light-weight thread (message processor) that passed the
     * Request.
     * </p>
     *
     * @param _response The response to a request.
     * @return True when this is the first response.
     */
    protected boolean processObjectResponse(final Object _response)
            throws Exception {
        final ModuleContext moduleContext = messageProcessor.getModuleContext();
        if (foreign)
            moduleContext.removeAutoClosable(RequestBase.this);
        if (!responsePending)
            return false;
        setResponse(_response, messageProcessor);
        if (responseProcessor != SignalResponseProcessor.SINGLETON) {
            messageSource.incomingResponse(RequestBase.this, messageProcessor);
        } else {
            if (_response instanceof Throwable) {
                messageProcessor.getLogger().warn("Uncaught throwable",
                        (Throwable) _response);
            }
        }
        return true;
    }

    /**
     * Returns the ModuleContext of the request source.
     *
     * @return The ModuleContext of the request source, or null when the request was
     *         passed using signal or call.
     */
    public ModuleContext getModuleContext() {
        if (messageSource == null)
            return null;
        if (!(messageSource instanceof MessageProcessor))
            return null;
        return ((MessageProcessorBase) messageSource).getModuleContext();
    }

    @Override
    public boolean isForeign() {
        return foreign;
    }

    @Override
    public boolean isResponsePending() {
        return responsePending;
    }

    @Override
    public void close() {
        if (!responsePending)
            return;
        responsePending = false;
        response = new ServiceClosedException();
        messageSource.incomingResponse(this, null);
    }

    /**
     * Process a request or the response.
     */
    @Override
    public void eval() {
        if (responsePending) {
            final ModuleContext moduleContext = messageProcessor.getModuleContext();
            if (foreign)
                moduleContext.addAutoClosable(this);
            messageProcessor.setExceptionHandler(null);
            messageProcessor.setCurrentMessage(this);
            messageProcessor.requestBegin();
            try {
                processRequestMessage();
            } catch (final Exception e) {
                if (foreign)
                    moduleContext.removeAutoClosable(this);
                processException(messageProcessor, e);
            }
        } else {
            processResponseMessage();
        }
    }

    /**
     * Process a request.
     */
    abstract protected void processRequestMessage() throws Exception;

    /**
     * Process a response.
     */
    private void processResponseMessage() {
        MessageProcessorBase sourceMessageProcessor = (MessageProcessorBase) messageSource;
        sourceMessageProcessor.setExceptionHandler(sourceExceptionHandler);
        sourceMessageProcessor.setCurrentMessage(oldMessage);
        if (response instanceof Exception) {
            oldMessage.processException(sourceMessageProcessor, (Exception) response);
            return;
        }
        try {
            responseProcessor.processAsyncResponse(response);
        } catch (final Exception e) {
            oldMessage.processException(sourceMessageProcessor, e);
        }
    }

    @Override
    public void processException(final MessageProcessor _activeMessageProcessor, final Exception _e) {
        MessageProcessorBase activeMessageProcessor = (MessageProcessorBase) _activeMessageProcessor;
        ExceptionHandler<RESPONSE_TYPE> exceptionHandler = activeMessageProcessor.getExceptionHandler();
        if (exceptionHandler != null) {
            try {
                processObjectResponse(exceptionHandler.processException(_e));
            } catch (final Throwable u) {
                activeMessageProcessor.getLogger().error("Exception handler unable to process throwable "
                        + exceptionHandler.getClass().getName(), u);
                if (!(responseProcessor instanceof SignalResponseProcessor)) {
                    if (!responsePending)
                        return;
                    setResponse(u, activeMessageProcessor);
                    messageSource.incomingResponse(this, activeMessageProcessor);
                } else {
                    activeMessageProcessor.getLogger().error("Thrown by exception handler and uncaught "
                            + exceptionHandler.getClass().getName(), _e);
                }
            }
        } else {
            if (!responsePending) {
                return;
            }
            setResponse(_e, activeMessageProcessor);
            if (!(responseProcessor instanceof SignalResponseProcessor))
                messageSource.incomingResponse(this, activeMessageProcessor);
            else {
                activeMessageProcessor.getLogger().warn("Uncaught throwable", _e);
            }
        }
    }

    /**
     * Replace the current ExceptionHandler with another.
     * <p>
     * When an event or request message is processed by a message processor, the current
     * exception handler is set to null. When a request is sent by a message processor, the
     * current exception handler is saved in the outgoing message and restored when
     * the response message is processed.
     * </p>
     *
     * @param _exceptionHandler The exception handler to be used now.
     *                         May be null if the default exception handler is to be used.
     * @return The exception handler that was previously in effect, or null if the
     *         default exception handler was in effect.
     */
    public ExceptionHandler<RESPONSE_TYPE> setExceptionHandler(final ExceptionHandler<RESPONSE_TYPE> _exceptionHandler) {
        ExceptionHandler<RESPONSE_TYPE> old = messageProcessor.getExceptionHandler();
        messageProcessor.setExceptionHandler(_exceptionHandler);
        return old;
    }

    /**
     * Pender is used by the RequestBase.call method to block the current thread until a
     * result is available and then either return the result or rethrow it if the result
     * is an exception.
     */
    private static final class Pender implements MessageSource {

        /**
         * Used to signal the arrival of a response.
         */
        private final Semaphore done = new Semaphore(0);

        /**
         * The result from the incoming response. May be null or an Exception.
         */
        private transient Object result;

        /**
         * Returns the response, which may be null. But if the response
         * is an exception, then it is thrown.
         *
         * @return The response or null, but not an exception.
         */
        Object pend() throws Exception {
            done.acquire();
            if (result instanceof Exception)
                throw (Exception) result;
            if (result instanceof Error)
                throw (Error) result;
            return result;
        }

        @Override
        public void incomingResponse(final Message _message,
                                     final MessageProcessor _responseSource) {
            result = ((RequestBase) _message).response;
            done.release();
        }
    }

    /**
     * A subclass of AsyncResponseProcessor that is used as a place holder when the RequestBase.call
     * method is used.
     */
    final private static class CallResponseProcessor implements AsyncResponseProcessor<Object> {
        /**
         * The singleton.
         */
        public static final CallResponseProcessor SINGLETON = new CallResponseProcessor();

        /**
         * Restrict the use of this class to being a singleton.
         */
        private CallResponseProcessor() {
        }

        @Override
        public void processAsyncResponse(final Object response) {
            throw new UnsupportedOperationException();
        }
    }
}
