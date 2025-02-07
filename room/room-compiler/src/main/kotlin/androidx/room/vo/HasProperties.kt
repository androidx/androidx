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

package androidx.room.vo

interface HasProperties {
    val properties: Properties
}

// we need to make it class to enable caching (see columnNames by lazy), extension properties
// and functions don't have a way to store calculated value.
data class Properties(private val properties: List<Property> = emptyList()) :
    List<Property> by properties {
    constructor(property: Property) : this(listOf(property))

    internal val columnNames by lazy(LazyThreadSafetyMode.NONE) { map { it.columnName } }
}

val HasProperties.columnNames
    get() = properties.columnNames

fun HasProperties.findPropertyByColumnName(columnName: String) =
    properties.find { it.columnName == columnName }
