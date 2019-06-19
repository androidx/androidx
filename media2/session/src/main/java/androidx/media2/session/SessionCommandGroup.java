/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static androidx.media2.session.SessionCommand.COMMAND_CODE_CUSTOM;
import static androidx.media2.session.SessionCommand.COMMAND_VERSION_1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.media2.session.SessionCommand.CommandCode;
import androidx.media2.session.SessionCommand.CommandVersion;
import androidx.media2.session.SessionCommand.Range;
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
     * Checks whether this command group has a command that matches given {@code command}.
     *
     * @param command A command to find. Shouldn't be {@code null}.
     */
    public boolean hasCommand(@NonNull SessionCommand command) {
        if (command == null) {
            throw new NullPointerException("command shouldn't be null");
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
    @NonNull
    public Set<SessionCommand> getCommands() {
        return new HashSet<>(mCommands);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SessionCommandGroup)) return false;

        SessionCommandGroup that = (SessionCommandGroup) obj;
        if (mCommands == null) {
            return that.mCommands == null;
        }
        return mCommands.equals(that.mCommands);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hashCode(mCommands);
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
            if (commandGroup == null) {
                throw new NullPointerException("commandGroup shouldn't be null");
            }
            mCommands = commandGroup.getCommands();
        }

        /**
         * Adds a command to this command group.
         *
         * @param command A command to add. Shouldn't be {@code null}.
         */
        @NonNull
        public Builder addCommand(@NonNull SessionCommand command) {
            if (command == null) {
                throw new NullPointerException("command shouldn't be null");
            }
            mCommands.add(command);
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
        @NonNull
        public Builder addAllPredefinedCommands(@CommandVersion int version) {
            if (version != COMMAND_VERSION_1) {
                throw new IllegalArgumentException("Unknown command version " + version);
            }
            addAllPlayerCommands(version, /* includeHidden= */ true);
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
        @NonNull
        public Builder removeCommand(@NonNull SessionCommand command) {
            if (command == null) {
                throw new NullPointerException("command shouldn't be null");
            }
            mCommands.remove(command);
            return this;
        }

        @NonNull
        Builder addAllPlayerCommands(@CommandVersion int version, boolean includeHidden) {
            addAllPlayerBasicCommands(version);
            addAllPlayerPlaylistCommands(version);
            if (includeHidden) addAllPlayerHiddenCommands(version);
            return this;
        }

        @NonNull
        Builder addAllPlayerBasicCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_PLAYER_BASIC_COMMANDS_MAP);
            return this;
        }

        @NonNull
        Builder addAllPlayerPlaylistCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_PLAYER_PLAYLIST_COMMANDS_MAP);
            return this;
        }

        @NonNull
        Builder addAllPlayerHiddenCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_PLAYER_HIDDEN_COMMANDS_MAP);
            return this;
        }

        @NonNull
        Builder addAllVolumeCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_VOLUME_COMMANDS_MAP);
            return this;
        }

        @NonNull
        Builder addAllSessionCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_SESSION_COMMANDS_MAP);
            return this;
        }

        @NonNull
        Builder addAllLibraryCommands(@CommandVersion int version) {
            addCommands(version, SessionCommand.VERSION_LIBRARY_COMMANDS_MAP);
            return this;
        }

        private void addCommands(@CommandVersion int version, ArrayMap<Integer, Range> map) {
            for (int i = COMMAND_VERSION_1; i <= version; i++) {
                Range range = map.get(i);
                for (int code = range.lower; code <= range.upper; code++) {
                    addCommand(new SessionCommand(code));
                }
            }
        }

        /**
         * Builds {@link SessionCommandGroup}.
         *
         * @return a new {@link SessionCommandGroup}.
         */
        @NonNull
        public SessionCommandGroup build() {
            return new SessionCommandGroup(mCommands);
        }
    }
}
