
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
package androidx.contentaccess

import kotlin.reflect.KClass

/**
 * Annotates a method that accesses a content provider.
 *
 * @property projection optional single field to query, otherwise queried fields are inferred from
 * return type. If this is specified and but the return type is a POJO, then that will result in
 * an error.
 *
 * @property selection The matching conditions, if empty applies to all (e.g "column1 = :value").
 *
 * @property orderBy The entity fields to order by, passed to the content provider in the
 * specified order. An entry in the array can either a single column name, or a single column
 * name followed by "asc" or "desc". (e.g order by = arrayOf("column1", "column2 asc")).
 *
 * @property uri The string representation of the uri to query, if empty then uses the entity's uri,
 * if existing.
 */

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ContentQuery(
    val projection: Array<String> = arrayOf(),
    val selection: String = "",
    val orderBy: Array<String> = arrayOf(),
    val uri: String = "",
    val contentEntity: KClass<*> = Void::class
)
