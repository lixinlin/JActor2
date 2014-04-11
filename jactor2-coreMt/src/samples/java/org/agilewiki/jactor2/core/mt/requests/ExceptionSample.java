package org.agilewiki.jactor2.core.mt.requests;

import org.agilewiki.jactor2.core.blades.NonBlockingBladeBase;
import org.agilewiki.jactor2.core.Plant;
import org.agilewiki.jactor2.core.requests.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.requests.ExceptionHandler;

import java.io.IOException;

public class ExceptionSample {
    public static void main(final String[] _args) throws Exception {
        new Plant();
        try {
            A a = new A();
            a.new Start().call();
        } finally {
            Plant.close();
        }
    }
}

class A extends NonBlockingBladeBase {
    class Start extends AsyncBladeRequest<Void> {
        B b = new B();

        AsyncResponseProcessor<Void> woopsResponse = new AsyncResponseProcessor<Void>() {
            @Override
            public void processAsyncResponse(Void _response) {
                System.out.println("can not get here!");
                Start.this.processAsyncResponse(null);
            }
        };

        ExceptionHandler<Void> exceptionHandler = new ExceptionHandler<Void>() {
            @Override
            public void processException(final Exception _e,
                                         final AsyncResponseProcessor<Void> _asyncResponseProcessor)
                    throws Exception {
                if (_e instanceof IOException) {
                    System.out.println("got IOException");
                    _asyncResponseProcessor.processAsyncResponse(null);
                } else
                    throw _e;
            }
        };

        @Override
        public void processAsyncRequest() {
            setExceptionHandler(exceptionHandler);
            send(b.new Woops(), woopsResponse);
        }
    }
}

class B extends NonBlockingBladeBase {
    class Woops extends AsyncBladeRequest<Void> {

        @Override
        public void processAsyncRequest() throws IOException {
            throw new IOException();
        }
    }
}
