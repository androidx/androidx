/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.mediarouting.data;

import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * MediaItem helps keep track of the media items before adding to playlist.
 */
public final class MediaItem {
    public final String mName;
    public final Uri mUri;
    public final String mMime;

    public MediaItem(@NonNull String name, @NonNull Uri uri, @NonNull String mime) {
        mName = name;
        mUri = uri;
        mMime = mime;
    }
}
