import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.util.Image;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tuckerthomas on 10/7/16.
 */

public class PizzaBot {
    private static final Logger log = LoggerFactory.getLogger(PizzaBot.class);
    private IDiscordClient client;
    private String token;
    private final AtomicBoolean reconnect = new AtomicBoolean(true);
    private IUser iconChooser;

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
            String messageContent = message.getContent();
            IUser messageAuthor = message.getAuthor();
            IChannel messageChannel = message.getChannel();
            IGuild messageGuild = message.getGuild();

            log.info("New message from " + message.getAuthor().getName() + ": " + message);

            if(message.getContent().startsWith("!chooseIconWinner")) {
                if(checkAdmin(messageAuthor, message)) {
                    pickIcon(messageChannel);
                    messageChannel.sendMessage("@" + iconChooser.getName() + " has won the drawing!");
                }else {
                    message.reply("Sorry you are not an Administrator");
                }

            }

            if(messageContent.startsWith("!setIcon")) {
                if(messageAuthor.equals(iconChooser)) {
                    if(message.getAttachments().size() != 0) {
                        List<IMessage.Attachment> attachments = message.getAttachments();
                        String imageUrl = attachments.get(0).getUrl();
                        Image image = Image.forUrl("png", imageUrl);
                        messageGuild.changeIcon(image);
                        messageChannel.sendMessage("Channel icon changed!");
                    }else {
                        messageChannel.sendMessage("You did not include a picture to change the icon to.");
                    }
                } else {
                    messageChannel.sendMessage("You are not this weeks winner.");
                }
            }

        } catch (Exception e) {
            log.warn("Could not reply to message", e);
        }

    }

    public void pickIcon (IChannel channel) {
        List<IUser> users = channel.getUsersHere();
        Random random = new Random();
        iconChooser = users.get(random.nextInt(users.size() - 1)); //Get a random user in the channel
        if(iconChooser.isBot())
            pickIcon(channel);
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

    public IUser getIconChooser() {
        return iconChooser;
    }

    public void setIconChooser(IUser iconChooser) {
        this.iconChooser = iconChooser;
    }

    public boolean checkAdmin(IUser user, IMessage message) {
        if(user.getRolesForGuild(message.getGuild()).get(0).getName().equals("PizzaBot")) {
            return true;
        }else {
            return false;
        }

    }
}
