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

package androidx.appactions.interaction.capabilities.core.testing.spec

import androidx.appactions.builtintypes.experimental.types.ListItem
import androidx.appactions.interaction.capabilities.core.AppEntityListener
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import java.util.Optional

private const val CAPABILITY_NAME = "actions.intent.TEST"

/** Used to test the filling behavior of structured entities (e.g. ListItem)  */
class CapabilityStructFill {

    class Arguments internal constructor(
        val listItem: ListItem?,
        val anyString: String?,
    ) {
        override fun toString(): String {
            return "Arguments(listItem=$listItem, " +
                "anyString=$anyString)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (listItem != other.listItem) return false
            if (anyString != other.anyString) return false
            return true
        }

        override fun hashCode(): Int {
            var result = listItem.hashCode()
            result += 31 * anyString.hashCode()
            return result
        }

        class Builder : BuilderOf<Arguments> {
            private var listItem: ListItem? = null
            private var anyString: String? = null

            fun setListItem(listItem: ListItem): Builder =
                apply { this.listItem = listItem }

            fun setAnyString(stringSlotB: String): Builder =
                apply { this.anyString = stringSlotB }

            override fun build(): Arguments = Arguments(listItem, anyString)
        }
    }

    interface ExecutionSession : BaseExecutionSession<Arguments, Void> {
        val listItemListener: AppEntityListener<ListItem>
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val ACTION_SPEC = ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
            .setArguments(Arguments::class.java, Arguments::Builder)
            .bindOptionalParameter(
                "listItem",
                { properties ->
                    Optional.ofNullable(properties["listItem"] as Property<ListItem>)
                },
                Arguments.Builder::setListItem,
                ParamValueConverter.of(TypeConverters.LIST_ITEM_TYPE_SPEC),
                EntityConverter.of(TypeConverters.LIST_ITEM_TYPE_SPEC)::convert
            )
            .bindOptionalParameter(
                "string",
                { properties ->
                    Optional.ofNullable(properties["anyString"] as Property<StringValue>)
                },
                Arguments.Builder::setAnyString,
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
            .build()
    }
}
