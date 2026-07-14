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

package androidx.compose.remote.integration.view.demos

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterInterface
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.integration.view.demos.dsl.dslTicker
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android Instrumentation Benchmark measuring layout performance of RemoteCompose on-device under
 * ART (Android Runtime).
 */
@RunWith(AndroidJUnit4::class)
class AndroidLayoutPerformanceTest {

    private class DummyPaintContext(context: RemoteContext) : PaintContext(context) {
        override fun drawBitmap(
            imageId: Int,
            srcLeft: Int,
            srcTop: Int,
            srcRight: Int,
            srcBottom: Int,
            dstLeft: Int,
            dstTop: Int,
            dstRight: Int,
            dstBottom: Int,
            cdId: Int,
        ) {}

        override fun drawBitmap(id: Int, left: Float, top: Float, right: Float, bottom: Float) {}

        override fun scale(scaleX: Float, scaleY: Float) {}

        override fun translate(translateX: Float, translateY: Float) {}

        override fun drawArc(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}

        override fun drawSector(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}

        override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {}

        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {}

        override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun drawPath(id: Int, start: Float, end: Float) {}

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun savePaint() {}

        override fun restorePaint() {}

        override fun replacePaint(
            paintBundle: androidx.compose.remote.core.operations.paint.PaintBundle
        ) {}

        override fun drawRoundRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            rx: Float,
            ry: Float,
        ) {}

        override fun drawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {}

        override fun getTextBounds(p0: Int, p1: Int, p2: Int, p3: Int, p4: FloatArray) {}

        override fun layoutComplexText(
            p0: Int,
            p1: Int,
            p2: Int,
            p3: Int,
            p4: Int,
            p5: Int,
            p6: Float,
            p7: Float,
            p8: Float,
            p9: Float,
            p10: Float,
            p11: Int,
            p12: Int,
            p13: Int,
            p14: Boolean,
            p15: Boolean,
            p16: Int,
        ): androidx.compose.remote.core.RcPlatformServices.ComputedTextLayout? = null

        override fun drawTextRun(
            textId: Int,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            isRtl: Boolean,
        ) {}

        override fun drawComplexText(
            p0: androidx.compose.remote.core.RcPlatformServices.ComputedTextLayout?
        ) {}

        override fun drawTweenPath(
            path1Id: Int,
            path2Id: Int,
            tween: Float,
            start: Float,
            end: Float,
        ) {}

        override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {}

        override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {}

        override fun applyPaint(
            mPaintData: androidx.compose.remote.core.operations.paint.PaintBundle
        ) {}

        override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {}

        override fun matrixTranslate(translateX: Float, translateY: Float) {}

        override fun matrixSkew(skewX: Float, skewY: Float) {}

        override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {}

        override fun matrixSave() {}

        override fun matrixRestore() {}

        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun clipPath(pathId: Int, regionOp: Int) {}

