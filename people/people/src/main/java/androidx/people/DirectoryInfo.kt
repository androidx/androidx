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

/** Metadata describing a registered People Directory application. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DirectoryInfo(
    /** Package name of the directory application. */
    public val packageName: String,
    /** Content provider authority (or GMS identifier) of the directory. */
    public val authority: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DirectoryInfo) return false
        return packageName == other.packageName && authority == other.authority
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + authority.hashCode()
        return result
    }

    override fun toString(): String =
        "DirectoryInfo(packageName=$packageName, authority=$authority)"
}
