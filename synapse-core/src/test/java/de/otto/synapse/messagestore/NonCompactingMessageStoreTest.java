package de.otto.synapse.messagestore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static de.otto.synapse.channel.ChannelPosition.shardPosition;
import static de.otto.synapse.message.Header.responseHeader;
import static de.otto.synapse.message.Message.message;
import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)

/**
 * Tests specific for all non-compacting MessageStore implementations
 */
public class NonCompactingMessageStoreTest {

    @Parameterized.Parameters
    public static Iterable<? extends Supplier<MessageStore>> messageStores() {
        return asList(
                InMemoryMessageStore::new
        );
    }

    @Parameter
    public Supplier<MessageStore> messageStoreBuilder;

    @Test
    public void shouldCalculateUncompactedChannelPositions() {
        final MessageStore messageStore = messageStoreBuilder.get();
        final ExecutorService executorService = newFixedThreadPool(10);
        final CompletableFuture[] completion = new CompletableFuture[5];
        for (int shard=0; shard<5; ++shard) {
            final String shardId = valueOf(shard);
            completion[shard] = CompletableFuture.runAsync(() -> {
                for (int pos = 0; pos < 10000; ++pos) {
                    messageStore.add(message(valueOf(pos), responseHeader(shardPosition("shard-" + shardId, valueOf(pos)), now()), "some payload"));
                    assertThat(messageStore.getLatestChannelPosition().positionOf("shard-" + shardId), is(valueOf(pos)));
                }

            }, executorService);
        };
        allOf(completion).join();
        assertThat(messageStore.getLatestChannelPosition().shards(), containsInAnyOrder("shard-0", "shard-1", "shard-2", "shard-3", "shard-4"));
        assertThat(messageStore.getLatestChannelPosition().positionOf("shard-0"), is("9999"));
        assertThat(messageStore.getLatestChannelPosition().positionOf("shard-1"), is("9999"));
        assertThat(messageStore.getLatestChannelPosition().positionOf("shard-2"), is("9999"));
        assertThat(messageStore.getLatestChannelPosition().positionOf("shard-3"), is("9999"));
        assertThat(messageStore.getLatestChannelPosition().positionOf("shard-4"), is("9999"));
        assertThat(messageStore.size(), is(50000));
    }
}