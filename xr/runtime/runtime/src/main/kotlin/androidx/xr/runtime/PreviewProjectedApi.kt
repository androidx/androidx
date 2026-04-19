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

package androidx.xr.runtime

/**
 * Marks declarations that are part of the unstable Projected API Preview.
 *
 * These APIs are not final and are subject to change or removal in future releases without notice.
 * They are intended for development and testing purposes only and require a specific developer
 * platform version to function correctly.
 *
 * Any usage of a declaration annotated with `@PreviewProjectedApi` must be explicitly opted-in by
 * annotating the calling code with `@OptIn(PreviewProjectedApi::class)`.
 *
 * Furthermore, to prevent runtime errors, applications must wrap calls to these APIs in a
 * `try-catch` block to handle cases where the device does not support the required preview API
 * version.
 *
 * Example of opting-in and performing a runtime check:
 * <pre><code class="language-kotlin">
 * @PreviewProjectedApi
 * fun newPreviewApi() {
 *     // ...
 * }
 *
 * @OptIn(PreviewProjectedApi::class)
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
    message =
        "This API is in an unstable preview state and requires a developer platform version" +
            " to work properly. Do not use this API in release builds as it will likely to lead" +
            " to crashes."
)
@Retention(AnnotationRetention.BINARY)
public annotation class PreviewProjectedApi
