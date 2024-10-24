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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.versioning.CarAppApiLevel;
import androidx.car.app.versioning.CarAppApiLevels;

import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.util.Objects;
import java.util.Set;

/** Information about a {@link Session}, such as the physical display and the session ID. */
@RequiresCarApi(6)
@CarProtocol
@KeepFields
public class SessionInfo {
    private static final char DIVIDER = '/';

    /** The primary infotainment display usually in the center column of the vehicle. */
    @DisplayType
    public static final int DISPLAY_TYPE_MAIN = 0;

    /** The cluster display, usually located behind the steering wheel. */
    @DisplayType
    public static final int DISPLAY_TYPE_CLUSTER = 1;

    private static final ImmutableSet<Class<? extends Template>> CLUSTER_SUPPORTED_TEMPLATES_API_6 =
            ImmutableSet.of(NavigationTemplate.class);
    private static final ImmutableSet<Class<? extends Template>>
            CLUSTER_SUPPORTED_TEMPLATES_LESS_THAN_API_6 = ImmutableSet.of();

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({DISPLAY_TYPE_MAIN, DISPLAY_TYPE_CLUSTER})
    @Retention(SOURCE)
    public @interface DisplayType {
    }

    /**
     * A default {@link SessionInfo} for the main display, used when the host is on a version
     * that doesn't support this new class.
     */
    public static final @NonNull SessionInfo DEFAULT_SESSION_INFO = new SessionInfo(
            DISPLAY_TYPE_MAIN, "main");

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

    /** A string identifier unique per physical display. */
    private final @NonNull String mSessionId;

    /** The type of display the {@link Session} is rendering on. */
    @DisplayType
    private final int mDisplayType;

    /**
     * Returns a session-stable ID, unique to the display that the {@link Session} is rendering on.
     */
    public @NonNull String getSessionId() {
        return mSessionId;
    }

    /** Returns the type of display that the {@link Session} is rendering on. */
    @DisplayType
    public int getDisplayType() {
        return mDisplayType;
    }

    /**
     * Returns the set of templates that are allowed for this {@link Session}, or {@code null} if
     * there are no restrictions (ie. all templates are allowed).
     */
    @SuppressWarnings("NullableCollection") // Set does not contain nulls
    public @Nullable Set<Class<? extends Template>> getSupportedTemplates(
            @CarAppApiLevel int carAppApiLevel) {
        if (mDisplayType == DISPLAY_TYPE_CLUSTER) {
            if (carAppApiLevel >= CarAppApiLevels.LEVEL_6) {
                return CLUSTER_SUPPORTED_TEMPLATES_API_6;
            }

            return CLUSTER_SUPPORTED_TEMPLATES_LESS_THAN_API_6;
        }

        return null;
    }

    @Override
    public @NonNull String toString() {
        return String.valueOf(mDisplayType) + DIVIDER + mSessionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSessionId, mDisplayType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SessionInfo)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        SessionInfo object = (SessionInfo) obj;
        return this.getSessionId().equals(object.getSessionId())
                && this.getDisplayType() == object.getDisplayType();
    }
}
