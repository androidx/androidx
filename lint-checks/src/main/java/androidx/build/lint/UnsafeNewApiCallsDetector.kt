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
package androidx.build.lint
import com.android.SdkConstants.ATTR_VALUE
import com.android.SdkConstants.DOT_JAVA
import com.android.tools.lint.checks.ApiDetector.Companion.REQUIRES_API_ANNOTATION
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.checks.ApiLookup
import com.android.tools.lint.checks.ApiLookup.equivalentName
import com.android.tools.lint.checks.ApiLookup.startsWithEquivalentPrefix
import com.android.tools.lint.checks.VersionChecks.Companion.codeNameToApi
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Desugaring
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.UastLintUtils.Companion.getLongAttribute
import com.android.tools.lint.detector.api.getInternalMethodName
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UInstanceExpression
import org.jetbrains.uast.USuperExpression
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.java.JavaUAnnotation
import org.jetbrains.uast.util.isConstructorCall
import org.jetbrains.uast.util.isMethodCall
/**
 * This detects usages of a platform api that are not within a class annotated with RequiresApi(x)
 * where x is equal or higher to that api. It is to encourage developers to move calls to apis
 * higher than minSdk to be within a specialized annotated class (classes with names
 * traditionally ending with ....ApiXImpl.
 */
class UnsafeNewApiCallsDetector : Detector(), SourceCodeScanner {
    private var apiDatabase: ApiLookup? = null
    override fun beforeCheckEachProject(context: Context) {
        apiDatabase = ApiLookup.get(context.client, context.project.buildTarget)
    }
    override fun afterCheckEachProject(context: Context) {
        apiDatabase = null
    }
    override fun createUastHandler(context: JavaContext): UElementHandler? {
        if (apiDatabase == null) {
            return null
        }
        return ApiVisitor(context)
    }
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)
    // Consider making this a top class and pass in apiDatabase explicitly.
    private inner class ApiVisitor(private val context: JavaContext) : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val method = node.resolve()
            if (method != null) {
                visitCall(method, node, node)
            }
        }
        private fun visitCall(
            method: PsiMethod,
            call: UCallExpression?,
            reference: UElement
        ) {
            if (call == null) {
                return
            }
            val apiDatabase = apiDatabase ?: return
            val containingClass = method.containingClass ?: return
            val evaluator = context.evaluator
            val owner = evaluator.getQualifiedName(containingClass)
                ?: return // Couldn't resolve type
            if (!apiDatabase.containsClass(owner)) {
                return
            }
            val name = getInternalMethodName(method)
            val desc = evaluator.getMethodDescription(
                method,
                false,
                false
            ) // Couldn't compute description of method for some reason; probably
                // failure to resolve parameter types
                ?: return
            var api = apiDatabase.getMethodVersion(owner, name, desc)
            if (api == NO_API_REQUIREMENT) {
                return
            }
            if (api <= context.project.minSdk) {
                return
            }
            if (call.isMethodCall()) {
                val qualifier = call.receiver
                if (qualifier != null &&
                    qualifier !is UThisExpression &&
                    qualifier !is PsiSuperExpression
                ) {
                    val receiverType = qualifier.getExpressionType()
                    if (receiverType is PsiClassType) {
                        val containingType = context.evaluator.getClassType(containingClass)
                        val inheritanceChain =
                            getInheritanceChain(receiverType, containingType)
                        if (inheritanceChain != null) {
                            for (type in inheritanceChain) {
                                val expressionOwner = evaluator.getQualifiedName(type)
                                if (expressionOwner != null && expressionOwner != owner) {
                                    val specificApi = apiDatabase.getMethodVersion(
                                        expressionOwner, name, desc
                                    )
                                    if (specificApi == NO_API_REQUIREMENT) {
                                        if (apiDatabase.isRelevantOwner(expressionOwner)) {
                                            return
                                        }
                                    } else if (specificApi <= context.project.minSdk) {
                                        return
                                    } else {
                                        // For example, for Bundle#getString(String,String) the
                                        // API level is 12, whereas for BaseBundle#getString
                                        // (String,String) the API level is 21. If the code
                                        // specified a Bundle instead of a BaseBundle, reported
                                        // the Bundle level in the error message instead.
                                        if (specificApi < api) {
                                            api = specificApi
                                        }
                                        api = Math.min(specificApi, api)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Unqualified call; need to search in our super hierarchy
                    // Unfortunately, expression.getReceiverType() does not work correctly
                    // in Java; it returns the type of the static binding of the call
                    // instead of giving the virtual dispatch type, as described in
                    // https://issuetracker.google.com/64528052 (and covered by
                    // for example ApiDetectorTest#testListView). Therefore, we continue
                    // to use the workaround method for Java (which isn't correct, and is
                    // particularly broken in Kotlin where the dispatch needs to take into
                    // account top level functions and extension methods), and then we use
                    // the correct receiver type in Kotlin.
                    var cls: PsiClass? = null
                    if (context.file.path.endsWith(DOT_JAVA)) {
                        cls = call.getContainingUClass()?.javaPsi
                    } else {
                        val receiverType = call.receiverType
                        if (receiverType is PsiClassType) {
                            cls = receiverType.resolve()
                        }
                    }
                    if (qualifier is UThisExpression || qualifier is USuperExpression) {
                        val pte = qualifier as UInstanceExpression
                        val resolved = pte.resolve()
                        if (resolved is PsiClass) {
                            cls = resolved
                        }
                    }
                    while (cls != null) {
                        if (cls is PsiAnonymousClass) {
                            // If it's an unqualified call in an anonymous class, we need to
                            // rely on the resolve method to find out whether the method is
                            // picked up from the anonymous class chain or any outer classes
                            var found = false
                            val anonymousBaseType = cls.baseClassType
                            val anonymousBase = anonymousBaseType.resolve()
                            if (anonymousBase != null && anonymousBase.isInheritor(
                                    containingClass,
                                    true
                                )
                            ) {
                                cls = anonymousBase
                                found = true
                            } else {
                                val surroundingBaseType =
                                    PsiTreeUtil.getParentOfType(cls, PsiClass::class.java, true)
                                if (surroundingBaseType != null && surroundingBaseType.isInheritor(
                                        containingClass,
                                        true
                                    )
                                ) {
                                    cls = surroundingBaseType
                                    found = true
                                }
                            }
                            if (!found) {
                                break
                            }
                        }
                        val expressionOwner = evaluator.getQualifiedName(cls)
                        if (expressionOwner == null || equivalentName(
                                expressionOwner,
                                "java/lang/Object"
                            )
                        ) {
                            break
                        }
                        val specificApi =
                            apiDatabase.getMethodVersion(expressionOwner, name, desc)
                        if (specificApi == NO_API_REQUIREMENT) {
                            if (apiDatabase.isRelevantOwner(expressionOwner)) {
                                break
                            }
                        } else if (specificApi <= context.project.minSdk) {
                            return
                        } else {
                            if (specificApi < api) {
                                api = specificApi
                            }
                            api = Math.min(specificApi, api)
                            break
                        }
                        cls = cls.superClass
                    }
                }
            }
            if (call.isMethodCall()) {
                val receiver = call.receiver
                var target: PsiClass? = null
                if (!method.isConstructor) {
                    if (receiver != null) {
                        val type = receiver.getExpressionType()
                        if (type is PsiClassType) {
                            target = type.resolve()
                        }
                    } else {
                        target = call.getContainingUClass()?.javaPsi
                    }
                }
                // Look to see if there's a possible local receiver
                if (target != null) {
                    val methods = target.findMethodsBySignature(method, true)
                    if (methods.size > 1) {
                        for (m in methods) {
                            if (!method.isEquivalentTo(m)) {
                                val provider = m.containingClass
                                if (provider != null) {
                                    val methodOwner = evaluator.getQualifiedName(provider)
                                    if (methodOwner != null) {
                                        val methodApi = apiDatabase.getMethodVersion(
                                            methodOwner, name, desc
                                        )
                                        if (methodApi == NO_API_REQUIREMENT ||
                                            methodApi <= context.project.minSdk
                                        ) {
                                            // Yes, we found another call that doesn't have an
                                            // API requirement
                                            return
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // If you're simply calling super.X from method X, even if method X is in a higher
                // API level than the minSdk, we're generally safe; that method should only be
                // called by the framework on the right API levels. (There is a danger of somebody
                // calling that method locally in other contexts, but this is hopefully unlikely.)
                if (receiver is USuperExpression) {
                    val containingMethod = call.getContainingUMethod()?.javaPsi
                    if (containingMethod != null &&
                        name == containingMethod.name &&
                        evaluator.areSignaturesEqual(method, containingMethod) &&
                        // We specifically exclude constructors from this check, because we
                        // do want to flag constructors requiring the new API level; it's
                        // highly likely that the constructor is called by local code so
                        // you should specifically investigate this as a developer
                        !method.isConstructor
                    ) {
                        return
                    }
                }
                // If it's a method we have source for, obviously it shouldn't be a
                // violation, happens in androidx (appcompat?)
                if (method !is PsiCompiledElement) {
                    return
                }
            }
            // Desugar rewrites compare calls (see b/36390874)
            if (name == "compare" &&
                api == 19 &&
                startsWithEquivalentPrefix(owner, "java/lang/") &&
                desc.length == 4 &&
                context.project.isDesugaring(Desugaring.LONG_COMPARE) &&
                (
                    desc == "(JJ)" ||
                        desc == "(ZZ)" ||
                        desc == "(BB)" ||
                        desc == "(CC)" ||
                        desc == "(II)" ||
                        desc == "(SS)"
                    )
            ) {
                return
            }
            // Desugar rewrites Objects.requireNonNull calls (see b/32446315)
            if (name == "requireNonNull" &&
                api == 19 &&
                owner == "java.util.Objects" &&
                desc == "(Ljava.lang.Object;)" &&
                context.project.isDesugaring(Desugaring.OBJECTS_REQUIRE_NON_NULL)
            ) {
                return
            }
            if (name == "addSuppressed" &&
                api == 19 &&
                owner == "java.lang.Throwable" &&
                desc == "(Ljava.lang.Throwable;)" &&
                context.project.isDesugaring(Desugaring.TRY_WITH_RESOURCES)
            ) {
                return
            }
            val nameIdentifier = call.methodIdentifier
            val location = if (call.isConstructorCall() &&
                call.classReference != null
            ) {
                context.getRangeLocation(call, 0, call.classReference!!, 0)
            } else if (nameIdentifier != null) {
                context.getLocation(nameIdentifier)
            } else {
                context.getLocation(reference)
            }
            if (call.getContainingUClass() == null) {
                // Can't verify if containing class is annotated with @RequiresApi
                return
            }
            val potentialRequiresApiVersion = getRequiresApiFromAnnotations(
                call
                    .getContainingUClass()!!.javaPsi
            )
            if (potentialRequiresApiVersion == NO_API_REQUIREMENT ||
                api > potentialRequiresApiVersion
            ) {
                val containingClassName = call.getContainingUClass()!!.qualifiedName.toString()
                context.report(
                    ISSUE, reference, location,
                    "This call is to a method from API $api, the call containing " +
                        "class $containingClassName is not annotated with " +
                        "@RequiresApi(x) where x is at least $api. Either annotate the " +
                        "containing class with at least @RequiresApi($api) or move the " +
                        "call to a static method in a wrapper class annotated with at " +
                        "least @RequiresApi($api)."
                )
            }
        }
        private fun getInheritanceChain(
            derivedClass: PsiClassType,
            baseClass: PsiClassType?
        ): List<PsiClassType>? {
            if (derivedClass == baseClass) {
                return emptyList()
            }
            val chain = getInheritanceChain(derivedClass, baseClass, HashSet(), 0)
            chain?.reverse()
            return chain
        }
        private fun getInheritanceChain(
            derivedClass: PsiClassType,
            baseClass: PsiClassType?,
            visited: HashSet<PsiType>,
            depth: Int
        ): MutableList<PsiClassType>? {
            if (derivedClass == baseClass) {
                return ArrayList(depth)
            }
            for (type in derivedClass.superTypes) {
                if (visited.add(type) && type is PsiClassType) {
                    val chain = getInheritanceChain(type, baseClass, visited, depth + 1)
                    if (chain != null) {
                        chain.add(derivedClass)
                        return chain
                    }
                }
            }
            return null
        }
        private fun getRequiresApiFromAnnotations(modifierListOwner: PsiModifierListOwner): Int {
            for (annotation in context.evaluator.getAllAnnotations(modifierListOwner, false)) {
                val qualifiedName = annotation.qualifiedName
                if (REQUIRES_API_ANNOTATION.isEquals(qualifiedName)) {
                    val wrapped = JavaUAnnotation.wrap(annotation)
                    var api = getLongAttribute(
                        context, wrapped,
                        ATTR_VALUE, NO_API_REQUIREMENT.toLong()
                    ).toInt()
                    if (api <= 1) {
                        // @RequiresApi has two aliasing attributes: api and value
                        api = getLongAttribute(context, wrapped, "api", NO_API_REQUIREMENT.toLong())
                            .toInt()
                    }
                    return api
                } else if (qualifiedName == null) {
                    // Work around UAST type resolution problems
                    // Work around bugs in UAST type resolution for file annotations:
                    // parse the source string instead.
                    if (annotation is PsiCompiledElement) {
                        continue
                    }
                    val text = annotation.text
                    if (text.contains("RequiresApi(")) {
                        val start = text.indexOf('(')
                        val end = text.indexOf(')', start + 1)
                        if (end != -1) {
                            var name = text.substring(start + 1, end)
                            // Strip off attribute name and qualifiers, e.g.
                            //   @RequiresApi(api = Build.VERSION.O) -> O
                            var index = name.indexOf('=')
                            if (index != -1) {
                                name = name.substring(index + 1).trim()
                            }
                            index = name.indexOf('.')
                            if (index != -1) {
                                name = name.substring(index + 1)
                            }
                            if (!name.isEmpty()) {
                                if (name[0].isDigit()) {
                                    val api = Integer.parseInt(name)
                                    if (api > 0) {
                                        return api
                                    }
                                } else {
                                    return codeNameToApi(name)
                                }
                            }
                        }
                    }
                }
            }
            return NO_API_REQUIREMENT
        }
    }
    companion object {
        const val NO_API_REQUIREMENT = -1
        val ISSUE = Issue.create(
            "UnsafeNewApiCall",
            "Calling method with API level higher than minSdk outside a " +
                "@RequiresApi class or with insufficient required API.",
            """
                Even though wrapping a call to a method from an API above minSdk
                inside an SDK_INT check makes it runtime safe, it is not optimal. When
                ART tries to optimize a class, it will do so regardless of the execution
                path, and will fail if it tries to resolve a method at a higher API if
                that method is being referenced
                somewhere in the class, even if that method would never be called at runtime
                due to the SDK_INT check. ART will however only try to optimize a class the
                first time it's referenced at runtime, this means if we wrap our above
                minSdk method calls inside classes that are only referenced at runtime at
                the appropriate API level, then we guarantee the ablity to resolve all the
                methods. To enforce this we require that all references to methods above
                minSdk are made inside classes that are annotated with RequiresApi(x) where
                x is at least the api at which the methods becomes available.
                For example if our minSdk is 14, and framework method a.x(params...) is
                available starting sdk 16, then creating the following example class is
                considered good practice:
                @RequiresApi(16)
                private static class Api16Impl{
                  static void callX(params...) {
                    a.x(params...);
                  }
                }
                The call site is changed from a.x(params...) to Api16Impl.callX(params).
                Since ART will only try to optimize Api16Impl when it's on the execution
                path, we are guaranteed to have a.x(...) available.
                In addition, shrinkers like r8/proguard may inline the method in the separate
                class and replace the wrapper call with the actual call, so you may have to disable
                inlining the class by using a proguard rule. The following is an example of how to
                disable inlining methods from Impl classes inside the WindowInsetsCompat class:
                -keepclassmembernames,allowobfuscation,allowshrinking class 
                    androidx.core.view.WindowInsetsCompat${'$'}*Impl* {
                  <methods>;
                }
                This will still allow them to be removed, but if they are kept, they will not be
                inlined.
                Failure to do the above may result in overall performance degradation.
            """,
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(UnsafeNewApiCallsDetector::class.java, Scope.JAVA_FILE_SCOPE)
        ).setAndroidSpecific(true)
    }
}
