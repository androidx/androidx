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
            "queryMethods. It must be bound to a type through base Dao class."
    val CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS = "Cannot use unbound fields in entities."
    val CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES = "Cannot use unbound generics in Dao classes." +
            " If you are trying to create a base DAO, create a normal class, extend it with type" +
            " params then mark the subclass with @Dao."
    val CANNOT_FIND_GETTER_FOR_FIELD = "Cannot find getter for field."
    val CANNOT_FIND_SETTER_FOR_FIELD = "Cannot find setter for field."
    val MISSING_PRIMARY_KEY = "An entity must have at least 1 field annotated with @PrimaryKey"
    val DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE = "Dao class must be an abstract class or" +
            " an interface"
    val DATABASE_MUST_BE_ANNOTATED_WITH_DATABASE = "Database must be annotated with @Database"
    private val TOO_MANY_MATCHING_GETTERS = "Ambiguous getter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."
    private val TOO_MANY_MATCHING_SETTERS = "Ambiguous setter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."
    val DAO_MUST_BE_ANNOTATED_WITH_DAO = "Dao class must be annotated with @Dao"
    val ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY = "Entity class must be annotated with @Entity"
    val DATABASE_ANNOTATION_MUST_HAVE_LIST_OF_ENTITIES = "@Database annotation must specify list" +
            " of entities"
    val COLUMN_NAME_CANNOT_BE_EMPTY = "Column name cannot be blank. If you don't want to set it" +
            ", just remove the @ColumnName annotation."

    val ENTITY_TABLE_NAME_CANNOT_BE_EMPTY = "Entity table name cannot be blank. If you don't want" +
            " to set it, just remove the tableName property."

    fun tooManyMatchingGetters(field : Field, methodNames : List<String>) : String {
        return TOO_MANY_MATCHING_GETTERS.format(field, methodNames.joinToString(", "))
    }

    fun tooManyMatchingSetter(field: Field, methodNames: List<String>) : String {
        return TOO_MANY_MATCHING_SETTERS.format(field, methodNames.joinToString(", "))
    }
}
