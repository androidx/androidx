// Copyright 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package androidx.appactions.builtintypes.samples.properties

import androidx.`annotation`.Sampled
import androidx.appactions.builtintypes.properties.DisambiguatingDescription
import kotlin.String

@Sampled
public fun disambiguatingDescriptionMapWhenUsage(
  disambiguatingDescription: DisambiguatingDescription
) =
  disambiguatingDescription.mapWhen(
    object : DisambiguatingDescription.Mapper<String> {
      public override fun text(instance: String): String = """Got String: $instance"""

      public override fun canonicalValue(
        instance: DisambiguatingDescription.CanonicalValue
      ): String = """Got a canonical value for DisambiguatingDescription: $instance"""

      public override fun orElse(): String =
        """Got some unrecognized variant: $disambiguatingDescription"""
    }
  )
