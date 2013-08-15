package org.agilewiki.jactor2.util;

import org.agilewiki.jactor2.core.Actor;
import org.agilewiki.jactor2.core.context.JAContext;
import org.agilewiki.jactor2.core.context.Properties;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A hierarchy of concurrent property sets.
 */
public class JAProperties extends AncestorBase implements Properties {
    /**
     * Finds an ancestor that implements Properties.
     *
     * @param child The actor whose dependency stack is searched.
     * @return An implementation of Properties, or null.
     */
    public static Properties getAncestor(final Ancestor child) {
        return (Properties) getAncestor(child, Properties.class);
    }

    /**
     * Finds an implementation of Properties.
     *
     * @param child An implementation of Properties or an actor with a Properties actor in its dependencies stack.
     * @return An implementation of Properties, or null.
     */
    public static Properties getMatch(final Ancestor child) {
        return (Properties) getMatch(child, Properties.class);
    }

    /**
     * Returns the value of a property.
     *
     * @param actor        An actor.
     * @param propertyName The name of the property.
     * @return The value or, when the property could not be found, null.
     * @throws UnsupportedOperationException Thrown when a Properties actor could not be found.
     */
    public static Object getProperty(final Actor actor,
                                     final String propertyName) throws Exception {
        Properties properties = actor.getMessageProcessor().getJAContext()
                .getProperties();
        if (properties == null)
            throw new UnsupportedOperationException("no Properties ancestor");
        return properties.getProperty(propertyName);
    }

    /**
     * Assign a value to a property.
     *
     * @param actor         An implementation of Properties or an actor with a Properties actor in its dependencies stack.
     * @param propertyName  The name of the property.
     * @param propertyValue The value to be assigned to the property.
     * @throws UnsupportedOperationException Thrown when a Properties actor could not be found.
     */
    public static void putProperty(final Actor actor,
                                   final String propertyName, final Object propertyValue)
            throws Exception {
        Properties properties = actor.getMessageProcessor().getJAContext()
                .getProperties();
        if (properties == null)
            throw new UnsupportedOperationException("no Properties ancestor");
        properties.putProperty(propertyName, propertyValue);
    }

    /**
     * Table of registered actors.
     */
    private ConcurrentSkipListMap<String, Object> properties = new ConcurrentSkipListMap<String, Object>();

    public JAProperties(final JAContext _jaContext) throws Exception {
        this(_jaContext, null);
    }

    public JAProperties(final JAContext _jaContext,
                        final Ancestor _parent) throws Exception {
        initialize(_parent);
        _jaContext.setProperties(this);
        putProperty("jaContext", _jaContext);
    }

    @Override
    public Object getProperty(final String propertyName) {
        if (properties.containsKey(propertyName))
            return properties.get(propertyName);
        Properties properties = getAncestor(this);
        if (properties == null)
            return null;
        return properties.getProperty(propertyName);
    }

    @Override
    public void putProperty(final String propertyName,
                            final Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    @Override
    public void copyTo(final Map<String, Object> _map) {
        Properties p = getAncestor(this);
        if (p != null)
            p.copyTo(_map);
        _map.putAll(properties);
    }
}