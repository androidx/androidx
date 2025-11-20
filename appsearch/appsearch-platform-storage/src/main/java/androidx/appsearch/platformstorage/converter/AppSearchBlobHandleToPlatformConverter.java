/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * Translates a jetpack {@link AppSearchBlobHandle} into a platform
 * {@link android.app.appsearch.AppSearchBlobHandle}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
public class AppSearchBlobHandleToPlatformConverter {

    private AppSearchBlobHandleToPlatformConverter() {}

    /**
     * Translates a jetpack {@link AppSearchBlobHandle} into a platform
     * {@link android.app.appsearch.AppSearchBlobHandle}.
     */
    public static android.app.appsearch.@NonNull AppSearchBlobHandle toPlatformBlobHandle(
            @NonNull AppSearchBlobHandle jetpackBlobHandle) {
        Preconditions.checkNotNull(jetpackBlobHandle);
        return android.app.appsearch.AppSearchBlobHandle.createWithSha256(
                jetpackBlobHandle.getSha256Digest(),
                jetpackBlobHandle.getPackageName(),
                jetpackBlobHandle.getDatabaseName(),
                jetpackBlobHandle.getNamespace());
    }

    /**
     * Translates a platform {@link android.app.appsearch.AppSearchBlobHandle} into a jetpack
     * {@link AppSearchBlobHandle}.
     */
    @NonNull
    public static AppSearchBlobHandle toJetpackBlobHandle(
            android.app.appsearch.@NonNull AppSearchBlobHandle platformBlobHandle) {
        Preconditions.checkNotNull(platformBlobHandle);
        return AppSearchBlobHandle.createWithSha256(
                platformBlobHandle.getSha256Digest(),
                platformBlobHandle.getPackageName(),
                platformBlobHandle.getDatabaseName(),
                platformBlobHandle.getNamespace());
    }
}
