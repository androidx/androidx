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

package androidx.wear.watchface.complications.data

import android.app.Instrumentation
import android.content.pm.PackageManager
import androidx.test.filters.AbstractFilter
import androidx.test.filters.CustomFilter
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description

/**
 * Marks a test that should only run on a watch. Note that custom filters are not applied to
 * classes, so each test method must be annotated.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@CustomFilter(filterClass = RequiresWatchFilter::class)
internal annotation class RequiresWatch

internal class RequiresWatchFilter(
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
) : AbstractFilter() {
    override fun evaluateTest(description: Description?): Boolean =
        instrumentation.context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

    override fun describe(): String = "Skips watch-only tests on non-watch devices."
}
