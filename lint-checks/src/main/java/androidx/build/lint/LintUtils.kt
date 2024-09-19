/*
 * Copyright 2023 The Android Open Source Project
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

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.model.DefaultLintModelAndroidLibrary
import com.android.tools.lint.model.DefaultLintModelJavaLibrary
import com.android.tools.lint.model.LintModelLibrary
import com.android.tools.lint.model.LintModelMavenName
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/*
 * See ag/25264517. This was previously a public extension function that is now private.
 * It's currently used by [BanRestrictToTestsScope] and [BanVisibleForTestingParams].
 */
internal fun PsiElement.getFqName(): String? =
    when (val element = namedUnwrappedElement) {
        is PsiMember ->
            element.getName()?.let { name ->
                val prefix = element.containingClass?.qualifiedName
                (if (prefix != null) "$prefix.$name" else name)
            }
        is KtNamedDeclaration -> element.fqName.toString()
        else -> null
    }

/** Attempts to find the Maven coordinate for the library containing [member]. */
internal fun JavaContext.findMavenCoordinate(member: PsiMember): LintModelMavenName? {
    val mavenName =
        evaluator.getLibrary(member) ?: evaluator.getProject(member)?.mavenCoordinate ?: return null

    // If the lint model is missing a Maven coordinate for this class, try to infer one from the
    // JAR's owner library. If we fail, return the broken Maven name anyway.
    if (mavenName == LintModelMavenName.NONE) {
        return evaluator
            .findJarPath(member)
            ?.let { jarPath ->
                evaluator.findOwnerLibrary(jarPath.replace('/', File.separatorChar))
            }
            ?.getMavenNameFromIdentifier() ?: mavenName
    }

    // If the lint model says the class lives in a "local AAR", try a little bit harder to match
    // that to an artifact in a real library based on build directory containment.
    if (mavenName.groupId == "__local_aars__") {
        val artifactPath = mavenName.artifactId

        // The artifact is being repackaged within this project. Assume that means it's in the same
        // Maven group.
        if (artifactPath.startsWith(project.buildModule.buildFolder.path)) {
            return project.mavenCoordinate
        }

        val lastIndexOfBuild = artifactPath.lastIndexOf("/build/")
        if (lastIndexOfBuild < 0) return null

        // Otherwise, try to find a dependency with a matching path and use its Maven group.
        val path = artifactPath.substring(0, lastIndexOfBuild)
        return evaluator.dependencies?.getAll()?.findMavenNameWithJarFileInPath(path, mavenName)
            ?: mavenName
    }

    return mavenName
}

/**
 * Attempts to find the Maven name for the library with at least one JAR file matching the [path].
 */
internal fun List<LintModelLibrary>.findMavenNameWithJarFileInPath(
    path: String,
    excludeMavenName: LintModelMavenName? = null
): LintModelMavenName? {
    return firstNotNullOfOrNull { library ->
        val resolvedCoordinates =
            when {
                library is DefaultLintModelJavaLibrary -> library.resolvedCoordinates
                library is DefaultLintModelAndroidLibrary -> library.resolvedCoordinates
                else -> null
            }

        if (resolvedCoordinates == null || resolvedCoordinates == excludeMavenName) {
            return@firstNotNullOfOrNull null
        }

        val hasMatchingJarFile =
            when {
                library == excludeMavenName -> emptyList()
                library is DefaultLintModelJavaLibrary -> library.jarFiles
                library is DefaultLintModelAndroidLibrary -> library.jarFiles
                else -> emptyList()
            }.any { jarFile -> jarFile.path.startsWith(path) }

        if (hasMatchingJarFile) {
            return@firstNotNullOfOrNull resolvedCoordinates
        }

        return@firstNotNullOfOrNull null
    }
}
