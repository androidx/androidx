/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.media;

import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.Rating;
import android.os.Parcel;

import androidx.annotation.RequiresApi;

import java.util.Set;

@RequiresApi(21)
class MediaMetadataCompatApi21 {
    public static Set<String> keySet(Object metadataObj) {
        return ((MediaMetadata)metadataObj).keySet();
    }

    public static Bitmap getBitmap(Object metadataObj, String key) {
        return ((MediaMetadata)metadataObj).getBitmap(key);
    }

    public static long getLong(Object metadataObj, String key) {
        return ((MediaMetadata)metadataObj).getLong(key);
    }

    public static Object getRating(Object metadataObj, String key) {
        return ((MediaMetadata)metadataObj).getRating(key);
    }

    public static CharSequence getText(Object metadataObj, String key) {
        return ((MediaMetadata) metadataObj).getText(key);
    }

    public static void writeToParcel(Object metadataObj, Parcel dest, int flags) {
        ((MediaMetadata) metadataObj).writeToParcel(dest, flags);
    }

    public static Object createFromParcel(Parcel in) {
        return MediaMetadata.CREATOR.createFromParcel(in);
    }

    public static class Builder {
        public static Object newInstance() {
            return new MediaMetadata.Builder();
        }

        public static void putBitmap(Object builderObj, String key, Bitmap value) {
            ((MediaMetadata.Builder)builderObj).putBitmap(key, value);
        }

        public static void putLong(Object builderObj, String key, long value) {
            ((MediaMetadata.Builder)builderObj).putLong(key, value);
        }

        public static void putRating(Object builderObj, String key, Object ratingObj) {
            ((MediaMetadata.Builder)builderObj).putRating(key, (Rating)ratingObj);
        }

        public static void putText(Object builderObj, String key, CharSequence value) {
            ((MediaMetadata.Builder) builderObj).putText(key, value);
        }

        public static void putString(Object builderObj, String key, String value) {
            ((MediaMetadata.Builder) builderObj).putString(key, value);
        }

        public static Object build(Object builderObj) {
            return ((MediaMetadata.Builder)builderObj).build();
        }

        private Builder() {
        }
    }

    private MediaMetadataCompatApi21() {
    }
}
