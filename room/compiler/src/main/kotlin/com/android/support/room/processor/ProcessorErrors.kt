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

import com.android.support.room.Insert
import com.android.support.room.Query
import com.android.support.room.vo.Field

object ProcessorErrors {
    val MISSING_QUERY_ANNOTATION = "Query methods must be annotated with ${Query::class.java}"
    val MISSING_INSERT_ANNOTATION = "Insertion methods must be annotated with ${Insert::class.java}"
    val INVALID_ON_CONFLICT_VALUE = "On conflict value must be one of Insert.OnConflict values."
    val INVALID_INSERTION_METHOD_RETURN_TYPE = "Methods annotated with @Insert can return either" +
            " void, long, long[] or List<Long>."
    val ABSTRACT_METHOD_IN_DAO_MISSING_ANY_ANNOTATION = "Abstract method in DAO must be annotated" +
            " with ${Query::class.java} AND ${Insert::class.java}"
    val CANNOT_USE_BOTH_QUERY_AND_INSERT = "A method cannot be annotated with both " +
            " ${Query::class.java} AND ${Insert::class.java}"
    val CANNOT_RESOLVE_RETURN_TYPE = "Cannot resolve return type for %s"
    val CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS = "Cannot use unbound generics in query" +
            " methods. It must be bound to a type through base Dao class."
    val CANNOT_USE_UNBOUND_GENERICS_IN_INSERTION_METHODS = "Cannot use unbound generics in" +
            " insertion methods. It must be bound to a type through base Dao class."
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
    val DAO_MUST_BE_ANNOTATED_WITH_DAO = "Dao class must be annotated with @Dao"
    val ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY = "Entity class must be annotated with @Entity"
    val DATABASE_ANNOTATION_MUST_HAVE_LIST_OF_ENTITIES = "@Database annotation must specify list" +
            " of entities"
    val COLUMN_NAME_CANNOT_BE_EMPTY = "Column name cannot be blank. If you don't want to set it" +
            ", just remove the @ColumnName annotation."

    val ENTITY_TABLE_NAME_CANNOT_BE_EMPTY = "Entity table name cannot be blank. If you don't want" +
            " to set it, just remove the tableName property."

    val CANNOT_CONVERT_QUERY_PARAMETER_TO_STRING = "Query method parameters should either be a" +
            " type that can be converted into String or a List / Array that contains such type."

    val QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE = "Query/Insert method parameters cannot " +
            "start with underscore (_)."

    val CANNOT_FIND_QUERY_RESULT_ADAPTER = "Not sure how to convert a Cursor to this method's " +
            "return type"

    val INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT = "Method annotated with" +
            " @Insert but does not have any parameters to insert."

    val INSERTION_METHOD_PARAMETERS_MUST_HAVE_THE_SAME_ENTITY_TYPE = "Parameter types in " +
            "insertion methods must be the same type. If you want to insert entities from " +
            "different types atomically, use a transaction."

    val CANNOT_FIND_ENTITY_FOR_INSERT_PARAMETER = "Cannot find the entity type for the" +
            " insert parameter."

    private val TOO_MANY_MATCHING_GETTERS = "Ambiguous getter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."

    fun tooManyMatchingGetters(field: Field, methodNames: List<String>): String {
        return TOO_MANY_MATCHING_GETTERS.format(field, methodNames.joinToString(", "))
    }

    private val TOO_MANY_MATCHING_SETTERS = "Ambiguous setter for %s. All of the following " +
            "match: %s. You can @Ignore the ones that you don't want to match."

    fun tooManyMatchingSetter(field: Field, methodNames: List<String>): String {
        return TOO_MANY_MATCHING_SETTERS.format(field, methodNames.joinToString(", "))
    }

    private val MISSING_PARAMETER_FOR_BIND = "Each bind variable in the query must have a" +
            " matching method parameter. Cannot find method parameters for %s."

    fun missingParameterForBindVariable(bindVarName: List<String>): String {
        return MISSING_PARAMETER_FOR_BIND.format(bindVarName.joinToString(", "))
    }

    private val UNUSED_QUERY_METHOD_PARAMETER = "Unused parameter%s: %s"
    fun unusedQueryMethodParameter(unusedParams: List<String>): String {
        return UNUSED_QUERY_METHOD_PARAMETER.format(
                if (unusedParams.size > 1) "s" else "",
                unusedParams.joinToString(","))
    }
}
