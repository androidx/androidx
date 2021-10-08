#!/usr/bin/env kotlin

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

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("junit:junit:4.11")

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.exitProcess

if (args[0] == "test") {
    runTests()
    println("All tests passed")
    exitProcess(0)
}

if (args.isEmpty()) {
    println("Expected space-delimited list of files. Consider parsing all baselines with:")
    println("  ./<path-to-script> `find . -name lint-baseline.xml`")
    println("Also, consider updating baselines before running the script using:")
    println("  ./gradlew lintDebug -PupdateLintBaseline --continue")
    exitProcess(1)
}

val missingFiles = args.filter { arg -> !File(arg).exists() }
if (missingFiles.isNotEmpty()) {
    println("Could not find files:\n  ${missingFiles.joinToString("\n  ")}")
    exitProcess(1)
}

val executionPath = File(".")
// TODO: Consider adding argument "--output <output-file-path>"
val csvOutputFile = File("output.csv")
val csvData = StringBuilder()
val columnLabels = listOf(
    "Baseline",
    "ID",
    "Message",
    "Error",
    "Location",
    "Line"
)

// Emit labels into the CSV if it's being created from scratch.
if (!csvOutputFile.exists()) {
    csvData.append(columnLabels.joinToString(","))
    csvData.append("\n")
}

// For each file, emit one issue per line into the CSV.
args.forEach { lintBaselinePath ->
    val lintBaselineFile = File(lintBaselinePath)
    println("Parsing ${lintBaselineFile.path}...")

    val lintIssuesList = LintBaselineParser.parse(lintBaselineFile)
    lintIssuesList.forEach { lintIssues ->
        lintIssues.issues.forEach { lintIssue ->
            val columns = listOf(
                lintIssues.file.toRelativeString(executionPath),
                lintIssue.id,
                lintIssue.message,
                lintIssue.errorLines.joinToString("\n"),
                lintIssue.locations.getOrNull(0)?.file ?: "",
                lintIssue.locations.getOrNull(0)?.line?.toString() ?: "",
            )
            csvData.append(columns.joinToString(",") { data ->
                // Wrap every item with quotes and escape existing quotes.
                "\"${data.replace("\"", "\"\"")}\""
            })
            csvData.append("\n")
        }
    }
}

csvOutputFile.appendText(csvData.toString())

println("Wrote CSV output to ${csvOutputFile.path} for ${args.size} baselines")

object LintBaselineParser {
    fun parse(lintBaselineFile: File): List<LintIssues> {
        val builderFactory = DocumentBuilderFactory.newInstance()!!
        val docBuilder = builderFactory.newDocumentBuilder()!!
        val doc: Document = docBuilder.parse(lintBaselineFile)
        return parseIssuesListFromDocument(doc, lintBaselineFile)
    }

    fun parse(lintBaselineText: String): List<LintIssues> {
        val builderFactory = DocumentBuilderFactory.newInstance()!!
        val docBuilder = builderFactory.newDocumentBuilder()!!
        val doc: Document = docBuilder.parse(InputSource(StringReader(lintBaselineText)))
        return parseIssuesListFromDocument(doc, File("."))
    }

    private fun parseIssuesListFromDocument(doc: Document, file: File): List<LintIssues> =
        doc.getElementsByTagName("issues").mapElementsNotNull { issues ->
            LintIssues(
                file = file,
                issues = parseIssueListFromIssues(issues),
            )
        }

    private fun parseIssueListFromIssues(issues: Element): List<LintIssue> =
        issues.getElementsByTagName("issue").mapElementsNotNull { issue ->
            LintIssue(
                id = issue.getAttribute("id"),
                message = issue.getAttribute("message"),
                errorLines = parseErrorLineListFromIssue(issue),
                locations = parseLocationListFromIssue(issue),
            )
        }

    private fun parseLocationListFromIssue(issue: Element): List<LintLocation> =
        issue.getElementsByTagName("location").mapElementsNotNull { location ->
            LintLocation(
                file = location.getAttribute("file"),
                line = location.getAttribute("line")?.toIntOrNull() ?: 0,
                column = location.getAttribute("column")?.toIntOrNull() ?: 0,
            )
        }

    private fun parseErrorLineListFromIssue(issue: Element): List<String> {
        val list = mutableListOf<String>()
        var i = 1
        while (issue.hasAttribute("errorLine$i")) {
            issue.getAttribute("errorLine$i")?.let{ list.add(it) }
            i++
        }
        return list.toList()
    }

    // This MUST be inside the class, otherwise we'll get a compilation error.
    private fun <T> NodeList.mapElementsNotNull(transform: (element: Element) -> T?): List<T> {
        val list = mutableListOf<T>()
        for (i in 0 until length) {
            val node = item(i)
            if (node.nodeType == Node.ELEMENT_NODE && node is Element) {
                transform(node)?.let { list.add(it) }
            }
        }
        return list.toList()
    }
}

data class LintIssues(
    val file: File,
    val issues: List<LintIssue>,
)

data class LintIssue(
    val id: String,
    val message: String,
    val errorLines: List<String>,
    val locations: List<LintLocation>,
)

data class LintLocation(
    val file: String,
    val line: Int,
    val column: Int,
)

fun runTests() {
    `Baseline with one issue parses contents correctly`()
    `Empty baseline has no issues`()
}

@Test
fun `Baseline with one issue parses contents correctly`() {
    /* ktlint-disable max-line-length */
    var lintBaselineText = """
        <?xml version="1.0" encoding="UTF-8"?>
        <issues format="5" by="lint 4.2.0-beta06" client="gradle" variant="debug" version="4.2.0-beta06">
        
            <issue
                id="ClassVerificationFailure"
                message="This call references a method added in API level 19; however, the containing class androidx.print.PrintHelper is reachable from earlier API levels and will fail run-time class verification."
                errorLine1="        PrintAttributes attr = new PrintAttributes.Builder()"
                errorLine2="                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
                <location
                    file="src/main/java/androidx/print/PrintHelper.java"
                    line="271"
                    column="32"/>
            </issue>
        </issues>
    """.trimIndent()
    /* ktlint-enable max-line-length */

    var listIssues = LintBaselineParser.parse(lintBaselineText)
    assertEquals(1, listIssues.size)

    var issues = listIssues[0].issues
    assertEquals(1, issues.size)

    var issue = issues[0]
    assertEquals("ClassVerificationFailure", issue.id)
    assertEquals("This call references a method added in API level 19; however, the containing " +
        "class androidx.print.PrintHelper is reachable from earlier API levels and will fail " +
        "run-time class verification.", issue.message)
    assertEquals(2, issue.errorLines.size)
    assertEquals(1, issue.locations.size)

    var location = issue.locations[0]
    assertEquals("src/main/java/androidx/print/PrintHelper.java", location.file)
    assertEquals(271, location.line)
    assertEquals(32, location.column)
}

@Test
fun `Empty baseline has no issues`() {
    /* ktlint-disable max-line-length */
    var lintBaselineText = """
        <?xml version="1.0" encoding="UTF-8"?>
        <issues format="5" by="lint 4.2.0-beta06" client="gradle" version="4.2.0-beta06">

        </issues>
    """.trimIndent()
    /* ktlint-enable max-line-length */

    var listIssues = LintBaselineParser.parse(lintBaselineText)
    assertEquals(1, listIssues.size)

    var issues = listIssues[0].issues
    assertEquals(0, issues.size)
}
