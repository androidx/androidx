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

package androidx.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AppFunctionSerializableFactory

// TODO(b/413622177): Temporary workaround of generating factory before being able to apply KSP on
// appfunctions module.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class `$AppFunctionTextResourceFactory` :
    AppFunctionSerializableFactory<AppFunctionTextResource> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): AppFunctionTextResource {
        val appFunctionDataWithSpec =
            getAppFunctionDataWithSpec(
                appFunctionData,
                "androidx.appfunctions.AppFunctionTextResource",
            )

        val mimeType = checkNotNull(appFunctionDataWithSpec.getString("mimeType"))
        val text = checkNotNull(appFunctionDataWithSpec.getString("content"))

        val resultAppFunctionTextResource = AppFunctionTextResource(mimeType, text)
        return resultAppFunctionTextResource
    }

    override fun toAppFunctionData(
        appFunctionSerializable: AppFunctionTextResource
    ): AppFunctionData {
        val appFunctionTextResource_appFunctionSerializable = appFunctionSerializable
        val builder = getAppFunctionDataBuilder("androidx.appfunctions.AppFunctionTextResource")
        builder.setString("mimeType", appFunctionTextResource_appFunctionSerializable.mimeType)
        builder.setString("content", appFunctionTextResource_appFunctionSerializable.content)

        return builder.build()
    }
}
