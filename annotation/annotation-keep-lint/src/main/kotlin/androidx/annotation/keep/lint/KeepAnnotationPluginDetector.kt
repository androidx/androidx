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

package androidx.annotation.keep.lint

import com.android.tools.lint.client.api.LintTomlDocument
import com.android.tools.lint.client.api.LintTomlMapValue
import com.android.tools.lint.client.api.LintTomlValue
import com.android.tools.lint.client.api.TomlContext
import com.android.tools.lint.client.api.TomlScanner
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.GradleContext
import com.android.tools.lint.detector.api.GradleScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

/**
 * Detector that flags usage of the `androidx.annotation:annotation-keep` library dependency when
 * the associated `androidx.annotation.keep` Gradle plugin is not applied in the module's build
 * file.
 *
 * Note: this detector doesn't attempt to validate that jar transformations are wired up, nor does
 * it account for explicit `plugins.apply` invocations or convention plugins applied elsewhere in
 * the codebase (e.g. in `buildSrc` or root build scripts).
 *
 * Potential future improvement: When a version catalog is used but does not declare the
 * `androidx.annotation.keep` plugin under `[plugins]`, the quickfix could add an entry to the
 * catalog instead of inlining the plugin ID.
 */
class KeepAnnotationPluginDetector : Detector(), GradleScanner, TomlScanner {

    /** File-scoped state reset before checking each build file. */
    private class FileState {
        var hasKeepDependency = false
        var keepDependencyCookie: Any? = null
        var keepDependencyLocation: Location? = null
        var hasKeepPlugin = false
        var hasPluginsBlock = false
        var catalogAccessorPrefix: String? = null
    }

    /** Project/catalog-scoped state caching version catalog mappings across files. */
    private class CatalogState {
        val matchingLibraryAliases = mutableSetOf<String>()
        val matchingPluginAliases = mutableSetOf<String>()
        var knownPluginTomlAlias: String? = null
    }

    private var fileState = FileState()
    private var catalogState = CatalogState()

    override fun beforeCheckRootProject(context: Context) {
        catalogState = CatalogState()
    }

    override fun beforeCheckFile(context: Context) {
        fileState = FileState()
    }

    override fun visitTomlDocument(context: TomlContext, document: LintTomlDocument) {
        val libraries = document.getValue(TOML_LIBRARIES) as? LintTomlMapValue
        if (libraries != null) {
            for ((key, value) in libraries.getMappedValues()) {
                if (isKeepLibraryTomlValue(value)) {
                    catalogState.matchingLibraryAliases.add(normalizeCatalogKeyToAccessorPath(key))
                }
            }
        }

        val plugins = document.getValue(TOML_PLUGINS) as? LintTomlMapValue
        if (plugins != null) {
            for ((key, value) in plugins.getMappedValues()) {
                if (isKeepPluginTomlValue(value)) {
                    val normalized = normalizeCatalogKeyToAccessorPath(key)
                    catalogState.matchingPluginAliases.add(normalized)
                    if (catalogState.knownPluginTomlAlias == null) {
                        catalogState.knownPluginTomlAlias = normalized
                    }
                }
            }
        }
    }

    override fun checkDslPropertyAssignment(
        context: GradleContext,
        property: String,
        value: String,
        parent: String,
        parentParent: String?,
        propertyCookie: Any,
        valueCookie: Any,
        statementCookie: Any,
    ) {
        if (parent == "plugins") {
            fileState.hasPluginsBlock = true
            if (property == "id") {
                val pluginId = unquoteStringLiteral(value)
                if (pluginId == KEEP_PLUGIN_ID) {
                    fileState.hasKeepPlugin = true
                }
            } else if (property == "alias") {
                val aliasVal = unquoteStringLiteral(value)
                if (isKeepPluginAlias(aliasVal, context)) {
                    fileState.hasKeepPlugin = true
                }
            }
        } else if (isDependencyBlock(parent, parentParent)) {
            if (isKeepLibraryDependency(value, context)) {
                fileState.hasKeepDependency = true
                if (fileState.keepDependencyCookie == null) {
                    fileState.keepDependencyCookie = statementCookie
                    fileState.keepDependencyLocation = context.getLocation(statementCookie)
                }
            }
        }
    }

