/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.persistence.room.TypeConverter;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Stores a set of {@link Trigger}s
 */

public class ContentUriTriggers implements Iterable<ContentUriTriggers.Trigger> {
    private final Set<Trigger> mTriggers = new HashSet<>();

    /**
     * Add a Content {@link Uri} to observe
     * @param uri {@link Uri} to observe
     * @param triggerForDescendants {@code true} if any changes in descendants cause this
     *                              {@link WorkSpec} to run
     */
    public void add(Uri uri, boolean triggerForDescendants) {
        Trigger trigger = new Trigger(uri, triggerForDescendants);
        mTriggers.add(trigger);
    }

    @NonNull
    @Override
    public Iterator<Trigger> iterator() {
        return mTriggers.iterator();
    }

    /**
     * @return number of {@link Trigger} objects
     */
    public int size() {
        return mTriggers.size();
    }

    /**
     * Converts a list of {@link Trigger}s to byte array representation
     * @param triggers the list of {@link Trigger}s to convert
     * @return corresponding byte array representation
     */
    @TypeConverter
    public static byte[] toByteArray(ContentUriTriggers triggers) {
        if (triggers.size() == 0) {
            // Return null for no triggers. Needed for SQL query check in ForegroundProcessor
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeInt(triggers.size());
            for (Trigger trigger : triggers) {
                objectOutputStream.writeUTF(trigger.getUri().toString());
                objectOutputStream.writeBoolean(trigger.shouldTriggerForDescendants());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * Converts a byte array to list of {@link Trigger}s
     * @param bytes byte array representation to convert
     * @return list of {@link Trigger}s
     */
    @TypeConverter
    public static ContentUriTriggers fromByteArray(byte[] bytes) {
        ContentUriTriggers triggers = new ContentUriTriggers();
        if (bytes == null) {
            // bytes will be null if there are no Content Uri Triggers
            return triggers;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(inputStream);
            for (int i = objectInputStream.readInt(); i > 0; i--) {
                Uri uri = Uri.parse(objectInputStream.readUTF());
                boolean triggersForDescendants = objectInputStream.readBoolean();
                triggers.add(uri, triggersForDescendants);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return triggers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentUriTriggers that = (ContentUriTriggers) o;

        return mTriggers.equals(that.mTriggers);
    }

    @Override
    public int hashCode() {
        return mTriggers.hashCode();
    }

    /**
     * Defines a content {@link Uri} trigger for a {@link WorkSpec}
     */

    public static class Trigger {
        @NonNull
        private final Uri mUri;
        private final boolean mTriggerForDescendants;

        public Trigger(@NonNull Uri uri,
                       boolean triggerForDescendants) {
            mUri = uri;
            mTriggerForDescendants = triggerForDescendants;
        }

        @NonNull
        public Uri getUri() {
            return mUri;
        }

        /**
         * @return {@code true} if trigger applies to descendants of {@link Uri} also
         */
        public boolean shouldTriggerForDescendants() {
            return mTriggerForDescendants;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Trigger trigger = (Trigger) o;

            return mTriggerForDescendants == trigger.mTriggerForDescendants
                    && mUri.equals(trigger.mUri);
        }

        @Override
        public int hashCode() {
            int result = mUri.hashCode();
            result = 31 * result + (mTriggerForDescendants ? 1 : 0);
            return result;
        }
    }
}
