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

interface Thing {
    val identifier: String?
    val name: Name?

    fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = ThingBuilderImpl()
    }

    @Suppress("StaticFinalBuilder")
    interface Builder<Self : Builder<Self>> {
        fun build(): Thing

        fun setIdentifier(text: String?): Self
        fun setName(text: String): Self
        fun setName(name: Name?): Self
        fun clearName(): Self
    }
}

private class ThingBuilderImpl : Thing.Builder<ThingBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null

    override fun build(): Thing = ThingImpl(identifier, name)

    override fun setIdentifier(text: String?): ThingBuilderImpl =
        apply { identifier = text }

    override fun setName(text: String): ThingBuilderImpl = apply { name = Name(text) }
    override fun setName(name: Name?): ThingBuilderImpl = apply { this.name = name }
    override fun clearName(): ThingBuilderImpl = apply { name = null }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as ThingBuilderImpl
        if (this.name != other.name) return false
        if (this.identifier != other.identifier) return false
        return true
    }
}

private class ThingImpl(override val identifier: String?, override val name: Name?) : Thing {
    override fun toBuilder(): Thing.Builder<*> =
        ThingBuilderImpl().setIdentifier(identifier).setName(name)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThingImpl) return false
        if (this.name != other.name) return false
        if (this.identifier != other.identifier) return false
        return true
    }
}