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

import android.graphics.PointF;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A configuration used to trigger a focus and/or metering action.
 *
 * <p>A {@link FocusMeteringAction} must be created by the {@link Builder}. To construct a
 * {@link Builder}, a {@link MeteringPoint} is required to specify the focus/metering area. Apps
 * can use various {@link MeteringPointFactory} to create the points. After the
 * {@link FocusMeteringAction} is built, apps can pass it to
 * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)} to initiate the focus
 * and metering action.
 *
 * <p>When specifying a {@link MeteringPoint}, a metering mode can also be specified. Metering
 * mode is a combination of flags consisting of {@link #FLAG_AF}, {@link #FLAG_AE}, and
 * {@link #FLAG_AWB}. This combination indicates whether the {@link MeteringPoint} is
 * used to set an AF(Auto Focus) region, AE(Auto Exposure) region or AWB(Auto White Balance) region.
 * The default meteringMode is {@link #FLAG_AF} | {@link #FLAG_AE} | {@link #FLAG_AWB} which
 * means the point is used for all AF/AE/AWB regions. Apps can set the proper metering mode to
 * optionally exclude some 3A regions. Multiple regions for specific 3A types are also supported
 * via {@link Builder#addPoint(MeteringPoint)} or {@link Builder#addPoint(MeteringPoint, int)}.
 * An app can also use this API to enable different regions for AF and AE respectively.
 *
 * <p>When the action is submitted via
 * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)}, the following operations
 * are performed:
 * <ul>
 * <li><b>Region Updates:</b> The camera's metering regions are updated based on the specified
 * {@link MeteringPoint}s.
 *   <ul>
 *     <li>This update happens regardless of the locking modes set for AF, AE, and AWB.
 *     <li>If a 3A component (AF/AE/AWB) does not support metering regions on the device, or if
 *     the maximum region count is 0, the points for that component are ignored.
 *     <li>If more points are specified than supported, only the first supported points are used.
 *   </ul>
 * <li><b>Trigger and Lock:</b> The camera then applies the locks specified in the locking mode
 * (see {@link Builder#setLockingMode(int)}):
 *   <ul>
 *     <li><b>AF:</b> If AF points are specified and {@link #FLAG_AF} is included in the locking
 *     mode (which is the default), it triggers an autofocus manual scan and locks focus. If
 *     {@link #FLAG_AF} is NOT in the locking mode, it updates the AF region without triggering a
 *     scan, allowing the camera to continue in its current AF mode (e.g., continuous autofocus).
 *     <li><b>AE/AWB:</b> If AE/AWB points are specified and {@link #FLAG_AE} / {@link #FLAG_AWB}
 *     are included in the locking mode, the camera locks the exposure and white balance
 *     respectively. If not included, the regions are updated but exposure and white balance
 *     continue to adjust automatically.
 *   </ul>
 * <li><b>Completion:</b> The returned {@link ListenableFuture} completes when the regions are
 * updated and the requested locks are acquired. {@link FocusMeteringResult#isFocusSuccessful()}
 * will be {@code true} if an AF lock was requested and successfully acquired, or if AF is not
 * supported on the device. It will be {@code false} if the AF lock failed, if no AF points were
 * specified, or if {@link #FLAG_AF} was excluded from the locking mode.
 * </ul>
 *
 * <p>App can set a auto-cancel duration to let CameraX call
 * {@link CameraControl#cancelFocusAndMetering()} automatically in the specified duration. By
 * default, the auto-cancel duration is 5 seconds. Apps can call {@link Builder#disableAutoCancel()}
 * to disable auto-cancel.
 *
 * <p>If a focus-metering action is completed with
 * {@link FocusMeteringResult#isFocusSuccessful()} {@code true}, the focus distance will be
 * locked and continuous auto-focus will be disabled. Continuous autofocus will be re-enabled
 * when {@link CameraControl#cancelFocusAndMetering()} is called or the auto-cancel duration is
 * reached.
 *
 * <p>AE (Auto Exposure) and AWB (Auto White Balance) can also be locked if they are enabled
 * in {@link Builder#setLockingMode(int)}. Locking mode is a combination of flags consisting
 * of {@link #FLAG_AF}, {@link #FLAG_AE}, and {@link #FLAG_AWB}. For example, to lock both AF and
 * AE, use {@code FLAG_AF | FLAG_AE}.
 */
public final class FocusMeteringAction {

    /**
     * A flag used in metering mode indicating the AF (Auto Focus) region is enabled. An autofocus
     * scan is also triggered when FLAG_AF is assigned.
     */
    public static final int FLAG_AF = 1;

    /**
     * A flag used in metering mode indicating the AE (Auto Exposure) region is enabled.
     */
    public static final int FLAG_AE = 1 << 1;

    /**
     * A flag used in metering mode indicating the AWB (Auto White Balance) region is enabled.
     */
    public static final int FLAG_AWB = 1 << 2;

    @MeteringMode
    static final int DEFAULT_METERING_MODE = FLAG_AF | FLAG_AE | FLAG_AWB;

    @MeteringMode
    static final int DEFAULT_LOCKING_MODE = FLAG_AF;

    /** The default duration for auto-cancelling a focus-metering action. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final long DEFAULT_AUTO_CANCEL_DURATION_MILLIS = 5000;

    static final float AF_SIZE = 1.0f / 6.0f;
    static final float AE_SIZE = AF_SIZE * 1.5f;

    private final List<MeteringPoint> mMeteringPointsAf;
    private final List<MeteringPoint> mMeteringPointsAe;
    private final List<MeteringPoint> mMeteringPointsAwb;
    private final long mAutoCancelDurationInMillis;

    @MeteringMode
    private final int mLockingMode;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    FocusMeteringAction(Builder builder) {
        mMeteringPointsAf = Collections.unmodifiableList(builder.mMeteringPointsAf);
        mMeteringPointsAe = Collections.unmodifiableList(builder.mMeteringPointsAe);
        mMeteringPointsAwb = Collections.unmodifiableList(builder.mMeteringPointsAwb);
        mAutoCancelDurationInMillis = builder.mAutoCancelDurationInMillis;
        mLockingMode = builder.mLockingMode;
    }

    /**
     * Returns auto-cancel duration. Returns {@code 0} if auto-cancel is disabled.
     */
    public long getAutoCancelDurationInMillis() {
        return mAutoCancelDurationInMillis;
    }

    /**
     * Returns all {@link MeteringPoint}s used for AF regions.
     */
    public @NonNull List<MeteringPoint> getMeteringPointsAf() {
        return mMeteringPointsAf;
    }

    /**
     * Returns all {@link MeteringPoint}s used for AE regions.
     */
    public @NonNull List<MeteringPoint> getMeteringPointsAe() {
        return mMeteringPointsAe;
    }

    /**
     * Returns all {@link MeteringPoint}s used for AWB regions.
     */
    public @NonNull List<MeteringPoint> getMeteringPointsAwb() {
        return mMeteringPointsAwb;
    }

    /**
     * Returns if auto-cancel is enabled or not.
     */
    public boolean isAutoCancelEnabled() {
        return mAutoCancelDurationInMillis > 0;
    }

    /**
     * Returns the locking mode.
     *
     * <p>Locking mode is a combination of flags consisting of {@link #FLAG_AF},
     * {@link #FLAG_AE}, and {@link #FLAG_AWB}. This combination indicates whether the
     * AF (Auto Focus), AE (Auto Exposure) or AWB (Auto White Balance) should be locked after
     * focus and metering action is completed.
     */
    @MeteringMode
    public int getLockingMode() {
        return mLockingMode;
    }

    /**
     * Creates a {@link FocusMeteringAction} with a single AF/AE point.
     *
     * <p>The created {@link FocusMeteringAction} will have one AF point and one AE point
     * at the same location. The AF point will have a size of 1/6 of the surface width, and the
     * AE point will have a size of 1.5 times the AF point size.
     *
     * @param meteringPointFactory The {@link MeteringPointFactory} to create points.
     * @param point The point to focus and meter on.
     * @param autoCancelDurationInMillis The duration in milliseconds for auto-cancel. If 0,
     *                                   auto-cancel will be disabled.
     * @return The created {@link FocusMeteringAction}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static @NonNull FocusMeteringAction create(
            @NonNull MeteringPointFactory meteringPointFactory,
            @NonNull PointF point,
            @IntRange(from = 0) long autoCancelDurationInMillis) {
        Builder builder = new Builder(meteringPointFactory.createPoint(point.x, point.y, AF_SIZE),
                FLAG_AF)
                .addPoint(meteringPointFactory.createPoint(point.x, point.y, AE_SIZE), FLAG_AE);
        if (autoCancelDurationInMillis >= 1) {
            builder.setAutoCancelDuration(autoCancelDurationInMillis, TimeUnit.MILLISECONDS);
        } else {
            builder.disableAutoCancel();
        }
        return builder.build();
    }

    /**
     * Focus/Metering mode used to specify which 3A regions is activated for corresponding
     * {@link MeteringPoint}.
     */
    @IntDef(flag = true, value = {FLAG_AF, FLAG_AE, FLAG_AWB})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface MeteringMode {
    }

    /**
     * The builder used to create the {@link FocusMeteringAction}.
     */
    public static class Builder {
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        final List<MeteringPoint> mMeteringPointsAf = new ArrayList<>();
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        final List<MeteringPoint> mMeteringPointsAe = new ArrayList<>();
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        final List<MeteringPoint> mMeteringPointsAwb = new ArrayList<>();
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
                long mAutoCancelDurationInMillis = DEFAULT_AUTO_CANCEL_DURATION_MILLIS;
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        @MeteringMode
        int mLockingMode = DEFAULT_LOCKING_MODE;

        /**
         * Creates a Builder from a {@link MeteringPoint} with default mode {@link #FLAG_AF} |
         * {@link #FLAG_AE} | {@link #FLAG_AWB}.
         *
         * <p>The default locking mode is {@link #FLAG_AF}. Use {@link #setLockingMode(int)} to lock
         * AE or AWB as well.
         */
        public Builder(@NonNull MeteringPoint point) {
            this(point, DEFAULT_METERING_MODE);
        }

        /**
         * Creates a Builder from a {@link MeteringPoint} and MeteringMode.
         *
         * <p>Metering mode is a combination of flags consisting of {@link #FLAG_AF},
         * {@link #FLAG_AE}, and {@link #FLAG_AWB}. This combination indicates whether the
         * {@link MeteringPoint} is used to set AF(Auto Focus) region, AE(Auto
         * Exposure) region or AWB(Auto White Balance) region.
         *
         * <p>The default locking mode is {@link #FLAG_AF}. Use {@link #setLockingMode(int)} to lock
         * AE or AWB as well.
         */
        public Builder(@NonNull MeteringPoint point, @MeteringMode int meteringMode) {
            addPoint(point, meteringMode);
        }

        /**
         * Create a Builder from a {@link FocusMeteringAction}.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public Builder(@NonNull FocusMeteringAction focusMeteringAction) {
            mMeteringPointsAf.addAll(focusMeteringAction.getMeteringPointsAf());
            mMeteringPointsAe.addAll(focusMeteringAction.getMeteringPointsAe());
            mMeteringPointsAwb.addAll(focusMeteringAction.getMeteringPointsAwb());
            mAutoCancelDurationInMillis = focusMeteringAction.getAutoCancelDurationInMillis();
            mLockingMode = focusMeteringAction.getLockingMode();
        }

        /**
         * Adds another {@link MeteringPoint} with default metering mode {@link #FLAG_AF} |
         * {@link #FLAG_AE} | {@link #FLAG_AWB}.
         *
         * <p>The points added here will be appended in order after the point set in builder
         * constructor.
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
        public @NonNull Builder addPoint(@NonNull MeteringPoint point) {
            return addPoint(point, DEFAULT_METERING_MODE);
        }

        /**
         * Adds another {@link MeteringPoint} with specified meteringMode.
         *
         * <p>Metering mode is a combination of flags consisting of {@link #FLAG_AF},
         * {@link #FLAG_AE}, and {@link #FLAG_AWB}. This combination indicates whether the
         * {@link MeteringPoint} is used to set AF(Auto Focus) region, AE(Auto Exposure) region
         * or AWB(Auto White Balance) region.
         *
         * <p>The points added here will be appended in order after the point set in builder
         * constructor.
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
        public @NonNull Builder addPoint(@NonNull MeteringPoint point,
                @MeteringMode int meteringMode) {
            Preconditions.checkArgument(point != null, "Point cannot be null.");
            Preconditions.checkArgument(
                    (meteringMode >= FLAG_AF) && (meteringMode <= (FLAG_AF | FLAG_AE | FLAG_AWB)),
                    "Invalid metering mode " + meteringMode);

            if ((meteringMode & FLAG_AF) != 0) {
                mMeteringPointsAf.add(point);
            }
            if ((meteringMode & FLAG_AE) != 0) {
                mMeteringPointsAe.add(point);
            }
            if ((meteringMode & FLAG_AWB) != 0) {
                mMeteringPointsAwb.add(point);
            }
            return this;
        }

        /**
         * Sets the locking mode.
         *
         * <p>Locking mode is a combination of flags consisting of {@link #FLAG_AF},
         * {@link #FLAG_AE}, and {@link #FLAG_AWB}. This combination indicates whether the
         * AF (Auto Focus), AE (Auto Exposure) or AWB (Auto White Balance) should be locked after
         * focus and metering action is completed. For example, to lock both AF and AE, use
         * {@code FLAG_AF | FLAG_AE}.
         *
         * <p>Locking will only occur if the corresponding 3A component has at least one
         * {@link MeteringPoint} specified in this action and the camera device supports locking
         * for that component.
         *
         * <p>By default, only {@link #FLAG_AF} is set. Apps can also use {@code 0} to disable
         * any 3A locking while still updating the 3A regions. For example, if AF points are
         * specified but {@link #FLAG_AF} is not included in the locking mode set through this API,
         * the AF region will be updated but the camera will not explicitly trigger an autofocus
         * scan. In this case, how and when the camera focuses on the new region will depend on the
         * current AF mode (e.g., whether it is a continuous autofocus mode) and device-specific
         * behavior.
         *
         * @param lockingMode a combination of flags consisting of {@link #FLAG_AF},
         *                    {@link #FLAG_AE}, and {@link #FLAG_AWB}.
         * @return the current {@link Builder}.
         * @throws IllegalArgumentException if the locking mode is invalid.
         */
        public @NonNull Builder setLockingMode(@MeteringMode int lockingMode) {
            Preconditions.checkArgument(
                    (lockingMode >= 0) && (lockingMode <= (FLAG_AF | FLAG_AE | FLAG_AWB)),
                    "Invalid locking mode " + lockingMode);
            mLockingMode = lockingMode;
            return this;
        }

        /**
         * Sets the auto-cancel duration. After set, {@link CameraControl#cancelFocusAndMetering()}
         * will be called in specified duration. By default, auto-cancel is enabled with 5
         * seconds duration. The duration must be greater than or equal to 1 otherwise it
         * will throw a {@link IllegalArgumentException}.
         */
        public @NonNull Builder setAutoCancelDuration(@IntRange(from = 1) long duration,
                @NonNull TimeUnit timeUnit) {
            Preconditions.checkArgument(duration >= 1, "autoCancelDuration must be at least 1");
            mAutoCancelDurationInMillis = timeUnit.toMillis(duration);
            return this;
        }

        /**
         * Disables the auto-cancel.
         */
        public @NonNull Builder disableAutoCancel() {
            mAutoCancelDurationInMillis = 0;
            return this;
        }

        /**
         *
         * Removes all points of the given metering mode.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @NonNull Builder removePoints(@MeteringMode int meteringMode) {
            if ((meteringMode & FLAG_AF) != 0) {
                mMeteringPointsAf.clear();
            }

            if ((meteringMode & FLAG_AE) != 0) {
                mMeteringPointsAe.clear();
            }

            if ((meteringMode & FLAG_AWB) != 0) {
                mMeteringPointsAwb.clear();
            }
            return this;
        }

        /**
         * Builds the {@link FocusMeteringAction} instance.
         */
        public @NonNull FocusMeteringAction build() {
            return new FocusMeteringAction(this);
        }

    }
}
