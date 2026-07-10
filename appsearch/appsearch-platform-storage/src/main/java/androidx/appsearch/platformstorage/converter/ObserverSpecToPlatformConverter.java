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

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.observer.SchemaChangeInfo;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * Translates between Platform and Jetpack versions of {@link ObserverSpec}.
 */
@HideInPlatform
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public final class ObserverSpecToPlatformConverter {
    private ObserverSpecToPlatformConverter() {}

    /**
     * Translates a jetpack {@link ObserverSpec} into a platform
     * {@link android.app.appsearch.observer.ObserverSpec}.
     */
    public static android.app.appsearch.observer.@NonNull ObserverSpec toPlatformObserverSpec(
            @NonNull ObserverSpec jetpackSpec) {
        Preconditions.checkNotNull(jetpackSpec);
        return new android.app.appsearch.observer.ObserverSpec.Builder()
                .addFilterSchemas(jetpackSpec.getFilterSchemas())
                .build();
    }

    /**
     * Translates a platform {@link androidx.appsearch.observer.SchemaChangeInfo} into a jetpack
     * {@link SchemaChangeInfo}.
     */
    public static @NonNull SchemaChangeInfo toJetpackSchemaChangeInfo(
            android.app.appsearch.observer.@NonNull SchemaChangeInfo platformInfo) {
        Preconditions.checkNotNull(platformInfo);
        return new SchemaChangeInfo(
                platformInfo.getPackageName(),
                platformInfo.getDatabaseName(),
                platformInfo.getChangedSchemaNames());
    }

    /**
     * Translates a platform {@link androidx.appsearch.observer.DocumentChangeInfo} into a jetpack
     * {@link DocumentChangeInfo}.
     */
    public static @NonNull DocumentChangeInfo toJetpackDocumentChangeInfo(
            android.app.appsearch.observer.@NonNull DocumentChangeInfo platformInfo) {
        Preconditions.checkNotNull(platformInfo);
        return new DocumentChangeInfo(
                platformInfo.getPackageName(),
                platformInfo.getDatabaseName(),
                platformInfo.getNamespace(),
                platformInfo.getSchemaName(),
                platformInfo.getChangedDocumentIds());
    }
}
