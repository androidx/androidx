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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.StorageInfo;
import androidx.core.util.Preconditions;

/**
 * Translates between Platform and Jetpack versions of responses.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class ResponseToPlatformConverter {
    private ResponseToPlatformConverter() {}

    /**
     * Translates a platform {@link android.app.appsearch.StorageInfo} into a jetpack
     * {@link StorageInfo}.
     */
    @NonNull
    public static StorageInfo toJetpackStorageInfo(
            @NonNull android.app.appsearch.StorageInfo platformStorageInfo) {
        Preconditions.checkNotNull(platformStorageInfo);
        return new StorageInfo.Builder()
                .setAliveNamespacesCount(platformStorageInfo.getAliveNamespacesCount())
                .setAliveDocumentsCount(platformStorageInfo.getAliveDocumentsCount())
                .setSizeBytes(platformStorageInfo.getSizeBytes())
                .build();

    }
}
