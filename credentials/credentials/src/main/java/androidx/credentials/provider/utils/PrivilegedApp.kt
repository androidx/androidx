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

package androidx.credentials.provider.utils

import android.os.Build
import org.json.JSONObject

internal data class PrivilegedApp(
    val packageName: String,
    val fingerprints: Set<String>
) {
    companion object {
        private const val PACKAGE_NAME_KEY = "package_name"
        private const val SIGNATURES_KEY = "signatures"
        private const val FINGERPRINT_KEY = "cert_fingerprint_sha256"
        private const val BUILD_KEY = "build"
        private const val USER_DEBUG_KEY = "userdebug"
        private const val TYPE_KEY = "type"
        private const val APP_INFO_KEY = "info"
        private const val ANDROID_TYPE_KEY = "android"
        private const val USER_BUILD_TYPE = "userdebug"
        private const val APPS_KEY = "apps"

        @JvmStatic
        internal fun extractPrivilegedApps(jsonObject: JSONObject): List<PrivilegedApp> {
            val apps = mutableListOf<PrivilegedApp>()
            val appsJsonArray = jsonObject.getJSONArray(APPS_KEY)
            for (i in 0 until appsJsonArray.length()) {
                val appJsonObject = appsJsonArray.getJSONObject(i)
                if (appJsonObject.getString(TYPE_KEY) != ANDROID_TYPE_KEY) {
                    continue
                }
                apps.add(
                    createFromJSONObject(
                        appJsonObject.getJSONObject(APP_INFO_KEY), filterUserDebug = true
                    )
                )
            }
            return apps
        }

        @JvmStatic
        fun createFromJSONObject(
            appInfoJsonObject: JSONObject,
            filterUserDebug: Boolean
        ): PrivilegedApp {
            val signaturesJson = appInfoJsonObject.getJSONArray(SIGNATURES_KEY)
            val fingerprints = mutableSetOf<String>()
            for (j in 0 until signaturesJson.length()) {
                if (filterUserDebug) {
                    if (USER_DEBUG_KEY == signaturesJson.getJSONObject(j)
                            .optString(BUILD_KEY) && USER_BUILD_TYPE != Build.TYPE
                    ) {
                        continue
                    }
                }
                fingerprints.add(signaturesJson.getJSONObject(j).getString(FINGERPRINT_KEY))
            }
            return PrivilegedApp(
                packageName = appInfoJsonObject.getString(PACKAGE_NAME_KEY),
                fingerprints = fingerprints
            )
        }
    }
}
