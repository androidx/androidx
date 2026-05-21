/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.xr.testutils

import android.os.Build
import androidx.test.filters.AbstractFilter
import androidx.test.filters.CustomFilter
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import org.junit.runner.Description

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@CustomFilter(filterClass = XrDeviceFilter::class)
annotation class XrDeviceTest(
    val include: Array<XrDeviceConfig> = [],
    val exclude: Array<XrDeviceConfig> = [],
)

enum class XrDeviceConfig(val predicate: () -> Boolean) {
    EMULATOR(::isEmulator),
    PHYSICAL_DEVICE({ !isEmulator() }),
}

class XrDeviceFilter : AbstractFilter() {

    override fun evaluateTest(description: Description): Boolean {
        if (!isXrDevice()) {
            return false
        }

        val annotationValue = description.getAnnotation(XrDeviceTest::class.java) ?: return true
        val include = annotationValue.include
        val exclude = annotationValue.exclude

        if (exclude.isNotEmpty() && exclude.any { it.predicate() }) {
            return false
        }

        return if (include.isNotEmpty()) {
            include.any { it.predicate() }
        } else {
            // If no include is specified, default to passing all non-excluded devices
            true
        }
    }

    override fun describe(): String {
        return "Filters tests based on XrDeviceTest annotation"
    }
}

private fun isEmulator(): Boolean =
    Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.HARDWARE.contains("gce_x86")

private fun isXrDevice(): Boolean {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return context.packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)
}
