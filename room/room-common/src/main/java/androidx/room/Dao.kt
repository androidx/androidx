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

package androidx.room

/**
 * Marks the class as a Data Access Object.
 *
 * Data Access Objects are the main classes where you define your database interactions. They can
 * include a variety of query methods.
 *
 * The class marked with `@Dao` should either be an interface or an abstract class. At compile
 * time, Room will generate an implementation of this class when it is referenced by a
 * [Database].
 *
 * An abstract `@Dao` class can optionally have a constructor that takes a [Database]
 * as its only parameter.
 *
 * It is recommended to have multiple `Dao` classes in your codebase depending on the tables
 * they touch.
 *
 * @see [Query]
 * @see [Delete]
 * @see [Insert]
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Dao
