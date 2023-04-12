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

package androidx.appactions.builtintypes.types

import androidx.appactions.builtintypes.properties.Name
import androidx.appactions.builtintypes.properties.ItemListElement

interface ItemList : Thing {
    val itemListElements: List<ItemListElement>
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = ItemListBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        fun addItemListElements(itemListElements: List<ItemListElement>): Self
        fun addItemListElement(itemListElement: ItemListElement): Self
        fun addItemListElement(listItem: ListItem): Self
        override fun build(): ItemList
    }
}

private class ItemListBuilderImpl : ItemList.Builder<ItemListBuilderImpl> {
    private var identifier: String? = null
    private var name: Name? = null
    private var itemListElements = mutableListOf<ItemListElement>()

    override fun build() = ItemListImpl(identifier, name, itemListElements.toList())

    override fun addItemListElements(itemListElements: List<ItemListElement>): ItemListBuilderImpl =
        apply {
            this.itemListElements.addAll(itemListElements)
        }

    override fun addItemListElement(itemListElement: ItemListElement): ItemListBuilderImpl = apply {
        itemListElements.add(itemListElement)
    }

    override fun addItemListElement(listItem: ListItem): ItemListBuilderImpl = apply {
        itemListElements.add(ItemListElement(listItem))
    }

    override fun setIdentifier(text: String?): ItemListBuilderImpl = apply {
        identifier = text
    }

    override fun setName(text: String): ItemListBuilderImpl = apply {
        name = Name(text)
    }

    override fun setName(name: Name?): ItemListBuilderImpl = apply {
        this.name = name
    }

    override fun clearName(): ItemListBuilderImpl = apply { name = null }
}

private class ItemListImpl(
    override val identifier: String?,
    override val name: Name?,
    override val itemListElements: List<ItemListElement>
) : ItemList {
    override fun toBuilder(): ItemList.Builder<*> =
        ItemListBuilderImpl()
            .setIdentifier(identifier)
            .setName(name)
            .addItemListElements(itemListElements)
}