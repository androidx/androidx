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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XPackageElement
import androidx.room.compiler.processing.javac.kotlin.KmFlags
import java.lang.UnsupportedOperationException
import javax.lang.model.element.PackageElement

internal class JavacPackageElement(
    env: JavacProcessingEnv,
    private val packageElement: PackageElement
) : JavacElement(env, packageElement), XPackageElement {
    override val qualifiedName: String by lazy {
        packageElement.qualifiedName.toString()
    }
    override val kotlinMetadata: KmFlags?
        get() = null
    override val name: String by lazy {
        packageElement.simpleName.toString()
    }
    override val fallbackLocationText: String
        get() = qualifiedName
    override val enclosingElement: XElement?
        get() = null
    override val closestMemberContainer: XMemberContainer
        get() = throw UnsupportedOperationException("Packages don't have a closestMemberContainer" +
            " as we don't consider packages a member container for now and it" +
            " has no enclosingElement.")
}
