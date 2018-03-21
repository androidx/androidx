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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an entity. This class will have a mapping SQLite table in the database.
 * <p>
 * Each entity must have at least 1 field annotated with {@link PrimaryKey}.
 * You can also use {@link #primaryKeys()} attribute to define the primary key.
 * <p>
 * Each entity must either have a no-arg constructor or a constructor whose parameters match
 * fields (based on type and name). Constructor does not have to receive all fields as parameters
 * but if a field is not passed into the constructor, it should either be public or have a public
 * setter. If a matching constructor is available, Room will always use it. If you don't want it
 * to use a constructor, you can annotate it with {@link Ignore}.
 * <p>
 * When a class is marked as an Entity, all of its fields are persisted. If you would like to
 * exclude some of its fields, you can mark them with {@link Ignore}.
 * <p>
 * If a field is {@code transient}, it is automatically ignored <b>unless</b> it is annotated with
 * {@link ColumnInfo}, {@link Embedded} or {@link Relation}.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Entity
 * public class User {
 *   {@literal @}PrimaryKey
 *   private final int uid;
 *   private String name;
 *   {@literal @}ColumnInfo(name = "last_name")
 *   private String lastName;
 *
 *   public User(int uid) {
 *       this.uid = uid;
 *   }
 *   public String getLastName() {
 *       return lastName;
 *   }
 *   public void setLastName(String lastName) {
 *       this.lastName = lastName;
 *   }
 * }
 * </pre>
 *
 * @see Dao
 * @see Database
 * @see PrimaryKey
 * @see ColumnInfo
 * @see Index
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Entity {
    /**
     * The table name in the SQLite database. If not set, defaults to the class name.
     *
     * @return The SQLite tableName of the Entity.
     */
    String tableName() default "";

    /**
     * List of indices on the table.
     *
     * @return The list of indices on the table.
     */
    Index[] indices() default {};

    /**
     * If set to {@code true}, any Index defined in parent classes of this class will be carried
     * over to the current {@code Entity}. Note that if you set this to {@code true}, even if the
     * {@code Entity} has a parent which sets this value to {@code false}, the {@code Entity} will
     * still inherit indices from it and its parents.
     * <p>
     * When the {@code Entity} inherits an index from the parent, it is <b>always</b> renamed with
     * the default naming schema since SQLite <b>does not</b> allow using the same index name in
     * multiple tables. See {@link Index} for the details of the default name.
     * <p>
     * By default, indices defined in parent classes are dropped to avoid unexpected indices.
     * When this happens, you will receive a {@link RoomWarnings#INDEX_FROM_PARENT_FIELD_IS_DROPPED}
     * or {@link RoomWarnings#INDEX_FROM_PARENT_IS_DROPPED} warning during compilation.
     *
     * @return True if indices from parent classes should be automatically inherited by this Entity,
     *         false otherwise. Defaults to false.
     */
    boolean inheritSuperIndices() default false;

    /**
     * The list of Primary Key column names.
     * <p>
     * If you would like to define an auto generated primary key, you can use {@link PrimaryKey}
     * annotation on the field with {@link PrimaryKey#autoGenerate()} set to {@code true}.
     *
     * @return The primary key of this Entity. Can be empty if the class has a field annotated
     * with {@link PrimaryKey}.
     */
    String[] primaryKeys() default {};

    /**
     * List of {@link ForeignKey} constraints on this entity.
     *
     * @return The list of {@link ForeignKey} constraints on this entity.
     */
    ForeignKey[] foreignKeys() default {};
}
