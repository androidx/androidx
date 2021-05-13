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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XEquality
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.util.Locale

internal abstract class KspElement(
    protected val env: KspProcessingEnv,
    open val declaration: KSAnnotated
) : XElement, XEquality {
    override fun kindName(): String {
        return when (declaration) {
            is KSClassDeclaration ->
                (declaration as KSClassDeclaration).classKind.name
                    .toLowerCase(Locale.US)
            is KSPropertyDeclaration -> "property"
            is KSFunctionDeclaration -> "function"
            else -> declaration::class.simpleName ?: "unknown"
        }
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return declaration.toString()
    }

    /**
     * Return a reference to the containing file that implements the
     * [javax.lang.model.element.Element] API so that we can report it to JavaPoet.
     */
    fun containingFileAsOriginatingElement(): KSFileAsOriginatingElement? {
        return (declaration as? KSDeclaration)?.containingFile?.let {
            KSFileAsOriginatingElement(it)
        }
    }

    override val docComment: String? by lazy {
        // TODO: Not yet implemented in KSP.
        // https://github.com/google/ksp/issues/392
        null
    }
}