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

package androidx.health.services.client.data

import androidx.health.services.client.data.ExerciseEventType.Companion.GOLF_SHOT_EVENT
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseEventTypeTest {
  @Test
  fun exerciseEventTypeGolfShotEventRoundTrip() {
    val proto = GOLF_SHOT_EVENT.toProto()
    val exerciseEventType = ExerciseEventType.fromProto(proto)

    assertThat(exerciseEventType.toString()).isEqualTo("ExerciseEventType{ GolfShotEvent }")
    assertThat(exerciseEventType).isEqualTo(GOLF_SHOT_EVENT)
  }
}
