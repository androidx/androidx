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
 * Specifies additional type converters that Room can use. The [TypeConverter] functions are added
 * to the scope of the element so if you put it on a class / interface, all functions / properties
 * in that class will be able to use the converters.
 *
 * `@TypeConverters` can only be used to convert columns / properties, hence cannot be used by a
 * function to convert a row return value such as DAO function that query rows, for such cases use
 * [DaoReturnTypeConverters].
 * * If you put it on a [Database], all DAOs and entities in that database will be able to use it.
 * * If you put it on a [Dao], all functions in the DAO will be able to use it.
 * * If you put it on an [Entity], all properties of the entity will be able to use it.
 * * If you put it on a data object class, all properties of the data class will be able to use it.
 * * If you put it on an [Entity] property, only that property will be able to use it.
 * * If you put it on a [Dao] function, all parameters of the function will be able to use it.
 * * If you put it on a [Dao] function parameter, just that property will be able to use it.
 *
 * @see [TypeConverter]
 * @see [ProvidedTypeConverter]
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS,
)
@Retention(AnnotationRetention.BINARY)
public annotation class TypeConverters(
    /**
     * The list of type converter classes. If converter class is not an `object` Room will create an
     * instance of these classes.
     *
     * @return The list of classes that contains the converter functions.
     */
    vararg val value: KClass<*> = [],

    /**
     * Configure whether Room can use various built in converters for common types. See
     * [BuiltInTypeConverters] for details.
     */
    val builtInTypeConverters: BuiltInTypeConverters = BuiltInTypeConverters(),
)
