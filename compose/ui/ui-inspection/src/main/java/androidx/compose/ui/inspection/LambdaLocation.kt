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
package androidx.compose.ui.inspection

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.inspection.framework.classNameFromSignature

private const val SourceLocationAware = "com.android.tools.deploy.liveedit.SourceLocationAware"
private const val SourceLocationMethodName = "getSourceLocationInfo"
private const val LambdaAnnotation = "com.android.tools.r8.annotations.LambdaMethod"
private val SELECTOR_EXPR = Regex("(\\\$(lambda-)?[0-9]+)+$")

data class LambdaLocation(
    val lambdaClassName: String,
    val fileName: String,
    val startLine: Int,
    val endLine: Int,
) {

    @Suppress("unused") // Called from JNI
    constructor(
        clazz: Class<*>,
        fileName: String,
        startLine: Int,
        endLine: Int,
    ) : this(clazz.name, fileName, startLine, endLine)

    val packageName: String
        get() = lambdaClassName.substringBeforeLast(".")

    val lambdaName: String
        get() = findLambdaSelector(lambdaClassName)

    companion object {
        init {
            // TODO(b/179314197): Can we avoid try/catch by setting up by...
            //  - linking differently?
            //  - making sure previous classloader that loaded this was GC'ed
            //  - Searching list of already loaded libraries?
            try {
                System.loadLibrary("compose_inspection_jni")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(
                    SPAM_LOG_TAG,
                    "Swallowing loadLibrary exception. Already loaded by a previous classloader?",
                    e,
                )
            }
        }

        fun resolve(o: Any): LambdaLocation? {
            return resolveWithLiveEdit(o) ?: resolveLambda(o)
        }

        /**
         * Return the lambda selector from the [lambdaClassName].
         *
         * Example:
         * - className:
         *   com.example.composealertdialog.ComposableSingletons$MainActivityKt$lambda-10$1$2$2$1
         * - selector: lambda-10$1$2$2$1
         */
        @VisibleForTesting
        fun findLambdaSelector(lambdaClassName: String): String =
            SELECTOR_EXPR.find(lambdaClassName)?.value?.substring(1) ?: ""

        /** Resolve the location of a given lambda instance. */
        @SuppressLint("BanUncheckedReflection")
        private fun resolveLambda(lambda: Any): LambdaLocation? {
            val annotation =
                lambda.javaClass.annotations.firstOrNull {
                    it.annotationClass.qualifiedName == LambdaAnnotation
                }
            if (annotation == null) {
                // The lambda must have been generated with -Xlambdas=class
                return resolveWithJvmTI(lambda.javaClass, "")
            }
            // The lambda must have been generated with -Xlambdas=indy
            // Extract the bridge class and private method name from the d8 annotation.
            try {
                val clazz = annotation.javaClass
                val holder = clazz.getMethod("holder").invoke(annotation) as? String ?: return null
                val method = clazz.getMethod("method").invoke(annotation) as? String ?: return null
                val className = classNameFromSignature(holder)
                val holderClass = lambda.javaClass.classLoader?.loadClass(className) ?: return null
                return resolveWithJvmTI(holderClass, method)
            } catch (ex: Throwable) {
                Log.w(SPAM_LOG_TAG, "Could not locate lambda", ex)
                return null
            }
        }

        /** Extract the LambdaLocation from the Live Edit class. */
        private fun resolveWithLiveEdit(lambda: Any): LambdaLocation? {
            val location = lambda.getSourceLineLocationInfo() ?: return null
            val internalName = location["lambda"] as? String ?: return null
            val fileName = location["file"] as? String ?: return null
            val startLine = location["startLine"] as? Int ?: return null
            val endLine = location["endLine"] as? Int ?: return null
            return LambdaLocation(internalName, fileName, startLine, endLine)
        }

        @SuppressLint("BanUncheckedReflection")
        private fun Any.getSourceLineLocationInfo(): Map<*, *>? {
            try {
                val iAware = javaClass.interfaces.find { it.name == SourceLocationAware }
                val method = iAware?.getDeclaredMethod(SourceLocationMethodName)
                method?.isAccessible = true
                return method?.invoke(this) as? Map<*, *>
            } catch (_: Exception) {
                return null
            }
        }

        @JvmStatic
        private external fun resolveWithJvmTI(clazz: Class<*>, methodName: String): LambdaLocation?
    }
}
