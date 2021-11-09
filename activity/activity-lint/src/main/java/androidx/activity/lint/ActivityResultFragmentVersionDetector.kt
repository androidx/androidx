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

package androidx.activity.lint

import com.android.builder.model.AndroidLibrary
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.GradleContext
import com.android.tools.lint.detector.api.GradleScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Project
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import java.util.EnumSet
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class ActivityResultFragmentVersionDetector : Detector(), UastScanner, GradleScanner {
    companion object {
        const val FRAGMENT_VERSION = "1.3.0"

        val ISSUE = Issue.create(
            id = "InvalidFragmentVersionForActivityResult",
            briefDescription = "Update to Fragment $FRAGMENT_VERSION to use ActivityResult APIs",
            explanation = """In order to use the ActivityResult APIs you must upgrade your \
                Fragment version to $FRAGMENT_VERSION. Previous versions of FragmentActivity \
                failed to call super.onRequestPermissionsResult() and used invalid request codes""",
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                ActivityResultFragmentVersionDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.GRADLE_FILE),
                Scope.JAVA_FILE_SCOPE,
                Scope.GRADLE_SCOPE
            )
        ).addMoreInfo(
            "https://developer.android.com/training/permissions/requesting#make-the-request"
        )
    }

    var locations = ArrayList<Location>()
    lateinit var expression: UCallExpression

    private var checkedImplementationDependencies = false

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodIdentifier?.name != "registerForActivityResult") {
                    return
                }
                expression = node
                locations.add(context.getLocation(node))
            }
        }
    }

    override fun checkDslPropertyAssignment(
        context: GradleContext,
        property: String,
        value: String,
        parent: String,
        parentParent: String?,
        valueCookie: Any,
        statementCookie: Any
    ) {
        if (locations.isEmpty()) {
            return
        }
        if (property == "api") {
            // always check api dependencies
            reportIssue(value, context)
        } else if (!checkedImplementationDependencies) {
            val project = context.project
            if (useNewLintVersion(project)) {
                checkWithNewLintVersion(project, context)
            } else {
                checkWithOldLintVersion(project, context)
            }
        }
    }

    private fun useNewLintVersion(project: Project): Boolean {
        project::class.memberFunctions.forEach { function ->
            if (function.name == "getBuildVariant") {
                return true
            }
        }
        return false
    }

    private fun checkWithNewLintVersion(project: Project, context: GradleContext) {
        val buildVariant = callFunctionWithReflection(project, "getBuildVariant")
        val mainArtifact = getMemberWithReflection(buildVariant, "mainArtifact")
        val dependencies = getMemberWithReflection(mainArtifact, "dependencies")
        val all = callFunctionWithReflection(dependencies, "getAll")
        (all as ArrayList<*>).forEach { lmLibrary ->
            lmLibrary::class.memberProperties.forEach { libraryMembers ->
                if (libraryMembers.name == "resolvedCoordinates") {
                    libraryMembers.isAccessible = true
                    reportIssue(libraryMembers.call(lmLibrary).toString(), context, false)
                }
            }
        }
    }

    private fun checkWithOldLintVersion(project: Project, context: GradleContext) {
        lateinit var explicitLibraries: Collection<AndroidLibrary>
        val currentVariant = callFunctionWithReflection(project, "getCurrentVariant")
        val mainArtifact = callFunctionWithReflection(currentVariant, "getMainArtifact")
        val dependencies = callFunctionWithReflection(mainArtifact, "getDependencies")
        @Suppress("UNCHECKED_CAST")
        explicitLibraries =
            callFunctionWithReflection(dependencies, "getLibraries") as Collection<AndroidLibrary>

        // collect all of the library dependencies
        val allLibraries = HashSet<AndroidLibrary>()
        addIndirectAndroidLibraries(explicitLibraries, allLibraries)
        // check all of the dependencies
        allLibraries.forEach {
            val resolvedCoords = it.resolvedCoordinates
            val groupId = resolvedCoords.groupId
            val artifactId = resolvedCoords.artifactId
            val version = resolvedCoords.version
            reportIssue("$groupId:$artifactId:$version", context, false)
        }
    }

    private fun callFunctionWithReflection(caller: Any, functionName: String): Any {
        caller::class.memberFunctions.forEach { function ->
            if (function.name == functionName) {
                function.isAccessible = true
                return function.call(caller)!!
            }
        }
        return Unit
    }

    private fun getMemberWithReflection(caller: Any, memberName: String): Any {
        caller::class.memberProperties.forEach { member ->
            if (member.name == memberName) {
                member.getter.isAccessible = true
                return member.getter.call(caller)!!
            }
        }
        return Unit
    }

    private fun addIndirectAndroidLibraries(
        libraries: Collection<AndroidLibrary>,
        result: MutableSet<AndroidLibrary>
    ) {
        for (library in libraries) {
            if (!result.contains(library)) {
                result.add(library)
                addIndirectAndroidLibraries(library.libraryDependencies, result)
            }
        }
    }

    private fun reportIssue(
        value: String,
        context: GradleContext,
        removeQuotes: Boolean = true
    ) {
        val library = if (removeQuotes) {
            getStringLiteralValue(value)
        } else {
            value
        }

        if (library.isNotEmpty()) {
            val currentVersion = library.substringAfter("androidx.fragment:fragment:")
                .substringBeforeLast("-")
            if (library != currentVersion && currentVersion < FRAGMENT_VERSION) {
                locations.forEach { location ->
                    context.report(
                        ISSUE, expression, location,
                        "Upgrade Fragment version to at least $FRAGMENT_VERSION."
                    )
                }
            }
        }
    }

    /**
     * Extracts the string value from the DSL value by removing surrounding quotes.
     *
     * Returns an empty string if [value] is not a string literal.
     */
    private fun getStringLiteralValue(value: String): String {
        if (value.length > 2 && (
            value.startsWith("'") && value.endsWith("'") ||
                value.startsWith("\"") && value.endsWith("\"")
            )
        ) {
            return value.substring(1, value.length - 1)
        }
        return ""
    }
}
