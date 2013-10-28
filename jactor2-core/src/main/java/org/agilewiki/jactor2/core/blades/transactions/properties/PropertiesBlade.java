package org.agilewiki.jactor2.core.blades.transactions.properties;

import org.agilewiki.jactor2.core.blades.BladeBase;
import org.agilewiki.jactor2.core.blades.transactions.*;
import org.agilewiki.jactor2.core.messages.AsyncRequest;
import org.agilewiki.jactor2.core.messages.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.reactors.IsolationReactor;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;

import java.util.Set;
import java.util.SortedMap;

public class PropertiesBlade extends BladeBase {

    final private PropertiesProcessor propertiesProcessor;

    public PropertiesBlade(final NonBlockingReactor _reactor) throws Exception {
        initialize(_reactor);
        propertiesProcessor = new PropertiesProcessor(new IsolationReactor(_reactor.getFacility()), _reactor);
    }

    public PropertiesBlade(final NonBlockingReactor _reactor, final SortedMap<String, Object> _initialState)
            throws Exception {
        initialize(_reactor);
        propertiesProcessor =
                new PropertiesProcessor(new IsolationReactor(_reactor.getFacility()), _reactor, _initialState);
    }

    public PropertiesProcessor getPropertiesProcessor() {
        return propertiesProcessor;
    }

    public AsyncRequest<Void> putAReq(final String _key, final Object _newValue) {
        return new PropertiesTransactionAReq((NonBlockingReactor) getReactor(), this) {
            @Override
            protected void evalTransaction(PropertiesWrapper _stateWrapper, AsyncResponseProcessor<Void> rp)
                    throws Exception {
                _stateWrapper.put(_key, _newValue);
                rp.processAsyncResponse(null);
            }
        };
    }

    public AsyncRequest<Void> firstPutAReq(final String _key, final Object _newValue) {
        return new PropertiesTransactionAReq((NonBlockingReactor) getReactor(), this) {
            @Override
            protected void evalTransaction(PropertiesWrapper _stateWrapper, AsyncResponseProcessor<Void> rp)
                    throws Exception {
                Object oldValue = _stateWrapper.oldReadOnlyProperties.get(_key);
                if (oldValue != null)
                    throw new UnsupportedOperationException(_key + " already has value " + oldValue);
                _stateWrapper.put(_key, _newValue);
                rp.processAsyncResponse(null);
            }
        };
    }

    public SortedMap<String, Object> getImmutableState() {
        return propertiesProcessor.getImmutableState();
    }

    public AsyncRequest<ValidationSubscription<PropertyChanges>> addValidatorAReq(
            final Validator<PropertyChanges> _validator) {
        return propertiesProcessor.addValidatorAReq(_validator);
    }

    public AsyncRequest<ChangeSubscription<PropertyChanges>> addChangeNotificationSubscriberAReq(
            final ChangeNotificationSubscriber<PropertyChanges> _changeNotificationSubscriber) {
        return propertiesProcessor.addChangeNotificationSubscriberAReq(_changeNotificationSubscriber);
    }

    /**
     * Returns a copy of the property names.
     *
     * @return A copy of the property names.
     */
    public Set<String> getPropertyNames() {
        return getImmutableState().keySet();
    }

    public SortedMap<String, Object> matchingProperties(final String _prefix) throws Exception {
        return getImmutableState().subMap(_prefix + Character.MIN_VALUE, _prefix + Character.MAX_VALUE);
    }
}
