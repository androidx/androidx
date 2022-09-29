/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.androidx.widget.selection.fancy;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

final class Uris {

    private Uris() {}

    static final String SCHEME = "content";
    static final String AUTHORITY = "CheeseWorld";
    static final String PARAM_GROUP = "g";
    static final String PARAM_CHEESE = "c";

    static @NonNull Uri forGroup(@NonNull String group) {
        return new Uri.Builder()
                .scheme(SCHEME)
                .encodedAuthority(AUTHORITY)
                .appendQueryParameter(PARAM_GROUP, group)
                .build();
    }

    static @NonNull Uri forCheese(@NonNull String group, @NonNull String cheese) {
        return new Uri.Builder()
                .scheme(SCHEME)
                .encodedAuthority(AUTHORITY)
                .appendQueryParameter(PARAM_GROUP, group)
                .appendQueryParameter(PARAM_CHEESE, cheese)
                .build();
    }

    static boolean isGroup(@NonNull Uri uri) {
        return !isCheese(uri);
    }

    static boolean isCheese(@NonNull Uri uri) {
        return uri.getQueryParameter(PARAM_GROUP) != null
                && uri.getQueryParameter(PARAM_CHEESE) != null;
    }

    static @NonNull String getGroup(@NonNull Uri uri) {
        String group = uri.getQueryParameter(PARAM_GROUP);
        Preconditions.checkArgument(group != null);
        return group;
    }

    static @NonNull String getCheese(@NonNull Uri uri) {
        Preconditions.checkArgument(isCheese(uri));
        return uri.getQueryParameter(PARAM_CHEESE);
    }
}
