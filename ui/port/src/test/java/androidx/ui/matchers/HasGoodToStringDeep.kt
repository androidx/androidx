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

package androidx.ui.matchers

import androidx.ui.foundation.diagnostics.DiagnosticableTree
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * Asserts that an object's toStringDeep() is a plausible multi-line
 * description.
 *
 * Specifically, this matcher checks that an object's
 * `toStringDeep(prefixLineOne, prefixOtherLines)`:
 *
 *  * Does not have leading or trailing whitespace.
 *  * Does not contain the default `Instance of ...` string.
 *  * The last line has characters other than tree connector characters and
 *    whitespace. For example: the line ` │ ║ ╎` has only tree connector
 *    characters and whitespace.
 *  * Does not contain lines with trailing white space.
 *  * Has multiple lines.
 *  * The first line starts with `prefixLineOne`
 *  * All subsequent lines start with `prefixOtherLines`.
 */
object HasGoodToStringDeep : BaseMatcher<DiagnosticableTree>() {

    override fun matches(item: Any?): Boolean {
        val errorDescription = getErrorDescription(item)
        return !errorDescription.isNotEmpty()
    }

    override fun describeMismatch(item: Any?, mismatchDescription: Description?) {
        mismatchDescription?.appendText(getErrorDescription(item))
    }

    override fun describeTo(description: Description?) {
        description?.appendText("multi line description")
    }

    private fun getErrorDescription(item: Any?): String {
        if (item !is DiagnosticableTree) {
            return "$item is not a DiagnosticableTree."
        }
        val issues = mutableListOf<String>()
        var description = item.toStringDeep()
        if (description.endsWith('\n')) {
            // Trim off trailing \n as the remaining calculations assume
            // the description does not end with a trailing \n.
            description = description.substring(0, description.length - 1)
        } else {
            issues.add("Not terminated with a line break.")
        }

        if (description.trim() != description)
            issues.add("Has trailing whitespace.")

        val lines = description.split("\n")
        if (lines.size < 2)
            issues.add("Does not have multiple lines.")

        if (description.contains("Instance of "))
            issues.add("Contains text \"Instance of \".")

        lines.forEachIndexed { i, line ->
            if (line.isEmpty())
                issues.add("Line ${i + 1} is empty.")

            if (line.trimEnd() != line)
                issues.add("Line ${i + 1} has trailing whitespace.")
        }

        if (isAllTreeConnectorCharacters(lines.last()))
            issues.add("Last line is all tree connector characters.")

        // If a toStringDeep method doesn"t properly handle nested values that
        // contain line breaks it can fail to add the required prefixes to all
        // lined when toStringDeep is called specifying prefixes.
        val prefixLineOne = "PREFIX_LINE_ONE____"
        val prefixOtherLines = "PREFIX_OTHER_LINES_"
        val prefixIssues = mutableListOf<String>()
        var descriptionWithPrefixes =
                item.toStringDeep(prefixLineOne = prefixLineOne,
                        prefixOtherLines = prefixOtherLines)
        if (descriptionWithPrefixes.endsWith("\n")) {
            // Trim off trailing \n as the remaining calculations assume
            // the description does not end with a trailing \n.
            descriptionWithPrefixes = descriptionWithPrefixes.substring(
                    0, descriptionWithPrefixes.length - 1)
        }
        val linesWithPrefixes = descriptionWithPrefixes.split("\n")
        if (!linesWithPrefixes.first().startsWith(prefixLineOne))
            prefixIssues.add("First line does not contain assertEqualsed prefix.")

        for (i in 1 until linesWithPrefixes.size) {
            if (!linesWithPrefixes[i].startsWith(prefixOtherLines)) {
                prefixIssues.add("Line ${i + 1} does not contain the assertEqualsed prefix.")
            }
        }

        val errorDescription = StringBuffer()
        if (issues.isNotEmpty()) {
            errorDescription.append("Bad toStringDeep():\n")
            errorDescription.append("$description\n")
            errorDescription.append(issues.joinToString("\n"))
        }

        if (prefixIssues.isNotEmpty()) {
            errorDescription.append("Bad toStringDeep(prefixLineOne: $prefixLineOne, " +
                    "prefixOtherLines: $prefixOtherLines):\n")
            errorDescription.append("$descriptionWithPrefixes\n")
            errorDescription.append(prefixIssues.joinToString("\n"))
        }
        return errorDescription.toString()
    }

    /**
     * Returns whether a [line] is all vertical tree connector characters.
     *
     * Example vertical tree connector characters: `│ ║ ╎`.
     * The last line of a text tree contains only vertical tree connector
     * characters indicates a poorly formatted tree.
     */
    private fun isAllTreeConnectorCharacters(line: String): Boolean {
        for (i in 0 until line.length) {
            val c = line[i].toInt()
            if (!isWhitespace(c) && !isVerticalLine(c))
                return false
        }
        return true
    }

    /**
     * Returns true if [c] represents a whitespace code unit.
     */
    private fun isWhitespace(c: Int) = (c in 0x0009..0x000D) || c == 0x0020

    /**
     * Returns true if [c] represents a vertical line Unicode line art code unit.
     *
     * See [https://en.wikipedia.org/wiki/Box-drawing_character]. This method only
     * specifies vertical line art code units currently used by Flutter line art.
     * There are other line art characters that technically also represent vertical
     * lines.
     */
    private fun isVerticalLine(c: Int) =
            c == 0x2502 || c == 0x2503 || c == 0x2551 || c == 0x254e
}