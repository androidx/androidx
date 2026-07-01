/*
 * Copyright 2026 The Android Open Source Project
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

package org.jetbrains.androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val MAX_CODE_POINT = 0x10ffff

// Font index digits: 'a'..'z', radix 26
private const val FONT_INDEX_DIGIT0 = 'a'.code
private const val FONT_INDEX_RADIX = 26

// Range size digits: 'a'..'z', radix 26
private const val RANGE_SIZE_DIGIT0 = 'a'.code
private const val RANGE_SIZE_RADIX = 26

// Range value digits: 'A'..'Z', radix 26
private const val RANGE_VALUE_DIGIT0 = 'A'.code
private const val RANGE_VALUE_RADIX = 26

private const val FONTS_GSTATIC_URL_PREFIX = "https://fonts.gstatic.com/s/"

// Required browser User-Agent so that Google Fonts CSS serves WOFF2 font URLs.
private const val WOFF2_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36"

// Maximum characters per line in the generated string concatenation.
private const val LINE_WIDTH = 120

// Number of parallel threads for downloading font files.
private const val DOWNLOAD_THREADS = 8

/**
 * Fonts that are split into multiple subsets served from separate files.
 * CSS is fetched and each @font-face block becomes a separate NotoFont entry.
 */
private val FALLBACK_FONTS = setOf(
    "Noto Color Emoji",
    "Noto Sans Symbols 2",
    "Noto Sans Cuneiform",
    "Noto Sans Duployan",
    "Noto Sans Egyptian Hieroglyphs",
    "Noto Sans HK",
    "Noto Sans JP",
    "Noto Sans KR",
    "Noto Sans SC",
    "Noto Sans TC",
    "Noto Sans",
    "Noto Music",
    "Noto Sans Symbols",
    "Noto Sans Adlam",
    "Noto Sans Anatolian Hieroglyphs",
    "Noto Sans Arabic",
    "Noto Sans Armenian",
    "Noto Sans Avestan",
    "Noto Sans Balinese",
    "Noto Sans Bamum",
    "Noto Sans Bassa Vah",
    "Noto Sans Batak",
    "Noto Sans Bengali",
    "Noto Sans Bhaiksuki",
    "Noto Sans Brahmi",
    "Noto Sans Buginese",
    "Noto Sans Buhid",
    "Noto Sans Canadian Aboriginal",
    "Noto Sans Carian",
    "Noto Sans Caucasian Albanian",
    "Noto Sans Chakma",
    "Noto Sans Cham",
    "Noto Sans Cherokee",
    "Noto Sans Chorasmian",
    "Noto Sans Coptic",
    "Noto Sans Cypro Minoan",
    "Noto Sans Cypriot",
    "Noto Sans Deseret",
    "Noto Sans Devanagari",
    "Noto Sans Elbasan",
    "Noto Sans Elymaic",
    "Noto Sans Ethiopic",
    "Noto Sans Georgian",
    "Noto Sans Glagolitic",
    "Noto Sans Gothic",
    "Noto Sans Grantha",
    "Noto Sans Gujarati",
    "Noto Sans Gunjala Gondi",
    "Noto Sans Gurmukhi",
    "Noto Sans Hanifi Rohingya",
    "Noto Sans Hanunoo",
    "Noto Sans Hatran",
    "Noto Sans Hebrew",
    "Noto Sans Imperial Aramaic",
    "Noto Sans Indic Siyaq Numbers",
    "Noto Sans Inscriptional Pahlavi",
    "Noto Sans Inscriptional Parthian",
    "Noto Sans Javanese",
    "Noto Sans Kaithi",
    "Noto Sans Kannada",
    "Noto Sans Kayah Li",
    "Noto Sans Kharoshthi",
    "Noto Sans Khmer",
    "Noto Sans Khojki",
    "Noto Sans Khudawadi",
    "Noto Sans Lao",
    "Noto Sans Lepcha",
    "Noto Sans Limbu",
    "Noto Sans Linear A",
    "Noto Sans Linear B",
    "Noto Sans Lisu",
    "Noto Sans Lycian",
    "Noto Sans Lydian",
    "Noto Sans Mahajani",
    "Noto Sans Malayalam",
    "Noto Sans Mandaic",
    "Noto Sans Manichaean",
    "Noto Sans Marchen",
    "Noto Sans Masaram Gondi",
    "Noto Sans Math",
    "Noto Sans Mayan Numerals",
    "Noto Sans Meetei Mayek",
    "Noto Sans Mende Kikakui",
    "Noto Sans Meroitic",
    "Noto Sans Miao",
    "Noto Sans Modi",
    "Noto Sans Mongolian",
    "Noto Sans Mro",
    "Noto Sans Multani",
    "Noto Sans Myanmar",
    "Noto Sans NKo",
    "Noto Sans Nabataean",
    "Noto Sans Nandinagari",
    "Noto Sans New Tai Lue",
    "Noto Sans Newa",
    "Noto Sans Nushu",
    "Noto Sans Ogham",
    "Noto Sans Ol Chiki",
    "Noto Sans Old Hungarian",
    "Noto Sans Old Italic",
    "Noto Sans Old North Arabian",
    "Noto Sans Old Permic",
    "Noto Sans Old Persian",
    "Noto Sans Old Sogdian",
    "Noto Sans Old South Arabian",
    "Noto Sans Old Turkic",
    "Noto Sans Oriya",
    "Noto Sans Osage",
    "Noto Sans Osmanya",
    "Noto Sans Pahawh Hmong",
    "Noto Sans Palmyrene",
    "Noto Sans Pau Cin Hau",
    "Noto Sans Phoenician",
    "Noto Sans Psalter Pahlavi",
    "Noto Sans Rejang",
    "Noto Sans Runic",
    "Noto Sans Samaritan",
    "Noto Sans Saurashtra",
    "Noto Sans Sharada",
    "Noto Sans Siddham",
    "Noto Sans SignWriting",
    "Noto Sans Sinhala",
    "Noto Sans Sogdian",
    "Noto Sans Sora Sompeng",
    "Noto Sans Soyombo",
    "Noto Sans Sundanese",
    "Noto Sans Syloti Nagri",
    "Noto Sans Symbols",
    "Noto Sans Symbols 2",
    "Noto Sans Syriac",
    "Noto Sans TC",
    "Noto Sans Tagalog",
    "Noto Sans Tagbanwa",
    "Noto Sans Tai Le",
    "Noto Sans Tai Tham",
    "Noto Sans Tai Viet",
    "Noto Sans Takri",
    "Noto Sans Tamil",
    "Noto Sans Tamil Supplement",
    "Noto Sans Telugu",
    "Noto Sans Thaana",
    "Noto Sans Thai",
    "Noto Sans Tifinagh",
    "Noto Sans Tirhuta",
    "Noto Sans Ugaritic",
    "Noto Sans Vai",
    "Noto Sans Wancho",
    "Noto Sans Warang Citi",
    "Noto Sans Yi",
    "Noto Sans Zanabazar Square",
    "Noto Serif Tibetan",
)

