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

package androidx.appactions.interaction.capabilities.productivity

import androidx.appactions.builtintypes.experimental.types.Timer
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.core.SearchAction
import java.util.Objects

class TimerValue
private constructor(
    val asTimer: Timer?,
    val asTimerFilter: SearchAction<Timer>?,
) {
    constructor(timer: Timer) : this(timer, null)

    // TODO(b/268071906) add TimerFilter type to SearchAction
    constructor(timerFilter: SearchAction<Timer>) : this(null, timerFilter)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimerValue

        return asTimer == other.asTimer && asTimerFilter == other.asTimerFilter
    }

    override fun hashCode(): Int {
        return Objects.hash(asTimer, asTimerFilter)
    }

    companion object {
        private val TYPE_SPEC =
            UnionTypeSpec.Builder<TimerValue>()
                .bindMemberType(
                    memberGetter = TimerValue::asTimer,
                    ctor = { TimerValue(it) },
                    typeSpec = TypeConverters.TIMER_TYPE_SPEC,
                )
                .bindMemberType(
                    memberGetter = TimerValue::asTimerFilter,
                    ctor = { TimerValue(it) },
                    typeSpec =
                        TypeConverters.createSearchActionTypeSpec(
                            TypeConverters.TIMER_TYPE_SPEC,
                        ),
                )
                .build()

        internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)

        internal val ENTITY_CONVERTER = EntityConverter.of(TYPE_SPEC)
    }
}
