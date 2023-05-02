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

package androidx.benchmark.macro

import androidx.benchmark.DeviceInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BaselineProfilesTest {
    @Test
    fun filterBaselineProfileRules() {
        val profile = """
            Landroidx/Foo/Bar;
            [Landroidx/Foo/Bar;
            HSPLjava/io/DataOutputStream;->writeByte(I)V+]Ljava/io/OutputStream;missing_types
        """.trimIndent()

        val filtered = filterProfileRulesToTargetP(profile, sortRules = false) { true }
        assertEquals(filtered.lines().size, 1)
        assertEquals("Landroidx/Foo/Bar;", filtered)
    }

    @Test
    fun filterBaselineRulesWithSorting() {
        val profile = """
            HPLandroidx/lifecycle/Lifecycle${dollar}Event;->downFrom(Landroidx/lifecycle/Lifecycle${dollar}State;)Landroidx/lifecycle/Lifecycle${dollar}Event;
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;-><init>(Ljava/util/Map;)V
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;->invokeCallbacks(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Lifecycle${dollar}Event;Ljava/lang/Object;)V
        """.trimIndent()
        val sorted = filterProfileRulesToTargetP(profile, sortRules = true) { true }
        assertEquals(sorted.lines().size, 3)
        val expected = """
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;-><init>(Ljava/util/Map;)V
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;->invokeCallbacks(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Lifecycle${dollar}Event;Ljava/lang/Object;)V
            HPLandroidx/lifecycle/Lifecycle${dollar}Event;->downFrom(Landroidx/lifecycle/Lifecycle${dollar}State;)Landroidx/lifecycle/Lifecycle${dollar}Event;
        """.trimIndent()
        assertEquals(expected, sorted)
    }

    @Test
    fun filterBaselineRulesWithSortingAndCustomFilter() {
        val profile = """
            HPLandroidx/lifecycle/Lifecycle${dollar}Event;->downFrom(Landroidx/lifecycle/Lifecycle${dollar}State;)Landroidx/lifecycle/Lifecycle${dollar}Event;
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;->invokeCallbacks(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Lifecycle${dollar}Event;Ljava/lang/Object;)V
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;-><init>(Ljava/util/Map;)V
        """.trimIndent()
        val sorted = filterProfileRulesToTargetP(profile, sortRules = true) {
            it.startsWith("HSPL")
        }
        assertEquals(sorted.lines().size, 2)
        val expected = """
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;-><init>(Ljava/util/Map;)V
            HSPLandroidx/lifecycle/ClassesInfoCache${dollar}CallbackInfo;->invokeCallbacks(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Lifecycle${dollar}Event;Ljava/lang/Object;)V
        """.trimIndent()
        assertEquals(expected, sorted)
    }

    @Test
    fun deviceSpecifier() {
        if (DeviceInfo.isEmulator) {
            assertEquals(deviceSpecifier, "-e ")
        } else {
            assertTrue(deviceSpecifier.startsWith("-s "), "observed $deviceSpecifier")
            assertTrue(deviceSpecifier.endsWith(" "), "observed $deviceSpecifier")
            assertNotEquals(deviceSpecifier, "-s  ")
        }
    }
    companion object {
        // https://youtrack.jetbrains.com/issue/KT-2425
        const val dollar = "$"
    }
}
