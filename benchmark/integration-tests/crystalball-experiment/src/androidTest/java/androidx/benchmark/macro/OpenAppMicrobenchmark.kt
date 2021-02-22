/*
 * Copyright 2019 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.platform.test.microbenchmark.Microbenchmark
import android.platform.test.rule.CompilationFilterRule
import android.platform.test.rule.DropCachesRule
import android.platform.test.rule.KillAppsRule
import android.platform.test.rule.NaturalOrientationRule
import android.platform.test.rule.PressHomeRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@SuppressLint("UnsupportedTestRunner")
@RunWith(Microbenchmark::class)
class OpenAppMicrobenchmark {

    @Test
    @LargeTest
    @Ignore("Gradle arguments are not passed to the TestRunner")
    fun open() {
        // Launch the app
        val context: Context = InstrumentationRegistry.getInstrumentation().context
        val intent: Intent = context.packageManager.getLaunchIntentForPackage(thePackage)!!
        // Clear out any previous instances
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        device.wait(
            Until.hasObject(By.pkg(thePackage).depth(0)),
            5000
        )
    }

    // Method-level rules
    @get:Rule
    var rules: RuleChain = RuleChain.outerRule(KillAppsRule(thePackage))
        .around(DropCachesRule())
        .around(CompilationFilterRule(thePackage))
        .around(PressHomeRule())

    companion object {
        // Pixel Settings App
        const val thePackage = "com.android.settings"
        private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        @JvmStatic
        @AfterClass
        fun close() {
            device.pressHome()
        }

        // Class-level rules
        @JvmField
        @ClassRule
        var orientationRule = NaturalOrientationRule()
    }
}
