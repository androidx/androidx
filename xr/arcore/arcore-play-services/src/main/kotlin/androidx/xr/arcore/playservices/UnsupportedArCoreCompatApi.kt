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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import kotlin.RequiresOptIn

/** Annotation for methods and properties that are not supported by ARCore 1.x. */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message =
        "This property is exposed for compatibility with existing ARCore 1.x applications. Usage of this property is not offically supported, and will be removed in a future release.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public annotation class UnsupportedArCoreCompatApi {}
