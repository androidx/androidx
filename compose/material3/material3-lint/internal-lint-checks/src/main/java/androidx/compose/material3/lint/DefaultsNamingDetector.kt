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

package androidx.compose.material3.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UClass

private val VariantPrefixes =
    listOf("filledTonal", "filled", "elevated", "tonal", "outlined", "text")

private val StandardImmutableTypes =
    setOf(
        "kotlin.Int",
        "kotlin.Float",
        "kotlin.time.Duration",
        "kotlin.ranges.IntRange",
        "kotlin.ranges.CharRange",
        "kotlin.ranges.LongRange",
    )

private val AllowedStableInterfaces =
    setOf(
        "androidx.compose.ui.graphics.Shape",
        "androidx.compose.foundation.layout.PaddingValues",
        "androidx.compose.ui.Alignment",
        "androidx.compose.foundation.layout.Arrangement.Horizontal",
        "androidx.compose.foundation.layout.Arrangement.Vertical",
        "androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical",
    )

private fun KaSession.isConstantType(type: KaType?): Boolean {
    if (type == null) return false
    val expandedSymbol = type.expandedSymbol as? KaClassSymbol ?: return false
    val fqName = expandedSymbol.classId?.asSingleFqName()?.asString() ?: return false

    if (fqName in StandardImmutableTypes) return true

    if (expandedSymbol.isImmutable() || fqName in AllowedStableInterfaces) return true

    for (superType in type.allSupertypes(false)) {
        val superSymbol = superType.expandedSymbol as? KaClassSymbol ?: continue
        if (superSymbol.isImmutable()) return true

        val superFqName = superSymbol.classId?.asSingleFqName()?.asString()
        if (superFqName in AllowedStableInterfaces) return true
    }

    return false
}

private fun KaClassSymbol.isImmutable(): Boolean {
    return annotations.any { annotation ->
        val classId = annotation.classId?.asSingleFqName()?.asString()
        classId == "androidx.compose.runtime.Immutable"
    }
}

