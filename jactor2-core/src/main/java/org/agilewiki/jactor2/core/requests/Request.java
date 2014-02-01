package org.agilewiki.jactor2.core.requests;

import org.agilewiki.jactor2.core.impl.RequestImpl;
import org.agilewiki.jactor2.core.reactors.Reactor;

/**
 * A request is a single-use object used to perform an operation safely and to optionally be pass back with a response
 * value that is also processed safely.
 *
 * @param <RESPONSE_TYPE>    The type response value.
 */
public interface Request<RESPONSE_TYPE> {
    /**
     * Returns the object used to implement the request.
     *
     * @return The object used to implement the request.
     */
    RequestImpl<RESPONSE_TYPE> asRequestImpl();

    /**
     * Returns the Reactor that provides the thread context in which this request will operate.
     *
     * @return The target Reactor.
     */
    Reactor getTargetReactor();

    /**
     * Passes this request and then blocks the source thread until the request is returned
     * with a response value.
     * This method can not be called within the thread context of a reactor.
     *
     * @return The result value.
     */
    RESPONSE_TYPE call() throws Exception;

    /**
     * Passes this Request to the target Reactor without any result being passed back.
     * I.E. The signal method results in a 1-way message being passed.
     * If an exception is thrown while processing this Request,
     * that exception is simply logged as a warning.
     */
    void signal() throws Exception;

    /**
     * Returns the source reactor, or null.
     * A null is returned if the request was passed using the signal or call methods.
     *
     * @return The source reactor or null.
     */
    Reactor getSourceReactor();

    /**
     * Returns true if the request is canceled.
     * Closing the source reactor, for example, will result in the request being canceled.
     * Thread.interrupted is also called to see if the thread is interrupted.
     * This method should ideally be called periodically within long loops.
     *
     * @return True if the request is canceled.
     * @throws InterruptedException Thrown when the thread is interrupted.
     */
    boolean isCanceled() throws InterruptedException ;

    /**
     * Returns true if the request is closed.
     * Closing the target reactor, for example, will result in the request being closed.
     *
     * @return True if the request is canceled.
     */
    boolean isClosed();

    /**
     * An optional callback used to signal that the request has been canceled.
     * This method must be thread-safe, as there is no constraint on which
     * thread is used to call it.
     * By default, onCancel does nothing.
     */
    void onCancel();

    /**
     * An optional callback used to signal that the request has been closed.
     * This method must be thread-safe, as there is no constraint on which
     * thread is used to call it.
     * By default, onClose does nothing.
     */
    void onClose();
}
