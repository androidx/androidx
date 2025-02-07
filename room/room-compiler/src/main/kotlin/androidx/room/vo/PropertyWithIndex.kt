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

package androidx.room.vo

/**
 * A common value object when we need to associate a Property with an Index variable.
 *
 * <p>
 * If we are sure that the property will be there at compile time, we set it to always Exists so
 * that the generated code does not check for -1 column indices.
 */
data class PropertyWithIndex(
    val property: Property,
    val indexVar: String,
    val alwaysExists: Boolean
) {
    companion object {
        fun byOrder(properties: List<Property>): List<PropertyWithIndex> {
            return properties.mapIndexed { index, property ->
                PropertyWithIndex(
                    property = property,
                    indexVar = "${index + 1}",
                    alwaysExists = true
                )
            }
        }
    }
}
