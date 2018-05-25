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

object NavParserErrors {
    val UNNAMED_DESTINATION = "Destination with arguments or actions must have " +
        "'name' or 'id' attributes."

    fun invalidDefaultValueReference(value: String) = "Failed to parse defaultValue " +
        "'$value' as reference. Reference must be in format @[+][package:]res_type/resource_name"

    fun invalidDefaultValue(value: String, type: NavType) = "Failed to parse defaultValue " +
        "'$value' as $type"

    fun invalidId(value: String) = "Failed to parse $value as id. 'id' must be in the format:" +
        " @[+][package:]id/resource_name "

    fun unknownType(type: String?) = "Unknown type '$type'"
}