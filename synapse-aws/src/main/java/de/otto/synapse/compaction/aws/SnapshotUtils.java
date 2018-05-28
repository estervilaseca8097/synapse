package de.otto.synapse.compaction.aws;

public class SnapshotUtils {

    public static final String COMPACTION_FILE_EXTENSION = ".json.zip";

    public static String getSnapshotFileNamePrefix(String channelName) {
        return String.format("compaction-%s-snapshot-", channelName);
    }

}