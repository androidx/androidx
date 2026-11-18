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

package androidx.compose.remote.generator

import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.generator.ast.BitmapDataNode
import androidx.compose.remote.generator.ast.BoxLayoutNode
import androidx.compose.remote.generator.ast.ContainerEndNode
import androidx.compose.remote.generator.ast.DocumentHeader
import androidx.compose.remote.generator.ast.DocumentNode
import androidx.compose.remote.generator.ast.DrawCircleNode
import androidx.compose.remote.generator.ast.DrawRectNode
import androidx.compose.remote.generator.ast.FloatConstantNode
import androidx.compose.remote.generator.ast.NamedVariableNode
import androidx.compose.remote.generator.ast.PaddingModifierNode
import androidx.compose.remote.generator.ast.RemoteDocument
import androidx.compose.remote.generator.ast.TextDataNode
import androidx.compose.remote.generator.codegen.CodegenExprUtils
import androidx.compose.remote.generator.codegen.CppGenerator
import androidx.compose.remote.generator.codegen.ExprLang
import androidx.compose.remote.generator.codegen.JavaGenerator
import androidx.compose.remote.generator.codegen.KotlinGenerator
import androidx.compose.remote.generator.codegen.KotlinProtocolAndSinkGenerator
import androidx.compose.remote.generator.codegen.PythonGenerator
import androidx.compose.remote.generator.codegen.RustGenerator
import androidx.compose.remote.generator.codegen.SwiftGenerator
import androidx.compose.remote.generator.codegen.TypeScriptGenerator
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Test

class RemoteComposeGeneratorTest {

    private fun loadSchema(): OperationSchema {
        val stream = javaClass.getResourceAsStream("/schema/operations.json")
        assertNotNull("Failed to locate /schema/operations.json in classpath", stream)
        return JsonSchemaParser.parse(stream!!)
    }

    @Test
    fun testOperationsJsonSortedById() {
        val stream = javaClass.getResourceAsStream("/schema/operations.json")
        assertNotNull("Failed to locate /schema/operations.json in classpath", stream)
        val text = stream!!.bufferedReader().readText()
        val json = JSONObject(text)
        val ops = json.getJSONArray("operations")
        var previousId = -1
        for (i in 0 until ops.length()) {
            val op = ops.getJSONObject(i)
            val id = op.getInt("id")
            val name = op.getString("name")
            assertTrue(
                "operations.json must be sorted by ID: $name ($id) must be greater than previous ID $previousId at index $i",
                id > previousId,
            )
            assertTrue(
                "operations.json operation $name must have restrictToLibraryGroup set to true",
                op.optBoolean("restrictToLibraryGroup", false),
            )
            previousId = id
        }
    }

    @Test
    fun testOperationsJsonMatchesMetaSchema() {
        val stream =
            RemoteComposeGeneratorTest::class
                .java
                .classLoader
                ?.getResourceAsStream("schema/operations.json")
                ?: File("src/main/resources/schema/operations.json").inputStream()
        val text = stream.bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val errors = JsonSchemaParser.validate(root)
        assertTrue(
            "Schema validation failed with errors:\n" + errors.joinToString("\n"),
            errors.isEmpty(),
        )

        val metaStream =
            RemoteComposeGeneratorTest::class
                .java
                .classLoader
                ?.getResourceAsStream("schema/schema.json")
                ?: File("src/main/resources/schema/schema.json").inputStream()
        val metaText = metaStream.bufferedReader().use { it.readText() }
        val metaRoot = JSONObject(metaText)
        assertTrue(metaRoot.has("definitions"))
        assertTrue(metaRoot.has("properties"))
    }

    @Test
    fun testPaintCommandsAlignmentWithPaintBundle() {
        val schema = loadSchema()
        assertTrue("paintCommands must not be empty", schema.paintCommands.isNotEmpty())

        val expectedCommands =
            mapOf(
                PaintBundle.TEXT_SIZE to "TextSize",
                PaintBundle.COLOR to "Color",
                PaintBundle.STROKE_WIDTH to "StrokeWidth",
                PaintBundle.STROKE_MITER to "StrokeMiter",
                PaintBundle.STROKE_CAP to "StrokeCap",
                PaintBundle.STYLE to "Style",
                PaintBundle.SHADER to "Shader",
                PaintBundle.IMAGE_FILTER_QUALITY to "ImageFilterQuality",
                PaintBundle.GRADIENT to "Gradient",
                PaintBundle.ALPHA to "Alpha",
                PaintBundle.COLOR_FILTER to "ColorFilter",
                PaintBundle.ANTI_ALIAS to "AntiAlias",
                PaintBundle.STROKE_JOIN to "StrokeJoin",
                PaintBundle.TYPEFACE to "Typeface",
                PaintBundle.FILTER_BITMAP to "FilterBitmap",
                PaintBundle.BLEND_MODE to "BlendMode",
                PaintBundle.COLOR_ID to "ColorId",
                PaintBundle.COLOR_FILTER_ID to "ColorFilterId",
                PaintBundle.CLEAR_COLOR_FILTER to "ClearColorFilter",
                PaintBundle.SHADER_MATRIX to "ShaderMatrix",
                PaintBundle.FONT_AXIS to "FontAxis",
                PaintBundle.TEXTURE to "Texture",
                PaintBundle.PATH_EFFECT to "PathEffect",
                PaintBundle.FALLBACK_TYPEFACE to "FallbackTypeface",
            )

        for ((id, name) in expectedCommands) {
            val cmd = schema.paintCommands[id]
            assertNotNull("Paint command $name ($id) missing from schema", cmd)
            assertEquals("Paint command name mismatch for id $id", name, cmd!!.name)
        }
    }

    @Test
    fun testRpnOperatorsAlignmentWithAnimatedFloatExpression() {
        val schema = loadSchema()
        val rpn = schema.rpnOperators
        assertNotNull("rpnOperators must not be null in schema", rpn)
        assertEquals(AnimatedFloatExpression.OFFSET, rpn!!.offset)

        val expectedOperators =
            mapOf(
                1 to "ADD",
                2 to "SUB",
                3 to "MUL",
                4 to "DIV",
                5 to "MOD",
                6 to "MIN",
                7 to "MAX",
                8 to "POW",
                9 to "SQRT",
                10 to "ABS",
                11 to "SIGN",
                12 to "COPY_SIGN",
                13 to "EXP",
                14 to "FLOOR",
                15 to "LOG",
                16 to "LN",
                17 to "ROUND",
                18 to "SIN",
                19 to "COS",
                20 to "TAN",
                21 to "ASIN",
                22 to "ACOS",
                23 to "ATAN",
                24 to "ATAN2",
                25 to "MAD",
                26 to "IFELSE",
                27 to "CLAMP",
                28 to "CBRT",
                29 to "DEG_TO_RAD",
                30 to "RAD_TO_DEG",
                31 to "CEIL",
            )

        for ((id, name) in expectedOperators) {
            val op = rpn.operatorsById[id]
            assertNotNull("RPN operator $name ($id) missing from schema", op)
            assertEquals("RPN operator name mismatch for id $id", name, op!!.name)
        }
    }

    @Test
    fun testPathCommandsAlignmentWithPathData() {
        val schema = loadSchema()
        assertTrue("pathCommands must not be empty", schema.pathCommands.isNotEmpty())

        val expectedCommands =
            mapOf(
                PathData.MOVE to "Move",
                PathData.LINE to "Line",
                PathData.QUADRATIC to "Quadratic",
                PathData.CONIC to "Conic",
                PathData.CUBIC to "Cubic",
                PathData.CLOSE to "Close",
                PathData.DONE to "Done",
            )

        for ((id, name) in expectedCommands) {
            val cmd = schema.pathCommands[id]
            assertNotNull("Path command $name ($id) missing from schema", cmd)
            assertEquals("Path command name mismatch for id $id", name, cmd!!.name)
        }
    }

    @Test
    fun testStructsAndAllowedChildren() {
        val schema = loadSchema()
        assertTrue(
            "structs must contain GraphicsLayerAttribute",
            schema.structs.containsKey("GraphicsLayerAttribute"),
        )
        assertTrue(
            "structs must contain ShaderFloatUniform",
            schema.structs.containsKey("ShaderFloatUniform"),
        )

        val componentStart = schema.operationsByName["ComponentStart"]
        assertNotNull(componentStart)
        assertTrue(componentStart!!.isContainer)
        assertNotNull(componentStart.allowedChildren)
        assertTrue(componentStart.allowedChildren!!.contains("ComponentEnd"))
    }

    @Test
    fun testLongEncodedIdsAndContainerHierarchy() {
        val schema = loadSchema()

        // ValueIntegerExpressionChangeActionOperation (218)
        val valueIntExpr = schema.operationsByName["ValueIntegerExpressionChangeActionOperation"]
        assertNotNull(
            "ValueIntegerExpressionChangeActionOperation missing from schema",
            valueIntExpr,
        )
        assertEquals(218, valueIntExpr!!.id)
        val targetValueId = valueIntExpr.fields.firstOrNull { it.name == "targetValueId" }
        assertNotNull(targetValueId)
        assertEquals("idRef", targetValueId!!.role)
        assertEquals("integerVariable", targetValueId.idKind)
        assertEquals("longEncodedId", targetValueId.encoding)
        assertEquals("0x100000000", targetValueId.offset)
        assertEquals(30, targetValueId.lookupBit)

        val valueExprId = valueIntExpr.fields.firstOrNull { it.name == "valueExpressionId" }
        assertNotNull(valueExprId)
        assertEquals("idRef", valueExprId!!.role)
        assertEquals("integerExpression", valueExprId.idKind)
        assertEquals("longEncodedId", valueExprId.encoding)
        assertEquals("0x100000000", valueExprId.offset)
        assertEquals(30, valueExprId.lookupBit)

        // LongConstant (148)
        val longConst = schema.operationsByName["LongConstant"]
        assertNotNull(longConst)
        assertEquals(148, longConst!!.id)
        val longVal = longConst.fields.firstOrNull { it.name == "value" }
        assertNotNull(longVal)
        assertEquals("longEncodedId", longVal!!.encoding)
        assertEquals("0x100000000", longVal.offset)
        assertEquals(30, longVal.lookupBit)

        // Containers: ClickModifier & Loop
        val clickMod = schema.operationsByName["ClickModifier"]
        assertNotNull(clickMod)
        assertTrue(clickMod!!.isContainer)
        assertEquals("ContainerEnd", clickMod.closedBy)
        assertNotNull(clickMod.allowedChildren)
        assertTrue(
            clickMod.allowedChildren!!.contains("ValueIntegerExpressionChangeActionOperation")
        )

        val loopOp = schema.operationsByName["Loop"]
        assertNotNull(loopOp)
        assertTrue(loopOp!!.isContainer)
        assertEquals("ContainerEnd", loopOp.closedBy)
    }

    @Test
    fun testLoadOperationsJson() {
        val schema = loadSchema()
        assertEquals("1.0.0", schema.schemaVersion)
        assertEquals(8, schema.targetApiLevel)
        assertTrue(schema.categories.isNotEmpty())
        assertTrue(schema.operations.isNotEmpty())

        val drawCircle = schema.operationsByName["DrawCircle"]
        assertNotNull(drawCircle)
        assertEquals(Operations.DRAW_CIRCLE, drawCircle!!.id)
        assertEquals("Canvas Operations", drawCircle.category)
        assertEquals(3, drawCircle.fields.size)
        assertEquals("centerX", drawCircle.fields[0].name)
        assertEquals("centerY", drawCircle.fields[1].name)
        assertEquals("radius", drawCircle.fields[2].name)

        val boxLayout = schema.operationsByName["BoxLayout"]
        assertNotNull(boxLayout)
        assertEquals(Operations.LAYOUT_BOX, boxLayout!!.id)
        assertTrue(boxLayout.isContainer)
        assertEquals("ContainerEnd", boxLayout.closedBy)
    }

    @Test
    fun testOpcodeAlignmentWithOperationsJava() {
        val schema = loadSchema()
        val expectedOpcodes =
            mapOf(
                "Header" to Operations.HEADER,
                "Theme" to Operations.THEME,
                "FloatConstant" to Operations.DATA_FLOAT,
                "IntegerConstant" to Operations.DATA_INT,
                "BooleanConstant" to Operations.DATA_BOOLEAN,
                "NamedVariable" to Operations.NAMED_VARIABLE,
                "DrawRect" to Operations.DRAW_RECT,
                "DrawCircle" to Operations.DRAW_CIRCLE,
                "DrawLine" to Operations.DRAW_LINE,
                "DrawRoundRect" to Operations.DRAW_ROUND_RECT,
                "DrawText" to Operations.DRAW_TEXT_RUN,
                "PaddingModifierOperation" to Operations.MODIFIER_PADDING,
                "WidthModifierOperation" to Operations.MODIFIER_WIDTH,
                "HeightModifierOperation" to Operations.MODIFIER_HEIGHT,
                "BackgroundModifierOperation" to Operations.MODIFIER_BACKGROUND,
                "ComponentStart" to Operations.COMPONENT_START,
                "ContainerEnd" to Operations.CONTAINER_END,
                "BoxLayout" to Operations.LAYOUT_BOX,
                "RowLayout" to Operations.LAYOUT_ROW,
                "ColumnLayout" to Operations.LAYOUT_COLUMN,
                "EventHandlerModifierOperation" to Operations.MODIFIER_EVENT_HANDLER,
                "BinaryData" to Operations.BINARY_DATA,
                "RcDocLayout" to Operations.LAYOUT_RC_DOC,
                "MapRcDocVariables" to Operations.MAP_RC_DOC_VARIABLES,
            )

        for ((name, expectedOpcode) in expectedOpcodes) {
            val op = schema.operationsByName[name]
            assertNotNull("Operation $name missing from schema", op)
            assertEquals("Opcode mismatch for $name", expectedOpcode, op!!.id)
        }
    }

    @Test
    fun testDocumentAstRoundTrip() {
        val header =
            DocumentHeader(
                majorVersion = 1,
                minorVersion = 0,
                patchVersion = 0,
                width = 400,
                height = 800,
                capabilities = 0x200L,
            )

        val floatConst = FloatConstantNode(id = 10, value = 3.14159f)
        val namedVar = NamedVariableNode(varId = 10, varType = 1, name = "pi")

        val box =
            BoxLayoutNode(
                componentId = 1,
                animationId = 0,
                horizontalPositioning = 1,
                verticalPositioning = 1,
                modifiers =
                    listOf(PaddingModifierNode(left = 16f, top = 16f, right = 16f, bottom = 16f)),
                children =
                    listOf(
                        DrawCircleNode(centerX = 200f, centerY = 200f, radius = 50f),
                        DrawRectNode(left = 10f, top = 10f, right = 100f, bottom = 100f),
                    ),
            )

        val originalDoc =
            RemoteDocument(
                header = header,
                nodes = listOf(floatConst, namedVar, box),
            )

        // Save to wire bytes
        val savedBytes = originalDoc.save()
        assertTrue("Saved bytes should not be empty", savedBytes.isNotEmpty())

        // Load back from wire bytes
        val loadedDoc = RemoteDocument.load(savedBytes)

        // Introspect header
        assertEquals(1, loadedDoc.header.majorVersion)
        assertEquals(0, loadedDoc.header.minorVersion)
        assertEquals(400, loadedDoc.header.width)
        assertEquals(800, loadedDoc.header.height)

        // Introspect root nodes
        assertEquals(3, loadedDoc.nodes.size)
        assertEquals(floatConst, loadedDoc.nodes[0])
        assertEquals(namedVar, loadedDoc.nodes[1])

        // Introspect container hierarchy
        val loadedBox = loadedDoc.nodes[2] as BoxLayoutNode
        assertEquals(1, loadedBox.componentId)
        assertEquals(1, loadedBox.horizontalPositioning)
        assertEquals(1, loadedBox.verticalPositioning)

        // Introspect modifiers and children
        assertEquals(1, loadedBox.modifiers.size)
        assertEquals(PaddingModifierNode(16f, 16f, 16f, 16f), loadedBox.modifiers[0])
        assertEquals(2, loadedBox.children.size)
        assertEquals(DrawCircleNode(200f, 200f, 50f), loadedBox.children[0])
        assertEquals(DrawRectNode(10f, 10f, 100f, 100f), loadedBox.children[1])

        // Verify full equality
        assertEquals(originalDoc, loadedDoc)
    }

    @Test
    fun testMaxContainerDepthExceeded() {
        val maxDepth = RemoteDocument.MAX_CONTAINER_DEPTH
        val buffer = WireBuffer()
        val header = DocumentHeader(1, 0, 0, 100, 100)
        header.write(buffer)
        for (i in 0 until maxDepth + 1) {
            buffer.start(Operations.LAYOUT_BOX)
            buffer.writeInt(i)
            buffer.writeInt(0)
            buffer.writeInt(0)
            buffer.writeInt(0)
        }
        for (i in 0 until maxDepth + 1) {
            ContainerEndNode.write(buffer)
        }
        buffer.setIndex(0)
        try {
            RemoteDocument.load(buffer, maxDepth = maxDepth)
            org.junit.Assert.fail(
                "Expected IllegalArgumentException when nesting exceeds MAX_CONTAINER_DEPTH"
            )
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Exceeded maximum container nesting depth") == true)
        }
    }

    @Test
    fun testDeeplyNestedIterativeAst() {
        val deepNestingLevel = 500
        var current: DocumentNode = DrawCircleNode(10f, 10f, 5f)
        for (i in 0 until deepNestingLevel) {
            current =
                BoxLayoutNode(
                    componentId = i,
                    animationId = 0,
                    horizontalPositioning = 0,
                    verticalPositioning = 0,
                    modifiers = emptyList(),
                    children = listOf(current),
                )
        }
        val buffer = WireBuffer()
        // Iterative write should not throw StackOverflowError
        current.write(buffer)
        assertTrue(buffer.size() > 0)

        // Iterative hashCode should not throw StackOverflowError
        val hash = current.hashCode()
        assertNotNull(hash)

        // Iterative toString should not throw StackOverflowError
        val str = current.toString()
        assertTrue(str.startsWith("BoxLayoutNode("))

        // Iterative equals should not throw StackOverflowError
        var currentCopy: DocumentNode = DrawCircleNode(10f, 10f, 5f)
        for (i in 0 until deepNestingLevel) {
            currentCopy =
                BoxLayoutNode(
                    componentId = i,
                    animationId = 0,
                    horizontalPositioning = 0,
                    verticalPositioning = 0,
                    modifiers = emptyList(),
                    children = listOf(currentCopy),
                )
        }
        assertEquals(current, currentCopy)
    }

