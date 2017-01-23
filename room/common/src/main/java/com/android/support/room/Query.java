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

package com.android.support.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Dao} annotated class as a query method.
 * <p>
 * The value of the annotation includes the query that will be run when this method is called.
 * <p>
 * The arguments of the method will be bound to the bind arguments in the SQL statement. See
 * https://www.sqlite.org/c3ref/bind_blob.html for details of bind arguments in SQLite.
 * <p>
 * Room supports only 2 types of bind arguments. {@code ?} placeholder and {@code :name}
 * named bind parameter.
 * If there are more than 1 argument, you must use named bind parameters to avoid confusion.
 * <p>
 * Room will automatically bind the parameters of the method into the bind arguments. When named
 * bind arguments are used, this is done by matching the name of the parameters to the name of the
 * bind arguments.
 * <pre>
 *     {@literal @}Query("SELECT * FROM user WHERE user_name LIKE :name AND last_name LIKE :last")
 *     public abstract List&lt;User&gt; findUsersByNameAndLastName(String name, String last);
 * </pre>
 * <p>
 * As an extension over SQLite bind arguments, Room supports binding a list of parameters to the
 * query. At runtime, Room will build the correct query to have matching number of bind arguments
 * depending on the number of items in the method parameter.
 * <pre>
 *     {@literal @}Query("SELECT * FROM user WHERE uid IN(?)")
 *     public abstract List&lt;User&gt; findByIds(int[] userIds);
 * </pre>
 * <p>
 * There are 3 types of queries supported in {@code Query} methods: SELECT, UPDATE and DELETE.
 * <p>
 * For SELECT queries, Room will infer the result contents from the method's return type and
 * generate the code that will automatically convert the query result into the method's return
 * type.
 * <p>
 * UPDATE or DELETE queries can return {@code void} or {@code int}. If it is an {@code int},
 * the value is the number of rows affected by this query.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Query {
    String value();
}
