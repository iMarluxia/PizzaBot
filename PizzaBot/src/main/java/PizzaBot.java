import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.util.Image;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tuckerthomas on 10/7/16.
 */

public class PizzaBot {
    private static final Logger log = LoggerFactory.getLogger(PizzaBot.class);
    private IDiscordClient client;
    private String token;
    private final AtomicBoolean reconnect = new AtomicBoolean(true);
    //Create an array of ChannelIDs and UserIDs for the icon chooser
    private List<List<String>> iconChoosers;

    public PizzaBot() {
        try {
            //System.out.println(System.getProperty("user.dir"));
            Scanner scanner = new Scanner(new File("PizzaBot.txt"));
            token = scanner.nextLine();
            while (scanner.hasNextLine()) {
                iconChoosers = new ArrayList<List<String>>();
                String currentUser = scanner.nextLine();
                List<String> currentList = new ArrayList<String>();
                currentList.add(currentUser.split(":")[0]);
                currentList.add(currentUser.split(":")[1]);
                iconChoosers.add(currentList);
            }


        }catch (FileNotFoundException e) {
            log.warn("token file not found", e);
        }catch (NoSuchElementException e) {
            log.warn("Check PizzaBot.txt", e);
        }

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
                    for (List<String> user : iconChoosers) {
                        if (user.get(1).equals(messageGuild.getID())) { //Check that the channelIDs are equal
                            //Print UserID
                            messageChannel.sendMessage(user.get(0) + " has won the drawing!");
                        }
                    }

                }else {
                    message.reply("Sorry you are not an Administrator");
                }

            }

            if(messageContent.startsWith("!setIcon")) {
                for(List<String> user : iconChoosers) { //Search through the icon choosers and match the GuildDs and UserIDs
                    //See if the UserIDs and ChannelIDs match
                    if(message.getID().equals(user.get(0)) && message.getGuild().getID().equals(user.get(1))) {
                        if(message.getAttachments().size() != 0) {
                            List<IMessage.Attachment> attachments = message.getAttachments();
                            String imageUrl = attachments.get(0).getUrl();
                            Image image = Image.forUrl("png", imageUrl);
                            messageGuild.changeIcon(image);
                            messageChannel.sendMessage("Channel icon changed!");
                        }else {
                            messageChannel.sendMessage("You did not include a picture to change the icon to.");
                        }
                        break;
                    }
                }
                messageChannel.sendMessage("You are not this weeks winner.");
            }

        } catch (Exception e) {
            log.warn("Could not reply to message", e);
        }

    }

    public void pickIcon (IChannel channel) {
        List<IUser> users = channel.getUsersHere();
        Random random = new Random();
        IUser iconChooser = users.get(random.nextInt(users.size() - 1)); //Get a random user in the channel
        if(iconChooser.isBot())
            pickIcon(channel);
        List<String> user = new ArrayList<String>() {{
            add(iconChooser.getID());
            add(channel.getGuild().getID());
        }};
        Path file = Paths.get("PizzaBot.txt");
        try {
            //Write the UserID and ChannelID to PizzaBot.txt on line two and onwards
            // TODO: 10/9/2016 Rewrite the writing lines. Follow: http://winterbe.com/posts/2015/03/25/java8-examples-string-number-math-files/ 
            //BufferedWriter writer = new BufferedWriter(new File(file));
            Files.newBufferedWriter(file).append(channel.getGuild().getID() + ":" + iconChooser.getID());
        }catch(IOException e){
            log.warn("PizzaBot.txt not found");
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


    public boolean checkAdmin(IUser user, IMessage message) {
        if(user.getRolesForGuild(message.getGuild()).get(0).getName().equals("PizzaBot")) {
            return true;
        }else {
            return false;
        }

    }
}
