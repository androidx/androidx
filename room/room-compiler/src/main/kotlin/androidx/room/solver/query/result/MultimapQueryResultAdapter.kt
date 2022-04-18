/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.solver.query.result

import androidx.room.compiler.processing.XType
import androidx.room.ext.L
import androidx.room.ext.W
import androidx.room.ext.implementsEqualsAndHashcode
import androidx.room.log.RLog
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.types.CursorValueReader
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.MapInfo
import androidx.room.vo.Warning
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock

/**
 * Abstract class for Map and Multimap result adapters.
 */
abstract class MultimapQueryResultAdapter(
    rowAdapters: List<RowAdapter>,
) : QueryResultAdapter(rowAdapters) {
    abstract val keyTypeArg: XType
    abstract val valueTypeArg: XType

    companion object {

        val declaredToImplCollection = mapOf<ClassName, ClassName>(
            ClassName.get(List::class.java) to ClassName.get(ArrayList::class.java),
            ClassName.get(Set::class.java) to ClassName.get(HashSet::class.java)
        )

        /**
         * Checks if the @MapInfo annotation is needed for clarification regarding the return type
         * of a Dao method.
         */
        fun validateMapTypeArgs(
            keyTypeArg: XType,
            valueTypeArg: XType,
            keyReader: CursorValueReader?,
            valueReader: CursorValueReader?,
            mapInfo: MapInfo?,
            logger: RLog
        ) {

            if (!keyTypeArg.implementsEqualsAndHashcode()) {
                logger.w(
                    Warning.DOES_NOT_IMPLEMENT_EQUALS_HASHCODE,
                    ProcessorErrors.classMustImplementEqualsAndHashCode(
                        keyTypeArg.typeName.toString()
                    )
                )
            }

            val hasKeyColumnName = mapInfo?.keyColumnName?.isNotEmpty() ?: false
            if (!hasKeyColumnName && keyReader != null) {
                logger.e(
                    ProcessorErrors.keyMayNeedMapInfo(
                        keyTypeArg.typeName
                    )
                )
            }

            val hasValueColumnName = mapInfo?.valueColumnName?.isNotEmpty() ?: false
            if (!hasValueColumnName && valueReader != null) {
                logger.e(
                    ProcessorErrors.valueMayNeedMapInfo(
                        valueTypeArg.typeName
                    )
                )
            }
        }
    }

    /**
     * Generates a code expression that verifies if all matched fields are null.
     */
    fun getColumnNullCheckCode(
        cursorVarName: String,
        indexVars: List<ColumnIndexVar>
    ): CodeBlock {
        val conditions = indexVars.map {
            CodeBlock.of(
                "$L.isNull($L)",
                cursorVarName,
                it.indexVar
            )
        }
        return CodeBlock.join(conditions, "$W&&$W")
    }
}
