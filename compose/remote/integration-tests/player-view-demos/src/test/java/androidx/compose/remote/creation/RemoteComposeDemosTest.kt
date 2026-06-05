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

package androidx.compose.remote.creation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.core.operations.utilities.MatrixOperations
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColor
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcText
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.verticalScroll
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.dslDemoPressureGauge
import androidx.compose.remote.integration.view.demos.dsl.dslTicker
import androidx.compose.remote.integration.view.demos.examples.RcMacroDemo
import androidx.compose.remote.integration.view.demos.examples.RcMacroLocalDemo
import androidx.compose.remote.integration.view.demos.examples.RcReferencedOperationsMacroDemo
import androidx.compose.remote.integration.view.demos.examples.RcStyleMacroDemo
import androidx.compose.remote.integration.view.demos.examples.RcTextDemo8
import androidx.compose.remote.integration.view.demos.examples.demoGraphs2
import androidx.compose.remote.integration.view.demos.examples.demoLinearRegression
import androidx.compose.remote.integration.view.demos.examples.rcJsonGraphs2
import androidx.compose.remote.integration.view.demos.examples.rcJsonLinearRegression
import androidx.compose.remote.integration.view.demos.examples.rcJsonMacroDemo
import androidx.compose.remote.integration.view.demos.examples.rcJsonMacroLocalDemo
import androidx.compose.remote.integration.view.demos.examples.rcJsonPressureGauge
import androidx.compose.remote.integration.view.demos.examples.rcJsonReferencedOperationsMacroDemo
import androidx.compose.remote.integration.view.demos.examples.rcJsonStyleMacroDemo
import androidx.compose.remote.integration.view.demos.examples.rcJsonTextDemo8
import androidx.compose.remote.integration.view.demos.examples.rcJsonTicker
import androidx.compose.remote.player.core.RemoteDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteComposeDemosTest {

    private val scratchDir: java.io.File by lazy {
        java.io.File(System.getProperty("java.io.tmpdir"), "rc-demos-test").apply { mkdirs() }
    }

    private fun scratch(name: String): java.io.File = java.io.File(scratchDir, name)

    private fun dumpOpcodesAndOffsets(
        bytes: ByteArray,
        opcodesFile: java.io.File,
        offsetsFile: java.io.File,
    ) {
        val buffer = RemoteComposeBuffer()
        RemoteComposeBuffer.read(ByteArrayInputStream(bytes), buffer)
        val operations = ArrayList<Operation>()

        val captured = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(captured))
        try {
            buffer.inflateFromBuffer(operations)
        } finally {
            System.setOut(originalOut)
        }

        fun writeOps(ops: List<Operation>, out: java.io.PrintWriter, indent: String) {
            for (op in ops) {
                out.println(op.deepToString(indent))
                val bodyBytes =
                    when (op) {
                        is androidx.compose.remote.core.operations.loom.PatternDefine ->
                            op.getBody()
                        is androidx.compose.remote.core.operations.ReferencedOperations ->
                            op.getBody()
                        else -> null
                    }
                if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                    out.println("$indent  BODY BYTES [${bodyBytes.size} bytes]:")
                    out.println(
                        "$indent    " + bodyBytes.joinToString(" ") { String.format("%02X", it) }
                    )
                    val subBuffer = RemoteComposeBuffer(7)
                    subBuffer.setVersion(7, 513)
                    subBuffer.addHeader(
                        shortArrayOf(androidx.compose.remote.core.operations.Header.DOC_PROFILES),
                        arrayOf<Any>(513),
                    )
                    subBuffer.getBuffer().write(bodyBytes)
                    val subOps = ArrayList<Operation>()
                    try {
                        subBuffer.inflateFromBuffer(subOps)
                    } catch (e: Exception) {
                        out.println("$indent  [Failed to inflate body: ${e.message}]")
                    }
                    val bodyOpsOnly = subOps.drop(1) // drop the header
                    writeOps(bodyOpsOnly, out, "$indent  ")
                }
            }
        }

        opcodesFile.printWriter().use { out -> writeOps(operations, out, "  ") }

        offsetsFile.printWriter().use { out ->
            captured
                .toString(Charsets.UTF_8.name())
                .lineSequence()
                .filter { it.startsWith("### OP_OFFSET: ") }
                .forEach { out.println(it.removePrefix("### OP_OFFSET: ")) }
        }
    }

    private fun dumpOnMismatch(label: String, dsl: ByteArray, json: ByteArray): Boolean {
        if (!dsl.contentEquals(json)) {
            val firstDiff = dsl.zip(json).indexOfFirst { it.first != it.second }
            val diffIndex = if (firstDiff == -1) Math.min(dsl.size, json.size) else firstDiff
            println(
                "### FIRST DIFFERENCE AT INDEX $diffIndex (DSL size: ${dsl.size}, JSON size: ${json.size})"
            )

            fun hexDumpContext(arr: ByteArray, start: Int, end: Int): String {
                val sb = StringBuilder()
                for (i in start until end) {
                    if (i < 0 || i >= arr.size) {
                        sb.append("   ")
                    } else {
                        sb.append(String.format("%02X ", arr[i]))
                    }
                }
                sb.append(" | ")
                for (i in start until end) {
                    if (i < 0 || i >= arr.size) {
                        sb.append(" ")
                    } else {
                        val c = arr[i].toInt().toChar()
                        if (c in ' '..'~') sb.append(c) else sb.append('.')
                    }
                }
                return sb.toString()
            }

            val startIdx = Math.max(0, diffIndex - 16)
            val endIdx = Math.min(Math.max(dsl.size, json.size), diffIndex + 16)
            println("  DSL:  ${hexDumpContext(dsl, startIdx, endIdx)}")
            println("  JSON: ${hexDumpContext(json, startIdx, endIdx)}")

            val dslFile = scratch("${label}_dsl_bytes.bin")
            val jsonFile = scratch("${label}_json_bytes.bin")
            val dslOpcodes = scratch("${label}_dsl_opcodes.txt")
            val dslOffsets = scratch("${label}_dsl_offsets.txt")
            val jsonOpcodes = scratch("${label}_json_opcodes.txt")
            val jsonOffsets = scratch("${label}_json_offsets.txt")
            dslFile.writeBytes(dsl)
            jsonFile.writeBytes(json)
            println("### DUMPED DSL BYTES TO: ${dslFile.absolutePath}")
            println("### DUMPED JSON BYTES TO: ${jsonFile.absolutePath}")
            println("### DUMPED DSL OPCODES TO: ${dslOpcodes.absolutePath}")
            println("### DUMPED JSON OPCODES TO: ${jsonOpcodes.absolutePath}")
            dumpOpcodesAndOffsets(dsl, dslOpcodes, dslOffsets)
            dumpOpcodesAndOffsets(json, jsonOpcodes, jsonOffsets)
            return true
        }
        return false
    }

    private class MockPlatform : RcPlatformServices {
        override fun imageToByteArray(image: Any): ByteArray? = null

        override fun getImageWidth(image: Any): Int = 10

        override fun getImageHeight(image: Any): Int = 10

        override fun isAlpha8Image(image: Any): Boolean = false

        override fun pathToFloatArray(path: Any): FloatArray? {
            if (path is RemotePath) {
                return path.createFloatArray()
            }
            return null
        }

        override fun parsePath(pathStr: String): Any = RemotePath(pathStr)

        override fun log(category: RcPlatformServices.LogCategory, message: String) {}
    }

    @Test
    fun testRcDslTickerComparison() {
        val platform = MockPlatform()
        println("### FORCING TICKER TEST RUN 106 ###")
        val dslBytes = dslTicker()

        val jsonContext = rcJsonTicker(platform)
        val jsonBytes = jsonContext.buffer()

        println(
            "RemoteComposeState Class Location: ${androidx.compose.remote.core.RemoteComposeState::class.java.protectionDomain?.codeSource?.location}"
        )
        println(
            "RemoteComposeState.START_ID: ${androidx.compose.remote.core.RemoteComposeState.START_ID}"
        )
        println("DSL Buffer Size: ${dslBytes.size}")
        println("JSON Buffer Size: ${jsonBytes.size}")

        println(
            "DSL First 20 bytes: ${dslBytes.take(20).joinToString { it.toInt().and(0xFF).toString(16).padStart(2, '0') }}"
        )

        if (!dslBytes.contentEquals(jsonBytes)) {
            dumpOpcodesAndOffsets(dslBytes, scratch("dsl_opcodes.txt"), scratch("dsl_offsets.txt"))
            dumpOpcodesAndOffsets(
                jsonBytes,
                scratch("json_opcodes.txt"),
                scratch("json_offsets.txt"),
            )
            println("Wrote opcodes, resources and offsets to files for diffing.")
        }

        val dslBytesFile = scratch("dsl_bytes.bin")
        dslBytesFile.writeBytes(dslBytes)
        val jsonBytesFile = scratch("json_bytes.bin")
        jsonBytesFile.writeBytes(jsonBytes)

        assertArrayEquals("Ticker DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    @Test
    fun testRcDslTextDemo8Comparison() {
        val dslBytes = RcTextDemo8().buffer()
        val jsonBytes = rcJsonTextDemo8().buffer()
        dumpOnMismatch("text8", dslBytes, jsonBytes)
        assertArrayEquals("TextDemo8 DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    @Test
    fun testRcDslMacroLocalComparison() {
        val platform = MockPlatform()
        val dslBytes = RcMacroLocalDemo().buffer()
        val jsonBytes = rcJsonMacroLocalDemo(platform).buffer()
        dumpOnMismatch("macro_local", dslBytes, jsonBytes)
        assertArrayEquals("MacroLocal DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    @Test
    fun testRcDslMacroComparison() {
        val platform = MockPlatform()
        val dslBytes = RcMacroDemo().buffer()
        val jsonBytes = rcJsonMacroDemo(platform).buffer()
        dumpOnMismatch("macro", dslBytes, jsonBytes)
        assertArrayEquals("Macro DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    @Test
    fun testRcDslStyleMacroComparison() {
        val platform = MockPlatform()
        val dslBytes = RcStyleMacroDemo().buffer()
        val jsonBytes = rcJsonStyleMacroDemo(platform).buffer()
        dumpOnMismatch("style_macro", dslBytes, jsonBytes)
        assertArrayEquals("StyleMacro DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    @Test
    fun testRcDslReferencedOperationsMacroComparison() {
        val platform = MockPlatform()
        val dslBytes = RcReferencedOperationsMacroDemo().buffer()
        val jsonBytes = rcJsonReferencedOperationsMacroDemo(platform).buffer()
        dumpOnMismatch("referenced_operations_macro", dslBytes, jsonBytes)
        assertArrayEquals(
            "ReferencedOperationsMacro DSL and JSON should be identical",
            dslBytes,
            jsonBytes,
        )
    }

    @Test
    fun testRcDslPressureGaugeComparison() {
        val dslBytes = dslDemoPressureGauge()
        val jsonBytes = rcJsonPressureGauge().buffer()
        dumpOnMismatch("pressure_gauge", dslBytes, jsonBytes)
        assertArrayEquals("PressureGauge DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    @Test
    fun testRcDslLinearRegressionComparison() {
        val nPoints = 50
        val xData = FloatArray(nPoints) { it.toFloat() }
        val yData = FloatArray(nPoints) { 2f * it + 10f }

        val dslBytes = demoLinearRegression(xData, yData).buffer()
        val jsonBytes = rcJsonLinearRegression(xData, yData).buffer()
        val dslOps = decodeOpcodes(dslBytes).filter { !isVariableOrConstantDecl(it) }
        val jsonOps = decodeOpcodes(jsonBytes).filter { !isVariableOrConstantDecl(it) }
        assertEquals(
            "LinearRegression DSL and JSON must have the same number of operations",
            dslOps.size,
            jsonOps.size,
        )
        for (i in dslOps.indices) {
            val dslOp = normalizeVariableIds(dslOps[i])
            val jsonOp = normalizeVariableIds(jsonOps[i])
            assertEquals(
                "LinearRegression operation at index $i must match structurally",
                dslOp,
                jsonOp,
            )
        }
        dumpOnMismatch("LinearRegression", dslBytes, jsonBytes)
        assertArrayEquals("LinearRegression DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    @Test
    fun testRcDslGraphs2Comparison() {
        val dslWriter = demoGraphs2()
        val dslBytes = java.util.Arrays.copyOf(dslWriter.buffer(), dslWriter.bufferSize())
        val jsonBytes = rcJsonGraphs2().buffer()

        dumpOnMismatch("Graphs2", dslBytes, jsonBytes)
        assertArrayEquals("Graphs2 DSL and JSON should be identical", dslBytes, jsonBytes)
    }

    private fun isVariableOrConstantDecl(opString: String): Boolean {
        val s = opString.trim()
        return s.startsWith("FloatConstant") ||
            s.startsWith("FloatExpression") ||
            s.startsWith("VariableName") ||
            s.startsWith("TextData") ||
            s.startsWith("TextFromFloat") ||
            s.startsWith("TextMerge") ||
            s.startsWith("DataListFloat")
    }

    private fun decodeOpcodes(bytes: ByteArray): List<String> {
        val buffer = RemoteComposeBuffer()
        RemoteComposeBuffer.read(ByteArrayInputStream(bytes), buffer)
        val operations = ArrayList<Operation>()
        val captured = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(captured))
        try {
            buffer.inflateFromBuffer(operations)
        } finally {
            System.setOut(originalOut)
        }
        return operations.map { it.deepToString("  ") }
    }

    private fun normalizeVariableIds(opString: String): String {
        val s = opString.trim()
        if (s.startsWith("PaintData")) {
            return "  PaintData \"[PAINT]\""
        }
        return opString
            .replace(Regex("\\[[0-9]+\\]"), "[VAR]")
            .replace(Regex("FloatExpression\\[[0-9]+\\]"), "FloatExpression[VAR]")
            .replace(Regex("FloatConstant\\[[0-9]+\\]"), "FloatConstant[VAR]")
            .replace(Regex("TextFromFloat\\[[0-9]+\\]"), "TextFromFloat[VAR]")
            .replace(Regex("TextMerge\\[[0-9]+\\]"), "TextMerge[VAR]")
            .replace(Regex("TextData\\[[0-9]+\\]"), "TextData[VAR]")
            .replace(Regex("PathExpression\\[id=[0-9]+"), "PathExpression[id=VAR")
            .replace(Regex("DrawPath\\[[0-9]+"), "DrawPath[VAR")
            .replace(
                Regex("ComponentValue\\([0-9]+, -[0-9]+, [0-9]+\\)"),
                "ComponentValue(INDEX, ID, VAR)",
            )
    }

    private fun printResources(bytes: ByteArray, out: java.io.PrintWriter) {
        val doc = RemoteDocument(bytes)
        val state = doc.document.remoteComposeState
        val context = MockRemoteContext(doc.document)
        val operations = ArrayList<Operation>()
        val buffer = RemoteComposeBuffer()
        val inputStream = java.io.ByteArrayInputStream(bytes)
        RemoteComposeBuffer.read(inputStream, buffer)
        buffer.inflateFromBuffer(operations)
        for (op in operations) {
            op.apply(context)
        }

        for (i in 0..200) {
            val d = state.getFromId(i)
            if (d != null) {
                out.println("  [$i] = Data: $d")
            }
            val color = state.getColor(i)
            if (color != 0) {
                out.println("  [$i] = Color: 0x${Integer.toHexString(color)}")
            }
            val f = state.getFloat(i)
            if (!f.isNaN() && f != 0f) {
                out.println("  [$i] = Float: $f")
            }
            val intVal = state.getInteger(i)
            if (intVal != 0) {
                out.println("  [$i] = Int: $intVal")
            }
        }
    }

    private fun printOpcodes(bytes: ByteArray) {
        val buffer = RemoteComposeBuffer()
        val inputStream = java.io.ByteArrayInputStream(bytes)
        RemoteComposeBuffer.read(inputStream, buffer)
        val operations = ArrayList<androidx.compose.remote.core.Operation>()
        buffer.inflateFromBuffer(operations)
        for (op in operations) {
            val str = op.deepToString("  ")
            if (str == "null" || str.trim() == "null") {
                println("  NULL OPERATION: ${op.javaClass.name}")
            } else {
                println(str)
            }
        }
    }

    private fun printVariables(bytes: ByteArray) {
        // Simple search for NAMED_VARIABLE (137) or DATA_FLOAT (101) etc.
        val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.position(12)
        while (buffer.hasRemaining()) {
            val start = buffer.position()
            val opcode = buffer.get().toInt() and 0xFF
            val size = buffer.getShort().toInt() and 0xFFFF
            if (opcode == 137) { // NAMED_VARIABLE
                val id = buffer.getShort().toInt()
                val type = buffer.get().toInt()
                val nameLen = buffer.get().toInt()
                val nameBytes = ByteArray(nameLen)
                buffer.get(nameBytes)
                println("Named Variable: ID=$id, Type=$type, Name=${String(nameBytes)}")
            } else if (opcode == 138) { // NAMED_COLOR
                val id = buffer.getShort().toInt()
                val nameLen = buffer.get().toInt()
                val nameBytes = ByteArray(nameLen)
                buffer.get(nameBytes)
                println("Named Color: ID=$id, Name=${String(nameBytes)}")
            }
            buffer.position(start + size)
        }
    }

    private fun printOffsets(bytes: ByteArray, out: java.io.PrintWriter) {
        val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.position(29) // Skip header (1 byte opcode + 28 bytes data)
        while (buffer.hasRemaining()) {
            val start = buffer.position()
            val opcode = buffer.get().toInt() and 0xFF
            val size = buffer.getShort().toInt() and 0xFFFF
            out.println("Offset $start: Opcode $opcode, Size $size")
            if (size == 0) break // Prevent infinite loop on corrupt data
            buffer.position(start + size)
        }
    }

    private class MockRemoteContext(document: CoreDocument) : RemoteContext(RemoteClock.SYSTEM) {
        init {
            mDocument = document
        }

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {}

        override fun getPathData(instanceId: Int): FloatArray? = null

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {}

        override fun loadColor(id: Int, color: Int) {
            mRemoteComposeState.updateColor(id, color)
        }

        override fun setNamedColorOverride(colorName: String, color: Int) {}

        override fun setNamedStringOverride(stringName: String, value: String) {}

        override fun clearNamedStringOverride(stringName: String) {}

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {}

        override fun clearNamedBooleanOverride(booleanName: String) {}

        override fun setNamedIntegerOverride(integerName: String, value: Int) {}

        override fun clearNamedIntegerOverride(integerName: String) {}

        override fun setNamedFloatOverride(floatName: String, value: Float) {}

        override fun clearNamedFloatOverride(floatName: String) {}

        override fun setNamedLong(name: String, value: Long) {}

        override fun setNamedDataOverride(dataName: String, value: Any) {}

        override fun clearNamedDataOverride(dataName: String) {}

        override fun addCollection(id: Int, collection: ArrayAccess) {}

        override fun putDataMap(id: Int, map: DataMap) {}

        override fun getDataMap(id: Int): DataMap? = null

        override fun runAction(id: Int, metadata: String) {}

        override fun runNamedAction(id: Int, value: Any?) {}

        override fun putObject(id: Int, value: Any) {
            mRemoteComposeState.updateData(id, value)
        }

        override fun getObject(id: Int): Any? = mRemoteComposeState.getObject(id)

        override fun hapticEffect(type: Int) {}

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {}

        override fun loadText(id: Int, text: String) {
            mRemoteComposeState.updateData(id, text)
        }

        override fun getText(id: Int): String? = mRemoteComposeState.getFromId(id) as String?

        override fun loadFloat(id: Int, value: Float) {
            mRemoteComposeState.updateFloat(id, value)
        }

        override fun overrideFloat(id: Int, value: Float) {
            mRemoteComposeState.updateFloat(id, value)
        }

        override fun loadInteger(id: Int, value: Int) {
            mRemoteComposeState.updateInteger(id, value)
        }

        override fun overrideInteger(id: Int, value: Int) {
            mRemoteComposeState.updateInteger(id, value)
        }

        override fun overrideText(id: Int, valueId: Int) {}

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {}

        override fun loadShader(id: Int, value: ShaderData) {}

        override fun getFloat(id: Int): Float = mRemoteComposeState.getFloat(id)

        override fun getInteger(id: Int): Int = mRemoteComposeState.getInteger(id)

        override fun getLong(id: Int): Long = 0

        override fun getColor(id: Int): Int = mRemoteComposeState.getColor(id)

        override fun listensTo(id: Int, variableSupport: VariableSupport) {}

        override fun updateOps(): Int = 0

        override fun getShader(id: Int): ShaderData? = null

        override fun addClickArea(
            id: Int,
            contentId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {}
    }

    @Test
    fun testFullCubeComparison() {
        val platform = MockPlatform()
        val tags =
            arrayOf(
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Cube"),
                RemoteComposeWriter.hTag(Header.DOC_DESIRED_FPS, 120),
            )
        tags.sortBy { it.mTag }
        val expectedWriter = RemoteComposeWriter(platform, 7, *tags)
        generateFullCubeKotlin(expectedWriter)
        val expected = expectedWriter.encodeToByteArray()

        val json = getFullCubeJson()
        val actualTags = RemoteComposeJsonParser.parseHeaderOnly(json)
        val actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json)
        val actualWriter = RemoteComposeWriter(platform, actualApiLevel, *actualTags!!)
        val parser = RemoteComposeJsonParser(actualWriter)
        parser.parse(json)
        val actual = actualWriter.encodeToByteArray()

        dumpOnMismatch("cube", expected, actual)
        assertArrayEquals(expected, actual)
    }

    private fun generateFullCubeKotlin(writer: RemoteComposeWriter) {
        val centerX =
            writer.floatExpression(writer.addComponentWidthValue(), 2f, AnimatedFloatExpression.DIV)
        val centerY =
            writer.floatExpression(
                writer.addComponentHeightValue(),
                2f,
                AnimatedFloatExpression.DIV,
            )
        val radius = writer.floatExpression(centerX, centerY, AnimatedFloatExpression.MIN)
        val time = RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC
        val rot =
            writer.floatExpression(
                time,
                2f,
                AnimatedFloatExpression.MUL,
                20f,
                AnimatedFloatExpression.MUL,
                360f,
                AnimatedFloatExpression.MOD,
            )
        val t1 =
            writer.floatExpression(
                time,
                2f,
                AnimatedFloatExpression.MUL,
                18f,
                AnimatedFloatExpression.DIV,
                AnimatedFloatExpression.ROUND,
                1f,
                AnimatedFloatExpression.ADD,
                3f,
                AnimatedFloatExpression.MOD,
                AnimatedFloatExpression.SIGN,
            )
        val t2 =
            writer.floatExpression(
                time,
                2f,
                AnimatedFloatExpression.MUL,
                18f,
                AnimatedFloatExpression.DIV,
                AnimatedFloatExpression.ROUND,
                3f,
                AnimatedFloatExpression.MOD,
                AnimatedFloatExpression.SIGN,
            )

        val rotX = writer.floatExpression(rot, t1, AnimatedFloatExpression.MUL)
        val rotY = writer.floatExpression(rot, t2, AnimatedFloatExpression.MUL)

        val pMatX = writer.floatExpression(centerX, 0.4f, AnimatedFloatExpression.MUL)
        val pMatY = writer.floatExpression(centerX, -0.4f, AnimatedFloatExpression.MUL)

        val worldId =
            writer.matrixExpression(
                6f,
                MatrixOperations.TRANSLATE_Z,
                rotX,
                MatrixOperations.ROT_X,
                rotY,
                MatrixOperations.ROT_Y,
            )
        val pMatrixId =
            writer.matrixExpression(
                60f,
                1f,
                0.1f,
                100f,
                MatrixOperations.PROJECTION,
                pMatX,
                pMatY,
                MatrixOperations.SCALE2,
            )

        writer.getBuffer().addRootStart()
        val boxMod = RecordingModifier()
        boxMod.fillMaxWidth(1.0f).fillMaxHeight(1.0f)
        writer.startBox(boxMod, BoxLayout.CENTER, BoxLayout.CENTER)

        val canvasMod = RecordingModifier()
        canvasMod.fillMaxWidth(1.0f).fillMaxHeight(1.0f)
        writer.startCanvas(canvasMod)

        writer.getRcPaint().setColor(0xFF444444.toInt()).setStyle(1).commit()
        writer.drawCircle(centerX, centerY, radius)

        writer.getRcPaint().setColor(0xFFD3D3D3.toInt()).commit()

        val v0 =
            floatArrayOf(
                writer.createNamedVariable("v0x", NamedVariable.FLOAT_TYPE),
                writer.createNamedVariable("v0y", NamedVariable.FLOAT_TYPE),
                writer.createNamedVariable("v0z", NamedVariable.FLOAT_TYPE),
            )
        writer.addMatrixMultiply(worldId, 0.toShort(), floatArrayOf(-1f, -1f, -1f), v0)

        val t0 =
            floatArrayOf(
                writer.createNamedVariable("t0x", NamedVariable.FLOAT_TYPE),
                writer.createNamedVariable("t0y", NamedVariable.FLOAT_TYPE),
                writer.createNamedVariable("t0z", NamedVariable.FLOAT_TYPE),
            )
        writer.addMatrixMultiply(pMatrixId, 1.toShort(), v0, t0)

        val f0 =
            writer.pathCreate(
                writer.floatExpression(t0[0], centerX, AnimatedFloatExpression.ADD),
                writer.floatExpression(t0[1], centerY, AnimatedFloatExpression.ADD),
            )
        writer.pathAppendClose(f0)

        writer.endCanvas()
        writer.endBox()
        writer.getBuffer().addContainerEnd()
    }

    private fun getFullCubeJson(): String {
        return """
        {
          "header": { "apiLevel": 7, "width": 400, "height": 400, "contentDescription": "Cube", "fps": 120 },
          "resources": {
            "order": ["variables", "matrices"],
            "variables": [
              { "name": "centerX", "value": "width / 2" },
              { "name": "centerY", "value": "height / 2" },
              { "name": "radius", "value": "min(@centerX, @centerY)" },
              { "name": "rot", "value": "time * 2 * 20 % 360" },
              { "name": "t1", "value": "sign((round(time * 2 / 18) + 1) % 3)" },
              { "name": "t2", "value": "sign(round(time * 2 / 18) % 3)" },
              { "name": "rotX", "value": "@rot * @t1" },
              { "name": "rotY", "value": "@rot * @t2" },
              { "name": "pMatX", "value": "@centerX * 0.4" },
              { "name": "pMatY", "value": "@centerX * -0.4" }
            ],
            "matrices": [
              { "name": "world", "value": [ 6, "matrix:TRANSLATE_Z", "@rotX", "matrix:ROT_X", "@rotY", "matrix:ROT_Y" ] },
              { "name": "pMatrix", "value": [ 60, 1, 0.1, 100, "matrix:PROJECTION", "@pMatX", "@pMatY", "matrix:SCALE2" ] }
            ]
          },
          "root": {
            "type": "box",
            "horizontalAlignment" : "center",
            "verticalAlignment" : "center",
            "modifiers": [ { "fillMaxWidth": 1.0 }, { "fillMaxHeight": 1.0 } ],
            "children": [
              {
                "type": "canvas",
                "modifiers": [ { "fillMaxWidth": 1.0 }, { "fillMaxHeight": 1.0 } ],
                "commands": [
                  { "type": "paint", "color": "#444444", "style": "stroke" },
                  { "type": "drawCircle", "cx": "@centerX", "cy": "@centerY", "radius": "@radius" },
                  { "type": "paint", "color": "#D3D3D3" },
                  { "type": "matrixMultiply", "matrix": "@matrices.world", "from": [-1, -1, -1], "out": ["v0x", "v0y", "v0z"] },
                  { "type": "matrixMultiply", "matrix": "@matrices.pMatrix", "mType": 1, "from": ["@v0x", "@v0y", "@v0z"], "out": ["t0x", "t0y", "t0z"] },
                  { "type": "pathCreate", "x": "@t0x + @centerX", "y": "@t0y + @centerY", "id": "f0" },
                  { "type": "pathAppendClose", "path": "@paths.f0" }
                ]
              }
            ]
          }
        }
        """
            .trimIndent()
    }

    @Test
    fun testTickerComparison() {
        println("### FORCINGDemosRebuild ###")
        val platform = MockPlatform()
        val tags =
            arrayOf(
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 800),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Ticker"),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, 769),
            )
        tags.sortBy { it.mTag }
        val expected = generateTickerDsl(tags)

        val json = getTickerJson()
        val actualTags = RemoteComposeJsonParser.parseHeaderOnly(json)
        val actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json)
        val actualWriter = RemoteComposeWriter(platform, actualApiLevel, *actualTags!!)
        val parser = RemoteComposeJsonParser(actualWriter)
        parser.parse(json)
        val actual = actualWriter.encodeToByteArray()

        dumpOnMismatch("ticker", expected, actual)
        assertArrayEquals(expected, actual)
    }

    @Suppress("RestrictedApiAndroidX")
    fun generateTickerDsl(tags: Array<RemoteComposeWriter.HTag>): ByteArray {
        return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX7), *tags, experimental = true) {
            // 1. Global themed colors & variables declarations
            var bg: RcColor? = null
            var priceText: RcText? = null
            Global {
                val lightColor = remoteColor(0xFFEEEEEE.toInt())
                val darkColor = remoteColor(0xFF111111.toInt())
                bg = remoteThemedColor(lightColor, darkColor)
                priceText = 123.45f.format(whole = 0, decimal = 2, flags = 0)
            }

            // 2. Root layout Column
            Column(
                modifier = Modifier.fillMaxSize(1f).background(bg!!),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                // 3. Scrollable inner Column
                Column(
                    modifier = Modifier.fillMaxWidth(1f).height(400f).verticalScroll(0f.rf),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    // 4. Text Component
                    Text(
                        text = priceText!!,
                        color = 0xFF000000.toInt(),
                        fontSize = 24.rsp,
                        maxLines = 1,
                    )

                    // 5. Canvas with looping path
                    Canvas(modifier = Modifier.fillMaxWidth(1f).height(100f)) {
                        val path = remotePath(0f, 100f)
                        loop(0f.rf, 10f.rf, 100f.rf) { index ->
                            val expression = index * index * 0.01f
                            path.lineTo(index.toFloat(), expression.toFloat())
                        }
                        paint {
                            color(0xFFFF0000.toInt())
                            style(RcPaintStyle.Stroke)
                            strokeWidth(2f)
                        }
                        drawPath(path.getPath())
                    }
                }
            }
        }
    }

    private fun getTickerJson(): String {
        return """
        {
          "header": { "apiLevel": 7, "profiles": 769, "width": 400, "height": 800, "contentDescription": "Ticker" },
          "resources": {
            "beginGlobal": true,
            "colors": [
              { "name": "bg", "value": { "light": "#EEEEEE", "dark": "#111111" }, "export": false }
            ],
            "endGlobal": true,
            "variables": [
              { "name": "priceText", "value": { "type": "textFromFloat", "value": 123.45, "after": 2 } }
            ]
          },
          "root": {
            "type": "column",
            "horizontalAlignment" : "center",
            "verticalAlignment" : "center",
            "modifiers": [
              { "fillMaxSize": 1.0 },
              { "background": "@colors.bg" }
            ],
            "children": [
              {
                "type": "column",
                "horizontalAlignment": "center",
                "verticalAlignment": "center",
                "modifiers": [
                  { "fillMaxWidth": 1.0 },
                  { "height": 400 },
                  { "verticalScroll": 0.0 }
                ],
                "children": [
                  {
                    "type": "text",
                    "value": "@priceText",
                    "fontSize": 24,
                    "maxLines": 1
                  },
                  {
                    "type": "canvas",
                    "modifiers": [
                      { "fillMaxWidth": 1.0 },
                      { "height": 100 }
                    ],
                    "commands": [
                      { "type": "pathCreate", "x": 0, "y": 100, "id": "p1" },
                      {
                        "type": "loop",
                        "from": 0, "step": 10, "until": 100,
                        "index": "index",
                        "commands": [
                          { "type": "pathAppendLineTo", "path": "@paths.p1", "x": "@index", "y": "@index * @index * 0.01" }
                        ]
                      },
                      { "type": "paint", "color": "#FF0000", "style": "stroke", "width": 2 },
                      { "type": "drawPath", "path": "@paths.p1" }
                    ]
                  }
                ]
              }
            ]
          }
        }
        """
            .trimIndent()
    }

    @Test
    fun testFitBoxComparison() {
        val platform = MockPlatform()
        val tags =
            arrayOf(
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "FitBox"),
            )
        tags.sortBy { it.mTag }
        val expectedWriter = RemoteComposeWriter(platform, 7, *tags)
        generateFitBoxKotlin(expectedWriter)
        val expected = expectedWriter.encodeToByteArray()

        val json = getFitBoxJson()
        val actualTags = RemoteComposeJsonParser.parseHeaderOnly(json)
        val actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json)
        val actualWriter = RemoteComposeWriter(platform, actualApiLevel, *actualTags!!)
        val parser = RemoteComposeJsonParser(actualWriter)
        parser.parse(json)
        val actual = actualWriter.encodeToByteArray()

        dumpOnMismatch("fitbox", expected, actual)
        assertArrayEquals(expected, actual)
    }

    private fun generateFitBoxKotlin(writer: RemoteComposeWriter) {
        val modifier = RecordingModifier()
        modifier.fillMaxSize(1.0f).background(0xFF0000FF.toInt())
        writer.getBuffer().addRootStart()
        writer.startFitBox(modifier, BoxLayout.CENTER, BoxLayout.CENTER)

        writer.startColumn(RecordingModifier(), BoxLayout.CENTER, BoxLayout.CENTER)
        val textId = writer.addText("Fitted Text")
        writer.startTextComponent(
            RecordingModifier(),
            textId,
            0xFFFFFFFF.toInt(),
            100f,
            0,
            400f,
            null,
            5, // textAlign start
            1, // overflow clip
            1, // maxlines
        )
        writer.endTextComponent()
        writer.endColumn()

        writer.endFitBox()
        writer.getBuffer().addContainerEnd()
    }

    private fun getFitBoxJson(): String {
        return """
        {
          "header": { "apiLevel": 7, "width": 400, "height": 400, "contentDescription": "FitBox" },
          "root": {
            "type": "fitBox",
            "horizontalAlignment": "center",
            "verticalAlignment": "center",
            "modifiers": [ { "fillMaxSize": 1.0 }, { "background": "#0000FF" } ],
            "children": [
              {
                "type": "column",
                "horizontalAlignment": "center",
                "verticalAlignment": "center",
                "children": [
                  {
                    "type": "text",
                    "value": "Fitted Text",
                    "color": "#FFFFFF",
                    "fontSize": 100,
                    "maxLines" : 1
                  }
                ]
              }
            ]
          }
        }
        """
            .trimIndent()
    }
}

private fun Modifier.fillMaxWidth(fraction: Float): Modifier =
    then(
        object : Modifier.Element {
            override fun applyTo(modifier: RecordingModifier) {
                modifier.fillMaxWidth(fraction)
            }
        }
    )

private fun Modifier.fillMaxHeight(fraction: Float): Modifier =
    then(
        object : Modifier.Element {
            override fun applyTo(modifier: RecordingModifier) {
                modifier.fillMaxHeight(fraction)
            }
        }
    )

private fun Modifier.fillMaxSize(fraction: Float): Modifier =
    fillMaxWidth(fraction).fillMaxHeight(fraction)
