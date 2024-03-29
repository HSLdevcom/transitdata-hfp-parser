package fi.hsl.transitdata.hfp;

import com.typesafe.config.Config;
import fi.hsl.common.config.ConfigParser;
import fi.hsl.common.pulsar.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting Hfp Parser");
        Config config = ConfigParser.createConfig();
        try (PulsarApplication app = PulsarApplication.newInstance(config)) {

            PulsarApplicationContext context = app.getContext();

            MessageHandler router = new MessageHandler(context, config.getString("application.messageType"));

            log.info("Start handling the messages");
            app.launchWithHandler(router);
        } catch (Exception e) {
            log.error("Exception at main", e);
        }
    }
}
