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

interface ListItem : Thing {
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = ListItemBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        override fun build(): ListItem
    }
}

private class ListItemBuilderImpl : ListItem.Builder<ListItemBuilderImpl> {
    private var identifier: String? = null
    private var name: Name? = null
    override fun build() = ListItemImpl(identifier, name)

    override fun setIdentifier(text: String?): ListItemBuilderImpl = apply {
        identifier = text
    }

    override fun setName(text: String): ListItemBuilderImpl = apply {
        name = Name(text)
    }

    override fun setName(name: Name?): ListItemBuilderImpl = apply {
        this.name = name
    }

    override fun clearName(): ListItemBuilderImpl = apply { name = null }
}

private class ListItemImpl(
    override val identifier: String?,
    override val name: Name?
) : ListItem {
    override fun toBuilder(): ListItem.Builder<*> =
        ListItemBuilderImpl().setIdentifier(identifier).setName(name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListItemImpl) return false
        if (this.name != other.name) return false
        if (this.identifier != other.identifier) return false
        return true
    }
}