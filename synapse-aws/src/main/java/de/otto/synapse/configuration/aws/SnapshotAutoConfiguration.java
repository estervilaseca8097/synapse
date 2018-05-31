package de.otto.synapse.configuration.aws;

import de.otto.edison.aws.s3.S3Service;
import de.otto.synapse.compaction.aws.SnapshotReadService;
import de.otto.synapse.compaction.aws.SnapshotWriteService;
import de.otto.synapse.messagestore.MessageStore;
import de.otto.synapse.messagestore.MessageStoreFactory;
import de.otto.synapse.messagestore.aws.SnapshotMessageStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SnapshotProperties.class)
public class SnapshotAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SnapshotReadService snapshotReadService(final S3Service s3Service,
                                                   final SnapshotProperties snapshotProperties) {
        return new SnapshotReadService(snapshotProperties, s3Service);
    }
    @Bean
    @ConditionalOnMissingBean
    public SnapshotWriteService snapshotWriteService(final S3Service s3Service,
                                                     final SnapshotProperties snapshotProperties) {
        return new SnapshotWriteService(s3Service, snapshotProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    MessageStoreFactory<MessageStore> snapshotMessageStoreFactory(final SnapshotReadService snapshotReadService) {
        return (channelName) -> new SnapshotMessageStore(channelName, snapshotReadService);
    }

}
