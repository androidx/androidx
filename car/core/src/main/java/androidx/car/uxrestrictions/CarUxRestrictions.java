/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.uxrestrictions;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Car UX Restrictions event.  A shim for {@link android.car.drivingstate.CarUxRestrictions} to be
 * used with {@link OnUxRestrictionsChangedListener}.
 *
 * <p>
 * This contains information on the set of UX restrictions that is in
 * place due to the car's driving state.
 * <p>
 * The restriction information is organized as follows:
 * <ul>
 * <li> When there are no restrictions in place, for example when the car is parked,
 * <ul>
 * <li> {@link #isDistractionOptimizationRequired()} returns {@code false}.  Apps can display
 * activities that are not distraction optimized.
 * <li> When {@link #isDistractionOptimizationRequired()} returns {@code false}, apps don't have to
 * call {@link #getActiveRestrictions()}, since there is no distraction optimization required.
 * </ul>
 * <li> When the driving state changes, causing the UX restrictions to come in effect,
 * <ul>
 * <li> {@link #isDistractionOptimizationRequired()} returns {@code true}.  Apps can only display
 * activities that are distraction optimized.  Distraction optimized activities must follow the base
 * design guidelines to ensure a distraction free driving experience for the user.
 * <li> When {@link #isDistractionOptimizationRequired()} returns {@code true}, apps must call
 * {@link #getActiveRestrictions()}, to get the currently active UX restrictions to adhere to.
 * {@link #getActiveRestrictions()} provides additional information on the set of UX
 * restrictions that are in place for the current driving state.
 * <p>
 * The UX restrictions returned by {@link #getActiveRestrictions()}, for the same driving state of
 * the vehicle, could vary depending on the OEM and the market.  For example, when the car is
 * idling, the set of active UX restrictions will depend on the car maker and the safety standards
 * of the market that the vehicle is deployed in.
 * </ul>
 * </ul>
 * <p>
 * Apps that intend to be run when the car is being driven need to
 * <ul>
 * <li> Comply with the general distraction optimization guidelines.
 * <li> Listen and react to the UX restrictions changes as detailed above.  Since the restrictions
 * could vary depending on the market, apps are expected to react to the restriction information
 * and not to the absolute driving state.
 * </ul>
 */
public final class CarUxRestrictions {

    // Default fallback values for the restriction related parameters if the information is
    // not available from the underlying service.
    private static final int DEFAULT_MAX_LENGTH = 120;
    private static final int DEFAULT_MAX_CUMULATIVE_ITEMS = 21;
    private static final int DEFAULT_MAX_CONTENT_DEPTH = 3;

    /**
     * No specific restrictions in place, but baseline distraction optimization guidelines need to
     * be adhered to when {@link #isDistractionOptimizationRequired()} is {@code true}.
     */
    public static final int UX_RESTRICTIONS_BASELINE = 0;

    // Granular UX Restrictions that are imposed when distraction optimization is required.
    /**
     * No dialpad for the purpose of initiating a phone call.
     */
    public static final int UX_RESTRICTIONS_NO_DIALPAD = 1;

    /**
     * No filtering a list.
     */
    public static final int UX_RESTRICTIONS_NO_FILTERING = 0x1 << 1;

    /**
     * General purpose strings length cannot exceed the character limit provided by
     * {@link #getMaxRestrictedStringLength()}
     */
    public static final int UX_RESTRICTIONS_LIMIT_STRING_LENGTH = 0x1 << 2;

    /**
     * No text entry for the purpose of searching etc.
     */
    public static final int UX_RESTRICTIONS_NO_KEYBOARD = 0x1 << 3;

    /**
     * No video - no animated frames > 1fps.
     */
    public static final int UX_RESTRICTIONS_NO_VIDEO = 0x1 << 4;

    /**
     * Limit the number of items displayed on the screen.
     * Refer to {@link #getMaxCumulativeContentItems()} and
     * {@link #getMaxContentDepth()} for the upper bounds on content
     * serving.
     */
    public static final int UX_RESTRICTIONS_LIMIT_CONTENT = 0x1 << 5;

    /**
     * No setup that requires form entry or interaction with external devices.
     */
    public static final int UX_RESTRICTIONS_NO_SETUP = 0x1 << 6;

    /**
     * No Text Message (SMS, email, conversational, etc.)
     */
    public static final int UX_RESTRICTIONS_NO_TEXT_MESSAGE = 0x1 << 7;

    /**
     * No text transcription (live or leave behind) of voice can be shown.
     */
    public static final int UX_RESTRICTIONS_NO_VOICE_TRANSCRIPTION = 0x1 << 8;


    /**
     * All the above restrictions are in effect.
     */
    public static final int UX_RESTRICTIONS_FULLY_RESTRICTED =
            UX_RESTRICTIONS_NO_DIALPAD | UX_RESTRICTIONS_NO_FILTERING
                    | UX_RESTRICTIONS_LIMIT_STRING_LENGTH | UX_RESTRICTIONS_NO_KEYBOARD
                    | UX_RESTRICTIONS_NO_VIDEO | UX_RESTRICTIONS_LIMIT_CONTENT
                    | UX_RESTRICTIONS_NO_SETUP | UX_RESTRICTIONS_NO_TEXT_MESSAGE
                    | UX_RESTRICTIONS_NO_VOICE_TRANSCRIPTION;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef(flag = true,
            value = {UX_RESTRICTIONS_BASELINE,
                    UX_RESTRICTIONS_NO_DIALPAD,
                    UX_RESTRICTIONS_NO_FILTERING,
                    UX_RESTRICTIONS_LIMIT_STRING_LENGTH,
                    UX_RESTRICTIONS_NO_KEYBOARD,
                    UX_RESTRICTIONS_NO_VIDEO,
                    UX_RESTRICTIONS_LIMIT_CONTENT,
                    UX_RESTRICTIONS_NO_SETUP,
                    UX_RESTRICTIONS_NO_TEXT_MESSAGE,
                    UX_RESTRICTIONS_NO_VOICE_TRANSCRIPTION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CarUxRestrictionsInfo {
    }

    private final long mTimestamp;
    private final boolean mIsDistractionOptimizationRequired;
    @CarUxRestrictionsInfo
    private final int mActiveRestrictions;
    // Restriction Parameters
    private final int mMaxStringLength;
    private final int mMaxCumulativeContentItems;
    private final int mMaxContentDepth;

    /**
     * Builder class for {@link CarUxRestrictions}
     */
    public static final class Builder {
        final long mTimestamp;
        final boolean mRequiresDistractionOptimization;
        @CarUxRestrictionsInfo
        final int mActiveRestrictions;
        // Restriction parameters.
        int mMaxStringLength = DEFAULT_MAX_LENGTH;
        int mMaxCumulativeContentItems = DEFAULT_MAX_CUMULATIVE_ITEMS;
        int mMaxContentDepth = DEFAULT_MAX_CONTENT_DEPTH;

        public Builder(boolean requiresDistractionOptimization,
                @CarUxRestrictionsInfo int activeRestrictions, long timestamp) {
            mRequiresDistractionOptimization = requiresDistractionOptimization;
            mActiveRestrictions = activeRestrictions;
            mTimestamp = timestamp;
        }

        /**
         * Set the maximum length of general purpose strings that can be displayed when
         * {@link CarUxRestrictions#UX_RESTRICTIONS_LIMIT_STRING_LENGTH} is imposed.
         */
        @NonNull
        public Builder setMaxStringLength(int maxStringLength) {
            mMaxStringLength = maxStringLength;
            return this;
        }

        /**
         *  Set the maximum number of cumulative content items that can be displayed when
         * {@link CarUxRestrictions#UX_RESTRICTIONS_LIMIT_CONTENT} is imposed.
         */
        @NonNull
        public Builder setMaxCumulativeContentItems(int maxCumulativeContentItems) {
            mMaxCumulativeContentItems = maxCumulativeContentItems;
            return this;
        }

        /**
         * Set the maximum number of levels that the user can navigate to when
         * {@link CarUxRestrictions#UX_RESTRICTIONS_LIMIT_CONTENT} is imposed.
         */
        @NonNull
        public Builder setMaxContentDepth(int maxContentDepth) {
            mMaxContentDepth = maxContentDepth;
            return this;
        }

        /**
         * Build and return the {@link CarUxRestrictions} object
         */
        @NonNull
        public CarUxRestrictions build() {
            return new CarUxRestrictions(this);
        }

    }

    /**
     * Time at which this UX restriction event was deduced based on the car's driving state.
     *
     * @return Elapsed time in nanoseconds since system boot.
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Conveys if the foreground activity needs to be distraction optimized.
     * Activities that can handle distraction optimization need to be tagged as a distraction
     * optimized in the app's manifest.
     * <p>
     * If the app has a foreground activity that has not been distraction optimized, the app has
     * to switch to another activity that is distraction optimized.  Failing that, the system will
     * stop the foreground activity.
     *
     * @return {@code true} if distraction optimization is required, {@code false} if not
     */
    public boolean isDistractionOptimizationRequired() {
        return mIsDistractionOptimizationRequired;
    }

    /**
     * A combination of the Car UX Restrictions that is active for the current state of driving.
     *
     * @return A combination of the above {@code @CarUxRestrictionsInfo}
     */
    @CarUxRestrictionsInfo
    public int getActiveRestrictions() {
        return mActiveRestrictions;
    }

    /**
     * Get the maximum length of general purpose strings that can be displayed when
     * {@link CarUxRestrictions#UX_RESTRICTIONS_LIMIT_STRING_LENGTH} is imposed.
     *
     * @return the maximum length of string that can be displayed
     */
    public int getMaxRestrictedStringLength() {
        return mMaxStringLength;
    }

    /**
     * Get the maximum allowable number of content items that can be displayed to a user during
     * traversal through any one path in a single task, when
     * {@link CarUxRestrictions#UX_RESTRICTIONS_LIMIT_CONTENT} is imposed.
     * <p>
     * For example, if a task involving only one view, this represents the maximum allowable number
     * of content items in this single view.
     * <p>
     * However, if the task involves selection of a content item in an originating view that then
     * surfaces a secondary view to the user, then this value represents the maximum allowable
     * number of content items between the originating and secondary views combined.
     * <p>
     * Specifically, if the maximum allowable value was 60 and a task involved browsing a list of
     * countries and then viewing the top songs within a country, it would be acceptable to do
     * either of the following:
     * <ul>
     * <li> list 10 countries, and then display the top 50 songs after country selection, or
     * <li> list 20 countries, and then display the top 40 songs after country selection.
     * </ul>
     * <p>
     * Please refer to this and {@link #getMaxContentDepth()} to know the upper bounds on the
     * content display when the restriction is in place.
     *
     * @return maximum number of cumulative items that can be displayed
     */
    public int getMaxCumulativeContentItems() {
        return mMaxCumulativeContentItems;
    }

    /**
     * Get the maximum allowable number of content depth levels or view traversals through any one
     * path in a single task.  This is applicable when
     * {@link CarUxRestrictions#UX_RESTRICTIONS_LIMIT_CONTENT} is imposed.
     * <p>
     * For example, if a task involves only selecting an item from a single list on one view,
     * the task's content depth would be considered 1.
     * <p>
     * However, if the task involves selection of a content item in an originating view that then
     * surfaces a secondary view to the user, the task's content depth would be considered 2.
     * <p>
     * Specifically, if a task involved browsing a list of countries, selecting a genre within the
     * country, and then viewing the top songs within a country, the task's content depth would be
     * considered 3.
     * <p>
     * Please refer to this and {@link #getMaxCumulativeContentItems()} to know the upper bounds on
     * the content display when the restriction is in place.
     *
     * @return maximum number of cumulative items that can be displayed
     */
    public int getMaxContentDepth() {
        return mMaxContentDepth;
    }

    public CarUxRestrictions(CarUxRestrictions uxRestrictions) {
        mTimestamp = uxRestrictions.getTimestamp();
        mIsDistractionOptimizationRequired = uxRestrictions.isDistractionOptimizationRequired();
        mActiveRestrictions = uxRestrictions.getActiveRestrictions();
        mMaxStringLength = uxRestrictions.mMaxStringLength;
        mMaxCumulativeContentItems = uxRestrictions.mMaxCumulativeContentItems;
        mMaxContentDepth = uxRestrictions.mMaxContentDepth;
    }

    /**
     * Copy constructor for framework class.
     *
     * @param uxRestrictions UXR sent by the framework
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public CarUxRestrictions(android.car.drivingstate.CarUxRestrictions uxRestrictions) {
        mTimestamp = uxRestrictions.getTimeStamp();
        mIsDistractionOptimizationRequired = uxRestrictions.isRequiresDistractionOptimization();
        mActiveRestrictions = uxRestrictions.getActiveRestrictions();
        mMaxStringLength = uxRestrictions.getMaxRestrictedStringLength();
        mMaxCumulativeContentItems = uxRestrictions.getMaxCumulativeContentItems();
        mMaxContentDepth = uxRestrictions.getMaxContentDepth();
    }

    CarUxRestrictions(Builder builder) {
        mTimestamp = builder.mTimestamp;
        mActiveRestrictions = builder.mActiveRestrictions;
        mIsDistractionOptimizationRequired = builder.mRequiresDistractionOptimization;
        mMaxStringLength = builder.mMaxStringLength;
        mMaxCumulativeContentItems = builder.mMaxCumulativeContentItems;
        mMaxContentDepth = builder.mMaxContentDepth;
    }

    @Override
    public String toString() {
        return "DO: " + mIsDistractionOptimizationRequired + " UxR: " + mActiveRestrictions
                + " time: " + mTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        CarUxRestrictions other = (CarUxRestrictions) o;
        return (mTimestamp == other.getTimestamp()
                && mIsDistractionOptimizationRequired == other.isDistractionOptimizationRequired()
                && mActiveRestrictions == other.getActiveRestrictions()
                && mMaxStringLength == other.getMaxRestrictedStringLength()
                && mMaxCumulativeContentItems == other.getMaxCumulativeContentItems()
                && mMaxContentDepth == other.getMaxContentDepth());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTimestamp, mIsDistractionOptimizationRequired, mActiveRestrictions,
                mMaxStringLength, mMaxCumulativeContentItems, mMaxContentDepth);
    }


    /**
     * Compares if the restrictions are the same.  Doesn't compare the timestamps.
     *
     * @param other the other CarUxRestrictions object
     * @return {@code true} if the restrictions are same, {@code false} otherwise
     */
    @SuppressWarnings("ReferenceEquality")
    public boolean hasSameRestrictionsAs(CarUxRestrictions other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        return other.mIsDistractionOptimizationRequired == mIsDistractionOptimizationRequired
                && other.mActiveRestrictions == mActiveRestrictions;
    }
}
