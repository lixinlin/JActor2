package org.agilewiki.jactor2.core.mailbox;

import org.agilewiki.jactor2.core.context.JAContext;
import org.agilewiki.jactor2.core.messaging.ExceptionHandler;
import org.agilewiki.jactor2.core.messaging.MessageSource;

/**
 * A mailbox has an inbox for incoming messages (events/requests)
 * and buffers outgoing messages by destination mailbox.
 */
public interface Mailbox extends Runnable, MessageSource, AutoCloseable {

    /**
     * Returns the mailbox factory.
     *
     * @return The mailbox factory.
     */
    JAContext getJAContext();

    /**
     * Replace the current ExceptionHandler with another.
     *
     * @param exceptionHandler The exception handler to be used now.
     *                         May be null if the default exception handler is to be used.
     * @return The exception handler that was previously in effect, or null if the
     *         default exception handler was in effect.
     */
    ExceptionHandler setExceptionHandler(final ExceptionHandler exceptionHandler);

    /**
     * Is the inbox empty?
     *
     * @return True when the inbox is empty.
     */
    boolean isInboxEmpty();
}