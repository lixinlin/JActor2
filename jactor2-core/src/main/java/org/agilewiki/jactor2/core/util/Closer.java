package org.agilewiki.jactor2.core.util;

import org.agilewiki.jactor2.core.blades.Blade;
import org.agilewiki.jactor2.core.impl.CloserImpl;

/**
 * <p>
 *     A closer is a blade that maintains a concurrent set of weak references.
 * </p>
 */
public interface Closer extends Blade, AutoCloseable {
    CloserImpl asCloserImpl();
    boolean addCloseable(final Closeable _closeable) throws Exception;
    boolean removeCloseable(final Closeable _closeable);
}
