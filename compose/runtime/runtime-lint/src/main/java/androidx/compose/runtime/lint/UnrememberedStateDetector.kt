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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.isNotRemembered
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

/**
 * [Detector] that checks `derivedStateOf`, `mutableStateOf`, `mutableStateListOf`, and
 * `mutableStateMapOf` calls to make sure that if they are called inside a Composable body, they are
 * `remember`ed.
 */
class UnrememberedStateDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.isStateFactoryInvocation() && node.isNotRemembered()) {
                    context.report(
                        UnrememberedState,
                        node,
                        context.getNameLocation(node),
                        "Creating a state object during composition without using `remember`"
                    )
                }
            }
        }
    }

    private fun UCallExpression.isStateFactoryInvocation(): Boolean =
        resolve()?.annotations?.any { it.hasQualifiedName(FqStateFactoryAnnotationName) } ?: false

    companion object {
        private const val FqStateFactoryAnnotationName =
            "androidx.compose.runtime.snapshots.StateFactoryMarker"

        val UnrememberedState =
            Issue.create(
                "UnrememberedMutableState", // Left as previous id for backwards compatibility
                "Creating a state object during composition without using `remember`",
                "State objects created during composition need to be `remember`ed, otherwise " +
                    "they will be recreated during recomposition, and lose their state. Either hoist " +
                    "the state to an object that is not created during composition, or wrap the " +
                    "state in a call to `remember`.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    UnrememberedStateDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
