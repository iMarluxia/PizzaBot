package com.PizzaBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Created by tuckerthomas on 10/6/16.
 */

public class main {
    private static final Logger log = LoggerFactory.getLogger(com.PizzaBot.main.class);

    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);
        try {
            PizzaBot bot = new PizzaBot();
            bot.login();

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
