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

import com.android.tools.lint.checks.ApiLookup
import com.android.tools.lint.checks.DataFlowAnalyzer
import com.android.tools.lint.client.api.LintFixPerformer
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TypeEvaluator
import com.android.tools.lint.detector.api.UastLintUtils
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiCapturedWildcardType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.PsiWildcardType
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UField
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.skipParenthesizedExprUp
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val CONSTRUCTOR_NAME = "<init>"

/** Looks for missing keep annotations on reflective usages. */
class KeepRuleDetector : Detector(), SourceCodeScanner {
    companion object {
        private val IMPLEMENTATION =
            Implementation(KeepRuleDetector::class.java, Scope.JAVA_FILE_SCOPE)

        /** Missing keep annotations. */
        @JvmField
        val ISSUE =
            Issue.create(
                id = "ReflectionAnnotation",
                briefDescription = "Missing Reflection Annotation",
                explanation =
                    """
          When methods are using reflection to access APIs, they should declare \
          what APIs they are accessing using the new `@$USES_REFLECTION_TO_ACCESS_METHOD_NAME` and \
          `@$USES_REFLECTION_TO_ACCESS_FIELD_NAME` annotations. \
          This makes it possible for the shrinker (R8) to correctly remove unused code \
          and resources without accidentally also removing reflectively accessed code, \
          which can lead to program crashes.
          """,
                category = Category.CORRECTNESS,
                priority = 2,
                severity = Severity.WARNING,
                androidSpecific = true,
                implementation = IMPLEMENTATION,
                // Opt-in for now until API is validated
                enabledByDefault = false,
            )

        private const val PKG_PREFIX = "androidx.annotation.keep."
        private const val USES_REFLECTION_TO_ACCESS_METHOD_NAME = "UsesReflectionToAccessMethod"
        private const val USES_REFLECTION_TO_ACCESS_FIELD_NAME = "UsesReflectionToAccessField"
        private const val USES_REFLECTION_TO_CONSTRUCT_NAME = "UsesReflectionToConstruct"
        private const val UNCONDITIONALLY_KEEP_NAME = "UnconditionallyKeep"

        const val USES_REFLECTION_TO_ACCESS_METHOD_FQN =
            "$PKG_PREFIX$USES_REFLECTION_TO_ACCESS_METHOD_NAME"
        const val USES_REFLECTION_TO_ACCESS_FIELD_FQN =
            "$PKG_PREFIX$USES_REFLECTION_TO_ACCESS_FIELD_NAME"
        const val USES_REFLECTION_TO_CONSTRUCT_FQN = "$PKG_PREFIX$USES_REFLECTION_TO_CONSTRUCT_NAME"

        const val LOAD_CLASS = "loadClass"
        const val FOR_NAME = "forName"
        const val GET_CONSTRUCTOR = "getConstructor"
        const val GET_METHOD = "getMethod"
        const val GET_FIELD = "getField"
        const val GET_DECLARED_CONSTRUCTOR = "getDeclaredConstructor"
        const val GET_DECLARED_METHOD = "getDeclaredMethod"
        const val GET_DECLARED_FIELD = "getDeclaredField"

        const val GET_CONSTRUCTORS = "getConstructors"
        const val GET_METHODS = "getMethods"
        const val GET_FIELDS = "getFields"
        const val GET_DECLARED_CONSTRUCTORS = "getDeclaredConstructors"
        const val GET_DECLARED_METHODS = "getDeclaredMethods"
        const val GET_DECLARED_FIELDS = "getDeclaredFields"

        private val REFLECTION_CALLS =
            listOf(
                FOR_NAME,
                LOAD_CLASS,
                GET_CONSTRUCTOR,
                GET_METHOD,
                GET_FIELD,
                GET_DECLARED_CONSTRUCTOR,
                GET_DECLARED_METHOD,
                GET_DECLARED_FIELD,
                GET_CONSTRUCTORS,
                GET_METHODS,
                GET_FIELDS,
                GET_DECLARED_CONSTRUCTORS,
                GET_DECLARED_METHODS,
                GET_DECLARED_FIELDS,
            )

        val KOTLIN_REFLECTION_METHODS =
            listOf(
                "members", // this is available in kotlin-stdlib, the rest are in kotlin-reflect
                "declaredMembers",
                "declaredFunctions",
                "declaredMemberFunctions",
                "declaredMemberProperties",
                "staticFunctions",
                "staticProperties",
                "memberFunctions",
                "primaryConstructor",
            )

        /**
         * We know [element] has type java.lang.Class<T> and we try to find out the PsiType for T.
         */
        fun getJavaClassType(context: JavaContext, element: UElement?): PsiType? {
            if (element is UExpression) {
                if (element is UParenthesizedExpression) {
                    return getJavaClassType(context, element.expression)
                }

                // First try the type inferred from the Psi, in case it's a known class reference.
                val type = element.getExpressionType()

                if (type is PsiClassType && type.parameterCount == 1) {
                    var clazz = type.parameters[0]

                    if (clazz is PsiClassType) {
                        PsiPrimitiveType.getUnboxedType(clazz)?.let {
                            // Make sure we extract the primitive type (int.class, Integer.TYPE in
                            // Java,
                            // Int::class.javaPrimitiveType in Kotlin)
                            if (element is UQualifiedReferenceExpression) {
                                val identifier =
                                    (element.selector.skipParenthesizedExprDown()
                                            as? USimpleNameReferenceExpression)
                                        ?.identifier
                                if (identifier == "javaPrimitiveType" || identifier == "TYPE") {
                                    clazz = it
                                }
                            }
                            if (
                                element is UClassLiteralExpression &&
                                    element.evaluate() is PsiPrimitiveType
                            ) {
                                clazz = it
                            }
                        }
                        return clazz
                    } else if (clazz is PsiArrayType) {
                        return clazz
                    }
                    // Here we might have a wildcard type, most likely an unbounded Class<?> coming
                    // from
                    // a loadClass or Class.forName() call. We can also have a bounded <? extends
                    // Foo>,
                    // from foo.getClass(), but if Foo is not final we cannot statically guarantee
                    // the
                    // receiver is indeed class Foo. So we fall-through to the handling below.
                }

                if (element is UClassLiteralExpression) {
                    val type = element.type
                    if (type != null) {
                        return type
                    }
                }

                if (element is UReferenceExpression) {
                    val resolved = element.resolve()
                    if (resolved is PsiVariable) {
                        // Follow the indirection and inspect the actual definition
                        UastLintUtils.findLastAssignment(resolved, element)?.let { expression ->
                            return getJavaClassType(context, expression)
                        }
                    }

                    val castType = getCastType(element)
                    if (castType != null) {
                        return castType.first
                    }

                    if (
                        element is UQualifiedReferenceExpression &&
                            element.selector.skipParenthesizedExprDown() is UCallExpression
                    ) {
                        val call = element.selector.skipParenthesizedExprDown() as UCallExpression
                        val name = call.methodName

                        if (FOR_NAME == name || LOAD_CLASS == name) {
                            val arguments = call.valueArguments
                            if (arguments.isNotEmpty()) {
                                return ConstantEvaluator.evaluateString(null, arguments[0], false)
                                    ?.let {
                                        PsiElementFactory.getInstance(context.project.ideaProject)
                                            .createTypeFromText(it, null)
                                    }
                            }
                        }
                    }
                }

                if (type is PsiArrayType) {
                    return null
                }
            }

            return null
        }

        private fun addParameterType(
            context: JavaContext,
            argument: UExpression,
            list: MutableList<String>,
            isKotlin: Boolean,
        ): Boolean {
            val sourcePsi = argument.sourcePsi?.parent
            if (sourcePsi is KtValueArgument && sourcePsi.isSpread) {
                // Handle Kotlin spread operator; we're referencing some variable and want
                // to inject its value (if we can find it) as arguments
                analyze(sourcePsi) {
                    val resolved = (argument.sourcePsi as KtElement).resolveToCall()
                    val psi = resolved?.successfulVariableAccessCall()?.symbol?.psi
                    if (psi is KtProperty && psi.initializer != null) {
                        val element = psi.initializer.toUElement()
                        if (element is UCallExpression) {
                            for (element in element.valueArguments) {
                                if (!addParameterType(context, element, list, isKotlin)) {
                                    return false
                                }
                            }
                            return true
                        } else {
                            return false
                        }
                    } else {
                        return false
                    }
                }
            }

            val source = argument.sourcePsi?.text ?: ""
            val fqn =
                when (source) {
                    "Integer",
                    "Character" -> source
                    // Avoid going via PSI type utilities which will confuse Int with Integer
                    "Int::class",
                    "Boolean::class",
                    "Byte::class",
                    "Short::class",
                    "Long::class",
                    "Float::class",
                    "Double::class",
                    "Char::class" -> source.substringBefore(":")
                    "int.class",
                    "boolean.class",
                    "byte.class",
                    "short.class",
                    "long.class",
                    "float.class",
                    "double.class",
                    "char.class" -> source.substringBefore(".")
                    else -> {
                        if (source.endsWith(".TYPE")) {
                            when (source) {
                                "java.lang.Integer.TYPE",
                                "Integer.TYPE" -> if (isKotlin) "Int" else "int"
                                "java.lang.Boolean.TYPE",
                                "Boolean.TYPE" -> if (isKotlin) "Boolean" else "boolean"
                                "java.lang.Long.TYPE",
                                "Long.TYPE" -> if (isKotlin) "Long" else "long"
                                "java.lang.Double.TYPE",
                                "Double.TYPE" -> if (isKotlin) "Double" else "double"
                                "java.lang.Character.TYPE",
                                "Character.TYPE" -> if (isKotlin) "Char" else "char"
                                "java.lang.Float.TYPE",
                                "Float.TYPE" -> if (isKotlin) "Float" else "float"
                                "java.lang.Short.TYPE",
                                "Short.TYPE" -> if (isKotlin) "Short" else "short"
                                "java.lang.Byte.TYPE",
                                "Byte.TYPE" -> if (isKotlin) "Byte" else "byte"
                                else ->
                                    getParameterType(
                                        context,
                                        getJavaClassType(context, argument),
                                        isKotlin,
                                    )
                            }
                        } else {
                            getParameterType(context, getJavaClassType(context, argument), isKotlin)
                        }
                    }
                }

            if (fqn != null) {
                list.add(fqn)
                return true
            } else {
                return false
            }
        }

        private fun getParameterType(
            context: JavaContext,
            type: PsiType?,
            isKotlin: Boolean,
        ): String? {
            type ?: return null

            val erasedType = context.evaluator.erasure(type)

            return if (erasedType is PsiClassType) {
                erasedType.resolve()?.qualifiedName ?: erasedType.canonicalText
            } else if (isKotlin && erasedType is PsiArrayType) {
                val componentType = erasedType.deepComponentType
                val dimensions = erasedType.arrayDimensions
                "Array<".repeat(dimensions) + componentType.canonicalText + ">".repeat(dimensions)
            } else if (isKotlin && erasedType is PsiPrimitiveType) {
                val javaType = erasedType.canonicalText
                javaPrimitiveToKotlinPrimitive(javaType) ?: javaType
            } else erasedType?.canonicalText ?: ""
        }

        /**
         * Returns whether we need to use a fully qualified name for the given type name; this is
         * the case for the number classes (which for reflection purposes are not the same as the
         * primitives; a method parameter of type java.lang.Integer should not match an Int type).
         */
        private fun useFullyQualifiedName(fqn: String): Boolean {
            return when (fqn) {
                "java.lang.Boolean",
                "java.lang.Double",
                "java.lang.Float",
                "java.lang.Short",
                "java.lang.Byte",
                "java.lang.Long" -> true
                // Note: this list does not include java.lang.Integer and java.lang.Boolean,
                // since these aren't Kotlin primitive type names so they'll implicitly
                // refer to the correct type
                else -> false
            }
        }

        private fun javaPrimitiveToKotlinPrimitive(type: String): String? {
            return when (type) {
                "int" -> "Int"
                "boolean" -> "Boolean"
                "char" -> "Char"
                "long" -> "Long"
                "double" -> "Double"
                "byte" -> "Byte"
                "short" -> "Short"
                "float" -> "Float"
                else -> null
            }
        }

        private fun getNestedClassType(clazz: PsiType): PsiType? {
            if (clazz is PsiCapturedWildcardType) {
                val bound = clazz.wildcard.bound
                if (bound is PsiClassType) {
                    return bound
                }
            } else if (clazz is PsiWildcardType) {
                val bound = clazz.bound
                if (bound is PsiClassType) {
                    return bound
                }
            } else if (clazz is PsiClassType) {
                val parameters = clazz.parameters
                if (parameters.size == 1) {
                    assert(clazz.className == "Class") { clazz.className }
                    return parameters[0]
                }
            }

            return null
        }

        private fun getAsSubclassType(selector: UExpression): Pair<PsiType, Boolean>? {
            if (
                selector is UCallExpression &&
                    selector.methodName == "asSubclass" &&
                    (selector.tryResolve() as? PsiMethod)?.containingClass?.qualifiedName ==
                        "java.lang.Class" &&
                    selector.valueArguments.size == 1
            ) {
                val argument = selector.valueArguments[0]
                return getCastType(argument)
            }
            return null
        }

        private fun getCastType(argument: UExpression): Pair<PsiType, Boolean>? {
            if (
                argument is UQualifiedReferenceExpression &&
                    argument.selector is USimpleNameReferenceExpression &&
                    (argument.selector as USimpleNameReferenceExpression).resolvedName ==
                        "getJavaClass"
            ) {
                // Kotlin syntax: asSubClass(Foo::class.java)
                val receiver = argument.receiver
                if (receiver is UClassLiteralExpression) {
                    val type = receiver.type
                    if (type != null) {
                        return type to true
                    }
                }
            } else if (argument is UClassLiteralExpression) {
                // Java syntax: asSubClass(Foo.class)
                val type = argument.type
                if (type != null) {
                    return type to true
                }
            }

            return null
        }

        private fun hasMemberReflectionCalls(container: UAnnotated): Boolean {
            var found = false
            container.accept(
                object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        val name = node.methodName
                        if (
                            REFLECTION_CALLS.contains(name) &&
                                name != LOAD_CLASS &&
                                name != FOR_NAME
                        ) {
                            found = true
                        }
                        return found || super.visitCallExpression(node)
                    }
                }
            )
            return found
        }

        private fun UExpression.isJavaClassAccess(): Boolean {
            return this is USimpleNameReferenceExpression &&
                this.identifier == "javaClass" &&
                (this.tryResolve() as? PsiMethod)?.containingClass?.qualifiedName ==
                    "kotlin.jvm.JvmClassMappingKt" ||
                this is UCallExpression &&
                    this.methodName == "getClass" &&
                    (this.tryResolve() as? PsiMethod)?.containingClass?.qualifiedName ==
                        "java.lang.Object"
        }

        private fun getReflectionAnnotations(
            context: JavaContext,
            method: UAnnotated,
            isKotlin: Boolean,
        ): List<Reflection> {
            val list = mutableListOf<Reflection>()
            var curr = method
            while (true) {
                addReflectionAnnotations(context, curr, isKotlin, list)
                curr = curr.getParentOfType<UAnnotated>() ?: break
            }
            return list
        }

        private fun addReflectionAnnotations(
            context: JavaContext,
            method: UAnnotated,
            isKotlin: Boolean,
            list: MutableList<Reflection>,
        ) {
            @Suppress("ExternalAnnotations")
            for (annotation in method.uAnnotations) {
                val qualifiedName = annotation.qualifiedName
                when (qualifiedName) {
                    USES_REFLECTION_TO_CONSTRUCT_FQN,
                    USES_REFLECTION_TO_ACCESS_METHOD_FQN -> {
                        val (className, classNameIsConstant) = annotation.getClassName()
                        val methodName =
                            if (qualifiedName != USES_REFLECTION_TO_CONSTRUCT_FQN)
                                annotation.findAttributeValue("methodName")?.evaluateString()
                            else CONSTRUCTOR_NAME

                        var params: List<String>? = null
                        var paramsAreStrings = false

                        val paramsAttribute =
                            annotation.findDeclaredAttributeValue("parameterTypes")
                        if (paramsAttribute is UCallExpression) {
                            val list = mutableListOf<String>()
                            for (argument in paramsAttribute.valueArguments) {
                                addParameterType(context, argument, list, isKotlin)
                            }
                            params = list
                        } else {
                            val paramClassNames =
                                annotation.findDeclaredAttributeValue("parameterTypeNames")
                            if (paramClassNames is UCallExpression) {
                                paramsAreStrings = true
                                val list = mutableListOf<String>()
                                for (argument in paramClassNames.valueArguments) {
                                    ConstantEvaluator.evaluateString(null, argument, false)
                                        ?.let(list::add)
                                }
                                params = list
                            }
                        }

                        var returnClass = ""
                        var returnClassIsConstant = false
                        val classNameAttribute =
                            annotation.findDeclaredAttributeValue("returnTypeName")
                        if (classNameAttribute != null) {
                            returnClass = classNameAttribute.evaluateString() ?: ""
                        } else {
                            val classConstantAttribute =
                                annotation.findDeclaredAttributeValue("returnType")
                            if (classConstantAttribute is UClassLiteralExpression) {
                                classConstantAttribute.type?.canonicalText?.let {
                                    returnClass = it
                                    returnClassIsConstant = true
                                }
                            }
                        }

                        list.add(
                            MethodReflection(
                                className = className,
                                classNameIsConstant = classNameIsConstant,
                                methodName = methodName ?: "*",
                                parameterTypes = params,
                                parameterTypesAreStrings = paramsAreStrings,
                                returnType = returnClass,
                                returnTypeIsConstant = returnClassIsConstant,
                            )
                        )
                    }
                    USES_REFLECTION_TO_ACCESS_FIELD_FQN -> {
                        val (className, classNameIsConstant) = annotation.getClassName()
                        val fieldName = annotation.findAttributeValue("fieldName")?.evaluateString()

                        var fieldClass = ""
                        var fieldClassIsConstant = false
                        val classNameAttribute =
                            annotation.findDeclaredAttributeValue("fieldTypeName")
                        if (classNameAttribute != null) {
                            fieldClass = classNameAttribute.evaluateString() ?: ""
                        } else {
                            val classConstantAttribute =
                                annotation.findDeclaredAttributeValue("fieldType")
                            if (classConstantAttribute is UClassLiteralExpression) {
                                classConstantAttribute.type?.canonicalText?.let {
                                    fieldClass =
                                        if (isKotlin) javaPrimitiveToKotlinPrimitive(it) ?: it
                                        else it
                                    fieldClassIsConstant = true
                                }
                            }
                        }

                        list.add(
                            FieldReflection(
                                className = className,
                                classNameIsConstant = classNameIsConstant,
                                fieldName = fieldName ?: "*",
                                fieldType = fieldClass,
                                fieldTypeIsConstant = fieldClassIsConstant,
                            )
                        )
                    }
                    UNCONDITIONALLY_KEEP_NAME -> {}
                }
            }
        }

        private fun UAnnotation.getClassName(): Pair<String, Boolean> {
            val classNameAttribute = findDeclaredAttributeValue("className")
            if (classNameAttribute != null) {
                val className = classNameAttribute.evaluateString() ?: return "" to false
                return className to false
            }
            val classConstantAttribute = findDeclaredAttributeValue("classConstant")
            if (classConstantAttribute is UClassLiteralExpression) {
                classConstantAttribute.type?.canonicalText?.let {
                    return it to true
                }
            }

            return "" to false
        }

        private fun isApplicableClassName(context: Context, className: String): Boolean {
            if (
                className.startsWith("android.") ||
                    className.startsWith("java.") ||
                    className.startsWith("javax.") ||
                    className.startsWith("dalvik.")
            ) {
                return false
            } else if (className.startsWith("androidx.")) {
                return true
            }

            if (
                className.startsWith("org.") ||
                    className.startsWith("com.android.") ||
                    className.startsWith("com.google.")
            ) {
                // e.g. org.xml.*, org.w3c.dom.*, org.json.*, org.apache.http.*, etc
                val lookup = ApiLookup.get(context.client, context.project.buildTarget)
                if (lookup != null && lookup.containsClass(className)) {
                    return false
                }
            }

            return true
        }

        private fun keepClassAvailable(context: JavaContext, keepTarget: Reflection): Boolean {
            return context.evaluator.findClass(keepTarget.fullName) != null
        }

        private fun createErrorMessage(
            className: String?,
            memberName: String?,
            isFieldLookup: Boolean,
            reflectionUsage: Reflection,
            invocationNode: UElement,
        ): String {
            val sb = StringBuilder()
            sb.append("This ")
            if (invocationNode.getParentOfType<UMethod>() != null) {
                sb.append("method")
            } else {
                sb.append("code")
            }
            sb.append(" ")
            if (isFieldLookup) {
                sb.append("references")
            } else {
                sb.append("calls")
            }
            if (className != null || memberName != null) {
                sb.append(" `")

                if (className != null) {
                    sb.append(className)
                    if (memberName != null) {
                        sb.append('.')
                    }
                }
                if (memberName != null) {
                    sb.append(memberName)
                }
                sb.append("`")
                if (!isFieldLookup && memberName != null) {
                    sb.append("()")
                }
            } else {
                sb.append(" code")
            }
            sb.append(
                " reflectively, so it should be annotated with `@${reflectionUsage.simpleName}(...)`"
            )

            val message = sb.toString()
            return message
        }

        private fun createReferencedMemberFix(
            context: JavaContext,
            isKotlin: Boolean,
            targetMethod: UAnnotated,
            keepTarget: Reflection,
        ): LintFix? {
            val autoFix = keepTarget.canAutoFix()
            if (keepTarget.memberName == "*" || keepTarget.memberName == null) {
                return null
            }

            val annotationSource = keepTarget.generateCode(isKotlin, true)
            val psi = targetMethod.sourcePsi ?: targetMethod.javaPsi
            val fix =
                LintFix.create()
                    .annotate(annotationSource, context, psi, replace = false)
                    .autoFix(autoFix, autoFix)
                    .apply {
                        if (annotationSource.contains("\"TODO\"")) {
                            select("\"(TODO)\"")
                        } else if (annotationSource.contains("TODO()")) {
                            select("(TODO\\(\\))")
                        }
                    }
                    .build()
            return fix
        }

        /**
         * Checks the given [expression] node which is performing reflection on the given
         * [className] and [methodName] for annotations guards, and if not, suggest adding them. A
         * default error message will be provided but can be overridden with [message].
         *
         * Returns true if the potential problem has been handled (e.g. with an existing guard
         * annotation or by issuing a warning.) False in cases where the check doesn't apply, such
         * as missing annotation target or missing keep annotations on the classpath.
         */
        fun checkMethodUsage(
            context: JavaContext,
            expression: UExpression,
            message: String?,
            className: String,
            methodName: String,
            parameterList: List<String>?,
        ): Boolean {
            val annotationTarget = expression.getAnnotationTarget() ?: return false

            val reflection =
                MethodReflection(
                        className = className,
                        classNameIsConstant = false,
                        methodName = methodName,
                        parameterTypes = parameterList,
                        parameterTypesAreStrings = true,
                    )
                    .apply { this.node = expression }

            // Already annotated?
            val isKotlin = context.psiFile is KtFile
            val annotations = getReflectionAnnotations(context, annotationTarget, isKotlin)
            if (annotations.any { it.contains(reflection) }) {
                return true
            }

            if (!keepClassAvailable(context, reflection)) {
                return false
            }

            val message =
                message ?: createErrorMessage(className, methodName, false, reflection, expression)
            val fix = createReferencedMemberFix(context, isKotlin, annotationTarget, reflection)
            context.report(ISSUE, expression, context.getNameLocation(expression), message, fix)
            return true
        }
    }

    override fun getApplicableMethodNames(): List<String> = REFLECTION_CALLS

    private enum class KeepTargetType() {
        METHOD,
        FIELD,
    }

    private sealed class Reflection(var className: String = "", var classNameIsConstant: Boolean) {
        abstract val memberName: String?

        abstract val simpleName: String
        abstract val fullName: String

        open fun canAutoFix(): Boolean = className.isNotEmpty()

        abstract fun generateAttributes(
            isKotlin: Boolean,
            fullyQualified: Boolean,
        ): List<Pair<String, String>>

        abstract fun contains(other: Reflection): Boolean

        protected fun getClassAttribute(isKotlin: Boolean): Pair<String, String> {
            return if (className.isEmpty()) {
                // Couldn't infer name; ask user to fill it in
                if (isKotlin) {
                    "className" to "TODO()"
                } else {
                    "className" to "\"TODO\""
                }
            } else if (classNameIsConstant) {
                "classConstant" to (className + if (isKotlin) "::class" else ".class")
            } else {
                "className" to "\"$className\""
            }
        }

        protected fun getListAttribute(
            name: String,
            isKotlin: Boolean,
            params: List<String>,
            transform: ((String) -> String),
        ): Pair<String, String> {
            val open = if (isKotlin) "[" else "{"
            val close = if (isKotlin) "]" else "}"
            val value = open + params.joinToString(", ", transform = transform) + close
            return name to value
        }

        fun generateCode(isKotlin: Boolean, fullyQualified: Boolean): String {
            return buildString {
                append("@")

                append(if (fullyQualified) fullName else simpleName)
                append("(\n")

                val attributes = generateAttributes(isKotlin, fullyQualified)
                for (i in attributes.indices) {
                    val (name, value) = attributes[i]
                    append("    ").append(name).append(" = ").append(value)
                    if (i < attributes.size - 1) {
                        append(',')
                    }
                    append('\n')
                }
                append(")")
            }
        }

        var node: UExpression? = null
    }

    private class FieldReflection(
        className: String = "",
        classNameIsConstant: Boolean = false,
        var fieldName: String = "",
        var fieldType: String = "",
        var fieldTypeIsConstant: Boolean = false,
    ) : Reflection(className, classNameIsConstant) {
        override val simpleName: String = USES_REFLECTION_TO_ACCESS_FIELD_NAME
        override val fullName: String = USES_REFLECTION_TO_ACCESS_FIELD_FQN

        override val memberName: String?
            get() = fieldName.ifEmpty { null }

        override fun canAutoFix(): Boolean {
            return super.canAutoFix() && fieldName.isNotEmpty()
        }

        override fun generateAttributes(
            isKotlin: Boolean,
            fullyQualified: Boolean,
        ): List<Pair<String, String>> {
            val attributes = mutableListOf<Pair<String, String>>()

            attributes.add(getClassAttribute(isKotlin))
            val name = fieldName.ifEmpty { "*" }
            attributes.add("fieldName" to "\"$name\"")

            if (fieldType.isNotEmpty()) {
                attributes.add(
                    if (fieldTypeIsConstant) {
                        "fieldType" to (fieldType + if (isKotlin) "::class" else ".class")
                    } else {
                        "fieldTypeName" to "\"$fieldType\""
                    }
                )
            }

            return attributes
        }

        override fun contains(other: Reflection): Boolean {
            if (
                className != other.className
                // See MethodReflection.contains for an explanation:
                && other.className.isNotEmpty()
            ) {
                return false
            }
            if (other !is FieldReflection) {
                return false
            }
            if (fieldName != other.fieldName && fieldName != "*" && fieldName != "") {
                return false
            }

            if (
                fieldType.isNotEmpty() &&
                    other.fieldType.isNotEmpty() &&
                    fieldType != other.fieldType
            ) {
                return false
            }

            return true
        }
    }

    private class MethodReflection(
        className: String = "",
        classNameIsConstant: Boolean = false,
        var methodName: String = "",
        var parameterTypes: List<String>? = null,
        var parameterTypesAreStrings: Boolean = true,
        var returnType: String = "",
        var returnTypeIsConstant: Boolean = false,
    ) : Reflection(className, classNameIsConstant) {

        override val simpleName: String
            get() =
                if (isConstructor) USES_REFLECTION_TO_CONSTRUCT_NAME
                else USES_REFLECTION_TO_ACCESS_METHOD_NAME

        override val fullName: String
            get() =
                if (isConstructor) USES_REFLECTION_TO_CONSTRUCT_FQN
                else USES_REFLECTION_TO_ACCESS_METHOD_FQN

        override val memberName: String?
            get() = methodName.ifEmpty { null }

        val isConstructor
            get() = methodName == CONSTRUCTOR_NAME

        override fun canAutoFix(): Boolean {
            return super.canAutoFix() && methodName.isNotEmpty()
        }

        override fun contains(other: Reflection): Boolean {
            if (
                className != other.className
                // In some cases, we cannot figure out the class name during static analysis,
                // for example when it's a type parameter. In this case, we'll still warn with
                // lint, and we'll ask the user to fill in the class name. However, once they
                // do this, we'll keep warning because the class name doesn't match our blank
                // class name from analysis. If the discovered class name is empty, we'll assume
                // it's a match for the one specified here.
                && other.className.isNotEmpty()
            ) {
                return false
            }
            if (other !is MethodReflection) {
                return false
            }
            if (
                methodName != other.methodName &&
                    methodName != "*" &&
                    methodName != ""
                    // Other.methodName = "": we couldn't figure out the method name in analysis.
                    // If the annotation carried a specific name, we'll assume this has been
                    // manually analyzed to cover the specific intended target.
                    && other.methodName != ""
            ) {
                return false
            }

            val params = parameterTypes
            val otherParams = other.parameterTypes
            if (params != null && otherParams != null) {
                if (params.size != otherParams.size) {
                    return false
                }
                for (i in params.indices) {
                    if (params[i] != otherParams[i]) {
                        return false
                    }
                }
            }

            if (
                returnType.isNotEmpty() &&
                    other.returnType.isNotEmpty() &&
                    returnType != other.returnType
            ) {
                return false
            }

            return true
        }

        fun setParameterList(
            context: JavaContext,
            methodParameterTypes: List<UExpression>,
            isKotlin: Boolean,
        ) {
            val list = mutableListOf<String>()
            for (argument in methodParameterTypes) {
                if (!addParameterType(context, argument, list, isKotlin)) {
                    // give up: we can't figure out what the parameter types are; don't
                    // try to set them partially
                    return
                }
            }

            parameterTypes = list
            // Types inferred from source are always recorded as class constants
            // since we know they're on the project class path -- they were already
            // referenced from source
            parameterTypesAreStrings = false
        }

        /**
         * Given a fully qualified name, returns the index where the class name begins. For example,
         * for java.util.Map.Entry this returns the index of the 'M'.
         */
        private fun findClassNameIndex(fqn: String): Int {
            if (fqn.isEmpty()) {
                return -1
            } else if (fqn[0].isUpperCase()) {
                return 0
            } else {
                var index = fqn.lastIndexOf('.')
                while (index != -1) {
                    if (index < fqn.length - 1 && fqn[index + 1].isUpperCase()) {
                        return index + 1
                    }
                    index = fqn.lastIndexOf('.', index - 1)
                }
                return -1
            }
        }

        override fun generateAttributes(
            isKotlin: Boolean,
            fullyQualified: Boolean,
        ): List<Pair<String, String>> {
            val attributes = mutableListOf<Pair<String, String>>()

            attributes.add(getClassAttribute(isKotlin))

            if (!isConstructor) {
                val name = methodName.ifEmpty { "*" }
                attributes.add("methodName" to "\"$name\"")
            }

            if (parameterTypes != null) {
                if (parameterTypesAreStrings) {
                    attributes.add(
                        getListAttribute("parameterTypeNames", isKotlin, parameterTypes!!) {
                            "\"$it\""
                        }
                    )
                } else {
                    attributes.add(
                        getListAttribute("parameterTypes", isKotlin, parameterTypes!!) {
                            // Don't use for example java.lang.String, just use String.
                            var fqn = it
                            val classNameIndex = findClassNameIndex(fqn)
                            if (classNameIndex > 0) {
                                if (
                                    LintFixPerformer.implicitlyImported(
                                        fqn.substring(0, classNameIndex - 1)
                                    ) && !useFullyQualifiedName(fqn)
                                ) {
                                    fqn = fqn.substring(classNameIndex)
                                }
                            }

                            fqn + if (isKotlin) "::class" else ".class"
                        }
                    )
                }
            }

            if (returnType.isNotEmpty()) {
                attributes.add(
                    if (returnTypeIsConstant) {
                        "returnType" to (returnType + if (isKotlin) "::class" else ".class")
                    } else {
                        "returnTypeName" to "\"$returnType\""
                    }
                )
            }

            return attributes
        }
    }

    /**
     * Given a Class#getMethodDeclaration or getFieldDeclaration call, figure out the corresponding
     * class name the method is being invoked on
     *
     * @param call the [Class.getDeclaredMethod] or [Class.getDeclaredField] call
     * @return the fully qualified name of the class, if found
     */
    private fun getJavaClassFromMemberLookup(
        context: JavaContext,
        call: UCallExpression,
    ): Pair<PsiType, Boolean>? = getJavaClassType(context, call.receiver)

    /** We know [element] has type java.lang.Class<T> and we try to find out the PsiType for T. */
    private fun getJavaClassType(
        context: JavaContext,
        element: UElement?,
    ): Pair<PsiType, Boolean>? {
        if (element is UExpression) {
            if (element is UParenthesizedExpression) {
                return getJavaClassType(context, element.expression)
            }

            if (element is UQualifiedReferenceExpression) {
                val selector = element.selector
                if (selector.isJavaClassAccess()) {
                    // We're looking up the dynamic type; we can't compute that here.
                    // Counterpoint: In something like this:
                    //    public void printFieldValues(PrintableFieldInterface objectWithFields)
                    // throws
                    // Exception {
                    //      for (Field field : objectWithFields.getClass().getDeclaredFields()) {
                    // we don't know which potential subclass of PrintableFieldInterface we're going
                    // to get
                    // from getClass,
                    // but it looks like the R8 annotation documentation suggests annotating it with
                    // the interface.

                    val receiverType = element.receiver.getExpressionType()
                    if (receiverType is PsiClassType) {
                        if (element.selector.skipParenthesizedExprDown().isJavaClassAccess()) {
                            // Dynamic getClass() call, such as "myObject.getClass()";
                            // we can't just conclude that it's the type of myObject.
                            return null
                        }

                        return if (receiverType.canonicalText == JAVA_LANG_OBJECT) null
                        else context.evaluator.erasure(receiverType)?.let { Pair(it, true) }
                    }

                    return null
                } else if (selector is UCallExpression) {
                    val subclassType = getAsSubclassType(selector)
                    if (subclassType != null) {
                        return subclassType
                    }
                }
            }

            // First try the type inferred from the Psi, in case it's a known class reference.
            val type = element.getExpressionType()

            if (type is PsiClassType && type.parameterCount == 1) {
                var clazz = type.parameters[0]
                if (clazz is PsiClassType) {
                    val resolved = clazz.resolve()
                    if (resolved == null) {
                        // probably a type parameter; don't use the actual type parameter name.
                        // We should attempt to find the bounds of the type parameter and use
                        // that here (some simple attempts didn't work.)
                        return null
                    }
                }

                val nested = getNestedClassType(clazz)
                if (nested != null) {
                    return nested to true
                }

                if (clazz is PsiClassType) {
                    PsiPrimitiveType.getUnboxedType(clazz)?.let {
                        // Make sure we extract the primitive type (int.class, Integer.TYPE in Java,
                        // Int::class.javaPrimitiveType in Kotlin)
                        if (element is UQualifiedReferenceExpression) {
                            val identifier =
                                (element.selector.skipParenthesizedExprDown()
                                        as? USimpleNameReferenceExpression)
                                    ?.identifier
                            if (identifier == "javaPrimitiveType" || identifier == "TYPE") {
                                clazz = it
                            }
                        }
                        if (
                            element is UClassLiteralExpression &&
                                element.evaluate() is PsiPrimitiveType
                        ) {
                            clazz = it
                        }
                    }

                    return Pair(
                        clazz,
                        element is UClassLiteralExpression ||
                            element is UQualifiedReferenceExpression &&
                                element.resolvedName == "getJavaClass" &&
                                element.receiver is UClassLiteralExpression,
                    )
                }
                // Here we might have a wildcard type, most likely an unbounded Class<?> coming from
                // a loadClass or Class.forName() call. We can also have a bounded <? extends Foo>,
                // from foo.getClass(), but if Foo is not final we cannot statically guarantee the
                // receiver is indeed class Foo. So we fall-through to the handling below.
            }
            if (element is UReferenceExpression) {
                val resolved = element.resolve()
                if (resolved is PsiVariable) {
                    // Follow the indirection and inspect the actual definition
                    UastLintUtils.findLastAssignment(resolved, element)?.let { expression ->
                        return getJavaClassType(context, expression)
                    }
                }

                if (
                    element is UQualifiedReferenceExpression &&
                        element.selector.skipParenthesizedExprDown() is UCallExpression
                ) {
                    val call = element.selector.skipParenthesizedExprDown() as UCallExpression
                    val name = call.methodName

                    if (FOR_NAME == name || LOAD_CLASS == name) {
                        val arguments = call.valueArguments
                        if (arguments.isNotEmpty()) {
                            return ConstantEvaluator.evaluateString(null, arguments[0], false)
                                ?.let {
                                    Pair(
                                        PsiElementFactory.getInstance(context.project.ideaProject)
                                            .createTypeFromText(it, null),
                                        false,
                                    )
                                }
                        }
                    }
                }
            }
        }
        return TypeEvaluator.evaluate(element)?.let { Pair(it, false) }
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator
        val name = method.name

        if (!evaluator.isMemberInClass(method, "java.lang.Class")) {
            return
        }

        val isKotlin = context.psiFile is KtFile

        val annotationTarget = node.getAnnotationTarget() ?: return
        if (name == FOR_NAME || name == LOAD_CLASS) {
            if (hasMemberReflectionCalls(annotationTarget) || node.valueArguments.isEmpty()) {
                // We found a specific reflection call on a member (like getDeclaredMethod,
                // getConstructors,
                // etc.); we'll handle that through a separate visitMethodCall callback.
                return
            }

            var className =
                ConstantEvaluator.evaluateString(context, node.valueArguments.first(), false)
            var classNameIsConstant = false
            if (className == null) {

                var p = skipParenthesizedExprUp(node.uastParent)
                if (
                    p is UQualifiedReferenceExpression &&
                        p.selector.skipParenthesizedExprDown() == node
                ) {
                    // Class.forName wasn't statically imported; p was Class.forName and node
                    // forName; go to its parent
                    p = skipParenthesizedExprUp(p.uastParent)
                }
                if (p is UBinaryExpressionWithType) {
                    val castType = getNestedClassType(p.type)
                    if (castType != null) {
                        className = context.evaluator.erasure(castType)?.canonicalText
                        if (className != null) {
                            classNameIsConstant = true
                        }
                    }
                }
                if (className == null) {
                    if (p is UQualifiedReferenceExpression && p.selector is UCallExpression) {
                        val subclassType = getAsSubclassType(p.selector)
                        if (subclassType != null) {
                            className = context.evaluator.erasure(subclassType.first)?.canonicalText
                            if (className != null) {
                                classNameIsConstant = true
                            }
                        }
                    }
                }
            }

            val reflection =
                MethodReflection(
                        className = className ?: "",
                        classNameIsConstant = classNameIsConstant,
                    )
                    .apply { this.node = node }

            // Already annotated?
            val annotations = getReflectionAnnotations(context, annotationTarget, isKotlin)
            if (annotations.any { it.contains(reflection) }) {
                return
            }

            if (!keepClassAvailable(context, reflection)) {
                return
            }

            val message = createErrorMessage(className, null, false, reflection, node)
            val fix = createReferencedMemberFix(context, isKotlin, annotationTarget, reflection)
            context.report(ISSUE, node, context.getNameLocation(node), message, fix)
            return
        }

        val (classType, classConstant) =
            getJavaClassFromMemberLookup(context, node) ?: Pair(null, false)
        val className = classType?.canonicalText
        if (className != null && !isApplicableClassName(context, className)) {
            return
        }
        // Run a data flow analysis and track what happens to the method *handles* -- whether
        // they're
        // invoked

        val isFieldLookup = name.contains("Field")
        val reflections = mutableListOf<Reflection>()
        val analyzer =
            object : DataFlowAnalyzer(listOf(node)) {
                override fun receiver(call: UCallExpression) {
                    val methodName = call.methodName
                    val type =
                        if (isMethodInvoke(methodName, call)) {
                            KeepTargetType.METHOD
                        } else if (isFieldGet(methodName, call)) {
                            KeepTargetType.FIELD
                        } else {
                            null
                        }
                    if (type != null) {
                        val invokedMethodName =
                            node.valueArguments.firstOrNull()?.let {
                                ConstantEvaluator.evaluateString(context, it, false)
                            }
                        if (invokedMethodName != null) {
                            var typeClass = ""
                            var typeClassIsConstant = false
                            var curr = call.uastParent
                            while (curr != null) {
                                val parent = curr.uastParent
                                if (
                                    parent is UQualifiedReferenceExpression ||
                                        parent is UParenthesizedExpression
                                ) {
                                    curr = parent
                                } else {
                                    if (parent is UBinaryExpressionWithType) {
                                        typeClass =
                                            (context.evaluator.erasure(parent.type)?.canonicalText
                                                    ?: "")
                                                .let {
                                                    if (isKotlin)
                                                        javaPrimitiveToKotlinPrimitive(it) ?: it
                                                    else it
                                                }
                                        if (typeClass.isNotEmpty()) {
                                            typeClassIsConstant = true
                                        }
                                    }
                                    break
                                }
                            }

                            val arguments = node.valueArguments
                            reflections.add(
                                if (type == KeepTargetType.METHOD) {
                                    createMethodReflection(
                                        context,
                                        isKotlin,
                                        call,
                                        classConstant,
                                        className ?: "",
                                        invokedMethodName,
                                        arguments.subList(1, arguments.size),
                                        typeClass,
                                        typeClassIsConstant,
                                    )
                                } else {
                                    createFieldReflection(
                                        call,
                                        classConstant,
                                        className ?: "",
                                        invokedMethodName,
                                        typeClass,
                                        typeClassIsConstant,
                                    )
                                }
                            )
                        }
                    } else if (isConstructorNewInstance(methodName, call)) {
                        reflections.add(
                            createMethodReflection(
                                context,
                                isKotlin,
                                call,
                                classConstant,
                                className ?: "",
                                CONSTRUCTOR_NAME,
                                node.valueArguments,
                            )
                        )
                    } else {
                        val expression = findFilter(methodName, call)
                        if (
                            expression is UBinaryExpression &&
                                (expression.operator == UastBinaryOperator.EQUALS ||
                                    expression.operator == UastBinaryOperator.IDENTITY_EQUALS)
                        ) {
                            val lhs = expression.leftOperand.skipParenthesizedExprDown()
                            val rhs = expression.rightOperand.skipParenthesizedExprDown()
                            val resolved = lhs.tryResolve()
                            val rhsValue = ConstantEvaluator.evaluate(context, rhs)
                            if (
                                rhsValue is String &&
                                    resolved is PsiMethod &&
                                    resolved.name == "getName" &&
                                    resolved.containingClass?.qualifiedName ==
                                        "java.lang.reflect.Method"
                            ) {
                                val access =
                                    if (!isFieldLookup) {
                                        createMethodReflection(
                                            context,
                                            isKotlin,
                                            expression,
                                            classConstant,
                                            className ?: "",
                                            rhsValue,
                                            null,
                                        )
                                    } else {
                                        createFieldReflection(
                                            expression,
                                            classConstant,
                                            className ?: "",
                                            rhsValue,
                                        )
                                    }

                                reflections.add(access)
                                trackAccess(context, isKotlin, annotationTarget, access, call)
                            }
                        }
                    }
                }
            }
        annotationTarget.accept(analyzer)

        if (reflections.isEmpty()) {
            // Didn't find any invocation, so we only looked up the method (or possibly passed
            // it on to a different method).
            when (name) {
                GET_DECLARED_METHOD,
                GET_DECLARED_FIELD,
                GET_METHOD,
                GET_FIELD -> {
                    if (node.valueArguments.isNotEmpty()) {
                        val memberName =
                            ConstantEvaluator.evaluateString(
                                context,
                                node.valueArguments.first(),
                                false,
                            )
                        if (memberName != null) {
                            reflections.add(
                                if (name == GET_DECLARED_METHOD || name == GET_METHOD) {
                                    createMethodReflection(
                                        context,
                                        isKotlin,
                                        node,
                                        classConstant,
                                        className ?: "",
                                        memberName,
                                        emptyList(),
                                    )
                                } else {
                                    createFieldReflection(
                                        node,
                                        classConstant,
                                        className ?: "",
                                        memberName,
                                    )
                                }
                            )
                        }
                    }
                }
                GET_DECLARED_CONSTRUCTOR,
                GET_CONSTRUCTOR -> {
                    reflections.add(
                        createMethodReflection(
                            context,
                            isKotlin,
                            node,
                            classConstant,
                            className ?: "",
                            CONSTRUCTOR_NAME,
                            emptyList(),
                        )
                    )
                }
                GET_DECLARED_CONSTRUCTORS,
                GET_CONSTRUCTORS -> {
                    reflections.add(
                        createMethodReflection(
                            context,
                            isKotlin,
                            node,
                            classConstant,
                            className ?: "",
                            CONSTRUCTOR_NAME,
                            null,
                        )
                    )
                }
                GET_DECLARED_METHODS,
                GET_METHODS -> {
                    reflections.add(
                        createMethodReflection(
                            context,
                            isKotlin,
                            node,
                            classConstant,
                            className ?: "",
                            "*",
                            emptyList(),
                        )
                    )
                }
                GET_DECLARED_FIELDS,
                GET_FIELDS -> {
                    // We looked up ALL methods but failed to figure out filtering on it.
                    // Maybe warn, but without fix
                    reflections.add(
                        createFieldReflection(node, classConstant, className ?: "", "*")
                    )
                }
            }
        }

        for (reflection in reflections) {
            val memberName = reflection.memberName
            val invocationNode = reflection.node ?: continue

            val annotations = getReflectionAnnotations(context, annotationTarget, isKotlin)
            if (annotations.any { it.contains(reflection) }) {
                continue
            }

            if (!keepClassAvailable(context, reflection)) {
                return
            }

            val message =
                createErrorMessage(className, memberName, isFieldLookup, reflection, invocationNode)
            val fix =
                if (className != null || memberName != null) {
                    createReferencedMemberFix(context, isKotlin, annotationTarget, reflection)
                } else {
                    null
                }
            context.report(
                ISSUE,
                invocationNode,
                context.getNameLocation(invocationNode),
                message,
                fix,
            )
        }
    }

    private fun createMethodReflection(
        context: JavaContext,
        isKotlin: Boolean,
        node: UExpression,
        isClassConstant: Boolean,
        className: String,
        methodName: String,
        methodParameterTypes: List<UExpression>?,
        returnClass: String = "",
        returnClassIsConstant: Boolean = false,
    ): MethodReflection {

        return MethodReflection(
                className = className,
                classNameIsConstant = isClassConstant,
                methodName = methodName,
                returnType = returnClass,
                returnTypeIsConstant = returnClassIsConstant,
            )
            .apply {
                this.node = node
                if (methodParameterTypes != null) {
                    setParameterList(context, methodParameterTypes, isKotlin)
                }
            }
    }

    private fun createFieldReflection(
        node: UExpression,
        isClassConstant: Boolean,
        className: String,
        fieldName: String,
        fieldClass: String = "",
        fieldClassIsConstant: Boolean = false,
    ): FieldReflection {

        return FieldReflection(
                className = className,
                classNameIsConstant = isClassConstant,
                fieldName = fieldName,
                fieldType = fieldClass,
                fieldTypeIsConstant = fieldClassIsConstant,
            )
            .apply { this.node = node }
    }

    private fun findFilter(methodName: String?, call: UCallExpression): UExpression? {
        if (call.valueArguments.size != 1) {
            return null
        }
        when (methodName) {
            "first",
            "firstOrNull",
            "find",
            "findLast",
            "single",
            "singleOrNull" -> {
                val lambda =
                    call.valueArguments[0].skipParenthesizedExprDown() as? ULambdaExpression
                        ?: return null
                val qualifiedName = call.resolve()?.containingClass?.qualifiedName
                if (
                    // CLI environment resolve:
                    qualifiedName != "kotlin.collections.ArraysKt___ArraysKt" &&
                        // IDE plugin resolve:
                        qualifiedName != "kotlin.collections.ArraysKt"
                ) {
                    return null
                }
                val bodyExpressions =
                    (lambda.body.skipParenthesizedExprDown() as? UBlockExpression)?.expressions
                        ?: return null
                if (bodyExpressions.size == 1) {
                    val returnExpression =
                        bodyExpressions[0].skipParenthesizedExprDown() as? UReturnExpression
                            ?: return null
                    return returnExpression.returnExpression?.skipParenthesizedExprDown()
                }
            }
        }
        return null
    }

    private fun trackAccess(
        context: JavaContext,
        isKotlin: Boolean,
        annotationTarget: UAnnotated,
        access: Reflection,
        call: UCallExpression,
    ) {
        val analyzer =
            object : DataFlowAnalyzer(listOf(call)) {
                override fun receiver(call: UCallExpression) {
                    val methodName = call.methodName
                    val type =
                        if (isMethodInvoke(methodName, call)) {
                            KeepTargetType.METHOD
                        } else if (isFieldGet(methodName, call)) {
                            KeepTargetType.FIELD
                        } else {
                            null
                        }
                    if (type != null) {
                        val arguments = call.valueArguments
                        access.node = call
                        if (access is MethodReflection) {
                            access.setParameterList(
                                context,
                                arguments.subList(1, arguments.size),
                                isKotlin,
                            )
                        }
                    } else if (isConstructorNewInstance(methodName, call)) {
                        access.node = call
                        if (access is MethodReflection) {
                            access.methodName = CONSTRUCTOR_NAME
                            access.setParameterList(context, call.valueArguments, isKotlin)
                        }
                    }
                }
            }
        annotationTarget.accept(analyzer)
    }

    override fun getApplicableReferenceNames(): List<String> = KOTLIN_REFLECTION_METHODS

    override fun visitReference(
        context: JavaContext,
        reference: UReferenceExpression,
        referenced: PsiElement,
    ) {
        if (referenced !is PsiMethod) {
            return
        }
        val containingClass = referenced.containingClass?.qualifiedName
        if (
            containingClass != "kotlin.reflect.KClass" &&
                containingClass != "kotlin.reflect.full.KClasses"
        ) {
            return
        }

        val isKotlin = true
        val isField = reference.resolvedName?.endsWith("Properties") == true
        val reflection =
            if (isField) {
                createFieldReflection(reference, false, "", "")
            } else {
                createMethodReflection(context, isKotlin, reference, false, "", "", null)
            }

        val annotationTarget = reference.getAnnotationTarget() ?: return
        val annotations = getReflectionAnnotations(context, annotationTarget, isKotlin)
        if (annotations.any { it.contains(reflection) }) {
            return
        }

        if (!keepClassAvailable(context, reflection)) {
            return
        }

        val message = createErrorMessage(null, null, false, reflection, reference)
        val fix = createReferencedMemberFix(context, isKotlin, annotationTarget, reflection)

        context.report(ISSUE, reference, context.getNameLocation(reference), message, fix)
    }

    private fun isConstructorNewInstance(methodName: String?, call: UCallExpression) =
        methodName == "newInstance" &&
            call.resolve()?.containingClass?.qualifiedName == "java.lang.reflect.Constructor"

    private fun isFieldGet(methodName: String?, call: UCallExpression) =
        methodName == "get" &&
            call.resolve()?.containingClass?.qualifiedName == "java.lang.reflect.Field"

    private fun isMethodInvoke(methodName: String?, call: UCallExpression) =
        methodName == "invoke" &&
            call.resolve()?.containingClass?.qualifiedName == "java.lang.reflect.Method"
}

private fun UExpression.getAnnotationTarget(): UAnnotated? {
    return getParentOfType(true, UMethod::class.java, UField::class.java, UClass::class.java)
}
