/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core

import android.content.Context
import androidx.annotation.RestrictTo
import java.util.Objects.requireNonNull
import java.util.regex.Pattern

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LibInfo(val context: Context) {
    fun getVersion(): Version {
        return Version.parse(
            context.resources.getString(R.string.appactions_interaction_library_version)
        )
    }

    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preReleaseId: String? = null,
  ) : Comparable<Version> {

    override fun compareTo(other: Version) = compareValuesBy(
        this, other,
        { it.major },
        { it.minor },
        { it.patch },
        { it.preReleaseId == null }, // False (no preReleaseId) sorts above true (has preReleaseId)
        { it.preReleaseId } // gradle uses lexicographic ordering
    )

    override fun toString(): String {
        return if (preReleaseId != null) {
            "$major.$minor.$patch-$preReleaseId"
        } else "$major.$minor.$patch"
    }

    companion object {
      private val VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(-.+)?$")

      fun parse(versionString: String): Version {
        val matcher = VERSION_REGEX.matcher(versionString)
        if (!matcher.matches()) {
          throw IllegalArgumentException("Can not parse version: $versionString")
        }
        return Version(
          Integer.parseInt(requireNonNull(matcher.group(1))),
          Integer.parseInt(requireNonNull(matcher.group(2))),
          Integer.parseInt(requireNonNull(matcher.group(3))),
          if (matcher.groupCount() == 4) requireNonNull(matcher.group(4)).drop(1) else null
        )
      }
    }
  }
}
