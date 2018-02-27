package de.otto.synapse.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.synapse.EventSourceBuilder;
import de.otto.synapse.inmemory.InMemoryChannel;
import de.otto.synapse.inmemory.InMemoryEventSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;


public class InMemoryEventSourceConfiguration {

    @Bean
    public InMemoryChannel inMemoryStream() {
        return new InMemoryChannel();
    }

    @Bean
    public EventSourceBuilder inMemEventSourceBuilder(final ApplicationEventPublisher eventPublisher,
                                                        final ObjectMapper objectMapper) {
        return (name, streamName) -> new InMemoryEventSource(name, streamName, inMemoryStream(), eventPublisher, objectMapper);
    }

}