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
import android.media.MediaDescription;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.RequiresApi;

@RequiresApi(21)
class MediaDescriptionCompatApi21 {

    public static String getMediaId(Object descriptionObj) {
        return ((MediaDescription) descriptionObj).getMediaId();
    }

    public static CharSequence getTitle(Object descriptionObj) {
        return ((MediaDescription) descriptionObj).getTitle();
    }

    public static CharSequence getSubtitle(Object descriptionObj) {
        return ((MediaDescription) descriptionObj).getSubtitle();
    }

    public static CharSequence getDescription(Object descriptionObj) {
        return ((MediaDescription) descriptionObj).getDescription();
    }

    public static Bitmap getIconBitmap(Object descriptionObj) {
        return ((MediaDescription) descriptionObj).getIconBitmap();
    }

    public static Uri getIconUri(Object descriptionObj) {
        return ((MediaDescription) descriptionObj).getIconUri();
    }

    public static Bundle getExtras(Object descriptionObj) {
        return ((MediaDescription) descriptionObj).getExtras();
    }

    public static void writeToParcel(Object descriptionObj, Parcel dest, int flags) {
        ((MediaDescription) descriptionObj).writeToParcel(dest, flags);
    }

    public static Object fromParcel(Parcel in) {
        return MediaDescription.CREATOR.createFromParcel(in);
    }

    static class Builder {
        public static Object newInstance() {
            return new MediaDescription.Builder();
        }


        public static void setMediaId(Object builderObj, String mediaId) {
            ((MediaDescription.Builder)builderObj).setMediaId(mediaId);
        }

        public static void setTitle(Object builderObj, CharSequence title) {
            ((MediaDescription.Builder)builderObj).setTitle(title);
        }

        public static void setSubtitle(Object builderObj, CharSequence subtitle) {
            ((MediaDescription.Builder)builderObj).setSubtitle(subtitle);
        }

        public static void setDescription(Object builderObj, CharSequence description) {
            ((MediaDescription.Builder)builderObj).setDescription(description);
        }

        public static void setIconBitmap(Object builderObj, Bitmap iconBitmap) {
            ((MediaDescription.Builder)builderObj).setIconBitmap(iconBitmap);
        }

        public static void setIconUri(Object builderObj, Uri iconUri) {
            ((MediaDescription.Builder)builderObj).setIconUri(iconUri);
        }

        public static void setExtras(Object builderObj, Bundle extras) {
            ((MediaDescription.Builder)builderObj).setExtras(extras);
        }

        public static Object build(Object builderObj) {
            return ((MediaDescription.Builder) builderObj).build();
        }
    }
}
