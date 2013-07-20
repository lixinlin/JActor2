package org.agilewiki.jactor2.impl;

import org.agilewiki.jactor2.api.ExceptionHandler;
import org.agilewiki.jactor2.api.Mailbox;
import org.agilewiki.jactor2.api.Message;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;

/**
 * Base class for mailboxes.
 */
abstract public class JAMailboxImpl implements JAMailbox {

    /**
     * Mailbox logger.
     */
    protected final Logger log;

    /**
     * The factory of this mailbox.
     */
    protected final JAMailboxFactory mailboxFactory;

    /**
     * The inbox, implemented as a local queue and a concurrent queue.
     */
    protected final Inbox inbox;

    /**
     * Initial size of the outbox for each unique message destination.
     */
    private final int initialBufferSize;

    /**
     * A table of outboxes, one for each unique message destination.
     */
    protected Map<JAMailbox, ArrayDeque<Message>> sendBuffer;

    /**
     * The currently active exception handler.
     */
    private ExceptionHandler exceptionHandler;

    /**
     * The request or signal message being processed.
     */
    private Message currentMessage;

    /**
     * Create a mailbox.
     *
     * @param _factory           The factory of this object.
     * @param _inbox             The inbox.
     * @param _log               The Mailbox log.
     * @param _initialBufferSize Initial size of the outbox for each unique message destination.
     */
    public JAMailboxImpl(final JAMailboxFactory _factory,
                         final Inbox _inbox,
                         final Logger _log,
                         final int _initialBufferSize) {
        mailboxFactory = _factory;
        inbox = _inbox;
        log = _log;
        initialBufferSize = _initialBufferSize;
        _factory.addAutoClosable(this);
    }

    @Override
    public final Logger getLogger() {
        return log;
    }

    @Override
    public final Message getCurrentMessage() {
        return currentMessage;
    }

    @Override
    public final void setCurrentMessage(Message message) {
        currentMessage = message;
    }

    @Override
    public final boolean isEmpty() {
        return !inbox.isNonEmpty();
    }

    @Override
    public void close() throws Exception {
        if (sendBuffer == null)
            return;
        final Iterator<Entry<JAMailbox, ArrayDeque<Message>>> iter = sendBuffer
                .entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<JAMailbox, ArrayDeque<Message>> entry = iter.next();
            final JAMailbox target = entry.getKey();
            if (target.getMailboxFactory() != mailboxFactory) {
                final ArrayDeque<Message> messages = entry.getValue();
                iter.remove();
                target.unbufferedAddMessages(messages);
            } else
                iter.remove();
        }
        while (true) {
            final Message message = inbox.poll();
            if (message == null)
                return;
            if (message.isForeign() && message.isResponsePending())
                try {
                    message.close();
                } catch (final Throwable t) {
                }
        }
    }

    @Override
    public final ExceptionHandler setExceptionHandler(
            final ExceptionHandler _handler) {
        if (!isRunning())
            throw new IllegalStateException(
                    "Attempt to set an exception handler on an idle mailbox");
        final ExceptionHandler rv = this.exceptionHandler;
        this.exceptionHandler = _handler;
        return rv;
    }

    @Override
    public final ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public void unbufferedAddMessages(final Message _message, final boolean _local)
            throws Exception {
        if (mailboxFactory.isClosing()) {
            if (_message.isForeign() && _message.isResponsePending())
                try {
                    _message.close();
                } catch (final Throwable t) {
                }
            return;
        }
        inbox.offer(_local, _message);
        afterAdd();
    }

    @Override
    public void unbufferedAddMessages(final Queue<Message> _messages)
            throws Exception {
        if (mailboxFactory.isClosing()) {
            final Iterator<Message> itm = _messages.iterator();
            while (itm.hasNext()) {
                final Message message = itm.next();
                if (message.isForeign() && message.isResponsePending())
                    try {
                        message.close();
                    } catch (final Throwable t) {
                    }
            }
            return;
        }
        inbox.offer(_messages);
        afterAdd();
    }

    /**
     * Called after adding some message(s) to the inbox.
     */
    abstract protected void afterAdd() throws Exception;

    @Override
    public boolean buffer(final Message _message, final Mailbox _target) {
        if (mailboxFactory.isClosing())
            return false;
        ArrayDeque<Message> buffer;
        if (sendBuffer == null) {
            sendBuffer = new IdentityHashMap<JAMailbox, ArrayDeque<Message>>();
            buffer = null;
        } else {
            buffer = sendBuffer.get(_target);
        }
        if (buffer == null) {
            buffer = new ArrayDeque<Message>(initialBufferSize);
            sendBuffer.put((JAMailbox) _target, buffer);
        }
        buffer.add(_message);
        return true;
    }

    @Override
    public void run() {
        while (true) {
            final Message message = inbox.poll();
            if (message == null) {
                try {
                    onIdle();
                } catch (final MigrationException me) {
                    throw me;
                } catch (Exception e) {
                    log.error("Exception thrown by onIdle", e);
                }
                if (inbox.isNonEmpty())
                    continue;
                return;
            }
            processMessage(message);
        }
    }

    protected void processMessage(final Message message) {
        message.eval(this);
    }

    /**
     * Called when all pending messages have been processed.
     */
    abstract protected void onIdle() throws Exception;

    @Override
    public final void incomingResponse(final Message _message,
                                       final Mailbox _responseSource) {
        try {
            unbufferedAddMessages(_message, this == _responseSource ||
                    (_responseSource != null && this == _responseSource));
        } catch (final Throwable t) {
            log.error("unable to add response message", t);
        }
    }

    @Override
    public JAMailboxFactory getMailboxFactory() {
        return mailboxFactory;
    }
}