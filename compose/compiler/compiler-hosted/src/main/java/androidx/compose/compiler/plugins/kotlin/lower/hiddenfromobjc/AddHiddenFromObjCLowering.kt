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

package androidx.compose.compiler.plugins.kotlin.lower.hiddenfromobjc

import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.lower.AbstractComposeLowering
import androidx.compose.compiler.plugins.kotlin.lower.ComposableSymbolRemapper
import androidx.compose.compiler.plugins.kotlin.lower.containsComposableAnnotation
import androidx.compose.compiler.plugins.kotlin.lower.needsComposableRemapping
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.konan.isNative

/**
 *  AddHiddenFromObjCLowering looks for functions and properties with @Composable types and
 *  adds the `kotlin.native.HiddenFromObjC` annotation to them.
 *  @see https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native/-hidden-from-obj-c/
 */
class AddHiddenFromObjCLowering(
    private val pluginContext: IrPluginContext,
    symbolRemapper: ComposableSymbolRemapper,
    metrics: ModuleMetrics,
    private val hideFromObjCDeclarationsSet: HideFromObjCDeclarationsSet,
    stabilityInferencer: StabilityInferencer,
) : AbstractComposeLowering(pluginContext, symbolRemapper, metrics, stabilityInferencer) {

    private val hiddenFromObjCAnnotation: IrClassSymbol by lazy {
        getTopLevelClass(ClassId.fromString("kotlin/native/HiddenFromObjC"))
    }

    override fun lower(module: IrModuleFragment) {
        require(context.platform.isNative()) {
            "AddHiddenFromObjCLowering is expected to run only for k/native. " +
                "The platform - ${context.platform}"
        }
        module.transformChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val f = super.visitFunction(declaration) as IrFunction
        if (f.isLocal ||
            !(f.visibility == DescriptorVisibilities.PUBLIC ||
                f.visibility == DescriptorVisibilities.PROTECTED))
            return f

        if (f.hasComposableAnnotation() || f.needsComposableRemapping()) {
            f.addHiddenFromObjCAnnotation()
            hideFromObjCDeclarationsSet.add(f)
        }

        return f
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val p = super.visitProperty(declaration) as IrProperty
        if (p.isLocal || p.visibility != DescriptorVisibilities.PUBLIC) return p

        val shouldAdd = p.getter?.hasComposableAnnotation() ?: false ||
            p.getter?.needsComposableRemapping() ?: false ||
            p.backingField?.type.containsComposableAnnotation()

        if (shouldAdd) {
            p.addHiddenFromObjCAnnotation()
            hideFromObjCDeclarationsSet.add(p)
        }

        return p
    }

    private fun IrDeclaration.addHiddenFromObjCAnnotation() {
        val annotation = IrConstructorCallImpl.fromSymbolOwner(
            type = hiddenFromObjCAnnotation.defaultType,
            constructorSymbol = hiddenFromObjCAnnotation.constructors.first()
        )
        pluginContext.annotationsRegistrar.addMetadataVisibleAnnotationsToElement(this, annotation)
    }
}
