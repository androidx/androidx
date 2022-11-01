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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LocationType
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiJvmModifiersOwner
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod

/**
 * This lint check is meant to help maintain binary compatibility in a one-time transition to using
 * `-Xjvm-default=all`. Applicable interfaces which existed before `-Xjvm-default=all` was used must
 * be annotated with @JvmDefaultWithCompatibility. However, after the initial change, new interfaces
 * should not use @JvmDefaultWithCompatibility.
 *
 * Because this check is only meant to be used once, it should not be added to the issue registry.
 */
class MissingJvmDefaultWithCompatibilityDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return InterfaceChecker(context)
    }

    private inner class InterfaceChecker(val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) {
            if (!isKotlin(node)) return
            if (!node.isInterface) return
            if (node.annotatedWithAnyOf(
                    // If the interface is not stable, it doesn't need the annotation
                    BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS +
                        // If the interface already has the annotation, it doesn't need it again
                        JVM_DEFAULT_WITH_COMPATIBILITY)
            ) return

            val stableMethods = node.stableMethods()
            if (stableMethods.any { it.hasDefaultImplementation() }) {
                val reason = "This interface must be annotated with @JvmDefaultWithCompatibility " +
                    "because it has a stable method with a default implementation"
                reportIncident(node, reason)
                return
            }

            if (stableMethods.any { it.hasParameterWithDefaultValue() }) {
                val reason = "This interface must be annotated with @JvmDefaultWithCompatibility " +
                    "because it has a stable method with a parameter with a default value"
                reportIncident(node, reason)
                return
            }

            // This only checks the interfaces that this interface directly extends, which means if
            // A extends B extends C and C is @JvmDefaultWithCompatibility, there will need to be
            // two passes of running the check to annotate A and B.
            if (node.interfaces.any {
                    it.annotatedWithAnyOf(listOf(JVM_DEFAULT_WITH_COMPATIBILITY))
            }) {
                val reason = "This interface must be annotated with @JvmDefaultWithCompatibility " +
                    "because it implements an interface which uses this annotation"
                reportIncident(node, reason)
                return
            }
        }

        private fun reportIncident(node: UClass, reason: String) {
            val location = context.getLocation(node, LocationType.ALL)
            val fix = fix()
                .name("Annotate with @JvmDefaultWithCompatibility")
                .annotate(JVM_DEFAULT_WITH_COMPATIBILITY)
                .range(location)
                .autoFix()
                .build()

            val incident = Incident(context)
                .fix(fix)
                .issue(ISSUE)
                .location(location)
                .message(reason)
                .scope(node)

            context.report(incident)
        }

        /**
         * Returns a list of the class's stable methods (methods not labelled as experimental).
         */
        private fun UClass.stableMethods(): List<UMethod> =
            methods.filter {
                !it.annotatedWithAnyOf(BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS)
            }

        /**
         * Checks if the element is annotated with any of the provided (fully qualified) annotation
         * names. This uses `PsiJvmModifiersOwner` because it seems to be the one common parent of
         * `UClass` and `UMethod` with an `annotations` property.
         */
        private fun PsiJvmModifiersOwner.annotatedWithAnyOf(
            qualifiedAnnotationNames: List<String>
        ): Boolean = annotations.any { qualifiedAnnotationNames.contains(it.qualifiedName) }

        private fun UMethod.hasDefaultImplementation(): Boolean =
            uastBody != null

        private fun UMethod.hasParameterWithDefaultValue(): Boolean =
            uastParameters.any { param -> param.uastInitializer != null }
    }

    companion object {
        val ISSUE = Issue.create(
            "MissingJvmDefaultWithCompatibility",
            "The @JvmDefaultWithCompatibility needs to be used with on applicable " +
                "interfaces when `-Xjvm-default=all` is turned on to preserve compatibility.",
            "Libraries that pass `-Xjvm-default=all` to the Kotlin compiler must " +
                "use the @JvmDefaultWithCompatibility annotation on previously existing " +
                "interfaces with stable methods with default implementations or default parameter" +
                " values, and interfaces that extend other @JvmDefaultWithCompatibility " +
                "interfaces. See go/androidx-api-guidelines#kotlin-jvm-default for more details.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                MissingJvmDefaultWithCompatibilityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        const val JVM_DEFAULT_WITH_COMPATIBILITY = "kotlin.jvm.JvmDefaultWithCompatibility"
    }
}