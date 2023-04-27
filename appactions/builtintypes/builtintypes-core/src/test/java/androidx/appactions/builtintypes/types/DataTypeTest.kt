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

package androidx.appactions.builtintypes.types

import androidx.appactions.builtintypes.properties.Name
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DataTypeTest {
  @Test
  fun testBuilder() {
    val thing =
      Thing.Builder()
        // convenience setter
        .setDisambiguatingDescription(Thing.DisambiguatingDescriptionValue.SONG)
        .setName(Name("Bohemian Rhapsody")) // authoritative setter
        .build()
    assertThat(thing.disambiguatingDescription?.asText).isNull()
    assertThat(thing.disambiguatingDescription?.asCanonicalValue)
      .isEqualTo(Thing.DisambiguatingDescriptionValue.SONG)
    assertThat(thing.name?.asText).isEqualTo("Bohemian Rhapsody")
  }

  @Test
  fun testEquals() {
    val thing1 =
      Thing.Builder()
        .setName("John Wick 4")
        .setDisambiguatingDescription(Thing.DisambiguatingDescriptionValue.MOVIE)
        .build()
    val thing2 =
      Thing.Builder()
        .setName("John Wick 4")
        .setDisambiguatingDescription(Thing.DisambiguatingDescriptionValue.MOVIE)
        .build()
    assertThat(thing1).isEqualTo(thing2)
  }

  @Test
  fun testCopying() {
    val thing =
      Thing.Builder()
        .setName("John Wick 4")
        .setDisambiguatingDescription(Thing.DisambiguatingDescriptionValue.MOVIE)
        .build()
    val copy = thing.toBuilder().setName("John Wick 2").build()
    assertThat(copy)
      .isEqualTo(
        Thing.Builder()
          .setName("John Wick 2")
          .setDisambiguatingDescription(Thing.DisambiguatingDescriptionValue.MOVIE)
          .build()
      )
  }
}
