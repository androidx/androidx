/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.compat;

import android.media.MediaMuxer;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Helper class to avoid verification errors for methods introduced in Android 8.0 (API 26).
 */
@RequiresApi(26)
public final class Api26Impl {

    private Api26Impl() {
    }

    /**
     * Uses a {@link FileDescriptor} as output destination to create a {@link MediaMuxer}.
     */
    @DoNotInline
    @NonNull
    public static MediaMuxer createMediaMuxer(@NonNull FileDescriptor fileDescriptor, int format)
            throws IOException {
        return new MediaMuxer(fileDescriptor, format);
    }
}
