/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.location;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.lang.Math.min;

import android.location.LocationRequest;
import android.os.Build.VERSION;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.core.util.TimeUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Compatibility version of {@link LocationRequest}.
 */
public final class LocationRequestCompat {

    /**
     * Represents a passive only request. Such a request will not trigger any active locations or
     * power usage itself, but may receive locations generated in response to other requests.
     *
     * @see LocationRequestCompat#getIntervalMillis()
     */
    public static final long PASSIVE_INTERVAL = LocationRequest.PASSIVE_INTERVAL;

    /** @hide */
    @RestrictTo(LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({QUALITY_LOW_POWER, QUALITY_BALANCED_POWER_ACCURACY, QUALITY_HIGH_ACCURACY})
    public @interface Quality {
    }

    /**
     * A quality constant indicating a location provider may choose to satisfy this request by
     * providing very accurate locations at the expense of potentially increased power usage. Each
     * location provider may interpret this field differently, but as an example, the network
     * provider may choose to return only wifi based locations rather than cell based locations in
     * order to have greater accuracy when this flag is present.
     */
    public static final int QUALITY_HIGH_ACCURACY = LocationRequest.QUALITY_HIGH_ACCURACY;

    /**
     * A quality constant indicating a location provider may choose to satisfy this request by
     * equally balancing power and accuracy constraints. Each location provider may interpret this
     * field differently, but location providers will generally use their default behavior when this
     * flag is present.
     */
    public static final int QUALITY_BALANCED_POWER_ACCURACY =
            LocationRequest.QUALITY_BALANCED_POWER_ACCURACY;

    /**
     * A quality constant indicating a location provider may choose to satisfy this request by
     * providing less accurate locations in order to save power. Each location provider may
     * interpret this field differently, but as an example, the network provider may choose to
     * return cell based locations rather than wifi based locations in order to save power when this
     * flag is present.
     */
    public static final int QUALITY_LOW_POWER = LocationRequest.QUALITY_LOW_POWER;

    private static final long IMPLICIT_MIN_UPDATE_INTERVAL = -1;

    private static Method sCreateFromDeprecatedProviderMethod;
    private static Method sSetQualityMethod;
    private static Method sSetFastestIntervalMethod;
    private static Method sSetNumUpdatesMethod;
    private static Method sSetExpireInMethod;

    @Quality
    final int mQuality;
    final long mIntervalMillis;
    final long mMinUpdateIntervalMillis;
    final long mDurationMillis;
    final int mMaxUpdates;
    final float mMinUpdateDistanceMeters;
    final long mMaxUpdateDelayMillis;

    LocationRequestCompat(
            long intervalMillis,
            @Quality int quality,
            long durationMillis,
            int maxUpdates,
            long minUpdateIntervalMillis,
            float minUpdateDistanceMeters,
            long maxUpdateDelayMillis) {
        mIntervalMillis = intervalMillis;
        mQuality = quality;
        mMinUpdateIntervalMillis = minUpdateIntervalMillis;
        mDurationMillis = durationMillis;
        mMaxUpdates = maxUpdates;
        mMinUpdateDistanceMeters = minUpdateDistanceMeters;
        mMaxUpdateDelayMillis = maxUpdateDelayMillis;
    }

    /**
     * Returns the quality hint for this location request. The quality hint informs the provider how
     * it should attempt to manage any accuracy vs power tradeoffs while attempting to satisfy this
     * location request.
     */
    public @Quality int getQuality() {
        return mQuality;
    }

    /**
     * Returns the desired interval of location updates, or {@link #PASSIVE_INTERVAL} if this is a
     * passive, no power request. A passive request will not actively generate location updates
     * (and thus will not be power blamed for location), but may receive location updates generated
     * as a result of other location requests. A passive request must always have an explicit
     * minimum update interval set.
     *
     * <p>Locations may be available at a faster interval than specified here, see
     * {@link #getMinUpdateIntervalMillis()} for the behavior in that case.
     */
    public @IntRange(from = 0) long getIntervalMillis() {
        return mIntervalMillis;
    }

    /**
     * Returns the minimum update interval. If location updates are available faster than the
     * request interval then locations will only be updated if the minimum update interval has
     * expired since the last location update.
     *
     * <p class=note><strong>Note:</strong> Some allowance for jitter is already built into the
     * minimum update interval, so you need not worry about updates blocked simply because they
     * arrived a fraction of a second earlier than expected.
     *
     * @return the minimum update interval
     */
    public @IntRange(from = 0) long getMinUpdateIntervalMillis() {
        if (mMinUpdateIntervalMillis == IMPLICIT_MIN_UPDATE_INTERVAL) {
            return mIntervalMillis;
        } else {
            return mMinUpdateIntervalMillis;
        }
    }

    /**
     * Returns the duration for which location will be provided before the request is automatically
     * removed. A duration of <code>Long.MAX_VALUE</code> represents an unlimited duration.
     *
     * @return the duration for which location will be provided
     */
    public @IntRange(from = 1) long getDurationMillis() {
        return mDurationMillis;
    }

    /**
     * Returns the maximum number of location updates for this request before the request is
     * automatically removed. A max updates value of <code>Integer.MAX_VALUE</code> represents an
     * unlimited number of updates.
     */
    public @IntRange(from = 1, to = Integer.MAX_VALUE) int getMaxUpdates() {
        return mMaxUpdates;
    }

    /**
     * Returns the minimum distance between location updates. If a potential location update is
     * closer to the last location update than the minimum update distance, then the potential
     * location update will not occur. A value of 0 meters implies that no location update will ever
     * be rejected due to failing this constraint.
     *
     * @return the minimum distance between location updates
     */
    public @FloatRange(from = 0, to = Float.MAX_VALUE) float getMinUpdateDistanceMeters() {
        return mMinUpdateDistanceMeters;
    }

    /**
     * Returns the maximum time any location update may be delayed, and thus grouped with following
     * updates to enable location batching. If the maximum update delay is equal to or greater than
     * twice the interval, then location providers may provide batched results. The maximum batch
     * size is the maximum update delay divided by the interval. Not all devices or location
     * providers support batching, and use of this parameter does not guarantee that the client will
     * see batched results, or that batched results will always be of the maximum size.
     *
     * When available, batching can provide substantial power savings to the device, and clients are
     * encouraged to take advantage where appropriate for the use case.
     *
     * @return the maximum time by which a location update may be delayed
     * @see LocationListenerCompat#onLocationChanged(java.util.List)
     */
    public @IntRange(from = 0) long getMaxUpdateDelayMillis() {
        return mMaxUpdateDelayMillis;
    }

    @RequiresApi(31)
    @NonNull
    LocationRequest toLocationRequest() {
        return new LocationRequest.Builder(mIntervalMillis)
                .setQuality(mQuality)
                .setMinUpdateIntervalMillis(mMinUpdateIntervalMillis)
                .setDurationMillis(mDurationMillis)
                .setMaxUpdates(mMaxUpdates)
                .setMinUpdateDistanceMeters(mMinUpdateDistanceMeters)
                .setMaxUpdateDelayMillis(mMaxUpdateDelayMillis)
                .build();
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @RequiresApi(19)
    @NonNull
    LocationRequest toLocationRequest(@NonNull String provider)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (VERSION.SDK_INT >= 31) {
            return toLocationRequest();
        } else if (VERSION.SDK_INT >= 19) {
            if (sCreateFromDeprecatedProviderMethod == null) {
                sCreateFromDeprecatedProviderMethod = LocationRequest.class.getDeclaredMethod(
                        "createFromDeprecatedProvider", String.class, long.class, float.class,
                        boolean.class);
                sCreateFromDeprecatedProviderMethod.setAccessible(true);
            }

            LocationRequest request =
                    (LocationRequest) sCreateFromDeprecatedProviderMethod.invoke(null, provider,
                            mIntervalMillis,
                            mMinUpdateDistanceMeters, false);
            if (request == null) {
                // should never happen
                throw new InvocationTargetException(new NullPointerException());
            }

            if (sSetQualityMethod == null) {
                sSetQualityMethod = LocationRequest.class.getDeclaredMethod(
                        "setQuality", int.class);
                sSetQualityMethod.setAccessible(true);
            }
            sSetQualityMethod.invoke(request, mQuality);

            if (getMinUpdateIntervalMillis() != mIntervalMillis) {
                if (sSetFastestIntervalMethod == null) {
                    sSetFastestIntervalMethod = LocationRequest.class.getDeclaredMethod(
                            "setFastestInterval", long.class);
                    sSetFastestIntervalMethod.setAccessible(true);
                }

                sSetFastestIntervalMethod.invoke(request, mMinUpdateIntervalMillis);
            }

            if (mMaxUpdates < Integer.MAX_VALUE) {
                if (sSetNumUpdatesMethod == null) {
                    sSetNumUpdatesMethod = LocationRequest.class.getDeclaredMethod(
                            "setNumUpdates", int.class);
                    sSetNumUpdatesMethod.setAccessible(true);
                }

                sSetNumUpdatesMethod.invoke(request, mMaxUpdates);
            }

            if (mDurationMillis < Long.MAX_VALUE) {
                if (sSetExpireInMethod == null) {
                    sSetExpireInMethod = LocationRequest.class.getDeclaredMethod(
                            "setExpireIn", long.class);
                    sSetExpireInMethod.setAccessible(true);
                }

                sSetExpireInMethod.invoke(request, mDurationMillis);
            }
            return request;
        }

        throw new NoClassDefFoundError();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LocationRequestCompat)) {
            return false;
        }

