package fi.hsl.transitlog.hfp;

import com.typesafe.config.Config;
import fi.hsl.common.pulsar.IMessageHandler;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import fi.hsl.common.transitdata.TransitdataProperties;
import fi.hsl.common.transitdata.TransitdataProperties.*;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class MessageHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private Consumer<byte[]> consumer;
    private Producer<byte[]> producer;
    private Config config;

    public MessageHandler(PulsarApplicationContext context) {
        consumer = context.getConsumer();
        producer = context.getProducer();
        this.config = context.getConfig();
    }

    private Optional<ProtobufSchema> parseProtobufSchema(Message received) {
        try {
            String schemaType = received.getProperty(TransitdataProperties.KEY_PROTOBUF_SCHEMA);
            log.debug("Received message with schema type " + schemaType);
            ProtobufSchema schema = ProtobufSchema.fromString(schemaType);
            return Optional.of(schema);
        }
        catch (Exception e) {
            log.error("Failed to parse protobuf schema", e);
            return Optional.empty();
        }
    }

    public void handleMessage(Message received) throws Exception {
        try {
            Optional<ProtobufSchema> maybeSchema = parseProtobufSchema(received);
            maybeSchema.ifPresent(schema -> {

            });

            consumer.acknowledgeAsync(received)
                    .exceptionally(throwable -> {
                        log.error("Failed to ack Pulsar message", throwable);
                        return null;
                    })
                    .thenRun(() -> {});
        }
        catch (Exception e) {
            log.error("Exception while handling message", e);
        }
    }

    private void sendPulsarMessage() {
        /*GtfsRealtime.FeedMessage feedMessage = GtfsRtFactory.newFeedMessage(dvjId, tripUpdate, tripUpdate.getTimestamp());
        producer.newMessage()
                .key(dvjId)
                .eventTime(pulsarEventTimestamp)
                .property(TransitdataProperties.KEY_PROTOBUF_SCHEMA, TransitdataProperties.ProtobufSchema.GTFS_TripUpdate.toString())
                .value(feedMessage.toByteArray())
                .sendAsync()
                .thenRun(() -> log.debug("Sending TripUpdate for dvjId {} with {} StopTimeUpdates and status {}",
                        dvjId, tripUpdate.getStopTimeUpdateCount(), tripUpdate.getTrip().getScheduleRelationship()));*/

    }
}
