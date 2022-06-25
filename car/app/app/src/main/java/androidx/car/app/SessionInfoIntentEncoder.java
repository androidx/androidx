/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.car.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

/** Helper to encode and decode a {@link SessionInfo} from an {@link Intent} */
public class SessionInfoIntentEncoder {
    /** Private constructor to prevent instantiation */
    private SessionInfoIntentEncoder() {
    }

    /** The key for a {@link Bundleable} extra containing the {@link SessionInfo} for a bind. */
    private static final String EXTRA_SESSION_INFO = "androidx.car.app.extra.SESSION_INFO";

    /**
     * Sets the unique identifier for the given {@code intent} and encodes the passed
     * {@link SessionInfo} in its extras.
     *
     * <p>The intent identifier that's created is unique based on {@link SessionInfo#toString()}.
     * The {@link Intent} field that's set is either {@link Intent#setIdentifier} on API 29 and
     * above, or {@link Intent#setData}.
     */
    public static void encode(@NonNull SessionInfo sessionInfo, @NonNull Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29.setIdentifier(intent, sessionInfo.toString());
        } else {
            intent.setData(new Uri.Builder().path(sessionInfo.toString()).build());
        }
        try {
            intent.putExtra(EXTRA_SESSION_INFO, Bundleable.create(sessionInfo));
        } catch (BundlerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decodes a new {@link SessionInfo} for a given {@code intent}
     */
    @NonNull
    @SuppressWarnings("deprecation")  /* getParcelable deprecation */
    public static SessionInfo decode(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            throw new IllegalArgumentException(
                    "Expected the SessionInfo to be encoded in the bind intent extras, but the "
                            + "extras were null.");
        }
        Bundleable sessionInfoBundleable = extras.getParcelable(EXTRA_SESSION_INFO);
        if (sessionInfoBundleable == null) {
            throw new IllegalArgumentException(
                    "Expected the SessionInfo to be encoded in the bind intent extras, but they "
                            + "couldn't be found in the extras.");
        }
        try {
            return (SessionInfo) sessionInfoBundleable.get();
        } catch (BundlerException e) {
            throw new IllegalArgumentException(
                    "Expected the SessionInfo to be encoded in the bind intent extras, but they "
                            + "were encoded improperly", e);
        }
    }

    /** Returns whether or not the given {@code intent} contains an encoded {@link SessionInfo}. */
    public static boolean containsSessionInfo(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return false;
        }

        return extras.containsKey(EXTRA_SESSION_INFO);
    }

    /** Android Q method calls wrapped in a {@link RequiresApi} class to appease the compiler. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29 {
        // Not instantiable
        private Api29() {
        }

        /** Wrapper for {@link Intent#getIdentifier()}. */
        @DoNotInline
        @Nullable
        static String getIdentifier(@NonNull Intent intent) {
            return intent.getIdentifier();
        }

        /** Wrapper for {@link Intent#setIdentifier(String)}. */
        @DoNotInline
        static void setIdentifier(@NonNull Intent intent, @NonNull String identifier) {
            intent.setIdentifier(identifier);
        }
    }
}
