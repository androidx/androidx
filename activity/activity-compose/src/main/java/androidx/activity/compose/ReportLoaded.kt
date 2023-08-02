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
package androidx.activity.compose

import android.app.Activity
import androidx.activity.FullyLoadedReporter
import androidx.activity.reportWhenComplete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.platform.LocalView

/**
 * Manages the composition callback for [ReportLoadedWhen].
 */
private class ReportLoadedComposition(
    private val fullyLoadedReporter: FullyLoadedReporter
) : () -> Unit {

    private val snapshotStateObserver = SnapshotStateObserver { command ->
        command()
    }.apply {
        start()
    }

    /**
     * Called whenever the values read in the lambda parameter has changed.
     */
    private val checkReporter: (() -> Boolean) -> Unit = ::observeReporter

    init {
        fullyLoadedReporter.addOnReportLoadedListener(this)
    }

    /**
     * Called when the [FullyLoadedReporter] has called [Activity.reportFullyDrawn]. This
     * stops watching for changes to the snapshot.
     */
    override fun invoke() {
        snapshotStateObserver.clear()
        snapshotStateObserver.stop()
        // We don't need this instance anymore
        fullyLoadedReporter.activity.window.decorView.setTag(R.id.report_loaded_composition, null)
     }

    /**
     * Tells the [FullyLoadedReporter] to wait until [removeReporter] is called before
     * calling [Activity.reportFullyDrawn].
     */
    fun addReporter(reporter: () -> Boolean) {
        if (!fullyLoadedReporter.hasReported) {
            fullyLoadedReporter.addReporter()
            observeReporter(reporter)
        }
    }

    /**
     * Tells the [FullyLoadedReporter] that one of the report conditions that caused
     * [addReporter] to be called has passed and that [Activity.reportFullyDrawn] can
     * be called if it is the last one to pass.
     */
    fun removeReporter(reporter: () -> Boolean) {
        if (!fullyLoadedReporter.hasReported) {
            snapshotStateObserver.clear(reporter)
            fullyLoadedReporter.removeReporter()
        }
    }

    private fun observeReporter(reporter: () -> Boolean) {
        var reporterPassed = false
        snapshotStateObserver.observeReads(reporter, checkReporter) {
            reporterPassed = reporter()
        }
        if (reporterPassed) {
            removeReporter(reporter)
        }
    }
}

/**
 * Adds [predicate] to the conditions that must be met prior to [Activity.reportFullyDrawn]
 * being called.
 *
 * The [Activity] used is extracted from the [LocalView]'s context.
 *
 * @sample androidx.activity.compose.samples.ReportLoadedWhenSample
 */
@Composable
fun ReportLoadedWhen(
    predicate: () -> Boolean
) {
    val view = LocalView.current
    DisposableEffect(view, predicate) {
        val fullyLoadedReporter = FullyLoadedReporter.findFullyLoadedReporter(view.context)
        if (fullyLoadedReporter == null || fullyLoadedReporter.hasReported) {
            onDispose {}
        } else {
            val decorView = fullyLoadedReporter.activity.window.decorView
            val compositionLoaded =
                decorView.getTag(R.id.report_loaded_composition) as? ReportLoadedComposition
                    ?: ReportLoadedComposition(fullyLoadedReporter).also {
                        decorView.setTag(R.id.report_loaded_composition, it)
                    }
            compositionLoaded.addReporter(predicate)
            onDispose {
                compositionLoaded.removeReporter(predicate)
            }
        }
    }
}

/**
 * Adds [block] to the methods that must complete prior to [Activity.reportFullyDrawn]
 * being called.
 *
 * The [Activity] used is extracted from the [LocalView]'s context.
 *
 * After [Activity.reportFullyDrawn] has been called, [block] will not be called again, even on
 * recomposition.
 *
 * @sample androidx.activity.compose.samples.ReportLoadedAfterSample
 */
@Composable
fun ReportLoadedAfter(
    block: suspend () -> Unit
) {
    val view = LocalView.current
    LaunchedEffect(block, view) {
        FullyLoadedReporter.findFullyLoadedReporter(view.context)?.reportWhenComplete(block)
    }
}
