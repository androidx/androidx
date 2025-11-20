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

import android.app.appsearch.AppSearchBatchResult;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.CommitBlobResponse;
import androidx.appsearch.app.OpenBlobForReadResponse;
import androidx.appsearch.app.OpenBlobForWriteResponse;
import androidx.appsearch.app.RemoveBlobResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.function.Function;

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
    public static @NonNull StorageInfo toJetpackStorageInfo(
            android.app.appsearch.@NonNull StorageInfo platformStorageInfo) {
        Preconditions.checkNotNull(platformStorageInfo);
        StorageInfo.Builder jetpackBuilder = new StorageInfo.Builder();
        jetpackBuilder
                .setAliveNamespacesCount(platformStorageInfo.getAliveNamespacesCount())
                .setAliveDocumentsCount(platformStorageInfo.getAliveDocumentsCount())
                .setSizeBytes(platformStorageInfo.getSizeBytes());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            jetpackBuilder
                    .setBlobsCount(platformStorageInfo.getBlobsCount())
                    .setBlobsSizeBytes(platformStorageInfo.getBlobsSizeBytes());
        }
        return jetpackBuilder.build();
    }

    /**
     * Translates a platform {@link android.app.appsearch.RemoveBlobResponse} into a jetpack
     * {@link RemoveBlobResponse}.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public static @NonNull RemoveBlobResponse toJetpackRemoveBlobResponse(
            android.app.appsearch.@NonNull RemoveBlobResponse platformRemoveBlobResponse) {
        Preconditions.checkNotNull(platformRemoveBlobResponse);
        AppSearchBatchResult<android.app.appsearch.AppSearchBlobHandle, Void> platformBatchResult =
                platformRemoveBlobResponse.getResult();

        return new RemoveBlobResponse(
                AppSearchResultToPlatformConverter.platformAppSearchBatchResultToJetpack(
                        platformBatchResult,
                        AppSearchBlobHandleToPlatformConverter::toJetpackBlobHandle,
                        Function.identity()));
    }

    /**
     * Translates a platform {@link android.app.appsearch.CommitBlobResponse} into a jetpack
     * {@link CommitBlobResponse}.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public static @NonNull CommitBlobResponse toJetpackCommitBlobResponse(
            android.app.appsearch.@NonNull CommitBlobResponse platformCommitBlobResponse) {
        Preconditions.checkNotNull(platformCommitBlobResponse);
        AppSearchBatchResult<android.app.appsearch.AppSearchBlobHandle, Void> platformBatchResult =
                platformCommitBlobResponse.getResult();

        return new CommitBlobResponse(
                AppSearchResultToPlatformConverter.platformAppSearchBatchResultToJetpack(
                        platformBatchResult,
                        AppSearchBlobHandleToPlatformConverter::toJetpackBlobHandle,
                        Function.identity()));
    }

    /**
     * Translates a platform {@link android.app.appsearch.OpenBlobForWriteResponse} into a jetpack
     * {@link OpenBlobForWriteResponse}.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public static @NonNull OpenBlobForWriteResponse toJetpackOpenBlobForWriteResponse(
            android.app.appsearch.@NonNull OpenBlobForWriteResponse
                    platformOpenBlobForWriteResponse) {
        Preconditions.checkNotNull(platformOpenBlobForWriteResponse);
        AppSearchBatchResult<android.app.appsearch.AppSearchBlobHandle, ParcelFileDescriptor>
                platformBatchResult = platformOpenBlobForWriteResponse.getResult();

        return new OpenBlobForWriteResponse(
                AppSearchResultToPlatformConverter.platformAppSearchBatchResultToJetpack(
                        platformBatchResult,
                        AppSearchBlobHandleToPlatformConverter::toJetpackBlobHandle,
                        Function.identity()));
    }

    /**
     * Translates a platform {@link android.app.appsearch.OpenBlobForReadResponse} into a jetpack
     * {@link OpenBlobForReadResponse}.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public static @NonNull OpenBlobForReadResponse toJetpackOpenBlobForReadResponse(
            android.app.appsearch.@NonNull OpenBlobForReadResponse
                    platformOpenBlobForReadResponse) {
        Preconditions.checkNotNull(platformOpenBlobForReadResponse);
        AppSearchBatchResult<android.app.appsearch.AppSearchBlobHandle, ParcelFileDescriptor>
                platformBatchResult = platformOpenBlobForReadResponse.getResult();

        return new OpenBlobForReadResponse(
                AppSearchResultToPlatformConverter.platformAppSearchBatchResultToJetpack(
                        platformBatchResult,
                        AppSearchBlobHandleToPlatformConverter::toJetpackBlobHandle,
                        Function.identity()));
    }
}
