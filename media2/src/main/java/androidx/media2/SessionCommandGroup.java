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
import static androidx.media2.SessionCommand.COMMAND_CODE_CUSTOM;
import static androidx.media2.SessionCommand.COMMAND_VERSION_1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.media2.SessionCommand.CommandCode;
import androidx.media2.SessionCommand.CommandVersion;
import androidx.media2.SessionCommand.Range;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of {@link SessionCommand} which represents a command group.
 */
@VersionedParcelize
public final class SessionCommandGroup implements VersionedParcelable {
    private static final String TAG = "SessionCommandGroup";

    @ParcelField(1)
    Set<SessionCommand> mCommands = new HashSet<>();

    /**
     * Default Constructor.
     */
    public SessionCommandGroup() { }

    /**
     * Creates a new SessionCommandGroup with commands copied from another object.
     *
     * @param commands The collection of commands to copy.
     */
    public SessionCommandGroup(@Nullable Collection<SessionCommand> commands) {
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
    @RestrictTo(LIBRARY_GROUP)
    public void addCommand(@NonNull SessionCommand command) {
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
     *                    Shouldn't be {@link SessionCommand#COMMAND_CODE_CUSTOM}.
     * @hide TODO remove this method
     */
    @RestrictTo(LIBRARY_GROUP)
    public void addCommand(@CommandCode int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException(
                    "Use addCommand(SessionCommand) for COMMAND_CODE_CUSTOM.");
        }
        if (!hasCommand(commandCode)) {
            mCommands.add(new SessionCommand(commandCode));
        }
    }

    /**
     * Checks whether this command group has a command that matches given {@code command}.
     *
     * @param command A command to find. Shouldn't be {@code null}.
     */
    public boolean hasCommand(@NonNull SessionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        return mCommands.contains(command);
    }

    /**
     * Checks whether this command group has a command that matches given {@code commandCode}.
     *
     * @param commandCode A command code to find.
     *                    Shouldn't be {@link SessionCommand#COMMAND_CODE_CUSTOM}.
     */
    public boolean hasCommand(@CommandCode int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("Use hasCommand(Command) for custom command");
        }
        for (SessionCommand command : mCommands) {
            if (command.getCommandCode() == commandCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all commands of this command group.
     */
    public @NonNull Set<SessionCommand> getCommands() {
        return new HashSet<>(mCommands);
    }

    /**
     * Builds a {@link SessionCommandGroup} object.
     */
    public static final class Builder {
        private Set<SessionCommand> mCommands;

        public Builder() {
            mCommands = new HashSet<>();
        }

        /**
         * Creates a new builder for {@link SessionCommandGroup} with commands copied from another
         * {@link SessionCommandGroup} object.
         * @param commandGroup
         */
        public Builder(@NonNull SessionCommandGroup commandGroup) {
            mCommands = commandGroup.getCommands();
        }

        /**
         * Adds a command to this command group.
         *
         * @param command A command to add. Shouldn't be {@code null}.
         */
        public @NonNull Builder addCommand(@NonNull SessionCommand command) {
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
         *                    Shouldn't be {@link SessionCommand#COMMAND_CODE_CUSTOM}.
         */
        public @NonNull Builder addCommand(@CommandCode int commandCode) {
            if (commandCode == COMMAND_CODE_CUSTOM) {
                throw new IllegalArgumentException(
                        "Use addCommand(SessionCommand) for COMMAND_CODE_CUSTOM.");
            }
            mCommands.add(new SessionCommand(commandCode));
            return this;
        }

        /**
         * Adds all predefined session commands except for the commands added after the specified
         * version without default implementation. This provides convenient way to add commands
         * with implementation.
         * <p>
         * When you update support library version, it's recommended to take a look
         * {@link SessionCommand} to double check whether this only adds commands that you want.
         * You may increase the version here.
         *
         * @param version command version
         * @see SessionCommand#COMMAND_VERSION_1
         * @see MediaSession.SessionCallback#onConnect(MediaSession, MediaSession.ControllerInfo)
         */
        public @NonNull Builder addAllPredefinedCommands(@CommandVersion int version) {
            if (version != COMMAND_VERSION_1) {
                throw new IllegalArgumentException("Unknown command version " + version);
            }
            addAllPlayerCommands(version);
            addAllVolumeCommands(version);
            addAllSessionCommands(version);
            addAllLibraryCommands(version);
            return this;
        }

        /**
         * Removes a command from this group which matches given {@code command}.
         *
         * @param command A command to find. Shouldn't be {@code null}.
         */
        public @NonNull Builder removeCommand(@NonNull SessionCommand command) {
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
         *                    Shouldn't be {@link SessionCommand#COMMAND_CODE_CUSTOM}.
         */
        public @NonNull Builder removeCommand(@CommandCode int commandCode) {
            if (commandCode == COMMAND_CODE_CUSTOM) {
                throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
            }
            mCommands.remove(new SessionCommand(commandCode));
            return this;
        }

        @NonNull Builder addAllPlayerCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_PLAYER_COMMANDS_MAP);
            return this;
        }

        @NonNull Builder addAllPlayerCommands(@CommandVersion int version,
                boolean includePlaylistCommands) {
            if (includePlaylistCommands) {
                return addAllPlayerCommands(version);
            }
            for (int i = COMMAND_VERSION_1; i <= version; i++) {
                Range include = SessionCommand.VERSION_PLAYER_COMMANDS_MAP.get(i);
                Range exclude = SessionCommand.VERSION_PLAYER_PLAYLIST_COMMANDS_MAP.get(i);
                for (int code = include.lower; code <= include.upper; code++) {
                    if (code < exclude.lower && code > exclude.upper) {
                        addCommand(code);
                    }
                }
            }
            return this;
        }

        @NonNull Builder addAllVolumeCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_VOLUME_COMMANDS_MAP);
            return this;
        }

        @NonNull Builder addAllSessionCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_SESSION_COMMANDS_MAP);
            return this;
        }

        @NonNull Builder addAllLibraryCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_LIBRARY_COMMANDS_MAP);
            return this;
        }

        private void addCommands(@CommandVersion int version, ArrayMap<Integer, Range> map) {
            for (int i = COMMAND_VERSION_1; i <= version; i++) {
                Range range = map.get(i);
                for (int code = range.lower; code <= range.upper; code++) {
                    addCommand(code);
                }
            }
        }

        /**
         * Builds {@link SessionCommandGroup}.
         *
         * @return a new {@link SessionCommandGroup}.
         */
        public @NonNull SessionCommandGroup build() {
            return new SessionCommandGroup(mCommands);
        }
    }
}
