/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media2.SessionCommand2.COMMAND_CODE_CUSTOM;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of {@link SessionCommand2} which represents a command group.
 */
@VersionedParcelize
public final class SessionCommandGroup2 implements VersionedParcelable {
    private static final String TAG = "SessionCommandGroup2";
    private static final String KEY_COMMANDS = "android.media.session2.commandgroup.commands";

    @ParcelField(1)
    Set<SessionCommand2> mCommands = new HashSet<>();

    /**
     * Default Constructor.
     */
    public SessionCommandGroup2() { }

    /**
     * Creates a new SessionCommandGroup2 with commands copied from another object.
     *
     * @param commands The collection of commands to copy.
     */
    public SessionCommandGroup2(@Nullable Collection<SessionCommand2> commands) {
        if (commands != null) {
            mCommands.addAll(commands);
        }
    }

    /**
     * Adds a command to this command group.
     *
     * @param command A command to add. Shouldn't be {@code null}.
     * @hide TODO remove this method
     */
    public void addCommand(@NonNull SessionCommand2 command) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        if (!hasCommand(command)) {
            mCommands.add(command);
        }
    }

    /**
     * Adds a predefined command with given {@code commandCode} to this command group.
     *
     * @param commandCode A command code to add.
     *                    Shouldn't be {@link SessionCommand2#COMMAND_CODE_CUSTOM}.
     * @hide TODO remove this method
     */
    public void addCommand(int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException(
                    "Use addCommand(SessionCommand2) for COMMAND_CODE_CUSTOM.");
        }
        if (!hasCommand(commandCode)) {
            mCommands.add(new SessionCommand2(commandCode));
        }
    }

    /**
     * Checks whether this command group has a command that matches given {@code command}.
     *
     * @param command A command to find. Shouldn't be {@code null}.
     */
    public boolean hasCommand(@NonNull SessionCommand2 command) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        return mCommands.contains(command);
    }

    /**
     * Checks whether this command group has a command that matches given {@code commandCode}.
     *
     * @param commandCode A command code to find.
     *                    Shouldn't be {@link SessionCommand2#COMMAND_CODE_CUSTOM}.
     */
    public boolean hasCommand(int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("Use hasCommand(Command) for custom command");
        }
        for (SessionCommand2 command : mCommands) {
            if (command.getCommandCode() == commandCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all commands of this command group.
     */
    public @NonNull Set<SessionCommand2> getCommands() {
        return new HashSet<>(mCommands);
    }

    /**
     * @return A new {@link Bundle} instance from the SessionCommandGroup2.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public @NonNull Bundle toBundle() {
        ArrayList<Bundle> list = new ArrayList<>();
        for (SessionCommand2 command : mCommands) {
            list.add(command.toBundle());
        }
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(KEY_COMMANDS, list);
        return bundle;
    }

    /**
     * @return A new {@link SessionCommandGroup2} instance from the bundle.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static @Nullable SessionCommandGroup2 fromBundle(Bundle commands) {
        if (commands == null) {
            return null;
        }
        List<Parcelable> list = commands.getParcelableArrayList(KEY_COMMANDS);
        if (list == null) {
            return null;
        }
        SessionCommandGroup2 commandGroup = new SessionCommandGroup2();
        for (int i = 0; i < list.size(); i++) {
            Parcelable parcelable = list.get(i);
            if (!(parcelable instanceof Bundle)) {
                continue;
            }
            Bundle commandBundle = (Bundle) parcelable;
            SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
            if (command != null) {
                commandGroup.addCommand(command);
            }
        }
        return commandGroup;
    }

    /**
     * Builds a {@link SessionCommandGroup2} object.
     */
    public static final class Builder {
        private Set<SessionCommand2> mCommands;

        public Builder() {
            mCommands = new HashSet<>();
        }

        /**
         * Creates a new builder for {@link SessionCommandGroup2} with commands copied from another
         * {@link SessionCommandGroup2} object.
         * @param commandGroup
         */
        public Builder(SessionCommandGroup2 commandGroup) {
            mCommands = commandGroup.getCommands();
        }

        /**
         * Adds a command to this command group.
         *
         * @param command A command to add. Shouldn't be {@code null}.
         */
        public @NonNull Builder addCommand(SessionCommand2 command) {
            if (command == null) {
                throw new IllegalArgumentException("command shouldn't be null");
            }
            mCommands.add(command);
            return this;
        }

        /**
         * Adds a predefined command with given {@code commandCode} to this command group.
         *
         * @param commandCode A command code to add.
         *                    Shouldn't be {@link SessionCommand2#COMMAND_CODE_CUSTOM}.
         */
        public @NonNull Builder addCommand(int commandCode) {
            if (commandCode == COMMAND_CODE_CUSTOM) {
                throw new IllegalArgumentException(
                        "Use addCommand(SessionCommand2) for COMMAND_CODE_CUSTOM.");
            }
            mCommands.add(new SessionCommand2(commandCode));
            return this;
        }

        /**
         * Adds all predefined commands to this command group.
         */
        public @NonNull Builder addAllPredefinedCommands() {
            addAllPlaybackCommands();
            addAllPlaylistCommands();
            addAllVolumeCommands();
            addAllSessionCommands();
            addAllLibraryCommands();
            return this;
        }

        /**
         * Removes a command from this group which matches given {@code command}.
         *
         * @param command A command to find. Shouldn't be {@code null}.
         */
        public @NonNull Builder removeCommand(@NonNull SessionCommand2 command) {
            if (command == null) {
                throw new IllegalArgumentException("command shouldn't be null");
            }
            mCommands.remove(command);
            return this;
        }

        /**
         * Removes a command from this group which matches given {@code commandCode}.
         *
         * @param commandCode A command code to find.
         *                    Shouldn't be {@link SessionCommand2#COMMAND_CODE_CUSTOM}.
         */
        public @NonNull Builder removeCommand(int commandCode) {
            if (commandCode == COMMAND_CODE_CUSTOM) {
                throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
            }
            mCommands.remove(new SessionCommand2(commandCode));
            return this;
        }

        @NonNull Builder addAllPlaybackCommands() {
            addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_RESET);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_SET_SPEED);
            return this;
        }

        @NonNull Builder addAllPlaylistCommands() {
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_GET_CURRENT_MEDIA_ITEM);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_GET_LIST_METADATA);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_REPLACE_ITEM);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST_METADATA);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM);
            addCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM);
            return this;
        }

        @NonNull Builder addAllVolumeCommands() {
            addCommand(SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME);
            addCommand(SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME);
            return this;
        }

        @NonNull Builder addAllSessionCommands() {
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_REWIND);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_SELECT_ROUTE);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_SET_RATING);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO);
            addCommand(SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO);
            return this;
        }

        @NonNull Builder addAllLibraryCommands() {
            addCommand(SessionCommand2.COMMAND_CODE_LIBRARY_GET_CHILDREN);
            addCommand(SessionCommand2.COMMAND_CODE_LIBRARY_GET_ITEM);
            addCommand(SessionCommand2.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT);
            addCommand(SessionCommand2.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT);
            addCommand(SessionCommand2.COMMAND_CODE_LIBRARY_SEARCH);
            addCommand(SessionCommand2.COMMAND_CODE_LIBRARY_SUBSCRIBE);
            addCommand(SessionCommand2.COMMAND_CODE_LIBRARY_UNSUBSCRIBE);
            return this;
        }

        private void addCommandsWithPrefix(String prefix) {
            final Field[] fields = SessionCommand2.class.getFields();
            if (fields != null) {
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].getName().startsWith(prefix)
                            && !fields[i].getName().equals("COMMAND_CODE_CUSTOM")) {
                        try {
                            mCommands.add(new SessionCommand2(fields[i].getInt(null)));
                        } catch (IllegalAccessException e) {
                            Log.w(TAG, "Unexpected " + fields[i] + " in MediaSession2");
                        }
                    }
                }
            }
        }

        /**
         * Builds {@link SessionCommandGroup2}.
         *
         * @return a new {@link SessionCommandGroup2}.
         */
        public @NonNull SessionCommandGroup2 build() {
            return new SessionCommandGroup2(mCommands);
        }
    }
}