/** A single Noto font entry: its display name and the URL suffix used to download it. */
private data class FontEntry(
    val name: String,
    val urlSuffix: String,  // path after FONTS_GSTATIC_URL_PREFIX
    val starts: List<Int>,  // inclusive start of each supported codepoint range
    val ends: List<Int>,    // inclusive end of each supported codepoint range
)

/** (name, urlSuffix) pair collected during CSS parsing, before charset extraction. */
private data class FontUrl(val name: String, val urlSuffix: String)

private data class IndexedFont(val index: Int, val entry: FontEntry)

/** A boundary event for the range-intersection algorithm. */
private data class Boundary(val value: Int, val isStart: Boolean, val font: IndexedFont)

/** A canonical set of fonts that all support the same set of codepoints. */
private class FontSet(val fonts: List<IndexedFont>) {
    var rangeCount: Int = 0
    var index: Int = 0
}

/** A range of codepoints all covered by the same FontSet. */
private data class Range(val start: Int, val end: Int, val fontSet: FontSet)

/** Trie node for canonicalizing FontSets. */
private class TrieNode {
    val children: MutableMap<Int, TrieNode> = mutableMapOf()
    var fontSet: FontSet? = null

    fun insert(fontIndices: List<Int>): TrieNode {
        var node = this
        for (idx in fontIndices) {
            node = node.children.getOrPut(idx) { TrieNode() }
        }
        return node
    }
}

