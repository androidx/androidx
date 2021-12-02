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

package androidx.glance.appwidget

import android.content.res.Configuration
import android.os.Build
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

/**
 * Annotation for specifying a per-test or per-method override of the device night mode
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class WithNightMode(val value: Boolean = true)

/** [TestRule] for using [WithNightMode] on a test class or method. */
internal object WithNightModeRule : TestRule {

    override fun apply(base: Statement, description: Description) =
        object : Statement() {
            override fun evaluate() {
                if (Build.VERSION.SDK_INT < 29) {
                    // Night mode is only usable on API 29+.
                    base.evaluate()
                    return
                }

                val isTestNightMode =
                    description.testMethod.isNightMode ?: description.testClass.isNightMode ?: false
                val isDeviceNightMode =
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES
                var previousNightMode: String? = null
                if (isTestNightMode != isDeviceNightMode) {
                    // The night mode status is of the form "Night mode: <mode>"
                    previousNightMode = runShellCommand("cmd uimode night").substring(12)
                    val newNightMode = if (isTestNightMode) "yes" else "no"
                    runShellCommand("cmd uimode night $newNightMode")
                }
                try {
                    base.evaluate()
                } finally {
                    if (previousNightMode != null) {
                        runShellCommand("cmd uimode night $previousNightMode")
                    }
                }
            }
        }

    private val AnnotatedElement.isNightMode: Boolean?
        get() = getAnnotation(WithNightMode::class.java)?.value
}

private val Description.testMethod: Method
    get() = testClass.getMethod(methodName)