    @Test
    fun testRustGeneratorOutput() {
        val schema = loadSchema()
        val generator = RustGenerator(schema)

        val rustEnum = generator.generateOpIdEnum()
        assertTrue(rustEnum.contains("DrawCircle = 46,"))
        assertTrue(rustEnum.contains("BoxLayout = 202,"))
        assertTrue(rustEnum.contains("Header = 0,"))
        assertTrue(rustEnum.contains("pub fn from_u8(value: u8) -> Option<Self>"))

        val drawCircleOp = schema.operationsByName["DrawCircle"]!!
        val rustStruct = generator.generateStruct(drawCircleOp)
        assertTrue(rustStruct.contains("pub struct DrawCircle"))
        assertTrue(rustStruct.contains("pub center_x: f32,"))
        assertTrue(rustStruct.contains("pub center_y: f32,"))
        assertTrue(rustStruct.contains("pub radius: f32,"))
        val fullCode = generator.generate()
        assertTrue(fullCode.contains("pub enum Operation {"))
        assertTrue(fullCode.contains("DrawCircle(DrawCircle),"))
        assertTrue(fullCode.contains("pub struct RemoteDocument {"))
        assertTrue(
            fullCode.contains("pub fn load(buffer: &mut WireBuffer) -> Result<Self, WireError>")
        )
        assertTrue(fullCode.contains("pub fn save(&self, buffer: &mut WireBuffer)"))
        assertTrue(fullCode.contains("pub struct WireBuffer {"))
        assertTrue(fullCode.contains("if args_raw_len < 0 {"))
        assertTrue(fullCode.contains("WireError::InvalidData"))
        assertTrue(
            fullCode.contains(
                "if num_props < 0 || (num_props as usize) > Self::MAX_HEADER_PROPERTIES"
            )
        )
        assertTrue(fullCode.contains("let _item_len = buffer.read_i16()?;"))
        assertTrue(fullCode.contains("let key = tag & 0x3F;"))
        assertTrue(fullCode.contains("feature = \"profile_widgets\""))
        assertTrue(fullCode.contains("feature = \"profile_baseline\""))
        assertTrue(fullCode.contains("rc_profile_all"))
        assertTrue(fullCode.contains("_ => Err(WireError::UnknownOpCode(op_id.as_u8())),"))
        assertTrue(fullCode.contains("buffer.remaining()"))
        assertFalse(fullCode.contains("feature = \"widgets\""))
        assertFalse(fullCode.contains("rc_all_profiles"))
    }

