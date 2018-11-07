/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.media.SubtitleData;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;

/**
 * Class encapsulating subtitle data, as received through the
 * {@link MediaPlayer.PlayerCallback#onSubtitleData} interface.
 * The subtitle data includes:
 * <ul>
 * <li> the track index</li>
 * <li> the start time (in microseconds) of the data</li>
 * <li> the duration (in microseconds) of the data</li>
 * <li> the actual data.</li>
 * </ul>
 * The data is stored in a byte-array, and is encoded in one of the supported in-band
 * subtitle formats. The subtitle encoding is determined by the MIME type of the
 * {@link MediaPlayer.TrackInfo} of the subtitle track, one of
 * {@link #MIMETYPE_TEXT_CEA_608}, {@link #MIMETYPE_TEXT_CEA_708},
 * {@link #MIMETYPE_TEXT_VTT}.
 * <p>
 * Here is an example of iterating over the tracks of a {@link MediaPlayer}, and checking which
 * encoding is used for the subtitle tracks:
 * <p>
 * <pre class="prettyprint">
 * MediaPlayer mp = new MediaPlayer(context);
 * // prepare the player with a valid media item.
 * &hellip;
 *
 * final TrackInfo[] trackInfos = mp.getTrackInfo();
 * for (TrackInfo info : trackInfo) {
 *     if (info.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
 *         final String mime = info.getFormat().getString(MediaFormat.KEY_MIME);
 *         if (SubtitleData2.MIMETYPE_TEXT_CEA_608.equals(mime) {
 *             // subtitle encoding is CEA 608
 *         } else if (SubtitleData2.MIMETYPE_TEXT_CEA_708.equals(mime) {
 *             // subtitle encoding is CEA 708
 *         } else if (SubtitleData2.MIMETYPE_TEXT_VTT.equals(mime) {
 *             // subtitle encoding is WebVTT
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * @see MediaPlayer#registerPlayerCallback(Executor, SessionPlayer2.PlayerCallback)
 * @see MediaPlayer.PlayerCallback#onSubtitleData(MediaPlayer, MediaItem2, SubtitleData2)
 */
public final class SubtitleData2 {
    private static final String TAG = "SubtitleData2";

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

    private int mTrackIndex;
    private long mStartTimeUs;
    private long mDurationUs;
    private byte[] mData;

    /** @hide */
    @TargetApi(28)
    @RestrictTo(LIBRARY_GROUP)
    public SubtitleData2(SubtitleData subtitleData) {
        mTrackIndex = subtitleData.getTrackIndex();
        mStartTimeUs = subtitleData.getStartTimeUs();
        mDurationUs = subtitleData.getDurationUs();
        mData = subtitleData.getData();
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public SubtitleData2(int trackIndex, long startTimeUs, long durationUs, byte[] data) {
        mTrackIndex = trackIndex;
        mStartTimeUs = startTimeUs;
        mDurationUs = durationUs;
        mData = data;
    }

    /**
     * Returns the index of the MediaPlayer track which contains this subtitle data.
     * @return an index in the array returned by {@link MediaPlayer#getTrackInfo()}.
     */
    public int getTrackIndex() {
        return mTrackIndex;
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
    public @NonNull byte[] getData() {
        return mData;
    }
}
