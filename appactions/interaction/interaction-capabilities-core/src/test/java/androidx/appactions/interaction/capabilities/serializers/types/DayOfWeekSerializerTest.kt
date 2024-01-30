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

package androidx.appactions.interaction.capabilities.serializers.types

import androidx.appactions.builtintypes.types.DayOfWeek
import androidx.appactions.interaction.protobuf.Value
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DayOfWeekSerializerTest {
  @Test
  fun DayOfWeek_conversion_success() {
    val MONDAY_VALUE = Value.newBuilder().setStringValue(DayOfWeek.MONDAY.canonicalUrl).build()

    assertThat(DAY_OF_WEEK_TYPE_SPEC.toValue(DayOfWeek.MONDAY)).isEqualTo(MONDAY_VALUE)
    assertThat(DAY_OF_WEEK_TYPE_SPEC.fromValue(MONDAY_VALUE)).isSameInstanceAs(DayOfWeek.MONDAY)
  }
}
