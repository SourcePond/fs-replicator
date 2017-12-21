package ch.sourcepond.io.fssync.distributor.hazelcast.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@ObjectClassDefinition(name = "Fssync Hazelcast distributor", description = "Configuration for Hazelcast distributor",
        factoryPid = {""})
public @interface DistributorTopicConfig {

    @AttributeDefinition
    int readBatchSize() default 50;

    @AttributeDefinition
    boolean statisticsEnabled() default false;

    @AttributeDefinition
    int capacity() default 2000;

    @AttributeDefinition
    int backupCount() default 1;

    @AttributeDefinition
    int asyncBackupCount() default 0;

    @AttributeDefinition
    int timeToLiveSeconds() default 300;
}
