/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.navigation.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.utils.CollectionUtils;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents the voice assistant capabilities of the navigation app.
 *
 * <p>This model is used to communicate to the host which voice actions and disruptions
 * the navigation app can currently handle, as well as whether the user has granted
 * consent for the navigation app to share its active routing state with the voice assistant.
 */
@CarProtocol
@KeepFields
@ExperimentalCarApi
@RequiresCarApi(9)
public final class NavigationVoiceAssistantCapabilities {

    /**
     * Defines the supported actions that a voice assistant can request from the navigation app.
     */
    @IntDef({
            ACTION_UNDEFINED,
            ACTION_ALLOW_AND_AVOID_FERRIES,
            ACTION_ALLOW_AND_AVOID_HIGHWAYS,
            ACTION_ALLOW_AND_AVOID_TOLLS,
            ACTION_CLEAR_SEARCH_RESULTS,
            ACTION_EXIT_NAVIGATION,
            ACTION_FOLLOW_MODE,
            ACTION_MUTE_AND_UNMUTE,
            ACTION_ROUTE_OVERVIEW,
            ACTION_SHOW_ALTERNATES,
            ACTION_SHOW_DIRECTIONS_LIST,
            ACTION_SHOW_SATELLITE,
            ACTION_SHOW_TRAFFIC,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface VoiceAssistantAction {}

    /**
     * Indicates an undefined or unknown action.
     */
    @VoiceAssistantAction
    public static final int ACTION_UNDEFINED = 0;

    /**
     * Indicates support for allowing and avoiding ferries.
     */
    @VoiceAssistantAction
    public static final int ACTION_ALLOW_AND_AVOID_FERRIES = 1;

    /**
     * Indicates support for allowing and avoiding highways.
     */
    @VoiceAssistantAction
    public static final int ACTION_ALLOW_AND_AVOID_HIGHWAYS = 2;

    /**
     * Indicates support for allowing and avoiding tolls.
     */
    @VoiceAssistantAction
    public static final int ACTION_ALLOW_AND_AVOID_TOLLS = 3;

    /**
     * Indicates support for clearing search results.
     */
    @VoiceAssistantAction
    public static final int ACTION_CLEAR_SEARCH_RESULTS = 4;

    /**
     * Indicates support for exiting navigation.
     */
    @VoiceAssistantAction
    public static final int ACTION_EXIT_NAVIGATION = 5;

    /**
     * Indicates support for follow mode.
     */
    @VoiceAssistantAction
    public static final int ACTION_FOLLOW_MODE = 6;

    /**
     * Indicates support for muting and unmuting voice guidance.
     */
    @VoiceAssistantAction
    public static final int ACTION_MUTE_AND_UNMUTE = 7;

    /**
     * Indicates support for showing the route overview.
     */
    @VoiceAssistantAction
    public static final int ACTION_ROUTE_OVERVIEW = 8;

    /**
     * Indicates support for showing alternate routes.
     */
    @VoiceAssistantAction
    public static final int ACTION_SHOW_ALTERNATES = 9;

    /**
     * Indicates support for showing the directions list.
     */
    @VoiceAssistantAction
    public static final int ACTION_SHOW_DIRECTIONS_LIST = 10;

    /**
     * Indicates support for showing the satellite view.
     */
    @VoiceAssistantAction
    public static final int ACTION_SHOW_SATELLITE = 11;

    /**
     * Indicates support for showing the traffic view.
     */
    @VoiceAssistantAction
    public static final int ACTION_SHOW_TRAFFIC = 12;

    /**
     * Defines the supported disruptions (e.g., accidents, hazards) that a voice assistant can
     * report to the navigation app.
     */
    @IntDef({
            DISRUPTION_REPORT_UNDEFINED,
            DISRUPTION_REPORT_CONSTRUCTION,
            DISRUPTION_REPORT_CRASH,
            DISRUPTION_REPORT_FLOODING,
            DISRUPTION_REPORT_FOG,
            DISRUPTION_REPORT_OBJECT_ON_ROAD,
            DISRUPTION_REPORT_POLICE,
            DISRUPTION_REPORT_POTHOLE,
            DISRUPTION_REPORT_ROAD_CLOSURE,
            DISRUPTION_REPORT_SNOW,
            DISRUPTION_REPORT_TRAFFIC,
            DISRUPTION_REPORT_VEHICLE
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface VoiceAssistantDisruption {}

    /**
     * Indicates an undefined or unknown disruption.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_UNDEFINED = 0;

    /**
     * Indicates support for receiving reports for construction zones.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_CONSTRUCTION = 1;

    /**
     * Indicates support for receiving reports for crashes.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_CRASH = 2;

    /**
     * Indicates support for receiving reports for flooding.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_FLOODING = 3;

    /**
     * Indicates support for receiving reports for fog.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_FOG = 4;

    /**
     * Indicates support for receiving reports for objects on the road.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_OBJECT_ON_ROAD = 5;

    /**
     * Indicates support for receiving reports for police presence.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_POLICE = 6;

    /**
     * Indicates support for receiving reports for potholes.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_POTHOLE = 7;

    /**
     * Indicates support for receiving reports for road closures.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_ROAD_CLOSURE = 8;

    /**
     * Indicates support for receiving reports for snow.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_SNOW = 9;

    /**
     * Indicates support for receiving reports for traffic jams.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_TRAFFIC = 10;

    /**
     * Indicates support for receiving reports for vehicles on the road.
     */
    @VoiceAssistantDisruption
    public static final int DISRUPTION_REPORT_VEHICLE = 11;

    private final boolean mIsVoiceAssistantConsentGranted;
    private final Set<Integer> mSupportedActions;
    private final Set<Integer> mSupportedDisruptions;

    private NavigationVoiceAssistantCapabilities(Builder builder) {
        mIsVoiceAssistantConsentGranted = builder.mIsVoiceAssistantConsentGranted;
        mSupportedActions = Collections.unmodifiableSet(new HashSet<>(builder.mSupportedActions));
        mSupportedDisruptions =
                Collections.unmodifiableSet(new HashSet<>(builder.mSupportedDisruptions));
    }

    /** Constructs an empty instance, used by serialization code. */
    private NavigationVoiceAssistantCapabilities() {
        mIsVoiceAssistantConsentGranted = false;
        mSupportedActions = Collections.emptySet();
        mSupportedDisruptions = Collections.emptySet();
    }

    /**
     * Returns whether the user has granted consent for the navigation app to share its state
     * with the voice assistant.
     *
     * @see Builder#setVoiceAssistantConsentGranted(boolean)
     */
    public boolean isVoiceAssistantConsentGranted() {
        return mIsVoiceAssistantConsentGranted;
    }

    /**
     * Returns the set of voice assistant actions supported by the navigation app.
     *
     * @see Builder#addSupportedAction(int)
     */
    @NonNull
    public Set<Integer> getSupportedActions() {
        return mSupportedActions;
    }

    /**
     * Returns the set of disruptions supported by the navigation app.
     *
     * @see Builder#addSupportedDisruption(int)
     */
    @NonNull
    public Set<Integer> getSupportedDisruptions() {
        return mSupportedDisruptions;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NavigationVoiceAssistantCapabilities)) {
            return false;
        }
        NavigationVoiceAssistantCapabilities otherCapabilities =
                (NavigationVoiceAssistantCapabilities) other;

        return mIsVoiceAssistantConsentGranted == otherCapabilities.mIsVoiceAssistantConsentGranted
                && Objects.equals(mSupportedActions, otherCapabilities.mSupportedActions)
                && Objects.equals(mSupportedDisruptions, otherCapabilities.mSupportedDisruptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsVoiceAssistantConsentGranted, mSupportedActions,
                mSupportedDisruptions);
    }

    @NonNull
    @Override
    public String toString() {
        return "NavigationVoiceAssistantCapabilities[ isVoiceAssistantConsentGranted: "
                + mIsVoiceAssistantConsentGranted
                + ", supportedActions: "
                + mSupportedActions
                + ", supportedDisruptions: "
                + mSupportedDisruptions
                + " ]";
    }

    /** A builder of {@link NavigationVoiceAssistantCapabilities}. */
    public static final class Builder {
        boolean mIsVoiceAssistantConsentGranted;
        final Set<Integer> mSupportedActions = new HashSet<>();
        final Set<Integer> mSupportedDisruptions = new HashSet<>();

        /**
         * Sets whether the user has granted explicit consent for the navigation app to share its
         * active routing state with the voice assistant.
         *
         * <p>By default, this is {@code false}.
         */
        @NonNull
        public Builder setVoiceAssistantConsentGranted(boolean isConsentGranted) {
            mIsVoiceAssistantConsentGranted = isConsentGranted;
            return this;
        }

        /**
         * Adds a voice assistant action supported by the navigation app.
         */
        @NonNull
        public Builder addSupportedAction(@VoiceAssistantAction int action) {
            mSupportedActions.add(action);
            return this;
        }

        /**
         * Adds a voice assistant disruption supported by the navigation app.
         */
        @NonNull
        public Builder addSupportedDisruption(@VoiceAssistantDisruption int disruption) {
            mSupportedDisruptions.add(disruption);
            return this;
        }

        /**
         * Constructs the {@link NavigationVoiceAssistantCapabilities} defined by this builder.
         */
        @NonNull
        public NavigationVoiceAssistantCapabilities build() {
            return new NavigationVoiceAssistantCapabilities(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
