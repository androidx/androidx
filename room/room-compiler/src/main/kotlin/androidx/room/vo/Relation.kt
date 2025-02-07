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

import androidx.room.compiler.processing.XType

/** Value object created from processing a @Relation annotation. */
class Relation(
    val entity: EntityOrView,
    // return type. e..g. String in @Relation List<String>
    val dataClassType: XType,
    // property in data class that holds these relations (e.g. List<Pet> pets)
    val property: Property,
    // the parent property referenced for matching
    val parentProperty: Property,
    // the property referenced for querying. does not need to be in the response but the query
    // we generate always has it in the response.
    val entityProperty: Property,
    // Used for joining on a many-to-many relation
    val junction: Junction?,
    // the projection for the query
    val projection: List<String>
) {
    val dataClassTypeName by lazy { dataClassType.asTypeName() }

    fun createLoadAllSql(): String {
        val resultProperties = projection.toSet()
        return createSelect(resultProperties)
    }

    private fun createSelect(resultProperties: Set<String>) = buildString {
        if (junction != null) {
            val resultColumns =
                resultProperties.map { "`${entity.tableName}`.`$it` AS `$it`" } +
                    "_junction.`${junction.parentProperty.columnName}`"
            append("SELECT ${resultColumns.joinToString(",")}")
            append(" FROM `${junction.entity.tableName}` AS _junction")
            append(
                " INNER JOIN `${entity.tableName}` ON" +
                    " (_junction.`${junction.entityProperty.columnName}`" +
                    " = `${entity.tableName}`.`${entityProperty.columnName}`)"
            )
            append(" WHERE _junction.`${junction.parentProperty.columnName}` IN (:args)")
        } else {
            val resultColumns =
                resultProperties.map { "`$it`" }.toSet() + "`${entityProperty.columnName}`"
            append("SELECT ${resultColumns.joinToString(",")}")
            append(" FROM `${entity.tableName}`")
            append(" WHERE `${entityProperty.columnName}` IN (:args)")
        }
    }
}
