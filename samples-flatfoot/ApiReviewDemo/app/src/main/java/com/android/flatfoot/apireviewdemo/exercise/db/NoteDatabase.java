/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.flatfoot.apireviewdemo.exercise.db;

import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.android.flatfoot.apireviewdemo.DemoApplication;

//@Database(entities = Note.class)
abstract public class NoteDatabase extends RoomDatabase {

    private static NoteDatabase sInstance;

    public abstract NoteDao getNoteDao();

    /**
     * Gets a database instance.
     */
    public static synchronized NoteDatabase getInstance() {
        if (sInstance == null) {
            Context context = DemoApplication.context();
            sInstance = Room.databaseBuilder(context, NoteDatabase.class, "notes.db").build();
        }
        return sInstance;
    }
}
