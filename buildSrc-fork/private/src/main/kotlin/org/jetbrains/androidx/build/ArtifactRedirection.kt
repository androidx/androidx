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

package org.jetbrains.androidx.build

import androidx.build.AndroidXMultiplatformExtension
import androidx.build.lazyReadFile
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.tomlj.Toml
import org.tomlj.TomlTable

/**
 * Loads the artifact-redirection version registry from `redirectversions.toml` (repo root) once per
 * build. The `[versions]` table maps a redirect-coordinate group prefix (e.g. `androidx.compose`) to
 * the `androidx.*` version the redirect points at.
 */
abstract class RedirectVersionsService : BuildService<RedirectVersionsService.Parameters> {
    interface Parameters : BuildServiceParameters {
        var tomlFileName: String
        var tomlFileContents: Provider<String>
    }

    /** Group prefix (e.g. `androidx.compose`) -> redirect version. */
    val versions: Map<String, String> by lazy {
        val parsed = Toml.parse(parameters.tomlFileContents.get())
        if (parsed.hasErrors()) {
            val issues =
                parsed.errors().joinToString("\n") {
                    "${parameters.tomlFileName}:${it.position()}: ${it.message}"
                }
            throw GradleException("${parameters.tomlFileName} has issues.\n$issues")
        }
        val table: TomlTable =
            parsed.getTable("versions")
                ?: throw GradleException("${parameters.tomlFileName} is missing the [versions] table")
        // tomlj treats a dotted String key as a path lookup, so the dotted group keys must be read
        // via the literal single-segment List overload (getString(listOf(key))), not getString(key).
        table.keySet().associateWith { key ->
            table.getString(listOf(key))
                ?: throw GradleException(
                    "${parameters.tomlFileName}: [versions] \"$key\" must be a string",
                )
        }
    }

    companion object {
        private const val TOML_FILE_NAME = "redirectversions.toml"

        internal fun registerOrGet(project: Project): Provider<RedirectVersionsService> {
            val contents = project.lazyReadFile(TOML_FILE_NAME)
            return project.gradle.sharedServices.registerIfAbsent(
                "redirectVersionsService",
                RedirectVersionsService::class.java,
            ) { spec ->
                spec.parameters.tomlFileName = TOML_FILE_NAME
                spec.parameters.tomlFileContents = contents
            }
        }
    }
}

/**
 * Project extension exposing the `redirectversions.toml` registry to build scripts (Groovy):
 * `project.redirectVersions.get("androidx.navigationevent")`. The key is an **exact** group; a
 * missing key fails fast — a build script asking for a redirect version it never registered is
 * always a bug.
 */
open class RedirectVersions(private val service: Provider<RedirectVersionsService>) {
    /** Exact lookup; throws if [key] is not in `redirectversions.toml`. */
    fun get(key: String): String =
        service.get().versions[key]
            ?: throw GradleException(
                "[artifactRedirection] no redirect version for '$key'. Add it to the [versions] " +
                    "table in redirectversions.toml.",
            )

    /** Exact lookup; null if [key] is not registered. */
    fun findOrNull(key: String): String? = service.get().versions[key]
}

/** Registers the [RedirectVersions] extension (`project.redirectVersions`). Idempotent. */
internal fun Project.registerRedirectVersionsExtension() {
    if (extensions.findByName("redirectVersions") == null) {
        extensions.create(
            "redirectVersions",
            RedirectVersions::class.java,
            RedirectVersionsService.registerOrGet(this),
        )
    }
}

/**
 * Look up an artifact-redirection version hierarchically from the most specific
 * (`<groupId>.<projectName>`) down to the least specific (`<groupId-prefix>`). E.g. for
 * `groupId = "androidx.compose.runtime"` and `project.name = "runtime"` searches:
 * `androidx.compose.runtime.runtime`, `androidx.compose.runtime`, `androidx.compose`, `androidx`.
 * Returns null if none is set.
 *
 * Reads the `[versions]` table of `redirectversions.toml`. Consumed by the `redirect { }`
 * parallel-graph back-end ([applyParallelRedirectGraph]) to resolve the version of the `androidx.*`
 * coordinate a redirect target points at.
 */