        LocationRequestCompat that = (LocationRequestCompat) o;
        return mQuality == that.mQuality && mIntervalMillis == that.mIntervalMillis
                && mMinUpdateIntervalMillis == that.mMinUpdateIntervalMillis
                && mDurationMillis == that.mDurationMillis && mMaxUpdates == that.mMaxUpdates
                && Float.compare(that.mMinUpdateDistanceMeters, mMinUpdateDistanceMeters) == 0
                && mMaxUpdateDelayMillis == that.mMaxUpdateDelayMillis;
    }

    @Override
    public int hashCode() {
        int result = mQuality;
        result = 31 * result + (int) (mIntervalMillis ^ (mIntervalMillis >>> 32));
        result = 31 * result + (int) (mMinUpdateIntervalMillis ^ (mMinUpdateIntervalMillis >>> 32));
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Request[");
        if (mIntervalMillis != PASSIVE_INTERVAL) {
            s.append("@");
            TimeUtils.formatDuration(mIntervalMillis, s);

            switch (mQuality) {
                case QUALITY_HIGH_ACCURACY:
                    s.append(" HIGH_ACCURACY");
                    break;
                case QUALITY_BALANCED_POWER_ACCURACY:
                    s.append(" BALANCED");
                    break;
                case QUALITY_LOW_POWER:
                    s.append(" LOW_POWER");
                    break;
            }
        } else {
            s.append("PASSIVE");
        }
        if (mDurationMillis != Long.MAX_VALUE) {
            s.append(", duration=");
            TimeUtils.formatDuration(mDurationMillis, s);
        }
        if (mMaxUpdates != Integer.MAX_VALUE) {
            s.append(", maxUpdates=").append(mMaxUpdates);
        }
        if (mMinUpdateIntervalMillis != IMPLICIT_MIN_UPDATE_INTERVAL
                && mMinUpdateIntervalMillis < mIntervalMillis) {
            s.append(", minUpdateInterval=");
            TimeUtils.formatDuration(mMinUpdateIntervalMillis, s);
        }
        if (mMinUpdateDistanceMeters > 0.0) {
            s.append(", minUpdateDistance=").append(mMinUpdateDistanceMeters);
        }
        if (mMaxUpdateDelayMillis / 2 > mIntervalMillis) {
            s.append(", maxUpdateDelay=");
            TimeUtils.formatDuration(mMaxUpdateDelayMillis, s);
        }
        s.append(']');
        return s.toString();
    }

    /**
     * A builder class for {@link LocationRequestCompat}.
     */
    public static final class Builder {

        private long mIntervalMillis;
        private @Quality int mQuality;
        private long mDurationMillis;
        private int mMaxUpdates;
        private long mMinUpdateIntervalMillis;
        private float mMinUpdateDistanceMeters;
        private long mMaxUpdateDelayMillis;

        /**
         * Creates a new Builder with the given interval. See {@link #setIntervalMillis(long)} for
         * more information on the interval. Note that the defaults for various Builder parameters
         * may be different from the defaults for the framework {@link LocationRequest}.
         */
        public Builder(long intervalMillis) {
            // gives us a range check
            setIntervalMillis(intervalMillis);

            mQuality = QUALITY_BALANCED_POWER_ACCURACY;
            mDurationMillis = Long.MAX_VALUE;
            mMaxUpdates = Integer.MAX_VALUE;
            mMinUpdateIntervalMillis = IMPLICIT_MIN_UPDATE_INTERVAL;
            mMinUpdateDistanceMeters = 0;
            mMaxUpdateDelayMillis = 0;
        }

        /**
         * Creates a new Builder with all parameters copied from the given location request.
         */
        public Builder(@NonNull LocationRequestCompat locationRequest) {
            mIntervalMillis = locationRequest.mIntervalMillis;
            mQuality = locationRequest.mQuality;
            mDurationMillis = locationRequest.mDurationMillis;
            mMaxUpdates = locationRequest.mMaxUpdates;
            mMinUpdateIntervalMillis = locationRequest.mMinUpdateIntervalMillis;
            mMinUpdateDistanceMeters = locationRequest.mMinUpdateDistanceMeters;
            mMaxUpdateDelayMillis = locationRequest.mMaxUpdateDelayMillis;
        }

        /**
         * Sets the request interval. The request interval may be set to {@link #PASSIVE_INTERVAL}
         * which indicates this request will not actively generate location updates (and thus will
         * not be power blamed for location), but may receive location updates generated as a result
         * of other location requests. A passive request must always have an explicit minimum
         * update interval set.
         *
         * <p>Locations may be available at a faster interval than specified here, see
         * {@link #setMinUpdateIntervalMillis(long)} for the behavior in that case.
         *
         * <p class="note"><strong>Note:</strong> On platforms below Android 12, using the
         * {@link #PASSIVE_INTERVAL} will not result in a truly passive request, but a request with
         * an extremely long interval. In most cases, this is effectively the same as a passive
         * request, but this may occasionally result in an initial location calculation for which
         * the client will be blamed.
         */
        public @NonNull Builder setIntervalMillis(@IntRange(from = 0) long intervalMillis) {
            mIntervalMillis = Preconditions.checkArgumentInRange(intervalMillis, 0, Long
                            .MAX_VALUE,
                    "intervalMillis");
            return this;
        }

        /**
         * Sets the request quality. The quality is a hint to providers on how they should weigh
         * power vs accuracy tradeoffs. High accuracy locations may cost more power to produce, and
         * lower accuracy locations may cost less power to produce. Defaults to
         * {@link #QUALITY_BALANCED_POWER_ACCURACY}.
         */
        public @NonNull Builder setQuality(@Quality int quality) {
            Preconditions.checkArgument(
                    quality == QUALITY_LOW_POWER || quality == QUALITY_BALANCED_POWER_ACCURACY
                            || quality == QUALITY_HIGH_ACCURACY,
                    "quality must be a defined QUALITY constant, not %d", quality);
            mQuality = quality;
            return this;
        }

        /**
         * Sets the duration this request will continue before being automatically removed. Defaults
         * to <code>Long.MAX_VALUE</code>, which represents an unlimited duration.
         *
         * <p class="note"><strong>Note:</strong> This parameter will be ignored on platforms below
         * Android Kitkat, and the request will not be removed after the duration expires.
         */
        public @NonNull Builder setDurationMillis(@IntRange(from = 1) long durationMillis) {
            mDurationMillis = Preconditions.checkArgumentInRange(durationMillis, 1, Long
                            .MAX_VALUE,
                    "durationMillis");
            return this;
        }

        /**
         * Sets the maximum number of location updates for this request before this request is
         * automatically removed. Defaults to <code>Integer.MAX_VALUE</code>, which represents an
         * unlimited number of updates.
         */
        public @NonNull Builder setMaxUpdates(
                @IntRange(from = 1, to = Integer.MAX_VALUE) int maxUpdates) {
            mMaxUpdates = Preconditions.checkArgumentInRange(maxUpdates, 1, Integer.MAX_VALUE,
                    "maxUpdates");
            return this;
        }

        /**
         * Sets an explicit minimum update interval. If location updates are available faster than
         * the request interval then an update will only occur if the minimum update interval has
         * expired since the last location update. Defaults to no explicit minimum update interval
         * set, which means the minimum update interval is the same as the interval.
         *
         * <p class=note><strong>Note:</strong> Some allowance for jitter is already built into the
         * minimum update interval, so you need not worry about updates blocked simply because they
         * arrived a fraction of a second earlier than expected.
         *
         * <p class="note"><strong>Note:</strong> When {@link #build()} is invoked, the minimum of
         * the interval and the minimum update interval will be used as the minimum update interval
         * of the built request.
         */
        public @NonNull Builder setMinUpdateIntervalMillis(
                @IntRange(from = 0) long minUpdateIntervalMillis) {
            mMinUpdateIntervalMillis = Preconditions.checkArgumentInRange(minUpdateIntervalMillis,
                    0, Long.MAX_VALUE, "minUpdateIntervalMillis");
            return this;
        }

        /**
         * Clears an explicitly set minimum update interval and reverts to an implicit minimum
         * update interval (ie, the minimum update interval is the same value as the interval).
         */
        public @NonNull Builder clearMinUpdateIntervalMillis() {
            mMinUpdateIntervalMillis = IMPLICIT_MIN_UPDATE_INTERVAL;
            return this;
        }

        /**
         * Sets the minimum update distance between location updates. If a potential location
         * update is closer to the last location update than the minimum update distance, then
         * the potential location update will not occur. Defaults to 0, which represents no minimum
         * update distance.
         */
        public @NonNull Builder setMinUpdateDistanceMeters(
                @FloatRange(from = 0, to = Float.MAX_VALUE) float minUpdateDistanceMeters) {
            mMinUpdateDistanceMeters = minUpdateDistanceMeters;
            mMinUpdateDistanceMeters = Preconditions.checkArgumentInRange(minUpdateDistanceMeters,
                    0, Float.MAX_VALUE, "minUpdateDistanceMeters");
            return this;
        }

        /**
         * Sets the maximum time any location update may be delayed, and thus grouped with following
         * updates to enable location batching. If the maximum update delay is equal to or greater
         * than twice the interval, then location providers may provide batched results. Defaults to
         * 0, which represents no batching allowed.
         */
        public @NonNull Builder setMaxUpdateDelayMillis(
                @IntRange(from = 0) long maxUpdateDelayMillis) {
            mMaxUpdateDelayMillis = maxUpdateDelayMillis;
            mMaxUpdateDelayMillis = Preconditions.checkArgumentInRange(maxUpdateDelayMillis, 0,
                    Long.MAX_VALUE, "maxUpdateDelayMillis");
            return this;
        }

        /**
         * Builds a location request from this builder. If an explicit minimum update interval is
         * set, the minimum update interval of the location request will be the minimum of the
         * interval and minimum update interval.
         *
         * <p>If building a passive request then you must have set an explicit minimum update
         * interval.
         */
        public @NonNull LocationRequestCompat build() {
            Preconditions.checkState(mIntervalMillis != PASSIVE_INTERVAL
                            || mMinUpdateIntervalMillis != IMPLICIT_MIN_UPDATE_INTERVAL,
                    "passive location requests must have an explicit minimum update interval");

            return new LocationRequestCompat(
                    mIntervalMillis,
                    mQuality,
                    mDurationMillis,
                    mMaxUpdates,
                    min(mMinUpdateIntervalMillis, mIntervalMillis),
                    mMinUpdateDistanceMeters,
                    mMaxUpdateDelayMillis);
        }
    }
}
