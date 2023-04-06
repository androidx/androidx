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

package androidx.appactions.interaction.capabilities.communication

import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.CapabilityBuilderBase
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.MESSAGE_TYPE_SPEC
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.RECIPIENT_TYPE_SPEC
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty
import androidx.appactions.interaction.capabilities.core.values.GenericErrorStatus
import androidx.appactions.interaction.capabilities.core.values.Message
import androidx.appactions.interaction.capabilities.core.values.properties.Recipient
import androidx.appactions.interaction.capabilities.core.values.SuccessStatus
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import java.util.Optional

private const val CAPABILITY_NAME: String = "actions.intent.CREATE_MESSAGE"

private val ACTION_SPEC =
    ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
        .setDescriptor(CreateMessage.Property::class.java)
        .setArgument(CreateMessage.Argument::class.java, CreateMessage.Argument::Builder)
        .setOutput(CreateMessage.Output::class.java)
        .bindRepeatedParameter(
            "message.recipient",
            { property -> Optional.ofNullable(property.recipient) },
            CreateMessage.Argument.Builder::setRecipientList,
            RecipientValue.PARAM_VALUE_CONVERTER,
            EntityConverter.of(RECIPIENT_TYPE_SPEC)
        )
        .bindOptionalParameter(
            "message.text",
            { property -> Optional.ofNullable(property.messageText) },
            CreateMessage.Argument.Builder::setMessageText,
            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )
        .bindOptionalOutput(
            "message",
            { output -> Optional.ofNullable(output.message) },
            ParamValueConverter.of(MESSAGE_TYPE_SPEC)::toParamValue
        )
        .bindOptionalOutput(
            "executionStatus",
            { output -> Optional.ofNullable(output.executionStatus) },
            CreateMessage.ExecutionStatus::toParamValue
        )
        .build()

@CapabilityFactory(name = CAPABILITY_NAME)
class CreateMessage private constructor() {
    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder, Property, Argument, Output, Confirmation, Session
        >(ACTION_SPEC) {
        override fun build(): Capability {
            super.setProperty(Property.Builder().build())
            // TODO(b/268369632): No-op remove empty property builder after Property is removed.
            super.setProperty(Property.Builder().build())
            return super.build()
        }
    }

    // TODO(b/268369632): Remove Property from public capability APIs.
    class Property
    internal constructor(
        val recipient: TypeProperty<Recipient>?,
        val messageText: TypeProperty<StringValue>?
    ) {
        override fun toString(): String {
            return "Property(recipient=$recipient, messageText=$messageText)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Property

            if (recipient != other.recipient) return false
            if (messageText != other.messageText) return false

            return true
        }

        override fun hashCode(): Int {
            var result = recipient.hashCode()
            result = 31 * result + messageText.hashCode()
            return result
        }

        class Builder {
            private var recipient: TypeProperty<Recipient>? = null
            private var messageText: TypeProperty<StringValue>? = null

            fun setRecipient(recipient: TypeProperty<Recipient>): Builder = apply {
                this.recipient = recipient
            }

            fun setMessageText(messageText: TypeProperty<StringValue>): Builder = apply {
                this.messageText = messageText
            }

            fun build(): Property = Property(recipient, messageText)
        }
    }

    class Argument
    internal constructor(val recipientList: List<RecipientValue>, val messageText: String?) {
        override fun toString(): String {
            return "Argument(recipient=$recipientList, messageTextList=$messageText)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Argument

            if (recipientList != other.recipientList) return false
            if (messageText != other.messageText) return false

            return true
        }

        override fun hashCode(): Int {
            var result = recipientList.hashCode()
            result = 31 * result + messageText.hashCode()
            return result
        }

        class Builder : BuilderOf<Argument> {
            private var recipientList: List<RecipientValue> = mutableListOf()
            private var messageText: String? = null

            fun setRecipientList(recipientList: List<RecipientValue>): Builder = apply {
                this.recipientList = recipientList
            }

            fun setMessageText(messageTextList: String): Builder = apply {
                this.messageText = messageTextList
            }

            override fun build(): Argument = Argument(recipientList, messageText)
        }
    }

    class Output
    internal constructor(val message: Message?, val executionStatus: ExecutionStatus?) {
        override fun toString(): String {
            return "Output(call=$message, executionStatus=$executionStatus)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Output

            if (message != other.message) return false
            if (executionStatus != other.executionStatus) return false

            return true
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + executionStatus.hashCode()
            return result
        }

        class Builder {
            private var message: Message? = null
            private var executionStatus: ExecutionStatus? = null

            fun setMessage(message: Message): Builder = apply { this.message = message }

            fun setExecutionStatus(executionStatus: ExecutionStatus): Builder = apply {
                this.executionStatus = executionStatus
            }

            fun build(): Output = Output(message, executionStatus)
        }
    }

    class ExecutionStatus {
        private var successStatus: SuccessStatus? = null
        private var genericErrorStatus: GenericErrorStatus? = null

        constructor(successStatus: SuccessStatus) {
            this.successStatus = successStatus
        }

        constructor(genericErrorStatus: GenericErrorStatus) {
            this.genericErrorStatus = genericErrorStatus
        }

        internal fun toParamValue(): ParamValue {
            var status = ""
            if (successStatus != null) {
                status = successStatus.toString()
            }
            if (genericErrorStatus != null) {
                status = genericErrorStatus.toString()
            }
            val value: Value = Value.newBuilder().setStringValue(status).build()
            return ParamValue.newBuilder()
                .setStructValue(
                    Struct.newBuilder().putFields(TypeConverters.FIELD_NAME_TYPE, value).build()
                )
                .build()
        }
    }

    class Confirmation internal constructor()

    sealed interface Session : BaseSession<Argument, Output>
}
