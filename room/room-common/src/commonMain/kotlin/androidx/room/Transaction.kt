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

/**
 * Marks a method in a [Dao] class as a transaction method.
 *
 * When used on a non-abstract method of an abstract [Dao] class,
 * the derived implementation of the method will execute the super method in a database transaction.
 * All the parameters and return types are preserved. The transaction will be marked as successful
 * unless an exception is thrown in the method body.
 *
 * Example:
 *
 * ```
 * @Dao
 * abstract class SongDao {
 *     @Insert
 *     abstract fun insert(song: Song)
 *     @Delete
 *     abstract fun delete(song: Song)
 *     @Transaction
 *     fun insertAndDeleteInTransaction(newSong: Song, oldSong: Song) {
 *         // Anything inside this method runs in a single transaction.
 *         insert(newSong)
 *         delete(oldSong)
 *     }
 * }
 * ```
 *
 * When used on a [Query] method that has a `SELECT` statement, the generated code for
 * the [Query] will be run in a transaction. There are 2 main cases where you may want to do that:
 *
 * * If the result of the query is fairly big, it is better to run it inside a transaction
 * to receive a consistent result. Otherwise, if the query result does not fit into a single
 * [android.database.CursorWindow], the query result may be corrupted due to changes in the database
 * in between cursor window swaps.
 *
 * * If the result of the query is a POJO with {@link Relation} fields, these fields are
 * queried separately. To receive consistent results between these queries, you also want
 * to run them in a single transaction.
 *
 * Example:
 *
 * ```
 * data class AlbumWithSongs : Album (
 *     @Relation(parentColumn = "albumId", entityColumn = "songId")
 *     val songs: List<Song>
 * )
 *
 * @Dao
 * public interface AlbumDao {
 *     @Transaction
 *     @Query("SELECT * FROM album")
 *     fun loadAll(): List<AlbumWithSongs>
 * }
 * ```
 *
 * If the query is asynchronous (e.g. returns a [androidx.lifecycle.LiveData]
 * or RxJava [Flowable]), the transaction is properly handled when the query is run, not when
 * the method is called.
 *
 * Putting this annotation on an [Insert], [Update] or [Delete] method has no
 * impact because those methods are always run inside a transaction. Similarly, if a method is
 * annotated with [Query] but runs an INSERT, UPDATE or DELETE statement, it is automatically
 * wrapped in a transaction and this annotation has no effect. Room will only perform at most one
 * transaction at a time, additional transactions are queued
 * and executed on a first come, first serve order.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Transaction
