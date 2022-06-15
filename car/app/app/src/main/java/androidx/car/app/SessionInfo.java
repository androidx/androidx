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

/**
 * Contains information about a {@link Session}.
 */
@RequiresCarApi(5)
@CarProtocol
public class SessionInfo {
    /**
     * The main infotainment display.
     */
    public static final int DISPLAY_TYPE_MAIN = 0;
    /**
     * Cluster display, usually located behind the steering wheel.
     */
    public static final int DISPLAY_TYPE_CLUSTER = 1;

    /**
     * Defines which kind of {@link androidx.car.app.model.Template}s {@link Screen}s from this
     * {@link Session} can return for the given display it will render on.
     *
     * @hide
     */

    @IntDef({DISPLAY_TYPE_MAIN, DISPLAY_TYPE_CLUSTER})
    @Retention(SOURCE)
    public @interface DisplayType {
    }

    /**
     * A default {@link SessionInfo} for the main display.
     *
     * @hide
     */
    public static final SessionInfo DEFAULT_SESSION_INFO = new SessionInfo(
            DISPLAY_TYPE_MAIN, "main");
    @Keep
    @NonNull
    private final String mSessionId;
    @Keep
    @DisplayType
    private final int mDisplayType;

    /**
     * Returns the {@code id} for the {@link Session}.
     */
    @NonNull
    public String getSessionId() {
        return mSessionId;
    }

    /**
     * Returns the {@code id} for the displayType.
     */
    public int getDisplayType() {
        return mDisplayType;
    }

    /**
     * Creates a new {@link SessionInfo} with the provided {@code displayType} and {@code sessionId}
     */
    public SessionInfo(@DisplayType int displayType, @NonNull String sessionId) {
        mDisplayType = displayType;
        mSessionId = sessionId;
    }

    private SessionInfo() {
        mSessionId = "main";
        mDisplayType = DISPLAY_TYPE_MAIN;
    }

}
