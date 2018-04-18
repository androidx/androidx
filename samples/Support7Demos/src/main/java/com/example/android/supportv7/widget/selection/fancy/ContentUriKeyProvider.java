/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.android.supportv7.widget.selection.fancy;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;

import java.util.HashMap;
import java.util.Map;

class ContentUriKeyProvider extends ItemKeyProvider<Uri> {

    private final Uri[] mUris;
    private final Map<Uri, Integer> mPositions;

    ContentUriKeyProvider(String authority, String[] values) {
        // Advise the world we can supply ids/position for entire copus
        // at any time.
        super(SCOPE_MAPPED);

        mUris = new Uri[values.length];
        mPositions = new HashMap<>();

        for (int i = 0; i < values.length; i++) {
            mUris[i] = new Uri.Builder()
                    .scheme("content")
                    .encodedAuthority(authority)
                    .appendPath(values[i])
                    .build();
            mPositions.put(mUris[i], i);
        }
    }

    @Override
    public @Nullable Uri getKey(int position) {
        return mUris[position];
    }

    @Override
    public int getPosition(Uri key) {
        return mPositions.get(key);
    }
}
