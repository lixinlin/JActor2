package org.agilewiki.jactor2.core.processing;

import org.agilewiki.jactor2.core.BladeBase;
import org.agilewiki.jactor2.core.messaging.Event;
import org.agilewiki.jactor2.core.threading.Facility;

public class ThreadBoundMessageProcessorSample {

    public static void main(String[] args) throws Exception {

        //A facility with no threads.
        final Facility facility = new Facility(0);

        //Get a reference to the main thread.
        final Thread mainThread = Thread.currentThread();

        //Create a thread-bound processing.
        final ThreadBoundReactor boundMessageProcessor =
                new ThreadBoundReactor(facility, new Runnable() {
                    @Override
                    public void run() {
                        //Interrupt the main thread when there are messages to process
                        mainThread.interrupt();
                    }
                });

        //Create an blade that uses the thread-bound processing.
        final ThreadBoundBlade threadBoundBlade = new ThreadBoundBlade(boundMessageProcessor);

        //Pass a FinEvent signal to the blade.
        new FinEvent().signal(threadBoundBlade);

        //Process messages when this thread is interrupted.
        while (true) {
            try {
                //Wait for an interrupt.
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                //Process messages when the main thread is interrupted.
                boundMessageProcessor.run();
            }
        }
    }
}

class ThreadBoundBlade extends BladeBase {

    ThreadBoundBlade(final Reactor _reactor) throws Exception {
        initialize(_reactor);
    }

    //Print "finished" and exit when fin is called.
    void fin() throws Exception {
        System.out.println("finished");
        System.exit(0);
    }
}

//When a FinEvent is passed to an blade, the fin method is called.
class FinEvent extends Event<ThreadBoundBlade> {
    @Override
    public void processEvent(ThreadBoundBlade _targetBlade) throws Exception {
        _targetBlade.fin();
    }
}