    @Test
    fun testRustNativeCompilationAndRoundTrip() {
        val candidates = listOf(File(System.getProperty("user.home"), ".cargo/bin/rustc"))
        val rustcFromPath =
            try {
                val p = ProcessBuilder("which", "rustc").start()
                val path = p.inputStream.bufferedReader().readText().trim()
                if (p.waitFor() == 0 && path.isNotEmpty()) File(path) else null
            } catch (_: Exception) {
                null
            }
        val rustc =
            (candidates + listOfNotNull(rustcFromPath)).firstOrNull {
                it.exists() && it.canExecute()
            }
        Assume.assumeNotNull("Skipping Rust native compilation test: rustc not found", rustc)

        val schema = loadSchema()
        val generator = RustGenerator(schema)
        val rustCode = generator.generate()

        val tmpDir = Files.createTempDirectory("remote_compose_rust_test").toFile()
        try {
            val mainRs = File(tmpDir, "main.rs")
            mainRs.writeText(
                """
                $rustCode

                fn main() {
                    let mut original_doc = RemoteDocument::new();
                    original_doc.width = 1080;
                    original_doc.height = 1920;
                    original_doc.operations.push(Operation::DrawCircle(DrawCircle {
                        center_x: 100.0,
                        center_y: 200.0,
                        radius: 50.0,
                    }));
                    original_doc.operations.push(Operation::DrawRect(DrawRect {
                        left: 10.0,
                        top: 20.0,
                        right: 300.0,
                        bottom: 400.0,
                    }));

                    let mut buffer = WireBuffer::new();
                    original_doc.save(&mut buffer).expect("Failed to save document");

                    assert!(buffer.as_bytes().len() > 0, "Buffer should not be empty");

                    let mut read_buffer = WireBuffer::from_bytes(buffer.as_bytes());
                    let loaded_doc = RemoteDocument::load(&mut read_buffer).expect("Failed to load document");

                    assert_eq!(loaded_doc.width, 1080);
                    assert_eq!(loaded_doc.height, 1920);
                    assert_eq!(loaded_doc.operations.len(), 2);

                    match &loaded_doc.operations[0] {
                        Operation::DrawCircle(circle) => {
                            assert_eq!(circle.center_x, 100.0);
                            assert_eq!(circle.center_y, 200.0);
                            assert_eq!(circle.radius, 50.0);
                        }
                        _ => panic!("Expected DrawCircle operation"),
                    }

                    match &loaded_doc.operations[1] {
                        Operation::DrawRect(rect) => {
                            assert_eq!(rect.left, 10.0);
                            assert_eq!(rect.top, 20.0);
                            assert_eq!(rect.right, 300.0);
                            assert_eq!(rect.bottom, 400.0);
                        }
                        _ => panic!("Expected DrawRect operation"),
                    }

                    let args: Vec<String> = std::env::args().collect();
                    if args.len() > 1 {
                        std::fs::write(&args[1], buffer.as_bytes()).expect("Failed to write output file");
                    }

                    println!("Native Rust execution and assertions succeeded!");
                }
                """
                    .trimIndent()
            )

            val binaryFile = File(tmpDir, "rust_test")
            val compileProcess =
                ProcessBuilder(
                        rustc!!.absolutePath,
                        mainRs.absolutePath,
                        "-O",
                        "-o",
                        binaryFile.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()

            val compileOutput = compileProcess.inputStream.bufferedReader().readText()
            val compileExitCode = compileProcess.waitFor()
            assertEquals("Rust compilation failed:\n$compileOutput", 0, compileExitCode)

            val binaryOutputFile = File(tmpDir, "rust_output.rc")
            val runProcess =
                ProcessBuilder(binaryFile.absolutePath, binaryOutputFile.absolutePath)
                    .redirectErrorStream(true)
                    .start()

            val runOutput = runProcess.inputStream.bufferedReader().readText()
            val runExitCode = runProcess.waitFor()
            assertEquals("Rust execution failed:\n$runOutput", 0, runExitCode)
            assertTrue(
                "Rust output should contain success message",
                runOutput.contains("Native Rust execution and assertions succeeded!"),
            )
            assertTrue("Rust binary output file should exist", binaryOutputFile.exists())

            // Cross-language fidelity: Load Rust-generated binary document into Kotlin AST
            val rustBytes = binaryOutputFile.readBytes()
            val kotlinLoadedDoc = RemoteDocument.load(rustBytes)

            assertEquals(1080, kotlinLoadedDoc.header.width)
            assertEquals(1920, kotlinLoadedDoc.header.height)
            assertEquals(1, kotlinLoadedDoc.header.majorVersion)
            assertEquals(2, kotlinLoadedDoc.nodes.size)

            val kotlinCircle = kotlinLoadedDoc.nodes[0] as DrawCircleNode
            assertEquals(100.0f, kotlinCircle.centerX)
            assertEquals(200.0f, kotlinCircle.centerY)
            assertEquals(50.0f, kotlinCircle.radius)

            val kotlinRect = kotlinLoadedDoc.nodes[1] as DrawRectNode
            assertEquals(10.0f, kotlinRect.left)
            assertEquals(20.0f, kotlinRect.top)
            assertEquals(300.0f, kotlinRect.right)
            assertEquals(400.0f, kotlinRect.bottom)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testKotlinGeneratorOutput() {
        val schema = loadSchema()
        val generator = KotlinGenerator(schema)
        val code = generator.generateAstNodes()

        assertTrue(code.contains("public sealed interface DocumentNode"))
        assertTrue(code.contains("private class Hidden : DocumentNode"))
        assertTrue(code.contains("public class DrawCircleNode("))
        assertTrue(code.contains("public class BoxLayoutNode("))
        assertTrue(code.contains("override val opcode: Int\n        get() = Companion.OPCODE"))
        assertTrue(code.contains("MaybeVariableFloat.fromRawBits(buffer.readInt())"))
        assertTrue(
            code.contains(
                "internal fun read(buffer: WireBuffer, apiLevel: Int = 8): DrawCircleNode"
            )
        )

        val writerCode = generator.generateWriterNodes()
        assertTrue(writerCode.contains("buffer.writeInt(radius.rawBits)"))
        assertTrue(
            writerCode.contains(
                "internal fun DrawCircleNode.write(\n" +
                    "    buffer: WireBuffer,\n" +
                    "    targetApiLevel: Int = DEFAULT_DOCUMENT_API_LEVEL,\n" +
                    ")"
            )
        )
    }

    @Test
    fun testPublicRemoteSerializationCodeGeneratorApi() {
        val tmpDir = Files.createTempDirectory("remote_compose_public_api_test")
        try {
            val cppDir = tmpDir.resolve("cpp")
            RemoteSerializationCodeGenerator.generate(
                RemoteSerializationCodeGenerator.LANGUAGE_CPP,
                cppDir,
            )
            assertTrue(Files.exists(cppDir.resolve("WireBuffer.h")))
            assertTrue(Files.exists(cppDir.resolve("WireBuffer.cpp")))
            assertTrue(Files.exists(cppDir.resolve("RemoteComposeOps.h")))
            assertTrue(Files.exists(cppDir.resolve("RemoteComposeOps.cpp")))

            val rustDir = tmpDir.resolve("rust")
            RemoteSerializationCodeGenerator.generate(
                RemoteSerializationCodeGenerator.LANGUAGE_RUST,
                rustDir,
            )
            assertTrue(Files.exists(rustDir.resolve("remote_compose_operations.rs")))
            assertTrue(Files.exists(rustDir.resolve("op_codes.rs")))

            val pyDir = tmpDir.resolve("python")
            RemoteSerializationCodeGenerator.generate(
                RemoteSerializationCodeGenerator.LANGUAGE_PYTHON,
                pyDir,
            )
            assertTrue(Files.exists(pyDir.resolve("remote_compose.py")))

            val swiftDir = tmpDir.resolve("swift")
            RemoteSerializationCodeGenerator.generate(
                RemoteSerializationCodeGenerator.LANGUAGE_SWIFT,
                swiftDir,
            )
            assertTrue(Files.exists(swiftDir.resolve("RemoteComposeOperations.swift")))

            val tsDir = tmpDir.resolve("typescript")
            RemoteSerializationCodeGenerator.generate(
                RemoteSerializationCodeGenerator.LANGUAGE_TYPESCRIPT,
                tsDir,
            )
            assertTrue(Files.exists(tsDir.resolve("remoteComposeOperations.ts")))

            try {
                RemoteSerializationCodeGenerator.generate(999, tmpDir.resolve("invalid"))
                fail("Expected IllegalArgumentException for unsupported language")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message?.contains("999") == true)
            }
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testTypeScriptGeneratorOutput() {
        val schema = loadSchema()
        val generator = TypeScriptGenerator(schema)
        val code = generator.generate()

        assertTrue(code.contains("export enum OpCode {"))
        assertTrue(code.contains("DrawCircle = 46,"))
        assertTrue(code.contains("BoxLayout = 202,"))
        assertTrue(code.contains("export class DrawCircle implements Operation {"))
        assertTrue(code.contains("export class WireBuffer {"))
        assertTrue(code.contains("export class RemoteDocument {"))
        assertTrue(code.contains("buffer.writeFloat(this.radius);"))
        assertTrue(code.contains("buffer.readFloat()"))
        assertTrue(
            code.contains("if (numProps < 0 || numProps > RemoteDocument.MAX_HEADER_PROPERTIES)")
        )
        assertTrue(code.contains("const key = tag & 0x3F;"))
        assertTrue(code.contains("if (_argsCount < 0) {"))
        assertTrue(code.contains("this.checkRemaining(len, \"UTF-8 string\");"))
        assertTrue(code.contains("this.checkRemaining(len, \"buffer\");"))
        assertTrue(code.contains("if (slice.length !== len)"))
        assertTrue(code.contains("> buffer.remaining())"))
        assertTrue(
            code.contains("throw new Error(\"Unknown opcode in RemoteDocument: \" + opByte);")
        )
    }

    @Test
    fun testSwiftGeneratorOutput() {
        val schema = loadSchema()
        val generator = SwiftGenerator(schema)
        val code = generator.generate()

        assertTrue(code.contains("public enum OpCode: UInt8, CaseIterable {"))
        assertTrue(code.contains("case drawCircle = 46"))
        assertTrue(code.contains("case boxLayout = 202"))
        assertTrue(code.contains("public struct DrawCircle: RemoteOperation, Equatable {"))
        assertTrue(code.contains("public struct WireBuffer {"))
        assertTrue(code.contains("public struct RemoteDocument {"))
        assertTrue(code.contains("buffer.writeFloat(radius)"))
        assertTrue(code.contains("try buffer.readFloat()"))
        assertTrue(code.contains("guard numProps >= 0 && numProps <= MAX_HEADER_PROPERTIES else {"))
        assertTrue(code.contains("let key = tag & 0x3F"))
        assertTrue(code.contains("guard _argsCount >= 0 else {"))
        assertTrue(code.contains("public mutating func readShort() throws -> Int16"))
        assertTrue(code.contains("public mutating func writeShort(_ value: Int16)"))
        assertTrue(code.contains("Unknown opcode:"))
    }

    @Test
    fun testPythonGeneratorOutput() {
        val schema = loadSchema()
        val generator = PythonGenerator(schema)
        val code = generator.generate()

        assertTrue(code.contains("class OpCode(enum.IntEnum):"))
        assertTrue(code.contains("DrawCircle = 46"))
        assertTrue(code.contains("BoxLayout = 202"))
        assertTrue(code.contains("class DrawCircle(Operation):"))
        assertTrue(code.contains("class WireBuffer:"))
        assertTrue(code.contains("class RemoteDocument:"))
        assertTrue(code.contains("buffer.write_nan_id(self.radius)"))
        assertTrue(code.contains("buffer.read_nan_id()"))
        assertTrue(code.contains("OPERATION_REGISTRY: Dict[OpCode, Type[Operation]] = {"))
        assertTrue(code.contains("def create_operation(op_code: OpCode) -> Optional[Operation]:"))
        assertTrue(code.contains("Unknown opcode in RemoteDocument:"))
    }

    @Test
    fun testCppGeneratorOutput() {
        val schema = loadSchema()
        val generator = CppGenerator(schema)

        val header = generator.generateHeader()
        assertTrue(header.contains("enum class OpCode : uint8_t {"))
        assertTrue(header.contains("DrawCircle = 46,"))
        assertTrue(header.contains("BoxLayout = 202,"))
        assertTrue(header.contains("class DrawCircle : public Operation {"))
        assertTrue(header.contains("class RemoteDocument {"))
        assertTrue(header.contains("#if !defined(RC_ENABLE_WRITER)"))
        assertTrue(header.contains("#if defined(RC_ENABLE_WRITER) && RC_ENABLE_WRITER"))
        assertFalse(header.contains("#if defined(RC_ENABLE_WRITER)\n"))

        val source = generator.generateSource()
        assertTrue(source.contains("void DrawCircle::read(WireBuffer& buffer)"))
        assertTrue(source.contains("void DrawCircle::write(WireBuffer& buffer) const"))
        assertTrue(source.contains("buffer.readFloat()"))
        assertTrue(source.contains("buffer.writeFloat(radius)"))
        assertTrue(source.contains("#if defined(RC_ENABLE_WRITER) && RC_ENABLE_WRITER"))
        assertFalse(source.contains("#if defined(RC_ENABLE_WRITER)\n"))
        assertTrue(source.contains("Unknown or unsupported opcode:"))
        assertTrue(source.contains("Invalid numProps in header"))
        assertTrue(source.contains("buffer.remaining()"))
    }

    @Test
    fun testJavaGeneratorOutput() {
        val schema = loadSchema()
        val generator = JavaGenerator(schema)

        val drawCircle = schema.operationsByName["DrawCircle"]
        assertNotNull(drawCircle)
        val baseClass = generator.generateBaseClass(drawCircle!!)
        assertTrue(baseClass.contains("public abstract class DrawCircleBase extends Operation"))
        assertTrue(baseClass.contains("public static final int OP_CODE = 46;"))
        assertTrue(baseClass.contains("public static final String CLASS_NAME = \"DrawCircle\";"))
        assertTrue(
            baseClass.contains(
                "public static void apply(@NonNull WireBuffer buffer, float centerX, float centerY, float radius)"
            )
        )
        assertTrue(baseClass.contains("buffer.writeFloat(centerX);"))
        assertTrue(
            baseClass.contains(
                "public static void documentation(@NonNull DocumentationBuilder doc)"
            )
        )
        assertTrue(baseClass.contains("public void serialize(@NonNull MapSerializer serializer)"))

        val concreteClass = generator.generateConcreteClass(drawCircle)
        assertTrue(concreteClass.contains("public class DrawCircle extends DrawCircleBase"))
        assertTrue(
            concreteClass.contains(
                "public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations)"
            )
        )
        assertTrue(concreteClass.contains("float centerX = buffer.readFloat();"))
        assertTrue(concreteClass.contains("operations.add(new DrawCircle("))

        val opsTable = generator.generateOperationsTable()
        assertTrue(opsTable.contains("public final class GeneratedOperations {"))
        assertTrue(opsTable.contains("public static final int DRAW_CIRCLE = 46;"))
        assertTrue(opsTable.contains("public static String name(int opCode)"))
        assertTrue(
            opsTable.contains(
                "public static boolean isSupportedInProfile(int opCode, @NonNull String profile)"
            )
        )
    }

    @Test
    fun testRustOpCodesModuleOutput() {
        val schema = loadSchema()
        val generator = RustGenerator(schema)
        val module = generator.generateOpCodesModule()

        assertTrue(module.contains("pub const PROFILE_BASELINE: i32 = 0x0;"))
        assertTrue(module.contains("pub const PROFILE_WIDGETS: i32 = 0x100;"))
        assertTrue(module.contains("pub const PROFILE_ANDROIDX: i32 = 0x200;"))
        assertTrue(module.contains("pub enum OpId {"))
        assertTrue(module.contains("    DRAW_CIRCLE = 46,"))
        assertTrue(module.contains("    LAYOUT_BOX = 202,"))
        assertTrue(module.contains("pub const SUPPORTED_OPERATIONS: [SupportedOperation;"))
        assertTrue(
            module.contains("SupportedOperation { id: 46, name: \"DrawCircle\", profile_mask: 0 }")
        )
    }

    private fun findExecutableOnPath(executable: String): File? {
        return try {
            val process = ProcessBuilder("which", executable).start()
            val path = process.inputStream.bufferedReader().readText().trim()
            if (
                process.waitFor(5, TimeUnit.SECONDS) &&
                    process.exitValue() == 0 &&
                    path.isNotEmpty()
            ) {
                File(path)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun canCompileCpp(compiler: File): Boolean {
        return try {
            val probeFile = Files.createTempFile("probe", ".cpp").toFile()
            val probeOut = Files.createTempFile("probe", ".out").toFile()
            try {
                probeFile.writeText(
                    "#include <cstdint>\n#include <iostream>\nint main() { return 0; }\n"
                )
                val process =
                    ProcessBuilder(
                            compiler.absolutePath,
                            "-std=c++17",
                            probeFile.absolutePath,
                            "-o",
                            probeOut.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val finished = process.waitFor(10, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    return false
                }
                process.exitValue() == 0 && probeOut.exists() && probeOut.canExecute()
            } finally {
                probeFile.delete()
                probeOut.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    @Test
    fun testCppNativeCompilationAndCrossLanguageFidelity() {
        val compilerCandidates =
            listOfNotNull(
                findExecutableOnPath("clang++"),
                findExecutableOnPath("g++"),
                File(
                    "../../../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
            )
        val clangFile = compilerCandidates.firstOrNull {
            it.exists() && it.canExecute() && canCompileCpp(it)
        }
        Assume.assumeNotNull(
            "Skipping C++ native compilation test: functional C++ compiler with C++17 STL not found",
            clangFile,
        )

        val schema = loadSchema()
        val generator = CppGenerator(schema)

        val tmpDir = Files.createTempDirectory("remote_compose_cpp_test").toFile()
        try {
            // Write generated C++ files
            File(tmpDir, "WireBuffer.h").writeText(generator.generateWireBufferHeader())
            File(tmpDir, "WireBuffer.cpp").writeText(generator.generateWireBufferSource())
            File(tmpDir, "RemoteComposeOps.h").writeText(generator.generateHeader())
            File(tmpDir, "RemoteComposeOps.cpp").writeText(generator.generateSource())

            // Write C++ test driver validating internal operations and emitting binary for Kotlin
            val testDriverSource =
                """
                #include <iostream>
                #include <cassert>
                #include <fstream>
                #include "RemoteComposeOps.h"

                using namespace androidx::compose::remote;

                int main(int argc, char* argv[]) {
                    std::cout << "[CPP TEST] Starting native execution..." << std::endl;

                    // 1. Build document in C++
                    auto doc = std::make_shared<RemoteDocument>();
                    doc->width = 800;
                    doc->height = 1200;
                    doc->majorVersion = 1;
                    doc->minorVersion = 1;
                    doc->patchVersion = 0;
                    doc->capabilities = 0x12345678L;

                    auto circle = std::make_shared<DrawCircle>(150.5f, 250.5f, 75.0f);
                    doc->operations.push_back(circle);

                    auto rect = std::make_shared<DrawRect>(10.0f, 20.0f, 300.0f, 400.0f);
                    doc->operations.push_back(rect);

                    // Add TextData with std::string_view
                    auto text = std::make_shared<TextData>(42, "Hello Zero-Copy Text");
                    doc->operations.push_back(text);

                    // Add BitmapData with BufferView
                    const uint8_t rawBitmapBytes[] = {
                        0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88
                    };
                    BufferView rawView(rawBitmapBytes, sizeof(rawBitmapBytes));
                    // width=16, imageType=1 -> (1 << 16) | 16
                    // height=32, bitmapEncoding=2 -> (2 << 16) | 32
                    int32_t widthAndType = (1 << 16) | 16;
                    int32_t heightAndEncoding = (2 << 16) | 32;
                    auto bitmap = std::make_shared<BitmapData>(
                        1001, widthAndType, heightAndEncoding, rawView);
                    doc->operations.push_back(bitmap);

                    // 2. Save to WireBuffer
                    WireBuffer writeBuffer;
                    doc->save(writeBuffer);
                    assert(writeBuffer.getSize() > 0);

                    // 3. Load back in C++ using non-owning WireBuffer and assert zero-copy
                    const auto& bytes = writeBuffer.getBytes();
                    WireBuffer readBuffer(bytes.data(), bytes.size(), false);
                    assert(!readBuffer.isOwning());

                    auto loadedDoc = RemoteDocument::load(readBuffer);
                    assert(loadedDoc->width == 800);
                    assert(loadedDoc->height == 1200);
                    assert(loadedDoc->majorVersion == 1);
                    assert(loadedDoc->operations.size() == 4);

                    auto loadedCircle = std::dynamic_pointer_cast<DrawCircle>(
                        loadedDoc->operations[0]);
                    assert(loadedCircle != nullptr);
                    assert(loadedCircle->centerX == 150.5f);
                    assert(loadedCircle->centerY == 250.5f);
                    assert(loadedCircle->radius == 75.0f);

                    auto loadedRect = std::dynamic_pointer_cast<DrawRect>(
                        loadedDoc->operations[1]);
                    assert(loadedRect != nullptr);
                    assert(loadedRect->left == 10.0f);
                    assert(loadedRect->top == 20.0f);
                    assert(loadedRect->right == 300.0f);
                    assert(loadedRect->bottom == 400.0f);

                    // Verify zero-copy TextData
                    auto loadedText = std::dynamic_pointer_cast<TextData>(
                        loadedDoc->operations[2]);
                    assert(loadedText != nullptr);
                    assert(loadedText->textId == 42);
                    assert(loadedText->text == "Hello Zero-Copy Text");
                    // Pointer must point directly inside backing bytes
                    const char* textPtr = loadedText->text.data();
                    const char* bufStart = reinterpret_cast<const char*>(bytes.data());
                    const char* bufEnd = bufStart + bytes.size();
                    assert(textPtr >= bufStart && textPtr + loadedText->text.size() <= bufEnd);

                    // Verify zero-copy BitmapData
                    auto loadedBitmap = std::dynamic_pointer_cast<BitmapData>(
                        loadedDoc->operations[3]);
                    assert(loadedBitmap != nullptr);
                    assert(loadedBitmap->imageId == 1001);
                    assert(loadedBitmap->getWidth() == 16);
                    assert(loadedBitmap->getImageType() == 1);
                    assert(loadedBitmap->getHeight() == 32);
                    assert(loadedBitmap->getBitmapEncoding() == 2);
                    assert(loadedBitmap->bitmap.size() == sizeof(rawBitmapBytes));
                    assert(loadedBitmap->bitmap == rawView);
                    // Pointer must point directly inside backing bytes
                    const uint8_t* bData = loadedBitmap->bitmap.data();
                    assert(bData >= bytes.data() &&
                           bData + loadedBitmap->bitmap.size() <= bytes.data() + bytes.size());

                    // 4. Output binary file for Kotlin validation if requested
                    if (argc > 1) {
                        std::ofstream out(argv[1], std::ios::binary);
                        out.write(reinterpret_cast<const char*>(bytes.data()), bytes.size());
                    }

                    std::cout << "[CPP TEST] Native C++ execution and assertions succeeded!"
                              << std::endl;
                    return 0;
                }
                """
                    .trimIndent()
            File(tmpDir, "main.cpp").writeText(testDriverSource)

            // Compile with prebuilt clang++
            val binaryFile = File(tmpDir, "cpp_test_runner")
            val compileProcess =
                ProcessBuilder(
                        clangFile!!.absolutePath,
                        "-std=c++17",
                        "-O2",
                        "-Wall",
                        File(tmpDir, "WireBuffer.cpp").absolutePath,
                        File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                        File(tmpDir, "main.cpp").absolutePath,
                        "-o",
                        binaryFile.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()

            val compileOutput = compileProcess.inputStream.bufferedReader().readText()
            val compileExitCode = compileProcess.waitFor()
            assertEquals("C++ compilation failed:\n$compileOutput", 0, compileExitCode)

            // Execute the compiled C++ test binary
            val binaryOutputFile = File(tmpDir, "cpp_output.rc")
            val runProcess =
                ProcessBuilder(
                        binaryFile.absolutePath,
                        binaryOutputFile.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()

            val runOutput = runProcess.inputStream.bufferedReader().readText()
            val runExitCode = runProcess.waitFor()
            assertEquals("C++ execution failed:\n$runOutput", 0, runExitCode)
            assertTrue(
                "C++ output should contain success message",
                runOutput.contains("Native C++ execution and assertions succeeded!"),
            )
            assertTrue("Binary output file should exist", binaryOutputFile.exists())

            // 5. Cross-language fidelity: Load C++-generated binary document into Kotlin AST
            val cppBytes = binaryOutputFile.readBytes()
            val kotlinLoadedDoc = RemoteDocument.load(cppBytes)

            assertEquals(800, kotlinLoadedDoc.header.width)
            assertEquals(1200, kotlinLoadedDoc.header.height)
            assertEquals(1, kotlinLoadedDoc.header.majorVersion)
            assertEquals(4, kotlinLoadedDoc.nodes.size)

            val kotlinCircle = kotlinLoadedDoc.nodes[0] as DrawCircleNode
            assertEquals(150.5f, kotlinCircle.centerX)
            assertEquals(250.5f, kotlinCircle.centerY)
            assertEquals(75.0f, kotlinCircle.radius)

            val kotlinRect = kotlinLoadedDoc.nodes[1] as DrawRectNode
            assertEquals(10.0f, kotlinRect.left)
            assertEquals(20.0f, kotlinRect.top)
            assertEquals(300.0f, kotlinRect.right)
            assertEquals(400.0f, kotlinRect.bottom)

            val kotlinText = kotlinLoadedDoc.nodes[2] as TextDataNode
            assertEquals(42, kotlinText.textId)
            assertEquals("Hello Zero-Copy Text", kotlinText.text)

            val kotlinBitmap = kotlinLoadedDoc.nodes[3] as BitmapDataNode
            assertEquals(1001, kotlinBitmap.imageId)
            assertEquals((1 shl 16) or 16, kotlinBitmap.widthAndType)
            assertEquals((2 shl 16) or 32, kotlinBitmap.heightAndEncoding)
            assertArrayEquals(
                byteArrayOf(
                    0x11.toByte(),
                    0x22.toByte(),
                    0x33.toByte(),
                    0x44.toByte(),
                    0x55.toByte(),
                    0x66.toByte(),
                    0x77.toByte(),
                    0x88.toByte(),
                ),
                kotlinBitmap.bitmap,
            )
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testPythonNativeRoundTripAndCrossLanguageFidelity() {
        val pythonExe = findExecutableOnPath("python3") ?: findExecutableOnPath("python")
        Assume.assumeNotNull(
            "Skipping Python native execution test: python3 not found on PATH",
            pythonExe,
        )

        val schema = loadSchema()
        val generator = PythonGenerator(schema)

        val tmpDir = Files.createTempDirectory("remote_compose_py_test").toFile()
        try {
            File(tmpDir, "remote_compose.py").writeText(generator.generate())

            val testDriverSource =
                """
                import sys
                from remote_compose import (
                    RemoteDocument, DrawCircle, DrawRect, TextData, BitmapData, WireBuffer
                )

                doc = RemoteDocument()
                doc.width = 800
                doc.height = 1200
                doc.major_version = 1
                doc.minor_version = 1
                doc.patch_version = 0
                doc.capabilities = 0x12345678

                circle = DrawCircle(center_x=150.5, center_y=250.5, radius=75.0)
                doc.operations.append(circle)

                rect = DrawRect(left=10.0, top=20.0, right=300.0, bottom=400.0)
                doc.operations.append(rect)

                text = TextData(text_id=42, text="Hello Python RemoteCompose")
                doc.operations.append(text)

                raw_bitmap_bytes = bytes([0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88])
                width_and_type = (1 << 16) | 16
                height_and_encoding = (2 << 16) | 32
                bitmap = BitmapData(
                    image_id=1001,
                    width_and_type=width_and_type,
                    height_and_encoding=height_and_encoding,
                    bitmap=raw_bitmap_bytes,
                )
                doc.operations.append(bitmap)

                # Save document
                saved_bytes = doc.save()
                assert len(saved_bytes) > 0, "Saved bytes must not be empty"

                # Load document back
                loaded_doc = RemoteDocument.load(saved_bytes)
                assert loaded_doc.width == 800
                assert loaded_doc.height == 1200
                assert loaded_doc.major_version == 1
                assert len(loaded_doc.operations) == 4

                loaded_circle = loaded_doc.operations[0]
                assert isinstance(loaded_circle, DrawCircle)
                assert loaded_circle.center_x == 150.5
                assert loaded_circle.center_y == 250.5
                assert loaded_circle.radius == 75.0

                loaded_rect = loaded_doc.operations[1]
                assert isinstance(loaded_rect, DrawRect)
                assert loaded_rect.left == 10.0
                assert loaded_rect.top == 20.0
                assert loaded_rect.right == 300.0
                assert loaded_rect.bottom == 400.0

                loaded_text = loaded_doc.operations[2]
                assert isinstance(loaded_text, TextData)
                assert loaded_text.text_id == 42
                assert loaded_text.text == "Hello Python RemoteCompose"

                loaded_bitmap = loaded_doc.operations[3]
                assert isinstance(loaded_bitmap, BitmapData)
                assert loaded_bitmap.image_id == 1001
                assert loaded_bitmap.width_and_type == width_and_type
                assert loaded_bitmap.height_and_encoding == height_and_encoding
                assert loaded_bitmap.bitmap == raw_bitmap_bytes

                if len(sys.argv) > 1:
                    with open(sys.argv[1], "wb") as f:
                        f.write(saved_bytes)

                print("[PYTHON TEST] Execution and assertions succeeded!")
                """
                    .trimIndent()

            File(tmpDir, "test_driver.py").writeText(testDriverSource)

            val outputRcFile = File(tmpDir, "py_output.rc")
            val runProcess =
                ProcessBuilder(
                        pythonExe!!.absolutePath,
                        File(tmpDir, "test_driver.py").absolutePath,
                        outputRcFile.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()

            val runOutput = runProcess.inputStream.bufferedReader().readText()
            val runExitCode = runProcess.waitFor()
            assertEquals("Python execution failed:\n$runOutput", 0, runExitCode)
            assertTrue(
                "Python output should contain success message",
                runOutput.contains("Execution and assertions succeeded!"),
            )
            assertTrue("Binary output file should exist", outputRcFile.exists())

            // Cross-language fidelity: Load Python-generated binary document into Kotlin AST
            val pyBytes = outputRcFile.readBytes()
            val kotlinLoadedDoc = RemoteDocument.load(pyBytes)

            assertEquals(800, kotlinLoadedDoc.header.width)
            assertEquals(1200, kotlinLoadedDoc.header.height)
            assertEquals(1, kotlinLoadedDoc.header.majorVersion)
            assertEquals(4, kotlinLoadedDoc.nodes.size)

            val kotlinCircle = kotlinLoadedDoc.nodes[0] as DrawCircleNode
            assertEquals(150.5f, kotlinCircle.centerX)
            assertEquals(250.5f, kotlinCircle.centerY)
            assertEquals(75.0f, kotlinCircle.radius)

            val kotlinRect = kotlinLoadedDoc.nodes[1] as DrawRectNode
            assertEquals(10.0f, kotlinRect.left)
            assertEquals(20.0f, kotlinRect.top)
            assertEquals(300.0f, kotlinRect.right)
            assertEquals(400.0f, kotlinRect.bottom)

            val kotlinText = kotlinLoadedDoc.nodes[2] as TextDataNode
            assertEquals(42, kotlinText.textId)
            assertEquals("Hello Python RemoteCompose", kotlinText.text)

            val kotlinBitmap = kotlinLoadedDoc.nodes[3] as BitmapDataNode
            assertEquals(1001, kotlinBitmap.imageId)
            assertEquals((1 shl 16) or 16, kotlinBitmap.widthAndType)
            assertEquals((2 shl 16) or 32, kotlinBitmap.heightAndEncoding)
            assertArrayEquals(
                byteArrayOf(
                    0x11.toByte(),
                    0x22.toByte(),
                    0x33.toByte(),
                    0x44.toByte(),
                    0x55.toByte(),
                    0x66.toByte(),
                    0x77.toByte(),
                    0x88.toByte(),
                ),
                kotlinBitmap.bitmap,
            )
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testIntRpnAndSystemVariablesAndProfileSynchronization() {
        val schema = loadSchema()
        val intRpn = schema.intRpnOperators
        assertNotNull("intRpnOperators must be present in schema", intRpn)
        assertEquals(IntegerExpressionEvaluator.OFFSET, intRpn!!.offset)
        assertEquals(
            IntegerExpressionEvaluator.I_ADD,
            intRpn.offset + intRpn.operatorsByName["I_ADD"]!!.id,
        )
        assertEquals(
            IntegerExpressionEvaluator.I_IFELSE,
            intRpn.offset + intRpn.operatorsByName["I_IFELSE"]!!.id,
        )
        assertEquals(
            IntegerExpressionEvaluator.I_VAR2,
            intRpn.offset + intRpn.operatorsByName["I_VAR2"]!!.id,
        )

        assertEquals(
            RemoteContext.ID_CONTINUOUS_SEC,
            schema.systemVariablesByName["CONTINUOUS_SEC"]!!.id,
        )
        assertEquals(RemoteContext.ID_DENSITY, schema.systemVariablesByName["DENSITY"]!!.id)
        assertEquals(
            RemoteContext.ID_EPOCH_SECOND,
            schema.systemVariablesByName["EPOCH_SECOND"]!!.id,
        )
        assertEquals("long", schema.systemVariablesByName["EPOCH_SECOND"]!!.type)
        assertEquals(RemoteContext.ID_YEAR, schema.systemVariablesByName["YEAR"]!!.id)

        val generator = KotlinGenerator(schema)
        val protocolCode = generator.generateProtocolConstants()
        assertTrue(protocolCode.contains("public object RcProfiles"))
        assertTrue(protocolCode.contains("public object FloatRpn"))
        assertTrue(protocolCode.contains("public object IntRpn"))
        assertTrue(protocolCode.contains("public object RemoteContextIds"))

        val sinkCode = generator.generateOperationSink()
        assertTrue(sinkCode.contains("public interface RemoteOperationSink"))
        assertTrue(sinkCode.contains("public class WireBufferOperationSink"))
        assertTrue(sinkCode.contains("public class NodeListOperationSink"))
        assertTrue(sinkCode.contains("public fun OperationNode.emitTo(sink: RemoteOperationSink)"))

        // Verify schema operation profile metadata matches Operations.valid
        val testProfiles =
            listOf(
                RcProfiles.PROFILE_BASELINE,
                RcProfiles.PROFILE_ANDROIDX,
                RcProfiles.PROFILE_WIDGETS,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                RcProfiles.PROFILE_WIDGETS or RcProfiles.PROFILE_EXPERIMENTAL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_DEPRECATED,
            )
        for (op in schema.operations) {
            assertEquals(
                "API 6 mismatch for ${op.name} (${op.id})",
                Operations.valid(op.id, 6, RcProfiles.PROFILE_BASELINE),
                op.addedVersion <= 6,
            )
            for (prof in testProfiles) {
                val expected = Operations.valid(op.id, 8, prof)
                val profiles = op.profiles
                val actual =
                    when {
                        "baseline" in profiles -> true
                        "oem" in profiles -> false
                        else -> {
                            val hasAndroidx = (prof and RcProfiles.PROFILE_ANDROIDX) != 0
                            val hasWidgets = (prof and RcProfiles.PROFILE_WIDGETS) != 0
                            val hasExperimental = (prof and RcProfiles.PROFILE_EXPERIMENTAL) != 0
                            val hasDeprecated = (prof and RcProfiles.PROFILE_DEPRECATED) != 0
                            if (!hasAndroidx && !hasWidgets) {
                                false
                            } else {
                                val platformOk =
                                    (!hasAndroidx || "androidx" in profiles) &&
                                        (!hasWidgets || "widgets" in profiles)
                                val expOk = "experimental" !in profiles || hasExperimental
                                val depOk = "deprecated" !in profiles || hasDeprecated
                                platformOk && expOk && depOk
                            }
                        }
                    }
                assertEquals(
                    "API 8 profile=$prof mismatch for ${op.name} (${op.id}, profiles=$profiles)",
                    expected,
                    actual,
                )
            }
        }
    }

    @Test
    fun testCppReaderOnlyAndUnknownOpcodeFailure() {
        val compilerCandidates =
            listOfNotNull(
                findExecutableOnPath("clang++"),
                findExecutableOnPath("g++"),
                File(
                    "../../../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
            )
        val clangFile = compilerCandidates.firstOrNull {
            it.exists() && it.canExecute() && canCompileCpp(it)
        }
        Assume.assumeNotNull(
            "Skipping C++ reader-only compilation test: functional C++ compiler not found",
            clangFile,
        )

        val schema = loadSchema()
        val generator = CppGenerator(schema)
        val tmpDir = Files.createTempDirectory("remote_compose_cpp_reader_only_test").toFile()
        try {
            File(tmpDir, "WireBuffer.h").writeText(generator.generateWireBufferHeader())
            File(tmpDir, "WireBuffer.cpp").writeText(generator.generateWireBufferSource())
            File(tmpDir, "RemoteComposeOps.h").writeText(generator.generateHeader())
            File(tmpDir, "RemoteComposeOps.cpp").writeText(generator.generateSource())

            val driver =
                """
                #include <cassert>
                #include <stdexcept>
                #include "RemoteComposeOps.h"

                #if RC_ENABLE_WRITER != 0
                #error "Expected RC_ENABLE_WRITER to be 0"
                #endif

                using namespace androidx::compose::remote;

                int main() {
                    // Valid header followed by unknown opcode 0xFE must throw
                    WireBuffer wb;
                    wb.writeByte(static_cast<uint8_t>(OpCode::Header));
                    wb.writeInt(1);
                    wb.writeInt(1);
                    wb.writeInt(0);
                    wb.writeInt(100);
                    wb.writeInt(200);
                    wb.writeLong(0);
                    wb.writeByte(0xFE);

                    WireBuffer reader(wb.getBytes());
                    bool threw = false;
                    try {
                        RemoteDocument::load(reader);
                    } catch (const std::runtime_error&) {
                        threw = true;
                    }
                    assert(threw);
                    return 0;
                }
                """
                    .trimIndent()
            val driverFile = File(tmpDir, "main.cpp")
            driverFile.writeText(driver)

            for (writerFlag in listOf("-DRC_ENABLE_WRITER=0", "-DRC_NO_WRITER")) {
                val binaryFile = File(tmpDir, "cpp_reader_${writerFlag.replace("=", "_")}")
                val compileProcess =
                    ProcessBuilder(
                            clangFile!!.absolutePath,
                            "-std=c++17",
                            "-Wall",
                            "-Werror",
                            writerFlag,
                            File(tmpDir, "WireBuffer.cpp").absolutePath,
                            File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                            driverFile.absolutePath,
                            "-o",
                            binaryFile.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val compileOut = compileProcess.inputStream.bufferedReader().readText()
                assertEquals(
                    "C++ compilation failed with $writerFlag:\n$compileOut",
                    0,
                    compileProcess.waitFor(),
                )

                val runProcess =
                    ProcessBuilder(binaryFile.absolutePath).redirectErrorStream(true).start()
                val runOut = runProcess.inputStream.bufferedReader().readText()
                assertEquals(
                    "C++ execution failed with $writerFlag:\n$runOut",
                    0,
                    runProcess.waitFor(),
                )
            }
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testRustProfileFeaturesAndReaderOnlyCompilation() {
        val candidates = listOf(File(System.getProperty("user.home"), ".cargo/bin/rustc"))
        val rustcFromPath = findExecutableOnPath("rustc")
        val rustc =
            (candidates + listOfNotNull(rustcFromPath)).firstOrNull {
                it.exists() && it.canExecute()
            }
        Assume.assumeNotNull("Skipping Rust profile compilation test: rustc not found", rustc)

        val schema = loadSchema()
        val rustCode = RustGenerator(schema).generate()
        val tmpDir = Files.createTempDirectory("remote_compose_rust_profile_test").toFile()
        try {
            // 1. Baseline-only via --cfg rc_profile_baseline: TextStyle (widgets) is rejected
            val baselineMain = File(tmpDir, "baseline.rs")
            baselineMain.writeText(
                """
                $rustCode

                fn main() {
                    let mut buf = WireBuffer::new();
                    buf.write_u8(OpId::Header.as_u8());
                    buf.write_i32(1);
                    buf.write_i32(1);
                    buf.write_i32(0);
                    buf.write_i32(100);
                    buf.write_i32(200);
                    buf.write_i64(0);
                    buf.write_u8(OpId::TextStyle.as_u8());

                    let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                    let res = RemoteDocument::load(&mut reader);
                    assert_eq!(res, Err(WireError::UnknownOpCode(OpId::TextStyle.as_u8())));
                }
                """
                    .trimIndent()
            )
            val baselineBin = File(tmpDir, "baseline_bin")
            val baselineCompile =
                ProcessBuilder(
                        rustc!!.absolutePath,
                        baselineMain.absolutePath,
                        "--cfg",
                        "rc_profile_baseline",
                        "-o",
                        baselineBin.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()
            val baselineOut = baselineCompile.inputStream.bufferedReader().readText()
            assertEquals(
                "Rust rc_profile_baseline compilation failed:\n$baselineOut",
                0,
                baselineCompile.waitFor(),
            )
            val baselineRun =
                ProcessBuilder(baselineBin.absolutePath).redirectErrorStream(true).start()
            val baselineRunOut = baselineRun.inputStream.bufferedReader().readText()
            assertEquals(
                "Rust rc_profile_baseline execution failed:\n$baselineRunOut",
                0,
                baselineRun.waitFor(),
            )

            // 2. Cargo feature="profile_widgets" + reader-only: TextStyle compiles, FlowLayout
            // (experimental) rejected
            val widgetsMain = File(tmpDir, "widgets.rs")
            widgetsMain.writeText(
                """
                $rustCode

                fn main() {
                    // Verify widgets-profile operation struct & variant are compiled in:
                    let _check_widgets_variant: fn(Box<TextStyle>) -> Operation = Operation::TextStyle;
                    let _ = _check_widgets_variant;
                    let mut buf = WireBuffer::new();
                    buf.write_u8(OpId::Header.as_u8());
                    buf.write_i32(1);
                    buf.write_i32(1);
                    buf.write_i32(0);
                    buf.write_i32(100);
                    buf.write_i32(200);
                    buf.write_i64(0);
                    buf.write_u8(OpId::FlowLayout.as_u8());

                    let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                    let res = RemoteDocument::load(&mut reader);
                    assert_eq!(res, Err(WireError::UnknownOpCode(OpId::FlowLayout.as_u8())));
                }
                """
                    .trimIndent()
            )
            val widgetsBin = File(tmpDir, "widgets_bin")
            val widgetsCompile =
                ProcessBuilder(
                        rustc.absolutePath,
                        widgetsMain.absolutePath,
                        "--cfg",
                        "feature=\"profile_widgets\"",
                        "--cfg",
                        "rc_no_writer",
                        "-o",
                        widgetsBin.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()
            val widgetsOut = widgetsCompile.inputStream.bufferedReader().readText()
            assertEquals(
                "Rust feature=profile_widgets + rc_no_writer compilation failed:\n$widgetsOut",
                0,
                widgetsCompile.waitFor(),
            )
            val widgetsRun =
                ProcessBuilder(widgetsBin.absolutePath).redirectErrorStream(true).start()
            val widgetsRunOut = widgetsRun.inputStream.bufferedReader().readText()
            assertEquals(
                "Rust feature=profile_widgets execution failed:\n$widgetsRunOut",
                0,
                widgetsRun.waitFor(),
            )
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testCppAdversarialReaderHardening() {
        val compilerCandidates =
            listOfNotNull(
                findExecutableOnPath("clang++"),
                findExecutableOnPath("g++"),
                File(
                    "../../../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
                File(
                    "../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
            )
        val clangFile = compilerCandidates.firstOrNull {
            it.exists() && it.canExecute() && canCompileCpp(it)
        }
        Assume.assumeNotNull(
            "Skipping C++ adversarial test: functional C++ compiler not found",
            clangFile,
        )

        val schema = loadSchema()
        val generator = CppGenerator(schema)
        val tmpDir = Files.createTempDirectory("remote_compose_cpp_security_test").toFile()
        try {
            File(tmpDir, "WireBuffer.h").writeText(generator.generateWireBufferHeader())
            File(tmpDir, "WireBuffer.cpp").writeText(generator.generateWireBufferSource())
            File(tmpDir, "RemoteComposeOps.h").writeText(generator.generateHeader())
            File(tmpDir, "RemoteComposeOps.cpp").writeText(generator.generateSource())

            // 1. Core adversarial suite (all profiles enabled)
            val driver =
                """
                #include <cassert>
                #include <cstdint>
                #include <stdexcept>
                #include <vector>
                #include "RemoteComposeOps.h"

                using namespace androidx::compose::remote;

                template <typename Fn>
                void assertThrowsRuntime(Fn fn) {
                    bool threw = false;
                    try {
                        fn();
                    } catch (const std::runtime_error&) {
                        threw = true;
                    }
                    assert(threw);
                }

                WireBuffer makeV6Header() {
                    WireBuffer wb;
                    wb.writeByte(static_cast<uint8_t>(OpCode::Header));
                    wb.writeInt(1);
                    wb.writeInt(1);
                    wb.writeInt(0);
                    wb.writeInt(100);
                    wb.writeInt(200);
                    wb.writeLong(0);
                    return wb;
                }

                int main() {
                    // Rule-of-Five: copy & move owning WireBuffer without UAF
                    {
                        WireBuffer a;
                        a.writeInt(0x12345678);
                        WireBuffer b(a);
                        WireBuffer c;
                        c = b;
                        WireBuffer d(std::move(b));
                        a.writeInt(0xCAFEBABE);
                        c.setIndex(0);
                        d.setIndex(0);
                        assert(c.readInt() == 0x12345678);
                        assert(d.readInt() == 0x12345678);
                    }

                    // Non-owning WireBuffer::getBytes() returns copy and preserves zero-copy ptr
                    {
                        std::vector<uint8_t> raw = {0, 0, 0, 2, 'h', 'i'};
                        WireBuffer nonOwning(raw.data(), raw.size(), false);
                        assert(!nonOwning.isOwning());
                        auto copyBytes = nonOwning.getBytes();
                        assert(copyBytes == raw);
                        assert(!nonOwning.isOwning());
                        auto sv = nonOwning.readStringView();
                        assert(sv == "hi");
                        assert(sv.data() == reinterpret_cast<const char*>(raw.data() + 4));
                        assertThrowsRuntime([&]() { nonOwning.writeByte(1); });
                    }

                    // BufferView(nullptr, nonZero) must not UB or crash in operator==
                    {
                        BufferView emptyNull(nullptr, 0);
                        BufferView nonZeroNull(nullptr, 10);
                        assert(nonZeroNull.empty());
                        assert(nonZeroNull.size() == 0);
                        assert(emptyNull == nonZeroNull);
                        assertThrowsRuntime([&]() { WireBuffer bad(nullptr, 4, false); });
                    }

                    // readByte() underflow throws std::runtime_error
                    {
                        WireBuffer empty(std::vector<uint8_t>{});
                        assertThrowsRuntime([&]() { empty.readByte(); });
                    }

                    // Invalid UTF-8 rejected without advancing index
                    {
                        std::vector<uint8_t> badUtf8 = {0, 0, 0, 2, 0xFF, 0xFE};
                        WireBuffer wb(badUtf8);
                        assertThrowsRuntime([&]() { wb.readStringView(); });
                        assert(wb.getIndex() == 4);
                    }

                    // Negative / invalid V7 magic rejected
                    for (int32_t badMagic : {static_cast<int32_t>(0x80000000u), 0x12340001, -1}) {
                        WireBuffer wb;
                        wb.writeByte(static_cast<uint8_t>(OpCode::Header));
                        wb.writeInt(badMagic);
                        wb.writeInt(0);
                        wb.writeInt(0);
                        wb.writeInt(0);
                        wb.writeInt(0);
                        wb.writeLong(0);
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }

                    // V7 numProps > kMaxHeaderProperties rejected
                    {
                        WireBuffer wb;
                        wb.writeByte(static_cast<uint8_t>(OpCode::Header));
                        wb.writeInt(0x048C0001);
                        wb.writeInt(1);
                        wb.writeInt(0);
                        wb.writeInt(1001);
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }

                    // V7 COMPRESS = 1 (key 31) rejected
                    {
                        WireBuffer wb;
                        wb.writeByte(static_cast<uint8_t>(OpCode::Header));
                        wb.writeInt(0x048C0001);
                        wb.writeInt(1);
                        wb.writeInt(0);
                        wb.writeInt(1);
                        wb.writeShort(31); // dataType=0 (INT), key=31 (COMPRESS)
                        wb.writeShort(4);
                        wb.writeInt(1);
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }

                    // Mid-stream Header opcode rejected
                    {
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::Header));
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });

                        HeaderProperty badProp;
                        badProp.tag = 5; // key=5, dataType=0 (INT), but intValue is std::nullopt
                        badProp.itemLen = 4;
                        WireBuffer propOut;
                        assertThrowsRuntime([&]() { badProp.write(propOut); });
                    }

                    // maxOperations limit enforced (ContainerEnd is 1-byte op)
                    {
                        WireBuffer wb = makeV6Header();
                        for (int i = 0; i < 6; ++i) {
                            wb.writeByte(static_cast<uint8_t>(OpCode::ContainerEnd));
                        }
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r, 5); });
                    }

                    // Struct array (BitmapFontData Glyph[] = 20 bytes/elem) over-length rejected
                    {
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::BitmapFontData));
                        wb.writeInt(1);  // id
                        wb.writeInt(10); // version=0, numGlyphs=10 (needs 200B)
                        for (int i = 0; i < 19; ++i) wb.writeByte(0);
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }

                    // Sparse count overflow rejected
                    {
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::CoreText));
                        wb.writeInt(1);
                        wb.writeShort(100); // needs >= 200B
                        wb.writeInt(0);
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }

                    // BitmapData 16-bit width/height and upper 16-bit type/encoding parity
                    {
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::BitmapData));
                        wb.writeInt(7);
                        wb.writeInt((3 << 16) | 100); // TYPE_RAW8888=3, width=100
                        wb.writeInt((1 << 16) | 200); // ENCODING_URL=1, height=200
                        wb.writeBuffer(std::vector<uint8_t>{1, 2, 3, 4});
                        WireBuffer r(wb.getBytes());
                        auto doc = RemoteDocument::load(r);
                        assert(doc->operations.size() == 1);
                        auto* bmp = dynamic_cast<BitmapData*>(doc->operations[0].get());
                        assert(bmp != nullptr);
                        assert(bmp->getWidth() == 100);
                        assert(bmp->getImageType() == 3);
                        assert(bmp->getHeight() == 200);
                        assert(bmp->getBitmapEncoding() == 1);
                    }

                    // BitmapFontData signed version: 0x80000000 -> version=-32768 must NOT read kerning
                    {
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::BitmapFontData));
                        wb.writeInt(9);
                        wb.writeInt(static_cast<int32_t>(0x80000000u)); // version=-32768, numGlyphs=0
                        wb.writeByte(static_cast<uint8_t>(OpCode::ContainerEnd));
                        WireBuffer r(wb.getBytes());
                        auto doc = RemoteDocument::load(r);
                        assert(doc->operations.size() == 2);
                        auto* font = dynamic_cast<BitmapFontData*>(doc->operations[0].get());
                        assert(font != nullptr);
                        assert(font->glyphs.empty());
                        assert(font->kerningTable.empty());
                        assert(doc->operations[1]->getOpCode() == OpCode::ContainerEnd);
                    }

                    // AddMesh2D type=99 reads literal float mesh; ColorExpression mode=99 rejected
                    {
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::AddMesh2D));
                        wb.writeInt(1);  // meshId
                        wb.writeInt(99); // fallback literal float mesh type
                        wb.writeInt(0);  // layout
                        wb.writeInt(0);  // uCount
                        wb.writeInt(0);  // vCount
                        wb.writeInt(5);  // flags (capSegments=5)
                        wb.writeInt(0);  // aux
                        wb.writeInt(0);  // indices len
                        wb.writeInt(2);  // vertsLen
                        wb.writeInt(0);  // uvLen
                        wb.writeInt(0);  // colorsLen
                        wb.writeFloat(3.0f);
                        wb.writeFloat(4.0f);
                        WireBuffer r(wb.getBytes());
                        auto doc = RemoteDocument::load(r);
                        assert(doc->operations.size() == 1);
                        auto* mesh = dynamic_cast<AddMesh2D*>(doc->operations[0].get());
                        assert(mesh != nullptr);
                        assert(mesh->verts.size() == 2);
                        assert(mesh->getCapSegments() == 5);
                        assert(mesh->antialiasWidth == 1.0f);

                        WireBuffer badCe = makeV6Header();
                        badCe.writeByte(static_cast<uint8_t>(OpCode::ColorExpression));
                        badCe.writeInt(1);
                        badCe.writeInt(99); // invalid mode > 6
                        badCe.writeInt(0);
                        badCe.writeInt(0);
                        badCe.writeInt(0);
                        WireBuffer rCe(badCe.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(rCe); });
                    }

                    // maxLength enforced even when buffer contains enough bytes
                    {
                        // TextData.text maxLength = 4000
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::TextData));
                        wb.writeInt(1);
                        wb.writeUTF8(std::string(4001, 'a'));
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }
                    {
                        // TimeAttribute.args maxLength = 32
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::TimeAttribute));
                        wb.writeInt(1);
                        wb.writeInt(2);
                        wb.writeShort(0);
                        wb.writeShort(33);
                        for (int i = 0; i < 33; ++i) wb.writeInt(i);
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }
                    {
                        // FloatExpression.expression maxLength = 32
                        WireBuffer wb = makeV6Header();
                        wb.writeByte(static_cast<uint8_t>(OpCode::FloatExpression));
                        wb.writeInt(1);
                        wb.writeInt(33); // expressionLength=33, animationLength=0
                        for (int i = 0; i < 33; ++i) wb.writeFloat(1.0f);
                        WireBuffer r(wb.getBytes());
                        assertThrowsRuntime([&]() { RemoteDocument::load(r); });
                    }

                    return 0;
                }
                """
                    .trimIndent()
            val driverFile = File(tmpDir, "security_main.cpp")
            driverFile.writeText(driver)
            val secBin = File(tmpDir, "cpp_security_bin")
            val compileSec =
                ProcessBuilder(
                        clangFile!!.absolutePath,
                        "-std=c++17",
                        "-Wall",
                        "-Werror",
                        File(tmpDir, "WireBuffer.cpp").absolutePath,
                        File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                        driverFile.absolutePath,
                        "-o",
                        secBin.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()
            val secCompileOut = compileSec.inputStream.bufferedReader().readText()
            assertEquals(
                "C++ security test compile failed:\n$secCompileOut",
                0,
                compileSec.waitFor(),
            )
            val runSec = ProcessBuilder(secBin.absolutePath).redirectErrorStream(true).start()
            val secRunOut = runSec.inputStream.bufferedReader().readText()
            assertEquals("C++ security test run failed:\n$secRunOut", 0, runSec.waitFor())

            // 2. C++ profile isolation: WIDGETS + EXPERIMENTAL accepts FlowLayout, rejects Custom
            val profileDriver =
                """
                #include <cassert>
                #include "RemoteComposeOps.h"

                using namespace androidx::compose::remote;

                int main() {
                    auto flowOp = createOperation(OpCode::FlowLayout);
                    auto customOp = createOperation(OpCode::Custom);
                #if RC_PROFILE_EXPERIMENTAL
                    assert(flowOp != nullptr);
                    assert(customOp == nullptr);
                #else
                    assert(flowOp == nullptr);
                    assert(customOp == nullptr);
                #endif
                    return 0;
                }
                """
                    .trimIndent()
            val profileDriverFile = File(tmpDir, "profile_main.cpp")
            profileDriverFile.writeText(profileDriver)
            for (expVal in listOf("1", "0")) {
                val pBin = File(tmpDir, "cpp_profile_exp_$expVal")
                val pCompile =
                    ProcessBuilder(
                            clangFile.absolutePath,
                            "-std=c++17",
                            "-Wall",
                            "-Werror",
                            "-DRC_PROFILE_WIDGETS=1",
                            "-DRC_PROFILE_EXPERIMENTAL=$expVal",
                            File(tmpDir, "WireBuffer.cpp").absolutePath,
                            File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                            profileDriverFile.absolutePath,
                            "-o",
                            pBin.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val pCompileOut = pCompile.inputStream.bufferedReader().readText()
                assertEquals(
                    "C++ profile exp=$expVal compile failed:\n$pCompileOut",
                    0,
                    pCompile.waitFor(),
                )
                val pRun = ProcessBuilder(pBin.absolutePath).redirectErrorStream(true).start()
                val pRunOut = pRun.inputStream.bufferedReader().readText()
                assertEquals("C++ profile exp=$expVal run failed:\n$pRunOut", 0, pRun.waitFor())
            }
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testRustAdversarialReaderHardening() {
        val candidates = listOf(File(System.getProperty("user.home"), ".cargo/bin/rustc"))
        val rustcFromPath = findExecutableOnPath("rustc")
        val rustc =
            (candidates + listOfNotNull(rustcFromPath)).firstOrNull {
                it.exists() && it.canExecute()
            }
        Assume.assumeNotNull("Skipping Rust adversarial test: rustc not found", rustc)

        val schema = loadSchema()
        val rustCode = RustGenerator(schema).generate()
        val tmpDir = Files.createTempDirectory("remote_compose_rust_security_test").toFile()
        try {
            val secMain = File(tmpDir, "security.rs")
            secMain.writeText(
                """
                $rustCode

                fn make_v6_header() -> WireBuffer {
                    let mut buf = WireBuffer::new();
                    buf.write_u8(OpId::Header.as_u8());
                    buf.write_i32(1);
                    buf.write_i32(1);
                    buf.write_i32(0);
                    buf.write_i32(100);
                    buf.write_i32(200);
                    buf.write_i64(0);
                    buf
                }

                fn main() {
                    // Operation enum size bounded by boxing large variants (>64B)
                    assert!(
                        std::mem::size_of::<Operation>() <= 64,
                        "Operation enum too large: {}",
                        std::mem::size_of::<Operation>()
                    );

                    // Negative string/buffer length -> WireError::InvalidData
                    let mut neg_str = WireBuffer::new();
                    neg_str.write_i32(-5);
                    let mut r = WireBuffer::from_bytes(neg_str.as_bytes());
                    assert_eq!(r.read_utf8(), Err(WireError::InvalidData));
                    let mut r2 = WireBuffer::from_bytes(neg_str.as_bytes());
                    assert_eq!(r2.read_buffer(), Err(WireError::InvalidData));

                    // Invalid UTF-8 rejected without advancing index
                    let bad_utf8: [u8; 6] = [0, 0, 0, 2, 0xFF, 0xFE];
                    let mut r_utf8 = WireBuffer::from_bytes(&bad_utf8);
                    assert_eq!(r_utf8.read_utf8(), Err(WireError::InvalidUtf8));
                    assert_eq!(r_utf8.remaining(), 2);

                    // Negative / invalid header magic -> InvalidHeaderMagic
                    for bad_magic in [i32::MIN, 0x12340001_i32, -1_i32] {
                        let mut buf = WireBuffer::new();
                        buf.write_u8(OpId::Header.as_u8());
                        buf.write_i32(bad_magic);
                        buf.write_i32(0);
                        buf.write_i32(0);
                        buf.write_i32(0);
                        buf.write_i32(0);
                        buf.write_i64(0);
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(
                            RemoteDocument::load(&mut reader),
                            Err(WireError::InvalidHeaderMagic(bad_magic))
                        );
                    }

                    // V7 num_props > MAX_HEADER_PROPERTIES -> InvalidData
                    {
                        let mut buf = WireBuffer::new();
                        buf.write_u8(OpId::Header.as_u8());
                        buf.write_i32(0x048C0001);
                        buf.write_i32(1);
                        buf.write_i32(0);
                        buf.write_i32(1001);
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(RemoteDocument::load(&mut reader), Err(WireError::InvalidData));
                    }

                    // V7 COMPRESS = 1 -> UnsupportedCompression
                    {
                        let mut buf = WireBuffer::new();
                        buf.write_u8(OpId::Header.as_u8());
                        buf.write_i32(0x048C0001);
                        buf.write_i32(1);
                        buf.write_i32(0);
                        buf.write_i32(1);
                        buf.write_i16(31);
                        buf.write_i16(4);
                        buf.write_i32(1);
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(
                            RemoteDocument::load(&mut reader),
                            Err(WireError::UnsupportedCompression)
                        );
                    }

                    // Mid-stream Header opcode -> InvalidHeader(0)
                    {
                        let mut buf = make_v6_header();
                        buf.write_u8(OpId::Header.as_u8());
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(
                            RemoteDocument::load(&mut reader),
                            Err(WireError::InvalidHeader(0))
                        );
                    }

                    // max_operations enforced -> LimitExceeded
                    {
                        let mut buf = make_v6_header();
                        for _ in 0..6 {
                            buf.write_u8(OpId::ContainerEnd.as_u8());
                        }
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(
                            RemoteDocument::load_with_limits(&mut reader, 5),
                            Err(WireError::LimitExceeded)
                        );
                    }

                    // Struct array (BitmapFontData Glyph[] = 20 bytes/elem) over-length -> Underflow
                    {
                        let mut buf = make_v6_header();
                        buf.write_u8(OpId::BitmapFontData.as_u8());
                        buf.write_i32(1);
                        buf.write_i32(10); // needs 200B
                        for _ in 0..19 {
                            buf.write_u8(0);
                        }
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(RemoteDocument::load(&mut reader), Err(WireError::Underflow));
                    }

                    // BitmapFontData signed version: i32::MIN -> version=-32768 must NOT read kerning
                    {
                        let mut buf = make_v6_header();
                        buf.write_u8(OpId::BitmapFontData.as_u8());
                        buf.write_i32(9);
                        buf.write_i32(i32::MIN); // version=-32768, num_glyphs=0
                        buf.write_u8(OpId::ContainerEnd.as_u8());
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        let doc = RemoteDocument::load(&mut reader).expect("Expected clean parse");
                        assert_eq!(doc.operations.len(), 2);
                        match &doc.operations[0] {
                            Operation::BitmapFontData(font) => {
                                assert!(font.glyphs.is_empty());
                                assert!(font.kerning_table.is_empty());
                            }
                            other => panic!("Expected BitmapFontData, got {:?}", other),
                        }
                        assert!(matches!(doc.operations[1], Operation::ContainerEnd(_)));
                    }

                    // AddMesh2D type=99 reads literal float mesh; ColorExpression mode=99 -> InvalidData
                    {
                        let mut buf = make_v6_header();
                        buf.write_u8(OpId::AddMesh2D.as_u8());
                        buf.write_i32(1);  // mesh_id
                        buf.write_i32(99); // fallback literal float mesh type
                        for _ in 0..5 {
                            buf.write_i32(0);
                        }
                        buf.write_i32(0); // indices len
                        buf.write_i32(2); // verts_len
                        buf.write_i32(0); // uv_len
                        buf.write_i32(0); // colors_len
                        buf.write_f32(3.0);
                        buf.write_f32(4.0);
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        let doc = RemoteDocument::load(&mut reader).expect("Expected AddMesh2D literal fallback");
                        assert_eq!(doc.operations.len(), 1);
                        match &doc.operations[0] {
                            Operation::AddMesh2D(mesh) => {
                                assert_eq!(mesh.verts.len(), 2);
                                assert_eq!(mesh.antialias_width, 1.0);
                            }
                            other => panic!("Expected AddMesh2D, got {:?}", other),
                        }

                        let mut bad_ce = make_v6_header();
                        bad_ce.write_u8(OpId::ColorExpression.as_u8());
                        bad_ce.write_i32(1);
                        bad_ce.write_i32(99); // invalid mode > 6
                        bad_ce.write_i32(0);
                        bad_ce.write_i32(0);
                        bad_ce.write_i32(0);
                        let mut ce_reader = WireBuffer::from_bytes(bad_ce.as_bytes());
                        assert_eq!(RemoteDocument::load(&mut ce_reader), Err(WireError::InvalidData));
                    }

                    // maxLength enforced even when backing buffer is large enough
                    {
                        // TextData.text maxLength = 4000
                        let mut buf = make_v6_header();
                        buf.write_u8(OpId::TextData.as_u8());
                        buf.write_i32(1);
                        let oversized = "a".repeat(4001);
                        buf.write_utf8(&oversized);
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(RemoteDocument::load(&mut reader), Err(WireError::InvalidData));
                    }
                    {
                        // TimeAttribute.args maxLength = 32
                        let mut buf = make_v6_header();
                        buf.write_u8(OpId::TimeAttribute.as_u8());
                        buf.write_i32(1);
                        buf.write_i32(2);
                        buf.write_i16(0);
                        buf.write_i16(33);
                        for i in 0..33 {
                            buf.write_i32(i);
                        }
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(RemoteDocument::load(&mut reader), Err(WireError::InvalidData));
                    }
                    {
                        // FloatExpression.expression maxLength = 32
                        let mut buf = make_v6_header();
                        buf.write_u8(OpId::FloatExpression.as_u8());
                        buf.write_i32(1);
                        buf.write_i32(33);
                        for _ in 0..33 {
                            buf.write_f32(1.0);
                        }
                        let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                        assert_eq!(RemoteDocument::load(&mut reader), Err(WireError::InvalidData));
                    }
                }
                """
                    .trimIndent()
            )
            val secBin = File(tmpDir, "rust_security_bin")
            val compileSec =
                ProcessBuilder(
                        rustc!!.absolutePath,
                        secMain.absolutePath,
                        "-o",
                        secBin.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()
            val secOut = compileSec.inputStream.bufferedReader().readText()
            assertEquals("Rust security compile failed:\n$secOut", 0, compileSec.waitFor())
            val runSec = ProcessBuilder(secBin.absolutePath).redirectErrorStream(true).start()
            val secRunOut = runSec.inputStream.bufferedReader().readText()
            assertEquals("Rust security run failed:\n$secRunOut", 0, runSec.waitFor())

            // Profile isolation: profile_widgets + profile_experimental enables FlowLayout,
            // rejects Custom (which is androidx + experimental only)
            val isoMain = File(tmpDir, "profile_iso.rs")
            isoMain.writeText(
                """
                $rustCode

                fn main() {
                    let _flow: fn(FlowLayout) -> Operation = Operation::FlowLayout;
                    let _ = _flow;
                    let mut buf = WireBuffer::new();
                    buf.write_u8(OpId::Header.as_u8());
                    buf.write_i32(1);
                    buf.write_i32(1);
                    buf.write_i32(0);
                    buf.write_i32(100);
                    buf.write_i32(200);
                    buf.write_i64(0);
                    buf.write_u8(OpId::Custom.as_u8());
                    let mut reader = WireBuffer::from_bytes(buf.as_bytes());
                    assert_eq!(
                        RemoteDocument::load(&mut reader),
                        Err(WireError::UnknownOpCode(OpId::Custom.as_u8()))
                    );
                }
                """
                    .trimIndent()
            )
            val isoBin = File(tmpDir, "rust_iso_bin")
            val compileIso =
                ProcessBuilder(
                        rustc.absolutePath,
                        isoMain.absolutePath,
                        "--cfg",
                        "feature=\"profile_widgets\"",
                        "--cfg",
                        "feature=\"profile_experimental\"",
                        "-o",
                        isoBin.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .start()
            val isoOut = compileIso.inputStream.bufferedReader().readText()
            assertEquals("Rust profile isolation compile failed:\n$isoOut", 0, compileIso.waitFor())
            val runIso = ProcessBuilder(isoBin.absolutePath).redirectErrorStream(true).start()
            val isoRunOut = runIso.inputStream.bufferedReader().readText()
            assertEquals("Rust profile isolation run failed:\n$isoRunOut", 0, runIso.waitFor())
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testPythonAdversarialReaderHardening() {
        val python3 = findExecutableOnPath("python3")
        Assume.assumeNotNull("Skipping Python adversarial test: python3 not found", python3)

        val schema = loadSchema()
        val pyCode = PythonGenerator(schema).generate()
        val tmpDir = Files.createTempDirectory("remote_compose_py_security_test").toFile()
        try {
            File(tmpDir, "remote_compose.py").writeText(pyCode)
            val testScript = File(tmpDir, "test_security.py")
            testScript.writeText(
                """
                import struct
                from remote_compose import HeaderProperty, OpCode, RemoteDocument, WireBuffer

                def assert_raises(exc_type, fn):
                    try:
                        fn()
                    except exc_type:
                        return
                    raise AssertionError(f"Expected {exc_type.__name__} to be raised")

                def make_v6_header() -> WireBuffer:
                    wb = WireBuffer()
                    wb.write_byte(OpCode.Header.value)
                    wb.write_int(1)
                    wb.write_int(1)
                    wb.write_int(0)
                    wb.write_int(100)
                    wb.write_int(200)
                    wb.write_long(0)
                    return wb

                # position setter bounds
                wb = WireBuffer.from_bytes(b"\x01\x02\x03")
                assert_raises(ValueError, lambda: setattr(wb, "position", -1))
                assert_raises(ValueError, lambda: setattr(wb, "position", 4))
                wb.position = 3
                assert wb.remaining() == 0

                # strict UTF-8 leaves position unchanged on failure
                bad_utf8 = WireBuffer.from_bytes(b"\x00\x00\x00\x02\xff\xfe")
                assert_raises(UnicodeDecodeError, bad_utf8.read_utf8)
                assert bad_utf8.position == 4

                # invalid / negative V7 magic rejected
                for bad_magic in (-2147483648, 0x12340001, -1):
                    b = WireBuffer()
                    b.write_byte(OpCode.Header.value)
                    b.write_int(bad_magic)
                    b.write_int(0)
                    b.write_int(0)
                    b.write_int(0)
                    b.write_int(0)
                    b.write_long(0)
                    assert_raises(ValueError, lambda data=b.get_bytes(): RemoteDocument.load(data))

                # num_props > MAX_HEADER_PROPERTIES rejected
                b = WireBuffer()
                b.write_byte(OpCode.Header.value)
                b.write_int(0x048C0001)
                b.write_int(1)
                b.write_int(0)
                b.write_int(1001)
                assert_raises(ValueError, lambda: RemoteDocument.load(b.get_bytes()))

                # V7 COMPRESS = 1 rejected
                b = WireBuffer()
                b.write_byte(OpCode.Header.value)
                b.write_int(0x048C0001)
                b.write_int(1)
                b.write_int(0)
                b.write_int(1)
                b.write_short(31)
                b.write_short(4)
                b.write_int(1)
                assert_raises(ValueError, lambda: RemoteDocument.load(b.get_bytes()))

                # mid-stream Header rejected
                b = make_v6_header()
                b.write_byte(OpCode.Header.value)
                assert_raises(ValueError, lambda: RemoteDocument.load(b.get_bytes()))

                # HeaderProperty.write rejects missing payload when tag dataType requires it
                bad_prop = HeaderProperty(tag=5, item_len=4)
                assert_raises(ValueError, lambda: bad_prop.write(WireBuffer()))

                # max_operations enforced
                b = make_v6_header()
                for _ in range(6):
                    b.write_byte(OpCode.ContainerEnd.value)
                assert_raises(ValueError, lambda: RemoteDocument.load(b.get_bytes(), max_operations=5))

                # Glyph[] struct array over-length rejected upfront with EOFError
                b = make_v6_header()
                b.write_byte(OpCode.BitmapFontData.value)
                b.write_int(1)
                b.write_int(10)  # needs 200B
                for _ in range(19):
                    b.write_byte(0)
                assert_raises(EOFError, lambda: RemoteDocument.load(b.get_bytes()))

                # sparse count overflow rejected with EOFError
                b = make_v6_header()
                b.write_byte(OpCode.CoreText.value)
                b.write_int(1)
                b.write_short(100)
                b.write_int(0)
                assert_raises(EOFError, lambda: RemoteDocument.load(b.get_bytes()))

                # BitmapFontData signed version: 0x80000000 -> -32768 must NOT read kerning
                b = make_v6_header()
                b.write_byte(OpCode.BitmapFontData.value)
                b.write_int(9)
                b.write_int(-2147483648)
                b.write_byte(OpCode.ContainerEnd.value)
                doc = RemoteDocument.load(b.get_bytes())
                assert len(doc.operations) == 2
                assert doc.operations[0].glyphs == []
                assert doc.operations[0].kerning_table == []
                assert type(doc.operations[1]).__name__ == "ContainerEnd"

                # AddMesh2D type=99 reads literal float mesh; ColorExpression mode=99 rejected
                b = make_v6_header()
                b.write_byte(OpCode.AddMesh2D.value)
                b.write_int(1)
                b.write_int(99)
                for _ in range(5):
                    b.write_int(0)
                b.write_int(0)  # indices
                b.write_int(2)  # verts_len
                b.write_int(0)  # uv_len
                b.write_int(0)  # colors_len
                b.write_float(3.0)
                b.write_float(4.0)
                doc = RemoteDocument.load(b.get_bytes())
                assert len(doc.operations) == 1
                assert doc.operations[0].verts == [3.0, 4.0]
                assert doc.operations[0].antialias_width == 1.0

                bad_ce = make_v6_header()
                bad_ce.write_byte(OpCode.ColorExpression.value)
                bad_ce.write_int(1)
                bad_ce.write_int(99)
                for _ in range(3):
                    bad_ce.write_int(0)
                assert_raises(ValueError, lambda: RemoteDocument.load(bad_ce.get_bytes()))

                # maxLength enforced even when backing buffer is large enough
                b = make_v6_header()
                b.write_byte(OpCode.TextData.value)
                b.write_int(1)
                b.write_utf8("a" * 4001)
                assert_raises(ValueError, lambda: RemoteDocument.load(b.get_bytes()))

                b = make_v6_header()
                b.write_byte(OpCode.TimeAttribute.value)
                b.write_int(1)
                b.write_int(2)
                b.write_short(0)
                b.write_short(33)
                for i in range(33):
                    b.write_int(i)
                assert_raises(ValueError, lambda: RemoteDocument.load(b.get_bytes()))

                b = make_v6_header()
                b.write_byte(OpCode.FloatExpression.value)
                b.write_int(1)
                b.write_int(33)
                for _ in range(33):
                    b.write_float(1.0)
                assert_raises(ValueError, lambda: RemoteDocument.load(b.get_bytes()))
                """
                    .trimIndent()
            )
            val pyRun =
                ProcessBuilder(python3!!.absolutePath, testScript.absolutePath)
                    .directory(tmpDir)
                    .redirectErrorStream(true)
                    .start()
            val pyOut = pyRun.inputStream.bufferedReader().readText()
            assertEquals("Python security test failed:\n$pyOut", 0, pyRun.waitFor())
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun testTypeScriptSwiftKotlinAdversarialCodegenInvariants() {
        val schema = loadSchema()

        // TypeScript invariants
        val tsCode = TypeScriptGenerator(schema).generate()
        assertFalse(
            "TS AddMesh2D must not self-shadow vertsLen",
            tsCode.contains("const vertsLen = vertsLen"),
        )
        assertTrue(tsCode.contains("fatal: true"))
        assertTrue(tsCode.contains("ignoreBOM: true"))
        assertTrue(tsCode.contains("MAX_HEADER_PROPERTIES"))
        assertTrue(tsCode.contains("MAX_OPERATION_COUNT"))
        assertTrue(tsCode.contains("Unexpected Header opcode in document body"))
        assertTrue(tsCode.contains("Compressed RemoteCompose documents are not supported"))
        assertTrue(tsCode.contains("Invalid header magic number"))
        assertTrue(tsCode.contains("_glyphsCount * 20 > buffer.remaining()"))
        assertTrue(tsCode.contains("sparseCount * 2 > buffer.remaining()"))
        assertTrue(tsCode.contains("Invalid enum value for mode:"))
        assertTrue(tsCode.contains("Invalid enum value for attributeType:"))
        assertTrue(tsCode.contains("buffer.readUTF8(4000)"))
        assertTrue(tsCode.contains("buffer.readBuffer(800000)"))
        assertTrue(tsCode.contains("(raw_versionAndNumGlyphs >> 16)"))
        assertTrue(tsCode.contains("(this.widthAndType & 0xFFFF)"))
        assertTrue(tsCode.contains("((this.widthAndType >>> 16) & 0xFFFF)"))

        // Swift invariants
        val swiftCode = SwiftGenerator(schema).generate()
        assertFalse(
            "Swift AddMesh2D must not self-shadow vertsLen",
            swiftCode.contains("let vertsLen = Int(vertsLen)"),
        )
        assertFalse("Swift must not use unaligned .load(as:)", swiftCode.contains(".load(as:"))
        assertFalse("Swift must not silently swallow invalid UTF-8", swiftCode.contains("?? \"\""))
        assertFalse(
            "Swift must not use overflowing index + len <= data.count",
            swiftCode.contains("index + len <= data.count"),
        )
        assertTrue(swiftCode.contains("data.startIndex == 0 ? data : Data(data)"))
        assertTrue(swiftCode.contains("public private(set) var index: Int = 0"))
        assertTrue(swiftCode.contains("MAX_HEADER_PROPERTIES"))
        assertTrue(swiftCode.contains("MAX_OPERATION_COUNT"))
        assertTrue(swiftCode.contains("Unexpected Header opcode inside document body"))
        assertTrue(swiftCode.contains("Compressed RemoteCompose documents are not supported"))
        assertTrue(swiftCode.contains("Invalid header magic number"))
        assertTrue(swiftCode.contains("guard _glyphsCount <= buffer.remaining / 20 else"))
        assertTrue(swiftCode.contains("guard sparseCount <= buffer.remaining / 2 else"))
        assertTrue(swiftCode.contains("Invalid enum value for mode"))
        assertTrue(swiftCode.contains("Invalid enum value for attributeType"))
        assertTrue(swiftCode.contains("buffer.readUTF8(maxLength: 4000)"))
        assertTrue(swiftCode.contains("buffer.readBuffer(maxLength: 800000)"))
        assertTrue(swiftCode.contains("(raw_versionAndNumGlyphs >> 16)"))
        assertTrue(swiftCode.contains("Int32(UInt32(bitPattern: widthAndType) & 0xFFFF)"))
        assertTrue(swiftCode.contains("Int32((UInt32(bitPattern: widthAndType) >> 16) & 0xFFFF)"))

        // Kotlin invariants
        val ktCode = KotlinGenerator(schema).generateAstNodes()
        assertTrue(ktCode.contains("require(count <= buffer.remaining() / 20)"))
        assertTrue(ktCode.contains("require(sparseCount <= buffer.remaining() / 2)"))
        assertTrue(ktCode.contains("Invalid ColorExpressionNode.mode:"))
        assertTrue(ktCode.contains("Invalid GraphicsLayerAttribute.attributeType:"))
        assertTrue(ktCode.contains("buffer.readUTF8(4000)"))
        assertTrue(ktCode.contains("buffer.readBuffer(800000)"))
        assertTrue(ktCode.contains("val version = (raw_versionAndNumGlyphs shr 16)"))
        assertTrue(ktCode.contains("get() = widthAndType and 0xFFFF"))
        assertTrue(ktCode.contains("get() = (widthAndType ushr 16) and 0xFFFF"))
    }

    @Test
    fun testSchemaValidationAndSecurityMetadata() {
        val schema = loadSchema()

        // BitmapData 16-bit dimensions and upper 16-bit type/encoding
        val bitmapData = schema.operationsByName["BitmapData"]!!
        val widthAndType = bitmapData.fields.first { it.name == "widthAndType" }
        assertEquals("0..15", widthAndType.bits!!.first { it.name == "width" }.range)
        assertEquals("16..31", widthAndType.bits!!.first { it.name == "imageType" }.range)
        val heightAndEncoding = bitmapData.fields.first { it.name == "heightAndEncoding" }
        assertEquals("0..15", heightAndEncoding.bits!!.first { it.name == "height" }.range)
        assertEquals(
            "16..31",
            heightAndEncoding.bits!!.first { it.name == "bitmapEncoding" }.range,
        )

        // BitmapFontData signed version bitfield
        val bitmapFontData = schema.operationsByName["BitmapFontData"]!!
        val versionField =
            bitmapFontData.fields
                .first { it.name == "versionAndNumGlyphs" }
                .bits!!
                .first { it.name == "version" }
        assertTrue("BitmapFontData.version must be signed", versionField.signed)

        // AddMesh2D literal fallback & default antialiasWidth = 1.0
        val addMesh = schema.operationsByName["AddMesh2D"]!!
        assertFalse(addMesh.fields.first { it.name == "type" }.strictEnum)
        assertEquals(
            "type != 0 && type != 3 && type != 4",
            addMesh.fields.first { it.name == "indices" }.presentIf,
        )
        assertEquals(
            "type != 0 && type != 2 && type != 3 && type != 4",
            addMesh.fields.first { it.name == "verts" }.presentIf,
        )
        assertEquals(
            "1.0",
            addMesh.fields.first { it.name == "antialiasWidth" }.defaultValue,
        )
        assertEquals(
            "Limits.MAX_EXPRESSION_SIZE",
            addMesh.fields.first { it.name == "expX" }.javaLimit,
        )
        assertEquals(
            "Limits.MAX_MESH_2D_INDICES",
            addMesh.fields.first { it.name == "indices" }.javaLimit,
        )
        assertEquals(
            "Limits.MAX_MESH_2D_VERTICES * 2",
            addMesh.fields.first { it.name == "verts" }.javaLimit,
        )
        assertEquals(
            "Limits.MAX_MESH_2D_VERTICES",
            addMesh.fields.first { it.name == "colors" }.javaLimit,
        )
        assertEquals(
            "Limits.MAX_MESH_2D_WIDTH_SAMPLES",
            addMesh.fields.first { it.name == "widths" }.javaLimit,
        )

        // Header full schema parity (A7-A10, C4)
        val headerOp = schema.operationsByName["Header"]!!
        assertEquals(
            "majorVersion < 65536",
            headerOp.fields.first { it.name == "width" }.presentIf,
        )
        val headerPropsField = headerOp.fields.first { it.name == "properties" }
        assertEquals("majorVersion >= 65536", headerPropsField.presentIf)
        assertEquals(1000, headerPropsField.maxLength)
        assertEquals(27, headerOp.extensions!!.supportedTags.size)
        val headerPropStruct = schema.structs["HeaderProperty"]!!
        assertEquals(
            "0..5",
            headerPropStruct.fields
                .first { it.name == "tag" }
                .bits!!
                .first { it.name == "key" }
                .range,
        )

        // TouchExpression 16-bit expressionLen (A12)
        val touchOp = schema.operationsByName["TouchExpression"]!!
        assertEquals(
            "expLen",
            touchOp.fields.first { it.name == "expression" }.countFrom,
        )
        assertEquals(
            "Limits.MAX_EXPRESSION_SIZE",
            touchOp.fields.first { it.name == "expression" }.javaLimit,
        )
        assertEquals(
            "Limits.MAX_TOUCH_STOPS",
            touchOp.fields.first { it.name == "stopSpec" }.javaLimit,
        )
        assertEquals(
            "Limits.MAX_EASING_LEN",
            touchOp.fields.first { it.name == "easingSpec" }.javaLimit,
        )

        // GraphicsLayerModifierOperation & GraphicsLayerAttribute (A13-A16, C7)
        val glOp = schema.operationsByName["GraphicsLayerModifierOperation"]!!
        assertEquals(22, glOp.fields.first { it.name == "attributes" }.maxLength)
        val glStruct = schema.structs["GraphicsLayerAttribute"]!!
        val glTagBits = glStruct.fields.first { it.name == "tag" }.bits!!
        assertEquals("0..5", glTagBits.first { it.name == "attributeType" }.range)
        assertTrue(glTagBits.first { it.name == "attributeType" }.strictEnum)
        assertEquals("10..31", glTagBits.first { it.name == "dataType" }.range)
        assertTrue(glTagBits.first { it.name == "dataType" }.signed)
        assertEquals(
            "(tag >> 10) == 0 || (tag >> 10) == 1",
            glStruct.fields.first { it.name == "value" }.presentIf,
        )

        // DrawBitmapTextAnchored conditional bit range (A17)
        val anchoredOp = schema.operationsByName["DrawBitmapTextAnchored"]!!
        val anchoredTextBit =
            anchoredOp.fields.first { it.name == "textId" }.bits!!.first { it.name == "textId" }
        assertEquals(31, anchoredTextBit.whenBitSet?.bit)
        assertEquals("0..15", anchoredTextBit.whenBitSet?.range)

        // FloatConstant float_or_id (A18)
        val floatConstOp = schema.operationsByName["FloatConstant"]!!
        assertEquals("float_or_id", floatConstOp.fields.first { it.name == "value" }.type)

        // Sparse framing metadata (A19, A20, C6)
        for (spOpName in listOf("CoreText", "TextStyle")) {
            val spOp = schema.operationsByName[spOpName]!!
            assertEquals("ushort", spOp.sparseCountType)
            assertEquals("ubyte", spOp.sparseKeyType)
            assertEquals(
                "ushort",
                spOp.sparseParameters!!.first { it.id == 20 }.lengthType,
            )
            assertEquals(
                "ushort",
                spOp.sparseParameters!!.first { it.id == 21 }.lengthType,
            )
        }
        val textStyleOp = schema.operationsByName["TextStyle"]!!
        assertTrue(textStyleOp.sparseParameters!!.first { it.id == 2 }.ignored)
        assertTrue(textStyleOp.sparseParameters!!.first { it.id == 23 }.ignored)

        // Enums & all containers closedBy ContainerEnd (C1, C5)
        val themeEnums =
            schema.operationsByName["Theme"]!!.fields.first { it.name == "themeId" }.enumValues!!
        assertEquals(-3, themeEnums["LIGHT"])
        assertEquals(-2, themeEnums["DARK"])
        assertEquals(-1, themeEnums["UNSPECIFIED"])
        val nvEnums =
            schema.operationsByName["NamedVariable"]!!
                .fields
                .first { it.name == "varType" }
                .enumValues!!
        assertEquals(0, nvEnums["STRING"])
        assertEquals(4, nvEnums["INT"])
        assertEquals(2, nvEnums["COLOR"])
        assertEquals(
            1,
            schema.operationsByName["BoxLayout"]!!
                .fields
                .first { it.name == "horizontalPositioning" }
                .enumValues!!["START"],
        )
        assertEquals(
            6,
            schema.operationsByName["ConditionalOperations"]!!
                .fields
                .first { it.name == "type" }
                .enumValues!!["TYPE_CHANGED"],
        )
        for (op in schema.operations.filter { it.isContainer }) {
            assertEquals("ContainerEnd", op.closedBy)
        }
        val soundParams =
            schema.operationsByName["SoundExpression"]!!.fields.first { it.name == "params" }
        assertEquals(64, soundParams.maxLength)
        assertEquals("SoundExpression.MAX_PARAMS", soundParams.javaLimit)

        // Nested operation payload metadata
        val skip = schema.operationsByName["Skip"]!!
        assertEquals("operations", skip.fields.first { it.name == "skippedBytes" }.payloadKind)
        val patternDefine = schema.operationsByName["PatternDefine"]!!
        assertEquals(
            "operations",
            patternDefine.fields.first { it.name == "bodyBytes" }.payloadKind,
        )

        // Explicit lengthType, maxLength, and javaLimit
        for (opName in listOf("ImageAttribute", "TimeAttribute")) {
            val args = schema.operationsByName[opName]!!.fields.first { it.name == "args" }
            assertEquals("short", args.lengthType)
            assertEquals(32, args.maxLength)
            assertEquals("Limits.MAX_FUNCTION_ARGUMENTS", args.javaLimit)
        }
        val fontData = schema.operationsByName["FontData"]!!.fields.first { it.name == "fontData" }
        assertEquals(800000, fontData.maxLength)
        assertEquals("Limits.MAX_FONT_DATA", fontData.javaLimit)

        // int_id type declaration & polymorphic conditionalTypes / conditionalRoles
        val intIdType = schema.types["int_id"]!!
        assertEquals("INT32", intIdType.wire)
        assertEquals("int", intIdType.base)

        val customPropRaw =
            schema.structs["CustomProperty"]!!.fields.first { it.name == "rawValue" }
        assertEquals(
            "int",
            customPropRaw.conditionalTypes!!.first { it.condition == "dataType == 0" }.type,
        )
        assertEquals(
            "float_or_id",
            customPropRaw.conditionalTypes!!
                .first { it.condition == "dataType == 1 || dataType == 3" }
                .type,
        )
        assertEquals(
            "color",
            customPropRaw.conditionalTypes!!.first { it.condition == "dataType == 8" }.type,
        )
        assertEquals(
            "int_id",
            customPropRaw.conditionalTypes!!
                .first {
                    it.condition ==
                        "dataType == 2 || dataType == 4 || dataType == 5 || dataType == 6 || dataType == 7 || dataType == 9"
                }
                .type,
        )
        val customPropRoles = customPropRaw.conditionalRoles!!
        assertEquals(
            "text",
            customPropRoles.first { it.condition == "dataType == 2 || dataType == 4" }.idKind,
        )
        assertEquals(
            "int",
            customPropRoles.first { it.condition == "dataType == 5 || dataType == 9" }.idKind,
        )
        assertEquals(
            "color",
            customPropRoles.first { it.condition == "dataType == 6 || dataType == 7" }.idKind,
        )

        val colorExprOp = schema.operationsByName["ColorExpression"]!!
        val ceArg1 = colorExprOp.fields.first { it.name == "arg1" }
        assertEquals(
            "int_id",
            ceArg1.conditionalTypes!!
                .first { it.condition == "(flagsAndMode & 255) == 1 || (flagsAndMode & 255) == 3" }
                .type,
        )
        assertEquals(
            "color",
            ceArg1.conditionalTypes!!
                .first { it.condition == "(flagsAndMode & 255) == 0 || (flagsAndMode & 255) == 2" }
                .type,
        )
        val ceArg2 = colorExprOp.fields.first { it.name == "arg2" }
        assertEquals(
            "int_id",
            ceArg2.conditionalTypes!!
                .first { it.condition == "(flagsAndMode & 255) == 2 || (flagsAndMode & 255) == 3" }
                .type,
        )
        val textLayoutColor =
            schema.operationsByName["TextLayout"]!!.fields.first { it.name == "color" }
        assertEquals(
            "int_id",
            textLayoutColor.conditionalTypes!!
                .first { it.condition == "(textAlign & 65536) != 0" }
                .type,
        )
        assertEquals(
            "color",
            textLayoutColor.conditionalTypes!!
                .first { it.condition == "(textAlign & 65536) == 0" }
                .type,
        )

        // Direct int_id field codegen across all target generators
        val intIdSchemaJson =
            """
            {
              "schemaVersion": "1.0.0",
              "targetApiLevel": 8,
              "types": {
                "int": { "wire": "INT32", "description": "32-bit int" },
                "long": { "wire": "INT64", "description": "64-bit long" },
                "int_id": { "wire": "INT32", "base": "int", "description": "32-bit ID" }
              },
              "categories": [{ "name": "Test", "description": "Test category" }],
              "operations": [
                {
                  "id": 0,
                  "name": "Header",
                  "category": "Test",
                  "description": "Header",
                  "fields": [
                    { "name": "majorVersion", "type": "int" },
                    { "name": "minorVersion", "type": "int" },
                    { "name": "patchVersion", "type": "int" },
                    { "name": "width", "type": "int" },
                    { "name": "height", "type": "int" },
                    { "name": "capabilities", "type": "long" }
                  ]
                },
                {
                  "id": 210,
                  "name": "IntIdOp",
                  "category": "Test",
                  "description": "Op with int_id field",
                  "fields": [
                    { "name": "targetId", "type": "int_id", "description": "Target ID" }
                  ]
                }
              ]
            }
            """
                .trimIndent()
        assertEquals(emptyList<String>(), JsonSchemaParser.validate(JSONObject(intIdSchemaJson)))
        val intIdSchema = JsonSchemaParser.parse(intIdSchemaJson.byteInputStream())
        assertTrue(KotlinGenerator(intIdSchema).generateAstNodes().contains("val targetId: Int"))
        assertTrue(
            KotlinGenerator(intIdSchema).generateWriterNodes().contains("buffer.writeInt(targetId)")
        )
        assertTrue(CppGenerator(intIdSchema).generateHeader().contains("int32_t targetId{};"))
        assertTrue(RustGenerator(intIdSchema).generate().contains("pub target_id: i32"))
        assertTrue(TypeScriptGenerator(intIdSchema).generate().contains("targetId: number = 0"))
        val swiftIntId = SwiftGenerator(intIdSchema).generate()
        assertTrue(
            swiftIntId.contains("public var targetId: Int32") &&
                swiftIntId.contains("targetId: Int32 = 0")
        )
        assertTrue(PythonGenerator(intIdSchema).generate().contains("target_id: int = 0"))
        assertEquals("int", JavaGenerator.mapToJavaType("int_id"))
        assertEquals("DocumentedOperation.INT", JavaGenerator.mapToJavaDocType("int_id"))

        // Validator rejects invalid strictEnum, maxLength, and payloadKind
        val invalidRoot =
            JSONObject(
                """
                {
                  "schemaVersion": "1.0.0",
                  "targetApiLevel": 8,
                  "types": {
                    "int": { "size": 4 },
                    "string": { "size": -1 }
                  },
                  "categories": ["Test"],
                  "operations": [
                    {
                      "id": 250,
                      "name": "BadOp",
                      "category": "Test",
                      "description": "Invalid test op",
                      "fields": [
                        {
                          "name": "badEnum",
                          "type": "int",
                          "strictEnum": true,
                          "description": "strictEnum without enum"
                        },
                        {
                          "name": "badMax",
                          "type": "string",
                          "maxLength": -1,
                          "description": "negative maxLength"
                        },
                        {
                          "name": "badPayload",
                          "type": "int",
                          "payloadKind": "operations",
                          "description": "payloadKind on non-bytes"
                        }
                      ]
                    }
                  ]
                }
                """
                    .trimIndent()
            )
        val errors = JsonSchemaParser.validate(invalidRoot)
        assertTrue(
            "Expected strictEnum validation error, got: $errors",
            errors.any { it.contains("strictEnum") },
        )
        assertTrue(
            "Expected maxLength validation error, got: $errors",
            errors.any { it.contains("maxLength") },
        )
        assertTrue(
            "Expected payloadKind validation error, got: $errors",
            errors.any { it.contains("payloadKind") },
        )
    }

    @Test
    fun testCodeReviewFixesAcrossAllGenerators() {
        // 1. CodegenExprUtils.extractBitExpr rejects out-of-range whenBitSet.bit
        for (badBit in listOf(-1, 32)) {
            val badBitField =
                SchemaBitField(
                    name = "value",
                    range = "0..15",
                    whenBitSet = SchemaConditionalBitRange(bit = badBit, range = "0..7"),
                )
            try {
                CodegenExprUtils.extractBitExpr("raw", badBitField, ExprLang.KOTLIN)
                fail("Expected IllegalArgumentException for whenBitSet.bit=$badBit")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message?.contains("$badBit") == true)
            }
        }

        // 2. ConditionParser masks Kotlin unsigned byte with (x.toInt() and 0xFF) and casts
        // signed short with x.toInt()
        assertEquals(
            "(flagByte.toInt() and 0xFF) == 1",
            CodegenExprUtils.translateCondition(
                "flagByte == 1",
                ExprLang.KOTLIN,
                needsKotlinIntCast = { false },
                isKotlinUnsignedByte = { it == "flagByte" },
            ) {
                it
            },
        )
        assertEquals(
            "(((flagByte.toInt() and 0xFF) shr 4)) == 12",
            CodegenExprUtils.translateCondition(
                "(flagByte >> 4) == 12",
                ExprLang.KOTLIN,
                needsKotlinIntCast = { false },
                isKotlinUnsignedByte = { it == "flagByte" },
            ) {
                it
            },
        )
        assertEquals(
            "flagShort.toInt() == 1",
            CodegenExprUtils.translateCondition(
                "flagShort == 1",
                ExprLang.KOTLIN,
                needsKotlinIntCast = { it == "flagShort" },
                isKotlinUnsignedByte = { false },
            ) {
                it
            },
        )
        assertEquals(
            "((flagShort.toInt() and 1)) != 0",
            CodegenExprUtils.translateCondition(
                "(flagShort & 1) != 0",
                ExprLang.KOTLIN,
                needsKotlinIntCast = { it == "flagShort" },
            ) {
                it
            },
        )
        assertEquals(
            "((flagShort.toInt() shr 8)) == 2",
            CodegenExprUtils.translateCondition(
                "(flagShort >> 8) == 2",
                ExprLang.KOTLIN,
                needsKotlinIntCast = { it == "flagShort" },
            ) {
                it
            },
        )
        assertEquals(
            "flagBool",
            CodegenExprUtils.translateCondition(
                "flagBool",
                ExprLang.KOTLIN,
                needsKotlinIntCast = { it == "flagShort" },
                isKotlinUnsignedByte = { it == "flagByte" },
            ) {
                it
            },
        )

        // 3. JsonSchemaParser.validate checks structs, whenBitSet, conditionalTypes, sparse types
        val invalidMetadataRoot =
            JSONObject(
                """
                {
                  "schemaVersion": "1.0.0",
                  "targetApiLevel": 8,
                  "types": { "int": { "size": 4 } },
                  "categories": ["Test"],
                  "structs": {
                    "BadStruct": {
                      "description": "Struct with bad bitfield",
                      "fields": [
                        {
                          "name": "packed",
                          "type": "int",
                          "bits": [
                            {
                              "name": "v",
                              "range": "0..15",
                              "whenBitSet": { "bit": 32, "range": "0..7" }
                            }
                          ]
                        }
                      ]
                    }
                  },
                  "operations": [
                    {
                      "id": 251,
                      "name": "BadSparseOp",
                      "category": "Test",
                      "description": "Op with bad sparse and conditionalTypes metadata",
                      "sparseCountType": "long",
                      "sparseKeyType": "float",
                      "fields": [
                        {
                          "name": "x",
                          "type": "int",
                          "lengthType": "bogus",
                          "conditionalTypes": [
                            { "when": "x == 1", "type": "UnknownType" }
                          ]
                        }
                      ],
                      "sparseParameters": [
                        {
                          "id": 1,
                          "name": "arr",
                          "type": "int[]",
                          "lengthType": "badlen"
                        }
                      ]
                    }
                  ]
                }
                """
                    .trimIndent()
            )
        val metaErrors = JsonSchemaParser.validate(invalidMetadataRoot)
        assertTrue(
            "Expected whenBitSet validation error, got: $metaErrors",
            metaErrors.any { it.contains("whenBitSet.bit") && it.contains("BadStruct") },
        )
        assertTrue(
            "Expected sparseCountType validation error, got: $metaErrors",
            metaErrors.any { it.contains("sparseCountType") },
        )
        assertTrue(
            "Expected sparseKeyType validation error, got: $metaErrors",
            metaErrors.any { it.contains("sparseKeyType") },
        )
        assertTrue(
            "Expected sparse parameter lengthType validation error, got: $metaErrors",
            metaErrors.any { it.contains("sparse parameter arr") && it.contains("lengthType") },
        )
        assertTrue(
            "Expected conditionalTypes validation error, got: $metaErrors",
            metaErrors.any { it.contains("conditionalTypes") },
        )
        assertTrue(
            "Expected missing encoding == 'sparse' validation error, got: $metaErrors",
            metaErrors.any {
                it.contains("declares sparseParameters without encoding == 'sparse'")
            },
        )

        // 4-7. Synthetic schema exercising:
        // - Nullable presentIf + packed !unpackBits strictEnum (C++ (*field), Kotlin/Rust/Swift/TS)
        // - addedVersion + packed !unpackBits strictEnum (Kotlin)
        // - unpackBits = false + sparseParameters in KotlinProtocolAndSinkGenerator convenience
        // overload
        // - sparseCountType = "int", sparseKeyType = "short", sparse param lengthType = "int"
        // matching read & write across all 6 generators
        // - Rust write() / save() returning Result<(), WireError::MissingRequiredField>
        val syntheticJson =
            """
            {
              "schemaVersion": "1.0.0",
              "targetApiLevel": 8,
              "types": {
                "int": { "wire": "int", "description": "32-bit int" },
                "short": { "wire": "short", "description": "16-bit short" },
                "byte": { "wire": "byte", "description": "8-bit byte" },
                "long": { "wire": "long", "description": "64-bit long" },
                "int[]": { "wire": "int[]", "description": "int array" }
              },
              "categories": [{ "name": "Test", "description": "Test category" }],
              "operations": [
                {
                  "id": 0,
                  "name": "Header",
                  "category": "Test",
                  "description": "Header op",
                  "fields": [
                    { "name": "majorVersion", "type": "int" },
                    { "name": "minorVersion", "type": "int" },
                    { "name": "patchVersion", "type": "int" },
                    { "name": "width", "type": "int" },
                    { "name": "height", "type": "int" },
                    { "name": "capabilities", "type": "long" }
                  ]
                },
                {
                  "id": 200,
                  "name": "SyntheticTestOp",
                  "category": "Test",
                  "description": "Synthetic operation for codegen review checks",
                  "encoding": "sparse",
                  "sparseCountType": "int",
                  "sparseKeyType": "short",
                  "fields": [
                    { "name": "flags", "type": "byte", "description": "Condition byte" },
                    {
                      "name": "optPacked",
                      "type": "int",
                      "presentIf": "flags == 1",
                      "unpackBits": false,
                      "bits": [
                        {
                          "name": "mode",
                          "range": "0..7",
                          "enum": { "A": 1, "B": 2 },
                          "strictEnum": true
                        }
                      ]
                    },
                    {
                      "name": "versionedPacked",
                      "type": "int",
                      "addedVersion": 7,
                      "defaultValue": "1",
                      "unpackBits": false,
                      "bits": [
                        {
                          "name": "vMode",
                          "range": "0..7",
                          "enum": { "X": 1, "Y": 3 },
                          "strictEnum": true
                        }
                      ]
                    }
                  ],
                  "sparseParameters": [
                    {
                      "id": 10,
                      "name": "sparseInts",
                      "type": "int[]",
                      "lengthType": "int",
                      "description": "Sparse int array with 32-bit length"
                    },
                    {
                      "id": 11,
                      "name": "sparseSmall",
                      "type": "int[]",
                      "lengthType": "ubyte",
                      "description": "Sparse int array with 8-bit length"
                    },
                    {
                      "id": 12,
                      "name": "sparseSignedShort",
                      "type": "int[]",
                      "lengthType": "short",
                      "description": "Sparse int array with signed 16-bit length"
                    }
                  ]
                },
                {
                  "id": 201,
                  "name": "SignedShortSparseCountOp",
                  "category": "Test",
                  "description": "Synthetic operation with signed 16-bit sparse count",
                  "encoding": "sparse",
                  "sparseCountType": "short",
                  "sparseKeyType": "ubyte",
                  "fields": [],
                  "sparseParameters": [
                    { "id": 1, "name": "value", "type": "int", "description": "Sparse int value" }
                  ]
                }
              ]
            }
            """
                .trimIndent()
        assertEquals(emptyList<String>(), JsonSchemaParser.validate(JSONObject(syntheticJson)))
        val synSchema = JsonSchemaParser.parse(syntheticJson.byteInputStream())

        // Verify KotlinGenerator & KotlinProtocolAndSinkGenerator
        val ktAst = KotlinGenerator(synSchema).generateAstNodes()
        val ktWriter = KotlinGenerator(synSchema).generateWriterNodes()
        val ktSink = KotlinProtocolAndSinkGenerator(synSchema).generateOperationSink()
        assertTrue(
            "Kotlin must mask unsigned byte condition with and 0xFF",
            ktAst.contains("(flags.toInt() and 0xFF) == 1"),
        )
        assertTrue(
            "Kotlin must validate packed bit strictEnum inside presentIf",
            ktAst.contains("val bit_mode = (parsed and 0xFF)") &&
                ktAst.contains("Invalid SyntheticTestOpNode.mode:"),
        )
        assertTrue(
            "Kotlin must validate packed bit strictEnum inside addedVersion",
            ktAst.contains("val bit_vMode = (parsed and 0xFF)") &&
                ktAst.contains("Invalid SyntheticTestOpNode.vMode:"),
        )
        assertTrue(
            "Kotlin sparse read must use readInt count",
            ktAst.contains("val sparseCount = buffer.readInt()"),
        )
        assertTrue(
            "Kotlin signed short sparse count must sign-extend readShort()",
            ktAst.contains("val sparseCount = buffer.readShort().toShort().toInt()"),
        )
        assertTrue(
            "Kotlin signed short sparse key must sign-extend readShort()",
            ktAst.contains("when (val paramId = buffer.readShort().toShort().toInt())"),
        )
        assertTrue(
            "Kotlin signed short array length must sign-extend readShort()",
            ktAst.contains("val count = buffer.readShort().toShort().toInt()"),
        )
        assertTrue(
            "Kotlin sparse read must use readByte ubyte len",
            ktAst.contains("buffer.readByte() and 0xFF"),
        )
        assertTrue(
            "Kotlin sparse write must use writeInt count",
            ktWriter.contains("buffer.writeInt(sparseCount)"),
        )
        assertTrue(
            "Kotlin sparse write must use writeShort key",
            ktWriter.contains("buffer.writeShort(10)"),
        )
        assertTrue(
            "Kotlin sparse write must use writeInt array len",
            ktWriter.contains("buffer.writeInt(sp_sparseInts.size)"),
        )
        assertTrue(
            "Kotlin sparse write must use writeByte array len",
            ktWriter.contains("buffer.writeByte(sp_sparseSmall.size)"),
        )
        assertTrue(
            "Kotlin sink unpacked overload must include and forward sparseParameters",
            ktSink.contains("sparseInts: IntArray? = null") &&
                ktSink.contains("sparseSmall: IntArray? = null"),
        )

        // Verify CppGenerator
        val cppGen = CppGenerator(synSchema)
        val cppSource = cppGen.generateSource()
        assertTrue(
            "C++ must dereference std::optional for packed bit strictEnum",
            cppSource.contains("static_cast<uint32_t>((*optPacked))"),
        )
        assertTrue(
            "C++ sparse read must use readInt count",
            cppSource.contains("int32_t sparseCount = buffer.readInt();"),
        )
        assertTrue(
            "C++ sparse write must use writeInt count",
            cppSource.contains("buffer.writeInt(sparseCount);"),
        )
        assertTrue(
            "C++ sparse write must use writeInt array len",
            cppSource.contains("buffer.writeInt(static_cast<int32_t>((*sparseInts).size()));"),
        )
        assertTrue(
            "C++ sparse write must use writeByte array len",
            cppSource.contains("buffer.writeByte(static_cast<uint8_t>((*sparseSmall).size()));"),
        )

        // Verify RustGenerator
        val rustCode = RustGenerator(synSchema).generate()
        assertTrue(
            "Rust write must return Result<(), WireError>",
            rustCode.contains(
                "pub fn write(&self, buffer: &mut WireBuffer) -> Result<(), WireError>"
            ),
        )
        assertTrue(
            "Rust save must return Result<(), WireError>",
            rustCode.contains(
                "pub fn save(&self, buffer: &mut WireBuffer) -> Result<(), WireError>"
            ),
        )
        assertTrue(
            "Rust must return WireError::MissingRequiredField instead of panicking",
            rustCode.contains("ok_or(WireError::MissingRequiredField(\"optPacked\"))?"),
        )
        assertTrue(
            "Rust must validate packed bit strictEnum inside presentIf",
            rustCode.contains("let bit_mode = ((val as u32) & 0xFF) as i32;"),
        )
        assertTrue(
            "Rust sparse read must use read_i32 count",
            rustCode.contains("let sparse_raw_count = buffer.read_i32()?;"),
        )
        assertTrue(
            "Rust sparse write must use write_i32 count",
            rustCode.contains("buffer.write_i32(sparse_count as i32);"),
        )
        assertTrue(
            "Rust sparse write must use write_i32 array len",
            rustCode.contains("buffer.write_i32(val.len() as i32);"),
        )
        assertTrue(
            "Rust sparse write must use write_u8 array len",
            rustCode.contains("buffer.write_u8(val.len() as u8);"),
        )

        // Verify TypeScriptGenerator
        val tsCode = TypeScriptGenerator(synSchema).generate()
        assertTrue(
            "TS must validate packed bit strictEnum inside presentIf",
            tsCode.contains("const bit_mode = (this.optPacked & 0xFF);"),
        )
        assertTrue(
            "TS sparse read must use readInt count",
            tsCode.contains("const sparseCount = buffer.readInt();"),
        )
        assertTrue(
            "TS sparse write must use writeInt count",
            tsCode.contains("buffer.writeInt(sparseCount);"),
        )
        assertTrue(
            "TS sparse write must use writeInt array len",
            tsCode.contains("buffer.writeInt(this.sparseInts.length);"),
        )
        assertTrue(
            "TS sparse write must use writeByte array len",
            tsCode.contains("buffer.writeByte(this.sparseSmall.length);"),
        )

        // Verify SwiftGenerator
        val swiftCode = SwiftGenerator(synSchema).generate()
        assertTrue(
            "Swift protocol write must throw",
            swiftCode.contains("func write(to buffer: inout WireBuffer) throws"),
        )
        assertTrue(
            "Swift save must throw and forward try op.write",
            swiftCode.contains("public func save() throws -> Data") &&
                swiftCode.contains("try op.write(to: &buffer)"),
        )
        assertTrue(
            "Swift must throw NSError instead of preconditionFailure on missing required field",
            swiftCode.contains(
                "throw NSError(domain: \"SyntheticTestOp\", code: 3, userInfo: [NSLocalizedDescriptionKey: \"Missing required field optPacked\"])"
            ) && !swiftCode.contains("preconditionFailure"),
        )
        assertTrue(
            "Swift must validate packed bit strictEnum on non-optional parsed inside presentIf",
            swiftCode.contains("let parsed = try buffer.readInt()") &&
                swiftCode.contains("let bit_mode = Int32(UInt32(bitPattern: parsed) & 0xFF)"),
        )
        assertTrue(
            "Swift sparse read must use readInt count",
            swiftCode.contains("let sparseCount = Int(try buffer.readInt())"),
        )
        assertTrue(
            "Swift sparse write must use writeInt count",
            swiftCode.contains("buffer.writeInt(sparseCount)"),
        )
        assertTrue(
            "Swift sparse write must use writeInt array len",
            swiftCode.contains("buffer.writeInt(Int32(val.count))"),
        )
        assertTrue(
            "Swift sparse write must use writeByte array len",
            swiftCode.contains("buffer.writeByte(UInt8(truncatingIfNeeded: val.count))"),
        )

        // Verify PythonGenerator
        val pyCode = PythonGenerator(synSchema).generate()
        assertTrue(
            "Python sparse read must use read_int count",
            pyCode.contains("_sparse_count = buffer.read_int()"),
        )
        assertTrue(
            "Python sparse write must use write_int count",
            pyCode.contains("buffer.write_int(_sparse_count)"),
        )
        assertTrue(
            "Python sparse write must use write_int array len",
            pyCode.contains("buffer.write_int(len(self.sparse_ints))"),
        )
        assertTrue(
            "Python sparse write must use write_byte array len",
            pyCode.contains("buffer.write_byte(len(self.sparse_small))"),
        )

        // Native compile & execute synthetic C++ (verifies std::optional packed strictEnum & sparse
        // round-trip)
        val compilerCandidates =
            listOfNotNull(
                findExecutableOnPath("clang++"),
                findExecutableOnPath("g++"),
            )
        val cppCompiler = compilerCandidates.firstOrNull {
            it.exists() && it.canExecute() && canCompileCpp(it)
        }
        if (cppCompiler != null) {
            val tmpDir = Files.createTempDirectory("remote_compose_review_cpp").toFile()
            try {
                File(tmpDir, "WireBuffer.h").writeText(cppGen.generateWireBufferHeader())
                File(tmpDir, "WireBuffer.cpp").writeText(cppGen.generateWireBufferSource())
                File(tmpDir, "RemoteComposeOps.h").writeText(cppGen.generateHeader())
                File(tmpDir, "RemoteComposeOps.cpp").writeText(cppGen.generateSource())
                val driver =
                    """
                    #include <cassert>
                    #include <stdexcept>
                    #include "RemoteComposeOps.h"
                    using namespace androidx::compose::remote;
                    int main() {
                        SyntheticTestOp op;
                        op.flags = 1;
                        op.optPacked = 2; // valid mode B=2
                        op.versionedPacked = 3; // valid vMode Y=3
                        op.sparseInts = std::vector<int32_t>{10, 20, 30};
                        op.sparseSmall = std::vector<int32_t>{7, 8};

                        WireBuffer wb;
                        op.write(wb);
                        WireBuffer rb(wb.getBytes());
                        assert(rb.readByte() == static_cast<uint8_t>(OpCode::SyntheticTestOp));
                        SyntheticTestOp decoded;
                        decoded.read(rb);
                        assert(!rb.available());
                        assert(decoded.flags == 1);
                        assert(decoded.optPacked.has_value() && *decoded.optPacked == 2);
                        assert(decoded.sparseInts.has_value() && decoded.sparseInts->size() == 3);
                        assert(decoded.sparseSmall.has_value() && decoded.sparseSmall->size() == 2);

                        // Invalid packed bit enum inside optional field must throw
                        SyntheticTestOp bad;
                        bad.flags = 1;
                        bad.optPacked = 99;
                        bad.versionedPacked = 1;
                        WireBuffer badWb;
                        bad.write(badWb);
                        WireBuffer badRb(badWb.getBytes());
                        badRb.readByte();
                        SyntheticTestOp badOut;
                        bool threw = false;
                        try {
                            badOut.read(badRb);
                        } catch (const std::runtime_error&) {
                            threw = true;
                        }
                        assert(threw);
                        return 0;
                    }
                    """
                        .trimIndent()
                val driverFile = File(tmpDir, "main.cpp")
                driverFile.writeText(driver)
                val binFile = File(tmpDir, "test_bin")
                val comp =
                    ProcessBuilder(
                            cppCompiler.absolutePath,
                            "-std=c++17",
                            "-Wall",
                            "-Werror",
                            File(tmpDir, "WireBuffer.cpp").absolutePath,
                            File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                            driverFile.absolutePath,
                            "-o",
                            binFile.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val compOut = comp.inputStream.bufferedReader().readText()
                assertEquals("Synthetic C++ compile failed:\n$compOut", 0, comp.waitFor())
                val run = ProcessBuilder(binFile.absolutePath).redirectErrorStream(true).start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("Synthetic C++ run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        // Native compile & execute synthetic Rust (verifies Result<(),
        // WireError::MissingRequiredField>,
        // optional packed strictEnum, and custom sparse framing round-trip)
        val rustcCandidates = listOf(File(System.getProperty("user.home"), ".cargo/bin/rustc"))
        val rustc =
            (rustcCandidates + listOfNotNull(findExecutableOnPath("rustc"))).firstOrNull {
                it.exists() && it.canExecute()
            }
        if (rustc != null) {
            val tmpDir = Files.createTempDirectory("remote_compose_review_rust").toFile()
            try {
                val mainRs = File(tmpDir, "main.rs")
                mainRs.writeText(
                    """
                    $rustCode

                    fn main() {
                        // Missing required optional field returns Err without panicking
                        let missing = SyntheticTestOp {
                            flags: 1,
                            opt_packed: None,
                            versioned_packed: 1,
                            sparse_ints: None,
                            sparse_small: None,
                            sparse_signed_short: None,
                        };
                        let mut doc = RemoteDocument::new();
                        doc.operations.push(Operation::SyntheticTestOp(Box::new(missing)));
                        let mut err_buf = WireBuffer::new();
                        assert_eq!(
                            doc.save(&mut err_buf),
                            Err(WireError::MissingRequiredField("optPacked"))
                        );

                        // Valid round-trip with custom sparse count/key/length framing
                        let valid = SyntheticTestOp {
                            flags: 1,
                            opt_packed: Some(2),
                            versioned_packed: 3,
                            sparse_ints: Some(vec![10, 20, 30]),
                            sparse_small: Some(vec![7, 8]),
                            sparse_signed_short: Some(vec![9]),
                        };
                        let mut ok_buf = WireBuffer::new();
                        valid.write(&mut ok_buf).expect("write should succeed");
                        let mut rb = WireBuffer::from_bytes(ok_buf.as_bytes());
                        assert_eq!(rb.read_u8().unwrap(), SyntheticTestOp::OP_CODE);
                        let decoded = SyntheticTestOp::read(&mut rb).expect("read should succeed");
                        assert!(!rb.available());
                        assert_eq!(decoded, valid);

                        // Invalid packed bit strictEnum inside optional presentIf returns InvalidData
                        let bad = SyntheticTestOp {
                            flags: 1,
                            opt_packed: Some(99),
                            versioned_packed: 1,
                            sparse_ints: None,
                            sparse_small: None,
                            sparse_signed_short: None,
                        };
                        let mut bad_buf = WireBuffer::new();
                        bad.write(&mut bad_buf).expect("write should succeed");
                        let mut bad_rb = WireBuffer::from_bytes(bad_buf.as_bytes());
                        let _ = bad_rb.read_u8().unwrap();
                        assert_eq!(SyntheticTestOp::read(&mut bad_rb), Err(WireError::InvalidData));
                    }
                    """
                        .trimIndent()
                )
                val binFile = File(tmpDir, "rust_review_bin")
                val comp =
                    ProcessBuilder(
                            rustc.absolutePath,
                            mainRs.absolutePath,
                            "-o",
                            binFile.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val compOut = comp.inputStream.bufferedReader().readText()
                assertEquals("Synthetic Rust compile failed:\n$compOut", 0, comp.waitFor())
                val run = ProcessBuilder(binFile.absolutePath).redirectErrorStream(true).start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("Synthetic Rust run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        // Native execute synthetic Python (verifies sparse framing round-trip and optional packed
        // strictEnum)
        val python3 = findExecutableOnPath("python3")
        if (python3 != null) {
            val tmpDir = Files.createTempDirectory("remote_compose_review_py").toFile()
            try {
                File(tmpDir, "remote_compose.py").writeText(pyCode)
                val pyTest = File(tmpDir, "test_review.py")
                pyTest.writeText(
                    """
                    from remote_compose import SyntheticTestOp, WireBuffer

                    op = SyntheticTestOp(
                        flags=1,
                        opt_packed=2,
                        versioned_packed=3,
                        sparse_ints=[10, 20, 30],
                        sparse_small=[7, 8],
                    )
                    wb = WireBuffer()
                    op.write(wb)
                    rb = WireBuffer.from_bytes(wb.get_bytes())
                    assert rb.read_byte() == SyntheticTestOp.op_code.value
                    decoded = SyntheticTestOp()
                    decoded.read_fields(rb)
                    assert not rb.available()
                    assert decoded == op

                    bad = SyntheticTestOp(flags=1, opt_packed=99, versioned_packed=1)
                    bwb = WireBuffer()
                    bad.write(bwb)
                    brb = WireBuffer.from_bytes(bwb.get_bytes())
                    brb.read_byte()
                    try:
                        SyntheticTestOp().read_fields(brb)
                        raise AssertionError("Expected ValueError for invalid packed enum")
                    except ValueError:
                        pass
                    """
                        .trimIndent()
                )
                val run =
                    ProcessBuilder(python3.absolutePath, pyTest.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("Synthetic Python run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }
    }

    @Test
    fun testSubsetSchemaGoldenOutput() {
        val schemaStream =
            requireNotNull(javaClass.getResourceAsStream("/golden/subset_schema.json")) {
                "Missing test resource /golden/subset_schema.json"
            }
        val schemaJsonText = schemaStream.bufferedReader().use { it.readText() }
        assertEquals(emptyList<String>(), JsonSchemaParser.validate(JSONObject(schemaJsonText)))

        val subsetSchema = JsonSchemaParser.parse(schemaJsonText.byteInputStream())
        assertEquals(11, subsetSchema.operations.size)

        val kotlinGen = KotlinGenerator(subsetSchema)
        val kotlinSinkGen = KotlinProtocolAndSinkGenerator(subsetSchema)
        val javaGen = JavaGenerator(subsetSchema)
        val cppGen = CppGenerator(subsetSchema)

        val sections =
            listOf(
                "KOTLIN AST (RemoteOperations.kt)" to kotlinGen.generateAstNodes(),
                "KOTLIN WRITER (RemoteDocumentWriter.kt)" to kotlinGen.generateWriterNodes(),
                "KOTLIN SINK (RemoteOperationSink.kt)" to kotlinSinkGen.generateOperationSink(),
                "JAVA OPERATIONS TABLE (GeneratedOperations.java)" to
                    javaGen.generateOperationsTable(),
                "CPP HEADER (RemoteComposeOps.h)" to cppGen.generateHeader(),
                "CPP SOURCE (RemoteComposeOps.cpp)" to cppGen.generateSource(),
                "RUST (remote_compose.rs)" to RustGenerator(subsetSchema).generate(),
                "TYPESCRIPT (remote_compose.ts)" to TypeScriptGenerator(subsetSchema).generate(),
                "SWIFT (RemoteCompose.swift)" to SwiftGenerator(subsetSchema).generate(),
                "PYTHON (remote_compose.py)" to PythonGenerator(subsetSchema).generate(),
            )
        val actual =
            sections.joinToString("\n") { (title, content) ->
                "=== $title ===\n${content.replace("\r\n", "\n").trimEnd()}\n"
            }

        val updateGolden =
            System.getProperty("updateGolden") == "true" || System.getenv("UPDATE_GOLDEN") == "true"
        if (updateGolden) {
            val localFile = File("src/test/resources/golden/subset_expected_output.txt")
            val targetFile =
                if (localFile.parentFile?.exists() == true) {
                    localFile
                } else {
                    File(
                        "compose/remote/remote-generator/src/test/resources/golden/subset_expected_output.txt"
                    )
                }
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(actual)
            return
        }

        val expectedStream =
            requireNotNull(javaClass.getResourceAsStream("/golden/subset_expected_output.txt")) {
                "Missing golden file /golden/subset_expected_output.txt. " +
                    "Run with UPDATE_GOLDEN=true to generate it."
            }
        val expected = expectedStream.bufferedReader().use { it.readText() }.replace("\r\n", "\n")
        assertEquals(
            "Generated output differs from golden snapshot. " +
                "Re-run with UPDATE_GOLDEN=true if this generator change is intentional.",
            expected,
            actual,
        )
    }

    @Test
    fun testHeaderPropertyNullableWriteGuardAndAstHeaderBounds() {
        val schema = loadSchema()
        assertTrue(
            KotlinGenerator(schema)
                .generateWriterNodes()
                .contains("requireNotNull(intValue) { \"Missing required field intValue\" }")
        )
        val cppSrc = CppGenerator(schema).generateSource()
        assertTrue(cppSrc.contains("throw std::runtime_error(\"Missing required field intValue\")"))
        assertTrue(
            "C++ modern Header must default omitted width/height to 256",
            cppSrc.contains("doc->width = 256;\n        doc->height = 256;"),
        )
        val rustSrc = RustGenerator(schema).generate()
        assertTrue(rustSrc.contains(".ok_or(WireError::MissingRequiredField(\"intValue\"))?"))
        assertTrue(
            "Rust modern Header must default omitted width/height to 256",
            rustSrc.contains("doc.width = 256;\n            doc.height = 256;"),
        )
        assertTrue(
            "Rust modern Header string skip should use skip_utf8_limited(4000) without allocating",
            rustSrc.contains("3 => { buffer.skip_utf8_limited(4000)?; }"),
        )
        val tsSrc = TypeScriptGenerator(schema).generate()
        assertTrue(tsSrc.contains("throw new Error(\"Missing required field intValue\")"))
        assertTrue(
            "TS modern Header must default omitted width/height to 256",
            tsSrc.contains("doc.width = 256;") && tsSrc.contains("doc.height = 256;"),
        )
        val swiftSrc = SwiftGenerator(schema).generate()
        assertTrue(
            swiftSrc.contains(
                "throw NSError(domain: \"HeaderProperty\", code: 3, userInfo: [NSLocalizedDescriptionKey: \"Missing required field intValue\"])"
            )
        )
        assertTrue(
            "Swift modern Header must default omitted width/height to 256",
            swiftSrc.contains("doc.width = 256") && swiftSrc.contains("doc.height = 256"),
        )
        val pySrc = PythonGenerator(schema).generate()
        assertTrue(pySrc.contains("raise ValueError(\"Missing required field intValue\")"))
        assertTrue(
            "Python modern Header must default omitted width/height to 256",
            pySrc.contains("doc.width = 256") && pySrc.contains("doc.height = 256"),
        )

        // DocumentHeader rejects property keys outside 0..63 and defaults missing dimensions to 256
        val badHeader = DocumentHeader(1, 2, 0, 256, 256, properties = mapOf(64.toShort() to 1))
        try {
            badHeader.write(WireBuffer())
            fail("Expected IllegalArgumentException for out-of-range header property key 64")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("0..63") == true)
        }

        val validHeader = DocumentHeader(1, 2, 0, 256, 256, properties = mapOf(16.toShort() to 1))
        val wb = WireBuffer()
        validHeader.write(wb)
        wb.setIndex(0)
        val roundTripped = DocumentHeader.read(wb)
        assertEquals(256, roundTripped.width)
        assertEquals(256, roundTripped.height)
        assertEquals(1, roundTripped.properties[16.toShort()])

        // Modern 256x256 header with omitted width/height and a string property decodes as 256x256
        // in Python, C++, and Rust, and Rust skip_utf8_limited rejects > 4000-byte header strings.
        val modern256Wb = WireBuffer()
        DocumentHeader(1, 2, 0, 256, 256, properties = mapOf(12.toShort() to "hello"))
            .write(modern256Wb)
        val modern256Bytes = modern256Wb.cloneBytes()
        val modern256Hex =
            modern256Bytes.joinToString(", ") { "0x%02X".format(it.toInt() and 0xFF) }

        val oversizedStrWb = WireBuffer()
        oversizedStrWb.writeByte(0) // Header
        oversizedStrWb.writeInt(0x048C0001)
        oversizedStrWb.writeInt(2)
        oversizedStrWb.writeInt(0)
        oversizedStrWb.writeInt(1) // 1 property
        oversizedStrWb.writeShort((3 shl 10) or 12) // STRING, key 12
        oversizedStrWb.writeShort(4005)
        oversizedStrWb.writeUTF8("a".repeat(4001))
        val oversizedStrHex =
            oversizedStrWb.cloneBytes().joinToString(", ") { "0x%02X".format(it.toInt() and 0xFF) }

        val python3 = findExecutableOnPath("python3")
        if (python3 != null) {
            val tmpDir = Files.createTempDirectory("rc_header_256_py").toFile()
            try {
                File(tmpDir, "remote_compose.py").writeText(pySrc)
                val pyTest = File(tmpDir, "test_hdr.py")
                pyTest.writeText(
                    """
                    from remote_compose import RemoteDocument
                    doc = RemoteDocument.load(bytes([$modern256Hex]))
                    assert doc.width == 256 and doc.height == 256, (doc.width, doc.height)
                    try:
                        RemoteDocument.load(bytes([$oversizedStrHex]))
                        raise AssertionError("Expected ValueError for >4000-byte header string")
                    except ValueError:
                        pass
                    """
                        .trimIndent()
                )
                val run =
                    ProcessBuilder(python3.absolutePath, pyTest.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                val out = run.inputStream.bufferedReader().readText()
                assertEquals("Python modern 256x256 header test failed:\n$out", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        val cppCompiler =
            listOfNotNull(findExecutableOnPath("clang++"), findExecutableOnPath("g++"))
                .firstOrNull {
                    it.exists() && it.canExecute() && canCompileCpp(it)
                }
        if (cppCompiler != null) {
            val tmpDir = Files.createTempDirectory("rc_header_256_cpp").toFile()
            try {
                val cppGen = CppGenerator(schema)
                File(tmpDir, "WireBuffer.h").writeText(cppGen.generateWireBufferHeader())
                File(tmpDir, "WireBuffer.cpp").writeText(cppGen.generateWireBufferSource())
                File(tmpDir, "RemoteComposeOps.h").writeText(cppGen.generateHeader())
                File(tmpDir, "RemoteComposeOps.cpp").writeText(cppSrc)
                val driver = File(tmpDir, "main.cpp")
                driver.writeText(
                    """
                    #include <cassert>
                    #include <stdexcept>
                    #include <vector>
                    #include "RemoteComposeOps.h"
                    using namespace androidx::compose::remote;
                    int main() {
                        std::vector<uint8_t> ok = {$modern256Hex};
                        WireBuffer wb(ok);
                        auto doc = RemoteDocument::load(wb);
                        assert(doc->width == 256);
                        assert(doc->height == 256);

                        std::vector<uint8_t> bad = {$oversizedStrHex};
                        WireBuffer badWb(bad);
                        bool threw = false;
                        try {
                            RemoteDocument::load(badWb);
                        } catch (const std::runtime_error&) {
                            threw = true;
                        }
                        assert(threw);
                        return 0;
                    }
                    """
                        .trimIndent()
                )
                val bin = File(tmpDir, "test_hdr")
                val comp =
                    ProcessBuilder(
                            cppCompiler.absolutePath,
                            "-std=c++17",
                            "-Wall",
                            "-Werror",
                            File(tmpDir, "WireBuffer.cpp").absolutePath,
                            File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                            driver.absolutePath,
                            "-o",
                            bin.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val compOut = comp.inputStream.bufferedReader().readText()
                assertEquals("C++ modern 256x256 compile failed:\n$compOut", 0, comp.waitFor())
                val run = ProcessBuilder(bin.absolutePath).redirectErrorStream(true).start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("C++ modern 256x256 run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        val rustc =
            (listOf(File(System.getProperty("user.home"), ".cargo/bin/rustc")) +
                    listOfNotNull(findExecutableOnPath("rustc")))
                .firstOrNull { it.exists() && it.canExecute() }
        if (rustc != null) {
            val tmpDir = Files.createTempDirectory("rc_header_256_rust").toFile()
            try {
                val mainRs = File(tmpDir, "main.rs")
                mainRs.writeText(
                    """
                    $rustSrc
                    fn main() {
                        let ok: [u8; ${modern256Bytes.size}] = [$modern256Hex];
                        let mut wb = WireBuffer::from_bytes(&ok);
                        let doc = RemoteDocument::load(&mut wb).expect("256x256 header");
                        assert_eq!(doc.width, 256);
                        assert_eq!(doc.height, 256);

                        let bad = [$oversizedStrHex];
                        let mut bad_wb = WireBuffer::from_bytes(&bad);
                        assert_eq!(RemoteDocument::load(&mut bad_wb), Err(WireError::InvalidData));
                    }
                    """
                        .trimIndent()
                )
                val bin = File(tmpDir, "test_hdr_rust")
                val comp =
                    ProcessBuilder(rustc.absolutePath, mainRs.absolutePath, "-o", bin.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                val compOut = comp.inputStream.bufferedReader().readText()
                assertEquals("Rust modern 256x256 compile failed:\n$compOut", 0, comp.waitFor())
                val run = ProcessBuilder(bin.absolutePath).redirectErrorStream(true).start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("Rust modern 256x256 run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }
    }
}
