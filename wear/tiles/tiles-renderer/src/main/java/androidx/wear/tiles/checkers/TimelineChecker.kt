/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.checkers

import android.util.Log
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import kotlin.jvm.Throws

/**
 * Exception thrown when a TimelineEntryChecker fails.
 */
internal class CheckerException(message: String) : Exception(message)

/**
 * Checker for a Tile's TimelineEntries. Instances of this interface should check for a certain
 * condition on the given [TimelineEntry], and throw an instance of [CheckerException] if there
 * is a problem with that [TimelineEntry].
 */
internal interface TimelineEntryChecker {
    /** The name of this TimelineEntryChecker. This will be printed in any error output. */
    val name: String

    /**
     * Check a given [TimelineEntry].
     *
     * @throws CheckerException if there was an issue while checking the [TimelineEntry]
     */
    @Throws(CheckerException::class)
    fun check(entry: TimelineEntry)
}

/**
 * Checker for a given [Timeline]. This will run all provided [TimelineEntryChecker]s on the
 * given [Timeline], and if any fail, log an error to logcat.
 *
 * @param entryCheckers The list of checkers to use. Defaults to all built in checks.
 */
internal class TimelineChecker(
    private val entryCheckers: List<TimelineEntryChecker> = listOf(CheckAccessibilityAvailable()),
) {
    companion object {
        private const val TAG = "TileChecker"
    }

    /** Check a given [Timeline] against all registered [TimelineEntryChecker]s. */
    public fun doCheck(timeline: Timeline) {
        timeline.timelineEntries.forEach { entry ->
            entryCheckers.forEach {
                try {
                    it.check(entry)
                } catch (ex: CheckerException) {
                    Log.e(TAG, "${it.name} checker failed for tile.", ex)
                }
            }
        }
    }
}
