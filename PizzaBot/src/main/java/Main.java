import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Created by tuckerthomas on 10/6/16.
 */

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        String token = "";
        if (args.length == 0 && token == null) {
            throw new IllegalArgumentException("Please enter token");
        }
        try {
            PizzaBot bot = new PizzaBot(token);
            bot.login();
            Scanner scan = new Scanner(System.in);

            while (scan.hasNext()) {
                if (scan.next().equals("stop")) {
                    bot.terminate();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.warn("Could not start", e);
        }
    }
}
