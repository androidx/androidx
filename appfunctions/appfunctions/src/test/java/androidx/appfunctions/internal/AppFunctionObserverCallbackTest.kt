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

package androidx.appfunctions.internal

import android.os.Build
import androidx.appfunctions.AppFunctionName
import androidx.appfunctions.AppFunctionsChangeEvent
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_RUNTIME_DATABASE_NAME
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_RUNTIME_NAMESPACE
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_STATIC_DATABASE_NAME
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.SYSTEM_PACKAGE_NAME
import androidx.appsearch.observer.DocumentChangeInfo
import androidx.appsearch.observer.SchemaChangeInfo
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class AppFunctionObserverCallbackTest {

    private val observer = AppFunctionObserverCallback(debounceDuration = TEST_DEBOUNCE_DURATION)
    private val events = mutableListOf<AppFunctionsChangeEvent>()

    @After
    fun tearDown() {
        observer.close()
    }

    @Test
    fun onPackageRemoved_knownSeededPackage_emitsMetadataChangedOnce() = runTest {
        startCollecting()
        observer.seedKnownPackages(setOf(PACKAGE_1))

        observer.onPackageRemoved(PACKAGE_1)
        advancePastDebounce()
        assertThat(events)
            .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))

        // Removing the same package again without re-indexing should not emit another event.
        observer.onPackageRemoved(PACKAGE_1)
        advancePastDebounce()
        assertThat(events).hasSize(1)
    }

    @Test
    fun onPackageRemoved_unknownPackage_doesNotEmitMetadataChanged() = runTest {
        startCollecting()
        observer.seedKnownPackages(setOf(PACKAGE_1))

        observer.onPackageRemoved(PACKAGE_2)
        advancePastDebounce()

        assertThat(events).isEmpty()
    }

    @Test
    fun onPackageRemoved_afterSchemaChangeAddsPackage_emitsMetadataChanged() = runTest {
        startCollecting()
        observer.seedKnownPackages(emptySet())

        observer.onSchemaChanged(staticSchemaChange(PACKAGE_1))
        advancePastDebounce()
        assertThat(events)
            .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))
        events.clear()

        observer.onPackageRemoved(PACKAGE_1)
        advancePastDebounce()
        assertThat(events)
            .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))
    }

    @Test
    fun onPackageRemoved_afterRuntimeDocumentChangeAddsPackage_emitsMetadataChanged() = runTest {
        startCollecting()
        observer.seedKnownPackages(emptySet())

        observer.onDocumentChanged(
            DocumentChangeInfo(
                SYSTEM_PACKAGE_NAME,
                APP_FUNCTIONS_RUNTIME_DATABASE_NAME,
                APP_FUNCTIONS_RUNTIME_NAMESPACE,
                "AppFunctionRuntimeMetadata-$PACKAGE_1",
                setOf("$PACKAGE_1/func1"),
            )
        )
        advancePastDebounce()
        assertThat(events)
            .containsExactly(
                AppFunctionsChangeEvent.StatesChanged(setOf(AppFunctionName(PACKAGE_1, "func1")))
            )
        events.clear()

        observer.onPackageRemoved(PACKAGE_1)
        advancePastDebounce()
        assertThat(events)
            .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))
    }

    @Test
    fun seedKnownPackages_packageIndexedBeforeSeed_isNotDroppedBySeed() = runTest {
        startCollecting()
        // Package indexed after observer registration but before the snapshot query returned,
        // and missing from the (stale) snapshot.
        observer.onSchemaChanged(staticSchemaChange(PACKAGE_1))
        observer.seedKnownPackages(emptySet())
        advancePastDebounce()
        events.clear()

        observer.onPackageRemoved(PACKAGE_1)
        advancePastDebounce()

        assertThat(events)
            .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))
    }

    @Test
    fun seedKnownPackages_packageRemovedBeforeSeed_emitsMetadataChangedAndExcludesFromSeed() =
        runTest {
            startCollecting()
            // Package uninstalled after observer registration, but still present in the (stale)
            // snapshot.
            observer.onPackageRemoved(PACKAGE_1)
            observer.seedKnownPackages(setOf(PACKAGE_1, PACKAGE_2))
            advancePastDebounce()

            assertThat(events)
                .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))
            events.clear()

            observer.onPackageRemoved(PACKAGE_1)
            observer.onPackageRemoved(PACKAGE_2)
            advancePastDebounce()

            assertThat(events)
                .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_2)))
        }

    @Test
    fun seedKnownPackages_unknownPackageRemovedBeforeSeed_doesNotEmitMetadataChanged() = runTest {
        startCollecting()
        observer.onPackageRemoved(PACKAGE_1)
        observer.seedKnownPackages(setOf(PACKAGE_2))
        advancePastDebounce()

        assertThat(events).isEmpty()
    }

    @Test
    fun seedKnownPackages_packageRemovedThenReindexedBeforeSeed_remainsKnown() = runTest {
        startCollecting()
        observer.onPackageRemoved(PACKAGE_1)
        observer.onSchemaChanged(staticSchemaChange(PACKAGE_1))
        advancePastDebounce()
        assertThat(events)
            .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))
        events.clear()

        observer.seedKnownPackages(setOf(PACKAGE_1))
        advancePastDebounce()
        assertThat(events).isEmpty()

        observer.onPackageRemoved(PACKAGE_1)
        advancePastDebounce()

        assertThat(events)
            .containsExactly(AppFunctionsChangeEvent.MetadataChanged(setOf(PACKAGE_1)))
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch { observer.observe().collect { events.add(it) } }
        runCurrent()
    }

    private fun TestScope.advancePastDebounce() {
        advanceTimeBy(TEST_DEBOUNCE_DURATION + 10.milliseconds)
    }

    private fun staticSchemaChange(packageName: String) =
        SchemaChangeInfo(
            SYSTEM_PACKAGE_NAME,
            APP_FUNCTIONS_STATIC_DATABASE_NAME,
            setOf("AppFunctionStaticMetadata-$packageName"),
        )

    private companion object {
        private val TEST_DEBOUNCE_DURATION = 100.milliseconds
        private const val PACKAGE_1 = "com.example.pkg1"
        private const val PACKAGE_2 = "com.example.pkg2"
    }
}
