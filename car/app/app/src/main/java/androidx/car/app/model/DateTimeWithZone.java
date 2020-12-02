/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A time with an associated time zone information.
 *
 * <p>In order to avoid time zone databases being out of sync between the app and the host, this
 * model avoids using <a href="https://www.iana.org/time-zones">IANA database</a> time zone IDs and
 * instead relies on the app passing the time zone offset and its abbreviated name. Apps can use
 * their time library of choice to retrieve the time zone information.
 *
 * <p>{@link #create(long, TimeZone)} and {@link #create(ZonedDateTime)} are provided for
 * convenience if using {@code java.util} and {@code java.time} respectively. If using another
 * library such as Joda time, {@link #create(long, int, String)} can be used.
 */
@SuppressWarnings("MissingSummary")
public class DateTimeWithZone {
    /** The maximum allowed offset for a time zone, in seconds. */
    private static final long MAX_ZONE_OFFSET_SECONDS = 18 * HOURS.toSeconds(1);

    @Keep
    private final long mTimeSinceEpochMillis;
    @Keep
    private final int mZoneOffsetSeconds;
    @Nullable
    @Keep
    private final String mZoneShortName;

    /** Returns the number of milliseconds from the epoch of 1970-01-01T00:00:00Z. */
    public long getTimeSinceEpochMillis() {
        return mTimeSinceEpochMillis;
    }

    /** Returns the offset of the time zone from UTC. */
    @SuppressLint("MethodNameUnits")
    public int getZoneOffsetSeconds() {
        return mZoneOffsetSeconds;
    }

    /**
     * Returns the abbreviated name of the time zone, for example "PST" for Pacific Standard
     * Time.
     */
    @Nullable
    public String getZoneShortName() {
        return mZoneShortName;
    }

    @Override
    @NonNull
    @RequiresApi(26)
    // TODO(shiufai): consider removing the @RequiresApi annotation for a toString method.
    @SuppressLint("UnsafeNewApiCall")
    public String toString() {
        return "[local: "
                + LocalDateTime.ofEpochSecond(
                mTimeSinceEpochMillis / 1000,
                /* nanoOfSecond= */ 0,
                ZoneOffset.ofTotalSeconds(mZoneOffsetSeconds))
                + ", zone: "
                + mZoneShortName
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTimeSinceEpochMillis, mZoneOffsetSeconds, mZoneShortName);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DateTimeWithZone)) {
            return false;
        }
        DateTimeWithZone otherDateTime = (DateTimeWithZone) other;

        return mTimeSinceEpochMillis == otherDateTime.mTimeSinceEpochMillis
                && mZoneOffsetSeconds == otherDateTime.mZoneOffsetSeconds
                && Objects.equals(mZoneShortName, otherDateTime.mZoneShortName);
    }

    /**
     * Returns an instance of a {@link DateTimeWithZone}.
     *
     * @param timeSinceEpochMillis The number of milliseconds from the epoch of
     *                             1970-01-01T00:00:00Z.
     * @param zoneOffsetSeconds    The offset of the time zone from UTC at the date specified by
     *                             {@code timeInUtcMillis}. This offset must be in the range
     *                             {@code -18:00} to {@code +18:00}, which corresponds to -64800
     *                             to +64800.
     * @param zoneShortName        The abbreviated name of the time zone, for example, "PST" for
     *                             Pacific Standard Time. This string may be used to display to
     *                             the user along with the date when needed, for example, if this
     *                             time zone is different than the current system time zone.
     * @throws IllegalArgumentException if {@code timeSinceEpochMillis} is a negative value.
     * @throws IllegalArgumentException if {@code zoneOffsetSeconds} is no within the required
     *                                  range.
     * @throws NullPointerException     if {@code zoneShortName} is {@code null}.
     * @throws IllegalArgumentException if {@code zoneShortName} is empty.
     */
    @NonNull
    public static DateTimeWithZone create(
            long timeSinceEpochMillis, int zoneOffsetSeconds, @NonNull String zoneShortName) {
        if (timeSinceEpochMillis < 0) {
            throw new IllegalArgumentException(
                    "Time since epoch must be greater than or equal to zero");
        }
        if (Math.abs(zoneOffsetSeconds) > MAX_ZONE_OFFSET_SECONDS) {
            throw new IllegalArgumentException("Zone offset not in valid range: -18:00 to +18:00");
        }
        if (requireNonNull(zoneShortName).isEmpty()) {
            throw new IllegalArgumentException("The time zone short name can not be null or empty");
        }
        return new DateTimeWithZone(timeSinceEpochMillis, zoneOffsetSeconds, zoneShortName);
    }

    /**
     * Returns an instance of a {@link DateTimeWithZone}.
     *
     * @param timeSinceEpochMillis The number of milliseconds from the epoch of
     *                             1970-01-01T00:00:00Z.
     * @param timeZone             The time zone at the date specified by {@code timeInUtcMillis}.
     *                             The abbreviated
     *                             name of this time zone, formatted using the default locale, may
     *                             be displayed to the user
     *                             when needed, for example, if this time zone is different than
     *                             the current system time zone.
     * @throws IllegalArgumentException if {@code timeSinceEpochMillis} is a negative value.
     * @throws NullPointerException     if {@code timeZone} is {@code null}.
     */
    @NonNull
    public static DateTimeWithZone create(long timeSinceEpochMillis, @NonNull TimeZone timeZone) {
        if (timeSinceEpochMillis < 0) {
            throw new IllegalArgumentException(
                    "timeSinceEpochMillis must be greater than or equal to zero");
        }
        return create(
                timeSinceEpochMillis,
                (int) MILLISECONDS.toSeconds(
                        requireNonNull(timeZone).getOffset(timeSinceEpochMillis)),
                timeZone.getDisplayName(false, TimeZone.SHORT));
    }

    /**
     * Returns an instance of a {@link DateTimeWithZone}.
     *
     * @param zonedDateTime The time with a time zone. The abbreviated name of this time zone,
     *                      formatted using the default locale, may be displayed to the user when
     *                      needed, for example,
     *                      if this time zone is different than the current system time zone.
     * @throws NullPointerException if {@code zonedDateTime} is {@code null}.
     */
    // TODO(shiufai): revisit wrapping this method in a container class (e.g. Api26Impl).
    @SuppressLint("UnsafeNewApiCall")
    @RequiresApi(26)
    @NonNull
    public static DateTimeWithZone create(@NonNull ZonedDateTime zonedDateTime) {
        LocalDateTime localDateTime = requireNonNull(zonedDateTime).toLocalDateTime();
        ZoneId zoneId = zonedDateTime.getZone();
        ZoneOffset zoneOffset = zoneId.getRules().getOffset(localDateTime);
        return create(
                SECONDS.toMillis(localDateTime.toEpochSecond(zoneOffset)),
                zoneOffset.getTotalSeconds(),
                zoneId.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
    }

    private DateTimeWithZone() {
        mTimeSinceEpochMillis = 0;
        mZoneOffsetSeconds = 0;
        mZoneShortName = null;
    }

    private DateTimeWithZone(
            long timeSinceEpochMillis, int zoneOffsetSeconds, @Nullable String timeZoneShortName) {
        this.mTimeSinceEpochMillis = timeSinceEpochMillis;
        this.mZoneOffsetSeconds = zoneOffsetSeconds;
        this.mZoneShortName = timeZoneShortName;
    }
}
