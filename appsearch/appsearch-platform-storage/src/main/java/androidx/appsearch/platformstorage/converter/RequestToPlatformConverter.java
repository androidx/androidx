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
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportSystemUsageRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Map;

/**
 * Translates between Platform and Jetpack versions of requests.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class RequestToPlatformConverter {
    private RequestToPlatformConverter() {}

    /**
     * Translates a jetpack {@link PutDocumentsRequest} into a platform
     * {@link android.app.appsearch.PutDocumentsRequest}.
     */
    @NonNull
    public static android.app.appsearch.PutDocumentsRequest toPlatformPutDocumentsRequest(
            @NonNull PutDocumentsRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        android.app.appsearch.PutDocumentsRequest.Builder platformBuilder =
                new android.app.appsearch.PutDocumentsRequest.Builder();
        for (GenericDocument jetpackDocument : jetpackRequest.getGenericDocuments()) {
            platformBuilder.addGenericDocuments(
                    GenericDocumentToPlatformConverter.toPlatformGenericDocument(jetpackDocument));
        }
        return platformBuilder.build();
    }

    /**
     * Translates a jetpack {@link GetByDocumentIdRequest} into a platform
     * {@link android.app.appsearch.GetByDocumentIdRequest}.
     */
    @NonNull
    public static android.app.appsearch.GetByDocumentIdRequest toPlatformGetByDocumentIdRequest(
            @NonNull GetByDocumentIdRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        android.app.appsearch.GetByDocumentIdRequest.Builder platformBuilder =
                new android.app.appsearch.GetByDocumentIdRequest.Builder(
                        jetpackRequest.getNamespace())
                        .addIds(jetpackRequest.getIds());
        for (Map.Entry<String, List<String>> projection :
                jetpackRequest.getProjectionsInternal().entrySet()) {
            platformBuilder.addProjection(projection.getKey(), projection.getValue());
        }
        return platformBuilder.build();
    }

    /**
     * Translates a jetpack {@link RemoveByDocumentIdRequest} into a platform
     * {@link android.app.appsearch.RemoveByDocumentIdRequest}.
     */
    @NonNull
    public static android.app.appsearch.RemoveByDocumentIdRequest
            toPlatformRemoveByDocumentIdRequest(
            @NonNull RemoveByDocumentIdRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        return new android.app.appsearch.RemoveByDocumentIdRequest.Builder(
                jetpackRequest.getNamespace())
                .addIds(jetpackRequest.getIds())
                .build();
    }

    /**
     * Translates a jetpack {@link androidx.appsearch.app.ReportUsageRequest} into a platform
     * {@link android.app.appsearch.ReportUsageRequest}.
     */
    @NonNull
    public static android.app.appsearch.ReportUsageRequest toPlatformReportUsageRequest(
            @NonNull ReportUsageRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        return new android.app.appsearch.ReportUsageRequest.Builder(
                jetpackRequest.getNamespace(), jetpackRequest.getDocumentId())
                .setUsageTimestampMillis(jetpackRequest.getUsageTimestampMillis())
                .build();
    }

    /**
     * Translates a jetpack {@link androidx.appsearch.app.ReportSystemUsageRequest} into a platform
     * {@link android.app.appsearch.ReportSystemUsageRequest}.
     */
    @NonNull
    public static android.app.appsearch.ReportSystemUsageRequest toPlatformReportSystemUsageRequest(
            @NonNull ReportSystemUsageRequest jetpackRequest) {
        Preconditions.checkNotNull(jetpackRequest);
        return new android.app.appsearch.ReportSystemUsageRequest.Builder(
                jetpackRequest.getPackageName(),
                jetpackRequest.getDatabaseName(),
                jetpackRequest.getNamespace(),
                jetpackRequest.getDocumentId())
                .setUsageTimestampMillis(jetpackRequest.getUsageTimestampMillis())
                .build();
    }
}
