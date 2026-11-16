package fi.hsl.transitdata.hfp;

import fi.hsl.common.pulsar.PulsarApplicationContext;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Producer;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageHandlerTest {

    @SuppressWarnings("unchecked")
    private PulsarApplicationContext context() {
        final PulsarApplicationContext context = mock(PulsarApplicationContext.class);
        when(context.getConsumer()).thenReturn(mock(Consumer.class));
        when(context.getSingleProducer()).thenReturn(mock(Producer.class));
        return context;
    }

    @Test
    public void acceptsApcAndHfpMessageTypes() {
        new MessageHandler(context(), "apc");
        new MessageHandler(context(), "hfp");
    }

    @Test
    public void rejectsUnknownMessageType() {
        assertThrows(IllegalArgumentException.class, () -> new MessageHandler(context(), "nonsense"));
    }
}
