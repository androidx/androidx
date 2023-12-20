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

import androidx.collection.MutableObjectList
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ObjectList
import androidx.collection.ScatterMap
import androidx.collection.ScatterSet
import androidx.collection.scatterSetOf
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.impl.source.PsiClassReferenceType
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

/**
 * Using [ScatterMap.asMap], [ScatterSet.asSet], [ObjectList.asList], or their mutable
 * counterparts indicates that the developer may be using the collection incorrectly.
 * Using the interfaces is slower access. It is best to use those only for when it touches
 * public API.
 */
class AsCollectionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf<Class<out UElement>>(
        UCallExpression::class.java
    )

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val methodName = node.methodName ?: return
            if (methodName in MethodNames) {
                val receiverType = node.receiverType as? PsiClassReferenceType ?: return
                val qualifiedName = receiverType.reference.qualifiedName ?: return
                val indexOfAngleBracket = qualifiedName.indexOf('<')
                if (indexOfAngleBracket > 0 &&
                    qualifiedName.substring(0, indexOfAngleBracket) in CollectionClasses
                ) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Use method $methodName() only for public API usage"
                    )
                }
            }
        }
    }

    companion object {
        private val MethodNames = scatterSetOf(
            "asMap",
            "asMutableMap",
            "asSet",
            "asMutableSet",
            "asList",
            "asMutableList"
        )
        private val CollectionClasses = scatterSetOf(
            ScatterMap::class.qualifiedName,
            MutableScatterMap::class.qualifiedName,
            ScatterSet::class.qualifiedName,
            MutableScatterSet::class.qualifiedName,
            ObjectList::class.qualifiedName,
            MutableObjectList::class.qualifiedName,
        )

        private val AsCollectionDetectorId = "AsCollectionCall"

        val ISSUE = Issue.create(
            id = AsCollectionDetectorId,
            briefDescription = "High performance collections don't implement standard collection " +
                "interfaces so that they can remain high performance. Converting to standard " +
                "collections wraps the classes with another object. Use these interface " +
                "wrappers only for exposing to public API.",
            explanation = "ScatterMap, ScatterSet, and AnyList are written for high " +
                "performance access. Using the standard collection interfaces for these classes " +
                "forces slower performance access to these collections. The methods returning " +
                "these interfaces should be limited to public API, where standard collection " +
                "interfaces are expected.",
            category = Category.PERFORMANCE, priority = 3, severity = Severity.ERROR,
            implementation = Implementation(
                AsCollectionDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }
}
