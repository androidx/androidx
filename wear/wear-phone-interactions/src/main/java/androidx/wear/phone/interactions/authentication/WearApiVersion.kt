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
package androidx.wear.phone.interactions.authentication

import android.os.Build
import com.google.wear.Sdk

/** Provides wear sdk api version. */
internal class WearApiVersion {

    // TODO(b/307543793): Reuse the generalized `WearApiVersionHelper` once available.
    /** Exposes version of wear sdk. Returns 0 if wear-sdk is not present. */
    val wearSdkVersion: Int
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Wear SDK INT can only be accessed safely from UPSIDE_DOWN_CAKE, introduced from
                // tiramisu kr2.
                // Or crashes with `NoSuchField` will be experienced.
                return Sdk.VERSION.WEAR_SDK_INT
            }
            return 0
        }
}
