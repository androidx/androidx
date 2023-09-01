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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XPackageElement
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSAnnotation
import java.lang.UnsupportedOperationException

// This is not a KspElement as we don't have a backing model in KSP.
internal class KspPackageElement(
    env: KspProcessingEnv,
    private val packageName: String
) : KspAnnotated(env), XPackageElement {

    override val qualifiedName: String by lazy {
        packageName.getNormalizedPackageName()
    }

    override val name: String by lazy {
        qualifiedName.substringAfterLast(".")
    }

    override fun kindName(): String = "package"

    override val fallbackLocationText: String
        get() = qualifiedName

    override val docComment: String? = null

    override fun validate(): Boolean = true

    override val enclosingElement: XElement? = null
    override val closestMemberContainer: XMemberContainer
        get() = throw UnsupportedOperationException(
            "Packages don't have a closestMemberContainer as we don't consider packages " +
                "a member container for now and it has no enclosingElement.")

    @OptIn(KspExperimental::class)
    override fun annotations(): Sequence<KSAnnotation> {
        return env.resolver.getPackageAnnotations(qualifiedName)
    }
}
