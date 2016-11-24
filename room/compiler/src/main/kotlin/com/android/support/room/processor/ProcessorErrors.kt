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

package com.android.support.room.processor

import com.android.support.room.Query
import com.android.support.room.vo.Field

object ProcessorErrors {
    val MISSING_QUERY_ANNOTATION = "Query methods must be annotated with ${Query::class.java}"
    val CANNOT_RESOLVE_RETURN_TYPE = "Cannot resolve return type for %s"
    val CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS = "Cannot use unbound generics in query " +
            "methods. It must be bound to a type through base Dao class."
    val CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS = "Cannot use unbound fields in entities."
    val CANNOT_FIND_GETTER_FOR_FIELD = "Cannot find getter for field."
    val CANNOT_FIND_SETTER_FOR_FIELD = "Cannot find setter for field."
    private val TOO_MANY_MATCHING_GETTERS = "Ambiguous getter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."
    private val TOO_MANY_MATCHING_SETTERS = "Ambiguous setter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."

    fun tooManyMatchingGetters(field : Field, methodNames : List<String>) : String {
        return TOO_MANY_MATCHING_GETTERS.format(field, methodNames.joinToString(", "))
    }

    fun tooManyMatchingSetter(field: Field, methodNames: List<String>) : String {
        return TOO_MANY_MATCHING_SETTERS.format(field, methodNames.joinToString(", "))
    }
}
