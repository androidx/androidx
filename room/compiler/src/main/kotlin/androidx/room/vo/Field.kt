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

package androidx.room.vo

import androidx.room.ext.isNonNull
import androidx.room.ext.typeName
import androidx.room.migration.bundle.FieldBundle
import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.solver.types.CursorValueReader
import androidx.room.solver.types.StatementValueBinder
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
// used in cache matching, must stay as a data class or implement equals
data class Field(val element: Element, val name: String, val type: TypeMirror,
                 var affinity: SQLTypeAffinity?,
                 val collate: Collate? = null,
                 val columnName: String = name,
                 /* means that this field does not belong to parent, instead, it belongs to a
                 * embedded child of the main Pojo*/
                 val parent: EmbeddedField? = null,
                 // index might be removed when being merged into an Entity
                 var indexed: Boolean = false) : HasSchemaIdentity {
    lateinit var getter: FieldGetter
    lateinit var setter: FieldSetter
    // binds the field into a statement
    var statementBinder: StatementValueBinder? = null
    // reads this field from a cursor column
    var cursorValueReader: CursorValueReader? = null
    val typeName: TypeName by lazy { type.typeName() }

    /** Whether the table column for this field should be NOT NULL */
    val nonNull = element.isNonNull() && (parent == null || parent.isNonNullRecursively())

    override fun getIdKey(): String {
        // we don't get the collate information from sqlite so ignoring it here.
        return "$columnName-${affinity?.name ?: SQLTypeAffinity.TEXT.name}-$nonNull"
    }

    /**
     * Used when reporting errors on duplicate names
     */
    fun getPath(): String {
        return if (parent == null) {
            name
        } else {
            "${parent.field.getPath()} > $name"
        }
    }

    private val pathWithDotNotation: String by lazy {
        if (parent == null) {
            name
        } else {
            "${parent.field.pathWithDotNotation}.$name"
        }
    }

    /**
     * List of names that include variations.
     * e.g. if it is mUser, user is added to the list
     * or if it is isAdmin, admin is added to the list
     */
    val nameWithVariations by lazy {
        val result = arrayListOf(name)
        if (name.length > 1) {
            if (name.startsWith('_')) {
                result.add(name.substring(1))
            }
            if (name.startsWith("m") && name[1].isUpperCase()) {
                result.add(name.substring(1).decapitalize())
            }

            if (typeName == TypeName.BOOLEAN || typeName == TypeName.BOOLEAN.box()) {
                if (name.length > 2 && name.startsWith("is") && name[2].isUpperCase()) {
                    result.add(name.substring(2).decapitalize())
                }
                if (name.length > 3 && name.startsWith("has") && name[3].isUpperCase()) {
                    result.add(name.substring(3).decapitalize())
                }
            }
        }
        result
    }

    val getterNameWithVariations by lazy {
        nameWithVariations.map { "get${it.capitalize()}" } +
                if (typeName == TypeName.BOOLEAN || typeName == TypeName.BOOLEAN.box()) {
                    nameWithVariations.flatMap {
                        listOf("is${it.capitalize()}", "has${it.capitalize()}")
                    }
                } else {
                    emptyList()
                }
    }

    val setterNameWithVariations by lazy {
        nameWithVariations.map { "set${it.capitalize()}" }
    }

    /**
     * definition to be used in create query
     */
    fun databaseDefinition(autoIncrementPKey: Boolean): String {
        val columnSpec = StringBuilder("")
        if (autoIncrementPKey) {
            columnSpec.append(" PRIMARY KEY AUTOINCREMENT")
        }
        if (nonNull) {
            columnSpec.append(" NOT NULL")
        }
        if (collate != null) {
            columnSpec.append(" COLLATE ${collate.name}")
        }
        return "`$columnName` ${(affinity ?: SQLTypeAffinity.TEXT).name}$columnSpec"
    }

    fun toBundle(): FieldBundle = FieldBundle(pathWithDotNotation, columnName,
            affinity?.name ?: SQLTypeAffinity.TEXT.name, nonNull
    )
}
