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

interface Timer : Thing {
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = TimerBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        override fun build(): Timer
    }
}

private class TimerBuilderImpl : Timer.Builder<TimerBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null

    override fun build() = TimerImpl(identifier, name)

    override fun setIdentifier(text: String?): TimerBuilderImpl = apply { identifier = text }

    override fun setName(text: String): TimerBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): TimerBuilderImpl = apply { this.name = name }

    override fun clearName(): TimerBuilderImpl = apply { name = null }
}

private class TimerImpl(override val identifier: String?, override val name: Name?) : Timer {
    override fun toBuilder(): Timer.Builder<*> =
        TimerBuilderImpl().setIdentifier(identifier).setName(name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimerImpl) return false
        if (identifier != other.identifier) return false
        if (name != other.name) return false
        return true
    }
}