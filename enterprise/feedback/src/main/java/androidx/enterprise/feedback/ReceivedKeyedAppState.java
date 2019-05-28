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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.enterprise.feedback.KeyedAppState.Severity;

import com.google.auto.value.AutoValue;

/**
 * A keyed app state received from an app. This contains all of the information added by the app to
 * the {@link KeyedAppState} as well as the packageName and timestamp added when the state
 * was received.
 */
@AutoValue
public abstract class ReceivedKeyedAppState {

    // Create a no-args constructor so it doesn't appear in current.txt
    ReceivedKeyedAppState() {}

    /** Create a {@link ReceivedKeyedAppStateBuilder}. */
    @NonNull
    public static ReceivedKeyedAppStateBuilder builder() {
        return new AutoValue_ReceivedKeyedAppState.Builder();
    }

    /** Assumes {@link KeyedAppState#isValid(Bundle)}. */
    static ReceivedKeyedAppState fromBundle(Bundle bundle, String packageName, long timestamp) {
        if (!KeyedAppState.isValid(bundle)) {
            throw new IllegalArgumentException("Bundle is not valid");
        }

        return builder()
                .setPackageName(packageName)
                .setTimestamp(timestamp)
                .setKey(bundle.getString(APP_STATE_KEY))
                .setSeverity(bundle.getInt(APP_STATE_SEVERITY))
                .setMessage(bundle.getString(APP_STATE_MESSAGE))
                .setData(bundle.getString(APP_STATE_DATA))
                .build();
    }

    /**
     * The name of the package which submitted the states.
     *
     * <p>This is automatically set to the correct value by the receiver; it is NOT self-reported by
     * the app sending the feedback.
     */
    @NonNull
    public abstract String getPackageName();

    /**
     * The unix timestamp, in milliseconds, when the states were received.
     *
     * <p>This is automatically set to the correct value by the receiver; it is NOT self-reported by
     * the app sending the feedback.
     */
    public abstract long getTimestamp();

    /** See {@link KeyedAppState#getKey()} */
    @NonNull
    public abstract String getKey();

    /** See {@link KeyedAppState#getSeverity()} */
    public abstract int getSeverity();

    /** See {@link KeyedAppState#getMessage()} */
    @Nullable
    public abstract String getMessage();

    /** See {@link KeyedAppState#getData()} */
    @Nullable
    public abstract String getData();

    /** The builder for {@link ReceivedKeyedAppState}. */
    @AutoValue.Builder
    public abstract static class ReceivedKeyedAppStateBuilder {

        // Create a no-args constructor so it doesn't appear in current.txt
        ReceivedKeyedAppStateBuilder() {}

        /** Set {@link ReceivedKeyedAppState#getPackageName()}. */
        @NonNull
        public abstract ReceivedKeyedAppStateBuilder setPackageName(@NonNull String packageName);

        /** Set {@link ReceivedKeyedAppState#getTimestamp()}. */
        @NonNull
        public abstract ReceivedKeyedAppStateBuilder setTimestamp(long timestamp);

        /** Set {@link ReceivedKeyedAppState#getKey()}. */
        @NonNull
        public abstract ReceivedKeyedAppStateBuilder setKey(@NonNull String key);

        /** Set {@link ReceivedKeyedAppState#getSeverity()}. */
        @NonNull
        public abstract ReceivedKeyedAppStateBuilder setSeverity(@Severity int severity);

        /** Set {@link ReceivedKeyedAppState#getMessage()}. */
        @NonNull
        public abstract ReceivedKeyedAppStateBuilder setMessage(@Nullable String message);

        /** Set {@link ReceivedKeyedAppState#getData()}. */
        @NonNull
        public abstract ReceivedKeyedAppStateBuilder setData(@Nullable String data);

        /**
         * Instantiate the {@link ReceivedKeyedAppState}.
         *
         * <p>Assumes the key and severity are set.
         */
        @NonNull
        public abstract ReceivedKeyedAppState build();
    }
}