        override fun roundedClipRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            rx: Float,
            ry: Float,
        ) {}

        override fun reset() {}

        override fun startGraphicsLayer(w: Int, h: Int) {}

        override fun setGraphicsLayer(attributes: java.util.HashMap<Int, Any>) {}

        override fun endGraphicsLayer() {}

        override fun getText(id: Int): String? = null

        override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}

        override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
    }

    private class AndroidMockRemoteContext(document: CoreDocument) :
        RemoteContext(RemoteClock.SYSTEM) {
        init {
            mDocument = document
            mPaintContext = DummyPaintContext(this)
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

        override fun addCollection(
            id: Int,
            collection: androidx.compose.remote.core.operations.utilities.ArrayAccess,
        ) {
            mRemoteComposeState.addCollection(id, collection)
        }

        override fun putDataMap(
            id: Int,
            map: androidx.compose.remote.core.operations.utilities.DataMap,
        ) {}

        override fun getDataMap(
            id: Int
        ): androidx.compose.remote.core.operations.utilities.DataMap? = null

        override fun runAction(id: Int, metadata: String) {}

        override fun runNamedAction(id: Int, value: Any?) {}

        override fun putObject(id: Int, value: Any) {
            mRemoteComposeState.updateData(id, value)
        }

        override fun getObject(id: Int): Any? = mRemoteComposeState.getObject(id)

        override fun hapticEffect(type: Int) {}

        override fun loadSound(soundId: Int, data: ByteArray) {}

        override fun playSound(soundId: Int) {}

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

        override fun loadAnimatedFloat(
            id: Int,
            animatedFloat: androidx.compose.remote.core.operations.FloatExpression,
        ) {}

        override fun loadShader(
            id: Int,
            value: androidx.compose.remote.core.operations.ShaderData,
        ) {}

        override fun getFloat(id: Int): Float = mRemoteComposeState.getFloat(id)

        override fun getInteger(id: Int): Int = mRemoteComposeState.getInteger(id)

        override fun getLong(id: Int): Long = 0

        override fun getColor(id: Int): Int = mRemoteComposeState.getColor(id)

        override fun listensTo(
            id: Int,
            variableSupport: androidx.compose.remote.core.VariableSupport,
        ) {}

        override fun updateOps(): Int = 0

        override fun getShader(id: Int): androidx.compose.remote.core.operations.ShaderData? = null

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

    private class MockPlatform : RcPlatformServices {
        override fun pathToFloatArray(path: Any): FloatArray? = FloatArray(0)

        override fun parsePath(path: String): Any = Any()

        override fun imageToByteArray(image: Any): ByteArray? = ByteArray(0)

        override fun getImageWidth(image: Any): Int = 0

        override fun getImageHeight(image: Any): Int = 0

        override fun isAlpha8Image(image: Any): Boolean = false

        override fun log(category: RcPlatformServices.LogCategory, message: String) {}
    }

    // ======================================================================================
    // 1. Layout Byte Generators
    // ======================================================================================

    private fun createTickerLayoutBytes(w: Int, h: Int): ByteArray {
        val platform = MockPlatform()
        val writer =
            RemoteComposeWriter(
                platform,
                7,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, w),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, h),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Ticker"),
            )

        writer.beginGlobal()
        val bgId = writer.addThemedColor(null, 0xFFEEEEEE.toInt(), null, 0xFF111111.toInt())
        writer.setColorName(bgId.toInt(), "bg")
        val textId = writer.addText("123.45")
        writer.setStringName(textId, "priceText")

        writer.root(
            RemoteComposeWriterInterface {
                writer.endGlobal()
                val rootMod = RecordingModifier()
                rootMod.fillMaxSize(1.0f).backgroundId(bgId.toInt())
                writer.startColumn(rootMod, BoxLayout.START, BoxLayout.TOP)

                val scrollMod = RecordingModifier()
                scrollMod.fillMaxWidth(1.0f).height(400f)
                writer.startColumn(scrollMod, BoxLayout.START, BoxLayout.TOP)

                for (i in 1..30) {
                    val itemTextId = writer.addText("Item $i")
                    writer.startTextComponent(
                        RecordingModifier().fillMaxWidth(1.0f).height(30f),
                        itemTextId,
                        -1,
                        0xFF000000.toInt(),
                        -1,
                        16f,
                        -1f,
                        -1f,
                        0,
                        400f,
                        null,
                        1,
                        1,
                        1,
                        0f,
                        0f,
                        1f,
                        0,
                        0,
                        0,
                        false,
                        false,
                        null,
                        null,
                        false,
                        0,
                    )
                    writer.endTextComponent()
                }

                val canvasMod = RecordingModifier()
                canvasMod.fillMaxWidth(1.0f).height(100f)
                writer.startCanvas(canvasMod)
                writer.endCanvas()

                writer.endColumn()
                writer.endColumn()
            }
        )
        return writer.encodeToByteArray()
    }

    private fun createWeightedLayoutBytes(w: Int, h: Int): ByteArray {
        val platform = MockPlatform()
        val writer =
            RemoteComposeWriter(
                platform,
                7,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, w),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, h),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "WeightedTree"),
            )

        writer.beginGlobal()
        val bgId = writer.addThemedColor(null, 0xFFFFFFFF.toInt(), null, 0xFF000000.toInt())
        writer.setColorName(bgId.toInt(), "bg")
        val textId = writer.addText("Val")
        writer.setStringName(textId, "valueText")

        writer.root(
            RemoteComposeWriterInterface {
                writer.endGlobal()
                val rootMod = RecordingModifier().fillMaxSize(1.0f).backgroundId(bgId.toInt())
                writer.startRow(rootMod, RowLayout.START, RowLayout.TOP)
                createWeightedTree(writer, textId, depth = 1, maxDepth = 5, isParentRow = true)
                writer.endRow()
            }
        )
        return writer.encodeToByteArray()
    }

    private fun createWeightedTree(
        writer: RemoteComposeWriter,
        textId: Int,
        depth: Int,
        maxDepth: Int,
        isParentRow: Boolean,
    ) {
        if (depth == maxDepth) {
            val leafMod = RecordingModifier()
            if (isParentRow) {
                leafMod.horizontalWeight(1.0f).fillMaxHeight(1.0f)
            } else {
                leafMod.verticalWeight(1.0f).fillMaxWidth(1.0f)
            }
            writer.startTextComponent(
                leafMod,
                textId,
                -1,
                0xFF000000.toInt(),
                -1,
                12f,
                -1f,
                -1f,
                0,
                400f,
                null,
                1,
                1,
                1,
                0f,
                0f,
                1f,
                0,
                0,
                0,
                false,
                false,
                null,
                null,
                false,
                0,
            )
            writer.endTextComponent()
            return
        }

        val containerMod = RecordingModifier()
        if (isParentRow) {
            containerMod.horizontalWeight(1.0f).fillMaxHeight(1.0f)
        } else {
            containerMod.verticalWeight(1.0f).fillMaxWidth(1.0f)
        }

        if (depth % 2 == 0) {
            writer.startRow(containerMod, RowLayout.START, RowLayout.TOP)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = true)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = true)
            writer.endRow()
        } else {
            writer.startColumn(containerMod, ColumnLayout.START, ColumnLayout.TOP)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = false)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = false)
            writer.endColumn()
        }
    }

    // ======================================================================================
    // 2. Android Instrumentation Tests (Benchmarks)
    // ======================================================================================

    @Test
    fun runLayoutBenchmark_Android_TickerColumn() {
        val w = 1000
        val h = 1000
        val byteBuffer = createTickerLayoutBytes(w, h)

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("   ANDROID ON-DEVICE BENCHMARK 1: TICKER COLUMN PERFORMANCE")
        println("==============================================================")
        println("Document: 30-item scrolling Column with Canvas")
        println("Iterations: $benchmarkIterations runs (alternating viewport sizes)")
        println("==============================================================")

        val results =
            runSuite("Android Ticker Column", byteBuffer, warmUpIterations, benchmarkIterations)
        printResultsTable(results)
    }

    @Test
    fun runLayoutBenchmark_Android_WeightedNestedTree() {
        val w = 1000
        val h = 1000
        val byteBuffer = createWeightedLayoutBytes(w, h)

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("  ANDROID ON-DEVICE BENCHMARK 2: NESTED WEIGHTED TREE SPEED")
        println("==============================================================")
        println("Document: 5-Level Deep Weighted Row/Column Tree (63 Components)")
        println("Iterations: $benchmarkIterations runs (alternating viewport sizes)")
        println("==============================================================")

        val results =
            runSuite("Android Weighted Tree", byteBuffer, warmUpIterations, benchmarkIterations)
        printResultsTable(results)
    }

    @Test
    fun runLayoutBenchmark_Android_DslTicker() {
        val byteBuffer = dslTicker()

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("  ANDROID ON-DEVICE BENCHMARK 3: AUTHENTIC TICKER PERFORMANCE")
        println("==============================================================")
        println("Document: Official dslTicker() layout from player-view-demos")
        println("Iterations: $benchmarkIterations runs (alternating viewport sizes)")
        println("==============================================================")

        val results =
            runSuite("Android Authentic Ticker", byteBuffer, warmUpIterations, benchmarkIterations)
        printResultsTable(results)
    }

    @Test
    fun runLayoutBenchmark_Android_DynamicStateMutation() {
        val w = 1000
        val h = 1000
        val byteBuffer = createWeightedLayoutBytes(w, h)

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("  ANDROID ON-DEVICE BENCHMARK 5: DYNAMIC FRAME INVALIDATION")
        println("==============================================================")
        println(
            "Document: 5-Level Deep Weighted Tree (63 Components) under frame-by-frame invalidation"
        )
        println("Iterations: $benchmarkIterations runs")
        println("==============================================================")

        val results =
            runDynamicSuite(
                "Dynamic Invalidation",
                byteBuffer,
                warmUpIterations,
                benchmarkIterations,
            )
        printResultsTable(results)
    }

    @Test
    fun runLayoutBenchmark_Android_DemoSuite() {
        val demoByteArrays =
            arrayOf(
                androidx.compose.remote.integration.view.demos.dsl.dslTicker(),
                androidx.compose.remote.integration.view.demos.dsl.dslStopwatchDemo(),
                androidx.compose.remote.integration.view.demos.dsl.dslCollapsiblePriorityDemo(),
                androidx.compose.remote.integration.view.demos.dsl.dslDemoActivityRings(),
                androidx.compose.remote.integration.view.demos.dsl.dslDemoCalendarHeatmap(),
                androidx.compose.remote.integration.view.demos.dsl.dslDemoStepProgressArc(),
                androidx.compose.remote.integration.view.demos.dsl.dslServerClock(),
                androidx.compose.remote.integration.view.demos.dsl.dslSpreadSheet(),
                androidx.compose.remote.integration.view.demos.dsl.dslClock(),
                androidx.compose.remote.integration.view.demos.dsl.dslCountdown(),
            )

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("  ANDROID ON-DEVICE BENCHMARK 4: COMPREHENSIVE DEMO SUITE (10 DEMOS)")
        println("==============================================================")
        println("Documents: 10 authentic production-grade layouts from player-view-demos")
        println("Iterations: $benchmarkIterations runs per document (alternating viewports)")
        println("==============================================================")

        val results = ArrayList<BenchmarkResult>()

        results.add(
            runOnDeviceSuiteBenchmark(
                "1. OPTIMIZATION_NONE (No Cache, Relayout All, HashMap)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_NONE,
            )
        )
        results.add(
            runOnDeviceSuiteBenchmark(
                "2. OPTIMIZATION_MEASURE_CACHE (Constraint Cache Only)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_MEASURE_CACHE,
            )
        )
        results.add(
            runOnDeviceSuiteBenchmark(
                "3. OPTIMIZATION_LAYOUT_BOUNDARIES (Relayout Boundaries Only)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES,
            )
        )
        results.add(
            runOnDeviceSuiteBenchmark(
                "4. OPTIMIZATION_FLAT_MEASURE_PASS (Flat Measure Pass Only)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS,
            )
        )
        results.add(
            runOnDeviceSuiteBenchmark(
                "5. OPTIMIZATION_ALL (Cache + Boundaries + Flat Pass)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask =
                    CoreDocument.OPTIMIZATION_MEASURE_CACHE or
                        CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES or
                        CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS,
            )
        )

        printResultsTable(results)
    }

    @Suppress("BanThreadSleep")
    private fun runOnDeviceSuiteBenchmark(
        name: String,
        demoByteArrays: Array<ByteArray>,
        warmUpRuns: Int,
        measuredRuns: Int,
        optimizationMask: Int,
    ): BenchmarkResult {
        val docs =
            demoByteArrays.map { bytes ->
                val doc = CoreDocument(SystemClock())
                doc.setOptimizationLevel(optimizationMask)
                val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes))
                doc.initFromBuffer(buffer)
                doc
            }

        val contexts =
            docs.map { doc ->
                val debugContext = AndroidMockRemoteContext(doc)
                debugContext.mWidth = 1000f
                debugContext.mHeight = 1000f
                doc.initializeContext(debugContext)
                debugContext
            }

        val width = 1000f
        val height = 1000f

        // 1. Warm-up Phase
        for (i in 0 until warmUpRuns) {
            for (docIdx in docs.indices) {
                val doc = docs[docIdx]
                val debugContext = contexts[docIdx]
                debugContext.mWidth = width
                debugContext.mHeight = height
                debugContext.loadFloat(RemoteContext.ID_WINDOW_WIDTH, debugContext.mWidth)
                debugContext.loadFloat(RemoteContext.ID_WINDOW_HEIGHT, debugContext.mHeight)

                doc.measure(debugContext, 0f, debugContext.mWidth, 0f, debugContext.mHeight)
            }
        }

        // 2. Measurement Phase
        System.gc()
        Thread.sleep(100) // Stabilize GC

        val startTime = System.nanoTime()

        for (i in 0 until measuredRuns) {
            for (docIdx in docs.indices) {
                val doc = docs[docIdx]
                val debugContext = contexts[docIdx]
                debugContext.mWidth = width
                debugContext.mHeight = height
                debugContext.loadFloat(RemoteContext.ID_WINDOW_WIDTH, debugContext.mWidth)
                debugContext.loadFloat(RemoteContext.ID_WINDOW_HEIGHT, debugContext.mHeight)

                if (!doc.isMeasureCacheEnabled()) {
                    doc.rootLayoutComponent?.invalidateMeasure()
                }

                doc.measure(debugContext, 0f, debugContext.mWidth, 0f, debugContext.mHeight)
            }
        }

        val endTime = System.nanoTime()
        val durationNs = endTime - startTime
        val avgTimeUs = (durationNs / measuredRuns.toDouble()) / 1000.0
        val opsPerSec = 1_000_000_000.0 / (durationNs / measuredRuns.toDouble())

        return BenchmarkResult(name, avgTimeUs, opsPerSec)
    }

    // ======================================================================================
    // 3. Benchmark Runner & Suite Helpers
    // ======================================================================================

    private fun runSuite(
        suiteName: String,
        byteBuffer: ByteArray,
        warmUpRuns: Int,
        measuredRuns: Int,
    ): ArrayList<BenchmarkResult> {
        val results = ArrayList<BenchmarkResult>()

        results.add(
            runBenchmark(
                "1. OPTIMIZATION_NONE (No Cache, Relayout All, HashMap)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_NONE,
            )
        )
        results.add(
            runBenchmark(
                "2. OPTIMIZATION_MEASURE_CACHE (Constraint Cache Only)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_MEASURE_CACHE,
            )
        )
        results.add(
            runBenchmark(
                "3. OPTIMIZATION_LAYOUT_BOUNDARIES (Relayout Boundaries Only)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES,
            )
        )
        results.add(
            runBenchmark(
                "4. OPTIMIZATION_FLAT_MEASURE_PASS (Flat Measure Pass Only)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS,
            )
        )
        results.add(
            runBenchmark(
                "5. OPTIMIZATION_ALL (Cache + Boundaries + Flat Pass)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask =
                    CoreDocument.OPTIMIZATION_MEASURE_CACHE or
                        CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES or
                        CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS,
            )
        )

        return results
    }

    @Suppress("BanThreadSleep")
    private fun runBenchmark(
        name: String,
        byteBuffer: ByteArray,
        warmUpRuns: Int,
        measuredRuns: Int,
        optimizationMask: Int,
    ): BenchmarkResult {
        val doc = CoreDocument(SystemClock())
        doc.setOptimizationLevel(optimizationMask)
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(byteBuffer))
        doc.initFromBuffer(buffer)

        val debugContext = AndroidMockRemoteContext(doc)
        debugContext.mWidth = 1000f
        debugContext.mHeight = 1000f
        doc.initializeContext(debugContext)

        val width = 1000f
        val height = 1000f

        // 1. Warm-up Phase
        for (i in 0 until warmUpRuns) {
            debugContext.mWidth = width
            debugContext.mHeight = height
            debugContext.loadFloat(RemoteContext.ID_WINDOW_WIDTH, debugContext.mWidth)
            debugContext.loadFloat(RemoteContext.ID_WINDOW_HEIGHT, debugContext.mHeight)

            doc.measure(debugContext, 0f, debugContext.mWidth, 0f, debugContext.mHeight)
        }

        // 2. Measurement Phase
        System.gc()
        Thread.sleep(100) // Stabilize GC

        val startTime = System.nanoTime()

        for (i in 0 until measuredRuns) {
            debugContext.mWidth = width
            debugContext.mHeight = height
            debugContext.loadFloat(RemoteContext.ID_WINDOW_WIDTH, debugContext.mWidth)
            debugContext.loadFloat(RemoteContext.ID_WINDOW_HEIGHT, debugContext.mHeight)

            if (!doc.isMeasureCacheEnabled()) {
                doc.rootLayoutComponent?.invalidateMeasure()
            }

            doc.measure(debugContext, 0f, debugContext.mWidth, 0f, debugContext.mHeight)
        }

        val endTime = System.nanoTime()
        val durationNs = endTime - startTime
        val avgTimeUs = (durationNs / measuredRuns.toDouble()) / 1000.0
        val opsPerSec = 1_000_000_000.0 / (durationNs / measuredRuns.toDouble())

        return BenchmarkResult(name, avgTimeUs, opsPerSec)
    }

    private fun runDynamicSuite(
        suiteName: String,
        byteBuffer: ByteArray,
        warmUpRuns: Int,
        measuredRuns: Int,
    ): ArrayList<BenchmarkResult> {
        val results = ArrayList<BenchmarkResult>()

        results.add(
            runDynamicBenchmark(
                "1. OPTIMIZATION_NONE (No Cache, Relayout All, HashMap)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_NONE,
            )
        )
        results.add(
            runDynamicBenchmark(
                "2. OPTIMIZATION_MEASURE_CACHE (Constraint Cache Only)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_MEASURE_CACHE,
            )
        )
        results.add(
            runDynamicBenchmark(
                "3. OPTIMIZATION_LAYOUT_BOUNDARIES (Relayout Boundaries Only)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES,
            )
        )
        results.add(
            runDynamicBenchmark(
                "4. OPTIMIZATION_FLAT_MEASURE_PASS (Flat Measure Pass Only)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask = CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS,
            )
        )
        results.add(
            runDynamicBenchmark(
                "5. OPTIMIZATION_ALL (Cache + Boundaries + Flat Pass)",
                byteBuffer,
                warmUpRuns,
                measuredRuns,
                optimizationMask =
                    CoreDocument.OPTIMIZATION_MEASURE_CACHE or
                        CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES or
                        CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS,
            )
        )

        return results
    }

    @Suppress("BanThreadSleep")
    private fun runDynamicBenchmark(
        name: String,
        byteBuffer: ByteArray,
        warmUpRuns: Int,
        measuredRuns: Int,
        optimizationMask: Int,
    ): BenchmarkResult {
        val doc = CoreDocument(SystemClock())
        doc.setOptimizationLevel(optimizationMask)
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(byteBuffer))
        doc.initFromBuffer(buffer)

        val debugContext = AndroidMockRemoteContext(doc)
        debugContext.mWidth = 1000f
        debugContext.mHeight = 1000f
        doc.initializeContext(debugContext)

        val width = 1000f
        val height = 1000f

        val leafComponent = doc.rootLayoutComponent?.let { findLeaf(it) } ?: doc.rootLayoutComponent

        // 1. Warm-up Phase with frame-by-frame invalidation
        for (i in 0 until warmUpRuns) {
            debugContext.mWidth = width
            debugContext.mHeight = height
            leafComponent?.invalidateMeasure()
            doc.measure(debugContext, 0f, debugContext.mWidth, 0f, debugContext.mHeight)
        }

        // 2. Measurement Phase with frame-by-frame invalidation
        System.gc()
        Thread.sleep(100)

        val startTime = System.nanoTime()

        for (i in 0 until measuredRuns) {
            debugContext.mWidth = width
            debugContext.mHeight = height
            leafComponent?.invalidateMeasure()
            doc.measure(debugContext, 0f, debugContext.mWidth, 0f, debugContext.mHeight)
        }

        val endTime = System.nanoTime()
        val durationNs = endTime - startTime
        val avgTimeUs = (durationNs / measuredRuns.toDouble()) / 1000.0
        val opsPerSec = 1_000_000_000.0 / (durationNs / measuredRuns.toDouble())

        return BenchmarkResult(name, avgTimeUs, opsPerSec)
    }

    private fun findLeaf(
        c: androidx.compose.remote.core.operations.layout.Component
    ): androidx.compose.remote.core.operations.layout.Component {
        for (op in c.mList) {
            if (op is androidx.compose.remote.core.operations.layout.Component) {
                return findLeaf(op)
            }
        }
        return c
    }

    private fun printResultsTable(results: ArrayList<BenchmarkResult>) {
        println(
            "\n=================================================================================================="
        )
        println(
            String.format(
                "| %-45s | %-15s | %-10s | %-12s |",
                "Configuration",
                "Avg Time (us)",
                "Ops/Sec",
                "Speedup",
            )
        )
        println(
            "=================================================================================================="
        )
        val baselineAvg = results[0].avgTimeUs
        for (res in results) {
            val speedup = baselineAvg / res.avgTimeUs
            println(
                String.format(
                    "| %-45s | %12.2f us | %10.0f | %11.2fx |",
                    res.name,
                    res.avgTimeUs,
                    res.opsPerSec,
                    speedup,
                )
            )
        }
        println(
            "==================================================================================================\n"
        )
    }

    private data class BenchmarkResult(
        val name: String,
        val avgTimeUs: Double,
        val opsPerSec: Double,
    )
}
