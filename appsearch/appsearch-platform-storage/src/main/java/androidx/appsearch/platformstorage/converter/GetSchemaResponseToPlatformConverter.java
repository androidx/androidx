/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.appsearch.app.GetSchemaResponse;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

/**
 * Translates between Platform and Jetpack versions of {@link GetSchemaResponse}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class GetSchemaResponseToPlatformConverter {
    private GetSchemaResponseToPlatformConverter() {}

    /**
     * Translates a platform {@link android.app.appsearch.GetSchemaResponse} into a jetpack
     * {@link GetSchemaResponse}.
     */
    @NonNull
    @BuildCompat.PrereleaseSdkCheck
    public static GetSchemaResponse toJetpackGetSchemaResponse(
            @NonNull android.app.appsearch.GetSchemaResponse platformResponse) {
        Preconditions.checkNotNull(platformResponse);
        GetSchemaResponse.Builder jetpackBuilder;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // Android API level in S-v2 and lower won't have any supported feature.
            jetpackBuilder = new GetSchemaResponse.Builder(/*getVisibilitySettingSupported=*/false);
        } else {
            // The regular builder has all supported features.
            jetpackBuilder = new GetSchemaResponse.Builder();
        }
        for (android.app.appsearch.AppSearchSchema platformSchema : platformResponse.getSchemas()) {
            jetpackBuilder.addSchema(SchemaToPlatformConverter.toJetpackSchema(platformSchema));
        }
        jetpackBuilder.setVersion(platformResponse.getVersion());
        // TODO(b/205749173) add visibility setting when they are supported in framework.
        return jetpackBuilder.build();
    }
}
