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
    private List<IUser> eligableUsers;
    private List<IUser> oldEligableUsers;
    private List<IUser> winners;

    public PizzaBot() {
        try {
            System.out.println(System.getProperty("user.dir"));
            Scanner scanner = new Scanner(new File("PizzaBot.txt"));
            token = scanner.nextLine();
            scanner = new Scanner(new File("IconChoosers.txt"));
            iconChoosers = new ArrayList<>();
            winners = new ArrayList<>();
            while (scanner.hasNextLine()) {
                String currentUser = scanner.nextLine();
                List<String> currentList = new ArrayList<String>();
                currentList.add(currentUser.split(":")[0]);
                currentList.add(currentUser.split(":")[1]);
                iconChoosers.add(currentList);
            }
            eligableUsers = new ArrayList<>();


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


    //Bots cannot accept normal invites apparently.
    /*@EventSubscriber
    public void onGuildInvite(InviteReceivedEvent event) {
        try {
            event.getInvites()[0].accept();
            log.info("Invite received from " + event.toString());
        } catch (Exception e) {
            log.warn("Could not accept invite", e);
        }
    }*/

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        try {
            IMessage message = event.getMessage();
            String messageContent = message.getContent();
            IUser messageAuthor = message.getAuthor();
            IChannel messageChannel = message.getChannel();
            IGuild messageGuild = message.getGuild();

            //Uncomment if you want to see the messages from each event
            //log.info("New message from " + messageGuild.getName() + ":" + messageAuthor.getName() + ":" + message);

            if(message.getContent().startsWith("!chooseIconWinner")) {
                if(checkAdmin(messageAuthor, message)) {
                    if(eligableUsers.size() > 0) {
                        pickIcon(messageChannel);
                        for (List<String> user : iconChoosers) { //Navigate through the lit of iconchoosers
                            if (user.get(1).equals(messageGuild.getID())) { //Check that the channelIDs are equal
                                //Print UserID
                                messageChannel.sendMessage(messageGuild.getUserByID(user.get(0)).getName() + " has won the " +
                                        "drawing!");
                            }
                        }
                    }else {
                        message.reply("There are no users to pick");
                    }
                }else {
                    message.reply("Sorry you are not an Administrator");
                }

            }

            if(messageContent.startsWith("!setNewIcon")) {
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
                            //// TODO: 10/20/2016 Find new way to remove the start of a command
                        } else if(messageContent.substring(12).startsWith("http")) { //12 characters after "!setNewIcon "
                            Image image = Image.forUrl("png", messageContent.substring(12));
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
                if (message.getMentions().size() > 0) {
                    List<IUser> mentions = message.getMentions();
                    String content = "";
                    Collections.reverse(mentions);
                    for (IUser user : mentions) {
                        content += user.getID() + ", ";
                    }
                    message.reply(content);
                } else {
                    message.reply(messageAuthor.getID());
                }
            }

            if (messageContent.startsWith("!setIconChooser")) {
                if (checkAdmin(messageAuthor, message)) {
                    if (message.getMentions().size() == 0) {
                        message.reply("You did not specify a new winner");
                    } else if (message.getMentions().size() > 1) {
                        message.reply("You have specified too many users");
                    } else if (message.getMentions().size() == 1) {
                        changeWinner(message.getMentions().get(0), messageGuild);
                        message.reply("You have successfully set the new user.");
                    }
                } else {
                    message.reply("You are not an Administrator");
                }
            }

            if (messageContent.startsWith("!joinIcon")) {
                if (eligableUsers.size() == 0 && winners.size() == 0) {
                    eligableUsers.add(messageAuthor);
                    message.reply("has been added to the list!");
                } else {
                    boolean userFound = false;
                    boolean isWinner = false;
                    for (IUser winner : winners) {
                        if (messageAuthor.getName().equals(winner.getName())) {
                            isWinner = true;
                            break;
                        }
                    }
                    if (!isWinner) {
                        for (IUser nonwinner : eligableUsers) {
                            if (messageAuthor.getName().equals(nonwinner.getName())) {
                                userFound = true;
                                break;
                            }
                        }
                        if (userFound) {
                            message.reply("you are already on the list");
                        } else {
                            eligableUsers.add(messageAuthor);
                            message.reply("you have been added to the list");
                        }
                    } else {
                        message.reply("you have already won.");
                    }
                }

            }

            if (messageContent.startsWith("!revertIconList")) {
                if (checkAdmin(messageAuthor, message)) {
                    eligableUsers = oldEligableUsers;
                    message.reply("the list has been reverted.");
                }else {
                    message.reply("You are not an Administrator");
                }

            }

            if (messageContent.startsWith("!iconHelp")) {
                messageChannel.sendMessage(
                        "!joinIcon   to join the list of available icon choosers\n" +
                        "!setIcon    to set the icon if you are the winner. You can attach an image or send a link to one.");
            }

            if (messageContent.equals("/o/")) {
                messageChannel.sendMessage("\\o\\");
            }
            if (messageContent.equals("\\o\\")) {
                messageChannel.sendMessage("/o/");
            }

            if (messageContent.equals("!help")) {
                messageChannel.sendMessage("Information about my commands has been sent to you. Check your PM's");
                messageAuthor.getOrCreatePMChannel().sendMessage("" +
                        "```Commands:\t\t\tDescription\n" +
                        "~~~~~~~~~~~~~~~~~~~~~~~~\t-------------------------\n" +
                        "!chooseIconWinner\t\tPick the new Icon winner\n" +
                        "!setNewIcon\t\t\tAs the winner, choose the new icon by attaching an image or sending a link to an image\n" +
                        "!getUserID\t\t\tGet a user id, mention a user for their ID as well\n" +
                        "!setIconChooser\t\t\tManually set the icon chooser with a mention\n" +
                        "!joinIcon\t\t\tJoin the list of available icon choosers\n" +
                        "!revertIconList\t\t\tSomeone fucked up, revert the new list to the previous one```");
            }

            if (messageContent.startsWith("!getIconList")) {
                String listOfUsers = "";
                for (IUser user : eligableUsers) {
                    listOfUsers += user.getName() + "\n";
                }
                messageChannel.sendMessage("The current list of eligible icon choosers:\n" + listOfUsers);
            }

            if (messageContent.startsWith("!getWinners")) {
                String listOfUsers = "";
                for (IUser user : winners) {
                    listOfUsers += user.getName() + "\n";
                }
                messageChannel.sendMessage("The current list of old icon choosers:\n" + listOfUsers);
            }

            if (messageContent.startsWith("!resetWinners")) {
                if(checkAdmin(messageAuthor, message)) {
                    winners = new ArrayList<>();
                    message.reply("the list of winners has been reset.");
                }else {
                    message.reply("you are not an Administrator.");
                }
            }
        } catch (Exception e) {
            log.warn("Could not reply to message", e);
        }

    }

    public void pickIcon (IChannel channel) {
        Random random = new Random();
        IUser iconChooser;
        if(eligableUsers.size() > 1) {
            iconChooser = eligableUsers.get(random.nextInt(eligableUsers.size() - 1)); //Get a random user in the channel
        } else {
            iconChooser = eligableUsers.get(0);
        }

        oldEligableUsers = eligableUsers;
        eligableUsers = new ArrayList<>(); //reset the list

        if(iconChooser.isBot())
            pickIcon(channel);

        changeWinner(iconChooser, channel.getGuild());
    }

    public void changeWinner(IUser user, IGuild guild) {

        List<String> newChooser = new ArrayList<String>() {{
            add(user.getID());
            add(guild.getID());
        }};

        //Find the old user in the list of users/guilds and remove them
        int x = 0;
        if (iconChoosers.size() > 0) {
            for (List<String> oldUser : iconChoosers) {
                if (oldUser.get(1).equals(newChooser.get(1))) {
                    iconChoosers.remove(x);
                    break;
                }
                x++;
            }
        }
        //Add the new user
        iconChoosers.add(newChooser);

        //Write the new user to IconChoosers.txt
        try {
            Path file = Paths.get("IconChoosers.txt");
            List<String> lines = Files.readAllLines(file);
            int y = 0; //Start a counter for the line number
            boolean newGuild = true;
            for(String line : lines) {
                String userID = line.split(":")[0];
                String guildID = line.split(":")[1];
                if (guildID.equals(guild.getID())) {
                    lines.set(y, user.getID() + ":" + guild.getID());
                    //Replace the line number with new user
                    newGuild = false;
                    break;
                }
                y++;
            }
            if (newGuild) {
                lines.add(user.getID() + ":" + guild.getID());
            }
            Files.write(file, lines);
        }catch(IOException e){
            log.warn("IconChoosers.txt not found");
        }

        //Add them to the winners list
        winners.add(user);
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
        for (IRole role : user.getRolesForGuild(message.getGuild())) {
            if(role.getName().equals("Moderators")) {
                //PizzaBot is the name of the admin role
                return true;
            }
        }
        return false;
    }

}
