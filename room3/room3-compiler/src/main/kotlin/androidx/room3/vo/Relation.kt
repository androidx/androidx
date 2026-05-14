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

package androidx.room3.vo

import androidx.room3.compiler.processing.XType

/** Value object created from processing a @Relation annotation. */
class Relation(
    val entity: EntityOrView,
    // return type. e.g. String in @Relation List<String>
    val dataClassType: XType,
    // property in data class that holds these relations (e.g. List<Pet> pets)
    val property: Property,
    // the parent properties referenced for matching
    val parentProperties: List<Property>,
    // the properties referenced for querying. does not need to be in the response but the query
    // we generate always has it in the response.
    val entityProperties: List<Property>,
    // Used for joining on a many-to-many relation
    val junction: Junction?,
    // the projection for the query
    val projection: List<String>,
) {
    val hasCompositeKey = parentProperties.size > 1
    val dataClassTypeName by lazy { dataClassType.asTypeName() }
}
