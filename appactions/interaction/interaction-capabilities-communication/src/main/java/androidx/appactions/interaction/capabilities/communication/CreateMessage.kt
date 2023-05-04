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

import androidx.appactions.builtintypes.experimental.properties.Recipient
import androidx.appactions.builtintypes.experimental.types.GenericErrorStatus
import androidx.appactions.builtintypes.experimental.types.Message
import androidx.appactions.builtintypes.experimental.types.SuccessStatus
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.MESSAGE_TYPE_SPEC
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.RECIPIENT_TYPE_SPEC
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import java.util.Optional

private const val CAPABILITY_NAME: String = "actions.intent.CREATE_MESSAGE"

/** A capability corresponding to actions.intent.CREATE_MESSAGE */
@CapabilityFactory(name = CAPABILITY_NAME)
class CreateMessage private constructor() {
    internal enum class PropertyMapStrings(val key: String) {
        MESSAGE_TEXT("message.text"),
        RECIPIENT("message.recipient"),
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder, Arguments, Output, Confirmation, ExecutionSession
            >(ACTION_SPEC) {

        private var properties = mutableMapOf<String, Property<*>>()

        fun setMessageText(messageText: Property<StringValue>): CapabilityBuilder = apply {
            properties[PropertyMapStrings.MESSAGE_TEXT.key] = messageText
        }

        fun setRecipient(recipient: Property<Recipient>): CapabilityBuilder = apply {
            properties[PropertyMapStrings.RECIPIENT.key] = recipient
        }

        override fun build(): Capability {
            super.setProperty(properties)
            return super.build()
        }
    }

    class Arguments
    internal constructor(val recipientList: List<RecipientValue>, val messageText: String?) {
        override fun toString(): String {
            return "Arguments(recipient=$recipientList, messageTextList=$messageText)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (recipientList != other.recipientList) return false
            if (messageText != other.messageText) return false

            return true
        }

        override fun hashCode(): Int {
            var result = recipientList.hashCode()
            result = 31 * result + messageText.hashCode()
            return result
        }

        class Builder : BuilderOf<Arguments> {
            private var recipientList: List<RecipientValue> = mutableListOf()
            private var messageText: String? = null

            fun setRecipientList(recipientList: List<RecipientValue>): Builder = apply {
                this.recipientList = recipientList
            }

            fun setMessageText(messageTextList: String): Builder = apply {
                this.messageText = messageTextList
            }

            override fun build(): Arguments = Arguments(recipientList, messageText)
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

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>

    companion object {
        @Suppress("UNCHECKED_CAST")
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder)
                .setOutput(Output::class.java)
                .bindRepeatedParameter(
                    "message.recipient",
                    { properties ->
                        Optional.ofNullable(
                            properties[PropertyMapStrings.RECIPIENT.key]
                                as Property<Recipient>
                        )
                    },
                    Arguments.Builder::setRecipientList,
                    RecipientValue.PARAM_VALUE_CONVERTER,
                    EntityConverter.of(RECIPIENT_TYPE_SPEC)
                )
                .bindOptionalParameter(
                    "message.text",
                    { properties ->
                        Optional.ofNullable(
                            properties[PropertyMapStrings.MESSAGE_TEXT.key]
                                as Property<StringValue>
                        )
                    },
                    Arguments.Builder::setMessageText,
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
                    ExecutionStatus::toParamValue
                )
                .build()
    }
}
