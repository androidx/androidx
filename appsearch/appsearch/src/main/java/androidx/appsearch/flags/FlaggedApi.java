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

package androidx.appsearch.flags;

import androidx.annotation.RestrictTo;

/**
 * Indicates an API is part of a feature that is guarded by an aconfig flag in the framework, and
 * only available if the flag is enabled.
 *
 * <p>Our own Jetpack version is created here for code sync purpose.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY)
public @interface FlaggedApi {
    String value();
}
