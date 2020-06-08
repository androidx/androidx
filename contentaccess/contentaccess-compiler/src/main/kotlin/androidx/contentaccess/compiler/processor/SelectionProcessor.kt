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

package androidx.contentaccess.compiler.processor

import androidx.contentaccess.compiler.vo.ContentColumnVO
import androidx.contentaccess.compiler.vo.SelectionVO
import javax.annotation.processing.Messager
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

class SelectionProcessor(
    val method: ExecutableElement,
    val messager: Messager,
    val selection: String,
    val columns: Map<String, ContentColumnVO>,
    val paramsNamesAndTypes: HashMap<String, TypeMirror>
) {

    // TODO(obenabde): validate the selection: make sure it's valid syntax, make sure the
    //  comparisons between parameters are valid etc...
    fun process(): Pair<SelectionVO?, Boolean> {
        // TODO(obenabde): right now this forces all to be separated by spaces, so a=:param won't
        //  work since we split here by spaces, either make it support no spaces for comparisons
        //  or decide to keep it this way. Also the detection and substitution logic is pretty
        //  basic, maybe more should be done.
        val selectionsArgs = ArrayList<String>()
        val splitSelection = ArrayList<String>(selection.split(" "))
        for (i in splitSelection.indices) {
            val commaLessWord = splitSelection.get(i).replace(",", "")
            if (splitSelection.get(i).startsWith(":")) {
                val word = splitSelection.get(i)
                if (word.length == 1) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Found stray \":\" in the " +
                            "selection", method)
                    return Pair(null, true)
                }
                val strippedParamName = splitSelection.get(i).substring(1)
                if (!paramsNamesAndTypes.containsKey(strippedParamName)) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Selection argument " +
                            ":${strippedParamName.substring(1)} is not specified in the " +
                            "method's parameters.", method)
                    return Pair(null, true)
                }
                selectionsArgs.add(strippedParamName)
                splitSelection[i] = "?"
            } else if (columns.containsKey(commaLessWord)) {
                splitSelection[i] = splitSelection[i].replace(commaLessWord, columns.get
                    (commaLessWord)!!.columnName)
            }
        }
        return Pair(SelectionVO(splitSelection.joinToString(" "), selectionsArgs), false)
    }
}