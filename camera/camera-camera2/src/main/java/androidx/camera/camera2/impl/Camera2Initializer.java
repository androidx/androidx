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

package androidx.camera.camera2.impl;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.core.CameraX;

/**
 * A {@link ContentProvider} used to initialize {@link CameraX} from a {@link Context}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Camera2Initializer extends ContentProvider {
    private static final String TAG = "Camera2Initializer";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "CameraX initializing with Camera2 ...");

        CameraX.init(getContext(), Camera2AppConfig.create(getContext()));
        return false;
    }

    @Nullable
    @Override
    public Cursor query(
            Uri uri,
            @Nullable String[] strings,
            @Nullable String s,
            @Nullable String[] strings1,
            @Nullable String s1) {
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(
            Uri uri,
            @Nullable ContentValues contentValues,
            @Nullable String s,
            @Nullable String[] strings) {
        return 0;
    }
}
