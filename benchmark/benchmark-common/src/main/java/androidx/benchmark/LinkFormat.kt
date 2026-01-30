/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.benchmark

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkFormat {
    /**
     * Compat version for old versions of studio, which does not support any URI parameters.
     *
     * [my trace](file://<outputRelativePath.perfetto-trace>)
     */
    V2,

    /**
     * URI + parameter file format link, only supported in newer versions of Studio.
     *
     * TODO: specify which version of Studio
     *
     * [my
     * trace](uri://<outputRelativePath.perfetto-trace>?selectionParams=<base64encodedCompressedParams>)
     */
    V3,
}