    /**
     * Detect manual plugin application without DSL plugin block.
     *
     * We don't expect this to work much, since generally this would be applied from outside the
     * module build file, but we try anyway since it's simple to do.
     *
     * If large projects register the keep library plugin automatically outside the module build
     * file, we assume they will just suppress this lint.
     */
    override fun checkMethodCall(
        context: GradleContext,
        statement: String,
        parent: String?,
        parentParent: String?,
        namedArguments: Map<String, String>,
        unnamedArguments: List<String>,
        cookie: Any,
    ) {
        if (statement == "plugins") {
            fileState.hasPluginsBlock = true
        }

        if (statement == "apply") {
            val plugin = namedArguments["plugin"]?.let { unquoteStringLiteral(it) }
            if (plugin == KEEP_PLUGIN_ID) {
                fileState.hasKeepPlugin = true
            }
            val unnamedPlugin = unnamedArguments.firstOrNull()?.let { unquoteStringLiteral(it) }
            if (unnamedPlugin == KEEP_PLUGIN_ID) {
                fileState.hasKeepPlugin = true
            }
        } else if (parent == "plugins") {
            fileState.hasPluginsBlock = true
            if (statement == "id") {
                val pluginId = unnamedArguments.firstOrNull()?.let { unquoteStringLiteral(it) }
                if (pluginId == KEEP_PLUGIN_ID) {
                    fileState.hasKeepPlugin = true
                }
            } else if (statement == "alias") {
                val aliasVal = unnamedArguments.firstOrNull()?.let { unquoteStringLiteral(it) }
                if (aliasVal != null && isKeepPluginAlias(aliasVal, context)) {
                    fileState.hasKeepPlugin = true
                }
            }
        }

        if (isDependencyBlock(parent, parentParent)) {
            val group = namedArguments["group"]?.let { unquoteStringLiteral(it) }
            val name = namedArguments["name"]?.let { unquoteStringLiteral(it) }
            if (group == KEEP_LIBRARY_GROUP && name == KEEP_LIBRARY_NAME) {
                fileState.hasKeepDependency = true
                if (fileState.keepDependencyCookie == null) {
                    fileState.keepDependencyCookie = cookie
                    fileState.keepDependencyLocation = context.getLocation(cookie)
                }
            }

            for (arg in unnamedArguments) {
                if (isKeepLibraryDependency(arg, context)) {
                    fileState.hasKeepDependency = true
                    if (fileState.keepDependencyCookie == null) {
                        fileState.keepDependencyCookie = cookie
                        fileState.keepDependencyLocation = context.getLocation(cookie)
                    }
                    break
                }
            }
        }
    }

    override fun afterCheckFile(context: Context) {
        if (context !is GradleContext) return
        if (fileState.hasKeepDependency && !fileState.hasKeepPlugin) {
            val cookie = fileState.keepDependencyCookie ?: return
            val location = fileState.keepDependencyLocation ?: context.getLocation(cookie)
            val isKts = context.file.name.endsWith(".kts")
            val fix = createQuickFix(context, isKts)
            val incident =
                Incident(
                    ISSUE,
                    cookie,
                    location,
                    "The `$KEEP_LIBRARY_COORDINATE` dependency requires the `$KEEP_PLUGIN_ID` plugin to be applied in this build file",
                    fix,
                )
            context.report(incident)
        }
    }