// ---------------- Gradle task ----------------

/**
 * Generates [NotoFontFallbackData.web.kt] by fetching real glyph coverage from Google Fonts.
 *
 * Unlike relying on CSS unicode-range (which can declare codepoints the font file doesn't contain),
 * this task downloads every woff2 font file and uses Python fonttools to read the actual cmap table.
 * This matches the approach used by Flutter's roll_fallback_fonts.dart (which uses fc-query).
 *
 * Prerequisites: python3 with fonttools + brotli installed.
 *   pip install fonttools brotli
 */
abstract class GenerateNotoFontFallbackDataTask : DefaultTask() {

    /** The Kotlin source file to generate. */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        checkPythonFontTools()

        // Step 1: Fetch CSS for each family to collect (name, urlSuffix) pairs.
        val allFontUrls = mutableListOf<FontUrl>()
        for (familyName in FALLBACK_FONTS.sorted()) {
            allFontUrls.addAll(fetchFontUrls(familyName))
        }
        logger.lifecycle("${allFontUrls.size} font subsets across ${FALLBACK_FONTS.size} families.")

        // Step 2: Download all font files in parallel.
        val tempDir = Files.createTempDirectory("noto_fonts").toFile()
        try {
            logger.lifecycle("Downloading font files (${DOWNLOAD_THREADS} threads)…")
            val fontFiles = downloadFontsParallel(allFontUrls, tempDir)

            // Step 3: Extract real cmap charsets from font binaries via Python fonttools.
            logger.lifecycle("Extracting charsets from ${fontFiles.size} font files…")
            val charsets = extractCharsetsInBatch(fontFiles)

            // Step 4: Build FontEntry list, skipping files that failed or have no cmap.
            val allEntries = allFontUrls.indices.mapNotNull { i ->
                val (starts, ends) = charsets[i]
                if (starts.isEmpty()) null
                else FontEntry(allFontUrls[i].name, allFontUrls[i].urlSuffix, starts, ends)
            }

            val (encodedSets, encodedRanges) = computeEncodedFontSets(allEntries)
            outputFile.get().asFile.apply {
                parentFile.mkdirs()
                writeText(generateKotlinSource(allEntries, encodedSets, encodedRanges))
            }
            logger.lifecycle("Written: ${outputFile.get().asFile.absolutePath}")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ---------------- Prerequisite check ----------------

    private fun checkPythonFontTools() {
        val proc = ProcessBuilder("python3", "-c",
            "from fontTools.ttLib import TTFont; import brotli; print('ok')"
        ).redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().readText().trim()
        val code = proc.waitFor()
        if (code != 0 || output != "ok") {
            throw RuntimeException(
                "Python fonttools + brotli are required to generate font data.\n" +
                "Install with:  pip install fonttools brotli\n" +
                "Python output: $output"
            )
        }
    }

    // ---------------- CSS parsing (URL extraction only) ----------------

    /**
     * Fetches the Google Fonts CSS for [familyName] and returns the list of
     * (name, urlSuffix) pairs — one per @font-face block with a WOFF2 src URL.
     * The CSS unicode-range is intentionally ignored; real coverage is read from
     * the font binaries in [extractCharsetsInBatch].
     */
    private fun fetchFontUrls(familyName: String): List<FontUrl> {
        val familyParam = familyName.replace(" ", "+")
        val cssUrl = "https://fonts.googleapis.com/css2?family=$familyParam"
        logger.lifecycle("  Fetching CSS: $cssUrl")
        val css = fetchText(cssUrl, mapOf("User-Agent" to WOFF2_USER_AGENT))

        val urlRegex = Regex("""src:\s*url\((https?://[^)]+?\.woff2)\)""")
        val result = mutableListOf<FontUrl>()
        var counter = 0
        for (block in css.split("@font-face").drop(1)) {
            val urlMatch = urlRegex.find(block) ?: continue
            val woff2Url = urlMatch.groupValues[1]
            if (!woff2Url.startsWith(FONTS_GSTATIC_URL_PREFIX)) {
                logger.warn("Unexpected URL in CSS for $familyName: $woff2Url — skipping.")
                continue
            }
            result += FontUrl(
                name = "$familyName $counter",
                urlSuffix = woff2Url.removePrefix(FONTS_GSTATIC_URL_PREFIX),
            )
            counter++
        }
        return result
    }

    // ---------------- Font downloading ----------------

    /**
     * Downloads every font listed in [fontUrls] to [tempDir] using [DOWNLOAD_THREADS] parallel
     * threads. Returns the downloaded [File] at each index (null if download failed).
     */
    private fun downloadFontsParallel(fontUrls: List<FontUrl>, tempDir: File): List<File?> {
        val executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS)
        val futures = fontUrls.mapIndexed { i, fontUrl ->
            executor.submit<File?> {
                val url = FONTS_GSTATIC_URL_PREFIX + fontUrl.urlSuffix
                try {
                    val file = File(tempDir, "font_$i.woff2")
                    file.writeBytes(fetchBytes(url))
                    file
                } catch (e: Exception) {
                    logger.warn("Failed to download $url: ${e.message}")
                    null
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.MINUTES)
        return futures.map { it.get() }
    }

    // ---------------- Charset extraction via Python fonttools ----------------

    // language=Python
    private val FONTTOOLS_SCRIPT = """
import sys
from fontTools.ttLib import TTFont

for path in sys.stdin:
    path = path.rstrip('\n')
    if not path:
        print('', flush=True)
        continue
    try:
        font = TTFont(path)
        cmap = font.getBestCmap()
        if not cmap:
            print('', flush=True)
            continue
        cps = sorted(cmap.keys())
        result = []
        s = p = cps[0]
        for cp in cps[1:]:
            if cp == p + 1:
                p = cp
            else:
                result.append(f'{s:X}' if s == p else f'{s:X}-{p:X}')
                s = p = cp
        result.append(f'{s:X}' if s == p else f'{s:X}-{p:X}')
        print(' '.join(result), flush=True)
    except Exception as e:
        sys.stderr.write(f'ERROR {path}: {e}\n')
        sys.stderr.flush()
        print('', flush=True)
""".trimIndent()

    /**
     * Extracts cmap charsets from [fontFiles] using [DOWNLOAD_THREADS] parallel Python fonttools
     * processes. Each thread runs its own Python process over a contiguous slice of the list,
     * communicating via line-by-line stdin/stdout so that every font is logged as it completes.
     */
    private fun extractCharsetsInBatch(fontFiles: List<File?>): List<Pair<List<Int>, List<Int>>> {
        val results = arrayOfNulls<Pair<List<Int>, List<Int>>>(fontFiles.size)
        val chunkSize = maxOf(1, (fontFiles.size + DOWNLOAD_THREADS - 1) / DOWNLOAD_THREADS)
        val chunks = fontFiles.indices.toList().chunked(chunkSize)

        val executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS)
        val futures = chunks.mapIndexed { threadIdx, indices ->
            executor.submit<Unit> {
                val batchResults = extractCharsetsForChunk(indices.map { fontFiles[it] }, threadIdx)
                for ((i, result) in batchResults.withIndex()) {
                    results[indices[i]] = result
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(30, TimeUnit.MINUTES)
        futures.forEach { it.get() }  // re-throw any exception from worker threads

        return results.map { it ?: (emptyList<Int>() to emptyList()) }
    }

    /**
     * Runs a single Python fonttools process for [fontFiles], communicating via line-by-line
     * stdin/stdout. Logs each font as its result arrives.
     */
    private fun extractCharsetsForChunk(
        fontFiles: List<File?>,
        threadIdx: Int,
    ): List<Pair<List<Int>, List<Int>>> {
        val proc = ProcessBuilder("python3", "-c", FONTTOOLS_SCRIPT)
            .redirectErrorStream(false)
            .start()

        val results = mutableListOf<Pair<List<Int>, List<Int>>>()

        val writer = proc.outputStream.bufferedWriter()
        val reader = proc.inputStream.bufferedReader()
        try {
            for (file in fontFiles) {
                writer.write(if (file != null) file.absolutePath else "")
                writer.newLine()
                writer.flush()

                val line = reader.readLine() ?: ""
                val charset = parseCharsetLine(line)
                results += charset
                logger.lifecycle(
                    "  [thread-$threadIdx] ${file?.name ?: "(null)"} → " +
                    if (charset.first.isEmpty()) "empty" else "${charset.first.size} ranges"
                )
            }
        } finally {
            writer.close()
        }

        val errOutput = proc.errorStream.bufferedReader().readText()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            logger.warn("  [thread-$threadIdx] fonttools exited with code $exitCode. Errors:\n$errOutput")
        } else if (errOutput.isNotBlank()) {
            logger.warn("  [thread-$threadIdx] fonttools warnings:\n$errOutput")
        }

        return results
    }

    /**
     * Parses one line of charset output from the Python script.
     * Format: space-separated hex ranges, e.g. `0-FF 200-2FF AC00-D7A3`.
     * An empty line means no coverage (returns empty lists).
     */
    private fun parseCharsetLine(line: String): Pair<List<Int>, List<Int>> {
        if (line.isBlank()) return emptyList<Int>() to emptyList()
        val starts = mutableListOf<Int>()
        val ends = mutableListOf<Int>()
        for (range in line.trim().split(' ')) {
            val parts = range.split('-')
            val start = parts[0].toInt(16)
            val end = if (parts.size > 1) parts[1].toInt(16) else start
            starts += start
            ends += end
        }
        return starts to ends
    }

    // ---------------- STMR encoding ----------------

    /**
     * Computes the STMR-encoded font set and range data from [entries].
     *
     * The algorithm is a direct port of `_computeEncodedFontSets()` from Flutter's
     * `roll_fallback_fonts.dart`.  The encoded strings are returned as a pair:
     *  - first: `encodedFontSets` (comma-separated font-set encodings)
     *  - second: `encodedFontSetRanges` (concatenated range encodings)
     */
    private fun computeEncodedFontSets(entries: List<FontEntry>): Pair<String, String> {
        val indexedFonts = entries.mapIndexed { i, e -> IndexedFont(i, e) }

        // Build boundary list.
        val boundaries = mutableListOf<Boundary>()
        for (font in indexedFonts) {
            for (start in font.entry.starts) boundaries += Boundary(start, true, font)
            for (end in font.entry.ends) boundaries += Boundary(end + 1, false, font)
        }
        boundaries.sortWith(compareBy { it.value })

        // Walk boundaries and collect ranges with their canonical FontSets.
        val trieRoot = TrieNode()
        val current = mutableSetOf<IndexedFont>()
        val ranges = mutableListOf<Range>()
        val allSets = mutableListOf<FontSet>()

        fun recordRange(start: Int, end: Int) {
            val sortedFonts = current.sortedBy { it.index }
            val node = trieRoot.insert(sortedFonts.map { it.index })
            val fontSet = node.fontSet ?: FontSet(sortedFonts).also {
                node.fontSet = it
                allSets += it
            }
            fontSet.rangeCount++
            ranges += Range(start, end, fontSet)
        }

        var start = 0
        for (b in boundaries) {
            if (b.value > start) {
                recordRange(start, b.value - 1)
                start = b.value
            }
            if (b.isStart) current += b.font else current -= b.font
        }
        check(current.isEmpty()) { "Boundary walk ended with non-empty current set." }
        if (start <= MAX_CODE_POINT) recordRange(start, MAX_CODE_POINT)

        logger.lifecycle("  ${allSets.size} font sets, ${ranges.size} ranges.")

        // Sort font sets: most-referenced sets get the smallest indices (smaller encoded values).
        allSets.sortWith(
            compareByDescending<FontSet> { it.rangeCount }
                .thenComparator { a, b ->
                    for (i in 0 until minOf(a.fonts.size, b.fonts.size)) {
                        val cmp = a.fonts[i].index.compareTo(b.fonts[i].index)
                        if (cmp != 0) return@thenComparator cmp
                    }
                    a.fonts.size - b.fonts.size
                }
        )
        allSets.forEachIndexed { i, s -> s.index = i }

        // Encode font sets.
        val setsSb = StringBuilder()
        for ((i, fontSet) in allSets.withIndex()) {
            var prevIndex = -1
            for (font in fontSet.fonts) {
                val delta = font.index - prevIndex  // always >= 1
                prevIndex = font.index
                stmrEncode(delta - 1, FONT_INDEX_RADIX, FONT_INDEX_DIGIT0, setsSb)
            }
            if (i < allSets.lastIndex) setsSb.append(',')
        }

        // Encode ranges.
        val rangesSb = StringBuilder()
        for (range in ranges) {
            val size = range.end - range.start + 1
            if (size >= 2) stmrEncode(size - 2, RANGE_SIZE_RADIX, RANGE_SIZE_DIGIT0, rangesSb)
            stmrEncode(range.fontSet.index, RANGE_VALUE_RADIX, RANGE_VALUE_DIGIT0, rangesSb)
        }

        return setsSb.toString() to rangesSb.toString()
    }

    /**
     * STMR (Self-Terminating Multiple Radix) encoding.
     *
     * Encodes [value] into [sb] using decimal prefix digits followed by a single terminating
     * digit in the range `[firstDigitCode, firstDigitCode + radix)`.
     *
     * Example (radix=26, firstDigitCode='A'.code):
     *   encode(12)   → "M"     (0*26 + 12 = 12)
     *   encode(1000) → "38M"   (38*26 + 12 = 1000, prefix written as decimal "38")
     */
    private fun stmrEncode(value: Int, radix: Int, firstDigitCode: Int, sb: StringBuilder) {
        val prefix = value / radix
        if (prefix != 0) sb.append(prefix)          // decimal prefix (may be > 9)
        sb.append((firstDigitCode + value % radix).toChar())
    }

    // ---------------- Kotlin source generation ----------------

    private fun generateKotlinSource(
        entries: List<FontEntry>,
        encodedSets: String,
        encodedRanges: String,
    ): String {
        return buildString {
            // File header.
            append(
                """
                /*
                 * Copyright 2026 The Android Open Source Project
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

                package androidx.compose.ui.platform
                
                // !!! DO NOT EDIT THIS FILE MANUALLY !!!
                // the code is auto-generated by GenerateNotoFontFallbackDataTask.kt

                internal data class NotoFont(val name: String, val url: String)

                internal fun getNotoFonts(): List<NotoFont> = listOf(

                """.trimIndent()
            )

            // Font list.
            for (entry in entries) {
                appendLine("""    NotoFont(name = "${entry.name}", url = "${entry.urlSuffix}"),""")
            }
            // Remove the trailing comma from the last entry.
            val trailingComma = lastIndexOf(",\n")
            if (trailingComma >= 0) {
                deleteRange(trailingComma, trailingComma + 1)  // remove the ','
            }

            appendLine(")")
            appendLine()

            // encodedNotoFontSets.
            append("internal val encodedNotoFontSets: String =\n")
            appendMultilineString(encodedSets)
            appendLine()

            // encodedNotoFontSetRanges.
            append("internal val encodedNotoFontSetRanges: String =\n")
            appendMultilineString(encodedRanges)
        }
    }

    /**
     * Appends [data] as a multi-line Kotlin string concatenation where each line is at most
     * [LINE_WIDTH] characters wide. Lines are formatted as `    "..." +` except the last which
     * omits the `+`.
     */
    private fun StringBuilder.appendMultilineString(data: String) {
        var pos = 0
        while (pos < data.length) {
            val end = minOf(pos + LINE_WIDTH, data.length)
            val chunk = data.substring(pos, end)
            val isLast = end >= data.length
            if (isLast) {
                appendLine("""    "$chunk"""")
            } else {
                appendLine("""    "$chunk" +""")
            }
            pos = end
        }
    }

    // ---------------- HTTP utilities ----------------

    private fun fetchText(url: String, headers: Map<String, String> = emptyMap()): String {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.connectTimeout = 30_000
            conn.readTimeout   = 60_000
            conn.connect()
            if (conn.responseCode != 200) {
                error("HTTP ${conn.responseCode} for $url: ${conn.responseMessage}")
            }
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchBytes(url: String): ByteArray {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 30_000
            conn.readTimeout   = 60_000
            conn.connect()
            if (conn.responseCode != 200) {
                error("HTTP ${conn.responseCode} for $url: ${conn.responseMessage}")
            }
            return conn.inputStream.readBytes()
        } finally {
            conn.disconnect()
        }
    }
}
