package fi.hsl.transitlog.hfp;

import com.typesafe.config.Config;
import fi.hsl.common.hfp.HfpData;
import fi.hsl.common.hfp.HfpParser;
import fi.hsl.common.hfp.HfpPayload;
import fi.hsl.common.hfp.HfpTopic;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.mqtt.proto.Mqtt;
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

    private final HfpParser parser = HfpParser.newInstance();

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
            if (maybeSchema.isPresent() && maybeSchema.get() == ProtobufSchema.MqttRawMessage) {
                final long timestamp = received.getEventTime();
                byte[] data = received.getData();

                Hfp.Data converted = parseData(data, timestamp);
                sendPulsarMessage(received.getMessageId(), converted, timestamp);
            }
            else {
                log.warn("Received unexpected schema, ignoring.");
                ack(received.getMessageId()); //Ack so we don't receive it again
            }
        }
        catch (Exception e) {
            log.error("Exception while handling message", e);
        }
    }

    private void ack(MessageId received) {
        consumer.acknowledgeAsync(received)
                .exceptionally(throwable -> {
                    log.error("Failed to ack Pulsar message", throwable);
                    return null;
                })
                .thenRun(() -> {});
    }

    Hfp.Data parseData(byte[] data, long timestamp) throws Exception {
        final Mqtt.RawMessage raw = Mqtt.RawMessage.parseFrom(data);
        final String rawTopic = raw.getTopic();
        final byte[] rawPayload = raw.getPayload().toByteArray();

        final HfpPayload payload = parser.parse(rawPayload);
        final Optional<HfpTopic> maybeTopic = HfpParser.parseTopic(rawTopic, timestamp);

        //TODO
        return null;
    }

    private void sendPulsarMessage(MessageId received, Hfp.Data hfp, long timestamp) {

        producer.newMessage()
                //.key(dvjId) //TODO think about this
                .eventTime(timestamp)
                .property(TransitdataProperties.KEY_PROTOBUF_SCHEMA, ProtobufSchema.HfpData.toString())
                .value(hfp.toByteArray())
                .sendAsync()
                .whenComplete((MessageId id, Throwable t) -> {
                    if (t != null) {
                        log.error("Failed to send Pulsar message", t);
                        //Should we abort?
                    }
                    else {
                        //Does this become a bottleneck? Does pulsar send more messages before we ack the previous one?
                        //If yes we need to get rid of this
                        ack(received);
                    }
                });

    }
}