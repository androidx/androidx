/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.xr.runtime

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Defines the valid integer constants for *stable* Spatial API versions. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    SpatialApiVersions.UNKNOWN,
    SpatialApiVersions.SPATIAL_API_V1,
    SpatialApiVersions.SPATIAL_API_V2,
    SpatialApiVersions.SPATIAL_API_V3,
)
public annotation class SpatialApiVersion

/** All the Spatial API versions supported by Android XR. */
// To add a new API version, add a new constant here and update the @IntDef above.
public object SpatialApiVersions {
    /**
     * Unknown API version.
     *
     * Used when the API version hasn't been established yet
     */
    public const val UNKNOWN: Int = 0
    /** Initial API version. */
    public const val SPATIAL_API_V1: Int = 1
    /** API version 2. */
    public const val SPATIAL_API_V2: Int = 2
    /** API version 3. */
    public const val SPATIAL_API_V3: Int = 3

    /** The latest stable Spatial API version. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public const val LATEST_STABLE_API_LEVEL: Int = SPATIAL_API_V2
}

/**
 * Denotes that the annotated element should only be called on devices that support a given Spatial
 * API version.
 *
 * The Spatial API version required by an element is specified by the [value] parameter. The value
 * should be one of the constants from [androidx.xr.runtime.SpatialApiVersions].
 *
 * To ensure that an API is available at runtime, applications should check the current Spatial API
 * version by querying [androidx.xr.runtime.SpatialApiVersionHelper.spatialApiVersion]. This check
 * is crucial for preventing runtime errors when an application is running on a device that does not
 * support the required API version.
 *
 * Example of annotating an API and performing a runtime check:
 * <pre><code class="language-kotlin">
 * @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
 * fun newApiForV2() {
 *     // ...
 * }
 *
 * fun callNewApi() {
 *     if (XrApiVersionHelper.spatialApiVersion >= SpatialApiVersions.SPATIAL_API_V2) {
 *         newApiForV2()
 *     } else {
 *         // Handle the case where the API is not available.
 *     }
 * }
 * </code></pre>
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
public annotation class RequiresSpatialApi(
    /**
     * The minimum Spatial API version required. The value should be one of the constants from
     * [androidx.xr.runtime.SpatialApiVersions].
     */
    @SpatialApiVersion public val value: Int
)

/**
 * Marks declarations that are part of the unstable Spatial API Preview, version 3.
 *
 * These APIs are not final and are subject to change or removal in future releases without notice.
 * They are intended for development and testing purposes only and require a specific developer
 * preview system image to function correctly. The version number in this annotation's name will
 * increase in future releases to correspond with the next upcoming stable API version (e.g.,
 * `@PreviewSpatialApi4` for `SpatialApiVersions.SPATIAL_API_V4`).
 *
 * Any usage of a declaration annotated with `@PreviewSpatialApi3` must be explicitly opted-in by
 * annotating the calling code with `@OptIn(PreviewSpatialApi3::class)`.
 *
 * Furthermore, to prevent runtime errors, applications must wrap calls to these APIs in a
 * `try-catch` block to handle cases where the device does not support the required preview API
 * version.
 *
 * Example of opting-in and performing a runtime check:
 * <pre><code class="language-kotlin">
 * @PreviewSpatialApi3
 * fun newPreviewApi() {
 *     // ...
 * }
 *
 * @OptIn(PreviewSpatialApi3::class)
 * fun callPreviewApi() {
 *     try {
 *         newPreviewApi()
 *     } catch (e: NoSuchMethodError) {
 *         // Handle the case where the preview API is not available.
 *     }
 * }
 * </code></pre>
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message =
        "This API is in an unstable preview state and requires a developer preview system " +
            "image work properly. Do not use this API in release builds as it will likely to " +
            "lead to crashes.",
)
@Retention(AnnotationRetention.BINARY)
public annotation class PreviewSpatialApi3
