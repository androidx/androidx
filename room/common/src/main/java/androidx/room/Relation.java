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
 * A convenience annotation which can be used in a POJO to automatically fetch relation entities.
 * When the POJO is returned from a query, all of its relations are also fetched by Room.
 *
 * <pre>
 * {@literal @}Entity
 * public class Song {
 *     {@literal @} PrimaryKey
 *     int songId;
 *     int albumId;
 *     String name;
 *     // other fields
 * }
 * public class AlbumNameAndAllSongs {
 *     int id;
 *     String name;
 *     {@literal @}Relation(parentColumn = "id", entityColumn = "albumId")
 *     List&lt;Song&gt; songs;
 * }
 *
 * {@literal @}Dao
 * public interface MusicDao {
 *     {@literal @}Query("SELECT id, name FROM Album")
 *     List&lt;AlbumNameAndAllSongs&gt; loadAlbumAndSongs();
 * }
 * </pre>
 * <p>
 * For a one-to-many or many-to-many relationship, the type of the field annotated with
 * {@code Relation} must be a {@link java.util.List} or {@link java.util.Set}.
 * <p>
 * By default, the {@link Entity} type is inferred from the return type.
 * If you would like to return a different object, you can specify the {@link #entity()} property
 * in the annotation.
 * <pre>
 * public class Album {
 *     int id;
 *     // other fields
 * }
 * public class SongNameAndId {
 *     int songId;
 *     String name;
 * }
 * public class AlbumAllSongs {
 *     {@literal @}Embedded
 *     Album album;
 *     {@literal @}Relation(parentColumn = "id", entityColumn = "albumId", entity = Song.class)
 *     List&lt;SongNameAndId&gt; songs;
 * }
 * {@literal @}Dao
 * public interface MusicDao {
 *     {@literal @}Query("SELECT * from Album")
 *     List&lt;AlbumAllSongs&gt; loadAlbumAndSongs();
 * }
 * </pre>
 * <p>
 * In the example above, {@code SongNameAndId} is a regular POJO but all of fields are fetched
 * from the {@code entity} defined in the {@code @Relation} annotation (<i>Song</i>).
 * {@code SongNameAndId} could also define its own relations all of which would also be fetched
 * automatically.
 * <p>
 * If you would like to specify which columns are fetched from the child {@link Entity}, you can
 * use {@link #projection()} property in the {@code Relation} annotation.
 * <pre>
 * public class AlbumAndAllSongs {
 *     {@literal @}Embedded
 *     Album album;
 *     {@literal @}Relation(
 *             parentColumn = "id",
 *             entityColumn = "albumId",
 *             entity = Song.class,
 *             projection = {"name"})
 *     List&lt;String&gt; songNames;
 * }
 * </pre>
 * <p>
 * If the relationship is defined by an associative table (also know as junction table) then you can
 * use {@link #associateBy()} to specify it. This is useful for fetching many-to-many relations.
 * <p>
 * Note that {@code @Relation} annotation can be used only in POJO classes, an {@link Entity} class
 * cannot have relations. This is a design decision to avoid common pitfalls in {@link Entity}
 * setups. You can read more about it in the main
 * <href="https://developer.android.com/training/data-storage/room/referencing-data#understand-no-object-references">
 * Room documentation</>. When loading data, you can simply work around this limitation by creating
 * POJO classes that extend the {@link Entity}.
 *
 * @see Junction
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface Relation {
    /**
     * The entity or view to fetch the item from. You don't need to set this if the entity or view
     * matches the type argument in the return type.
     *
     * @return The entity or view to fetch from. By default, inherited from the return type.
     */
    Class<?> entity() default Object.class;

    /**
     * Reference column in the parent POJO.
     * <p>
     * In a one-to-one or one-to-many relation, this value will be matched against the column
     * defined in {@link #entityColumn()}. In a many-to-many using {@link #associateBy()} then
     * this value will be matched against the {@link Junction#parentColumn()}
     *
     * @return The column reference in the parent object.
     */
    String parentColumn();

    /**
     * The column to match in the {@link #entity()}.
     * <p>
     * In a one-to-one or one-to-many relation, this value will be matched against the column
     * defined in {@link #parentColumn()} ()}. In a many-to-many using {@link #associateBy()} then
     * this value will be matched against the {@link Junction#entityColumn()}
     */
    String entityColumn();

    /**
     * The entity or view to be used as a associative table (also known as a junction table) when
     * fetching the relating entities.
     *
     * @return The junction describing the associative table. By default, no junction is specified
     * and none will be used.
     *
     * @see Junction
     */
    Junction associateBy() default @Junction(Object.class);

    /**
     * If sub columns should be fetched from the entity, you can specify them using this field.
     * <p>
     * By default, inferred from the the return type.
     *
     * @return The list of columns to be selected from the {@link #entity()}.
     */
    String[] projection() default {};
}
