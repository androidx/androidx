

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

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.lower.AbstractComposeLowering
import androidx.compose.compiler.plugins.kotlin.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.resolve.BindingTrace

/**
 * Record signatures of the functions created by the [CreateDecoysTransformer] to match them from
 * other compilation units. This step should be applied after other transforms are finished to
 * ensure that signature are matching serialized functions.
 */
class RecordDecoySignaturesTransformer(
    pluginContext: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    override val signatureBuilder: IdSignatureSerializer,
    metrics: ModuleMetrics,
    val mangler: KotlinMangler.IrMangler
) : AbstractComposeLowering(
    context = pluginContext,
    symbolRemapper = symbolRemapper,
    bindingTrace = bindingTrace,
    metrics = metrics,
),
    ModuleLoweringPass,
    DecoyTransformBase {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid()
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (!declaration.isDecoy() || !declaration.canBeLinkedAgainst()) {
            return super.visitFunction(declaration)
        }

        val decoyAnnotation = declaration.getAnnotation(DecoyFqNames.Decoy)!!
        val decoyFunction =
            symbolRemapper.getReferencedFunction(declaration.getComposableForDecoy())
        val sig =
            signatureBuilder.computeSignature(decoyFunction.owner)
                as? IdSignature.CommonSignature

        if (sig != null) {
            decoyAnnotation.putValueArgument(
                1,
                irVarargString(
                    listOf(
                        sig.packageFqName,
                        sig.declarationFqName,
                        sig.id.toString(),
                        sig.mask.toString()
                    )
                )
            )
        } else {
            error("${declaration.dump()} produced unsupported signature $sig")
        }

        return super.visitFunction(declaration)
    }

    private fun IrDeclaration.canBeLinkedAgainst(): Boolean =
        mangler.run { this@canBeLinkedAgainst.isExported(false) }
}
