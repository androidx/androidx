/*
 * Copyright 2020 The Android Open Source Project
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
 * When present, {@link RewriteQueriesToDropUnusedColumns} annotation will cause Room to
 * rewrite your {@link Query} methods such that only the columns that are used in the response are
 * queried from the database.
 * <p>
 * This annotation is useful if you don't need all columns returned in a query but also don't
 * want to spell out their names in the query projection.
 * <p>
 * For example, if you have a {@code User} class with 10 fields and want to return only
 * the {@code name} and {@code lastName} fields in a POJO, you could write the query like this:
 *
 * <pre>
 * {@literal @}Dao
 * interface MyDao {
 *     {@literal @Query("SELECT * FROM User")}
 *     {@literal public List<NameAndLastName> getAll();}
 * }
 * class NameAndLastName {
 *     public String name;
 *     public String lastName;
 * }
 * </pre>
 * <p>
 * Normally, Room would print a {@link RoomWarnings#CURSOR_MISMATCH} warning since the query result
 * has additional columns that are not used in the response. You can annotate the method with
 * {@link RewriteQueriesToDropUnusedColumns} to inform Room to rewrite your query at compile time to
 * avoid fetching extra columns.
 * <pre>
 * {@literal @}Dao
 * interface MyDao {
 *     {@literal @RewriteQueriesToDropUnusedColumns}
 *     {@literal @Query("SELECT * FROM User")}
 *     {@literal public List<NameAndLastName> getAll();}
 * }
 * </pre>
 * At compile time, Room will convert this query to {@code SELECT name, lastName FROM (SELECT *
 * FROM User)} which gets flattened by <b>Sqlite</b> to {@code SELECT name, lastName FROM User}.
 * <p>
 * When the annotation is used on a {@link Dao} method annotated with {@link Query}, it will only
 * affect that query. You can put the annotation on the {@link Dao} annotated class/interface or
 * the {@link Database} annotated class where it will impact all methods in the dao / database
 * respectively.
 * <p>
 * Note that Room will not rewrite the query if it has multiple columns that have the same name as
 * it does not yet have a way to distinguish which one is necessary.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface RewriteQueriesToDropUnusedColumns {
}
