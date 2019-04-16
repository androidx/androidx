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

package androidx.enterprise.feedback;

import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_DATA;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_KEY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_MESSAGE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_SEVERITY;

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A keyed app state to be sent to an EMM (enterprise mobility management), with the intention that
 * it is displayed to the management organization.
 */
@AutoValue
public abstract class KeyedAppState {

    // Create a no-args constructor so it doesn't appear in current.txt
    KeyedAppState() {}

    @IntDef({SEVERITY_INFO, SEVERITY_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface Severity {
    }

    public static final int SEVERITY_INFO = 1;
    public static final int SEVERITY_ERROR = 2;

    /** The maximum length of the key. */
    public static final int MAX_KEY_LENGTH = 100;
    /** The maximum length of the message field. */
    public static final int MAX_MESSAGE_LENGTH = 1000;
    /** The maximum length of the data field. */
    public static final int MAX_DATA_LENGTH = 1000;

    /** Create a {@link KeyedAppStateBuilder}. */
    @NonNull
    public static KeyedAppStateBuilder builder() {
        return new AutoValue_KeyedAppState.Builder().setSeverity(SEVERITY_INFO);
    }

    /**
     * The key for the app state. Acts as a point of reference for what the app is providing state
     * for. For example, when providing managed configuration feedback, this key could be the
     * managed
     * configuration key to allow EMMs to take advantage of the connection in their UI.
     */
    @NonNull
    public abstract String key();

    /**
     * The severity of the app state. This allows EMMs to choose to notify admins of errors. This
     * should only be set to {@link #SEVERITY_ERROR} for genuine error conditions that a management
     * organization needs to take action to fix.
     *
     * <p>When sending an app state containing errors, it is critical that follow-up app states are
     * sent when the errors have been resolved, using the same {@link #key()} and this value set to
     * {@link #SEVERITY_INFO}.
     */
    @Severity
    public abstract int severity();

    /**
     * Optionally, a free-form message string to explain the app state. If the state was
     * triggered by a particular value (e.g. a managed configuration value), it should be
     * included in the message.
     */
    @Nullable
    public abstract String message();

    /**
     * Optionally, a machine-readable value to be read by the EMM. For example, setting values that
     * the admin can choose to query against in the EMM console (e.g. “notify me if the
     * battery_warning data < 10”).
     */
    @Nullable
    public abstract String data();

    Bundle toStateBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(APP_STATE_KEY, key());
        bundle.putInt(APP_STATE_SEVERITY, severity());
        if (message() != null) {
            bundle.putString(APP_STATE_MESSAGE, message());
        }
        if (data() != null) {
            bundle.putString(APP_STATE_DATA, data());
        }
        return bundle;
    }

    /** Assumes {@link #isValid(Bundle)}. */
    static KeyedAppState fromBundle(Bundle bundle) {
        if (!isValid(bundle)) {
            throw new IllegalArgumentException("Bundle is not valid");
        }

        return KeyedAppState.builder()
                .setKey(bundle.getString(APP_STATE_KEY))
                .setSeverity(bundle.getInt(APP_STATE_SEVERITY))
                .setMessage(bundle.getString(APP_STATE_MESSAGE))
                .setData(bundle.getString(APP_STATE_DATA))
                .build();
    }

    static boolean isValid(Bundle bundle) {
        String key = bundle.getString(APP_STATE_KEY);
        if (key == null || key.length() > MAX_KEY_LENGTH) {
            return false;
        }

        int severity = bundle.getInt(APP_STATE_SEVERITY);
        if (severity != SEVERITY_INFO && severity != SEVERITY_ERROR) {
            return false;
        }

        String message = bundle.getString(APP_STATE_MESSAGE);
        if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
            return false;
        }

        String data = bundle.getString(APP_STATE_DATA);
        if (data != null && data.length() > MAX_DATA_LENGTH) {
            return false;
        }

        return true;
    }

    /** The builder for {@link KeyedAppState}. */
    @AutoValue.Builder
    public abstract static class KeyedAppStateBuilder {

        // Create a no-args constructor so it doesn't appear in current.txt
        KeyedAppStateBuilder() {}

        /** Set {@link KeyedAppState#key()}. */
        @NonNull
        public abstract KeyedAppStateBuilder setKey(@NonNull String key);

        /** Set {@link KeyedAppState#severity()}. */
        @NonNull
        public abstract KeyedAppStateBuilder setSeverity(@Severity int severity);

        /** Set {@link KeyedAppState#message()}. */
        @NonNull
        public abstract KeyedAppStateBuilder setMessage(@Nullable String message);

        /** Set {@link KeyedAppState#data()}. */
        @NonNull
        public abstract KeyedAppStateBuilder setData(@Nullable String data);

        abstract KeyedAppState autoBuild();

        /**
         * Instantiate the {@link KeyedAppState}.
         *
         * <p>Severity will default to {@link #SEVERITY_INFO} if not set.
         *
         * <p>Assumes the key is set, key length is at most 100 characters, message length is as
         * most 1000 characters, data length is at most 1000 characters, and severity is set to
         * either {@link #SEVERITY_INFO} or {@link #SEVERITY_ERROR}.
         */
        @NonNull
        public KeyedAppState build() {
            KeyedAppState keyedAppState = autoBuild();
            if (keyedAppState.key().length() > MAX_KEY_LENGTH) {
                throw new IllegalStateException(
                        String.format("Key length can be at most %s", MAX_KEY_LENGTH));
            }

            if (keyedAppState.message() != null
                    && keyedAppState.message().length() > MAX_MESSAGE_LENGTH) {
                throw new IllegalStateException(
                        String.format("Message length can be at most %s", MAX_MESSAGE_LENGTH));
            }

            if (keyedAppState.data() != null && keyedAppState.data().length() > MAX_DATA_LENGTH) {
                throw new IllegalStateException(
                        String.format("Data length can be at most %s", MAX_DATA_LENGTH));
            }

            if (keyedAppState.severity() != SEVERITY_ERROR
                    && keyedAppState.severity() != SEVERITY_INFO) {
                throw new IllegalStateException("Severity must be SEVERITY_ERROR or SEVERITY_INFO");
            }

            return keyedAppState;
        }
    }
}
