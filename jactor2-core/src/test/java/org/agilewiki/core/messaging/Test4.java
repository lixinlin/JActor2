package org.agilewiki.core.messaging;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.DefaultMailboxFactory;
import org.agilewiki.jactor2.core.Mailbox;
import org.agilewiki.jactor2.core.MailboxFactory;

/**
 * Test code.
 */
public class Test4 extends TestCase {
    public void testb() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactory();
        final Mailbox mailbox = mailboxFactory.createNonBlockingMailbox();
        final Actor1 actor1 = new Actor1(mailbox);
        final Actor4 actor4 = new Actor4(mailbox);
        actor4.hi4(actor1).call();
        mailboxFactory.close();
    }

    public void testd() throws Exception {
        final MailboxFactory mailboxFactory = new DefaultMailboxFactory();
        final Actor1 actor1 = new Actor1(mailboxFactory.createAtomicMailbox());
        final Actor4 actor4 = new Actor4(mailboxFactory.createAtomicMailbox());
        actor4.hi4(actor1).call();
        mailboxFactory.close();
    }
}