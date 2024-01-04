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
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DataTypeTest {
  @Test
  fun testBuilder() {
    val schedule =
      Schedule.Builder()
        .setName(Name("Some schedule")) // authoritative setter
        .setExceptDate(LocalDate.of(2023, 12, 31)) // convenience setter
        .build()
    assertThat(schedule.name?.asText).isEqualTo("Some schedule")
    assertThat(schedule.exceptDate?.asDate).isEqualTo(LocalDate.of(2023, 12, 31))
    assertThat(schedule.exceptDate?.asInstant).isNull()
  }

  @Test
  fun testEquals() {
    val thing1 = Thing.Builder().setName("John Wick 4").build()
    val thing2 = Thing.Builder().setName("John Wick 4").build()
    assertThat(thing1).isEqualTo(thing2)
  }

  @Test
  fun testCopying() {
    val thing: Thing =
      Schedule.Builder().setExceptDate(LocalDate.of(2023, 12, 31)).setName("Some schedule").build()
    val copy = thing.toBuilder().setName("Another schedule").build()
    assertThat(copy)
      .isEqualTo(
        Schedule.Builder()
          .setExceptDate(LocalDate.of(2023, 12, 31))
          .setName("Another schedule")
          .build()
      )
  }
}
