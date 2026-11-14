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
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersionHelper
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import org.junit.runner.Description

/**
 * Restricts a test to XR devices, optionally narrowed by [include] and [exclude].
 *
 * May be placed on a test method or on a test class. An annotation on the method fully replaces the
 * one on the class (the [include] and [exclude] arrays are not merged). [RequiresSpatialApi] is
 * resolved the same way, independently of this annotation, and defaults to
 * [SpatialApiVersions.SPATIAL_API_V2] when absent from both the method and the class. Annotations
 * on superclasses are not inherited.
 */
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

/**
 * Filters tests annotated with [XrDeviceTest], and additionally honors [RequiresSpatialApi].
 *
 * Both annotations may be placed on the test method or on the test class. Each annotation is
 * resolved independently: an annotation on the method overrides the same annotation on the class,
 * and an annotation that is absent from the method is taken from the class. When neither the method
 * nor the class is annotated with [RequiresSpatialApi], [SpatialApiVersions.SPATIAL_API_V2] is used
 * as the default minimum Spatial API version; annotate with
 * `@RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1)` when a test must also run on V1 devices.
 */
class XrDeviceFilter internal constructor(private val spatialApiVersionProvider: () -> Int) :
    AbstractFilter() {

    constructor() : this({ SpatialApiVersionHelper.spatialApiVersion })

    override fun evaluateTest(description: Description): Boolean {
        if (!isXrDevice()) {
            return false
        }

        val annotationValue = description.findAnnotation<XrDeviceTest>() ?: return true
        val minSpatialApiVersion =
            description.findAnnotation<RequiresSpatialApi>()?.value
                ?: SpatialApiVersions.SPATIAL_API_V2
        if (spatialApiVersionProvider() < minSpatialApiVersion) {
            return false
        }

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

/**
 * Returns the annotation of type [T] on the test method, falling back to the test class when the
 * method does not have it.
 */
private inline fun <reified T : Annotation> Description.findAnnotation(): T? =
    getAnnotation(T::class.java) ?: testClass?.getAnnotation(T::class.java)

private fun isEmulator(): Boolean =
    Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.HARDWARE.contains("gce_x86") ||
        Build.HARDWARE.contains("cutf_cvm")

internal fun isXrDevice(): Boolean {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return context.packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)
}
