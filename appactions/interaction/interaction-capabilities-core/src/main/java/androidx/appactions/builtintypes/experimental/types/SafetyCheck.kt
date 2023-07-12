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

package androidx.appactions.builtintypes.experimental.types

import androidx.appactions.builtintypes.experimental.properties.Name
import java.time.Duration
import java.time.ZonedDateTime

interface SafetyCheck : Thing {
    val duration: Duration?
    val checkInTime: ZonedDateTime?

    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = SafetyCheckBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        fun setDuration(duration: Duration?): Self
        fun setCheckInTime(checkInTime: ZonedDateTime?): Self
        override fun build(): SafetyCheck
    }
}

private class SafetyCheckBuilderImpl : SafetyCheck.Builder<SafetyCheckBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null
    private var duration: Duration? = null
    private var checkInTime: ZonedDateTime? = null

    override fun build(): SafetyCheck = SafetyCheckImpl(identifier, name, duration, checkInTime)

    override fun setDuration(duration: Duration?): SafetyCheckBuilderImpl =
        apply { this.duration = duration }

    override fun setCheckInTime(checkInTime: ZonedDateTime?): SafetyCheckBuilderImpl =
        apply { this.checkInTime = checkInTime }

    override fun setIdentifier(text: String?): SafetyCheckBuilderImpl = apply { identifier = text }

    override fun setName(text: String): SafetyCheckBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): SafetyCheckBuilderImpl = apply { this.name = name }

    override fun clearName(): SafetyCheckBuilderImpl = apply { name = null }
}

private class SafetyCheckImpl(
    override val identifier: String?,
    override val name: Name?,
    override val duration: Duration?,
    override val checkInTime: ZonedDateTime?
) : SafetyCheck {
    override fun toBuilder(): SafetyCheck.Builder<*> =
        SafetyCheckBuilderImpl()
            .setIdentifier(identifier)
            .setName(name)
            .setDuration(duration)
            .setCheckInTime(checkInTime)
}