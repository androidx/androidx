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

package androidx.camera.testing

import androidx.annotation.RequiresApi
import androidx.camera.testing.IgnoreProblematicDeviceRule.Companion.isEmulator
import androidx.test.filters.AbstractFilter
import java.util.Locale
import org.junit.runner.Description

/**
 * Class that filters out tests annotated with [RequiresDevice] when running on emulator
 *
 * The detection conditions of emulator should be the same as
 * androidx.test.internal.runner.TestRequestBuilder.RequiresDeviceFilter.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class RequiresDeviceFilter : AbstractFilter() {

    companion object {
        private val annotationClass = RequiresDevice::class.java
    }

    override fun describe(): String {
        return String.format(Locale.US, "skip tests annotated with RequiresDevice if necessary")
    }

    override fun evaluateTest(description: Description): Boolean {
        if (isAnnotationPresent(description)) {
            // annotation is present - check if device is an emulator
            return !isEmulator
        }
        // Return true to run the test.
        return true
    }

    private fun isAnnotationPresent(description: Description): Boolean {
        val testClass: Class<*>? = description.testClass
        return (testClass != null && testClass.isAnnotationPresent(annotationClass)) ||
            description.getAnnotation(annotationClass) != null
    }
}
