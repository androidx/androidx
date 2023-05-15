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

interface SuccessStatus : Thing {
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = SuccessStatusBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        override fun build(): SuccessStatus
    }
}

private class SuccessStatusBuilderImpl :
    SuccessStatus.Builder<SuccessStatusBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null

    override fun build() = SuccessStatusImpl(identifier, name)

    override fun setIdentifier(text: String?): SuccessStatusBuilderImpl =
        apply { identifier = text }

    override fun setName(text: String): SuccessStatusBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): SuccessStatusBuilderImpl = apply { this.name = name }

    override fun clearName(): SuccessStatusBuilderImpl = apply { name = null }
}

private class SuccessStatusImpl(override val identifier: String?, override val name: Name?) :
    SuccessStatus {
    override fun toBuilder(): SuccessStatus.Builder<*> =
        SuccessStatusBuilderImpl().setIdentifier(identifier).setName(name)

    override fun toString(): String = "SuccessStatus"
}