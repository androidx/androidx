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

package androidx.message.browser;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.message.browser.MessageCommand.COMMAND_CODE_CUSTOM;
import static androidx.message.browser.MessageCommand.COMMAND_VERSION_1;
import static androidx.message.browser.MessageCommand.COMMAND_VERSION_CURRENT;
import static androidx.message.browser.MessageCommand.VERSION_COMMANDS_MAP;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.message.browser.MessageCommand.CommandCode;
import androidx.message.browser.MessageCommand.CommandVersion;
import androidx.message.browser.MessageCommand.Range;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of {@link MessageCommand} which represents a command group.
 * @hide
 */
@RestrictTo(LIBRARY)
public final class MessageCommandGroup {
    private static final String TAG = "SessionCommandGroup";

    static final String KEY_SIZE = "androidx.message.MessageCommandGroup.KEY_SIZE";
    static final String KEY_COMMAND_PREFIX = "androidx.message.MessageCommandGroup.KEY_COMMAND_";
    static final MessageCommandGroup EMPTY_COMMANDS_GROUP = new MessageCommandGroup(null);

    final Set<MessageCommand> mCommands = new HashSet<>();

    /**
     * Creates a new MessageCommandGroup with a collection of commands.
     *
     * @param commands The collection of commands to copy.
     */
    public MessageCommandGroup(@Nullable Collection<MessageCommand> commands) {
        if (commands != null) {
            mCommands.addAll(commands);
        }
    }

    /**
     * Checks whether this command group has a command that matches given {@code command}.
     *
     * @param command A command to find. Shouldn't be {@code null}.
     */
    public boolean hasCommand(@NonNull MessageCommand command) {
        if (command == null) {
            throw new NullPointerException("command shouldn't be null");
        }
        return mCommands.contains(command);
    }

    /**
     * Checks whether this command group has a command that matches given {@code commandCode}.
     *
     * @param commandCode A command code to find.
     *                    Shouldn't be {@link MessageCommand#COMMAND_CODE_CUSTOM}.
     */
    public boolean hasCommand(@CommandCode int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("Use hasCommand(Command) for custom command");
        }
        for (MessageCommand command : mCommands) {
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
    public Set<MessageCommand> getCommands() {
        return new HashSet<>(mCommands);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MessageCommandGroup)) return false;

        MessageCommandGroup that = (MessageCommandGroup) obj;
        return mCommands.equals(that.mCommands);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hashCode(mCommands);
    }

    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_SIZE, mCommands.size());
        int i = 0;
        for (MessageCommand command : mCommands) {
            bundle.putBundle(KEY_COMMAND_PREFIX + i, command.toBundle());
        }
        return bundle;
    }

    @NonNull
    static MessageCommandGroup fromBundle(@Nullable Bundle bundle) {
        Bundle commandGroup = MessageUtils.unparcelWithClassLoader(bundle);
        if (commandGroup == null) return EMPTY_COMMANDS_GROUP;

        int size = commandGroup.getInt(KEY_SIZE);
        if (size < 1) return EMPTY_COMMANDS_GROUP;

        Builder builder = new Builder();
        for (int i = 0; i < size; ++i) {
            builder.addCommand(
                    MessageCommand.fromBundle(commandGroup.getBundle(KEY_COMMAND_PREFIX + i)));
        }
        return builder.build();
    }

    /**
     * Builds a {@link MessageCommandGroup} object.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final class Builder {
        private Set<MessageCommand> mCommands;

        public Builder() {
            mCommands = new HashSet<>();
        }

        /**
         * Creates a new builder for {@link MessageCommandGroup} initialized to the contents of
         * the specified message command group.
         *
         * @param commandGroup The initial contents of the message command group.
         */
        public Builder(@NonNull MessageCommandGroup commandGroup) {
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
        public Builder addCommand(@NonNull MessageCommand command) {
            if (command == null) {
                throw new NullPointerException("command shouldn't be null");
            }
            mCommands.add(command);
            return this;
        }

        /**
         * Removes a command from this group which matches given {@code command}.
         *
         * @param command A command to find. Shouldn't be {@code null}.
         */
        @NonNull
        public Builder removeCommand(@NonNull MessageCommand command) {
            if (command == null) {
                throw new NullPointerException("command shouldn't be null");
            }
            mCommands.remove(command);
            return this;
        }

        /**
         * Adds all predefined message commands except for the commands added after the specified
         * version without default implementation. This provides convenient way to add commands
         * with implementation.
         * <p>
         * When you update the message library version, it's recommended to take a look
         * {@link MessageCommand} to double check whether this only adds commands that you want.
         * You may increase the version here.
         *
         * @param version command version
         * @see MessageCommand#COMMAND_VERSION_1
         * @see MessageLibraryService#onConnect
         */
        @NonNull
        public Builder addAllPredefinedCommands(@CommandVersion int version) {
            if (version < COMMAND_VERSION_1 || version > COMMAND_VERSION_CURRENT) {
                throw new IllegalArgumentException("Unknown command version " + version);
            }
            addCommands(version, VERSION_COMMANDS_MAP);
            return this;
        }

        private void addCommands(@CommandVersion int version, ArrayMap<Integer, Range> map) {
            for (int i = COMMAND_VERSION_1; i <= version; i++) {
                Range range = map.get(i);
                if (range != null) {
                    for (int code = range.lower; code <= range.upper; code++) {
                        addCommand(new MessageCommand(code));
                    }
                }
            }
        }

        /**
         * Builds a {@link MessageCommandGroup}.
         *
         * @return a new {@link MessageCommandGroup}
         */
        @NonNull
        public MessageCommandGroup build() {
            return new MessageCommandGroup(mCommands);
        }
    }
}
