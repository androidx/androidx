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
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder

private const val CAPABILITY_NAME = "actions.intent.TEST"

/** Used to test the filling behavior of structured entities (e.g. ListItem)  */
class CapabilityStructFill {

    class Arguments internal constructor(
        val listItem: ListItem?,
        val string: String?
    ) {
        override fun toString(): String {
            return "Arguments(listItem=$listItem, " +
                "string=$string)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (listItem != other.listItem) return false
            if (string != other.string) return false
            return true
        }

        override fun hashCode(): Int {
            var result = listItem.hashCode()
            result += 31 * string.hashCode()
            return result
        }

        class Builder {
            private var listItem: ListItem? = null
            private var string: String? = null

            fun setListItem(listItem: ListItem): Builder =
                apply { this.listItem = listItem }

            fun setAnyString(stringSlotB: String): Builder =
                apply { this.string = stringSlotB }

            fun build(): Arguments = Arguments(listItem, string)
        }
    }

    class Output internal constructor()

    class Confirmation internal constructor()

    interface ExecutionSession : BaseExecutionSession<Arguments, Output> {
        val listItemListener: AppEntityListener<ListItem>
    }

    companion object {
        val ACTION_SPEC = ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
            .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
            .setOutput(Output::class.java)
            .bindParameter(
                "listItem",
                Arguments::listItem,
                Arguments.Builder::setListItem,
                ParamValueConverter.of(TypeConverters.LIST_ITEM_TYPE_SPEC)
            )
            .bindParameter(
                "string",
                Arguments::string,
                Arguments.Builder::setAnyString,
                TypeConverters.STRING_PARAM_VALUE_CONVERTER
            )
            .build()
    }
}
