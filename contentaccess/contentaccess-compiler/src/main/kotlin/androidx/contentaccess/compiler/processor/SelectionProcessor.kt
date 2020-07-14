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

import androidx.contentaccess.compiler.utils.ErrorReporter
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.compiler.vo.SelectionVO
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.schema.Column
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

class SelectionProcessor(
    private val method: ExecutableElement,
    private val selection: String,
    private val paramsNamesAndTypes: HashMap<String, TypeMirror>,
    private val errorReporter: ErrorReporter,
    private val resolvedContentEntity: ContentEntityVO
) {

    // TODO(obenabde): this is low priority but maybe validate that method arguments being compared
    //  to columns have the same type (e.g "description = :param", if description is a string, then
    //  ensure that param is a string as well.
    fun process(): SelectionVO? {
        // TODO(obenabde): Consider returning a kotlin Result to avoid returning null in case
        //  of failure.
        var modifiedSelection = selection
        val selectionsArgs = ArrayList<String>()
        val wordsStartingWithColumn = findWordsStartingWithColumn(selection)
        for (wordStartingWithColumn in wordsStartingWithColumn) {
                if (wordStartingWithColumn.length == 1) {
                    errorReporter.reportError(strayColumnInSelectionErrorMessage(), method)
                    return null
                }
                val strippedParamName = wordStartingWithColumn.substring(1)
                if (!paramsNamesAndTypes.containsKey(strippedParamName)) {
                    errorReporter.reportError(selectionParameterNotInMethodParameters
                        (strippedParamName), method)
                    return null
                }
                selectionsArgs.add(strippedParamName)
                modifiedSelection = modifiedSelection.replaceFirst(wordStartingWithColumn, "?")
        }
        val selectionExpression = CCJSqlParserUtil.parseCondExpression(modifiedSelection)
        var foundMissingColumn = false
        selectionExpression.accept(object : ExpressionVisitorAdapter() {
            override fun visit(column: Column?) {
                val columnString = column.toString()
                // So it seems to assume that in the expression col1 = "ll" that "ll" is a column
                // and doesn't understand that it's a string reference for some reasons... work
                // around this for now.
                if (!columnString.startsWith('"') || !columnString.endsWith('"')) {
                    if (!resolvedContentEntity.columns.contains(columnString)) {
                        errorReporter.reportError(columnInSelectionMissingFromEntity(columnString,
                                resolvedContentEntity.type.toString()), method)
                        foundMissingColumn = true
                    }
                }
            }
        })
        if (foundMissingColumn) {
            return null
        }
        return SelectionVO(modifiedSelection, selectionsArgs)
    }
}

fun findWordsStartingWithColumn(expression: String): List<String> {
    val wordsStartingWithColumn = mutableListOf<String>()
    var currIndex = 0
    while (currIndex < expression.length) {
        if (expression[currIndex] == ':') {
            val startingIndex = currIndex
            while (currIndex + 1 < expression.length && expression[currIndex + 1]
                    .isLetterOrDigit()) {
                currIndex++
            }
            wordsStartingWithColumn.add(expression.substring(startingIndex, currIndex + 1))
        }
        currIndex++
    }
    return wordsStartingWithColumn
}