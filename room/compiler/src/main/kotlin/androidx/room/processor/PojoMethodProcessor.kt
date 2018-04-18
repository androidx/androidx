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

package androidx.room.processor

import androidx.room.vo.PojoMethod
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

/**
 * processes an executable element as member of the owning class
 */
class PojoMethodProcessor(
        private val context: Context,
        private val element: ExecutableElement,
        private val owner: DeclaredType) {
    fun process(): PojoMethod {
        val asMember = context.processingEnv.typeUtils.asMemberOf(owner, element)
        val name = element.simpleName.toString()
        return PojoMethod(
                element = element,
                resolvedType = MoreTypes.asExecutable(asMember),
                name = name
        )
    }
}