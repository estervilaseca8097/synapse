package de.otto.synapse.endpoint.sender.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.synapse.channel.selector.MessageLog;
import de.otto.synapse.channel.selector.MessageQueue;
import de.otto.synapse.channel.selector.Sqs;
import de.otto.synapse.endpoint.MessageInterceptor;
import de.otto.synapse.endpoint.MessageInterceptorRegistration;
import de.otto.synapse.endpoint.MessageInterceptorRegistry;
import de.otto.synapse.endpoint.sender.MessageSenderEndpoint;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SqsMessageSenderEndpointFactoryTest {

    @Test
    public void shouldBuildSqsMessageSender() {
        final ObjectMapper objectMapper = mock(ObjectMapper.class);
        final SqsAsyncClient sqsAsyncClient = mock(SqsAsyncClient.class);
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(completedFuture(GetQueueUrlResponse.builder().queueUrl("http://example.com").build()));

        final SqsMessageSenderEndpointFactory factory = new SqsMessageSenderEndpointFactory(new MessageInterceptorRegistry(), objectMapper, sqsAsyncClient, "test");

        final MessageSenderEndpoint sender = factory.create("foo-stream");
        assertThat(sender.getChannelName(), is("foo-stream"));
        assertThat(sender, is(instanceOf(SqsMessageSender.class)));
    }

    @Test
    public void shouldMatchMessageQueueSelectors() {
        final MessageInterceptorRegistry interceptorRegistry = mock(MessageInterceptorRegistry.class);
        final ObjectMapper objectMapper = mock(ObjectMapper.class);
        final SqsAsyncClient sqsAsyncClient = mock(SqsAsyncClient.class);

        final SqsMessageSenderEndpointFactory factory = new SqsMessageSenderEndpointFactory(new MessageInterceptorRegistry(), objectMapper, sqsAsyncClient, "test");

        assertThat(factory.matches(MessageQueue.class), is(true));
        assertThat(factory.matches(Sqs.class), is(true));
    }

    @Test
    public void shouldNotMatchMessageLogSelectors() {
        final MessageInterceptorRegistry interceptorRegistry = mock(MessageInterceptorRegistry.class);
        final ObjectMapper objectMapper = mock(ObjectMapper.class);
        final SqsAsyncClient sqsAsyncClient = mock(SqsAsyncClient.class);

        final SqsMessageSenderEndpointFactory factory = new SqsMessageSenderEndpointFactory(new MessageInterceptorRegistry(), objectMapper, sqsAsyncClient, "test");

        assertThat(factory.matches(MessageLog.class), is(false));
    }

    @Test
    public void shouldRegisterMessageInterceptor() {
        final ObjectMapper objectMapper = mock(ObjectMapper.class);
        final SqsAsyncClient sqsAsyncClient = mock(SqsAsyncClient.class);
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(completedFuture(GetQueueUrlResponse.builder().queueUrl("http://example.com").build()));

        final MessageInterceptorRegistry registry = new MessageInterceptorRegistry();
        final MessageInterceptor interceptor = mock(MessageInterceptor.class);
        registry.register(MessageInterceptorRegistration.allChannelsWith(interceptor));

        final SqsMessageSenderEndpointFactory factory = new SqsMessageSenderEndpointFactory(registry, objectMapper, sqsAsyncClient, "test");

        final MessageSenderEndpoint sender = factory.create("foo-stream");

        assertThat(sender.getInterceptorChain().getInterceptors(), contains(interceptor));
    }
}