package fi.hsl.transitdata.hfp;


import com.google.protobuf.GeneratedMessageV3;
import fi.hsl.common.hfp.HfpJson;
import fi.hsl.common.hfp.HfpParser;
import fi.hsl.common.hfp.proto.Hfp;
import fi.hsl.common.mqtt.proto.Mqtt;
import fi.hsl.common.passengercount.PassengerCountParser;
import fi.hsl.common.passengercount.json.APCJson;
import fi.hsl.common.passengercount.proto.PassengerCount;
import fi.hsl.common.pulsar.IMessageHandler;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import fi.hsl.common.transitdata.TransitdataProperties;
import fi.hsl.common.transitdata.TransitdataSchema;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private final Consumer<byte[]> consumer;
    private final Producer<byte[]> hfpProducer;
    private final Producer<byte[]> passengerCountProducer;

    private static final String PASSENGER_COUNT_MQTT_TOPIC = "";
    private final HfpParser hfpParser = HfpParser.newInstance();
    private final PassengerCountParser passengerCountParser = PassengerCountParser.newInstance();

    public MessageHandler(PulsarApplicationContext context) {
        consumer = context.getConsumer();
        hfpProducer = context.getProducers().get("hfp-data");
        passengerCountProducer = context.getProducers().get("passenger-count");
    }

    public void handleMessage(Message received) {
        try {
            if (TransitdataSchema.hasProtobufSchema(received, TransitdataProperties.ProtobufSchema.MqttRawMessage)) {
                final long timestamp = received.getEventTime();
                byte[] data = received.getData();
                if(PASSENGER_COUNT_MQTT_TOPIC.equals(received.getTopicName())){
                    PassengerCount.Data converted = parsePassengerCountData(data);
                    sendPulsarMessage(received.getMessageId(), converted, timestamp, TransitdataProperties.ProtobufSchema.PassengerCount.toString(), passengerCountProducer);
                }
                else{
                    Hfp.Data converted = parseHfpData(data, timestamp);
                    sendPulsarMessage(received.getMessageId(), converted, timestamp,  TransitdataProperties.ProtobufSchema.HfpData.toString(), hfpProducer);
                }

            }
            else {
                log.warn("Received unexpected schema, ignoring.");
                ack(received.getMessageId()); //Ack so we don't receive it again
            }
        } catch (HfpParser.InvalidHfpTopicException | HfpParser.InvalidHfpPayloadException invalidHfpException) {
            log.warn("Failed to parse HFP data", invalidHfpException);
            //Ack messages with invalid data so they don't fill Pulsar backlog
            ack(received.getMessageId());
        } catch (Exception e) {
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


    PassengerCount.Data parsePassengerCountData(byte[] data) throws IOException, PassengerCountParser.InvalidAPCPayloadException {
        final Mqtt.RawMessage raw = Mqtt.RawMessage.parseFrom(data);
        final String rawTopic = raw.getTopic();
        final byte[] rawPayload = raw.getPayload().toByteArray();
        final APCJson apcJson = passengerCountParser.parseJson(rawPayload);
        PassengerCount.Payload payload = PassengerCountParser.newInstance().parsePayload(apcJson);

        PassengerCount.Data.Builder builder = PassengerCount.Data.newBuilder();
        builder.setPayload(payload);
        builder.setTopic(rawTopic);// Maybe we need additional logic for this

        return builder.build();
    }

    Hfp.Data parseHfpData(byte[] data, long timestamp) throws IOException, HfpParser.InvalidHfpPayloadException, HfpParser.InvalidHfpTopicException {
        final Mqtt.RawMessage raw = Mqtt.RawMessage.parseFrom(data);
        final String rawTopic = raw.getTopic();
        final byte[] rawPayload = raw.getPayload().toByteArray();

        final HfpJson jsonPayload = hfpParser.parseJson(rawPayload);
        Hfp.Payload payload = HfpParser.parsePayload(jsonPayload);
        Hfp.Topic topic = HfpParser.parseTopic(rawTopic, timestamp);

        Hfp.Data.Builder builder = Hfp.Data.newBuilder();
        builder.setSchemaVersion(builder.getSchemaVersion())
                .setPayload(payload)
                .setTopic(topic);
        return builder.build();
    }

    private void sendPulsarMessage(MessageId received, GeneratedMessageV3 message, long timestamp, String protobufSchema, Producer<byte[]> producer) {
        producer.newMessage()
            //.key(dvjId) //TODO think about this
            .eventTime(timestamp)
            .property(TransitdataProperties.KEY_PROTOBUF_SCHEMA, protobufSchema)
            .value(message.toByteArray())
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
