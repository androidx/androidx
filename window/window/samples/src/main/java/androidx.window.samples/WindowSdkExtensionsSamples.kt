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

package androidx.window.samples

import androidx.annotation.Sampled
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributesCalculatorParams
import androidx.window.embedding.SplitController
import androidx.window.samples.embedding.context

@Sampled
fun checkWindowSdkExtensionsVersion() {
    // For example, SplitController#setSplitAttributesCalculator requires extension version 2.
    // Callers must check the current extension version before invoking the API, or exception will
    // be thrown.
    if (WindowSdkExtensions.getInstance().extensionVersion >= 2) {
        SplitController.getInstance(context).setSplitAttributesCalculator(splitAttributesCalculator)
    }
}

val splitAttributesCalculator = { _: SplitAttributesCalculatorParams ->
    SplitAttributes.Builder().build()
}

@Sampled
fun annotateRequiresWindowSdkExtension() {
    // Given that there's an API required Window SDK Extension version 3
    @RequiresWindowSdkExtension(3)
    fun coolFeature() {}

    // Developers can use @RequiresWindowSdkExtension to annotate their own functions to document
    // the required minimum API level.
    @RequiresWindowSdkExtension(3)
    fun useCoolFeatureNoCheck() {
        coolFeature()
    }

    // Then users know they should wrap the function with version check
    if (WindowSdkExtensions.getInstance().extensionVersion >= 3) {
        useCoolFeatureNoCheck()
    }
}
