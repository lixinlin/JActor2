package org.agilewiki.jactor2.core.plant;

import java.util.concurrent.ThreadFactory;

/**
 * Base class used to create the pool threads used by reactors.
 * Created by PlantConfiguration.
 */
public final class DefaultThreadFactory implements ThreadFactory {
    /**
     * The newThread method returns a newly created PoolThread.
     *
     * @param _runnable The run method is called when the thread is started.
     */
    @Override
    public PoolThread newThread(final Runnable _runnable) {
        return new PoolThread(_runnable);
    }
}
