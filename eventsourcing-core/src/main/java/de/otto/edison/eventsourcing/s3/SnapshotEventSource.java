package de.otto.edison.eventsourcing.s3;

import de.otto.edison.eventsourcing.consumer.Event;
import de.otto.edison.eventsourcing.consumer.EventConsumer;
import de.otto.edison.eventsourcing.consumer.EventSource;
import de.otto.edison.eventsourcing.consumer.StreamPosition;
import org.slf4j.Logger;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.nio.file.Files.delete;
import static org.slf4j.LoggerFactory.getLogger;

public class SnapshotEventSource<T> implements EventSource<T> {

    private static final Logger LOG = getLogger(SnapshotEventSource.class);

    private final SnapshotService snapshotService;
    private final String name;
    private final Class<T> payloadType;

    public SnapshotEventSource(final String name,
                               final SnapshotService snapshotService,
                               final Class<T> payloadType) {
        this.name = name;
        this.snapshotService = snapshotService;
        this.payloadType = payloadType;
    }

    /**
     * Returns the name of the EventSource.
     * <p>
     * For streaming event-sources, this is the name of the event stream.
     * </p>
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Consumes all events from the EventSource, beginning with {@link StreamPosition startFrom}, until
     * the {@link Predicate stopCondition} is met.
     * <p>
     * The {@link EventConsumer consumer} will be called zero or more times, depending on
     * the number of events retrieved from the EventSource.
     * </p>
     *
     * @param startFrom     the read position returned from earlier executions
     * @param stopCondition the predicate used as a stop condition
     * @param consumer      consumer used to process events
     * @return the new read position
     */
    @Override
    public StreamPosition consumeAll(final StreamPosition startFrom,
                                     final Predicate<Event<T>> stopCondition,
                                     final EventConsumer<T> consumer) {
        // TODO: startFrom is ignored. the source should ignore / drop all events until startFrom is reached.

        consumer.init(name);
        Optional<File> latestSnapshot = Optional.empty();
        try {
            latestSnapshot = downloadLatestSnapshot();
            LOG.info("Downloaded Snapshot");

            final StreamPosition readPosition = snapshotService.consumeSnapshot(latestSnapshot, name, stopCondition, consumer, payloadType);
            LOG.info("Read Snapshot into Memory");

            deleteSnapshotFile(latestSnapshot);
            consumer.completed(name);
            return readPosition;
        } catch (final IOException | S3Exception e) {
            LOG.warn("Unable to load snapshot: {}", e.getMessage());
            deleteSnapshotFile(latestSnapshot);
            consumer.aborted(name);
            //throw new EventSourceException("Error consuming Events from snapshot EventSource: " + e.getMessage(), e);
            return StreamPosition.of();
        }
    }

    private Optional<File> downloadLatestSnapshot() throws IOException {
        LOG.info("Start downloading snapshot from S3");
        infoDiskUsage();

        Optional<File> latestSnapshot = snapshotService.getLatestSnapshotFromBucket(name);
        if (latestSnapshot.isPresent()) {
            LOG.info("Finished downloading snapshot {}", latestSnapshot.get().getName());
            infoDiskUsage();
        } else {
            LOG.warn("No snapshot found.");
        }
        return latestSnapshot;
    }

    private void deleteSnapshotFile(final Optional<File> latestSnapshot) {
        latestSnapshot.ifPresent(snapshotFile -> {
            final boolean success = snapshotService.deleteDownloadedSnapshot(snapshotFile);
            if (success) {
                LOG.info(format("deleted %s", snapshotFile.getName()));
            } else {
                LOG.warn(format("deletion of %s failed", snapshotFile.getName()));
            }
            infoDiskUsage();
        });
    }

    private void infoDiskUsage() {
        File file = null;
        try {
            file = File.createTempFile("tempFileForDiskUsage", ".txt");
            float usableSpace = (float) file.getUsableSpace() / 1024 / 1024 / 1024;
            float freeSpace = (float) file.getFreeSpace() / 1024 / 1024 / 1024;
            LOG.info(format("Available DiskSpace: usable %.3f GB / free %.3f GB", usableSpace, freeSpace));
        } catch (IOException e) {
            LOG.info("Error calculating disk usage: " + e.getMessage());
        } finally {
            try {
                if (file != null) {
                    delete(file.toPath());
                }
            } catch (IOException e) {
                LOG.error("Error deleting temp file while calculating disk usage:" + e.getMessage());
            }
        }
    }

}
