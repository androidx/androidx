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

@file:Suppress("UnstableApiUsage")

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.jvm.types.JvmType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import java.util.EnumSet
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getContainingUClass

/**
 * This detects uses of Set, List, and Map with primitive type arguments. Internally, these should
 * be replaced by the primitive-specific collections in androidx.
 */
class PrimitiveInCollectionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() =
        listOf<Class<out UElement>>(UMethod::class.java, UVariable::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                if (
                    context.evaluator.isOverride(node) || node.isDataClassGeneratedMethod(context)
                ) {
                    return
                }

                val primitiveCollection = node.returnType?.primitiveCollectionReplacement(context)
                if (primitiveCollection != null) {
                    // The location doesn't appear to work with property types with getters rather
                    // than
                    // full fields. Target the property name instead if we don't have a location.
                    val target =
                        if (context.getLocation(node.returnTypeReference).start == null) {
                            node
                        } else {
                            node.returnTypeReference
                        }
                    report(
                        context,
                        node,
                        target,
                        "return type ${node.returnType?.presentableText} of ${node.name}:" +
                            " replace with $primitiveCollection"
                    )
                }
            }

            override fun visitVariable(node: UVariable) {
                // Kotlin destructuring expression is desugared. E.g.,
                //
                //   val (x, y) = pair
                //
                // is mapped to
                //
                //   val varHash = pair // temp variable
                //   val x = varHash.component1()
                //   val y = varHash.component2()
                //
                // and thus we don't need to analyze the temporary variable.
                // Their `sourcePsi`s are different:
                //   KtDestructuringDeclaration (for overall expression) v.s.
                //   KtDestructuringDeclarationEntry (for individual local variables)
                if (node.sourcePsi is KtDestructuringDeclaration) return

                val primitiveCollection =
                    node.type.primitiveCollectionReplacement(context) ?: return
                if (node.isLambdaParameter()) {
                    // Don't notify for lambda parameters. We'll be notifying for the method
                    // that accepts the lambda, so we already have it flagged there. The
                    // person using it doesn't really have a choice about the parameters that
                    // are passed.
                    return
                }
                val parent = node.uastParent
                val messageContext =
                    if (parent is UMethod) {
                        // Data class constructor parameters are caught 4 times:
                        // 1) constructor method parameter
                        // 2) the field of the backing 'val'
                        // 3) the getter for the field
                        // 4) the generated copy() method.
                        // We can eliminate the copy() at least, even if we get duplicates for the
                        // other 3. It would be ideal to eliminate 2 of the other 3, but it isn't
                        // easy to do and still catch all uses.
                        if (
                            context.evaluator.isOverride(parent) ||
                                (context.evaluator.isData(parent) && parent.name.startsWith("copy"))
                        ) {
                            return
                        }
                        val methodName =
                            if (parent.isConstructor) {
                                "constructor ${parent.getContainingUClass()?.name}"
                            } else {
                                "method ${parent.name}"
                            }
                        "$methodName has parameter ${node.name} " +
                            "with type ${node.type.presentableText}: replace with $primitiveCollection"
                    } else {
                        val varOrField = if (node is UField) "field" else "variable"

                        "$varOrField ${node.name} with type ${node.type.presentableText}: replace" +
                            " with $primitiveCollection"
                    }
                report(context, node, node.typeReference, messageContext)
            }
        }

    private fun report(context: JavaContext, node: UElement, target: Any?, message: String) {
        val location =
            if (target == null) {
                context.getLocation(node)
            } else {
                context.getLocation(target)
            }
        context.report(issue = ISSUE, scope = node, location = location, message = message)
    }

    companion object {
        private const val PrimitiveInCollectionId = "PrimitiveInCollection"

        val ISSUE =
            Issue.create(
                id = PrimitiveInCollectionId,
                briefDescription =
                    "A primitive (Short, Int, Long, Char, Float, Double) or " +
                        "a value class wrapping a primitive was used as in a collection. Primitive " +
                        "versions of collections exist.",
                explanation =
                    "Using a primitive type in a collection will autobox the primitive " +
                        "value, causing an allocation. To avoid the allocation, use one of the androidx " +
                        "collections designed for use with primitives. For example instead of Set<Int>, " +
                        "use IntSet.",
                category = Category.PERFORMANCE,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        PrimitiveInCollectionDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE)
                    )
            )
    }
}

private val SetType = java.util.Set::class.java.canonicalName
private val ListType = java.util.List::class.java.canonicalName
private val MapType = java.util.Map::class.java.canonicalName

