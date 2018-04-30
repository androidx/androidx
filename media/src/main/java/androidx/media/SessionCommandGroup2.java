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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media.SessionCommand2.COMMAND_CODE_CUSTOM;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of {@link SessionCommand2} which represents a command group.
 */
public final class SessionCommandGroup2 {

    private static final String TAG = "SessionCommandGroup2";
    private static final String KEY_COMMANDS = "android.media.mediasession2.commandgroup.commands";
    // Prefix for all command codes
    private static final String PREFIX_COMMAND_CODE = "COMMAND_CODE_";
    // Prefix for command codes that will be sent directly to the MediaPlayerInterface
    private static final String PREFIX_COMMAND_CODE_PLAYBACK = "COMMAND_CODE_PLAYBACK_";
    // Prefix for command codes that will be sent directly to the MediaPlaylistAgent
    private static final String PREFIX_COMMAND_CODE_PLAYLIST = "COMMAND_CODE_PLAYLIST_";
    // Prefix for command codes that will be sent directly to AudioManager or VolumeProvider.
    private static final String PREFIX_COMMAND_CODE_VOLUME = "COMMAND_CODE_VOLUME_";

    private Set<SessionCommand2> mCommands = new HashSet<>();

    /**
     * Default Constructor.
     */
    public SessionCommandGroup2() { }

    /**
     * Creates a new SessionCommandGroup2 with commands copied from another object.
     *
     * @param other The SessionCommandGroup2 instance to copy.
     */
    public SessionCommandGroup2(@Nullable SessionCommandGroup2 other) {
        if (other != null) {
            mCommands.addAll(other.mCommands);
        }
    }

    /**
     * Adds a command to this command group.
     *
     * @param command A command to add. Shouldn't be {@code null}.
     */
    public void addCommand(@NonNull SessionCommand2 command) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        mCommands.add(command);
    }

    /**
     * Adds a predefined command with given {@code commandCode} to this command group.
     *
     * @param commandCode A command code to add.
     *                    Shouldn't be {@link SessionCommand2#COMMAND_CODE_CUSTOM}.
     */
    public void addCommand(int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        mCommands.add(new SessionCommand2(commandCode));
    }

    /**
     * Adds all predefined commands to this command group.
     */
    public void addAllPredefinedCommands() {
        addCommandsWithPrefix(PREFIX_COMMAND_CODE);
    }

    void addAllPlaybackCommands() {
        addCommandsWithPrefix(PREFIX_COMMAND_CODE_PLAYBACK);
    }

    void addAllPlaylistCommands() {
        addCommandsWithPrefix(PREFIX_COMMAND_CODE_PLAYLIST);
    }

    void addAllVolumeCommands() {
        addCommandsWithPrefix(PREFIX_COMMAND_CODE_VOLUME);
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
     * Removes a command from this group which matches given {@code command}.
     *
     * @param command A command to find. Shouldn't be {@code null}.
     */
    public void removeCommand(@NonNull SessionCommand2 command) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        mCommands.remove(command);
    }

    /**
     * Removes a command from this group which matches given {@code commandCode}.
     *
     * @param commandCode A command code to find.
     *                    Shouldn't be {@link SessionCommand2#COMMAND_CODE_CUSTOM}.
     */
    public void removeCommand(int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
        }
        mCommands.remove(new SessionCommand2(commandCode));
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
}
