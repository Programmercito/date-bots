package org.osbo.bots.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.osbo.bots.processor.MessageProcessor;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;

@Component
public class StartupRunner implements CommandLineRunner {
    @Value("${telegram.token}")
    private String telegramToken;

    @Autowired
    private MessageProcessor messageProcessor;

    @Override
    public void run(String... args) throws Exception {
        // Código que se ejecuta al iniciar la app
        TelegramBot bot = new TelegramBot(telegramToken);
        bot.setUpdatesListener(updates -> {
            for (var update : updates) {
                messageProcessor.process(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                e.response().errorCode();
                e.response().description();
            } else {
                e.printStackTrace();
            }
        });
    }
}