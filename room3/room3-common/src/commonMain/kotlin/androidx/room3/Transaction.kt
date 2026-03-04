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

/**
 * Marks a function in a [Dao] class as a transaction function.
 *
 * When used on a non-abstract function of an abstract [Dao] class, the derived implementation of
 * the function will execute the super function in a database transaction. All the parameters and
 * return types are preserved. The transaction will be marked as successful unless an exception is
 * thrown in the function body.
 *
 * Example:
 * ```
 * @Dao
 * abstract class SongDao {
 *     @Insert
 *     abstract suspend fun insert(song: Song)
 *
 *     @Delete
 *     abstract suspend fun delete(song: Song)
 *
 *     @Transaction
 *     suspend fun insertAndDeleteInTransaction(newSong: Song, oldSong: Song) {
 *         // Anything inside this function runs in a single transaction.
 *         insert(newSong)
 *         delete(oldSong)
 *     }
 * }
 * ```
 *
 * When used on a [Query] function that has a `SELECT` statement, the generated code for the [Query]
 * will be run in a transaction. There are 2 main cases where you may want to do that:
 * * If the result of the query is a data object class with [Relation] properties, these properties
 *   are queried separately. To receive consistent results between these queries, you also want to
 *   run them in a single transaction.
 * * If the result of the query is fairly big such that it creates implications with the
 *   [androidx.sqlite.SQLiteDriver] implementation provided to Room. Specifically for a driver that
 *   uses Android's SQLite binding it is better to run a query with a large result inside a
 *   transaction to receive a consistent results. Otherwise, if the query result does not fit into a
 *   single [android.database.CursorWindow], the query result may be corrupted due to changes in the
 *   database in between cursor window swaps.
 *
 * Example:
 * ```
 * data class AlbumWithSongs(
 *     val album: Album
 *     @Relation(parentColumn = "albumId", entityColumn = "songId")
 *     val songs: List<Song>
 * )
 *
 * @Dao
 * interface AlbumDao {
 *     @Transaction
 *     @Query("SELECT * FROM album")
 *     suspend fun loadAll(): List<AlbumWithSongs>
 * }
 * ```
 *
 * If the query is asynchronous (e.g. returns a [kotlinx.coroutines.flow.Flow]), the transaction is
 * properly handled when the query is run, not when the function is called.
 *
 * Putting this annotation on an [Insert], [Update] or [Delete] function has no impact because those
 * functions are always run inside a transaction. Similarly, if a function is annotated with [Query]
 * but executes an `INSERT`, `UPDATE` or `DELETE` statement, it is automatically wrapped in a
 * transaction and this annotation has no effect. For write statement Room will only perform at most
 * one transaction at a time, additional transactions are queued and executed on a first come, first
 * served order.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Transaction
