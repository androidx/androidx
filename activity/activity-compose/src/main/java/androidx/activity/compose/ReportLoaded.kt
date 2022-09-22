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
import androidx.activity.FullyLoadedReporterOwner
import androidx.activity.findViewTreeFullyLoadedReporterOwner
import androidx.activity.reportWhenComplete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Manages the composition callback for [ReportLoadedWhen].
 */
private class ReportLoadedComposition(
    private val fullyLoadedReporter: FullyLoadedReporter,
    private val predicate: () -> Boolean
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
        if (!fullyLoadedReporter.isFullyDrawnReported) {
            fullyLoadedReporter.addReporter()
            observeReporter(predicate)
        }
    }

    /**
     * Called when the [FullyLoadedReporter] has called [Activity.reportFullyDrawn]. This
     * stops watching for changes to the snapshot.
     */
    override fun invoke() {
        snapshotStateObserver.clear()
        snapshotStateObserver.stop()
     }

    /**
     * Stops observing [predicate] and marks the [fullyLoadedReporter] as ready for it.
     */
    fun removeReporter() {
        snapshotStateObserver.clear(predicate)
        if (!fullyLoadedReporter.isFullyDrawnReported) {
            fullyLoadedReporter.removeReporter()
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
 * Provides a [FullyLoadedReporterOwner] that can be used by Composables hosted in a
 * [androidx.activity.ComponentActivity].
 */
object LocalFullyLoadedReporterOwner {
    private val LocalFullyLoadedReporterOwner =
        compositionLocalOf<FullyLoadedReporterOwner?> { null }

    /**
     * Returns current composition local value for the owner or `null` if one has not
     * been provided, one has not been set via
     * [androidx.activity.setViewTreeFullyLoadedReporterOwner], nor is one available by
     * looking at the [LocalContext].
     */
    val current: FullyLoadedReporterOwner?
        @Composable
        get() = LocalFullyLoadedReporterOwner.current
            ?: LocalView.current.findViewTreeFullyLoadedReporterOwner()
            ?: findOwner<FullyLoadedReporterOwner>(LocalContext.current)

    /**
     * Associates a [LocalFullyLoadedReporterOwner] key to a value.
     */
    infix fun provides(fullyLoadedReporterOwner: FullyLoadedReporterOwner):
        ProvidedValue<FullyLoadedReporterOwner?> {
        return LocalFullyLoadedReporterOwner.provides(fullyLoadedReporterOwner)
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
    val fullyLoadedReporter =
        LocalFullyLoadedReporterOwner.current?.fullyLoadedReporter ?: return
    DisposableEffect(fullyLoadedReporter, predicate) {
        if (fullyLoadedReporter.isFullyDrawnReported) {
            onDispose {}
        } else {
            val compositionLoaded = ReportLoadedComposition(fullyLoadedReporter, predicate)
            onDispose {
                compositionLoaded.removeReporter()
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
    val fullyLoadedReporter =
        LocalFullyLoadedReporterOwner.current?.fullyLoadedReporter ?: return
    LaunchedEffect(block, fullyLoadedReporter) {
        fullyLoadedReporter.reportWhenComplete(block)
    }
}