    private fun createQuickFix(context: GradleContext, isKts: Boolean): LintFix {
        var pluginToml = catalogState.knownPluginTomlAlias
        if (pluginToml == null) {
            val plugins = context.getTomlValue(TOML_PLUGINS) as? LintTomlMapValue
            if (plugins != null) {
                for ((key, value) in plugins.getMappedValues()) {
                    if (isKeepPluginTomlValue(value)) {
                        pluginToml = normalizeCatalogKeyToAccessorPath(key)
                        break
                    }
                }
            }
        }
        val contents = context.getContents()?.toString() ?: ""
        val analysis = PluginBlockAnalyzer.analyze(contents, isKts)
        val indent = analysis.indent
        val quote = analysis.quote

        val catalogPrefix = fileState.catalogAccessorPrefix ?: "libs" // fall back to a guess
        val pluginStatement =
            when {
                pluginToml != null -> "alias($catalogPrefix.plugins.$pluginToml)"
                isKts -> "id(\"$KEEP_PLUGIN_ID\")"
                else -> "id $quote$KEEP_PLUGIN_ID$quote"
            }

        val wholeFileLocation = Location.create(context.file, contents, 0, contents.length)

        val fixBuilder =
            fix().name("Apply $KEEP_PLUGIN_ID plugin").replace().range(wholeFileLocation)

        val anchor = analysis.anchorLine
        if (anchor != null) {
            fixBuilder.text(anchor).with("$anchor\n$indent$pluginStatement")
        } else {
            fixBuilder.beginning().with("plugins {\n$indent$pluginStatement\n}\n\n")
        }

        return fixBuilder.build()
    }

    private fun isDependencyBlock(parent: String?, parentParent: String?): Boolean {
        if (parent == "dependencies" || parentParent == "dependencies") return true
        if (parent?.endsWith("dependencies", ignoreCase = true) == true) return true
        return false
    }

