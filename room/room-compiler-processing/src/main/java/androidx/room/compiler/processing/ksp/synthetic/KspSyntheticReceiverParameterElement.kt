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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.KspAnnotated
import androidx.room.compiler.processing.ksp.KspJvmTypeResolutionScope
import androidx.room.compiler.processing.ksp.KspMethodElement
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import com.google.devtools.ksp.symbol.KSTypeReference

internal class KspSyntheticReceiverParameterElement(
    val env: KspProcessingEnv,
    override val enclosingMethodElement: KspMethodElement,
    val receiverType: KSTypeReference,
) : XExecutableParameterElement,
    XEquality,
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = null, // does not matter, this is synthetic and has no annotations.
        filter = KspAnnotated.UseSiteFilter.NO_USE_SITE
    ) {

    override val name: String by lazy {
        // KAPT uses `$this$<functionName>`
        "$" + "this" + "$" + enclosingMethodElement.name
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(enclosingMethodElement, receiverType)
    }

    override val hasDefaultValue: Boolean
        get() = false

    private val jvmTypeResolutionScope by lazy {
        KspJvmTypeResolutionScope.MethodParameter(
            kspExecutableElement = enclosingMethodElement,
            parameterIndex = 0, // Receiver param is the 1st one
            annotated = enclosingMethodElement.declaration
        )
    }

    override val type: XType by lazy {
        env.wrap(receiverType).withJvmTypeResolver(jvmTypeResolutionScope)
    }

    override val fallbackLocationText: String
        get() = "receiver parameter of ${enclosingMethodElement.fallbackLocationText}"

    // Not applicable
    override val docComment: String? get() = null

    override fun asMemberOf(other: XType): KspType {
        check(other is KspType)
        val asMemberReceiverType = receiverType.resolve().let {
            if (it.isError) {
                return@let it
            }
            val asMember = enclosingMethodElement.declaration.asMemberOf(other.ksType)
            checkNotNull(asMember.extensionReceiverType)
        }
        return env.wrap(
            originatingReference = receiverType,
            ksType = asMemberReceiverType,
        ).withJvmTypeResolver(jvmTypeResolutionScope)
    }

    override fun kindName(): String {
        return "synthetic receiver parameter"
    }

    override fun validate(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }
}
