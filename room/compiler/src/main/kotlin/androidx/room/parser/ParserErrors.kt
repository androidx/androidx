/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.parser

object ParserErrors {
    val ANONYMOUS_BIND_ARGUMENT = "Room does not support ? as bind parameters. You must use" +
            " named bind arguments (e..g :argName)"

    val NOT_ONE_QUERY = "Must have exactly 1 query in @Query value"

    fun invalidQueryType(type: QueryType): String {
        return "$type query type is not supported yet. You can use:" +
                QueryType.SUPPORTED.joinToString(", ") { it.name }
    }

    fun cannotUseVariableIndices(name: String, position: Int) = "Cannot use variable indices." +
            " Use named parameters instead (e.g. WHERE name LIKE :nameArg and lastName LIKE " +
            ":lastName). Problem: $name at $position"
}
