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
import java.time.LocalDateTime
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
        .setDisambiguatingDescription("Awesome Thing")
        .setName(Name("Bohemian Rhapsody")) // authoritative setter
        .build()
    assertThat(thing.disambiguatingDescription?.asCanonicalValue).isNull()
    assertThat(thing.disambiguatingDescription?.asText).isEqualTo("Awesome Thing")
    assertThat(thing.name?.asText).isEqualTo("Bohemian Rhapsody")
  }

  @Test
  fun testEquals() {
    val thing1 = Thing.Builder().setName("John Wick 4").build()
    val thing2 = Thing.Builder().setName("John Wick 4").build()
    assertThat(thing1).isEqualTo(thing2)
  }

  @Test
  fun testCopying() {
    val thing = Thing.Builder().setName("John Wick 4").setDisambiguatingDescription("Movie").build()
    val copy = thing.toBuilder().setName("John Wick 2").build()
    assertThat(copy)
      .isEqualTo(
        Thing.Builder().setName("John Wick 2").setDisambiguatingDescription("Movie").build()
      )
  }

  @Test
  fun testPolymorphicCopying() {
    val thing1: Thing =
      Alarm.Builder()
        .setName("Wake up!")
        .setAlarmSchedule(
          Schedule.Builder()
            .setStartDate(
              LocalDateTime.of(
                /* year= */ 2023,
                /* month= */ 5,
                /* dayOfMonth= */ 3,
                /* hour= */ 8,
                /* minute=*/ 30
              )
            )
            .build()
        )
        .build()
    val thing2 = thing1.toBuilder().setName("Go to bed!").build()
    assertThat(thing2).isInstanceOf(Alarm::class.java)
    assertThat(thing2)
      .isEqualTo(
        Alarm.Builder()
          .setName("Go to bed!")
          .setAlarmSchedule(
            Schedule.Builder()
              .setStartDate(
                LocalDateTime.of(
                  /* year= */ 2023,
                  /* month= */ 5,
                  /* dayOfMonth= */ 3,
                  /* hour= */ 8,
                  /* minute=*/ 30
                )
              )
              .build()
          )
          .build()
      )
  }
}
