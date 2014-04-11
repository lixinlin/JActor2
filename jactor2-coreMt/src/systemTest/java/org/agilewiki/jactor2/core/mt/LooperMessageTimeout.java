package org.agilewiki.jactor2.core.mt;

import org.agilewiki.jactor2.core.Plant;

public class LooperMessageTimeout {
    static public void main(final String[] _args) throws Exception {
        new Plant();
        try {
            Hanger hanger = new Hanger();
            hanger.looperSReq().signal();
            Thread.sleep(Long.MAX_VALUE);
        } finally {
            Plant.close();
        }
    }
}
