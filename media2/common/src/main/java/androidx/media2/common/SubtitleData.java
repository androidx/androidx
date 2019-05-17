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

package androidx.media2.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Class encapsulating subtitle data, as received through the
 * {@link SessionPlayer.PlayerCallback#onSubtitleData} interface.
 * The subtitle data includes:
 * <ul>
 * <li> the start time (in microseconds) of the data</li>
 * <li> the duration (in microseconds) of the data</li>
 * <li> the actual data.</li>
 * </ul>
 * The data is stored in a byte-array, and is encoded in one of the supported in-band
 * subtitle formats. The subtitle encoding is determined by the MIME type of the
 * {@link SessionPlayer.TrackInfo} of the subtitle track, one of
 * {@link #MIMETYPE_TEXT_CEA_608}, {@link #MIMETYPE_TEXT_CEA_708},
 * {@link #MIMETYPE_TEXT_VTT}.
 *
 * @see SessionPlayer#registerPlayerCallback(Executor, SessionPlayer.PlayerCallback)
 * @see SessionPlayer.PlayerCallback#onSubtitleData(SessionPlayer, MediaItem,
 *      SessionPlayer.TrackInfo, SubtitleData)
 *
 * @hide
 */
// TODO: replace this byte oriented data with structured data (b/130312596)
@RestrictTo(LIBRARY_GROUP)
@VersionedParcelize
public final class SubtitleData implements VersionedParcelable {
    private static final String TAG = "SubtitleData";

    /**
     * MIME type for CEA-608 closed caption data.
     */
    public static final String MIMETYPE_TEXT_CEA_608 = "text/cea-608";

    /**
     * MIME type for CEA-708 closed caption data.
     */
    public static final String MIMETYPE_TEXT_CEA_708 = "text/cea-708";

    /**
     * MIME type for WebVTT subtitle data.
     */
    public static final String MIMETYPE_TEXT_VTT = "text/vtt";

    @ParcelField(1)
    long mStartTimeUs;
    @ParcelField(2)
    long mDurationUs;
    @ParcelField(3)
    byte[] mData;

    /**
     * Used for VersionedParcelable
     */
    SubtitleData() {
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public SubtitleData(long startTimeUs, long durationUs, @NonNull byte[] data) {
        mStartTimeUs = startTimeUs;
        mDurationUs = durationUs;
        mData = data;
    }

    /**
     * Returns the media time at which the subtitle should be displayed, expressed in microseconds.
     * @return the display start time for the subtitle
     */
    public long getStartTimeUs() {
        return mStartTimeUs;
    }

    /**
     * Returns the duration in microsecond during which the subtitle should be displayed.
     * @return the display duration for the subtitle
     */
    public long getDurationUs() {
        return mDurationUs;
    }

    /**
     * Returns the encoded data for the subtitle content.
     * Encoding format depends on the subtitle type, refer to
     * <a href="https://en.wikipedia.org/wiki/CEA-708">CEA 708</a>,
     * <a href="https://en.wikipedia.org/wiki/EIA-608">CEA/EIA 608</a> and
     * <a href="https://www.w3.org/TR/webvtt1/">WebVTT</a>, defined by the MIME type
     * of the subtitle track.
     * @return the encoded subtitle data
     */
    @NonNull
    public byte[] getData() {
        return mData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubtitleData that = (SubtitleData) o;
        return mStartTimeUs == that.mStartTimeUs
                && mDurationUs == that.mDurationUs
                && Arrays.equals(mData, that.mData);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mStartTimeUs, mDurationUs, Arrays.hashCode(mData));
    }
}
