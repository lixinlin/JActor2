package org.agilewiki.jactor2.core.messaging;

import org.agilewiki.jactor2.core.ActorBase;
import org.agilewiki.jactor2.core.processing.MessageProcessor;
import org.agilewiki.jactor2.core.processing.NonBlockingMessageProcessor;
import org.agilewiki.jactor2.core.threading.ModuleContext;

//Exploring the use of multiple contexts.
public class ServiceSample {

    public static void main(final String[] _args) throws Exception {

        //Application context with 1 thread.
        final ModuleContext applicationContext = new ModuleContext(1);

        //Create a service actor that uses its own context.
        Service service = new Service();

        try {
            //Test the delay echo request on the service actor.
            System.out.println(service.delayEchoAReq(1, "1 (Expected)").call());

            //close the context used by the service actor.
            service.getMessageProcessor().getModuleContext().close();
            try {
                //Try using delay echo request with the context closed.
                System.out.println(service.delayEchoAReq(1, "(Unexpected)").call());
            } catch (ServiceClosedException sce) {
                //The ServiceClosedException is now thrown because the context is closed.
                System.out.println("Exception as expected");
            }

            //Create a new service actor that uses its own context.
            service = new Service();
            //Create an application actor based on the application context
            //and with a reference to the service actor.
            final ServiceApplication serviceApplication =
                    new ServiceApplication(service, new NonBlockingMessageProcessor(applicationContext));
            //Start a delay echo service request using the application actor.
            EchoReqState echoReqState = serviceApplication.echoAReq(1, "2 (Expected)").call();
            //Print the results of the delay echo service request.
            System.out.println(serviceApplication.echoResultAReq(echoReqState).call());

            //Start a second delay echo service request using the application actor.
            EchoReqState echoReqState2 = serviceApplication.echoAReq(1, "(Unexpected)").call();
            //Close the service context while the delay echo service request is still sleeping.
            serviceApplication.closeServiceAReq().call();
            //The results should now show that an exception was thrown.
            System.out.println(serviceApplication.echoResultAReq(echoReqState2).call());
        } finally {
            service.getMessageProcessor().getModuleContext().close(); //Close the service context.
            applicationContext.close(); //Close the application context.
        }

    }
}

//A service actor that runs on its own context.
class Service extends ActorBase {

    Service() throws Exception {
        //Create a processing on a new context with 1 thread.
        initialize(new NonBlockingMessageProcessor(new ModuleContext(1)));
    }

    //Returns a delay echo request.
    AsyncRequest<String> delayEchoAReq(final int _delay, final String _text) {
        return new AsyncRequest<String>(getMessageProcessor()) {
            @Override
            public void processAsyncRequest() throws Exception {
                //Sleep a bit so that the request does not complete too quickly.
                try {
                    Thread.sleep(_delay);
                } catch (InterruptedException e) {
                    return;
                }
                //Echo the text back in the response.
                processAsyncResponse("Echo: " + _text);
            }
        };
    }

}

//Holds the state of a service application echo request.
class EchoReqState {
    //Not null when an echoResultRequest was received before
    // the result of the matching service delay echo request.
    AsyncResponseProcessor<String> responseProcessor;

    //Not null when the result of the service delay echo request is received
    //before the matching echoResultRequest.
    String response;
}

//An actor with a context that is different than the context of the service actor.
class ServiceApplication extends ActorBase {

    //The service actor, which operates in a different context.
    private final Service service;

    //Create a service application actor with a reference to a service actor.
    ServiceApplication(final Service _service, final MessageProcessor _messageProcessor) throws Exception {
        service = _service;
        initialize(_messageProcessor);
    }

    //Returns an application echo request.
    //The echo request is used to initiate a service delay echo request.
    //And the response returned by the echo request is state data needed to manage the
    //delivery of the response from the service delay echo request.
    AsyncRequest<EchoReqState> echoAReq(final int _delay, final String _text) {
        return new AsyncRequest<EchoReqState>(getMessageProcessor()) {
            @Override
            public void processAsyncRequest() throws Exception {

                //State data needed to manage the delivery of the response from
                //the service delay echo request.
                final EchoReqState echoReqState = new EchoReqState();

                //Establish an exception handler which traps a ServiceClosedException and
                //returns a notification that the exception occurred as a result.
                getMessageProcessor().setExceptionHandler(new ExceptionHandler() {
                    @Override
                    public void processException(Throwable throwable) throws Throwable {
                        if (throwable instanceof ServiceClosedException) {
                            String response = "Exception as expected";
                            if (echoReqState.responseProcessor == null) {
                                //No echo result request has yet been received,
                                //so save the response for later.
                                echoReqState.response = response;
                            } else {
                                //An echo result request has already been received,
                                //so now is the time to return the response.
                                echoReqState.responseProcessor.processAsyncResponse(response);
                            }
                        } else
                            throw throwable;
                    }
                });
                service.delayEchoAReq(_delay, _text).send(getMessageProcessor(), new AsyncResponseProcessor<String>() {
                    @Override
                    public void processAsyncResponse(String response) throws Exception {
                        if (echoReqState.responseProcessor == null) {
                            //No echo result request has yet been received,
                            //so save the response for later.
                            echoReqState.response = response;
                        } else {
                            //An echo result request has already been received,
                            //so now is the time to return the response.
                            echoReqState.responseProcessor.processAsyncResponse(response);
                        }
                    }
                });
                processAsyncResponse(echoReqState);
            }
        };
    }

    //Returns a close service request.
    AsyncRequest<Void> closeServiceAReq() {
        return new AsyncRequest<Void>(getMessageProcessor()) {
            @Override
            public void processAsyncRequest() throws Exception {
                //Close the context of the service actor.
                service.getMessageProcessor().getModuleContext().close();
                processAsyncResponse(null);
            }
        };
    }

    //Returns an echo result request.
    //An echo result request returns the response from the service delay echo request
    //associated with the given echo request state.
    AsyncRequest<String> echoResultAReq(final EchoReqState _echoReqState) {
        return new AsyncRequest<String>(getMessageProcessor()) {
            @Override
            public void processAsyncRequest() throws Exception {
                if (_echoReqState.response == null) {
                    //There is as yet no response from the associated service delay echo request,
                    //so save this request for subsequent delivery of that belated response.
                    _echoReqState.responseProcessor = this;
                } else {
                    //The response from the associated service delay echo request is already present,
                    //so return that response now.
                    processAsyncResponse(_echoReqState.response);
                }
            }
        };
    }
}