// Map from the kotlin type to the primitive type used in the collection
// e.g. Set<Byte> -> IntSet
private val BoxedTypeToSuggestedPrimitive =
    mapOf(
        "java.lang.Byte" to "Int",
        "java.lang.Character" to "Int",
        "java.lang.Short" to "Int",
        "java.lang.Integer" to "Int",
        "java.lang.Long" to "Long",
        "java.lang.Float" to "Float",
        "java.lang.Double" to "Float",
        "kotlin.UByte" to "Int",
        "kotlin.UShort" to "Int",
        "kotlin.UInt" to "Int",
        "kotlin.ULong" to "Long",
    )

private fun JvmType.primitiveCollectionReplacement(context: JavaContext): String? {
    if (this !is PsiClassReferenceType) return null
    val resolved = resolve() ?: return null
    val evaluator = context.evaluator
    if (evaluator.inheritsFrom(resolved, SetType, false)) {
        val typeArgument = typeArguments().firstOrNull() ?: return null
        val elementPrimitive = typeArgument.primitiveName()
        if (elementPrimitive != null) {
            return "${elementPrimitive}Set"
        }
    } else if (evaluator.inheritsFrom(resolved, ListType, false)) {
        val typeArgument = typeArguments().firstOrNull() ?: return null
        val elementPrimitive = typeArgument.primitiveName()
        if (elementPrimitive != null) {
            return "${elementPrimitive}List"
        }
    } else if (evaluator.inheritsFrom(resolved, MapType, false)) {
        val keyType = typeArguments().firstOrNull() ?: return null
        val valueType = typeArguments().lastOrNull() ?: return null
        val keyPrimitive = keyType.primitiveName()
        val valuePrimitive = valueType.primitiveName()
        if (keyPrimitive != null) {
            return if (valuePrimitive != null) {
                "$keyPrimitive${valuePrimitive}Map"
            } else {
                "${keyPrimitive}ObjectMap"
            }
        } else if (valuePrimitive != null) {
            return "Object${valuePrimitive}Map"
        }
    }
    return null
}

private fun JvmType.primitiveName(): String? =
    when (this) {
        is PsiClassReferenceType -> toPrimitiveName()
        is PsiWildcardType -> {
            val bound =
                if (isBounded) {
                    bound!!
                } else {
                    superBound
                }
            when (bound) {
                is PsiClassReferenceType -> bound.toPrimitiveName()
                is PsiPrimitiveType -> BoxedTypeToSuggestedPrimitive[bound.boxedTypeName]
                else -> null
            }
        }
        else -> null
    }

private fun PsiClassReferenceType.toPrimitiveName(): String? {
    val resolvedType = resolve() ?: return null
    if (hasJvmInline(resolvedType)) {
        // Depending on where the inline class is coming from, there are a couple places to check
        // for what the value is.
        val valueType =
            // For value classes in this compilation, find the field which corresponds to the value
            // class constructor parameter (the constructor is not modeled in k2 psi). There may be
            // other fields (a companion object and companion object fields).
            resolvedType.fields
                .singleOrNull { (it as? KtLightField)?.kotlinOrigin is KtParameter }
                ?.type
                // For value classes from a different compilation, the fields may not have a kotlin
                // origin attached, so there's nothing to differentiate companion fields and the
                // value class field. Instead find the "constructor-impl" method (which is not
                // present for value classes from this compilation).
                ?: resolvedType.methods
                    .firstOrNull { it.parameters.size == 1 && it.name == "constructor-impl" }
                    ?.parameters
                    ?.first()
                    ?.type
        if (valueType is PsiPrimitiveType) {
            return BoxedTypeToSuggestedPrimitive[valueType.boxedTypeName]
        }
        if (valueType is PsiClassReferenceType) {
            return valueType.toPrimitiveName()
        }
    }
    return BoxedTypeToSuggestedPrimitive[resolvedType.qualifiedName]
}

private val JvmInlineAnnotation = JvmInline::class.qualifiedName!!

private fun UMethod.isDataClassGeneratedMethod(context: JavaContext): Boolean =
    context.evaluator.isData(containingClass) &&
        (name.startsWith("copy") || name.startsWith("component"))

private fun UVariable.isLambdaParameter(): Boolean {
    val sourcePsi = sourcePsi
    return ((sourcePsi == null && (javaPsi as? PsiParameter)?.name == "it") ||
        (sourcePsi as? KtParameter)?.isLambdaParameter == true)
}

private fun hasJvmInline(type: PsiClass): Boolean {
    for (annotation in type.annotations) {
        if (annotation.qualifiedName == JvmInlineAnnotation) {
            return true
        }
    }
    return false
}
