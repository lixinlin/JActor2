package org.agilewiki.jactor2.general.exceptions;

import org.agilewiki.jactor2.api.Mailbox;
import org.agilewiki.jactor2.api.Request;
import org.agilewiki.jactor2.api.ResponseProcessor;
import org.agilewiki.jactor2.api.Transport;

public class ActorE {
    private final Mailbox mailbox;

    public ActorE(final Mailbox mbox) {
        this.mailbox = mbox;
    }

    public Request<Void> throwRequest(final ActorA actorA) {
        return new Request<Void>(mailbox) {
            @Override
            public void processRequest(final Transport<Void> responseProcessor)
                    throws Exception {
                // Note: we only respond to responseProcessor if we get a
                // response to our own request, which should NOT happen.
                // Therefore, responseProcessor is NOT called.
                try {
                    actorA.throwRequest.send(mailbox,
                            new ResponseProcessor<Void>() {

                                @Override
                                public void processResponse(final Void response)
                                        throws Exception {
                                    // Should NOT happen!
                                    ((Transport) responseProcessor)
                                            .processResponse(new IllegalStateException(
                                                    "We should have never got here!"));
                                }
                            });
                } catch (final Exception e) {
                    // Make sure we also don't throw anything, which would be
                    // passed to responseProcessor as a response ...
                    e.printStackTrace();
                }
            }
        };
    }
}
