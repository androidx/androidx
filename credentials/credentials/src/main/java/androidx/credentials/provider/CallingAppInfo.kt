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

package androidx.credentials.provider

/**
 * Information about the calling app. This is a read-only data class that
 * providers should use to retrieve information about the calling app.
 *
 * @property appPackage the package name of the calling app
 * @property appSignature the app signature of the calling app
 *
 * @hide
 */
class CallingAppInfo internal constructor(
    val appPackage: String,
    // TODO("Check for non empty when framework change available")
    val appSignature: Set<android.content.pm.Signature>
    ) {

    init {
        require(appPackage.isNotEmpty()) {
            "appPackage must not be empty"
        }
    }
}