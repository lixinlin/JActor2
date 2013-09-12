package org.agilewiki.jactor2.core;

import org.agilewiki.jactor2.core.messaging.SyncRequest;
import org.agilewiki.jactor2.core.threading.ModuleContext;

import java.io.PrintStream;
import java.util.Locale;

/**
 * An isolation actor for printing.
 * By using an isolation actor, printing is done on a different thread, along with
 * formatting. In effect, Printer implements a simple logger.
 * </p>
 * <h3>Sample Usage:</h3>
 * <pre>
 * public class PrinterSample {
 *
 *     public static void main(String[] args) throws Exception {
 *
 *         //A context with one thread.
 *         final ModuleContext moduleContext = new ModuleContext(1);
 *
 *         try {
 *
 *             //Create a Printer.
 *             Printer printer = new Printer(moduleContext);
 *
 *             //Print something.
 *             printer.printlnSReq("Hello World!").call();
 *
 *         } finally {
 *             //shutdown the context
 *             moduleContext.close();
 *         }
 *
 *     }
 * }
 * </pre>
 */
public class Printer extends IsolationActor {

    final public PrintStream printStream;

    final public Locale locale;

    /**
     * Create a Printer actor.
     *
     * @param _moduleContext A set of resources, including a thread pool, for use
     *                       by message processors and their actors.
     */
    public Printer(final ModuleContext _moduleContext) throws Exception {
        this(_moduleContext, System.out);
    }

    /**
     * Create a Printer actor.
     *
     * @param _moduleContext A set of resources, including a thread pool, for use
     *                       by message processors and their actors.
     * @param _printStream Where to print the string.
     */
    public Printer(final ModuleContext _moduleContext,
                   final PrintStream _printStream) throws Exception {
        this(_moduleContext, _printStream, null);
    }

    /**
     * Create a Printer actor.
     *
     * @param _moduleContext A set of resources, including a thread pool, for use
     *                       by message processors and their actors.
     * @param _printStream Where to print the string.
     */
    public Printer(final ModuleContext _moduleContext,
                   final PrintStream _printStream,
                   final Locale _locale) throws Exception {
        super(_moduleContext);
        printStream = _printStream;
        locale = _locale;
    }

    /**
     * A request to print a string.
     *
     * @param _string The string to be printed
     * @return The request.
     */
    public SyncRequest<Void> printlnSReq(final String _string) {
        return new SyncRequest<Void>(getMessageProcessor()) {
            @Override
            public Void processSyncRequest() throws Exception {
                System.out.println(_string);
                return null;
            }
        };
    }

    /**
     * A request to print a formated string.
     *
     * @param _format      The formatting.
     * @param _args        The data to be formatted.
     * @return The request.
     */
    public SyncRequest<Void> printSReq(final String _format,
                                       final Object... _args) {
        return new SyncRequest<Void>(getMessageProcessor()) {
            @Override
            public Void processSyncRequest() throws Exception {
                printStream.print(String.format(locale, _format, _args));
                return null;
            }
        };
    }
}
