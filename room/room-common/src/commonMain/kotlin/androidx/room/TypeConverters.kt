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

package androidx.room

import kotlin.reflect.KClass

/**
 * Specifies additional type converters that Room can use. The TypeConverter is added to the scope
 * of the element so if you put it on a class / interface, all methods / fields in that class will
 * be able to use the converters.
 *
 * TypeConverters can only be used to convert columns / fields, hence cannot be used by a method
 * with a row return value such as DAO methods that query rows.
 *
 * * If you put it on a [Database], all Daos and Entities in that database will be able to
 * use it.
 * * If you put it on a [Dao], all methods in the Dao will be able to use it.
 * * If you put it on an [Entity], all fields of the Entity will be able to use it.
 * * If you put it on a POJO, all fields of the POJO will be able to use it.
 * * If you put it on an [Entity] field, only that field will be able to use it.
 * * If you put it on a [Dao] method, all parameters of the method will be able to use it.
 * * If you put it on a [Dao] method parameter, just that field will be able to use it.
 *
 * @see [TypeConverter]
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.BINARY)
public annotation class TypeConverters(
    /**
     * The list of type converter classes. If converter methods are not static, Room will create
     * an instance of these classes.
     *
     * @return The list of classes that contains the converter methods.
     */
    vararg val value: KClass<*> = [],

    /**
     * Configure whether Room can use various built in converters for common types.
     * See [BuiltInTypeConverters] for details.
     */
    val builtInTypeConverters: BuiltInTypeConverters = BuiltInTypeConverters()
)
