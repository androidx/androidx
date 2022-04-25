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

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.util.Locale

/**
 * Annotation for specifying a per-test or per-method override of the default locale. Multiple
 * values may be passed to specify a locale list.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class WithRtl(val value: Boolean = true)

/** [TestRule] for using [WithRtl] on a test class or method. */
internal object WithRtlRule : TestRule {
    override fun apply(base: Statement, description: Description) =
        object : Statement() {
            override fun evaluate() {
                val override =
                    description.testMethod.isRtl ?: description.testClass.isRtl ?: false
                forceRtl = override
                val savedLocale = Locale.getDefault()
                val locale = if (override) Locale("he") else Locale.US
                Locale.setDefault(locale)
                try {
                    base.evaluate()
                } finally {
                    Locale.setDefault(savedLocale)
                    forceRtl = null
                }
            }
        }

    private val AnnotatedElement.isRtl: Boolean?
        get() = getAnnotation(WithRtl::class.java)?.value
}

private val Description.testMethod: Method
    get() = testClass.getMethod(methodName)
