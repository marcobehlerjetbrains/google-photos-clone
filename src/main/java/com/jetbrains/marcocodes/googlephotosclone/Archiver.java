package com.jetbrains.marcocodes.googlephotosclone;

import java.util.concurrent.atomic.AtomicInteger;

public class Archiver {

    AtomicInteger counter = new AtomicInteger(0);

    private String status = "Waiting";

    public String status() {
        return status;
    }

    public double progress() {
        int i = counter.incrementAndGet();
        if (i > 10) {
            status = "Complete";
            return 1.0;
        } else {
            return (double) i / 10;
        }
    }


    public void run() {
        this.status = "Running";
    }

    public void reset() {
        this.status = "Waiting";
        this.counter = new AtomicInteger(0);
    }
}
