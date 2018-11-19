/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument

object NavParserErrors {
    val UNNAMED_DESTINATION = "Destination with arguments or actions must have " +
        "'name' or 'id' attributes."

    fun invalidDefaultValueReference(value: String) = "Failed to parse defaultValue " +
        "'$value' as reference. Reference must be in format @[+][package:]res_type/resource_name"

    fun invalidDefaultValue(value: String, type: NavType) = "Failed to parse defaultValue " +
        "'$value' as $type"

    fun invalidId(value: String) = "Failed to parse $value as id. 'id' must be in the format:" +
        " @[+][package:]id/resource_name "

    fun defaultValueObjectType(type: String?) = "'$type' " +
            "doesn't allow default values other than @null"

    fun defaultNullButNotNullable(name: String?) = "android:defaultValue is @null, but '$name' " +
            "is not nullable. Add app:nullable=\"true\" to the argument to make it nullable."

    fun typeIsNotNullable(typeName: String?) = "'$typeName' is a simple type " +
            "and cannot be nullable. Remove app:nullable=\"true\" from the argument."

    fun sameSanitizedNameArguments(sanitizedName: String, args: List<Argument>) =
            "Multiple same name arguments. The named arguments: " +
            "[${args.joinToString(", ") { it.name }}] result in the generator using " +
                    "the same name: '$sanitizedName'."

    fun sameSanitizedNameActions(sanitizedName: String, actions: List<Action>) =
            "Multiple same name actions. The action ids: " +
                    "[${actions.joinToString(", ") { it.id.name }}] result in the " +
                    "generator using the same name: '$sanitizedName'."

    fun deprecatedTypeAttrUsed(name: String) =
            "The 'type' attribute used by argument '$name' is deprecated. " +
                    "Please change all instances of 'type' in navigation resources to 'argType'."

    val MISSING_GRAPH_ATTR = "Missing 'graph' attribute in <include> tag."

    fun invalidNavReference(value: String) = "Failed to parse '$value' as a navigation reference." +
            " Reference must be in format @[package:]navigation/resource_name"
}