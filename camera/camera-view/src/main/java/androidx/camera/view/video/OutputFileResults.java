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

package androidx.camera.view.video;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

/**
 * Info about the saved video file.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@ExperimentalVideo
@AutoValue
public abstract class OutputFileResults {

    // Restrict constructor to package
    OutputFileResults() {
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public static OutputFileResults create(@Nullable Uri savedUri) {
        return new AutoValue_OutputFileResults(savedUri);
    }

    /**
     * Returns the {@link Uri} of the saved video file.
     *
     * @return URI of saved video file if the {@link OutputFileOptions} is backed by
     * {@link MediaStore} using
     * {@link OutputFileOptions#builder(ContentResolver, Uri, ContentValues)}, {@code null}
     * otherwise.
     */
    @Nullable
    public abstract Uri getSavedUri();
}
