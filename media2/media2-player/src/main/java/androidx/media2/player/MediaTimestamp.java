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

package androidx.media2.player;

import androidx.annotation.NonNull;

/**
 * An immutable object that represents the linear correlation between the media time and the system
 * time. It contains the media clock rate, together with the media timestamp of an anchor frame and
 * the system time when that frame was presented or is committed to be presented.
 *
 * <p>The phrase "present" means that audio/video produced on device is detectable by an external
 * observer off device. The time is based on the implementation's best effort, using whatever
 * knowledge is available to the system, but cannot account for any delay unknown to the
 * implementation. The anchor frame could be any frame, including a just-rendered frame, or even a
 * theoretical or in-between frame, based on the source of the MediaTimestamp. When the anchor frame
 * is a just-rendered one, the media time stands for current position of the playback or recording.
 *
 * @see MediaPlayer#getTimestamp
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
public final class MediaTimestamp {
    /**
     * An unknown media timestamp value
     */
    public static @NonNull final MediaTimestamp TIMESTAMP_UNKNOWN =
            new MediaTimestamp(-1, -1, 0.0f);

    /**
     * Get the media time of the anchor in microseconds.
     */
    public long getAnchorMediaTimeUs() {
        return mMediaTimeUs;
    }

    /**
     * Get the {@link java.lang.System#nanoTime system time} corresponding to the media time
     * in nanoseconds.
     */
    public long getAnchorSystemNanoTime() {
        return mNanoTime;
    }

    /**
     * Get the rate of the media clock in relation to the system time.
     * <p>
     * It is 1.0 if media clock advances in sync with the system clock;
     * greater than 1.0 if media clock is faster than the system clock;
     * less than 1.0 if media clock is slower than the system clock.
     */
    public float getMediaClockRate() {
        return mClockRate;
    }

    private final long mMediaTimeUs;
    private final long mNanoTime;
    private final float mClockRate;

    /* package */ MediaTimestamp(long mediaUs, long systemNs, float rate) {
        mMediaTimeUs = mediaUs;
        mNanoTime = systemNs;
        mClockRate = rate;
    }

    /* package */ MediaTimestamp() {
        mMediaTimeUs = 0;
        mNanoTime = 0;
        mClockRate = 1.0f;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final MediaTimestamp that = (MediaTimestamp) obj;
        return (this.mMediaTimeUs == that.mMediaTimeUs)
                && (this.mNanoTime == that.mNanoTime)
                && (this.mClockRate == that.mClockRate);
    }

    @Override
    public int hashCode() {
        int result = Long.valueOf(mMediaTimeUs).hashCode();
        result = (int) (31 * result + mNanoTime);
        result = (int) (31 * result + mClockRate);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getName()
                + "{AnchorMediaTimeUs=" + mMediaTimeUs
                + " AnchorSystemNanoTime=" + mNanoTime
                + " ClockRate=" + mClockRate
                + "}";
    }
}
