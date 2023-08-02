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

package androidx.room.writer

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.vo.DatabaseView
import java.util.Locale

class ViewInfoValidationWriter(val view: DatabaseView) : ValidationWriter() {

    override fun write(dbParamName: String, scope: CountingCodeGenScope) {
        val suffix = view.viewName.stripNonJava().capitalize(Locale.US)
        scope.builder.apply {
            val expectedInfoVar = scope.getTmpVar("_info$suffix")
            addLocalVariable(
                name = expectedInfoVar,
                typeName = RoomTypeNames.VIEW_INFO,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    RoomTypeNames.VIEW_INFO,
                    "%S, %S",
                    view.viewName, view.createViewQuery
                )
            )

            val existingVar = scope.getTmpVar("_existing$suffix")
            addLocalVal(
                existingVar,
                RoomTypeNames.VIEW_INFO,
                "%M(%L, %S)",
                RoomMemberNames.VIEW_INFO_READ, dbParamName, view.viewName
            )

            beginControlFlow("if (!%L.equals(%L))", expectedInfoVar, existingVar).apply {
                addStatement(
                    "return %L",
                    XCodeBlock.ofNewInstance(
                        language,
                        RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                        "false, %S + %L + %S + %L",
                        "${view.viewName}(${view.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar,
                        "\n Found:\n",
                        existingVar
                    )
                )
            }
            endControlFlow()
        }
    }
}
