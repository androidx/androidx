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

package androidx.media2.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.media.MediaFormat;

import androidx.annotation.RestrictTo;
import androidx.media2.MediaPlayer2;

/**
 * Class for MediaPlayer2 implementations to return each audio/video/subtitle track's metadata.
 *
 * @see MediaPlayer2#getTrackInfo
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public final class TrackInfoImpl extends MediaPlayer2.TrackInfo {
    private final int mTrackType;
    private final MediaFormat mFormat;

    /**
     * Gets the track type.
     * @return TrackType which indicates if the track is video, audio, timed text.
     */
    @Override
    public int getTrackType() {
        return mTrackType;
    }

    /**
     * Gets the language code of the track.
     * @return a language code in either way of ISO-639-1 or ISO-639-2.
     * When the language is unknown or could not be determined,
     * ISO-639-2 language code, "und", is returned.
     */
    @Override
    public String getLanguage() {
        String language = mFormat.getString(MediaFormat.KEY_LANGUAGE);
        return language == null ? "und" : language;
    }

    /**
     * Gets the {@link MediaFormat} of the track.  If the format is
     * unknown or could not be determined, null is returned.
     */
    @Override
    public MediaFormat getFormat() {
        if (mTrackType == MEDIA_TRACK_TYPE_TIMEDTEXT
                || mTrackType == MEDIA_TRACK_TYPE_SUBTITLE) {
            return mFormat;
        }
        return null;
    }

    public TrackInfoImpl(int type, MediaFormat format) {
        mTrackType = type;
        mFormat = format;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(128);
        out.append(getClass().getName());
        out.append('{');
        switch (mTrackType) {
            case MEDIA_TRACK_TYPE_VIDEO:
                out.append("VIDEO");
                break;
            case MEDIA_TRACK_TYPE_AUDIO:
                out.append("AUDIO");
                break;
            case MEDIA_TRACK_TYPE_TIMEDTEXT:
                out.append("TIMEDTEXT");
                break;
            case MEDIA_TRACK_TYPE_SUBTITLE:
                out.append("SUBTITLE");
                break;
            case MEDIA_TRACK_TYPE_METADATA:
                out.append("METADATA");
                break;
            default:
                out.append("UNKNOWN");
                break;
        }
        out.append(", ");
        out.append(mFormat);
        out.append("}");
        return out.toString();
    }
}
