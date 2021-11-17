/*
 * Copyright 2018 The Android Open Source Project
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
 * Marks a method in a {@link Dao} annotated class as a raw query method where you can pass the
 * query as a {@link androidx.sqlite.db.SupportSQLiteQuery SupportSQLiteQuery}.
 * <pre>
 * {@literal @}Dao
 * interface RawDao {
 *   {@literal @}RawQuery
 *   Song getSongViaQuery(SupportSQLiteQuery query);
 * }
 *
 * // Usage of RawDao
 * SimpleSQLiteQuery query = new SimpleSQLiteQuery(
 *     "SELECT * FROM Song WHERE id = ? LIMIT 1",
 *     new Object[]{ songId});
 * Song song = rawDao.getSongViaQuery(query);
 * </pre>
 * <p>
 * Room will generate the code based on the return type of the function and failure to
 * pass a proper query will result in a runtime failure or an undefined result.
 * <p>
 * If you know the query at compile time, you should always prefer {@link Query} since it validates
 * the query at compile time and also generates more efficient code since Room can compute the
 * query result at compile time (e.g. it does not need to account for possibly missing columns in
 * the response).
 * <p>
 * On the other hand, {@code RawQuery} serves as an escape hatch where you can build your own
 * SQL query at runtime but still use Room to convert it into objects.
 * <p>
 * {@code RawQuery} methods must return a non-void type. If you want to execute a raw query that
 * does not return any value, use {@link androidx.room.RoomDatabase#query
 * RoomDatabase#query} methods.
 * <p>
 * RawQuery methods can only be used for read queries. For write queries, use
 * {@link androidx.room.RoomDatabase#getOpenHelper
 * RoomDatabase.getOpenHelper().getWritableDatabase()}.
 * <p>
 * <b>Observable Queries:</b>
 * <p>
 * {@code RawQuery} methods can return observable types but you need to specify which tables are
 * accessed in the query using the {@link #observedEntities()} field in the annotation.
 * <pre>
 * {@literal @}Dao
 * interface RawDao {
 *   {@literal @}RawQuery(observedEntities = Song.class)
 *   LiveData&lt;List&lt;Song&gt;&gt; getSongs(SupportSQLiteQuery query);
 * }
 *
 * // Usage of RawDao
 * LiveData&lt;List&lt;Song&gt;&gt; liveSongs = rawDao.getSongs(
 *     new SimpleSQLiteQuery("SELECT * FROM song ORDER BY name DESC"));
 * </pre>
 * <b>Returning POJOs:</b>
 * <p>
 * RawQueries can also return plain old java objects, similar to {@link Query} methods.
 * <pre>
 * public class NameAndReleaseYear {
 *   final String name;
 *   {@literal @}ColumnInfo(name = "release_year")
 *   final int year;
 *
 *   public NameAndReleaseYear(String name, int year) {
 *     this.name = name;
 *     this.year = year;
 *   }
 * }
 *
 * {@literal @}Dao
 * interface RawDao {
 *   {@literal @}RawQuery
 *   NameAndReleaseYear getNameAndReleaseYear(SupportSQLiteQuery query);
 * }
 *
 * // Usage of RawDao
 * NameAndReleaseYear result = rawDao.getNameAndReleaseYear(
 *     new SimpleSQLiteQuery("SELECT * FROM song WHERE id = ?", new Object[]{songId}))
 * </pre>
 * <p>
 * <b>POJOs with Embedded Fields:</b>
 * <p>
 * {@code RawQuery} methods can return POJOs that include {@link Embedded} fields as well.
 * <pre>
 * public class SongAndArtist {
 *   {@literal @}Embedded
 *   public Song song;
 *   {@literal @}Embedded
 *   public Artist artist;
 * }
 *
 * {@literal @}Dao
 * interface RawDao {
 *   {@literal @}RawQuery
 *   SongAndArtist getSongAndArtist(SupportSQLiteQuery query);
 * }
 *
 * // Usage of RawDao
 * SongAndArtist result = rawDao.getSongAndArtist(
 *     new SimpleSQLiteQuery("SELECT * FROM Song, Artist WHERE Song.artistId = Artist.id LIMIT 1"))
 * </pre>
 *
 * <b>Relations:</b>
 * <p>
 * {@code RawQuery} return types can also be objects with {@link Relation Relations}.
 * <pre>
 * public class AlbumAndSongs {
 *     {@literal @}Embedded
 *     public Album album;
 *     {@literal @}Relation(parentColumn = "id", entityColumn = "albumId")
 *     public List&lt;Song&gt; pets;
 * }
 *
 * {@literal @}Dao
 * interface RawDao {
 *   {@literal @}RawQuery
 *   List&lt;AlbumAndSongs&gt; getAlbumAndSongs(SupportSQLiteQuery query);
 * }
 *
 * // Usage of RawDao
 * List&lt;AlbumAndSongs&gt; result = rawDao.getAlbumAndSongs(
 *     new SimpleSQLiteQuery("SELECT * FROM album"));
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RawQuery {
    /**
     * Denotes the list of entities which are accessed in the provided query and should be observed
     * for invalidation if the query is observable.
     * <p>
     * The listed classes should either be annotated with {@link Entity} or they should reference to
     * at least 1 Entity (via {@link Embedded} or {@link Relation}).
     * <p>
     * Providing this field in a non-observable query has no impact.
     * <pre>
     * {@literal @}Dao
     * interface RawDao {
     *   {@literal @}RawQuery(observedEntities = Song.class)
     *   LiveData&lt;List&lt;User&gt;&gt; getUsers(String query);
     * }
     * LiveData&lt;List&lt;Song&gt;&gt; liveSongs = rawDao.getUsers(
     *     "SELECT * FROM song ORDER BY name DESC");
     * </pre>
     *
     * @return List of entities that should invalidate the query if changed.
     */
    Class<?>[] observedEntities() default {};
}
