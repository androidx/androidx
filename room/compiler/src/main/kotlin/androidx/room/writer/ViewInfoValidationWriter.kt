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

import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.vo.DatabaseView
import com.squareup.javapoet.ParameterSpec
import stripNonJava

class ViewInfoValidationWriter(val view: DatabaseView) : ValidationWriter() {

    override fun write(dbParam: ParameterSpec, scope: CountingCodeGenScope) {
        val suffix = view.viewName.stripNonJava().capitalize()
        scope.builder().apply {
            val expectedInfoVar = scope.getTmpVar("_info$suffix")
            addStatement("final $T $L = new $T($S, $S)",
                    RoomTypeNames.VIEW_INFO, expectedInfoVar, RoomTypeNames.VIEW_INFO,
                    view.viewName, view.createViewQuery)

            val existingVar = scope.getTmpVar("_existing$suffix")
            addStatement("final $T $L = $T.read($N, $S)",
                    RoomTypeNames.VIEW_INFO, existingVar, RoomTypeNames.VIEW_INFO,
                    dbParam, view.viewName)

            beginControlFlow("if (! $L.equals($L))", expectedInfoVar, existingVar).apply {
                addStatement("return new $T(false, $S + $L + $S + $L)",
                        RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                        "${view.viewName}(${view.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar, "\n Found:\n", existingVar)
            }
            endControlFlow()
        }
    }
}
