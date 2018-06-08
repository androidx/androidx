/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.ext

import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.KotlinMetadataUtils
import me.eugeniomarletti.kotlin.metadata.jvm.getJvmConstructorSignature
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.serialization.deserialization.getName
import javax.lang.model.element.ExecutableElement

/**
 * Utility interface for processors that wants to run kotlin specific code.
 */
interface KotlinMetadataProcessor : KotlinMetadataUtils {
    /**
     * Returns the parameter names of the function if all have names embedded in the metadata.
     */
    fun KotlinClassMetadata.getParameterNames(method: ExecutableElement): List<String>? {
        val valueParameterList = this.data.getFunctionOrNull(method)?.valueParameterList
                ?: findConstructor(method)?.valueParameterList
                ?: return null
        return if (valueParameterList.all { it.hasName() }) {
            valueParameterList.map {
                data.nameResolver.getName(it.name)
                        .asString()
                        .replace("`", "")
                        .removeSuffix("?")
                        .trim()
            }
        } else {
            null
        }
    }

    /**
     * Finds the kotlin metadata for a constructor.
     */
    private fun KotlinClassMetadata.findConstructor(
            executableElement: ExecutableElement
    ): ProtoBuf.Constructor? {
        val (nameResolver, classProto) = data
        val jvmSignature = executableElement.jvmMethodSignature
        // find constructor
        return classProto.constructorList.singleOrNull {
            it.getJvmConstructorSignature(nameResolver, classProto.typeTable) == jvmSignature
        }
    }
}