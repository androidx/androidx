/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import androidx.appfunctions.`internal`.serializableproxies.`$UriFactory`

// TODO(b/413622177): Temporary workaround of generating factory before being able to apply KSP on
// appfunctions module.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class `$AppFunctionUriGrantFactory` : AppFunctionSerializableFactory<AppFunctionUriGrant> {
    override fun fromAppFunctionData(appFunctionData: AppFunctionData): AppFunctionUriGrant {
        val appFunctionDataWithSpec =
            getAppFunctionDataWithSpec(appFunctionData, "androidx.appfunctions.AppFunctionUriGrant")

        val appFunctionUriFactory = `$UriFactory`()

        val uriData = checkNotNull(appFunctionDataWithSpec.getAppFunctionData("uri"))
        val uri = appFunctionUriFactory.fromAppFunctionData(uriData)
        val modeFlags = checkNotNull(appFunctionDataWithSpec.getIntOrNull("modeFlags"))

        val resultAppFunctionUriGrant = AppFunctionUriGrant(uri, modeFlags)
        return resultAppFunctionUriGrant
    }

    override fun toAppFunctionData(appFunctionSerializable: AppFunctionUriGrant): AppFunctionData {
        val appFunctionUriGrant_appFunctionSerializable = appFunctionSerializable
        val appFunctionUriFactory = `$UriFactory`()

        val builder = getAppFunctionDataBuilder("androidx.appfunctions.AppFunctionUriGrant")
        val uri = appFunctionUriGrant_appFunctionSerializable.uri
        builder.setAppFunctionData("uri", appFunctionUriFactory.toAppFunctionData(uri))
        val modeFlags = appFunctionUriGrant_appFunctionSerializable.modeFlags
        builder.setInt("modeFlags", modeFlags)

        return builder.build()
    }
}
