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

package androidx.window.core

import android.util.Log
import androidx.annotation.IntRange
import androidx.window.core.VerificationMode.LOG
import androidx.window.extensions.WindowExtensionsProvider

internal object ExtensionsUtil {

    private val TAG = ExtensionsUtil::class.simpleName

    @get:IntRange(from = 0)
    val safeVendorApiLevel: Int
        get() {
            return try {
                WindowExtensionsProvider.getWindowExtensions().vendorApiLevel
            } catch (e: NoClassDefFoundError) {
                if (BuildConfig.verificationMode == LOG) {
                    Log.d(TAG, "Embedding extension version not found")
                }
                0
            } catch (e: UnsupportedOperationException) {
                if (BuildConfig.verificationMode == LOG) {
                    Log.d(TAG, "Stub Extension")
                }
                0
            } catch (e: NullPointerException) {
                if (BuildConfig.verificationMode == LOG) {
                    Log.d(TAG, "Error with Extension implementation")
                }
                0
            }
        }
}
