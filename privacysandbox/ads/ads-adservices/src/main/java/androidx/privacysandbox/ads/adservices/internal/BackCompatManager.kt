/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.internal

import android.content.Context
import android.util.Log

internal object BackCompatManager {

    fun <T> getManager(context: Context, tag: String, manager: (Context) -> T?): T? {
        // catch NoClassDefFoundError in case uses-library tag is missing from manifest
        try {
            return manager(context)
        } catch (e: NoClassDefFoundError) {
            Log.d(
                tag,
                "Unable to find adservices code, check manifest for uses-library tag, " +
                    "versionR=${AdServicesInfo.extServicesVersionR()}, " +
                    "versionS=${AdServicesInfo.extServicesVersionS()}"
            )
            return null
        }
    }
}
