/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.playservicesstorage.converter;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.StorageInfo;
import androidx.core.util.Preconditions;

/**
 * Translates between Gms and Jetpack versions of responses.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ResponseToGmsConverter {
    private ResponseToGmsConverter() {
    }

    /**
     * Translates a Gms {@link com.google.android.gms.appsearch.StorageInfo} into
     * a jetpack {@link StorageInfo}.
     */
    @NonNull
    public static StorageInfo toJetpackStorageInfo(
            @NonNull com.google.android.gms.appsearch.StorageInfo gmsStorageInfo) {
        Preconditions.checkNotNull(gmsStorageInfo);
        return new StorageInfo.Builder()
                .setAliveNamespacesCount(gmsStorageInfo.getAliveNamespacesCount())
                .setAliveDocumentsCount(gmsStorageInfo.getAliveDocumentsCount())
                .setSizeBytes(gmsStorageInfo.getSizeBytes())
                .build();

    }
}
