/*
 * Copyright 2025 The Android Open Source Project
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
 * Specifies the classes that contain **DAO return type converters** that a RoomDatabase can use.
 *
 * This annotation is used to register converter classes that contain methods annotated with
 * [DaoReturnTypeConverter] via annotating a [Database] or [Dao].
 *
 * All [Dao] methods in that database will be able to use the converters. These converters allow you
 * to wrap or transform the result of a DAO method into a custom Kotlin return type (e.g.,
 * `Result<MyEntity>`, `Result<List<MyEntity>>`).
 *
 * **Important Distinction from [TypeConverters]:**
 *
 * Unlike [TypeConverters], which convert any query result column to be converted to a
 * field/property regardless if the field or property is on an entity or a data object class for
 * storage, [DaoReturnTypeConverters] only applies to the **return value of a DAO method**. They are
 * designed specifically to intercept the *return value* of a query or the result of a write
 * operation.
 *
 * @see [DaoReturnTypeConverter]
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class DaoReturnTypeConverters(
    /**
     * The list of DAO return type converter classes. If converter methods are not static, Room will
     * create an instance of these classes.
     *
     * @return The list of classes that contains the converter methods.
     */
    vararg val value: KClass<*> = []
)
