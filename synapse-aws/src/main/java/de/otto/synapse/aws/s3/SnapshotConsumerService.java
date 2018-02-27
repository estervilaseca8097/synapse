package de.otto.synapse.aws.s3;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import de.otto.synapse.consumer.DispatchingMessageConsumer;
import de.otto.synapse.consumer.StreamPosition;
import de.otto.synapse.message.Message;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

import static de.otto.synapse.message.Header.responseHeader;
import static de.otto.synapse.message.Message.message;

@Service
public class SnapshotConsumerService {

    private final JsonFactory jsonFactory = new JsonFactory();

    public <T> StreamPosition consumeSnapshot(final File latestSnapshot,
                                              final String streamName,
                                              final Predicate<Message<?>> stopCondition,
                                              final DispatchingMessageConsumer dispatchingMessageConsumer) {

        try (
                FileInputStream fileInputStream = new FileInputStream(latestSnapshot);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ZipInputStream zipInputStream = new ZipInputStream(bufferedInputStream)
        ) {
            StreamPosition shardPositions = StreamPosition.of();
            zipInputStream.getNextEntry();
            JsonParser parser = jsonFactory.createParser(zipInputStream);
            while (!parser.isClosed()) {
                JsonToken currentToken = parser.nextToken();
                if (currentToken == JsonToken.FIELD_NAME) {
                    switch (parser.getValueAsString()) {
                        case "startSequenceNumbers":
                            shardPositions = processSequenceNumbers(parser);
                            break;
                        case "data":
                            processSnapshotData(
                                    parser,
                                    shardPositions.positionOf(streamName),
                                    stopCondition,
                                    dispatchingMessageConsumer);
                            break;
                        default:
                            break;
                    }
                }
            }
            return shardPositions;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void processSnapshotData(final JsonParser parser,
                                         final String sequenceNumber,
                                         final Predicate<Message<?>> stopCondition,
                                         final DispatchingMessageConsumer dispatchingMessageConsumer) throws IOException {
        // Would be better to store event meta data together with key+value:
        final Instant arrivalTimestamp = Instant.EPOCH;
        boolean abort = false;
        while (!abort && parser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken currentToken = parser.currentToken();
            if (currentToken == JsonToken.FIELD_NAME) {
                final String key = parser.getValueAsString();
                final Message<String> message = message(
                        key,
                        responseHeader(sequenceNumber, arrivalTimestamp, null),
                        parser.nextTextValue()
                );
                dispatchingMessageConsumer.accept(message);
                abort = stopCondition.test(message);
            }
        }
    }

    private StreamPosition processSequenceNumbers(final JsonParser parser) throws IOException {
        final Map<String, String> shardPositions = new HashMap<>();

        String shardId = null;
        String sequenceNumber = null;
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken currentToken = parser.currentToken();
            switch (currentToken) {
                case FIELD_NAME:
                    switch (parser.getValueAsString()) {
                        case "shard":
                            parser.nextToken();
                            shardId = parser.getValueAsString();
                            break;
                        case "sequenceNumber":
                            parser.nextToken();
                            sequenceNumber = parser.getValueAsString();
                            break;
                        default:
                            break;
                    }
                    break;
                case END_OBJECT:
                    shardPositions.put(shardId, sequenceNumber);
                    shardId = null;
                    sequenceNumber = null;
                    break;
                default:
                    break;
            }
        }
        return StreamPosition.of(shardPositions);
    }


}