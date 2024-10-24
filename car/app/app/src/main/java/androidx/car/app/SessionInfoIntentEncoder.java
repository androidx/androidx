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

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;

/** Helper to encode and decode a {@link SessionInfo} from an {@link Intent} */
public class SessionInfoIntentEncoder {
    /** Private constructor to prevent instantiation */
    private SessionInfoIntentEncoder() {
    }

    /** The key for a {@link Bundle} extra containing the {@link SessionInfo} for a bind. */
    private static final String EXTRA_SESSION_INFO = "androidx.car.app.extra.SESSION_INFO_BUNDLE";

    /** Key for {@link SessionInfo#mDisplayType}. */
    private static final String KEY_DISPLAY_TYPE = "display-type";
    /** Key for {@link SessionInfo#mSessionId}. */
    private static final String KEY_SESSION_ID = "session-id";

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

        Bundle sessionInfoBundle = new Bundle();
        sessionInfoBundle.putInt(KEY_DISPLAY_TYPE, sessionInfo.getDisplayType());
        sessionInfoBundle.putString(KEY_SESSION_ID, sessionInfo.getSessionId());
        intent.putExtra(EXTRA_SESSION_INFO, sessionInfoBundle);
    }

    /**
     * Decodes a new {@link SessionInfo} for a given {@code intent}
     */
    public static @NonNull SessionInfo decode(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            throw new IllegalArgumentException(
                    "Expected the SessionInfo to be encoded in the bind intent extras, but the "
                            + "extras were null.");
        }

        Bundle sessionInfoBundle = extras.getBundle(EXTRA_SESSION_INFO);
        int displayType = sessionInfoBundle.getInt(KEY_DISPLAY_TYPE);
        String sessionId = sessionInfoBundle.getString(KEY_SESSION_ID);
        return new SessionInfo(displayType, sessionId);
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

        /** Wrapper for {@link Intent#setIdentifier(String)}. */
        static void setIdentifier(@NonNull Intent intent, @NonNull String identifier) {
            intent.setIdentifier(identifier);
        }
    }
}