    private fun isKeepLibraryDependency(rawExpression: String, context: GradleContext): Boolean {
        val expression = unquoteStringLiteral(rawExpression)
        if (expression.startsWith(KEEP_LIBRARY_COORDINATE)) {
            return true
        }

        if (expression.contains("group:") && expression.contains("name:")) {
            val group = extractNamedArgument(expression, "group")
            val name = extractNamedArgument(expression, "name")
            if (group == KEEP_LIBRARY_GROUP && name == KEEP_LIBRARY_NAME) {
                return true
            }
        }

        for (alias in catalogState.matchingLibraryAliases) {
            if (expression.endsWith(".$alias") || expression == alias) {
                if (expression.endsWith(".$alias")) {
                    val prefix = expression.substringBefore(".$alias")
                    if (prefix.isNotEmpty() && !prefix.contains('.')) {
                        fileState.catalogAccessorPrefix = prefix
                    }
                }
                return true
            }
        }

        val libraries = context.getTomlValue(TOML_LIBRARIES) as? LintTomlMapValue
        if (libraries != null) {
            for ((key, value) in libraries.getMappedValues()) {
                if (isKeepLibraryTomlValue(value)) {
                    val alias = normalizeCatalogKeyToAccessorPath(key)
                    catalogState.matchingLibraryAliases.add(alias)
                    if (expression.endsWith(".$alias") || expression == alias) {
                        if (expression.endsWith(".$alias")) {
                            val prefix = expression.substringBefore(".$alias")
                            if (prefix.isNotEmpty() && !prefix.contains('.')) {
                                fileState.catalogAccessorPrefix = prefix
                            }
                        }
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun isKeepPluginAlias(rawExpression: String, context: GradleContext): Boolean {
        val expression = unquoteStringLiteral(rawExpression)

        for (alias in catalogState.matchingPluginAliases) {
            if (
                expression.endsWith(".plugins.$alias") ||
                    expression.endsWith(".$alias") ||
                    expression == alias
            ) {
                return true
            }
        }

        val plugins = context.getTomlValue(TOML_PLUGINS) as? LintTomlMapValue
        if (plugins != null) {
            for ((key, value) in plugins.getMappedValues()) {
                if (isKeepPluginTomlValue(value)) {
                    val alias = normalizeCatalogKeyToAccessorPath(key)
                    catalogState.matchingPluginAliases.add(alias)
                    if (catalogState.knownPluginTomlAlias == null) {
                        catalogState.knownPluginTomlAlias = alias
                    }
                    if (
                        expression.endsWith(".plugins.$alias") ||
                            expression.endsWith(".$alias") ||
                            expression == alias
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isKeepLibraryTomlValue(value: Any?): Boolean {
        if (value is LintTomlMapValue) {
            val module = value["module"]?.getActualValue() as? String
            if (module != null && module.startsWith(KEEP_LIBRARY_COORDINATE)) {
                return true
            }
            val group = value["group"]?.getActualValue() as? String
            val name = value["name"]?.getActualValue() as? String
            if (group == KEEP_LIBRARY_GROUP && name == KEEP_LIBRARY_NAME) {
                return true
            }
        } else if (value is LintTomlValue) {
            val str = value.getActualValue() as? String ?: value.getText()
            if (str.startsWith(KEEP_LIBRARY_COORDINATE)) {
                return true
            }
        }
        return false
    }

    private fun isKeepPluginTomlValue(value: Any?): Boolean {
        if (value is LintTomlMapValue) {
            val id = value["id"]?.getActualValue() as? String
            if (id == KEEP_PLUGIN_ID) {
                return true
            }
        } else if (value is LintTomlValue) {
            val str = value.getActualValue() as? String ?: value.getText()
            if (str.startsWith(KEEP_PLUGIN_ID)) {
                return true
            }
        }
        return false
    }

    /**
     * Normalizes a version catalog key or DSL accessor expression to a canonical dot-separated path
     * (e.g., `"annotation-keep"` or `"annotation_keep"` -> `"annotation.keep"`).
     *
     * Gradle version catalogs permit hyphens (`-`), underscores (`_`), and dots (`.`) in TOML keys,
     * but Gradle transforms them into dot-separated nested accessors in generated DSL code (e.g.,
     * `libs.annotation.keep` or `libs.plugins.annotation.keep`). Normalizing both catalog keys and
     * build script expressions ensures reliable matching between TOML declarations and build file
     * usages.
     */
    private fun normalizeCatalogKeyToAccessorPath(key: String): String {
        return key.replace('-', '.').replace('_', '.')
    }

    private fun unquoteStringLiteral(str: String): String {
        return str.trim().removeSurrounding("\"").removeSurrounding("'")
    }

    /**
     * Extracts and unquotes the value of a named argument from a Groovy map-style dependency
     * expression (e.g., extracting `"androidx.annotation"` for `"group"` in `group:
     * 'androidx.annotation', name: 'annotation-keep'`).
     */
    private fun extractNamedArgument(expression: String, argName: String): String? {
        val target = "$argName:"
        val index = expression.indexOf(target)
        if (index == -1) return null
        val after = expression.substring(index + target.length).trim()
        val value = after.substringBefore(',').substringBefore(')').trim()
        return unquoteStringLiteral(value).takeIf { it.isNotEmpty() }
    }

    companion object {
        const val KEEP_LIBRARY_GROUP = "androidx.annotation"
        const val KEEP_LIBRARY_NAME = "annotation-keep"
        const val KEEP_LIBRARY_COORDINATE = "$KEEP_LIBRARY_GROUP:$KEEP_LIBRARY_NAME"

        const val KEEP_PLUGIN_ID = "androidx.annotation.keep"

        private const val TOML_LIBRARIES = "libraries"
        private const val TOML_PLUGINS = "plugins"

        @JvmField
        val ISSUE =
            Issue.create(
                id = "MissingKeepAnnotationPlugin",
                briefDescription = "Missing Keep Annotation Gradle Plugin",
                explanation =
                    """
          When including the `$KEEP_LIBRARY_COORDINATE` dependency, the corresponding \
          `$KEEP_PLUGIN_ID` Gradle plugin must also be applied in the module's build file \
          to process annotations like `@UsesReflectionToConstruct`, and generate `-keep` rules from
          them.
          """,
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        KeepAnnotationPluginDetector::class.java,
                        Scope.GRADLE_AND_TOML_SCOPE,
                    ),
                androidSpecific = false,
            )
    }
}
