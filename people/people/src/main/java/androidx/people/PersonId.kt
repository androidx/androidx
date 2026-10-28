/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people

import androidx.annotation.RestrictTo

/** Unique identifier for a person record within a specific People Directory application. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PersonId(
    /** Stable, opaque string key provided by the originating People Directory. */
    public val lookupKey: String,
    /** Android package name of the People Directory application. */
    public val directoryPackage: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PersonId) return false
        return lookupKey == other.lookupKey && directoryPackage == other.directoryPackage
    }

    override fun hashCode(): Int {
        var result = lookupKey.hashCode()
        result = 31 * result + directoryPackage.hashCode()
        return result
    }

    override fun toString(): String =
        "PersonId(lookupKey=$lookupKey, directoryPackage=$directoryPackage)"
}
