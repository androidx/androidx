package com.android.flatfoot.apireviewdemo.exercise;

import android.support.annotation.NonNull;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteWebService {

    interface Callback {
        void success(@NonNull Note note);

        void failure(String errorMsg);
    }

    public static void loadNoteWithId(int id, Callback callback) {
        InternalNoteWebService.load(id, callback);
    }

}

// IMPLEMENTATION DETAILS

class InternalNoteWebService {

    static ExecutorService sService = Executors.newSingleThreadExecutor();
    static Random sRandom = new Random(261);

    private static final String[] WILDE = new String[]{
            "I am so clever that sometimes I don't understand a single word of what I am saying",
            "I don't want to go to heaven. None of my friends are there",
            "I am not young enough to know everything.",
            "Quotation is a serviceable substitute for wit."
    };

    static void load(final int id, final NoteWebService.Callback callback) {
        sService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (sRandom.nextInt(10) == 0) {
                    callback.failure("Ooops! Something went wrong.");
                } else {
                    Note note = new Note(id, "Label " + id, WILDE[id % WILDE.length]);
                    callback.success(note);
                }
            }
        });
    }
}