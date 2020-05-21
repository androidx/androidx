/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.benchmark.test

import android.os.Handler
import android.os.Looper
import androidx.compose.FrameManager
import androidx.compose.frames.commit
import androidx.compose.frames.inFrame
import androidx.compose.frames.open
import androidx.compose.mutableStateOf
import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.core.ModelObserver
import androidx.ui.integration.test.foundation.RectsInColumnSharedModelTestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.random.Random

/**
 * Benchmark that runs [RectsInColumnSharedModelTestCase].
 */
@LargeTest
@RunWith(Parameterized::class)
class ModelObserverBenchmark(
    private val numberOfModels: Int,
    private val numberOfNodes: Int
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "models = {0}, nodes = {1}")
        fun initParameters(): Array<Any> = arrayOf(
            arrayOf(1000, 1000),
            arrayOf(10000, 100),
            arrayOf(100000, 10),
            arrayOf(100, 1000)
        )
    }

    private val doNothing: (Int) -> Unit = { _ -> }

    @get:Rule
    val rule = ComposeBenchmarkRule(enableTransitions = false)

    lateinit var modelObserver: ModelObserver
    val models = List(numberOfModels) { mutableStateOf(0) }
    val nodes = List(numberOfNodes) { it }
    lateinit var random: Random
    val numObservations = numberOfModels / 10

    @Before
    fun setup() {
        random = Random(0)
        rule.runOnUiThread {
            val handler = Handler(Looper.getMainLooper())
            modelObserver = ModelObserver { command ->
                if (Looper.myLooper() !== handler.looper) {
                    handler.post(command)
                } else {
                    command()
                }
            }
            FrameManager.ensureStarted()
        }
        if (!inFrame) {
            open(readOnly = true)
        }
        modelObserver.enableModelUpdatesObserving(true)
        setupObservations()
        commit()
    }

    @After
    fun teardown() {
        rule.runOnUiThread {
            modelObserver.enableModelUpdatesObserving(false)
        }
    }

    @Test
    fun modelObservation() {
        rule.runOnUiThread {
            rule.measureRepeated {
                random = Random(0)
                val node = nodes[random.nextInt(numberOfNodes)]
                observeForNode(node)
            }
        }
    }

    @Test
    fun nestedModelObservation() {
        rule.runOnUiThread {
            modelObserver.observeReads(nodes[0], doNothing) {
                rule.measureRepeated {
                    random = Random(0)
                    val node = nodes[random.nextInt(numberOfNodes)]
                    observeForNode(node)
                }
            }
        }
    }

    @Test
    fun modelClear() {
        rule.runOnUiThread {
            rule.measureRepeated {
                random = Random(0)
                val node = nodes[random.nextInt(numberOfNodes)]
                modelObserver.clear(node)
                runWithTimingDisabled {
                    observeForNode(node)
                }
            }
        }
    }

    @Test
    fun modelNotification() {
        // assume 5 model changes
        val fakeChanges = setOf(
            models[random.nextInt(numberOfModels)],
            models[random.nextInt(numberOfModels)],
            models[random.nextInt(numberOfModels)],
            models[random.nextInt(numberOfModels)],
            models[random.nextInt(numberOfModels)]
        )
        val fakeFrame = open(readOnly = true)
        commit(fakeFrame)
        rule.runOnUiThread {
            rule.measureRepeated {
                modelObserver.frameCommitObserver(fakeChanges, fakeFrame)
            }
        }
    }

    private fun setupObservations() {
        nodes.forEach { observeForNode(it) }
    }

    private fun observeForNode(node: Int) {
        modelObserver.observeReads(node, doNothing) {
            repeat(numObservations) {
                // just access the value
                models[random.nextInt(numberOfModels)].value
            }
        }
    }
}
