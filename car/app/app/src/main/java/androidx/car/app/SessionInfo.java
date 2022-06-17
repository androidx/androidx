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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;

import java.lang.annotation.Retention;

/** Information about a {@link Session}, such as the physical display and the session ID. */
@RequiresCarApi(5)
@CarProtocol
public class SessionInfo {
    /** The primary infotainment display usually in the center column of the vehicle. */
    public static final int DISPLAY_TYPE_MAIN = 0;

    /** The cluster display, usually located behind the steering wheel.  */
    public static final int DISPLAY_TYPE_CLUSTER = 1;

    /**
     * @hide
     */
    @IntDef({DISPLAY_TYPE_MAIN, DISPLAY_TYPE_CLUSTER})
    @Retention(SOURCE)
    public @interface DisplayType {
    }

    /**
     * A default {@link SessionInfo} for the main display, used when the host is on a version
     * that doesn't support this new class.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    static final SessionInfo DEFAULT_SESSION_INFO = new SessionInfo(
            DISPLAY_TYPE_MAIN, "main");

    /** A string identifier unique per physical display. */
    @Keep
    @NonNull
    private final String mSessionId;

    /** The type of display the {@link Session} is rendering on. */
    @Keep
    @DisplayType
    private final int mDisplayType;

    /**
     * Returns a session-stable ID, unique to the display that the {@link Session} is rendering on.
     */
    @NonNull
    public String getSessionId() {
        return mSessionId;
    }

    /** Returns the type of display that the {@link Session} is rendering on. */
    @DisplayType
    public int getDisplayType() {
        return mDisplayType;
    }

    /**
     * Creates a new {@link SessionInfo} with the provided {@code displayType} and {@code
     * sessionId}.
     */
    public SessionInfo(@DisplayType int displayType, @NonNull String sessionId) {
        mDisplayType = displayType;
        mSessionId = sessionId;
    }

    // Required for Bundler
    private SessionInfo() {
        mSessionId = "main";
        mDisplayType = DISPLAY_TYPE_MAIN;
    }
}
