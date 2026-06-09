/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room3

import kotlin.reflect.KClass

/**
 * Registers additional [ColumnTypeConverter] functions for Room.
 *
 * Adds converter functions to the scope of the annotated element:
 * * [Database]: available to all DAOs and entities in the database.
 * * [Dao]: available to all functions in the DAO.
 * * [Entity]: available to all properties of the entity.
 * * Data class: available to all properties of the data class.
 * * [Entity] property: available only to that property.
 * * [Dao] function: available to all parameters of the function.
 * * [Dao] function parameter: available only to that parameter.
 *
 * Note: `@ColumnTypeConverters` only converts column and parameter values. To convert a DAO
 * function return value (e.g., query result transformation), use [DaoReturnTypeConverters].
 *
 * @see [ColumnTypeConverter]
 * @see [ProvidedColumnTypeConverter]
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS,
)
@Retention(AnnotationRetention.BINARY)
public annotation class ColumnTypeConverters(
    /**
     * Classes containing type converter functions.
     *
     * If a converter class is not an `object`, Room creates an instance of the class.
     */
    vararg val value: KClass<*> = [],

    /** Configuration for built-in type converters. */
    val builtInColumnTypeConverters: BuiltInColumnTypeConverters = BuiltInColumnTypeConverters(),
)
