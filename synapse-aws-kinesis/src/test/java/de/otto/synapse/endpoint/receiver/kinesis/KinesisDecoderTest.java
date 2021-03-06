package de.otto.synapse.endpoint.receiver.kinesis;

import de.otto.synapse.message.Key;
import de.otto.synapse.message.Message;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.model.Record;

import java.time.Instant;
import java.util.Optional;

import static de.otto.synapse.channel.ShardPosition.fromPosition;
import static de.otto.synapse.message.DefaultHeaderAttr.MSG_ARRIVAL_TS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class KinesisDecoderTest {

    private final KinesisDecoder decoder = new KinesisDecoder();

    @Test
    public void shouldBuildKinesisMessage() {
        final Instant now = Instant.now();
        final Record record = Record.builder()
                .partitionKey("42")
                .data(SdkBytes.fromString("ßome dätä",UTF_8))
                .approximateArrivalTimestamp(now)
                .sequenceNumber("00001")
                .build();
        final Message<String> message = decoder.apply(new RecordWithShard("some-shard",
                record));
        assertThat(message.getKey(), is(Key.of("42")));
        assertThat(message.getPayload(), is("ßome dätä"));
        assertThat(message.getHeader().getAsInstant(MSG_ARRIVAL_TS), is(now));
        assertThat(message.getHeader().getShardPosition(), is(Optional.of(fromPosition("some-shard", "00001"))));
    }

    @Test
    public void shouldBuildKinesisMessageV2() {
        final String json = "{\"_synapse_msg_format\":\"v2\","
                + "\"_synapse_msg_headers\":{\"attr\":\"value\"},"
                + "\"_synapse_msg_payload\":{\"some\":\"payload\"}}";

        final Instant now = Instant.now();
        final Record record = Record.builder()
                .partitionKey("42")
                .data(SdkBytes.fromString(json,UTF_8))
                .approximateArrivalTimestamp(now)
                .sequenceNumber("00001")
                .build();
        final Message<String> message = decoder.apply(new RecordWithShard(
                "some-shard",
                record));
        assertThat(message.getKey(), is(Key.of("42")));
        assertThat(message.getPayload(), is("{\"some\":\"payload\"}"));
        assertThat(message.getHeader().getAsInstant(MSG_ARRIVAL_TS), is(now));
        assertThat(message.getHeader().getShardPosition(), is(Optional.of(fromPosition("some-shard", "00001"))));
        assertThat(message.getHeader().get("attr"), is("value"));
    }

    @Test
    public void shouldBuildKinesisMessageV2WithCompoundKey() {
        final String json = "{\"_synapse_msg_format\":\"v2\","
                + "\"_synapse_msg_key\":{\"partitionKey\":\"1\",\"compactionKey\":\"2\"},"
                + "\"_synapse_msg_headers\":{\"attr\":\"value\"},"
                + "\"_synapse_msg_payload\":{\"some\":\"payload\"}}";

        final Instant now = Instant.now();
        final Record record = Record.builder()
                .partitionKey("42")
                .data(SdkBytes.fromString(json,UTF_8))
                .approximateArrivalTimestamp(now)
                .sequenceNumber("00001")
                .build();
        final Message<String> message = decoder.apply(new RecordWithShard(
                "some-shard",
                record));
        assertThat(message.getKey(), is(Key.of("1", "2")));
        assertThat(message.getPayload(), is("{\"some\":\"payload\"}"));
        assertThat(message.getHeader().getAsInstant(MSG_ARRIVAL_TS), is(now));
        assertThat(message.getHeader().getShardPosition(), is(Optional.of(fromPosition("some-shard", "00001"))));
        assertThat(message.getHeader().get("attr"), is( "value"));
    }

    @Test
    public void shouldBuildKinesisDeletionMessageV2() {
        final String json = "{\n   \"_synapse_msg_format\"  :  \"v2\",     "
                + "\"_synapse_msg_headers\":{\"attr\":\"value\"},"
                + "\"_synapse_msg_payload\":null}";

        final Instant now = Instant.now();
        final Record record = Record.builder()
                .partitionKey("42")
                .data(SdkBytes.fromString(json,UTF_8))
                .approximateArrivalTimestamp(now)
                .sequenceNumber("00001")
                .build();
        final Message<String> message = decoder.apply(new RecordWithShard(
                "some-shard",
                record));
        assertThat(message.getKey(), is(Key.of("42")));
        assertThat(message.getPayload(), is(nullValue()));
        assertThat(message.getHeader().getShardPosition(), is(Optional.of(fromPosition("some-shard", "00001"))));
        assertThat(message.getHeader().get("attr"), is("value"));
        assertThat(message.getHeader().getAsInstant(MSG_ARRIVAL_TS), is(now));
    }

    @Test
    public void shouldBuildKinesisDeletionMessageWithoutHeadersV2() {
        final String json = "{\"_synapse_msg_format\":\"v2\","
                + "\"_synapse_msg_headers\":{},"
                + "\"_synapse_msg_payload\":null}";

        final Instant now = Instant.now();
        final Record record = Record.builder()
                .partitionKey("42")
                .data(SdkBytes.fromString(json,UTF_8))
                .approximateArrivalTimestamp(now)
                .sequenceNumber("00001")
                .build();
        final Message<String> message = decoder.apply(new RecordWithShard(
                "some-shard",
                record));
        assertThat(message.getKey(), is(Key.of("42")));
        assertThat(message.getPayload(), is(nullValue()));
        assertThat(message.getHeader().getShardPosition(), is(Optional.of(fromPosition("some-shard", "00001"))));
        assertThat(message.getHeader().getAsInstant(MSG_ARRIVAL_TS), is(now));
    }

    @Test
    public void shouldBuildMinimalKinesisDeletionMessageV2() {
        final String json = "{\"_synapse_msg_format\":\"v2\"}";

        final Instant now = Instant.now();
        final Record record = Record.builder()
                .partitionKey("42")
                .data(SdkBytes.fromString(json,UTF_8))
                .approximateArrivalTimestamp(now)
                .sequenceNumber("00001")
                .build();
        final Message<String> message = decoder.apply(new RecordWithShard(
                "some-shard",
                record));
        assertThat(message.getKey(), is(Key.of("42")));
        assertThat(message.getPayload(), is(nullValue()));
        assertThat(message.getHeader().getShardPosition(), is(Optional.of(fromPosition("some-shard", "00001"))));
        assertThat(message.getHeader().getAsInstant(MSG_ARRIVAL_TS), is(now));
    }

}
