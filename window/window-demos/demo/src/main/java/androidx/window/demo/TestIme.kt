/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.demo

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.window.demo.common.infolog.InfoLogAdapter
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator

/**
 * A test IME that currently provides a minimal UI containing a "Close" button. To use this, go to
 * "Settings > System > Languages & Input > On-screen keyboard" and enable "Test IME". Remember you
 * may still need to switch to this IME after the default on-screen keyboard pops up.
 */
internal class TestIme : InputMethodService() {

    private val adapter = InfoLogAdapter()

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.test_ime, null).apply {
            findViewById<RecyclerView>(R.id.recycler_view).adapter = adapter

            findViewById<Button>(R.id.button_clear).setOnClickListener {
                adapter.clear()
                adapter.notifyDataSetChanged()
            }

            findViewById<Button>(R.id.button_close).setOnClickListener {
                requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS)
            }

            displayCurrentWindowMetrics()
            displayMaximumWindowMetrics()
        }
    }

    private fun displayCurrentWindowMetrics() {
        val windowMetrics =
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this@TestIme)
        displayWindowMetrics("CurrentWindowMetrics update", windowMetrics)
    }

    private fun displayMaximumWindowMetrics() {
        val windowMetrics =
            WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(this@TestIme)
        displayWindowMetrics("MaximumWindowMetrics update", windowMetrics)
    }

    private fun displayWindowMetrics(title: String, windowMetrics: WindowMetrics) {

        val width = windowMetrics.bounds.width()
        val height = windowMetrics.bounds.height()

        val logBuilder =
            StringBuilder()
                .append(
                    "Width: $width, Height: $height\n" +
                        "Top: ${windowMetrics.bounds.top}, Bottom: ${windowMetrics.bounds.bottom}, " +
                        "Left: ${windowMetrics.bounds.left}, Right: ${windowMetrics.bounds.right}\n" +
                        "Density: ${windowMetrics.density}"
                )

        adapter.append(title, logBuilder.toString())
        adapter.notifyDataSetChanged()
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }
}
