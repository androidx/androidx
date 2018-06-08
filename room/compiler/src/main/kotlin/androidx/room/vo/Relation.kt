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

import androidx.room.ext.typeName
import javax.lang.model.type.TypeMirror

/**
 * Value object created from processing a @Relation annotation.
 */
class Relation(
        val entity: Entity,
        // return type. e..g. String in @Relation List<String>
        val pojoType: TypeMirror,
        // field in Pojo that holds these relations (e.g. List<Pet> pets)
        val field: Field,
        // the parent field referenced for matching
        val parentField: Field,
        // the field referenced for querying. does not need to be in the response but the query
        // we generate always has it in the response.
        val entityField: Field,
        // the projection for the query
        val projection: List<String>) {

    val pojoTypeName by lazy { pojoType.typeName() }

    fun createLoadAllSql(): String {
        val resultFields = projection.toSet() + entityField.columnName
        return createSelect(resultFields)
    }

    private fun createSelect(resultFields: Set<String>): String {
        return "SELECT ${resultFields.joinToString(",") {"`$it`"}}" +
                " FROM `${entity.tableName}`" +
                " WHERE `${entityField.columnName}` IN (:args)"
    }
}
