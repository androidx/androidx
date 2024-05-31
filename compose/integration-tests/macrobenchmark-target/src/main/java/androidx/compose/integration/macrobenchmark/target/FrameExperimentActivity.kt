/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark.target

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Trace
import android.view.View
import androidx.activity.ComponentActivity

private const val FRAME_ADDED_WORK_MS = 20L
private const val FRAME_BASELINE_WORK_MS = 10L
private const val FRAME_COUNT = 100

// NOTE: Keep in sync with FrameExperimentBenchmark!!
private enum class FrameMode(val id: Int) {
    Fast(0),
    PrefetchEveryFrame(1),
    WorkDuringEveryFrame(2),
    PrefetchSomeFrames(3),
}

private class FrameExperimentView(context: Context, val mode: FrameMode) : View(context) {

    init {
        setOnClickListener {
            remainingFrames = FRAME_COUNT - 1
            invalidate()
        }
    }

    var remainingFrames = 0

    fun work(durationMs: Long = FRAME_ADDED_WORK_MS, label: String = "Added item work") {
        Trace.beginSection(label)

        // spin!
        val endTime = System.nanoTime() + durationMs * 1_000_000
        @Suppress("ControlFlowWithEmptyBody") while (System.nanoTime() < endTime) {}

        Trace.endSection()
    }

    val paintA = Paint().apply { setColor(Color.LTGRAY) }
    val paintB = Paint().apply { setColor(Color.WHITE) }

    override fun onDraw(canvas: Canvas) {
        if (mode == FrameMode.WorkDuringEveryFrame) {
            work()
        }
        super.onDraw(canvas)

        work(durationMs = FRAME_BASELINE_WORK_MS, "Baseline work frame $remainingFrames")

        // small rect to reduce flicker
        canvas.drawRect(0f, 0f, 200f, 200f, if (remainingFrames % 2 == 0) paintA else paintB)

        if (remainingFrames >= 1) {
            remainingFrames--
            invalidate()

            if (
                mode == FrameMode.PrefetchEveryFrame ||
                    (mode == FrameMode.PrefetchSomeFrames && remainingFrames % 5 == 0)
            ) {
                this.post { work() }
            }
        }
    }
}

class FrameExperimentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frameModeId = intent.getIntExtra(EXTRA_FRAME_MODE, defaultMode.id)
        val frameMode = FrameMode.values().first { it.id == frameModeId }

        setContentView(FrameExperimentView(this, frameMode))
    }

    companion object {
        const val EXTRA_FRAME_MODE = "FRAME_MODE"
        private val defaultMode = FrameMode.Fast
    }
}