/** [Detector] that checks naming conventions for members of `*Defaults` objects. */
class DefaultsNamingDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        if (context.isTestSource) return UElementHandler.NONE
        val packageName = context.uastFile?.packageName ?: return UElementHandler.NONE
        if (
            !packageName.startsWith("androidx.compose.material3") || packageName.contains("samples")
        )
            return UElementHandler.NONE

        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                if (
                    context.file.path
                        .replace('\\', '/')
                        .contains("androidx/compose/material3/tokens/")
                ) {
                    return
                }
                val name = node.name ?: return
                if (!name.endsWith("Defaults")) return
                val objectDeclaration = node.sourcePsi as? KtObjectDeclaration ?: return

                val componentName = name.removeSuffix("Defaults")
                val componentNameDecapitalized = componentName.replaceFirstChar { it.lowercase() }
                val classNameHasVariant = VariantPrefixes.any { variant ->
                    name.contains(variant, ignoreCase = true)
                }

                // Syntactic prefix validations (fast, no type resolution required)
                objectDeclaration.declarations.forEach { declaration ->
                    if (!isDeclarationPublic(declaration)) return@forEach
                    val memberName = declaration.name ?: return@forEach
                    checkRedundantComponentPrefix(
                        context,
                        declaration,
                        memberName,
                        componentNameDecapitalized,
                    )
                    checkRedundantDefaultPrefix(context, declaration, memberName)
                    checkVariantPrefix(
                        context,
                        declaration,
                        memberName,
                        componentName,
                        classNameHasVariant,
                    )
                }

                // Semantic validations (type-resolved within a single analyze context)
                analyze(objectDeclaration) {
                    objectDeclaration.declarations.forEach { declaration ->
                        when (declaration) {
                            is KtProperty -> {
                                validatePropertyCamelCase(context, declaration)
                            }
                            is KtNamedFunction -> {
                                validateFunctionCamelCase(context, declaration)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun KaSession.validatePropertyCamelCase(context: JavaContext, property: KtProperty) {
        val name = property.name ?: return
        if (property.hasModifier(KtTokens.CONST_KEYWORD) || !isDeclarationPublic(property)) {
            return
        }
        val symbol = property.symbol as? KaPropertySymbol
        val type = symbol?.returnType
        if (!isConstantType(type)) {
            if (name.isNotEmpty() && name[0].isUpperCase()) {
                val newName = name.replaceFirstChar { it.lowercase() }
                val fix =
                    fix()
                        .name("Rename to '$newName'")
                        .replace()
                        .range(context.getNameLocation(property))
                        .with(newName)
                        .autoFix()
                        .build()
                context.report(
                    CAMEL_CASE_PROPERTY_ISSUE,
                    property,
                    context.getNameLocation(property),
                    "Properties on Defaults objects should be camelCase",
                    fix,
                )
            }
        }
    }

    private fun KaSession.validateFunctionCamelCase(
        context: JavaContext,
        function: KtNamedFunction,
    ) {
        val name = function.name ?: return
        if (!isDeclarationPublic(function)) {
            return
        }
        val symbol = function.symbol as? KaNamedFunctionSymbol
        val isComposable =
            symbol?.annotations?.any {
                it.classId?.asSingleFqName()?.asString() == "androidx.compose.runtime.Composable"
            } == true
        val isUnit = symbol?.returnType?.isUnitType == true
        val isComposableUnit = isComposable && isUnit

        if (!isComposableUnit) {
            if (name.isNotEmpty() && name[0].isUpperCase()) {
                val newName = name.replaceFirstChar { it.lowercase() }
                val fix =
                    fix()
                        .name("Rename to '$newName'")
                        .replace()
                        .range(context.getNameLocation(function))
                        .with(newName)
                        .autoFix()
                        .build()
                context.report(
                    CAMEL_CASE_PROPERTY_ISSUE,
                    function,
                    context.getNameLocation(function),
                    "Functions on Defaults objects should be camelCase",
                    fix,
                )
            }
        }
    }

    private fun checkRedundantComponentPrefix(
        context: JavaContext,
        declaration: KtDeclaration,
        name: String,
        componentNameDecapitalized: String,
    ) {
        if (name.startsWith(componentNameDecapitalized) && name != componentNameDecapitalized) {
            val newName =
                name.removePrefix(componentNameDecapitalized).replaceFirstChar { it.lowercase() }
            val fix =
                fix()
                    .name("Rename to '$newName'")
                    .replace()
                    .range(context.getNameLocation(declaration))
                    .with(newName)
                    .autoFix()
                    .build()
            context.report(
                REDUNDANT_PREFIX_ISSUE,
                declaration,
                context.getNameLocation(declaration),
                "Redundant component prefix: '$name' starts with '$componentNameDecapitalized'",
                fix,
            )
        }
    }

    private fun checkRedundantDefaultPrefix(
        context: JavaContext,
        declaration: KtDeclaration,
        name: String,
    ) {
        if (name.startsWith("default") && name.length > 7 && name[7].isUpperCase()) {
            val newName = name.substring(7).replaceFirstChar { it.lowercase() }
            val fix =
                fix()
                    .name("Rename to '$newName'")
                    .replace()
                    .range(context.getNameLocation(declaration))
                    .with(newName)
                    .autoFix()
                    .build()
            context.report(
                REDUNDANT_PREFIX_ISSUE,
                declaration,
                context.getNameLocation(declaration),
                "Redundant 'default' prefix: '$name' should be '$newName'",
                fix,
            )
        }
    }

    private fun checkVariantPrefix(
        context: JavaContext,
        declaration: KtDeclaration,
        name: String,
        componentName: String,
        classNameHasVariant: Boolean,
    ) {
        if (!classNameHasVariant) {
            val matchedVariant = VariantPrefixes.find { variant ->
                name.startsWith(variant) && name != variant
            }
            if (matchedVariant != null) {
                val suggestedObjectName =
                    matchedVariant.replaceFirstChar { it.uppercase() } + componentName + "Defaults"
                context.report(
                    REDUNDANT_PREFIX_ISSUE,
                    declaration,
                    context.getNameLocation(declaration),
                    "Should use dedicated defaults object (e.g., $suggestedObjectName.colors) instead of prefixed member '$name' in monolithic defaults",
                )
            }
        }
    }

    private fun isDeclarationPublic(declaration: KtDeclaration): Boolean {
        if (
            declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                declaration.hasModifier(KtTokens.INTERNAL_KEYWORD)
        ) {
            return false
        }
        var parent = declaration.parent
        while (parent != null) {
            if (parent is KtDeclaration) {
                if (
                    parent.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                        parent.hasModifier(KtTokens.INTERNAL_KEYWORD)
                ) {
                    return false
                }
            }
            parent = parent.parent
        }
        return true
    }

    companion object {
        val CAMEL_CASE_PROPERTY_ISSUE =
            Issue.create(
                id = "DefaultsCamelCaseWithoutConst",
                briefDescription = "Properties on Defaults objects should be camelCase",
                explanation =
                    """
                    Non-constant values and theme getters on Defaults objects (including @Composable val properties)
                    must use camelCase, reserving PascalCase strictly for true constants.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(DefaultsNamingDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        val REDUNDANT_PREFIX_ISSUE =
            Issue.create(
                id = "DefaultsRedundantPrefix",
                briefDescription = "Redundant prefix in Defaults member",
                explanation =
                    """
                    Inside Defaults objects, omit redundant component name prefixes (e.g., ToggleButtonDefaults.colors
                    instead of toggleButtonColors) and drop redundant "default" prefixes (e.g., CardDefaults.colors
                    or elevation instead of defaultColors or defaultElevation).
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(DefaultsNamingDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}