fun Project.findArtifactRedirectionVersion(groupId: String): String? {
    val versions = RedirectVersionsService.registerOrGet(this).get().versions
    val parts = groupId.split(".") + name
    val variations = (parts.size downTo 1).map { i -> parts.take(i).joinToString(".") }
    return variations.firstNotNullOfOrNull { versions[it] }
}

/**
 * Parallel-graph back-end for artifact redirection.
 *
 * For every target declared inside a `redirect { }` block (recorded in
 * [AndroidXMultiplatformExtension.redirectTargetDecls]), the redirect target is built **empty**: its
 * leaf source-set is re-rooted onto an empty parallel graph (`redirectCommonMain`) that carries only
 * `api(<androidx-coord>)`, instead of compiling the real `commonMain`. The fork then publishes an
 * empty-but-valid per-target klib/jar that depends on the `androidx.*` coordinate, and Gradle metadata
 * (`available-at`) carries the redirect. This is the sole redirection mechanism: the older
 * property-driven `CustomRootComponent` zero-artifact path was removed once every published module
 * had migrated to `redirect { }`.
 */
internal fun Project.applyParallelRedirectGraph(
    kmp: KotlinMultiplatformExtension,
    mpe: AndroidXMultiplatformExtension,
) {
    afterEvaluate {
        val decls = mpe.redirectTargetDecls
        if (decls.isEmpty()) return@afterEvaluate

        val redirectTargetNames = decls.map { it.targetName }.toSet()

        // --- Resolve the redirect coordinate (one per module). ---
        // Each redirect target carries its own RedirectCoordinate, but the published module has a
        // SINGLE shared `metadataApiElements` (commonMain) variant. That variant is the door a
        // consumer's commonMain resolves through, and it must list the redirect dependency (baseline
        // does: `androidx.annotation:annotation:1.9.1`) — otherwise common code compiles against the
        // empty fork metadata and loses every redirected symbol. One variant can carry only one
        // coordinate, so all redirect targets in a module must resolve to the same group:name:version;
        // `redirectCommonMain.api(coord)` then populates both that shared variant and every leaf.
        val coords = decls.map { decl ->
            val group = decl.redirectCoordinate.group
            val version = decl.redirectCoordinate.version
                ?: findArtifactRedirectionVersion(group)
                ?: error(
                    "[artifactRedirection] $path: target '${decl.targetName}' has no version " +
                        "argument and no `$group` (or any prefix) is registered in the [versions] " +
                        "table of redirectversions.toml",
                )
            "$group:$name:$version"
        }.distinct()
        val redirectCoord = coords.singleOrNull()
            ?: error(
                "[artifactRedirection] $path: redirect { } targets resolved to multiple distinct " +
                    "redirect coordinates $coords. The published commonMain metadata variant is " +
                    "singular and can carry only one redirect dependency — all redirect targets in a " +
                    "module must point at the same group:name:version.",
            )

        // Each source-set gets its OWN empty kotlin dir: KGP rejects the same .kt file appearing in
        // two fragments ("can be a part of only one module"). One generated tree, per-set subdirs.
        val graphRoot = layout.buildDirectory.dir("generated/redirectGraph").get().asFile
        fun emptyDirFor(name: String, withFile: Boolean): java.io.File {
            val dir = graphRoot.resolve(name).resolve("kotlin")
            dir.mkdirs()
            if (withFile) {
                val f = dir.resolve("EmptyRedirectRoot.kt")
                if (!f.exists()) {
                    f.writeText("// Auto-generated by artifactRedirection redirect { } for '$path'.\n")
                }
            }
            return dir
        }

        val allTargetNames = kmp.targets.map { it.name }.filter { it != "metadata" }.toSet()
        val forkBuiltExists = (allTargetNames - redirectTargetNames).isNotEmpty()

        // Parallel root: the redirect leaves were already wired to `redirectCommonMain` at
        // target-creation time (in `recordRedirect`), which opts them out of the default-hierarchy
        // auto-wiring to `commonMain`. Here we only fill it in: one empty .kt + api(coord), which
        // propagates to every redirect leaf's published variant.
        val redirectCommonMain = kmp.sourceSets.maybeCreate("redirectCommonMain")
        redirectCommonMain.kotlin.setSrcDirs(listOf(emptyDirFor("redirectCommonMain", withFile = true)))
        redirectCommonMain.resources.setSrcDirs(emptyList<Any>())
        dependencies.add("${redirectCommonMain.name}Api", redirectCoord)

        // Mirror commonMain's declared dependencies onto redirectCommonMain so they reach the redirect
        // targets' published metadata. These are the "keep-deps" (api(project(":lifecycle:...")) etc.)
        // that pin redirected versions and prevent stale fork-version pulls.
        // Since redirect targets are excluded from commonMain here, we re-add them explicitly. A
        // project dep publishes as its fork coordinate, which itself redirects onward to androidx.*.
        listOf("Api", "Implementation").forEach { kind ->
            configurations.findByName("commonMain$kind")?.dependencies?.toList()?.forEach { dep ->
                dependencies.add("${redirectCommonMain.name}$kind", dep)
            }
        }

        if (!forkBuiltExists) {
            // FULL STUB: no fork-built target needs the real `commonMain`. Empty it (and its
            // intermediates) so the published common-metadata variant carries no real classes. The
            // redirect leaves don't depend on commonMain (parallel root), so this only affects the
            // metadata variant.
            kmp.sourceSets.configureEach { ss ->
                if (ss.name == redirectCommonMain.name) return@configureEach
                ss.kotlin.setSrcDirs(listOf(emptyDirFor(ss.name, withFile = false)))
                ss.resources.setSrcDirs(emptyList<Any>())
            }
        } else {
            // PARTIAL redirect: each redirect leaf is excluded from the common hierarchy, so its only
            // parent is `redirectCommonMain`. But the leaf may carry per-target real source on disk
            // (e.g. `androidMain/AndroidTrace.android.kt`). Empty the leaf's own srcDirs so the
            // redirect artifact (klib/jar/AAR) compiles nothing — only the redirect dependency remains.
            redirectTargetNames.forEach { tname ->
                kmp.sourceSets.findByName("${tname}Main")?.let { leaf ->
                    leaf.kotlin.setSrcDirs(listOf(emptyDirFor("${tname}Main", withFile = false)))
                    leaf.resources.setSrcDirs(emptyList<Any>())
                }
            }
        }

        // Java sources (e.g. src/jvmMain/java/*.java) compile via separate JavaCompile tasks
        // (compileJvmMainJava), not kotlinc — so the kotlin-srcDir wipe above does not empty them.
        // Clear JavaCompile sources for redirect targets so the empty artifact carries no .class.
        // Full stub: clear all; partial: only the redirect targets' `compile<Target>MainJava`.
        val redirectJavaTasks =
            if (!forkBuiltExists) null
            else redirectTargetNames.map { "compile${it.replaceFirstChar(Char::uppercase)}MainJava" }.toSet()
        tasks.withType(JavaCompile::class.java).configureEach { jc ->
            if (redirectJavaTasks == null || jc.name in redirectJavaTasks) jc.setSource(files())
        }

        logger.lifecycle(
            "[artifactRedirection] {} -> {} (parallel graph: {} redirect target(s), forkBuilt={})",
            path, redirectCoord, redirectTargetNames.size, forkBuiltExists,
        )
    }
}
