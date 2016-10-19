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
            System.out.println(System.getProperty("user.dir"));
            Scanner scanner = new Scanner(new File("PizzaBot.txt"));
            token = scanner.nextLine();
            scanner = new Scanner(new File("IconChoosers.txt"));
            iconChoosers = new ArrayList<>();
            while (scanner.hasNextLine()) {
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
                    for (List<String> user : iconChoosers) { //Navigate through the lit of iconchoosers
                        if (user.get(1).equals(messageGuild.getID())) { //Check that the channelIDs are equal
                            //Print UserID
                            messageChannel.sendMessage(messageGuild.getUserByID(user.get(0)).getName() + " has won the " +
                                    "drawing!");
                        }
                    }

                }else {
                    message.reply("Sorry you are not an Administrator");
                }

            }

            if(messageContent.startsWith("!setIcon")) {
                boolean userNotFound = true; //Value to see if the user has been found in this search

                for(List<String> user : iconChoosers) { //Search through the icon choosers and match the GuildIDs and
                    // UserIDs
                    //See if the UserIDs and ChannelIDs match
                    if(messageAuthor.getID().equals(user.get(0)) && messageGuild.getID().equals(user.get(1))) {
                        // Check if they included an attachment
                        if(message.getAttachments().size() != 0) {
                            List<IMessage.Attachment> attachments = message.getAttachments();
                            String imageUrl = attachments.get(0).getUrl();
                            Image image = Image.forUrl("png", imageUrl);
                            messageGuild.changeIcon(image);
                            messageChannel.sendMessage("Channel icon changed!");
                            //Check if its a URL starting with http.
                        } else if(messageContent.substring(9).startsWith("http")) { //9 characters after "!setIcon "
                            Image image = Image.forUrl("png", messageContent.substring(9));
                            messageGuild.changeIcon(image);
                            messageChannel.sendMessage("Channel icon changed!");
                        } else{
                            messageChannel.sendMessage("You did not include a picture to change the icon to.");
                        }
                        userNotFound = false; //The user has been found and the icon has been changed, no need for a
                        // message.
                        break;
                    }
                }

                if (userNotFound) { //If the user has not been found, they are not a winner :(
                    messageChannel.sendMessage("You are not this weeks winner.");
                }

            }

            if (messageContent.startsWith("!getUserID")) {
                message.reply(messageAuthor.getID());
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

        List<String> newChooser = new ArrayList<String>() {{
            add(iconChooser.getID());
            add(channel.getGuild().getID());
        }};

        int x = 0;
        if (iconChoosers.size() > 0) {  //Find the user in the list of users and guilds and remove them
            for (List<String> user : iconChoosers) {
                if (user.get(1).equals(newChooser.get(1))) {
                    iconChoosers.remove(x);
                    break;
                }
                x++;
            }
        }
        iconChoosers.add(newChooser);



        try {
            Path file = Paths.get("IconChoosers.txt");
            List<String> lines = Files.readAllLines(file);
            int y = 0; //Start a counter for the line number
            boolean newGuild = true;
            for(String line : lines) {
                String userID = line.split(":")[0];
                String guildID = line.split(":")[1];
                if (guildID.equals(channel.getGuild().getID())) {
                    lines.set(y, iconChooser.getID() + ":" + channel.getGuild().getID());
                    //Replace the line number with new user
                    newGuild = false;
                    break;
                }
                y++;
            }
            if (newGuild) {
                lines.add(iconChooser.getID() + ":" + channel.getGuild().getID());
            }
            Files.write(file, lines);
        }catch(IOException e){
            log.warn("IconChoosers.txt not found");
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
            //PizzaBot being the admin role
            return true;
        }else {
            return false;
        }

    }

}
