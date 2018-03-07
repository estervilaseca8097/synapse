package de.otto.synapse.endpoint;

import com.google.common.collect.ImmutableList;
import de.otto.synapse.message.Message;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InterceptorChainTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBuildEmptyChain() {
        final InterceptorChain chain = InterceptorChain.emptyInterceptorChain();
        final Message message = mock(Message.class);
        final Message intercepted = chain.intercept(message);
        verifyZeroInteractions(message);
        assertThat(message, is(intercepted));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnNull() {
        final MessageInterceptor interceptor = mock(MessageInterceptor.class);
        when(interceptor.intercept(any(Message.class))).thenReturn(null);
        final InterceptorChain chain = InterceptorChain.of(ImmutableList.of(interceptor));
        assertThat(chain.intercept(someMessage("foo")), is(nullValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldStopInterceptingOnNull() {
        final MessageInterceptor first = mock(MessageInterceptor.class);
        when(first.intercept(any(Message.class))).thenReturn(null);
        final MessageInterceptor second = mock(MessageInterceptor.class);
        final InterceptorChain chain = InterceptorChain.of(ImmutableList.of(first, second));
        assertThat(chain.intercept(someMessage("foo")), is(nullValue()));
        verifyZeroInteractions(second);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnResultFromLastInterceptor() {
        final MessageInterceptor first = mock(MessageInterceptor.class);
        when(first.intercept(any(Message.class))).thenReturn(someMessage("foo"));
        final MessageInterceptor second = mock(MessageInterceptor.class);
        when(second.intercept(any(Message.class))).thenReturn(someMessage("bar"));
        final InterceptorChain chain = InterceptorChain.of(ImmutableList.of(first, second));
        //noinspection ConstantConditions
        assertThat(chain.intercept(someMessage("foo")).getKey(), is("bar"));
    }

    @Test
    public void shouldConstructDifferentInterceptorChains() {
        final MessageInterceptor first = mock(MessageInterceptor.class);
        final MessageInterceptor second = mock(MessageInterceptor.class);
        final InterceptorChain ofSingleInterceptors = InterceptorChain.of(first, second);
        final InterceptorChain ofInterceptorList = InterceptorChain.of(asList(first, second));
        final InterceptorChain ofImmutableInterceptorList = InterceptorChain.of(ImmutableList.of(first, second));
        assertThat(ofSingleInterceptors, is(ofInterceptorList));
        assertThat(ofInterceptorList, is(ofImmutableInterceptorList));
    }
    @SuppressWarnings("unchecked")
    private Message<String> someMessage(final String key) {
        return Message.message(key, null);
    }
}