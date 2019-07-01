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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Dao} class as a transaction method.
 * <p>
 * When used on a non-abstract method of an abstract {@link Dao} class,
 * the derived implementation of the method will execute the super method in a database transaction.
 * All the parameters and return types are preserved. The transaction will be marked as successful
 * unless an exception is thrown in the method body.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Dao
 * public abstract class SongDao {
 *   {@literal @}Insert
 *   public abstract void insert(Song song);
 *   {@literal @}Delete
 *   public abstract void delete(Song song);
 *   {@literal @}Transaction
 *   public void insertAndDeleteInTransaction(Song newSong, Song oldSong) {
 *     // Anything inside this method runs in a single transaction.
 *     insert(newSong);
 *     delete(oldSong);
 *   }
 * }
 * </pre>
 * <p>
 * When used on a {@link Query} method that has a {@code SELECT} statement, the generated code for
 * the Query will be run in a transaction. There are 2 main cases where you may want to do that:
 * <ol>
 *     <li>If the result of the query is fairly big, it is better to run it inside a transaction
 *     to receive a consistent result. Otherwise, if the query result does not fit into a single
 *     {@link android.database.CursorWindow CursorWindow}, the query result may be corrupted due to
 *     changes in the database in between cursor window swaps.
 *     <li>If the result of the query is a POJO with {@link Relation} fields, these fields are
 *     queried separately. To receive consistent results between these queries, you also want
 *     to run them in a single transaction.
 * </ol>
 * Example:
 * <pre>
 * class AlbumWithSongs extends Album {
 *     {@literal @}Relation(parentColumn = "albumId", entityColumn = "songId")
 *     public List&lt;Song&gt; songs;
 * }
 *
 * {@literal @}Dao
 * public interface AlbumDao {
 *     {@literal @}Transaction {@literal @}Query("SELECT * FROM album")
 *     public List&lt;AlbumWithSongs&gt; loadAll();
 * }
 * </pre>
 * If the query is asynchronous (e.g. returns a {@link androidx.lifecycle.LiveData LiveData}
 * or RxJava {@code Flowable}), the transaction is properly handled when the query is run, not when
 * the method is called.
 * <p>
 * Putting this annotation on an {@link Insert}, {@link Update} or {@link Delete} method has no
 * impact because those methods are always run inside a transaction. Similarly, if a method is
 * annotated with {@link Query} but runs an INSERT, UPDATE or DELETE statement, it is automatically
 * wrapped in a transaction and this annotation has no effect.
 * <p>
 * Room will only perform at most one transaction at a time, additional transactions are queued
 * and executed on a first come, first serve order.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface Transaction {
}
