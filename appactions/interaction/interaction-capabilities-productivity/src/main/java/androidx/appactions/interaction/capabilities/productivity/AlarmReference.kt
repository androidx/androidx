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

import androidx.appactions.builtintypes.types.Alarm
import androidx.appactions.interaction.capabilities.core.SearchAction
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.serializers.types.ALARM_TYPE_SPEC
import java.util.Objects

class AlarmReference private constructor(
    val asAlarm: Alarm?,
    val asSearchAction: SearchAction<Alarm>?
) {
    constructor(alarm: Alarm) : this(alarm, null)

    // TODO(b/268071906) add AlarmFilter type to SearchAction
    constructor(alarmFilter: SearchAction<Alarm>) : this(null, alarmFilter)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlarmReference

        return asAlarm == other.asAlarm && asSearchAction == other.asSearchAction
    }

    override fun hashCode(): Int {
        return Objects.hash(asAlarm, asSearchAction)
    }

    companion object {
        private val TYPE_SPEC =
            UnionTypeSpec.Builder<AlarmReference>()
                .bindMemberType(
                    memberGetter = AlarmReference::asAlarm,
                    ctor = { AlarmReference(it) },
                    typeSpec = ALARM_TYPE_SPEC,
                )
                .bindMemberType(
                    memberGetter = AlarmReference::asSearchAction,
                    ctor = { AlarmReference(it) },
                    typeSpec =
                        TypeConverters.createSearchActionTypeSpec(
                            ALARM_TYPE_SPEC
                        ),
                )
                .build()
        internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)
    }
}
