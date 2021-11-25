/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.isKotlin
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

class InvalidDaoMethodDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UClass::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {

        override fun visitClass(node: UClass) {
            if (!isKotlin(node.sourcePsi)) {
                return
            }
            if (!node.hasDaoAnnotation()) {
                return
            }
            val daoSuspendMethods = node.methods.filter { method ->
                method.isDaoMethod() && context.evaluator.isSuspend(method)
            }
            daoSuspendMethods.forEach { method ->
                if (method.hasRxJavaContinuationType()) {
                    context.report(ISSUE, context.getNameLocation(method), DESCRIPTION)
                }
            }
        }

        private fun UClass.hasDaoAnnotation(): Boolean {
            return uAnnotations.any { it.qualifiedName == DAO_ANNOTATION }
        }

        private fun UMethod.isDaoMethod(): Boolean {
            return uAnnotations.any { it.qualifiedName in DAO_METHOD_ANNOTATIONS }
        }

        private fun UMethod.hasRxJavaContinuationType(): Boolean {
            return uastParameters.any {
                it.typeReference?.type?.canonicalText in CONTINUATION_NAMES
            }
        }
    }

    companion object {
        private val DESCRIPTION = """
            Invalid Dao method.
        """.trimIndent()

        @JvmField
        val ISSUE = Issue.create(
            id = "InvalidDaoMethod",
            briefDescription = DESCRIPTION,
            explanation = """
                Dao methods that have a suspend modifier shouldn't return an RxJava type.
                Most probably this is an error. Consider changing the return type or \
                removing the suspend modifier.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                InvalidDaoMethodDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        private const val DAO_ANNOTATION = "androidx.room.Dao"

        private val DAO_METHOD_ANNOTATIONS = setOf(
            "androidx.room.Update",
            "androidx.room.Delete",
            "androidx.room.Insert",
            "androidx.room.Query",
            "androidx.room.RawQuery",
            "androidx.room.Transaction"
        )

        private val CONTINUATION_NAMES = setOf(
            // RxJava2 types
            "kotlin.coroutines.Continuation<? super io.reactivex.Flowable>",
            "kotlin.coroutines.Continuation<? super io.reactivex.Observable>",
            "kotlin.coroutines.Continuation<? super io.reactivex.Maybe>",
            "kotlin.coroutines.Continuation<? super io.reactivex.Single>",
            "kotlin.coroutines.Continuation<? super io.reactivex.Completable>",
            // RxJava3 types
            "kotlin.coroutines.Continuation<? super io.reactivex.rxjava3.core.Flowable>",
            "kotlin.coroutines.Continuation<? super io.reactivex.rxjava3.core.Observable>",
            "kotlin.coroutines.Continuation<? super io.reactivex.rxjava3.core.Maybe>",
            "kotlin.coroutines.Continuation<? super io.reactivex.rxjava3.core.Single>",
            "kotlin.coroutines.Continuation<? super io.reactivex.rxjava3.core.Completable>",
        )
    }
}
