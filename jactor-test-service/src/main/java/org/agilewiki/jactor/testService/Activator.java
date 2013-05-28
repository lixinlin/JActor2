package org.agilewiki.jactor.testService;

import org.agilewiki.jactor.api.*;
import org.agilewiki.jactor.testIface.Hello;
import org.agilewiki.jactor.util.osgi.durable.FactoriesImporter;
import org.agilewiki.jactor.util.osgi.durable.FactoryLocatorActivator;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

public class Activator extends FactoryLocatorActivator {
    private final Logger logger = LoggerFactory.getLogger(Activator.class);
    private HelloService hello;

    @Override
    public void updated(final Dictionary<String, ?> _config) throws ConfigurationException {
        Dictionary<String, ?> oldConfig = getConfig();
        hello.updated(_config);
        super.updated(_config);
        if (oldConfig == null && _config != null) {
            ServiceRegistration hsr = bundleContext.registerService(
                    Hello.class.getName(),
                    hello,
                    new Hashtable<String, String>());
        }
    }

    @Override
    protected void begin(final Transport<Void> _transport) throws Exception {
        FactoriesImporter factoriesImporter = new FactoriesImporter(getMailbox());
        factoriesImporter.startReq("jactor-util", getVersion()).send(getMailbox(), new ResponseProcessor<Void>() {
            @Override
            public void processResponse(Void response) throws Exception {
                factoryLocator.register(bundleContext);
                factoryLocator.setEssentialService(getMailboxFactory());
                hello = new HelloService(bundleContext, getMailbox());
                managedServiceRegistration();
                _transport.processResponse(null);
            }
        });
    }
}
