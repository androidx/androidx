/*
 * Copyright 2024 The Android Open Source Project
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
 * Defines a class that can instantiate the Room generated implementation of an 'abstract'
 * [Database] annotated [RoomDatabase] definition.
 *
 * This interface is to be used in conjunction with [ConstructedBy] to define an 'expect'
 * declaration of an 'object' that implements this interface. The defined 'object' can then be
 * optionally used in Room's `databaseBuilder` or `inMemoryDatabaseBuilder` as the `factory`.
 *
 * For example, with the following object definition:
 * ```
 * expect object MusicDatabaseConstructor : RoomDatabaseConstructor<MusicDatabase>
 * ```
 *
 * one can reference the object's [initialize] during database creation:
 * ```
 * fun createDatabase(): MusicDatabase {
 *     return Room.inMemoryDatabaseBuilder<MusicDatabase>(
 *         factory = MusicDatabaseConstructor::initialize
 *     ).build()
 * }
 * ```
 *
 * For Room to correctly and automatically use 'actual' implementations of this interface, they must
 * be linked to their respective [Database] definition via [ConstructedBy].
 *
 * @param T The [Database] and [ConstructedBy] annotated class linked to this constructor.
 * @see ConstructedBy
 */
interface RoomDatabaseConstructor<T : RoomDatabase> {
    /**
     * Instantiates an implementation of [T].
     *
     * @return T - A new instance of [T].
     */
    fun initialize(): T
}
