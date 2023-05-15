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

package androidx.appactions.interaction.capabilities.core.impl.converters

import androidx.appactions.builtintypes.experimental.types.Alarm
import androidx.appactions.builtintypes.experimental.types.Timer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* Union type for testing */
class AlarmOrTimer private constructor(
  val asAlarm: Alarm? = null,
  val asTimer: Timer? = null,
) {
  constructor(alarm: Alarm) : this(asAlarm = alarm)
  constructor(timer: Timer) : this(asTimer = timer)
}

private val ALARM_OR_TIMER_TYPE_SPEC = UnionTypeSpec.Builder<AlarmOrTimer>()
  .bindMemberType(
    memberGetter = AlarmOrTimer::asAlarm,
    ctor = { AlarmOrTimer(it) },
    typeSpec = TypeConverters.ALARM_TYPE_SPEC,
  )
  .bindMemberType(
    memberGetter = AlarmOrTimer::asTimer,
    ctor = { AlarmOrTimer(it) },
    typeSpec = TypeConverters.TIMER_TYPE_SPEC,
  ).build()

@RunWith(JUnit4::class)
class UnionTypeSpecTest {
  @Test
  fun unionType_identifier() {
    val alarmUnion = AlarmOrTimer(Alarm.Builder().setIdentifier("alarmId").build())
    val timerUnion = AlarmOrTimer(Timer.Builder().setIdentifier("timerId").build())
    val timerUnionWithoutId = AlarmOrTimer(Timer.Builder().build())
    assertThat(ALARM_OR_TIMER_TYPE_SPEC.getIdentifier(alarmUnion)).isEqualTo("alarmId")
    assertThat(ALARM_OR_TIMER_TYPE_SPEC.getIdentifier(timerUnion)).isEqualTo("timerId")
    assertThat(ALARM_OR_TIMER_TYPE_SPEC.getIdentifier(timerUnionWithoutId)).isNull()
  }
}