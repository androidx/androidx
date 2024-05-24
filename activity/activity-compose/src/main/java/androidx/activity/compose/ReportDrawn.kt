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
import androidx.activity.FullyDrawnReporter
import androidx.activity.FullyDrawnReporterOwner
import androidx.activity.findViewTreeFullyDrawnReporterOwner
import androidx.activity.reportWhenComplete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** Manages the composition callback for [ReportDrawnWhen]. */
private class ReportDrawnComposition(
    private val fullyDrawnReporter: FullyDrawnReporter,
    private val predicate: () -> Boolean
) : () -> Unit {

    private val snapshotStateObserver =
        SnapshotStateObserver { command -> command() }.apply { start() }

    /** Called whenever the values read in the lambda parameter has changed. */
    private val checkReporter: (() -> Boolean) -> Unit = ::observeReporter

    init {
        fullyDrawnReporter.addOnReportDrawnListener(this)
        if (!fullyDrawnReporter.isFullyDrawnReported) {
            fullyDrawnReporter.addReporter()
            observeReporter(predicate)
        }
    }

    /**
     * Called when the [FullyDrawnReporter] has called [Activity.reportFullyDrawn]. This stops
     * watching for changes to the snapshot.
     */
    override fun invoke() {
        snapshotStateObserver.clear()
        snapshotStateObserver.stop()
    }

    /** Stops observing [predicate] and marks the [fullyDrawnReporter] as ready for it. */
    fun removeReporter() {
        snapshotStateObserver.clear(predicate)
        if (!fullyDrawnReporter.isFullyDrawnReported) {
            fullyDrawnReporter.removeReporter()
        }
        this.invoke() // stop the snapshotStateObserver.
    }

    private fun observeReporter(predicate: () -> Boolean) {
        var reporterPassed = false
        snapshotStateObserver.observeReads(predicate, checkReporter) {
            reporterPassed = predicate()
        }
        if (reporterPassed) {
            removeReporter()
        }
    }
}

/**
 * Provides a [FullyDrawnReporterOwner] that can be used by Composables hosted in a
 * [androidx.activity.ComponentActivity].
 */
object LocalFullyDrawnReporterOwner {
    private val LocalFullyDrawnReporterOwner = compositionLocalOf<FullyDrawnReporterOwner?> { null }

    /**
     * Returns current composition local value for the owner or `null` if one has not been provided,
     * one has not been set via [androidx.activity.setViewTreeFullyDrawnReporterOwner], nor is one
     * available by looking at the [LocalContext].
     */
    val current: FullyDrawnReporterOwner?
        @Composable
        get() =
            LocalFullyDrawnReporterOwner.current
                ?: LocalView.current.findViewTreeFullyDrawnReporterOwner()
                ?: findOwner<FullyDrawnReporterOwner>(LocalContext.current)

    /** Associates a [LocalFullyDrawnReporterOwner] key to a value. */
    infix fun provides(
        fullyDrawnReporterOwner: FullyDrawnReporterOwner
    ): ProvidedValue<FullyDrawnReporterOwner?> {
        return LocalFullyDrawnReporterOwner.provides(fullyDrawnReporterOwner)
    }
}

/**
 * Adds [predicate] to the conditions that must be met prior to [Activity.reportFullyDrawn] being
 * called.
 *
 * The [Activity] used is extracted from the [LocalView]'s context.
 *
 * @sample androidx.activity.compose.samples.ReportDrawnWhenSample
 */
@Composable
fun ReportDrawnWhen(predicate: () -> Boolean) {
    val fullyDrawnReporter = LocalFullyDrawnReporterOwner.current?.fullyDrawnReporter ?: return
    DisposableEffect(fullyDrawnReporter, predicate) {
        if (fullyDrawnReporter.isFullyDrawnReported) {
            onDispose {}
        } else {
            val compositionDrawn = ReportDrawnComposition(fullyDrawnReporter, predicate)
            onDispose { compositionDrawn.removeReporter() }
        }
    }
}

/**
 * Calls [Activity.reportFullyDrawn] after this composition is completed.
 *
 * The [Activity] used is extracted from the [LocalView]'s context.
 *
 * @sample androidx.activity.compose.samples.ReportDrawnSample
 */
@Composable fun ReportDrawn() = ReportDrawnWhen { true }

/**
 * Adds [block] to the methods that must complete prior to [Activity.reportFullyDrawn] being called.
 *
 * The [Activity] used is extracted from the [LocalView]'s context.
 *
 * After [Activity.reportFullyDrawn] has been called, [block] will not be called again, even on
 * recomposition.
 *
 * @sample androidx.activity.compose.samples.ReportDrawnAfterSample
 */
@Composable
fun ReportDrawnAfter(block: suspend () -> Unit) {
    val fullyDrawnReporter = LocalFullyDrawnReporterOwner.current?.fullyDrawnReporter ?: return
    LaunchedEffect(block, fullyDrawnReporter) { fullyDrawnReporter.reportWhenComplete(block) }
}
