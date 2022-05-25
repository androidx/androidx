
/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TrivialTestModuleTest {
    @Test
    fun targetPackage() {
        // if self-instrumenting wasn't used, this would instrument the target app,
        // and we'd see TargetPackage instead
        assertEquals(
            TestPackage,
            InstrumentationRegistry.getInstrumentation().targetContext.packageName
        )
    }

    @Test
    fun testPackage() {
        assertEquals(
            TestPackage,
            InstrumentationRegistry.getInstrumentation().context.packageName
        )
    }

    @Ignore // b/202321897
    @Test
    @Suppress("DEPRECATION")
    fun targetPackageInstalled() {
        val pm = InstrumentationRegistry.getInstrumentation().context.packageManager
        try {
            pm.getApplicationInfo(TargetPackage, 0)
        } catch (notFoundException: PackageManager.NameNotFoundException) {
            throw AssertionError(
                "Unable to find target package $TargetPackage, is it installed?",
                notFoundException
            )
        }
    }

    companion object {
        const val TargetPackage = "androidx.benchmark.integration.macrobenchmark.target"
        const val TestPackage = "androidx.benchmark.integration.testmodulesample"
    }
}
