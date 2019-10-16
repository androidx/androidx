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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.versionedparcelable.VersionedParcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines a command that a {@link MessageBrowser} can send to a {@link MessageLibraryService}.
 * <p>
 * If {@link #getCommandCode()} isn't {@link #COMMAND_CODE_CUSTOM}), it's predefined command.
 * If {@link #getCommandCode()} is {@link #COMMAND_CODE_CUSTOM}), it's custom command and
 * {@link #getCustomAction()} shouldn't be {@code null}.
 * @hide
 */
@RestrictTo(LIBRARY)
public final class MessageCommand implements VersionedParcelable {
    /**
     * The first version of message commands. This version is for commands introduced in
     * AndroidX message 1.0.0.
     */
    public static final int COMMAND_VERSION_1 = 1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int COMMAND_VERSION_CURRENT = COMMAND_VERSION_1;

    static final String KEY_COMMAND_CODE = "androidx.message.MessageCommand.KEY_COMMAND_CODE";
    static final String KEY_CUSTOM_ACTION = "androidx.message.MessageCommand.KEY_CUSTOM_ACTION";
    static final String KEY_CUSTOM_EXTRAS = "androidx.message.MessageCommand.KEY_CUSTOM_EXTRAS";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @SuppressLint("UniqueConstants")
    @IntDef({COMMAND_VERSION_1, COMMAND_VERSION_CURRENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CommandVersion {}

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({COMMAND_CODE_CUSTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CommandCode {}

    /**
     * Command code for the custom command which can be defined by string action in the
     * {@link MessageCommand}.
     */
    public static final int COMMAND_CODE_CUSTOM = 0;

    // TODO(sungsoo): remove COMMAND_CODE_TEST
    static final int COMMAND_CODE_TEST = 1000;

    static final ArrayMap<Integer, Range> VERSION_COMMANDS_MAP = new ArrayMap<>();
    static {
        VERSION_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_TEST, COMMAND_CODE_TEST));
    }

    int mCommandCode;
    // Nonnull if it's custom command
    String mCustomAction;
    Bundle mCustomExtras;

    /**
     * Constructor for creating a predefined command.
     *
     * @param commandCode A command code for predefined command.
     */
    public MessageCommand(@CommandCode int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
        }
        mCommandCode = commandCode;
        mCustomAction = null;
        mCustomExtras = null;
    }

    /**
     * Constructor for creating a custom command.
     *
     * @param action The action of this custom command.
     * @param extras An extra bundle for this custom command.
     */
    public MessageCommand(@NonNull String action, @Nullable Bundle extras) {
        if (action == null) {
            throw new NullPointerException("action shouldn't be null");
        }
        mCommandCode = COMMAND_CODE_CUSTOM;
        mCustomAction = action;
        mCustomExtras = extras;
    }

    /**
     * Gets the command code of a predefined command.
     * This will return {@link #COMMAND_CODE_CUSTOM} for a custom command.
     */
    @CommandCode
    public int getCommandCode() {
        return mCommandCode;
    }

    /**
     * Gets the action of a custom command.
     * This will return {@code null} for a predefined command.
     */
    @Nullable
    public String getCustomAction() {
        return mCustomAction;
    }

    /**
     * Gets the extra bundle of a custom command.
     * This will return {@code null} for a predefined command.
     */
    @Nullable
    public Bundle getCustomExtras() {
        return mCustomExtras;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof MessageCommand)) {
            return false;
        }
        MessageCommand other = (MessageCommand) obj;
        return mCommandCode == other.mCommandCode
                && TextUtils.equals(mCustomAction, other.mCustomAction);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mCustomAction, mCommandCode);
    }

    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_COMMAND_CODE, mCommandCode);
        if (!TextUtils.isEmpty(mCustomAction)) {
            bundle.putString(KEY_CUSTOM_ACTION, mCustomAction);
            bundle.putBundle(KEY_CUSTOM_EXTRAS, mCustomExtras);
        }
        return bundle;
    }

    static MessageCommand fromBundle(Bundle bundle) {
        Bundle command = MessageUtils.unparcelWithClassLoader(bundle);
        String customAction = command.getString(KEY_CUSTOM_ACTION);
        if (TextUtils.isEmpty(customAction)) {
            return new MessageCommand(command.getInt(KEY_COMMAND_CODE));
        }
        return new MessageCommand(customAction, command.getBundle(KEY_CUSTOM_EXTRAS));
    }

    static final class Range {
        public final int lower;
        public final int upper;

        Range(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
        }
    }
}
