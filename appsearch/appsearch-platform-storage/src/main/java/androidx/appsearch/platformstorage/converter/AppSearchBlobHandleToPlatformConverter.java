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

import android.app.appsearch.AppSearchResult;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Translates a jetpack {@link AppSearchBlobHandle} into a platform
 * {@link android.app.appsearch.AppSearchBlobHandle}.
 */
@HideInPlatform
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

    /**
     * Converts Jetpack {@link AppSearchBlobHandle}s to platform handles and partitions them into
     * smaller batches to satisfy IPC limits based on the device build version.
     *
     * <p>On Android Baklava (API 36), there is a known issue (b/444290368) where large response
     * objects (> 16KiB) containing maps of {@link AppSearchBlobHandle} and {@link AppSearchResult}
     * cause marshalling crashes.
     *
     * <p>To mitigate this, this method limits the batch size to 10 for Baklava devices. For other
     * versions, the batch size defaults to the total size of the input (no partitioning).
     *
     * @param handles The set of Jetpack {@link AppSearchBlobHandle}s to convert and batch.
     * @return A list of sets, where each set contains a batch of platform handles ready for
     * IPC calls.
     */
    @NonNull
    public static List<Set<android.app.appsearch.AppSearchBlobHandle>>
                convertBlobHandleToSmallBatch(@NonNull Set<AppSearchBlobHandle> handles) {
        int batchSize = handles.size();
        //TODO(b/444290368) add SdkExtensions check for BAKLAVA when 26M02 version number is ready
        // in SdkExtensions
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.BAKLAVA && handles.size() > 10) {
            // b/444290368, if the size of response object > 16KiB, it will be marshalled and crash.
            // The response object contains a map of <AppSearchBlobHandle, AppSearchResult>, the
            // normal size of BlobHandle is 200 ~ 500 Byte and an AppSearchResult is ~ 200 Byte. An
            // entry will be < 800 Byte, it could allow most of 20 entries in the response object.
            // Set it to be 10 for safety.
            // It is fixed in C and above.
            batchSize = 10;
        }
        int totalBatches = handles.size() / batchSize + (handles.size() % batchSize == 0 ? 0 : 1);
        List<Set<android.app.appsearch.AppSearchBlobHandle>> platformBlobHandlesBuckets =
                new ArrayList<>(totalBatches);
        Set<android.app.appsearch.AppSearchBlobHandle> platformBlobHandlesBucket =
                new ArraySet<>(batchSize);
        for (AppSearchBlobHandle jetpackHandle : handles) {
            platformBlobHandlesBucket.add(AppSearchBlobHandleToPlatformConverter
                    .toPlatformBlobHandle(jetpackHandle));
            if (platformBlobHandlesBucket.size() == batchSize) {
                platformBlobHandlesBuckets.add(platformBlobHandlesBucket);
                platformBlobHandlesBucket = new ArraySet<>(batchSize);
            }
        }
        // Add the last bucket if there is any.
        if (!platformBlobHandlesBucket.isEmpty()) {
            platformBlobHandlesBuckets.add(platformBlobHandlesBucket);
        }
        return  platformBlobHandlesBuckets;
    }
}
