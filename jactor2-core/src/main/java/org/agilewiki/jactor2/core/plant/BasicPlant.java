package org.agilewiki.jactor2.core.plant;

import org.agilewiki.jactor2.core.blades.Blade;
import org.agilewiki.jactor2.core.blades.NonBlockingBlade;
import org.agilewiki.jactor2.core.facilities.Facility;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;

public interface BasicPlant extends NonBlockingBlade {
    Plant asPlant();
    Facility asFacility();
    PlantImpl asPlantImpl();
    boolean startedClosing();
    void close() throws Exception;
    boolean isExitOnClose();
    void exit();
    boolean isForcedExit();
    void forceExit();
}