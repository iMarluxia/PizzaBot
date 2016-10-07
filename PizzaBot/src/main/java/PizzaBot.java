import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tuckerthomas on 10/7/16.
 */

public class PizzaBot {
    private static final Logger log = LoggerFactory.getLogger(PizzaBot.class);
    private IDiscordClient client;
    private String token;
    private final AtomicBoolean reconnect = new AtomicBoolean(true);

    public PizzaBot(String token) {
        this.token = token;
    }

    public void login() throws DiscordException {
        client = new ClientBuilder().withToken(token).login();
        client.getDispatcher().registerListener(this);
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.info("Ready to serve some Pizza B^)");
    }

    @EventSubscriber
    public void onDisconnect(DiscordDisconnectedEvent event) {
        if (reconnect.get()) {
            log.info("Reconnecting the Pizza");
            try {
                login();
            } catch (DiscordException e) {
                log.warn("Failed to reconnect the Pizza", e);
            }
        }
    }

    @EventSubscriber
    public void onGuildInvite(InviteReceivedEvent event) {
        try {
            event.getInvites()[0].accept();
            log.info("Invite received from " + event.toString());
        } catch (Exception e) {
            log.warn("Could not accept invite", e);
        }
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        try {
            IMessage message = event.getMessage();
            log.info("New message from " + message.getAuthor().getName() + ": " + message);
        } catch (Exception e) {
            log.warn("Could not reply to message", e);
        }

    }

    public void terminate() {
        reconnect.set(false);
        try {
            client.logout();
            log.info("****The Pizza has been served!****");
        } catch (Exception e) {
            log.warn("Failed to log out", e);
        }
    }
}
