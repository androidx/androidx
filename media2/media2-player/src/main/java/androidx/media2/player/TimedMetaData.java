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

package androidx.media2.player;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;

/**
 * Class that embodies one timed metadata access unit, including
 *
 * <ul>
 *   <li>a time stamp, and
 *   <li>raw uninterpreted byte-array extracted directly from the container.
 * </ul>
 *
 * @see MediaPlayer.PlayerCallback#onTimedMetaDataAvailable
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
public class TimedMetaData {
    private static final String TAG = "TimedMetaData";

    private long mTimestampUs;
    private byte[] mMetaData;

    /**
     */
    @RestrictTo(LIBRARY)
    public TimedMetaData(long timestampUs, byte[] metaData) {
        mTimestampUs = timestampUs;
        mMetaData = metaData;
    }

    /**
     * @return the timestamp associated with this metadata access unit in microseconds;
     * 0 denotes playback start.
     */
    public long getTimestamp() {
        return mTimestampUs;
    }

    /**
     * @return raw, uninterpreted content of this metadata access unit; for ID3 tags this includes
     * everything starting from the 3 byte signature "ID3".
     */
    public byte[] getMetaData() {
        return mMetaData;
    }
}
