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
import androidx.appactions.builtintypes.experimental.properties.Recipient
import androidx.appactions.builtintypes.experimental.properties.Text

interface Message : Thing, CreativeWork {
    val recipientList: List<Recipient>
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = MessageBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self>, CreativeWork.Builder<Self> {
        fun addRecipient(person: Person): Self
        fun addRecipient(recipient: Recipient): Self
        fun addRecipients(value: List<Recipient>): Self

        override fun build(): Message
    }
}

private class MessageBuilderImpl : Message.Builder<MessageBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null
    private var text: Text? = null
    private var recipientList = mutableListOf<Recipient>()

    override fun build() = MessageImpl(identifier, name, text, recipientList.toList())

    override fun addRecipient(person: Person): MessageBuilderImpl = apply {
        recipientList.add(Recipient(person))
    }

    override fun addRecipient(recipient: Recipient): MessageBuilderImpl = apply {
        recipientList.add(recipient)
    }

    override fun addRecipients(value: List<Recipient>): MessageBuilderImpl = apply {
        recipientList.addAll(value)
    }

    override fun setIdentifier(text: String?): MessageBuilderImpl = apply { identifier = text }

    override fun setName(text: String): MessageBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): MessageBuilderImpl = apply { this.name = name }

    override fun clearName(): MessageBuilderImpl = apply { name = null }

    override fun setText(text: Text?): MessageBuilderImpl = apply { this.text = text }

    override fun setText(text: String): MessageBuilderImpl = apply { this.text = Text(text) }
}

private class MessageImpl(
    override val identifier: String?,
    override val name: Name?,
    override val text: Text?,
    override val recipientList: List<Recipient>
) : Message {
    override fun toBuilder(): Message.Builder<*> =
        MessageBuilderImpl()
            .setIdentifier(identifier)
            .setName(name)
            .setText(text)
            .addRecipients(recipientList)
}