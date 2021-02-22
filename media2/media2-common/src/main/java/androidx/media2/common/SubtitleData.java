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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 * {@link android.media.MediaFormat#MIMETYPE_TEXT_CEA_608} or
 * {@link android.media.MediaFormat#MIMETYPE_TEXT_CEA_708}.
 *
 * <p>
 * Here is an example of iterating over the tracks of a {@link SessionPlayer}, and checking which
 * encoding is used for the subtitle tracks:
 * <p>
 * <pre class="prettyprint">
 * // Initialize instance of player that extends SessionPlayer
 * SessionPlayerExtension player = new SessionPlayerExtension();
 *
 * final TrackInfo[] trackInfos = player.getTrackInfo();
 * for (TrackInfo info : trackInfo) {
 *     if (info.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
 *         final String mime = info.getFormat().getString(MediaFormat.KEY_MIME);
 *         if ("text/cea-608".equals(mime) {
 *             // subtitle encoding is CEA 608
 *         } else if ("text/cea-708".equals(mime) {
 *             // subtitle encoding is CEA 708
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * @see SessionPlayer#registerPlayerCallback(Executor, SessionPlayer.PlayerCallback)
 * @see SessionPlayer.PlayerCallback#onSubtitleData(SessionPlayer, MediaItem,
 *      SessionPlayer.TrackInfo, SubtitleData)
 */
@VersionedParcelize
public final class SubtitleData implements VersionedParcelable {
    private static final String TAG = "SubtitleData";

    @ParcelField(1)
    long mStartTimeUs;
    @ParcelField(2)
    long mDurationUs;
    @ParcelField(3)
    byte[] mData;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    /**
     * Used for VersionedParcelable
     */
    SubtitleData() {
    }

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
     * <a href="https://en.wikipedia.org/wiki/CEA-708">CEA 708</a>, and
     * <a href="https://en.wikipedia.org/wiki/EIA-608">CEA/EIA 608</a> defined by the MIME type
     * of the subtitle track.
     * @return the encoded subtitle data
     */
    @NonNull
    public byte[] getData() {
        return mData;
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
