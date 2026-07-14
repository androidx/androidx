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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.LayoutTestPlayer
import androidx.compose.remote.core.layout.MockRemoteContext
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterInterface
import androidx.compose.remote.creation.modifiers.RecordingModifier
import java.io.ByteArrayInputStream
import org.junit.Test

/**
 * High-performance layout benchmark measuring RemoteCompose layout throughput and speed under
 * different optimization configurations.
 */
class LayoutPerformanceTest : LayoutTestPlayer() {

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
    // 1. Ticker Layout Benchmark Definition
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

                // Add 30 text components to make the layout tree deep and representative
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

    // ======================================================================================
    // 2. Weighted Layout Tree Benchmark Definition
    // ======================================================================================

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
                // Alternating Row/Column tree starting at depth 0
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
            // Even depth: Row layout (distributes horizontally, so children use horizontalWeight)
            writer.startRow(containerMod, RowLayout.START, RowLayout.TOP)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = true)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = true)
            writer.endRow()
        } else {
            // Odd depth: Column layout (distributes vertically, so children use verticalWeight)
            writer.startColumn(containerMod, ColumnLayout.START, ColumnLayout.TOP)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = false)
            createWeightedTree(writer, textId, depth + 1, maxDepth, isParentRow = false)
            writer.endColumn()
        }
    }

    // ======================================================================================
    // 3. Junit Benchmarks Execution
    // ======================================================================================

    @Test
    fun runLayoutBenchmark_TickerColumn() {
        val w = 1000
        val h = 1000
        val byteBuffer = createTickerLayoutBytes(w, h)

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("       BENCHMARK 1: TICKER COLUMN LAYOUT PERFORMANCE")
        println("==============================================================")
        println("Document: 30-item scrolling Column with Canvas")
        println("Iterations: $benchmarkIterations runs (alternating viewport sizes)")
        println("==============================================================")

        val results = runSuite("Ticker Column", byteBuffer, warmUpIterations, benchmarkIterations)
        printResultsTable(results)
    }

    @Test
    fun runLayoutBenchmark_WeightedNestedTree() {
        val w = 1000
        val h = 1000
        val byteBuffer = createWeightedLayoutBytes(w, h)

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("    BENCHMARK 2: DEEPLY NESTED WEIGHTED TREE PERFORMANCE")
        println("==============================================================")
        println("Document: 5-Level Deep Weighted Row/Column Tree (63 Components)")
        println("Iterations: $benchmarkIterations runs (alternating viewport sizes)")
        println("==============================================================")

        val results = runSuite("Weighted Tree", byteBuffer, warmUpIterations, benchmarkIterations)
        printResultsTable(results)
    }

    @Test
    fun runLayoutBenchmark_DynamicStateMutation() {
        val w = 1000
        val h = 1000
        val byteBuffer = createWeightedLayoutBytes(w, h)

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("  JVM BENCHMARK 3: DYNAMIC FRAME INVALIDATION")
        println("==============================================================")
        println("Document: 5-Level Deep Weighted Tree (63 Components) under leaf invalidation")
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
    fun runLayoutBenchmark_DemoSuite() {
        val demoByteArrays =
            arrayOf(
                createTickerLayoutBytes(1000, 1000),
                createWeightedLayoutBytes(1000, 1000),
                createTickerLayoutBytes(800, 600),
                createWeightedLayoutBytes(800, 600),
                createTickerLayoutBytes(1200, 900),
                createWeightedLayoutBytes(1200, 900),
            )

        val warmUpIterations = 500
        val benchmarkIterations = 500

        println("\n==============================================================")
        println("   BENCHMARK 4: SYNTHETIC MULTI-DOCUMENT SUITE (6 DOCUMENTS)")
        println("==============================================================")
        println("Documents: 6 varying Ticker Columns and Weighted Layout Trees")
        println("Iterations: $benchmarkIterations runs per document (alternating viewports)")
        println("==============================================================")

        val results = ArrayList<BenchmarkResult>()

        results.add(
            runSuiteBenchmark(
                "1. OPTIMIZATION_NONE (No Cache, Relayout All, HashMap)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_NONE,
            )
        )
        results.add(
            runSuiteBenchmark(
                "2. OPTIMIZATION_MEASURE_CACHE (Constraint Cache Only)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_MEASURE_CACHE,
            )
        )
        results.add(
            runSuiteBenchmark(
                "3. OPTIMIZATION_LAYOUT_BOUNDARIES (Relayout Boundaries Only)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES,
            )
        )
        results.add(
            runSuiteBenchmark(
                "4. OPTIMIZATION_FLAT_MEASURE_PASS (Flat Measure Pass Only)",
                demoByteArrays,
                warmUpIterations,
                benchmarkIterations,
                optimizationMask = CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS,
            )
        )
        results.add(
            runSuiteBenchmark(
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
    private fun runSuiteBenchmark(
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
                val debugContext = MockRemoteContext()
                debugContext.setAnimationEnabled(false)
                debugContext.setDensity(doc.getDensity())
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

                debugContext.stringBuilder.setLength(0)
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

                debugContext.stringBuilder.setLength(0)
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
    // 4. Benchmark Runner Helpers
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
                optimizationMask = CoreDocument.OPTIMIZATION_ALL,
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
        // Load the document
        val doc = CoreDocument(SystemClock())
        doc.setOptimizationLevel(optimizationMask)
        val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(byteBuffer))
        doc.initFromBuffer(buffer)

        val debugContext = MockRemoteContext()
        debugContext.setAnimationEnabled(false)
        debugContext.setDensity(doc.getDensity())
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

            debugContext.stringBuilder.setLength(0)
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

            // Force measure invalidation if caching is disabled
            if (!doc.isMeasureCacheEnabled()) {
                doc.rootLayoutComponent?.invalidateMeasure()
            }

            debugContext.stringBuilder.setLength(0)
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

        val debugContext = MockRemoteContext()
        debugContext.setAnimationEnabled(false)
        debugContext.setDensity(doc.getDensity())
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

    private fun findLeaf(c: Component): Component {
        for (op in c.mList) {
            if (op is Component) {
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
