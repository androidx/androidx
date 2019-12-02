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

package androidx.camera.core;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A configuration used to trigger a focus and/or metering action.
 *
 * <p>To construct a {@link FocusMeteringAction}, apps have to create a {@link Builder} by
 * {@link Builder#from(MeteringPoint)} or {@link Builder#from(MeteringPoint, int)}.
 * {@link MeteringPoint} is a point used to specify the focus/metering areas. Apps can use various
 * {@link MeteringPointFactory} to create the points. When the {@link FocusMeteringAction} is built,
 * pass it to {@link CameraControl#startFocusAndMetering(FocusMeteringAction)} to initiate the focus
 * and metering action.
 *
 * <p>The default meteringMode is {@link #FLAG_AF} | {@link #FLAG_AE} | {@link #FLAG_AWB} which
 * means the point is used for all AF/AE/AWB regions. Apps can set the proper meteringMode to
 * optionally exclude some 3A regions. Multiple regions for specific 3A type are also supported
 * via {@link Builder#addPoint(MeteringPoint)} or {@link Builder#addPoint(MeteringPoint, int)}.
 * App can also this API to enable different region for AF and AE respectively.
 *
 * <p>If any AF points are specified, it will trigger autofocus to start a manual scan. When
 * focus is locked and specified AF/AE/AWB regions are updated in capture result, the returned
 * {@link ListenableFuture} in {@link CameraControl#startFocusAndMetering(FocusMeteringAction)}
 * will completed with {@link FocusMeteringResult#isFocusSuccessful()} set to indicate if focus is
 * done successfully or not. If AF point is not specified, it will not trigger autofocus and
 * simply wait for specified AE/AWB regions being updated to complete the returned
 * {@link ListenableFuture}. In the case of AF points not specified,
 * {@link FocusMeteringResult#isFocusSuccessful()} will be set to false. If Af points are
 * specified but current camera does not support auto focus,
 * {@link FocusMeteringResult#isFocusSuccessful()} will be set to true .
 *
 * <p>App can set a auto-cancel duration to let CameraX call
 * {@link CameraControl#cancelFocusAndMetering()} automatically in the specified duration. By
 * default the auto-cancel duration is 5 seconds. Apps can call {@link Builder#disableAutoCancel()}
 * to disable auto-cancel.
 */
public final class FocusMeteringAction {

    public static final int FLAG_AF = 1;
    public static final int FLAG_AE = 1 << 1;
    public static final int FLAG_AWB = 1 << 2;

    @MeteringMode
    static final int DEFAULT_METERINGMODE = FLAG_AF | FLAG_AE | FLAG_AWB;
    static final long DEFAULT_AUTOCANCEL_DURATION = 5000;
    private final List<MeteringPoint> mMeteringPointsAf;
    private final List<MeteringPoint> mMeteringPointsAe;
    private final List<MeteringPoint> mMeteringPointsAwb;
    private final long mAutoCancelDurationInMillis;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    FocusMeteringAction(Builder builder) {
        mMeteringPointsAf = Collections.unmodifiableList(builder.mMeteringPointsAf);
        mMeteringPointsAe = Collections.unmodifiableList(builder.mMeteringPointsAe);
        mMeteringPointsAwb = Collections.unmodifiableList(builder.mMeteringPointsAwb);
        mAutoCancelDurationInMillis = builder.mAutoCancelDurationInMillis;
    }

    /**
     * Returns auto-cancel duration.  Returns 0 if auto-cancel is disabled.
     */
    public long getAutoCancelDurationInMillis() {
        return mAutoCancelDurationInMillis;
    }

    /**
     * Returns all {@link MeteringPoint}s used for AF regions.
     */
    @NonNull
    public List<MeteringPoint> getMeteringPointsAf() {
        return mMeteringPointsAf;
    }

    /**
     * Returns all {@link MeteringPoint}s used for AE regions.
     */
    @NonNull
    public List<MeteringPoint> getMeteringPointsAe() {
        return mMeteringPointsAe;
    }

    /**
     * Returns all {@link MeteringPoint}s used for AWB regions.
     */
    @NonNull
    public List<MeteringPoint> getMeteringPointsAwb() {
        return mMeteringPointsAwb;
    }

    /**
     * Returns if auto-cancel is enabled or not.
     */
    public boolean isAutoCancelEnabled() {
        return mAutoCancelDurationInMillis > 0;
    }

    /**
     * Focus/Metering mode used to specify which 3A regions is activated for corresponding
     * {@link MeteringPoint}.
     *
     * @hide
     */
    @IntDef(flag = true, value = {FLAG_AF, FLAG_AE, FLAG_AWB})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface MeteringMode {
    }

    /**
     * The builder used to create the {@link FocusMeteringAction}. App must use
     * {@link Builder#from(MeteringPoint)}
     * or {@link Builder#from(MeteringPoint, int)} to create the {@link Builder}.
     */
    public static class Builder {
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        final List<MeteringPoint> mMeteringPointsAf = new ArrayList<>();
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        final List<MeteringPoint> mMeteringPointsAe = new ArrayList<>();
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        final List<MeteringPoint> mMeteringPointsAwb = new ArrayList<>();
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
                long mAutoCancelDurationInMillis = DEFAULT_AUTOCANCEL_DURATION;

        private Builder(@NonNull MeteringPoint point) {
            this(point, DEFAULT_METERINGMODE);
        }

        private Builder(@NonNull MeteringPoint point, @MeteringMode int mode) {
            addPoint(point, mode);
        }

        /**
         * Creates the Builder from a {@link MeteringPoint} with default
         * mode {@link #FLAG_AF} | {@link #FLAG_AE} | {@link #FLAG_AWB}.
         */
        @NonNull
        public static Builder from(@NonNull MeteringPoint meteringPoint) {
            return new Builder(meteringPoint);
        }

        /**
         * Creates the Builder from a {@link MeteringPoint} and MeteringMode.
         */
        @NonNull
        public static Builder from(@NonNull MeteringPoint meteringPoint, @MeteringMode int mode) {
            return new Builder(meteringPoint, mode);
        }

        /**
         * Adds another {@link MeteringPoint} with default mode {@link #FLAG_AF} |
         * {@link #FLAG_AE} | {@link #FLAG_AWB}.
         *
         * <p>The points added here will be appended in order after the point set in
         * {@link FocusMeteringAction.Builder#from(MeteringPoint)} or
         * {@link FocusMeteringAction.Builder#from(MeteringPoint, int)}.
         *
         * <p>If more points are added than what current device supports for AF/AE/AWB, only the
         * first point and then in order up to the number of points supported on the device
         * will be enabled.
         *
         * <p>If none of the points is supported on the device, this
         * {@link FocusMeteringAction} will cause
         * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)} to fail.
         *
         * @see CameraControl#startFocusAndMetering(FocusMeteringAction)
         */
        @NonNull
        public Builder addPoint(@NonNull MeteringPoint point) {
            return addPoint(point, DEFAULT_METERINGMODE);
        }

        /**
         * Adds another {@link MeteringPoint} with specified meteringMode.
         *
         * <p>The points added here will be appended in order after the point set in
         * {@link FocusMeteringAction.Builder#from(MeteringPoint)} or
         * {@link FocusMeteringAction.Builder#from(MeteringPoint, int)}.
         *
         * <p>If more points are added than what current device supports for AF/AE/AWB, only the
         * first point and then in order up to the number of points supported on the device
         * will be enabled.
         *
         * <p>If none of the points is supported on the device, this
         * {@link FocusMeteringAction} will cause
         * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)} to fail.
         *
         * @see CameraControl#startFocusAndMetering(FocusMeteringAction)
         */
        @NonNull
        public Builder addPoint(@NonNull MeteringPoint point, @MeteringMode int mode) {
            Preconditions.checkArgument(
                    (mode >= FLAG_AF) && (mode <= (FLAG_AF | FLAG_AE | FLAG_AWB)),
                    "Invalid metering mode " + mode);

            if ((mode & FLAG_AF) != 0) {
                mMeteringPointsAf.add(point);
            }
            if ((mode & FLAG_AE) != 0) {
                mMeteringPointsAe.add(point);
            }
            if ((mode & FLAG_AWB) != 0) {
                mMeteringPointsAwb.add(point);
            }
            return this;
        }

        /**
         * Sets the auto-cancel duration. After set, {@link CameraControl#cancelFocusAndMetering()}
         * will be called in specified duration. By default, auto-cancel is enabled with 5
         * seconds duration. The duration must be greater than or equal to 1 otherwise it
         * will throw a {@link IllegalArgumentException}.
         */
        @NonNull
        public Builder setAutoCancelDuration(@IntRange(from = 1) long duration,
                @NonNull TimeUnit timeUnit) {
            Preconditions.checkArgument(duration >= 1, "autoCancelDuration must be at least 1");
            mAutoCancelDurationInMillis = timeUnit.toMillis(duration);
            return this;
        }

        /**
         * Disables the auto-cancel.
         */
        @NonNull
        public Builder disableAutoCancel() {
            mAutoCancelDurationInMillis = 0;
            return this;
        }

        /**
         * Builds the {@link FocusMeteringAction} instance.
         */
        @NonNull
        public FocusMeteringAction build() {
            return new FocusMeteringAction(this);
        }

    }
}
