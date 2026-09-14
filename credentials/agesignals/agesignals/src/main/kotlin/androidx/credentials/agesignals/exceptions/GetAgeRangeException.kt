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

package androidx.credentials.agesignals.exceptions

import androidx.annotation.RestrictTo

/** Signals that an age range request failed. */
public abstract class GetAgeRangeException
@JvmOverloads
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public open val type: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val errorMessage: CharSequence? = null,
) : Exception(errorMessage?.toString())
