
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

/**
 * Annotates a method that accesses a content provider.
 *
 * Both the updates and selection parameters are ignored if the method takes as input the entity
 * as that entity will be updated instead.
 *
 * @property query The entity fields to updates (e.g arrayOf("column1 = :value1",
 *     "column2 = :value2").
 *
 * @property selection The matching conditions, if empty applies to all (e.g "column1 = :value").
 *
 * @property uri The string representation of the uri to query, if empty then uses the entity's uri,
 * if existing.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ContentUpdate(
    val updates: Array<String>,
    val selection: String,
    val uri: String
)
