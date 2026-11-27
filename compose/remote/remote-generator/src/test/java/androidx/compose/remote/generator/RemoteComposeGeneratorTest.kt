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

import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.DataListFloat
import androidx.compose.remote.core.operations.DataListIds
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.PathAppend
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.Skip
import androidx.compose.remote.core.operations.SoundData
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintPathEffects
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.core.operations.utilities.easing.Easing
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
import org.junit.Assert.assertNull
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
                PathAppend.RESET to "Reset",
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

        assertTrue(
            code.contains(
                "public enum OpCode: UInt8, CaseIterable, Equatable, Hashable, Sendable {"
            )
        )
        assertTrue(code.contains("case drawCircle = 46"))
        assertTrue(code.contains("case boxLayout = 202"))
        assertTrue(code.contains("public struct DrawCircle: Equatable, Hashable, Sendable {"))
        assertTrue(code.contains("public enum WireOperation: Equatable, Hashable, Sendable {"))
        assertTrue(code.contains("public struct WireBuffer: Equatable, Hashable, Sendable {"))
        assertTrue(code.contains("public struct WireDocument: Equatable, Hashable, Sendable {"))
        assertTrue(code.contains("buffer.writeFloat(radius)"))
        assertTrue(code.contains("try buffer.readFloat()"))
        assertTrue(code.contains("guard numProps >= 0 && numProps <= maxHeaderProperties else {"))
        assertTrue(code.contains("let key = Int32(rawTag) & 0x3F"))
        assertTrue(code.contains("guard _argsCount >= 0 else {"))
        assertTrue(code.contains("public mutating func readShort() throws(WireError) -> Int16"))
        assertTrue(code.contains("public mutating func writeShort(_ value: Int16)"))
        assertTrue(code.contains("throw WireError.unknownOpCode(opByte)"))
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
        assertTrue(protocolCode.contains("public object PaintConstants"))
        assertTrue(protocolCode.contains("public object RemoteContextIds"))
        assertTrue(protocolCode.contains("public fun isTime("))
        assertTrue(protocolCode.contains("public class FloatAnimationSpec"))
        assertTrue(protocolCode.contains("public object BitmapFormat"))
        assertTrue(protocolCode.contains("imageType and TYPE_AND_ENCODING_MASK"))
        assertTrue(protocolCode.contains("encoding and TYPE_AND_ENCODING_MASK"))
        assertTrue(protocolCode.contains("FEATURE_UNIFIED_POINTER_INPUT: Short = 33"))
        assertTrue(protocolCode.contains("REQUIRED_HORIZONTAL: Byte = 2"))
        assertTrue(protocolCode.contains("REQUIRED_VERTICAL: Byte = 3"))

        val astCode = generator.generateAstNodes()
        assertTrue(astCode.contains("public sealed class RemotePathEffect"))
        assertTrue(astCode.contains("public sealed class RemotePathCommand"))
        assertTrue(astCode.contains("public val uncompressedByteSize: Long"))
        assertTrue(astCode.contains("public val alphaPayload: Int"))

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
        assertTrue(tsCode.contains("((this.widthAndType >>> 16) & 0x7FFF)"))

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
        assertFalse("Swift must not import Foundation", swiftCode.contains("import Foundation"))
        assertFalse("Swift must not throw NSError", swiftCode.contains("NSError"))
        assertFalse(
            "Swift must not emit unqualified min() or max() that fields can shadow",
            Regex("""(?<!Swift\.)\b(?:min|max)\(""").containsMatchIn(swiftCode),
        )
        assertTrue(swiftCode.contains("public private(set) var data: [UInt8]"))
        assertTrue(swiftCode.contains("public private(set) var index: Int = 0"))
        assertTrue(swiftCode.contains("maxHeaderProperties"))
        assertTrue(swiftCode.contains("maxOperationCount"))
        assertTrue(swiftCode.contains("throw WireError.unexpectedHeaderInBody"))
        assertTrue(swiftCode.contains("throw WireError.compressedDocumentUnsupported"))
        assertTrue(swiftCode.contains("throw WireError.invalidHeaderMagic(rawMajor)"))
        assertTrue(swiftCode.contains("guard _glyphsCount <= buffer.remaining / 20 else"))
        assertTrue(swiftCode.contains("guard sparseCount <= buffer.remaining / 2 else"))
        assertTrue(
            swiftCode.contains(
                "throw WireError.invalidFieldValue(owner: \"ColorExpression\", field: \"mode\""
            )
        )
        assertTrue(
            swiftCode.contains(
                "throw WireError.invalidFieldValue(owner: \"GraphicsLayerAttribute\", field: \"attributeType\""
            )
        )
        assertTrue(swiftCode.contains("buffer.readUTF8(maxLength: 4000)"))
        assertTrue(swiftCode.contains("buffer.readBuffer(maxLength: 800000)"))
        assertTrue(swiftCode.contains("(raw_versionAndNumGlyphs >> 16)"))
        assertTrue(swiftCode.contains("Int32(UInt32(bitPattern: widthAndType) & 0xFFFF)"))
        assertTrue(swiftCode.contains("Int32((UInt32(bitPattern: widthAndType) >> 16) & 0x7FFF)"))

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
        assertTrue(ktCode.contains("get() = (widthAndType ushr 16) and 0x7FFF"))
    }

    @Test
    fun testSchemaValidationAndSecurityMetadata() {
        val schema = loadSchema()

        // BitmapData 16-bit dimensions, 15-bit upper type/encoding, and bit-31 zero check
        val bitmapData = schema.operationsByName["BitmapData"]!!
        val widthAndType = bitmapData.fields.first { it.name == "widthAndType" }
        assertEquals("0..15", widthAndType.bits!!.first { it.name == "width" }.range)
        assertEquals("16..30", widthAndType.bits!!.first { it.name == "imageType" }.range)
        assertEquals("31..31", widthAndType.mustBeZeroBits)
        val heightAndEncoding = bitmapData.fields.first { it.name == "heightAndEncoding" }
        assertEquals("0..15", heightAndEncoding.bits!!.first { it.name == "height" }.range)
        assertEquals(
            "16..30",
            heightAndEncoding.bits!!.first { it.name == "bitmapEncoding" }.range,
        )
        assertEquals("31..31", heightAndEncoding.mustBeZeroBits)

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

        // Nested operation payload metadata & conditionalSkip
        val skip = schema.operationsByName["Skip"]!!
        assertNotNull(skip.conditionalSkip)
        assertEquals("0..15", skip.conditionalSkip!!.conditionBits)
        assertTrue(skip.conditionalSkip!!.conditionSigned)
        assertFalse(skip.conditionalSkip!!.unknownConditionSkips)
        assertEquals(6, skip.conditionalSkip!!.conditions.size)
        val skipCondType = skip.fields.first { it.name == "conditionType" }
        assertEquals(1, skipCondType.enumValues!!["SKIP_IF_API_LESS_THAN"])
        assertEquals(6, skipCondType.enumValues!!["SKIP_IF_PROFILE_EXCLUDES"])
        assertEquals(0, skip.fields.first { it.name == "skipLength" }.minValue)
        val skipBytes = skip.fields.first { it.name == "skippedBytes" }
        assertEquals("operations", skipBytes.payloadKind)
        assertEquals("skipLength", skipBytes.countFrom)
        assertEquals("skipTaken", skipBytes.presentIf)
        val patternDefine = schema.operationsByName["PatternDefine"]!!
        assertEquals(
            "operations",
            patternDefine.fields.first { it.name == "bodyBytes" }.payloadKind,
        )

        // Explicit lengthType, maxLength, and javaLimit
        val imageArgs =
            schema.operationsByName["ImageAttribute"]!!.fields.first { it.name == "args" }
        assertEquals("short", imageArgs.lengthType)
        assertNull(imageArgs.maxLength)
        assertNull(imageArgs.javaLimit)

        val timeArgs = schema.operationsByName["TimeAttribute"]!!.fields.first { it.name == "args" }
        assertEquals("short", timeArgs.lengthType)
        assertEquals(32, timeArgs.maxLength)
        assertEquals("TimeAttribute.MAX_ARG_LEN", timeArgs.javaLimit)

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
                        },
                        {
                          "name": "badRange",
                          "type": "int[]",
                          "minLength": 5,
                          "maxLength": 2,
                          "description": "minLength exceeds maxLength"
                        },
                        {
                          "name": "badZeroBits",
                          "type": "int",
                          "mustBeZeroBits": "31..31",
                          "bits": [
                            { "name": "hi", "range": "16..31", "description": "overlaps bit 31" }
                          ],
                          "description": "mustBeZeroBits overlaps bitfield"
                        }
                      ],
                      "conditionalSkip": {
                        "conditionField": "missingField",
                        "valueField": "missingValue",
                        "lengthField": "missingLen",
                        "payloadField": "missingPayload",
                        "conditions": [
                          { "code": 1, "target": "badTarget", "op": "<" }
                        ]
                      }
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
        assertTrue(
            "Expected minLength > maxLength validation error, got: $errors",
            errors.any { it.contains("minLength > maxLength") },
        )
        assertTrue(
            "Expected overlapping mustBeZeroBits validation error, got: $errors",
            errors.any { it.contains("mustBeZeroBits") && it.contains("overlaps") },
        )
        assertTrue(
            "Expected conditionalSkip validation error, got: $errors",
            errors.any { it.contains("conditionalSkip") },
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
            "Swift WireOperation write must use typed throws(WireError)",
            swiftCode.contains("public func write(to buffer: inout WireBuffer) throws(WireError)"),
        )
        assertTrue(
            "Swift WireDocument.save must return [UInt8] with typed throws and forward try op.write",
            swiftCode.contains("public func save() throws(WireError) -> [UInt8]") &&
                swiftCode.contains("try op.write(to: &buffer)"),
        )
        assertTrue(
            "Swift must throw WireError.missingRequiredField instead of preconditionFailure",
            swiftCode.contains(
                "throw WireError.missingRequiredField(owner: \"SyntheticTestOp\", field: \"optPacked\")"
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
                "throw WireError.missingRequiredField(owner: \"HeaderProperty\", field: \"intValue\")"
            )
        )
        assertTrue(
            "Swift modern Header must default omitted width/height to 256",
            swiftSrc.contains("public static let defaultDimension: Int32 = 256") &&
                swiftSrc.contains("doc.width = defaultDimension") &&
                swiftSrc.contains("doc.height = defaultDimension"),
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

    @Test
    fun testRemainingWireFidelityJavaOracleAndSchemaMetadata() {
        val schema = loadSchema()

        // 1. ImageAttribute.args: Java reader accepts > 32 args (e.g. 40) and rejects < 0
        run {
            val wb = WireBuffer()
            wb.writeInt(10) // id
            wb.writeInt(20) // imageId
            wb.writeShort(0) // type = WIDTH
            wb.writeShort(40) // args.length = 40 (> 32)
            repeat(40) { wb.writeInt(100 + it) }
            wb.setIndex(0)
            val ops = ArrayList<Operation>()
            ImageAttribute.read(wb, ops)
            assertEquals(1, ops.size)
            assertFalse(wb.available())

            val negWb = WireBuffer()
            negWb.writeInt(10)
            negWb.writeInt(20)
            negWb.writeShort(0)
            negWb.writeShort(-1)
            negWb.setIndex(0)
            try {
                ImageAttribute.read(negWb, ArrayList())
                fail("Expected NegativeArraySizeException for negative ImageAttribute.args length")
            } catch (_: NegativeArraySizeException) {}
        }

        // 2. TimeAttribute.args: Java reader accepts 32 and rejects 33 via
        // TimeAttribute.MAX_ARG_LEN
        run {
            val okWb = WireBuffer()
            okWb.writeInt(10)
            okWb.writeInt(20)
            okWb.writeShort(0)
            okWb.writeShort(32)
            repeat(32) { okWb.writeInt(it) }
            okWb.setIndex(0)
            val ops = ArrayList<Operation>()
            TimeAttribute.read(okWb, ops)
            assertEquals(1, ops.size)

            val badWb = WireBuffer()
            badWb.writeInt(10)
            badWb.writeInt(20)
            badWb.writeShort(0)
            badWb.writeShort(33)
            repeat(33) { badWb.writeInt(it) }
            badWb.setIndex(0)
            try {
                TimeAttribute.read(badWb, ArrayList())
                fail("Expected RuntimeException for TimeAttribute.args > 32")
            } catch (_: RuntimeException) {}
        }

        // 3. Skip: Java reader jumps when condition holds, decodes inline when false/unknown,
        //    keeps only signed low 16 bits of conditionType, and rejects negative skipLength
        run {
            val skipStream = WireBuffer()
            // High 16 bits non-zero (0x0007_0001), low 16 bits = SKIP_IF_API_LESS_THAN (1), value =
            // 8
            val offset = Skip.apply(skipStream, 1.toShort(), 8, 0)
            skipStream.overwriteInt(1, 0x00070001)
            DrawCircle.apply(skipStream, 10f, 20f, 30f)
            Skip.applyEndSkip(skipStream, offset)
            DrawRect.apply(skipStream, 1f, 2f, 3f, 4f)
            val rawBytes = skipStream.cloneBytes()

            // Player API 6 < 8 -> skips DrawCircle, decodes only DrawRect
            val takenBuf = WireBuffer()
            takenBuf.write(rawBytes)
            takenBuf.setIndex(0)
            takenBuf.setSystemInfo(SystemInfo(6, 0))
            val map = Operations.getAllKnownOperations(8)!!
            val takenOps = ArrayList<Operation>()
            while (takenBuf.available()) {
                val opId = takenBuf.readByte() and 0xFF
                map.get(opId)!!.read(takenBuf, takenOps)
            }
            assertEquals(1, takenOps.size)
            assertTrue(takenOps[0] is DrawRect)

            // Player API 8 >= 8 -> does NOT skip, decodes DrawCircle then DrawRect inline
            val inlineBuf = WireBuffer()
            inlineBuf.write(rawBytes)
            inlineBuf.setIndex(0)
            inlineBuf.setSystemInfo(SystemInfo(8, 0))
            val inlineOps = ArrayList<Operation>()
            while (inlineBuf.available()) {
                val opId = inlineBuf.readByte() and 0xFF
                map.get(opId)!!.read(inlineBuf, inlineOps)
            }
            assertEquals(2, inlineOps.size)
            assertTrue(inlineOps[0] is DrawCircle)
            assertTrue(inlineOps[1] is DrawRect)

            // Unknown conditionType (99) never skips
            val unkStream = WireBuffer()
            val unkOff = Skip.apply(unkStream, 99.toShort(), 8, 0)
            DrawCircle.apply(unkStream, 5f, 6f, 7f)
            Skip.applyEndSkip(unkStream, unkOff)
            unkStream.setIndex(0)
            unkStream.setSystemInfo(SystemInfo(1, 0))
            val unkOps = ArrayList<Operation>()
            while (unkStream.available()) {
                val opId = unkStream.readByte() and 0xFF
                map.get(opId)!!.read(unkStream, unkOps)
            }
            assertEquals(1, unkOps.size)
            assertTrue(unkOps[0] is DrawCircle)

            // Negative skipLength rejected even when condition is false
            val negSkip = WireBuffer()
            negSkip.writeInt(99) // unknown condition -> false
            negSkip.writeInt(0)
            negSkip.writeInt(-1) // negative skipLength
            negSkip.setIndex(0)
            try {
                Skip.read(negSkip, ArrayList())
                fail("Expected RuntimeException for negative Skip.skipLength")
            } catch (_: RuntimeException) {}
        }

        // 4. BitmapData: bit 31 set on widthAndType or heightAndEncoding is rejected by Java reader
        run {
            val bmpOp = schema.operationsByName["BitmapData"]!!
            for ((fName, bitName) in
                listOf("widthAndType" to "imageType", "heightAndEncoding" to "bitmapEncoding")) {
                val fld = bmpOp.fields.first { it.name == fName }
                assertEquals("31..31", fld.mustBeZeroBits)
                assertEquals("16..30", fld.bits!!.first { it.name == bitName }.range)
            }

            for (badWidth in listOf(true, false)) {
                val wb = WireBuffer()
                wb.writeInt(1) // imageId
                wb.writeInt(if (badWidth) (0x80010010.toInt()) else 16)
                wb.writeInt(if (badWidth) 16 else (0x80010010.toInt()))
                wb.writeBuffer(ByteArray(4))
                wb.setIndex(0)
                try {
                    BitmapData.read(wb, ArrayList())
                    fail("Expected RuntimeException when bit 31 is set (badWidth=$badWidth)")
                } catch (_: RuntimeException) {}
            }
        }

        // 5. Newly audited reader limits match Java readers
        run {
            // ParticlesCompare accepts equation of length 40 (> 32, <= 46) and rejects 47
            val pcOk = WireBuffer()
            pcOk.writeInt(1) // id
            pcOk.writeShort(0) // flags
            pcOk.writeFloat(0f) // min
            pcOk.writeFloat(10f) // max
            pcOk.writeInt(40) // condition len = 40
            repeat(40) { pcOk.writeFloat(1f) }
            pcOk.writeInt(1) // equations1 count
            pcOk.writeInt(40) // equations1[0] len = 40
            repeat(40) { pcOk.writeFloat(2f) }
            pcOk.writeInt(0) // equations2 count
            pcOk.setIndex(0)
            val pcOps = ArrayList<Operation>()
            ParticlesCompare.read(pcOk, pcOps)
            assertEquals(1, pcOps.size)

            val pcBad = WireBuffer()
            pcBad.writeInt(1)
            pcBad.writeShort(0)
            pcBad.writeFloat(0f)
            pcBad.writeFloat(10f)
            pcBad.writeInt(47)
            repeat(47) { pcBad.writeFloat(1f) }
            pcBad.setIndex(0)
            try {
                ParticlesCompare.read(pcBad, ArrayList())
                fail("Expected RuntimeException for ParticlesCompare condition length 47 > 46")
            } catch (_: RuntimeException) {}

            // MatrixVectorMath rejects len 0 and len 5, accepts len 2
            for (badLen in listOf(0, 5)) {
                val mvBad = WireBuffer()
                mvBad.writeShort(0)
                mvBad.writeInt(1)
                mvBad.writeInt(badLen)
                repeat(badLen) { mvBad.writeInt(it) }
                mvBad.writeInt(2)
                repeat(2) { mvBad.writeFloat(1f) }
                mvBad.setIndex(0)
                try {
                    MatrixVectorMath.read(mvBad, ArrayList())
                    fail("Expected IllegalArgumentException for MatrixVectorMath lenOut=$badLen")
                } catch (_: IllegalArgumentException) {}
            }

            // PaintData (> 1024), PathData (> 20000), IntegerExpression (> 320), DataListIds (>
            // 2000),
            // DataListFloat (> 2000), PathAppend (> 2000), FloatFunctionCall (> 80), SoundData (>
            // 262144),
            // MatrixConstant (> 16), MatrixExpression (> 32)
            fun assertRejects(block: (WireBuffer) -> Unit) {
                val b = WireBuffer()
                block(b)
                b.setIndex(0)
                try {
                    fail("Unreachable if block throws")
                } catch (_: AssertionError) {}
            }
            val paintWb =
                WireBuffer().apply {
                    writeInt(1025)
                    setIndex(0)
                }
            try {
                PaintData.read(paintWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val pathWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(20001)
                    setIndex(0)
                }
            try {
                PathData.read(pathWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val intExpWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(0)
                    writeInt(321)
                    setIndex(0)
                }
            try {
                IntegerExpression.read(intExpWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val listIdsWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(2001)
                    setIndex(0)
                }
            try {
                DataListIds.read(listIdsWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val listFltWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(2001)
                    setIndex(0)
                }
            try {
                DataListFloat.read(listFltWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val pathAppWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(2001)
                    setIndex(0)
                }
            try {
                PathAppend.read(pathAppWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val fnCallWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(81)
                    setIndex(0)
                }
            try {
                FloatFunctionCall.read(fnCallWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val sndWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(262145)
                    setIndex(0)
                }
            try {
                SoundData.read(sndWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val matConstWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(0)
                    writeInt(17)
                    setIndex(0)
                }
            try {
                MatrixConstant.read(matConstWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
            val matExpWb =
                WireBuffer().apply {
                    writeInt(1)
                    writeInt(0)
                    writeInt(33)
                    setIndex(0)
                }
            try {
                MatrixExpression.read(matExpWb, ArrayList())
                fail()
            } catch (_: RuntimeException) {}
        }

        // Cross-check generated Kotlin, TypeScript, and Swift code for all fixes
        val ktAst = KotlinGenerator(schema).generateAstNodes()
        val tsCode = TypeScriptGenerator(schema).generate()
        val swiftCode = SwiftGenerator(schema).generate()

        // ImageAttribute has no 32 cap in Kotlin, TS, Swift
        val ktImageSection =
            ktAst
                .substringAfter("public class ImageAttributeNode")
                .substringBefore("public class TimeAttributeNode")
        assertFalse(ktImageSection.contains("count <= 32"))
        val tsImageSection =
            tsCode
                .substringAfter("export class ImageAttribute ")
                .substringBefore("export class TimeAttribute ")
        assertFalse(tsImageSection.contains("> 32"))
        val swiftImageSection =
            swiftCode
                .substringAfter("public struct ImageAttribute:")
                .substringBefore("public struct TimeAttribute:")
        assertFalse(swiftImageSection.contains("<= 32 else"))

        // Skip conditional framing & player load in Kotlin, TS, Swift
        assertTrue(ktAst.contains("internal fun shouldSkip("))
        assertTrue(ktAst.contains("internal fun readForPlayer("))
        assertTrue(ktAst.contains("buffer.readBytes(skipLength)"))
        assertTrue(tsCode.contains("static loadForPlayer("))
        assertTrue(tsCode.contains("static shouldSkip("))
        assertTrue(tsCode.contains("buffer.readBytes(this.skipLength)"))
        assertTrue(swiftCode.contains("public static func loadForPlayer("))
        assertTrue(swiftCode.contains("public static func shouldSkip("))
        assertTrue(swiftCode.contains("buffer.readBytes(count: Int(skipLength))"))

        // BitmapData bit 31 mustBeZeroBits & 16..30 upper bitfield in Kotlin, TS, Swift
        assertTrue(ktAst.contains("0x80000000L"))
        assertTrue(ktAst.contains("0x7FFF"))
        assertTrue(tsCode.contains("0x80000000"))
        assertTrue(tsCode.contains("0x7FFF"))
        assertTrue(swiftCode.contains("0x80000000"))
        assertTrue(swiftCode.contains("0x7FFF"))

        // ParticleCompareEquation (46) & MatrixVectorMath minLength 1..4
        assertTrue(ktAst.contains("public class ParticleCompareEquation"))
        assertTrue(ktAst.contains("require(count <= 46)"))
        assertTrue(ktAst.contains("require(count >= 1)"))
        assertTrue(tsCode.contains("export class ParticleCompareEquation"))
        assertTrue(swiftCode.contains("public struct ParticleCompareEquation:"))
    }

    @Test
    fun testRemainingWireFidelityNativeCppRustPythonExecution() {
        val schema = loadSchema()

        // Build shared wire test streams using remote-core writers so bytes match Java wire layout
        val img40Bytes =
            WireBuffer()
                .apply {
                    DocumentHeader(1, 0, 0, 100, 100).write(this)
                    writeByte(171) // ImageAttribute
                    writeInt(10)
                    writeInt(20)
                    writeShort(0)
                    writeShort(40)
                    repeat(40) { writeInt(1000 + it) }
                }
                .cloneBytes()

        val skipDocBytes =
            WireBuffer()
                .apply {
                    DocumentHeader(1, 1, 0, 100, 100).write(this)
                    val off = Skip.apply(this, Skip.SKIP_IF_API_LESS_THAN, 7, 0)
                    // Set upper 16 bits of conditionType to non-zero (0x0003_0001) to test 16-bit
                    // masking
                    overwriteInt(index - 12, 0x00030001)
                    DrawCircle.apply(this, 10f, 20f, 30f)
                    Skip.applyEndSkip(this, off)
                    DrawRect.apply(this, 1f, 2f, 3f, 4f)
                }
                .cloneBytes()

        val skipUnknownDocBytes =
            WireBuffer()
                .apply {
                    DocumentHeader(1, 1, 0, 100, 100).write(this)
                    val off = Skip.apply(this, 99.toShort(), 7, 0)
                    DrawCircle.apply(this, 5f, 6f, 7f)
                    Skip.applyEndSkip(this, off)
                }
                .cloneBytes()

        val skipNegBytes =
            WireBuffer()
                .apply {
                    DocumentHeader(1, 1, 0, 100, 100).write(this)
                    writeByte(241) // Skip
                    writeInt(99) // unknown condition -> false
                    writeInt(7)
                    writeInt(-1) // negative skipLength
                }
                .cloneBytes()

        val bmpBadWidthBytes =
            WireBuffer()
                .apply {
                    DocumentHeader(1, 0, 0, 100, 100).write(this)
                    writeByte(101) // BitmapData
                    writeInt(1)
                    writeInt(0x80010010.toInt()) // bit 31 set
                    writeInt(16)
                    writeBuffer(ByteArray(4))
                }
                .cloneBytes()

        val bmpValidBytes =
            WireBuffer()
                .apply {
                    DocumentHeader(1, 0, 0, 100, 100).write(this)
                    writeByte(101) // BitmapData
                    writeInt(1)
                    writeInt((2 shl 16) or 32) // imageType=2, width=32
                    writeInt((3 shl 16) or 0) // bitmapEncoding=3, height=0
                    writeBuffer(ByteArray(0))
                }
                .cloneBytes()

        val pc40Bytes =
            WireBuffer()
                .apply {
                    DocumentHeader(1, 1, 0, 100, 100).write(this)
                    writeByte(194) // ParticlesCompare
                    writeInt(1)
                    writeShort(0)
                    writeFloat(0f)
                    writeFloat(10f)
                    writeInt(40)
                    repeat(40) { writeFloat(1f) }
                    writeInt(1)
                    writeInt(40)
                    repeat(40) { writeFloat(2f) }
                    writeInt(0)
                }
                .cloneBytes()

        fun toHexInit(bytes: ByteArray): String =
            bytes.joinToString(", ") { "0x%02X".format(it.toInt() and 0xFF) }

        // 1. C++ native execution
        val compilerCandidates =
            listOfNotNull(
                findExecutableOnPath("clang++"),
                findExecutableOnPath("g++"),
                File(
                    "../../../../../prebuilts/fullsdk-linux/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                ),
            )
        val clangFile = compilerCandidates.firstOrNull {
            it.exists() && it.canExecute() && canCompileCpp(it)
        }
        if (clangFile != null) {
            val tmpDir = Files.createTempDirectory("rc_wire_fidelity_cpp").toFile()
            try {
                val cppGen = CppGenerator(schema)
                File(tmpDir, "WireBuffer.h").writeText(cppGen.generateWireBufferHeader())
                File(tmpDir, "WireBuffer.cpp").writeText(cppGen.generateWireBufferSource())
                File(tmpDir, "RemoteComposeOps.h").writeText(cppGen.generateHeader())
                File(tmpDir, "RemoteComposeOps.cpp").writeText(cppGen.generateSource())
                val mainCpp = File(tmpDir, "main.cpp")
                mainCpp.writeText(
                    """
                    #include "RemoteComposeOps.h"
                    #include <cassert>
                    #include <vector>
                    using namespace androidx::compose::remote;
                    template <typename F> void assertThrows(F&& fn) {
                        bool threw = false;
                        try { fn(); } catch (const std::exception&) { threw = true; }
                        assert(threw);
                    }
                    int main() {
                        // ImageAttribute with 40 args
                        {
                            std::vector<uint8_t> b = {${toHexInit(img40Bytes)}};
                            WireBuffer wb(b);
                            auto doc = RemoteDocument::load(wb);
                            assert(doc->operations.size() == 1);
                            auto* img = dynamic_cast<ImageAttribute*>(doc->operations[0].get());
                            assert(img != nullptr && img->args.size() == 40);
                        }
                        // Skip taken (API 6 < 7) vs inline not-taken (API 8 >= 7) + byte round-trip
                        {
                            std::vector<uint8_t> b = {${toHexInit(skipDocBytes)}};
                            WireBuffer wbTaken(b);
                            auto takenDoc = RemoteDocument::loadForPlayer(wbTaken, 6, 0);
                            assert(takenDoc->operations.size() == 2);
                            auto* sk1 = dynamic_cast<Skip*>(takenDoc->operations[0].get());
                            assert(sk1 != nullptr && sk1->getCondition() == 1);
                            assert(sk1->skippedBytes.has_value() && sk1->skippedBytes->size() == 13);
                            assert(takenDoc->operations[1]->getOpCode() == OpCode::DrawRect);

                            WireBuffer wbInline(b);
                            auto inlineDoc = RemoteDocument::loadForPlayer(wbInline, 8, 0);
                            assert(inlineDoc->operations.size() == 3);
                            auto* sk2 = dynamic_cast<Skip*>(inlineDoc->operations[0].get());
                            assert(sk2 != nullptr && !sk2->skippedBytes.has_value() && sk2->skipLength == 13);
                            assert(inlineDoc->operations[1]->getOpCode() == OpCode::DrawCircle);
                            assert(inlineDoc->operations[2]->getOpCode() == OpCode::DrawRect);

                            // Both readings re-serialize identical operation stream bytes
                            WireBuffer out1, out2;
                            for (const auto& op : takenDoc->operations) op->write(out1);
                            for (const auto& op : inlineDoc->operations) op->write(out2);
                            assert(out1.getBytes() == out2.getBytes());
                        }
                        // Skip unknown conditionType=99 never skips
                        {
                            std::vector<uint8_t> b = {${toHexInit(skipUnknownDocBytes)}};
                            WireBuffer wb(b);
                            auto doc = RemoteDocument::loadForPlayer(wb, 1, 0);
                            assert(doc->operations.size() == 2);
                            assert(doc->operations[1]->getOpCode() == OpCode::DrawCircle);
                        }
                        // Skip negative skipLength rejected in both load and loadForPlayer
                        {
                            std::vector<uint8_t> b = {${toHexInit(skipNegBytes)}};
                            assertThrows([&]() { WireBuffer wb(b); RemoteDocument::load(wb); });
                            assertThrows([&]() { WireBuffer wb(b); RemoteDocument::loadForPlayer(wb, 8, 0); });
                        }
                        // BitmapData bit 31 rejected; valid 16..30 unpacked
                        {
                            std::vector<uint8_t> bad = {${toHexInit(bmpBadWidthBytes)}};
                            assertThrows([&]() { WireBuffer wb(bad); RemoteDocument::load(wb); });
                            std::vector<uint8_t> ok = {${toHexInit(bmpValidBytes)}};
                            WireBuffer wb(ok);
                            auto doc = RemoteDocument::load(wb);
                            auto* bmp = dynamic_cast<BitmapData*>(doc->operations[0].get());
                            assert(bmp != nullptr && bmp->getWidth() == 32 && bmp->getImageType() == 2 && bmp->getBitmapEncoding() == 3);
                        }
                        // ParticlesCompare equation length 40 (> 32, <= 46) accepted
                        {
                            std::vector<uint8_t> b = {${toHexInit(pc40Bytes)}};
                            WireBuffer wb(b);
                            auto doc = RemoteDocument::load(wb);
                            auto* pc = dynamic_cast<ParticlesCompare*>(doc->operations[0].get());
                            assert(pc != nullptr && pc->condition.size() == 40 && pc->equations1[0].equation.size() == 40);
                        }
                        return 0;
                    }
                    """
                        .trimIndent()
                )
                val bin = File(tmpDir, "test_cpp")
                val comp =
                    ProcessBuilder(
                            clangFile.absolutePath,
                            "-std=c++17",
                            "-O1",
                            File(tmpDir, "WireBuffer.cpp").absolutePath,
                            File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                            mainCpp.absolutePath,
                            "-o",
                            bin.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val compOut = comp.inputStream.bufferedReader().readText()
                assertEquals("C++ compile failed:\n$compOut", 0, comp.waitFor())
                val run = ProcessBuilder(bin.absolutePath).redirectErrorStream(true).start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("C++ run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        // 2. Rust native execution
        val rustc =
            (listOf(File("../../../../../prebuilts/rust/linux-x86/1.85.1/bin/rustc")) +
                    listOfNotNull(findExecutableOnPath("rustc")))
                .firstOrNull { it.exists() && it.canExecute() }
        if (rustc != null) {
            val tmpDir = Files.createTempDirectory("rc_wire_fidelity_rust").toFile()
            try {
                val rustCode = RustGenerator(schema).generate()
                val mainRs = File(tmpDir, "main.rs")
                mainRs.writeText(
                    """
                    #![allow(dead_code)]
                    $rustCode

                    fn main() {
                        // ImageAttribute with 40 args
                        {
                            let b: [u8; ${img40Bytes.size}] = [${toHexInit(img40Bytes)}];
                            let mut wb = WireBuffer::from_bytes(&b);
                            let doc = RemoteDocument::load(&mut wb).expect("ImageAttribute 40 args");
                            match &doc.operations[0] {
                                Operation::ImageAttribute(op) => assert_eq!(op.args.len(), 40),
                                other => panic!("Unexpected {:?}", other),
                            }
                        }
                        // Skip taken vs inline not-taken + byte round-trip
                        {
                            let b: [u8; ${skipDocBytes.size}] = [${toHexInit(skipDocBytes)}];
                            let mut wb_taken = WireBuffer::from_bytes(&b);
                            let taken = RemoteDocument::load_for_player(&mut wb_taken, 6, 0).expect("Skip taken");
                            assert_eq!(taken.operations.len(), 2);
                            match &taken.operations[0] {
                                Operation::Skip(sk) => {
                                    assert_eq!(sk.condition(), 1);
                                    assert_eq!(sk.skipped_bytes.as_ref().unwrap().len(), 13);
                                }
                                other => panic!("Unexpected {:?}", other),
                            }
                            assert!(matches!(taken.operations[1], Operation::DrawRect(_)));

                            let mut wb_inline = WireBuffer::from_bytes(&b);
                            let inline_doc = RemoteDocument::load_for_player(&mut wb_inline, 8, 0).expect("Skip inline");
                            assert_eq!(inline_doc.operations.len(), 3);
                            match &inline_doc.operations[0] {
                                Operation::Skip(sk) => {
                                    assert!(sk.skipped_bytes.is_none());
                                    assert_eq!(sk.skip_length, 13);
                                }
                                other => panic!("Unexpected {:?}", other),
                            }
                            assert!(matches!(inline_doc.operations[1], Operation::DrawCircle(_)));
                            assert!(matches!(inline_doc.operations[2], Operation::DrawRect(_)));

                            let mut out1 = WireBuffer::new();
                            let mut out2 = WireBuffer::new();
                            for op in &taken.operations { op.write(&mut out1).expect("write taken"); }
                            for op in &inline_doc.operations { op.write(&mut out2).expect("write inline"); }
                            assert_eq!(out1.as_bytes(), out2.as_bytes());
                        }
                        // Skip unknown conditionType=99 never skips
                        {
                            let b: [u8; ${skipUnknownDocBytes.size}] = [${toHexInit(skipUnknownDocBytes)}];
                            let mut wb = WireBuffer::from_bytes(&b);
                            let doc = RemoteDocument::load_for_player(&mut wb, 1, 0).expect("Skip unknown");
                            assert_eq!(doc.operations.len(), 2);
                            assert!(matches!(doc.operations[1], Operation::DrawCircle(_)));
                        }
                        // Skip negative skipLength rejected
                        {
                            let b: [u8; ${skipNegBytes.size}] = [${toHexInit(skipNegBytes)}];
                            let mut wb1 = WireBuffer::from_bytes(&b);
                            assert_eq!(RemoteDocument::load(&mut wb1), Err(WireError::InvalidData));
                            let mut wb2 = WireBuffer::from_bytes(&b);
                            assert_eq!(RemoteDocument::load_for_player(&mut wb2, 8, 0), Err(WireError::InvalidData));
                        }
                        // BitmapData bit 31 rejected; valid 16..30 unpacked
                        {
                            let bad: [u8; ${bmpBadWidthBytes.size}] = [${toHexInit(bmpBadWidthBytes)}];
                            let mut wb_bad = WireBuffer::from_bytes(&bad);
                            assert_eq!(RemoteDocument::load(&mut wb_bad), Err(WireError::InvalidData));

                            let ok: [u8; ${bmpValidBytes.size}] = [${toHexInit(bmpValidBytes)}];
                            let mut wb_ok = WireBuffer::from_bytes(&ok);
                            let doc = RemoteDocument::load(&mut wb_ok).expect("BitmapData ok");
                            match &doc.operations[0] {
                                Operation::BitmapData(bmp) => {
                                    assert_eq!(bmp.width(), 32);
                                    assert_eq!(bmp.image_type(), 2);
                                    assert_eq!(bmp.bitmap_encoding(), 3);
                                }
                                other => panic!("Unexpected {:?}", other),
                            }
                        }
                        // ParticlesCompare equation length 40 accepted
                        {
                            let b: [u8; ${pc40Bytes.size}] = [${toHexInit(pc40Bytes)}];
                            let mut wb = WireBuffer::from_bytes(&b);
                            let doc = RemoteDocument::load(&mut wb).expect("ParticlesCompare 40");
                            match &doc.operations[0] {
                                Operation::ParticlesCompare(pc) => {
                                    assert_eq!(pc.condition.len(), 40);
                                    assert_eq!(pc.equations1[0].equation.len(), 40);
                                }
                                other => panic!("Unexpected {:?}", other),
                            }
                        }
                    }
                    """
                        .trimIndent()
                )
                val bin = File(tmpDir, "test_rust")
                val comp =
                    ProcessBuilder(
                            rustc.absolutePath,
                            mainRs.absolutePath,
                            "-O",
                            "-o",
                            bin.absolutePath,
                        )
                        .redirectErrorStream(true)
                        .start()
                val compOut = comp.inputStream.bufferedReader().readText()
                assertEquals("Rust compile failed:\n$compOut", 0, comp.waitFor())
                val run = ProcessBuilder(bin.absolutePath).redirectErrorStream(true).start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("Rust run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        // 3. Python native execution
        val python3 = findExecutableOnPath("python3")
        if (python3 != null) {
            val tmpDir = Files.createTempDirectory("rc_wire_fidelity_py").toFile()
            try {
                File(tmpDir, "remote_compose.py").writeText(PythonGenerator(schema).generate())
                val testPy = File(tmpDir, "test_fidelity.py")
                testPy.writeText(
                    """
                    from remote_compose import RemoteDocument, WireBuffer

                    def assert_raises(exc, fn):
                        try:
                            fn()
                            raise AssertionError("Expected exception")
                        except exc:
                            pass

                    # ImageAttribute 40 args
                    doc = RemoteDocument.load(bytes([${toHexInit(img40Bytes)}]))
                    assert len(doc.operations) == 1
                    assert len(doc.operations[0].args) == 40

                    # Skip taken vs inline not-taken + byte round-trip
                    skip_bytes = bytes([${toHexInit(skipDocBytes)}])
                    taken = RemoteDocument.load_for_player(skip_bytes, 6, 0)
                    assert len(taken.operations) == 2
                    assert taken.operations[0].condition == 1
                    assert len(taken.operations[0].skipped_bytes) == 13
                    assert type(taken.operations[1]).__name__ == "DrawRect"

                    inline_doc = RemoteDocument.load_for_player(skip_bytes, 8, 0)
                    assert len(inline_doc.operations) == 3
                    assert inline_doc.operations[0].skipped_bytes is None
                    assert inline_doc.operations[0].skip_length == 13
                    assert type(inline_doc.operations[1]).__name__ == "DrawCircle"
                    assert type(inline_doc.operations[2]).__name__ == "DrawRect"

                    out1, out2 = WireBuffer(), WireBuffer()
                    for op in taken.operations:
                        op.write(out1)
                    for op in inline_doc.operations:
                        op.write(out2)
                    assert out1.get_bytes() == out2.get_bytes()

                    # Skip unknown conditionType=99 never skips
                    unk = RemoteDocument.load_for_player(bytes([${toHexInit(skipUnknownDocBytes)}]), 1, 0)
                    assert len(unk.operations) == 2
                    assert type(unk.operations[1]).__name__ == "DrawCircle"

                    # Skip negative skipLength rejected
                    neg = bytes([${toHexInit(skipNegBytes)}])
                    assert_raises(ValueError, lambda: RemoteDocument.load(neg))
                    assert_raises(ValueError, lambda: RemoteDocument.load_for_player(neg, 8, 0))

                    # BitmapData bit 31 rejected; valid 16..30 unpacked
                    assert_raises(ValueError, lambda: RemoteDocument.load(bytes([${toHexInit(bmpBadWidthBytes)}])))
                    bmp_doc = RemoteDocument.load(bytes([${toHexInit(bmpValidBytes)}]))
                    bmp = bmp_doc.operations[0]
                    assert bmp.width == 32 and bmp.image_type == 2 and bmp.bitmap_encoding == 3

                    # ParticlesCompare equation length 40 accepted
                    pc_doc = RemoteDocument.load(bytes([${toHexInit(pc40Bytes)}]))
                    pc = pc_doc.operations[0]
                    assert len(pc.condition) == 40 and len(pc.equations1[0].equation) == 40
                    """
                        .trimIndent()
                )
                val run =
                    ProcessBuilder(python3.absolutePath, testPy.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                val runOut = run.inputStream.bufferedReader().readText()
                assertEquals("Python run failed:\n$runOut", 0, run.waitFor())
            } finally {
                tmpDir.deleteRecursively()
            }
        }
    }

    @Test
    fun testMultiLanguageSchemaProtocolAndSubWireCodecs() {
        val schema = loadSchema()

        // Verify schema pathEffects & animationSpec constants align with remote-core
        val peSchema = requireNotNull(schema.pathEffects)
        assertEquals(Limits.MAX_DASH_INTERVALS, peSchema.maxDashIntervals)
        val peByConst = peSchema.types.associate { it.constName to it.id }
        assertEquals(PaintPathEffects.DASH, peByConst["TYPE_DASH"])
        assertEquals(PaintPathEffects.DISCRETE_PATH, peByConst["TYPE_DISCRETE"])
        assertEquals(PaintPathEffects.PATH_DASH, peByConst["TYPE_PATH_DASH"])
        assertEquals(PaintPathEffects.SUM, peByConst["TYPE_SUM"])
        assertEquals(PaintPathEffects.COMPOSE, peByConst["TYPE_COMPOSE"])

        val animSchema = requireNotNull(schema.animationSpec)
        val easingByName = animSchema.easings.associate { it.name to it.id }
        assertEquals(Easing.CUBIC_STANDARD, easingByName["CUBIC_STANDARD"])
        assertEquals(Easing.CUBIC_ACCELERATE, easingByName["CUBIC_ACCELERATE"])
        assertEquals(Easing.CUBIC_DECELERATE, easingByName["CUBIC_DECELERATE"])
        assertEquals(Easing.CUBIC_LINEAR, easingByName["CUBIC_LINEAR"])
        assertEquals(Easing.CUBIC_ANTICIPATE, easingByName["CUBIC_ANTICIPATE"])
        assertEquals(Easing.CUBIC_OVERSHOOT, easingByName["CUBIC_OVERSHOOT"])
        assertEquals(Easing.CUBIC_CUSTOM, easingByName["CUBIC_CUSTOM"])
        assertEquals(Easing.SPLINE_CUSTOM, easingByName["SPLINE_CUSTOM"])
        assertEquals(Easing.EASE_OUT_BOUNCE, easingByName["EASE_OUT_BOUNCE"])
        assertEquals(Easing.EASE_OUT_ELASTIC, easingByName["EASE_OUT_ELASTIC"])

        // Verify Swift and TypeScript emit all schema-driven protocol constants and sub-wire codecs
        val swiftCode = SwiftGenerator(schema).generate()
        for (marker in
            listOf(
                "public enum OpCode: UInt8, CaseIterable, Equatable, Hashable, Sendable",
                "public enum WireError: Error, Equatable, Hashable, Sendable",
                "public struct WireBuffer: Equatable, Hashable, Sendable",
                "public enum WireOperation: Equatable, Hashable, Sendable",
                "public struct WireDocument: Equatable, Hashable, Sendable",
                "public enum HeaderPropertyValue: Equatable, Hashable, Sendable",
                "public struct WireHeaderProperty: Equatable, Hashable, Sendable",
                "private func wireFloatEquals(_ lhs: Float, _ rhs: Float) -> Bool",
                "public static func == (lhs: HeaderPropertyValue, rhs: HeaderPropertyValue) -> Bool",
                "public static func == (lhs: FloatExpression, rhs: FloatExpression) -> Bool",
                "let itemLen = Int(UInt16(bitPattern: try buffer.readShort()))",
                "let dataType = Int32(rawTag) >> 10",
                "case unknown(dataType: Int32, payload: [UInt8])",
                "let payload = try buffer.readBytes(count: itemLen)",
                "buffer.writeBytes(payload)",
                "outProps.lastIndex(where:",
                "guard majorVersion >= 0 && majorVersion <= 0xFFFF else",
                "if capabilities >= 0 && capabilities <= Int64(UInt32.max)",
                "public enum IdEncoding",
                "public static let nanExponent: Int32 = 2139095040",
                "public static let noId: Int32 = -1",
                "public enum HeaderConstants",
                "public enum PaintCommandTags",
                "public enum PaintConstants",
                "public enum PathCommandTags",
                "public enum RemoteContextIds",
                "public enum AnimationEasing",
                "public struct FloatAnimationSpec: Equatable, Hashable, Sendable",
                "public static func == (lhs: FloatAnimationSpec, rhs: FloatAnimationSpec) -> Bool",
                "public func hash(into hasher: inout Hasher)",
                "public indirect enum PathEffect: Equatable, Hashable, Sendable",
                "public static func == (lhs: PathEffect, rhs: PathEffect) -> Bool",
                "public enum PathCommand: Equatable, Hashable, Sendable",
                "public static func == (lhs: PathCommand, rhs: PathCommand) -> Bool",
                "public var isSpringAnimation: Bool",
                "public func unpackAnimationSpec() throws(WireError) -> FloatAnimationSpec",
                "public func encode() throws(WireError) -> [Float]",
                "public var uncompressedByteSize: Int64",
                "multipliedReportingOverflow",
                "public func decodePathCommands() -> [PathCommand]",
                "public var alphaPayload: Int32",
                "reserveCapacity(Swift.min(",
                "public static let roundedRectangle: Int32 = 2",
            )) {
            assertTrue("Swift output missing: $marker", swiftCode.contains(marker))
        }
        assertFalse(
            "Swift output must not define RemoteOperation protocol",
            swiftCode.contains("protocol RemoteOperation"),
        )
        assertFalse(
            "Swift output must not define RemoteDocument",
            swiftCode.contains("struct RemoteDocument"),
        )
        assertFalse(
            "Swift output must not import Foundation",
            swiftCode.contains("import Foundation"),
        )
        assertFalse("Swift output must not throw NSError", swiftCode.contains("NSError"))

        // Standalone Rust generateStruct output must not reference module-level protocol helpers
        val standaloneFloatExpr =
            RustGenerator(schema)
                .generateStruct(schema.operationsByName.getValue("FloatExpression"))
        assertFalse(standaloneFloatExpr.contains("unpack_animation_spec"))
        val standalonePathData =
            RustGenerator(schema).generateStruct(schema.operationsByName.getValue("PathData"))
        assertFalse(standalonePathData.contains("decode_path_commands"))

        val tsCode = TypeScriptGenerator(schema).generate()
        for (marker in
            listOf(
                "export const IdEncoding =",
                "export const HeaderConstants =",
                "export const PaintCommandTags =",
                "export const PaintConstants =",
                "export const PathCommandTags =",
                "export const RemoteContextIds =",
                "export const AnimationEasing =",
                "export interface FloatAnimationSpec",
                "export function isSpringAnimationSpec(data: ArrayLike<number>): boolean",
                "export function unpackAnimationSpec(data: ArrayLike<number>): FloatAnimationSpec",
                "export function packAnimationSpec(",
                "export type PathEffect =",
                "export function encodePathEffect(",
                "export function decodePathEffect(data: ArrayLike<number>): PathEffect | null",
                "export type PathCommand =",
                "export function decodePathCommands(pathData: ArrayLike<number>): PathCommand[]",
                "function rawInt32View(data: ArrayLike<number>): Int32Array | null",
                "getUncompressedByteSize(): number",
                "getAlphaPayload(): number",
            )) {
            assertTrue("TypeScript output missing: $marker", tsCode.contains(marker))
        }

        // 1. C++ native compilation & execution of protocol constants + sub-wire codecs
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
        val cppCompiler = compilerCandidates.firstOrNull {
            it.exists() && it.canExecute() && canCompileCpp(it)
        }
        if (cppCompiler != null) {
            val tmpDir = Files.createTempDirectory("rc_multilang_codecs_cpp").toFile()
            try {
                val cppGen = CppGenerator(schema)
                File(tmpDir, "WireBuffer.h").writeText(cppGen.generateWireBufferHeader())
                File(tmpDir, "WireBuffer.cpp").writeText(cppGen.generateWireBufferSource())
                File(tmpDir, "RemoteComposeOps.h").writeText(cppGen.generateHeader())
                File(tmpDir, "RemoteComposeOps.cpp").writeText(cppGen.generateSource())
                File(tmpDir, "main.cpp")
                    .writeText(
                        """
                        #include <cassert>
                        #include <cmath>
                        #include <iostream>
                        #include "RemoteComposeOps.h"
                        using namespace androidx::compose::remote;
                        int main() {
                            // IdEncoding & RemoteContextIds
                            int32_t nanBits = IdEncoding::asNanBits(42);
                            assert(IdEncoding::isNanBits(nanBits));
                            assert(IdEncoding::idFromNanBits(nanBits) == 42);
                            assert(IdEncoding::isNanBits(0x7F800005));
                            assert(IdEncoding::idFromNanBits(0x7F800005) == 5);
                            assert(!IdEncoding::isNanBits(0x7F800000));
                            assert(!IdEncoding::isNanBits(0x7FC00000));
                            assert(IdEncoding::idFromNanBits(0x7FC00000) == IdEncoding::NO_ID);
                            assert(!IdEncoding::isNanBits(static_cast<int32_t>(0xFF800000u)));
                            assert(IdEncoding::idFromNanBits(static_cast<int32_t>(0xFF800000u)) == IdEncoding::NO_ID);
                            assert(!IdEncoding::isNanBits(static_cast<int32_t>(0xFFC00000u)));
                            assert(IdEncoding::idFromNanBits(static_cast<int32_t>(0xFFC00000u)) == IdEncoding::NO_ID);
                            assert(!IdEncoding::isDereferenced(42));
                            assert(IdEncoding::directId(42) == 42);
                            assert(IdEncoding::pointerId(IdEncoding::PTR_DEREFERENCE | 7) == 7);
                            assert(RemoteContextIds::isTime(RemoteContextIds::ID_CONTINUOUS_SEC));
                            assert(RemoteContextIds::isTime(RemoteContextIds::ID_DAY_OF_MONTH));
                            assert(!RemoteContextIds::isTime(RemoteContextIds::ID_DENSITY));

                            // FloatAnimationSpec pack/unpack + FloatExpression extras
                            auto stdPts = AnimationEasing::cubicControlPoints(AnimationEasing::CUBIC_STANDARD);
                            assert(stdPts.has_value() && std::abs((*stdPts)[0] - 0.4f) < 1e-6f);
                            std::vector<float> customPts = {0.1f, 0.2f, 0.3f, 0.4f};
                            auto packed = packAnimationSpec(0.5f, AnimationEasing::CUBIC_CUSTOM, &customPts, 10.0f, 360.0f);
                            FloatExpression fe;
                            fe.animationSpec = packed;
                            assert(!fe.isSpringAnimation());
                            auto unpacked = fe.unpackAnimationSpec();
                            assert(std::abs(unpacked.duration - 0.5f) < 1e-6f);
                            assert(unpacked.type == AnimationEasing::CUBIC_CUSTOM);
                            assert(unpacked.params.size() == 4);
                            assert(std::abs(unpacked.initialValue - 10.0f) < 1e-6f);
                            assert(std::abs(unpacked.wrap - 360.0f) < 1e-6f);
                            fe.animationSpec = {0.0f, 1.0f, 100.0f, 10.0f, 0.01f};
                            assert(fe.isSpringAnimation());

                            // PathEffect encode/decode
                            PathEffect dash{PathEffect::Dash{2.5f, {4.0f, 6.0f}}};
                            PathEffect disc{PathEffect::Discrete{8.0f, 1.5f}};
                            PathEffect sum{PathEffect::Sum{std::make_shared<PathEffect>(dash), std::make_shared<PathEffect>(disc)}};
                            auto encodedPe = sum.encode();
                            auto decodedPe = PathEffect::decode(encodedPe);
                            assert(decodedPe.has_value());
                            auto* sumOut = std::get_if<PathEffect::Sum>(&decodedPe->value);
                            assert(sumOut != nullptr);
                            auto* dashOut = std::get_if<PathEffect::Dash>(&sumOut->first->value);
                            assert(dashOut != nullptr && dashOut->phase == 2.5f && dashOut->intervals.size() == 2);
                            assert(!PathEffect::decode({}).has_value());

                            // PathData decodePathCommands (Move + Line with 2 padding floats + Close + Done)
                            PathData pd;
                            pd.pathData = {
                                bitsToFloat(IdEncoding::asNanBits(PathCommandTags::MOVE)), 1.0f, 2.0f,
                                bitsToFloat(IdEncoding::asNanBits(PathCommandTags::LINE)), 0.0f, 0.0f, 3.0f, 4.0f,
                                bitsToFloat(IdEncoding::asNanBits(PathCommandTags::CLOSE)),
                                bitsToFloat(IdEncoding::asNanBits(PathCommandTags::DONE)),
                            };
                            auto cmds = pd.decodePathCommands();
                            assert(cmds.size() == 4);
                            auto* mv = std::get_if<PathCommand::MoveTo>(&cmds[0].value);
                            assert(mv != nullptr && mv->x == 1.0f && mv->y == 2.0f);
                            auto* ln = std::get_if<PathCommand::LineTo>(&cmds[1].value);
                            assert(ln != nullptr && ln->x == 3.0f && ln->y == 4.0f);
                            assert(std::holds_alternative<PathCommand::Close>(cmds[2].value));
                            assert(std::holds_alternative<PathCommand::Done>(cmds[3].value));

                            // BitmapData uncompressedByteSize + enum constants
                            BitmapData bmp;
                            bmp.widthAndType = (BitmapData::TYPE_PNG_ALPHA_8 << 16) | 10;
                            bmp.heightAndEncoding = (BitmapData::ENCODING_INLINE << 16) | 20;
                            assert(bmp.uncompressedByteSize() == 200);
                            bmp.widthAndType = (BitmapData::TYPE_PNG_8888 << 16) | 10;
                            assert(bmp.uncompressedByteSize() == 800);

                            // ColorExpression alphaPayload + TextFromFloat bitfields
                            ColorExpression ce;
                            ce.flagsAndMode = (0x80 << 16) | ColorExpression::HSV_MODE;
                            assert(ce.alphaPayload() == 0x80);
                            TextFromFloat tff;
                            tff.flags = TextFromFloat::PAD_AFTER_ZERO | TextFromFloat::GROUPING_BY3 | TextFromFloat::FULL_FORMAT;
                            assert(tff.getPadAfter() == 3);
                            assert(tff.getGrouping() == 1);
                            assert(tff.getFullFormat() == 1);

                            std::cout << "C++ codecs OK" << std::endl;
                            return 0;
                        }
                        """
                            .trimIndent()
                    )
                val bin = File(tmpDir, "test_cpp_codecs")
                runCheckedProcess(
                    "C++ codecs compile",
                    listOf(
                        cppCompiler.absolutePath,
                        "-std=c++17",
                        "-O2",
                        "-Wall",
                        File(tmpDir, "WireBuffer.cpp").absolutePath,
                        File(tmpDir, "RemoteComposeOps.cpp").absolutePath,
                        File(tmpDir, "main.cpp").absolutePath,
                        "-o",
                        bin.absolutePath,
                    ),
                    tmpDir,
                )
                runCheckedProcess("C++ codecs run", listOf(bin.absolutePath), tmpDir)
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        // 2. Rust native compilation & execution of protocol constants + sub-wire codecs
        val rustcCandidates =
            listOfNotNull(
                File("../../../../../prebuilts/rust/linux-x86/1.85.1/bin/rustc"),
                File("../../../../prebuilts/rust/linux-x86/1.85.1/bin/rustc"),
                File("../../../prebuilts/rust/linux-x86/1.85.1/bin/rustc"),
                File("../../prebuilts/rust/linux-x86/1.85.1/bin/rustc"),
                File(System.getProperty("user.home"), ".cargo/bin/rustc"),
                findExecutableOnPath("rustc"),
            )
        val rustc = rustcCandidates.firstOrNull { it.exists() && it.canExecute() }
        if (rustc != null) {
            val tmpDir = Files.createTempDirectory("rc_multilang_codecs_rust").toFile()
            try {
                val rustCode = RustGenerator(schema).generate()
                val mainRs = File(tmpDir, "main.rs")
                mainRs.writeText(
                    """
                    $rustCode

                    fn main() {
                        let nan_bits = id_encoding::as_nan_bits(42);
                        assert!(id_encoding::is_nan_bits(nan_bits));
                        assert_eq!(id_encoding::id_from_nan_bits(nan_bits), 42);
                        assert!(id_encoding::is_nan_bits(0x7F800005));
                        assert_eq!(id_encoding::id_from_nan_bits(0x7F800005), 5);
                        assert!(!id_encoding::is_nan_bits(0x7F800000));
                        assert!(!id_encoding::is_nan_bits(0x7FC00000));
                        assert_eq!(id_encoding::id_from_nan_bits(0x7FC00000), id_encoding::NO_ID);
                        assert!(!id_encoding::is_nan_bits(0xFF800000u32 as i32));
                        assert_eq!(id_encoding::id_from_nan_bits(0xFF800000u32 as i32), id_encoding::NO_ID);
                        assert!(!id_encoding::is_nan_bits(0xFFC00000u32 as i32));
                        assert_eq!(id_encoding::id_from_nan_bits(0xFFC00000u32 as i32), id_encoding::NO_ID);
                        assert_eq!(id_encoding::direct_id(42), Some(42));
                        assert_eq!(id_encoding::pointer_id(id_encoding::PTR_DEREFERENCE | 7), Some(7));
                        assert!(remote_context_ids::is_time(remote_context_ids::ID_CONTINUOUS_SEC));
                        assert!(remote_context_ids::is_time(remote_context_ids::ID_DAY_OF_MONTH));
                        assert!(!remote_context_ids::is_time(remote_context_ids::ID_DENSITY));

                        let pts = animation_easing::cubic_control_points(animation_easing::CUBIC_STANDARD).unwrap();
                        assert!((pts[0] - 0.4).abs() < 1e-6);
                        assert_eq!(unpack_animation_spec(&[]).unwrap(), unpack_animation_spec(&[]).unwrap());
                        let packed = pack_animation_spec(0.5, animation_easing::CUBIC_CUSTOM, Some(&[0.1, 0.2, 0.3, 0.4]), 10.0, 360.0);
                        let fe = FloatExpression { id: 1, expression: vec![], animation_spec: packed };
                        assert!(!fe.is_spring_animation());
                        let unpacked = fe.unpack_animation_spec().unwrap();
                        assert!((unpacked.duration - 0.5).abs() < 1e-6);
                        assert_eq!(unpacked.easing_type, animation_easing::CUBIC_CUSTOM);
                        assert_eq!(unpacked.params.len(), 4);
                        assert!((unpacked.initial_value - 10.0).abs() < 1e-6);
                        assert!((unpacked.wrap - 360.0).abs() < 1e-6);

                        let pe = PathEffect::Compose(
                            Box::new(PathEffect::Dash { phase: 2.5, intervals: vec![4.0, 6.0] }),
                            Box::new(PathEffect::Discrete { segment_length: 8.0, deviation: 1.5 }),
                        );
                        let decoded_pe = PathEffect::decode(&pe.encode().unwrap()).unwrap();
                        assert_eq!(decoded_pe, pe);
                        assert!(PathEffect::decode(&[]).is_none());
                        let huge_dash = PathEffect::Dash { phase: 0.0, intervals: vec![1.0; PathEffect::MAX_DASH_INTERVALS + 1] };
                        assert!(huge_dash.encode().is_err());

                        let pd = PathData {
                            id_and_winding: 1,
                            path_data: vec![
                                f32::from_bits(id_encoding::as_nan_bits(path_command_tags::MOVE) as u32), 1.0, 2.0,
                                f32::from_bits(id_encoding::as_nan_bits(path_command_tags::LINE) as u32), 0.0, 0.0, 3.0, 4.0,
                                f32::from_bits(id_encoding::as_nan_bits(path_command_tags::CLOSE) as u32),
                                f32::from_bits(id_encoding::as_nan_bits(path_command_tags::DONE) as u32),
                            ],
                        };
                        let cmds = pd.decode_path_commands();
                        assert_eq!(cmds, vec![
                            PathCommand::MoveTo { x: 1.0, y: 2.0 },
                            PathCommand::LineTo { x: 3.0, y: 4.0 },
                            PathCommand::Close,
                            PathCommand::Done,
                        ]);

                        let bmp = BitmapData {
                            image_id: 1,
                            width_and_type: (BitmapData::TYPE_PNG_ALPHA_8 << 16) | 10,
                            height_and_encoding: (BitmapData::ENCODING_INLINE << 16) | 20,
                            bitmap: vec![],
                        };
                        assert_eq!(bmp.uncompressed_byte_size(), 200);

                        let ce = ColorExpression {
                            id: 1,
                            flags_and_mode: (0x80 << 16) | ColorExpression::HSV_MODE,
                            arg1: 0,
                            arg2: 0,
                            arg3: 0,
                        };
                        assert_eq!(ce.alpha_payload(), 0x80);

                        let tff = TextFromFloat {
                            text_id: 1,
                            value: 0.0,
                            digits_before: 2,
                            digits_after: 2,
                            flags: TextFromFloat::PAD_AFTER_ZERO | TextFromFloat::GROUPING_BY3 | TextFromFloat::FULL_FORMAT,
                        };
                        assert_eq!(tff.pad_after(), 3);
                        assert_eq!(tff.grouping(), 1);
                        assert_eq!(tff.full_format(), 1);
                        println!("Rust codecs OK");
                    }
                    """
                        .trimIndent()
                )
                val bin = File(tmpDir, "test_rust_codecs")
                runCheckedProcess(
                    "Rust codecs compile",
                    listOf(rustc.absolutePath, mainRs.absolutePath, "-O", "-o", bin.absolutePath),
                    tmpDir,
                )
                runCheckedProcess("Rust codecs run", listOf(bin.absolutePath), tmpDir)
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        // 3. Python native execution of protocol constants + sub-wire codecs
        val python3 = findExecutableOnPath("python3")
        if (python3 != null) {
            val tmpDir = Files.createTempDirectory("rc_multilang_codecs_py").toFile()
            try {
                File(tmpDir, "remote_compose.py").writeText(PythonGenerator(schema).generate())
                val testPy = File(tmpDir, "test_codecs.py")
                testPy.writeText(
                    """
                    from remote_compose import (
                        AnimationEasing, BitmapData, ColorExpression, ComposePathEffect,
                        DashPathEffect, DiscretePathEffect, FloatExpression, IdEncoding,
                        LineToPathCommand, MoveToPathCommand, ClosePathCommand, DonePathCommand,
                        PathCommandTags, PathData, PathEffect, RemoteContextIds, TextFromFloat,
                        _bits_to_float, pack_animation_spec, unpack_animation_spec,
                    )

                    nan_bits = IdEncoding.as_nan_bits(42)
                    assert IdEncoding.is_nan_bits(nan_bits)
                    assert IdEncoding.id_from_nan_bits(nan_bits) == 42
                    assert IdEncoding.is_nan_bits(0x7F800005)
                    assert IdEncoding.id_from_nan_bits(0x7F800005) == 5
                    assert not IdEncoding.is_nan_bits(0x7F800000)
                    assert not IdEncoding.is_nan_bits(0x7FC00000)
                    assert IdEncoding.id_from_nan_bits(0x7FC00000) == IdEncoding.NO_ID
                    assert not IdEncoding.is_nan_bits(-0x800000)
                    assert IdEncoding.id_from_nan_bits(-0x800000) == IdEncoding.NO_ID
                    assert not IdEncoding.is_nan_bits(-0x400000)
                    assert IdEncoding.id_from_nan_bits(-0x400000) == IdEncoding.NO_ID
                    assert IdEncoding.direct_id(42) == 42
                    assert IdEncoding.pointer_id(IdEncoding.PTR_DEREFERENCE | 7) == 7
                    assert RemoteContextIds.is_time(RemoteContextIds.ID_CONTINUOUS_SEC)
                    assert RemoteContextIds.is_time(RemoteContextIds.ID_DAY_OF_MONTH)
                    assert not RemoteContextIds.is_time(RemoteContextIds.ID_DENSITY)

                    pts = AnimationEasing.cubic_control_points(AnimationEasing.CUBIC_STANDARD)
                    assert pts == (0.4, 0.0, 0.2, 1.0)
                    assert unpack_animation_spec([]) == unpack_animation_spec([])
                    packed = pack_animation_spec(0.5, AnimationEasing.CUBIC_CUSTOM, [0.1, 0.2, 0.3, 0.4], 10.0, 360.0)
                    fe = FloatExpression(id=1, expression=[], animation_spec=packed)
                    assert not fe.is_spring_animation
                    unpacked = fe.unpack_animation_spec()
                    assert abs(unpacked.duration - 0.5) < 1e-6
                    assert unpacked.easing_type == AnimationEasing.CUBIC_CUSTOM
                    assert len(unpacked.params) == 4
                    assert abs(unpacked.initial_value - 10.0) < 1e-6
                    assert abs(unpacked.wrap - 360.0) < 1e-6

                    pe = ComposePathEffect(
                        outer=DashPathEffect(phase=2.5, intervals=[4.0, 6.0]),
                        inner=DiscretePathEffect(segment_length=8.0, deviation=1.5),
                    )
                    assert PathEffect.decode(pe.encode()) == pe
                    assert PathEffect.decode([]) is None

                    pd = PathData(
                        id_and_winding=1,
                        path_data=[
                            _bits_to_float(IdEncoding.as_nan_bits(PathCommandTags.MOVE)), 1.0, 2.0,
                            _bits_to_float(IdEncoding.as_nan_bits(PathCommandTags.LINE)), 0.0, 0.0, 3.0, 4.0,
                            _bits_to_float(IdEncoding.as_nan_bits(PathCommandTags.CLOSE)),
                            _bits_to_float(IdEncoding.as_nan_bits(PathCommandTags.DONE)),
                        ],
                    )
                    assert pd.decode_path_commands() == [
                        MoveToPathCommand(x=1.0, y=2.0),
                        LineToPathCommand(x=3.0, y=4.0),
                        ClosePathCommand(),
                        DonePathCommand(),
                    ]

                    bmp = BitmapData(
                        image_id=1,
                        width_and_type=(BitmapData.TYPE_PNG_ALPHA_8 << 16) | 10,
                        height_and_encoding=(BitmapData.ENCODING_INLINE << 16) | 20,
                        bitmap=b"",
                    )
                    assert bmp.uncompressed_byte_size == 200

                    ce = ColorExpression(id=1, flags_and_mode=(0x80 << 16) | ColorExpression.HSV_MODE)
                    assert ce.alpha_payload == 0x80

                    tff = TextFromFloat(
                        text_id=1,
                        flags=TextFromFloat.PAD_AFTER_ZERO | TextFromFloat.GROUPING_BY3 | TextFromFloat.FULL_FORMAT,
                    )
                    assert tff.pad_after == 3 and tff.grouping == 1 and tff.full_format == 1
                    print("Python codecs OK")
                    """
                        .trimIndent()
                )
                runCheckedProcess(
                    "Python codecs run",
                    listOf(python3.absolutePath, testPy.absolutePath),
                    tmpDir,
                )
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        // 4. TypeScript native execution via Node.js (--experimental-transform-types, when enabled)
        val node = findExecutableOnPath("node")
        if (node != null) {
            val probeProc =
                ProcessBuilder(
                        node.absolutePath,
                        "--experimental-transform-types",
                        "-e",
                        "const x: number = 1;",
                    )
                    .redirectErrorStream(true)
                    .start()
            val probeFinished = probeProc.waitFor(10, TimeUnit.SECONDS)
            if (!probeFinished) probeProc.destroyForcibly()
            val supportsTs = probeFinished && probeProc.exitValue() == 0
            if (supportsTs) {
                val tmpDir = Files.createTempDirectory("rc_multilang_codecs_ts").toFile()
                try {
                    File(tmpDir, "remoteComposeOperations.ts").writeText(tsCode)
                    val testTs = File(tmpDir, "test_codecs.ts")
                    testTs.writeText(
                        """
                        import assert from "node:assert/strict";
                        import {
                          AnimationEasing, BitmapData, ColorExpression, FloatExpression,
                          IdEncoding, PathCommandTags, PathData, PathEffectConstants,
                          RemoteContextIds, TextFromFloat,
                          decodePathEffect, encodePathEffect, packAnimationSpec,
                        } from "./remoteComposeOperations.ts";

                        const nanBits = IdEncoding.asNanBits(42);
                        assert.equal(IdEncoding.isNanBits(nanBits), true);
                        assert.equal(IdEncoding.idFromNanBits(nanBits), 42);
                        assert.equal(IdEncoding.isNanBits(0x7F800005), true);
                        assert.equal(IdEncoding.idFromNanBits(0x7F800005), 5);
                        assert.equal(IdEncoding.isNanBits(0x7F800000), false);
                        assert.equal(IdEncoding.isNanBits(0x7FC00000), false);
                        assert.equal(IdEncoding.idFromNanBits(0x7FC00000), IdEncoding.NO_ID);
                        assert.equal(IdEncoding.isNanBits(0xFF800000 | 0), false);
                        assert.equal(IdEncoding.idFromNanBits(0xFF800000 | 0), IdEncoding.NO_ID);
                        assert.equal(IdEncoding.isNanBits(0xFFC00000 | 0), false);
                        assert.equal(IdEncoding.idFromNanBits(0xFFC00000 | 0), IdEncoding.NO_ID);
                        assert.equal(IdEncoding.directId(42), 42);
                        assert.equal(IdEncoding.pointerId(IdEncoding.PTR_DEREFERENCE | 7), 7);
                        assert.equal(RemoteContextIds.isTime(RemoteContextIds.ID_CONTINUOUS_SEC), true);
                        assert.equal(RemoteContextIds.isTime(RemoteContextIds.ID_DAY_OF_MONTH), true);
                        assert.equal(RemoteContextIds.isTime(RemoteContextIds.ID_DENSITY), false);

                        const pts = AnimationEasing.cubicControlPoints(AnimationEasing.CUBIC_STANDARD);
                        assert.deepEqual(pts, [0.4, 0.0, 0.2, 1.0]);
                        const packed = packAnimationSpec(0.5, AnimationEasing.CUBIC_CUSTOM, [0.1, 0.2, 0.3, 0.4], 10.0, 360.0);
                        const fe = new FloatExpression(1, new Float32Array(), new Float32Array(packed));
                        assert.equal(fe.isSpringAnimation(), false);
                        const unpacked = fe.unpackAnimationSpec();
                        assert.ok(Array.isArray(unpacked.params));
                        assert.ok(Math.abs(unpacked.duration - 0.5) < 1e-6);
                        assert.equal(unpacked.type, AnimationEasing.CUBIC_CUSTOM);
                        assert.equal(unpacked.params.length, 4);
                        assert.ok(Math.abs(unpacked.initialValue - 10.0) < 1e-6);
                        assert.ok(Math.abs(unpacked.wrap - 360.0) < 1e-6);

                        const pe = {
                          kind: "compose" as const,
                          outer: { kind: "dash" as const, phase: 2.5, intervals: [4.0, 6.0] },
                          inner: { kind: "discrete" as const, segmentLength: 8.0, deviation: 1.5 },
                        };
                        assert.deepEqual(decodePathEffect(new Float32Array(encodePathEffect(pe))), pe);
                        assert.equal(decodePathEffect([]), null);
                        assert.throws(
                          () => encodePathEffect({
                            kind: "dash" as const,
                            phase: 0,
                            intervals: new Array(PathEffectConstants.MAX_DASH_INTERVALS + 1).fill(1),
                          }),
                          RangeError,
                        );

                        // Build PathData Float32Array via Int32Array so NaN command payloads never pass through float64.
                        const pathBuf = new ArrayBuffer(10 * 4);
                        const pathF32 = new Float32Array(pathBuf);
                        const pathI32 = new Int32Array(pathBuf);
                        pathI32[0] = IdEncoding.asNanBits(PathCommandTags.MOVE);
                        pathF32[1] = 1.0;
                        pathF32[2] = 2.0;
                        pathI32[3] = IdEncoding.asNanBits(PathCommandTags.LINE);
                        pathF32[4] = 0.0;
                        pathF32[5] = 0.0;
                        pathF32[6] = 3.0;
                        pathF32[7] = 4.0;
                        pathI32[8] = IdEncoding.asNanBits(PathCommandTags.CLOSE);
                        pathI32[9] = IdEncoding.asNanBits(PathCommandTags.DONE);
                        const pd = new PathData(1, pathF32);
                        assert.deepEqual(pd.decodePathCommands(), [
                          { kind: "MoveTo", x: 1.0, y: 2.0 },
                          { kind: "LineTo", x: 3.0, y: 4.0 },
                          { kind: "Close" },
                          { kind: "Done" },
                        ]);

                        const bmp = new BitmapData(1, (BitmapData.TYPE_PNG_ALPHA_8 << 16) | 10, (BitmapData.ENCODING_INLINE << 16) | 20, new Uint8Array());
                        assert.equal(bmp.getUncompressedByteSize(), 200);

                        const ce = new ColorExpression(1, (0x80 << 16) | ColorExpression.HSV_MODE, 0, 0, 0);
                        assert.equal(ce.getAlphaPayload(), 0x80);

                        const tff = new TextFromFloat(1, 0, 2, 2, TextFromFloat.PAD_AFTER_ZERO | TextFromFloat.GROUPING_BY3 | TextFromFloat.FULL_FORMAT);
                        assert.equal(tff.getPadAfter(), 3);
                        assert.equal(tff.getGrouping(), 1);
                        assert.equal(tff.getFullFormat(), 1);
                        console.log("TypeScript codecs OK");
                        """
                            .trimIndent()
                    )
                    runCheckedProcess(
                        "TypeScript codecs run",
                        listOf(
                            node.absolutePath,
                            "--experimental-transform-types",
                            testTs.absolutePath,
                        ),
                        tmpDir,
                    )
                } finally {
                    tmpDir.deleteRecursively()
                }
            }
        }
    }

    @Test
    fun testSchemaEnrichmentsAndMetadataValidation() {
        val schema = loadSchema()

        // 1. STORE_R0 renamed cleanly from STORE_RO
        val rpnOps = checkNotNull(schema.rpnOperators)
        assertEquals("STORE_R0", rpnOps.operatorsById[56]?.name)
        assertNull(rpnOps.operatorsByName["STORE_RO"])

        // 2. idEncoding includes nanExponent and noId
        val enc = checkNotNull(schema.idEncoding)
        assertEquals(0x7F800000, enc.nanExponent)
        assertEquals(-1, enc.noId)

        // 3. headerConstants includes dataTypes and version groups
        assertEquals(0, schema.headerConstants["dataTypes"]?.get("DATA_TYPE_INT"))
        assertEquals(1, schema.headerConstants["dataTypes"]?.get("DATA_TYPE_FLOAT"))
        assertEquals(2, schema.headerConstants["dataTypes"]?.get("DATA_TYPE_LONG"))
        assertEquals(3, schema.headerConstants["dataTypes"]?.get("DATA_TYPE_STRING"))
        assertEquals(0x048C0000, schema.headerConstants["version"]?.get("MAGIC_NUMBER"))
        assertEquals(256, schema.headerConstants["version"]?.get("DEFAULT_DIMENSION"))
        assertEquals(0x77, schema.headerConstants["version"]?.get("COMPRESSED_STREAM_MARKER"))

        // 4. HeaderProperty.tag.bits[dataType] enum
        val hpTag = schema.structs["HeaderProperty"]!!.fields.first { it.name == "tag" }
        val dtBit = hpTag.bits!!.first { it.name == "dataType" }
        assertEquals(
            mapOf(
                "DATA_TYPE_INT" to 0,
                "DATA_TYPE_FLOAT" to 1,
                "DATA_TYPE_LONG" to 2,
                "DATA_TYPE_STRING" to 3,
            ),
            dtBit.enumValues,
        )

        // 5. visibilityConstants match Component.Visibility
        assertEquals(
            mapOf(
                "GONE" to 0,
                "VISIBLE" to 1,
                "INVISIBLE" to 2,
                "OVERRIDE_GONE" to 16,
                "OVERRIDE_VISIBLE" to 32,
                "OVERRIDE_INVISIBLE" to 64,
                "CLEAR_OVERRIDE" to 128,
            ),
            schema.visibilityConstants,
        )

        // 6. Border/Background/TextLayout enums and TextLayout.color.resolvedAfter
        val border = schema.operationsByName.getValue("BorderModifierOperation")
        assertEquals(
            mapOf("RECTANGLE" to 0, "CIRCLE" to 1, "ROUNDED_RECTANGLE" to 2),
            border.fields.first { it.name == "shapeType" }.enumValues,
        )
        assertEquals(
            mapOf("COLOR_REF" to 2),
            border.fields.first { it.name == "flags" }.enumValues,
        )
        assertEquals(
            mapOf("LEGACY" to 0, "VERSION_1" to 1),
            border.fields.first { it.name == "reserve1" }.enumValues,
        )
        val bg = schema.operationsByName.getValue("BackgroundModifierOperation")
        assertEquals(mapOf("COLOR_REF" to 2), bg.fields.first { it.name == "flags" }.enumValues)

        val textLayout = schema.operationsByName.getValue("TextLayout")
        assertEquals("textAlign", textLayout.fields.first { it.name == "color" }.resolvedAfter)
        assertEquals(
            mapOf("FONT_STYLE_NORMAL" to 0, "FONT_STYLE_ITALIC" to 1),
            textLayout.fields.first { it.name == "fontStyle" }.enumValues,
        )

        // 7. javaClass overrides on the 17 ops whose schema name differs from remote-core
        val customJavaClasses = schema.operations.filter { it.javaClassName != null }
        assertEquals(17, customJavaClasses.size)
        assertEquals(
            "TouchDownModifierOperation",
            schema.operationsByName.getValue("TouchModifier").resolvedJavaClassName,
        )
        assertEquals(
            "ClickModifierOperation",
            schema.operationsByName.getValue("ClickModifier").resolvedJavaClassName,
        )
        assertEquals(
            "TouchUpModifierOperation",
            schema.operationsByName.getValue("TouchUpModifier").resolvedJavaClassName,
        )
        assertEquals(
            "RippleModifierOperation",
            schema.operationsByName.getValue("RippleModifier").resolvedJavaClassName,
        )
        assertEquals(
            "LoopOperation",
            schema.operationsByName.getValue("Loop").resolvedJavaClassName,
        )
        assertEquals(
            "DrawRect",
            schema.operationsByName.getValue("DrawRect").resolvedJavaClassName,
        )

        // 8. reservedOpcodes cover experimental PropertyBundle (95, 96), 3D (110..118, 120),
        // compressed stream marker (119), and extension range (251..255)
        val reservedById = schema.reservedOpcodes.associateBy { it.id }
        for (id in listOf(95, 96)) {
            assertTrue("Missing experimental PropertyBundle opcode $id", id in reservedById)
            assertEquals(listOf("androidx", "experimental"), reservedById.getValue(id).profiles)
        }
        for (id in listOf(110, 111, 112, 113, 114, 115, 116, 117, 118, 120)) {
            assertTrue("Missing 3D opcode $id", id in reservedById)
            assertEquals(
                listOf("androidx", "widgets", "experimental"),
                reservedById.getValue(id).profiles,
            )
        }
        assertTrue("Missing compressed stream marker opcode 119", 119 in reservedById)
        assertEquals("COMPRESSED_STREAM_MARKER", reservedById.getValue(119).opCodeName)
        assertEquals(listOf("reserved"), reservedById.getValue(119).profiles)
        for (id in 251..255) {
            assertTrue("Missing reserved opcode $id", id in reservedById)
            assertEquals(listOf("reserved"), reservedById.getValue(id).profiles)
        }

        // 8b. Every operation's schema-only opCodeName (without Operations.java reflection)
        // matches Operations.java so validate() and generators never diverge by classpath
        for (op in schema.operations) {
            val expectedJavaOp = SchemaOperation.ID_TO_OPCODE_NAME[op.id]
            if (expectedJavaOp != null) {
                val schemaOnlyOpCodeName =
                    op.opCodeName ?: SchemaOperation.toScreamingSnakeCase(op.name)
                assertEquals(
                    "Operation ${op.name} (${op.id}) must match Operations.$expectedJavaOp without reflection",
                    expectedJavaOp,
                    schemaOnlyOpCodeName,
                )
            }
        }

        // 9. JsonSchemaParser.validate rejects bad resolvedAfter, blank javaClass, duplicate
        // opCodeName, & invalid reservedOpcodes
        val badJson =
            JSONObject(
                """
                {
                  "schemaVersion": "1.0.0",
                  "targetApiLevel": 8,
                  "types": { "int": { "wire": "INT32", "description": "int" } },
                  "categories": [{ "name": "Test", "description": "Test" }],
                  "operations": [
                    {
                      "id": 1,
                      "name": "Sample",
                      "opCodeName": "SAMPLE_OP",
                      "javaClass": "  ",
                      "category": "Test",
                      "description": "Sample",
                      "fields": [
                        { "name": "a", "type": "int", "resolvedAfter": null },
                        { "name": "b", "type": "int", "resolvedAfter": "a" },
                        { "name": "c", "type": "int", "resolvedAfter": 123 }
                      ]
                    },
                    {
                      "id": 2,
                      "name": "SampleDupOpCode",
                      "opCodeName": "SAMPLE_OP",
                      "category": "Test",
                      "description": "Dup opCodeName",
                      "fields": []
                    }
                  ],
                  "reservedOpcodes": [
                    { "id": 1, "name": "Overlap", "opCodeName": "OVERLAP", "profiles": ["reserved"] },
                    { "id": 3, "name": "MissingOpCode", "profiles": ["reserved"] },
                    { "id": 4, "name": "DupOpCode", "opCodeName": "OVERLAP", "profiles": ["reserved"] },
                    { "id": 5, "name": "CollideOpCode", "opCodeName": "SAMPLE_OP", "profiles": ["bogus_profile"] },
                    { "id": 95.5, "name": "FractionalId", "opCodeName": "FRAC_ID", "profiles": ["reserved"] },
                    { "id": 300, "name": "OutOfRangeId", "opCodeName": "OOR_ID", "profiles": ["reserved"] },
                    { "id": 251, "name": "ValidAfterOor", "opCodeName": "VALID_AFTER_OOR", "profiles": ["reserved"] },
                    { "name": "MissingId", "opCodeName": "MISSING_ID" }
                  ]
                }
                """
                    .trimIndent()
            )
        val errors = JsonSchemaParser.validate(badJson)
        assertTrue(
            "Expected blank javaClass error, got: $errors",
            errors.any { it.contains("blank or non-string javaClass") },
        )
        assertTrue(
            "Expected duplicate operation opCodeName error, got: $errors",
            errors.any { it.contains("Duplicate operation opCodeName: SAMPLE_OP") },
        )
        assertTrue(
            "Expected blank/non-string resolvedAfter error, got: $errors",
            errors.any { it.contains("field a has blank or non-string resolvedAfter") },
        )
        assertTrue(
            "Expected non-string resolvedAfter error, got: $errors",
            errors.any { it.contains("field c has blank or non-string resolvedAfter") },
        )
        assertTrue(
            "Expected resolvedAfter ordering error, got: $errors",
            errors.any { it.contains("resolvedAfter 'a' must reference a later field") },
        )
        assertTrue(
            "Expected reservedOpcodes overlap error, got: $errors",
            errors.any { it.contains("overlaps declared operation ID") },
        )
        assertTrue(
            "Expected missing opCodeName error, got: $errors",
            errors.any { it.contains("must specify a non-blank opCodeName") },
        )
        assertTrue(
            "Expected duplicate reserved opCodeName error, got: $errors",
            errors.any { it.contains("Duplicate reserved opcode opCodeName: OVERLAP") },
        )
        assertTrue(
            "Expected colliding reserved opCodeName error, got: $errors",
            errors.any { it.contains("opCodeName 'SAMPLE_OP' collides with declared operation") },
        )
        assertTrue(
            "Expected unknown profile error, got: $errors",
            errors.any { it.contains("unknown profile 'bogus_profile'") },
        )
        assertTrue(
            "Expected fractional id error, got: $errors",
            errors.any { it.contains("index 4 must specify an integer id") },
        )
        assertTrue(
            "Expected out-of-range id error, got: $errors",
            errors.any { it.contains("OutOfRangeId has invalid ID 300") },
        )
        assertFalse(
            "Out-of-range ID 300 must not cause spurious sort error on ValidAfterOor (251), got: $errors",
            errors.any { it.contains("ValidAfterOor") && it.contains("not sorted") },
        )
        assertTrue(
            "Expected missing id error, got: $errors",
            errors.any { it.contains("index 7 must specify an integer id") },
        )
    }

    @Test
    fun testSwiftWriterTablesMaxLengthAndHygieneInvariants() {
        val schema = loadSchema()
        val swiftCode = SwiftGenerator(schema).generate()

        // 1. Writer tables & constants
        for (marker in
            listOf(
                "public enum ReservedOpCode: UInt8, CaseIterable, Equatable, Hashable, Sendable",
                "case propertyBundleModifierOperation = 95",
                "case defineMesh3D = 110",
                "case compressedStreamMarker = 119",
                "case extendedOpcode = 255",
                "public enum ComponentVisibility",
                "public static let gone: Int32 = 0",
                "public static let visible: Int32 = 1",
                "public static let invisible: Int32 = 2",
                "public static let overrideGone: Int32 = 16",
                "public static let overrideVisible: Int32 = 32",
                "public static let overrideInvisible: Int32 = 64",
                "public static let clearOverride: Int32 = 128",
                "public static let tagMask: Int32 = 0xFFFF",
                "public static let inlineValueShift: Int32 = 16",
                "public static func packInline(tag: Int32, value: Int32) -> Int32",
                "public enum FloatExpressionOperator",
                "public static let idStoreR0: Int32 = 56",
                "public static let storeR0: Float =",
                "public static func token(fromId id: Int32) -> Float?",
                "public static func isOperator(_ token: Float) -> Bool",
                "public static func operatorId(_ token: Float) -> Int32",
                "public enum IntegerExpressionOperator",
                "public static let iAdd: Int32 = offset + 1",
                "public static func isOperatorCode(_ value: Int32) -> Bool",
                "public static let magicNumber: Int32 = 76283904",
                "public static let dataTypeInt: Int32 = 0",
                "public static let colorRef: Int32 = 2",
                "public static let fontStyleItalic: Int32 = 1",
            )) {
            assertTrue("Swift output missing writer marker: $marker", swiftCode.contains(marker))
        }

        // 2. Symmetric maxLength checks, implicit prefix/bitfield bounds, and atomic write rollback
        for (marker in
            listOf(
                "public mutating func truncate(to count: Int)",
                "buffer.truncate(to: _startCount)",
                "public mutating func writeUTF8(_ value: String, maxLength: Int? = nil) throws(WireError)",
                "public mutating func writeBuffer(_ value: [UInt8], maxLength: Int? = nil) throws(WireError)",
                "try buffer.writeUTF8(text, maxLength: 4000)",
                "try buffer.writeBuffer(data, maxLength: 100000)",
                "try buffer.writeBuffer(fontData, maxLength: 800000)",
                "guard words.count <= 1024 else",
                "guard expression.count <= 32 else",
                "guard args.count <= 32767 else",
                "guard kerningTable.count <= 65535 else",
                "guard floatUniforms.count <= 255 else",
                "guard animationSpec.count <= 65535 else",
                "try buffer.writeUTF8(val, maxLength: Self.maxHeaderStringLength)",
            )) {
            assertTrue(
                "Swift output missing maxLength write guard: $marker",
                swiftCode.contains(marker),
            )
        }

        // 3. Static Swift hygiene invariants across the full generated file
        assertFalse("Swift must not import Foundation", swiftCode.contains("import Foundation"))
        assertFalse("Swift must not reference NSError", swiftCode.contains("NSError"))
        assertFalse(
            "Swift must not emit SCREAMING_SNAKE static let constants",
            Regex("""public static let [A-Z][A-Z0-9_]+\s*:""").containsMatchIn(swiftCode),
        )
        assertFalse(
            "Swift must not call throwing writeUTF8 without try",
            Regex("""(?<!try )(?<!func )buffer\.writeUTF8\(""").containsMatchIn(swiftCode),
        )
        assertFalse(
            "Swift must not call throwing writeBuffer without try",
            Regex("""(?<!try )(?<!func )buffer\.writeBuffer\(""").containsMatchIn(swiftCode),
        )
        assertFalse(
            "Swift must not emit unqualified min() or max()",
            Regex("""(?<!Swift\.)\b(?:min|max)\(""").containsMatchIn(swiftCode),
        )
        assertEquals(
            "Swift braces must be balanced",
            swiftCode.count { it == '{' },
            swiftCode.count { it == '}' },
        )
        assertTrue(
            "Swift IntegerExpressionOperator.isOperatorCode must use overflow-safe subtraction",
            swiftCode.contains("value.subtractingReportingOverflow(offset)"),
        )

        // 4. Optional swiftc compile + runtime round-trip check when Swift 6 toolchain is present
        val swiftc =
            (listOfNotNull(findExecutableOnPath("swiftc")) +
                    listOf("/usr/bin/swiftc", "/usr/local/bin/swiftc", "/opt/homebrew/bin/swiftc")
                        .map { File(it) })
                .firstOrNull { it.exists() && it.canExecute() && canCompileSwift6(it) }
        if (swiftc != null) {
            val tmpDir = Files.createTempDirectory("rc_swift_test").toFile()
            try {
                File(tmpDir, "RemoteComposeWire.swift").writeText(swiftCode)
                File(tmpDir, "main.swift")
                    .writeText(
                        """
                        // Verify TouchExpression/PathExpression/ParticlesCompare compile,
                        // modern header round-trip, NaN ID equality & decoding, atomic rollback,
                        // and write maxLength/prefix-bound enforcement.
                        assert(IdEncoding.isNanBits(0x7F80_0005))
                        assert(IdEncoding.idFromNanBits(0x7F80_0005) == 5)
                        assert(!IdEncoding.isNanBits(0x7FC0_0000))
                        assert(IdEncoding.idFromNanBits(0x7FC0_0000) == IdEncoding.noId)
                        assert(FloatExpressionOperator.isOperator(FloatExpressionOperator.storeR0))
                        assert(FloatExpressionOperator.operatorId(FloatExpressionOperator.storeR0) == FloatExpressionOperator.idStoreR0)
                        assert(IntegerExpressionOperator.isOperatorCode(IntegerExpressionOperator.iAdd))
                        assert(!IntegerExpressionOperator.isOperatorCode(Int32.min))
                        assert(PaintCommandTags.packInline(tag: PaintCommandTags.style, value: PaintConstants.styleStroke) == (PaintCommandTags.style | (PaintConstants.styleStroke << PaintCommandTags.inlineValueShift)))

                        let nanVar = Float(bitPattern: UInt32(bitPattern: IdEncoding.asNanBits(42)))
                        let doc = WireDocument(
                            majorVersion: 1,
                            minorVersion: 1,
                            patchVersion: 0,
                            width: 400,
                            height: 300,
                            isModernHeader: true,
                            properties: [
                                WireHeaderProperty(key: HeaderProperty.docWidth, value: .int(400)),
                                WireHeaderProperty(key: HeaderProperty.docHeight, value: .int(300)),
                                WireHeaderProperty(key: HeaderProperty.docDensityAtGeneration, value: .float(2.0)),
                                WireHeaderProperty(key: HeaderProperty.docContentDescription, value: .string("test")),
                            ],
                            operations: [
                                .textData(TextData(textId: 42, text: "hello")),
                                .borderModifierOperation(BorderModifierOperation(shapeType: BorderModifierOperation.roundedRectangle)),
                                .floatExpression(FloatExpression(id: 7, expression: [nanVar, 2.0, FloatExpressionOperator.add])),
                            ]
                        )
                        let bytes = try doc.save()
                        let decoded = try WireDocument.load(from: bytes)
                        assert(decoded == doc)
                        assert(try decoded.save() == bytes)

                        // Verify PathEffect/PathCommand bit-exact equality & hashing with NaN IDs
                        let pe1 = PathEffect.dash(phase: nanVar, intervals: [4.0, nanVar])
                        let pe2 = PathEffect.dash(phase: nanVar, intervals: [4.0, nanVar])
                        assert(pe1 == pe2)
                        assert(Set([pe1]).contains(pe2))
                        let pc1 = PathCommand.moveTo(x: nanVar, y: 1.0)
                        let pc2 = PathCommand.moveTo(x: nanVar, y: 1.0)
                        assert(pc1 == pc2)
                        assert(Set([pc1]).contains(pc2))

                        // Verify unknown header payload round-trip & dimension/capabilities reconciliation
                        var mutDoc = decoded
                        mutDoc.width = 800
                        mutDoc.capabilities = 0x1_0000_0001
                        mutDoc.properties.append(
                            WireHeaderProperty(key: 60, value: .unknown(dataType: 7, payload: [1, 2, 3, 4]))
                        )
                        let mutBytes = try mutDoc.save()
                        let mutDecoded = try WireDocument.load(from: mutBytes)
                        assert(mutDecoded.width == 800)
                        assert(mutDecoded.height == 300)
                        assert(mutDecoded.capabilities == 0x1_0000_0001)
                        assert(mutDecoded.properties.contains(WireHeaderProperty(key: 60, value: .unknown(dataType: 7, payload: [1, 2, 3, 4]))))
                        assert(mutDecoded.operations == doc.operations)
                        let mixedInitDoc = WireDocument(
                            width: 640,
                            height: 480,
                            properties: [WireHeaderProperty(key: HeaderProperty.docDensityAtGeneration, value: .float(3.0))]
                        )
                        let mixedDecoded = try WireDocument.load(from: try mixedInitDoc.save())
                        assert(mixedDecoded.width == 640)
                        assert(mixedDecoded.height == 480)

                        // Verify atomic WireBuffer rollback on throwing write
                        var partialBuf = WireBuffer(data: [0xDE, 0xAD])
                        var threwOnTooLong = false
                        do {
                            let bigOp = TextData(textId: 1, text: String(repeating: "a", count: 4001))
                            try bigOp.write(to: &partialBuf)
                        } catch WireError.lengthOutOfRange {
                            threwOnTooLong = true
                        }
                        assert(threwOnTooLong)
                        assert(partialBuf.data == [0xDE, 0xAD])
                        print("Swift codecs OK")
                        """
                            .trimIndent()
                    )
                val outBin = File(tmpDir, "rc_swift_test")
                runCheckedProcess(
                    "Swift compile",
                    listOf(
                        swiftc.absolutePath,
                        "-swift-version",
                        "6",
                        File(tmpDir, "RemoteComposeWire.swift").absolutePath,
                        File(tmpDir, "main.swift").absolutePath,
                        "-o",
                        outBin.absolutePath,
                    ),
                    tmpDir,
                )
                runCheckedProcess("Swift run", listOf(outBin.absolutePath), tmpDir)
            } finally {
                tmpDir.deleteRecursively()
            }
        }
    }

    private fun canCompileSwift6(swiftc: File): Boolean {
        val tmpDir = Files.createTempDirectory("rc_swift6_probe").toFile()
        return try {
            val probe = File(tmpDir, "probe.swift")
            probe.writeText("enum E: Error, Sendable { case a }\nfunc f() throws(E) {}\n")
            val proc =
                ProcessBuilder(
                        swiftc.absolutePath,
                        "-swift-version",
                        "6",
                        "-typecheck",
                        probe.absolutePath,
                    )
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start()
            val finished = proc.waitFor(15, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                false
            } else {
                proc.exitValue() == 0
            }
        } catch (_: Exception) {
            false
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    private fun runCheckedProcess(
        label: String,
        command: List<String>,
        workDir: File,
        timeoutSeconds: Long = 60L,
    ) {
        val logFile = File.createTempFile("rc_proc_", ".log", workDir)
        val proc = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(logFile).start()
        val finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            proc.destroyForcibly()
            val out = logFile.readText()
            assertTrue("$label timed out after ${timeoutSeconds}s:\n$out", false)
        }
        val out = logFile.readText()
        assertEquals("$label failed:\n$out", 0, proc.exitValue())
    }
}
