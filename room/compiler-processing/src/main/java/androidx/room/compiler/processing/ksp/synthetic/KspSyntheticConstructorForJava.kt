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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.KspAnnotated
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspTypeElement

/**
 * KSP does not create any constructor for java classes that do not have explicit constructor so
 * we synthesize one.
 *
 * see: https://github.com/google/ksp/issues/98
 */
internal class KspSyntheticConstructorForJava(
    val env: KspProcessingEnv,
    val origin: KspTypeElement
) : XConstructorElement,
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = null, // there is no way to annotate this synthetic in kotlin
        filter = KspAnnotated.UseSiteFilter.NO_USE_SITE
    ) {

    override val enclosingTypeElement: XTypeElement
        get() = origin

    override val parameters: List<XExecutableParameterElement>
        get() = emptyList()

    override fun isVarArgs() = false

    override fun isPublic() = origin.isPublic()

    override fun isProtected() = origin.isProtected()

    override fun isAbstract() = false

    override fun isPrivate() = origin.isPrivate()

    override fun isStatic() = false

    override fun isTransient() = false

    override fun isFinal() = origin.isFinal()

    override fun kindName(): String {
        return "synthetic java constructor"
    }
}