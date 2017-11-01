/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.leanback;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.google.gson.annotations.SerializedName;

/**
 * Abstract data type to represent music item.
 */
public class MusicItem {
    // Duration information of current.
    @SerializedName("duration")
    private String mDuration;

    // File name of this media item.
    @SerializedName("file")
    private String mFile;

    // The title of this media item.
    @SerializedName("title")
    private String mMediaTitle;

    // The description of media item.
    @SerializedName("description")
    private String mDescription;

    // Art information (i.e. cover image) of this media item.
    @SerializedName("art")
    private String mArt;

    /**
     * The conversion function which can return media item's uri through the file name.
     *
     * @param context The context used to get resources of this app.
     * @return The Uri of the selected media item.
     */
    public Uri getMediaSourceUri(Context context) {
        return getResourceUri(context, context.getResources()
                .getIdentifier(mFile, "raw", context.getPackageName()));
    }

    /**
     * Return the title of current media item.
     *
     * @return The title of current media item.
     */
    public String getMediaTitle() {
        return mMediaTitle;
    }

    /**
     * Return the description of current media item.
     *
     * @return The description of current media item.
     */
    public String getMediaDescription() {
        return mDescription;
    }

    /**
     * Return the resource id through art file's name.
     *
     * @param context The context used to get resources of this app.
     * @return The resource Id of the selected media item.
     */
    public int getMediaAlbumArtResId(Context context) {
        return context.getResources()
                .getIdentifier(mArt, "drawable", context.getPackageName());
    }

    /**
     * Helper function to get resource uri based on android resource scheme
     *
     * @param context Context to get resources.
     * @param resID   Resource ID.
     * @return The Uri of current resource.
     */
    public static Uri getResourceUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://"
                + context.getResources().getResourcePackageName(resID)
                + '/'
                + context.getResources().getResourceTypeName(resID)
                + '/'
                + context.getResources().getResourceEntryName(resID));
    }
}
