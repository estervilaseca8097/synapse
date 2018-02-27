package de.otto.synapse;

import de.otto.synapse.annotation.EnableEventSource;
import de.otto.synapse.consumer.EventSource;

/**
 * A builder used to build {@link EventSource instances}.
 */
public interface EventSourceBuilder {

    /**
     * Should build an event source for a given stream name. Classes that implement this interface use this differently
     * depending on their specific event queuing tech.
     *
     * @param name A name taken from the {@link EnableEventSource} annotation and used to connect event sources and
     *             consumers.
     * @param streamName The name of the stream.
     * @return EventSource implementation for this specific technology
     */
    EventSource buildEventSource(final String name, final String streamName);

}