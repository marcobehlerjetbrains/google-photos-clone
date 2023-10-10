package com.jetbrains.marcocodes.googlephotosclone;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Chello {

    @JmsListener(destination = "media")
    public void receiveMessage(String string) {
        System.err.println("=============================> Received <" + string + ">");
    }

}
