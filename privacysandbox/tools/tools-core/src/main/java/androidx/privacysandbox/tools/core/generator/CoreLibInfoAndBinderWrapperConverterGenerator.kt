/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.generator.SpecNames.bundleClass
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

object CoreLibInfoAndBinderWrapperConverterGenerator {
    fun generate(annotatedInterface: AnnotatedInterface) =
        FileSpec.builder(
            annotatedInterface.type.packageName,
            annotatedInterface.coreLibInfoConverterName()
        ).build {
            addCommonSettings()
            addType(generateConverter(annotatedInterface))
        }

    private fun generateConverter(annotatedInterface: AnnotatedInterface) =
        TypeSpec.objectBuilder(annotatedInterface.coreLibInfoConverterName()).build {
            addFunction(FunSpec.builder("toParcelable").build {
                addParameter("coreLibInfo", bundleClass)
                addParameter("interface", annotatedInterface.aidlType().innerType.poetTypeName())
                returns(annotatedInterface.uiAdapterAidlWrapper().poetTypeName())
                addStatement(
                    "val parcelable = %T()",
                    annotatedInterface.uiAdapterAidlWrapper().poetTypeName()
                )
                addStatement("parcelable.coreLibInfo = coreLibInfo")
                addStatement("parcelable.binder = %N", "interface")
                addStatement("return parcelable")
            })
        }
}
