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

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.lower.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.resolve.BindingTrace
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Copies each IR declaration that won't match descriptors after Compose transforms (see [shouldBeRemapped]).
 * Original function are kept to match descriptors with a stubbed body, all other transforms are
 * applied to the copied version only.
 *
 * Example:
 * ```
 * @Composable
 * fun A(x: Any) {}
 * ```
 *
 * is transformed into:
 *
 * ```
 * @Decoy(targetName="A$composable")
 * fun A(x: Any) {
 *  illegalDecoyCallException("A")
 * }
 *
 * @Composable
 * @DecoyImplementation("A$composable")
 * fun A$composable(x: Any) {}
 * ```
 */
class CreateDecoysTransformer(
    pluginContext: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    signatureBuilder: IdSignatureSerializer,
    metrics: ModuleMetrics,
) : AbstractDecoysLowering(
    pluginContext = pluginContext,
    symbolRemapper = symbolRemapper,
    bindingTrace = bindingTrace,
    metrics = metrics,
    signatureBuilder = signatureBuilder
), ModuleLoweringPass {
    private var fieldModifiers: Field? = null

    private val originalFunctions: MutableMap<IrFunction, IrDeclarationParent> = mutableMapOf()

    private val stubDecoied = mutableSetOf<IrFunction>()

    private val decoyAnnotation by lazy {
        getTopLevelClass(DecoyFqNames.Decoy).owner
    }

    private val decoyImplementationAnnotation by lazy {
        getTopLevelClass(DecoyFqNames.DecoyImplementation).owner
    }

    private val decoyImplementationDefaultsBitmaskAnnotation =
        getTopLevelClass(DecoyFqNames.DecoyImplementationDefaultsBitMask).owner

    private val decoyStub by lazy {
        getInternalFunction("illegalDecoyCallException").owner
    }

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid()

        originalFunctions.forEach { (f, parent) ->
            (parent as? IrDeclarationContainer)?.addChild(f)
        }

        module.patchDeclarationParents()
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (!declaration.shouldBeRemapped()) {
            return super.visitSimpleFunction(declaration)
        }

        val newName = declaration.decoyImplementationName()
        val original = super.visitSimpleFunction(declaration) as IrSimpleFunction
        val copied = original.copyWithName(newName)
        copied.parent = original.parent

        originalFunctions += copied to declaration.parent

        return original.apply {
            setDecoyAnnotation(newName.asString())

            valueParameters.forEach { it.defaultValue = null }
            if (body != null) {
                stubBody()
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val function = (expression.symbol.owner as? IrSimpleFunction)
            ?: return super.visitCall(expression)
        val property = (function.correspondingPropertySymbol?.owner)
            ?: return super.visitCall(expression)

        if (function.isDecoy() || !function.shouldBeRemapped()) {
            return super.visitCall(expression)
        }

        when (function.origin) {
            IrDeclarationOrigin.FAKE_OVERRIDE -> {
                if (property.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) {
                    createExternalDecoyImplStub(function)
                }
            }
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ->
                createExternalDecoyImplStub(function)
        }

        return super.visitCall(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val function = (expression.symbol.owner as? IrSimpleFunction)
            ?: return super.visitFunctionReference(expression)
        val property = (function.correspondingPropertySymbol?.owner)
            ?: return super.visitFunctionReference(expression)

        if (function.isDecoy() || !function.shouldBeRemapped()) {
            return super.visitFunctionReference(expression)
        }

        when (function.origin) {
            IrDeclarationOrigin.FAKE_OVERRIDE -> {
                if (property.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) {
                    createExternalDecoyImplStub(function)
                }
            }
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ->
                createExternalDecoyImplStub(function)
        }

        return super.visitFunctionReference(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val function = (expression.symbol.owner as? IrConstructor)
            ?: return super.visitConstructorCall(expression)

        if (!function.isDecoy() && function.shouldBeRemapped() &&
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) {
            createExternalDecoyImplStub(function, ::externalStubConstructorBuilder)
        }

        return super.visitConstructorCall(expression)
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        if (!declaration.shouldBeRemapped()) {
            return super.visitConstructor(declaration)
        }

        val original = super.visitConstructor(declaration) as IrConstructor
        val newName = declaration.decoyImplementationName()

        val copied = original.copyWithName(newName, context.irFactory::buildConstructor)

        originalFunctions += copied to declaration.parent

        original.apply {
            setDecoyAnnotation(newName.asString())
            stubBody()
        }

        // We want to make origin constructor non-primary, cause ONLY primary constructor can have
        // instance initializer call. We create a constructor's copy to transfer to non-primary,
        // but after that, we should fix references to the origin one to make IR tree correct,
        // modify by reflecting is the more convenient way :).
        // If you want to copy from origin, should add @Decoy annotation on it also. References to
        // the origin constructor haven't been changed, pass "SubstituteDecoyCallsTransformer"
        // will check @Decoy annotation and change reference to decoy implementation.
        if (context.platform.isNative() && original.isPrimary) {
            try {
                val field =
                    original.javaClass.getDeclaredField("isPrimary").also { it.isAccessible = true }
                // compatible with JDK12 or above
                findFieldModifier().also {
                    it?.isAccessible = true
                    it?.setInt(field, field.modifiers and Modifier.FINAL.inv())
                }
                field.setBoolean(original, false)
            } catch (t: Throwable) {
                throw t
            }

            // After transfer flag "isPrimary" to new one, we MUST care about a special situation.
            // When a field declaration occurs in constructor, FE will generate a "IrProperty",
            // which has a "backing field" behind. And its "Initializer" will point to the
            // IrValueParameter which IrConstructor has. But in K/N, ONLY a primary constructor can
            // define a field, in other words, the "Initializer" of property MUST point to a
            // IrValueParameter which locate in primary constructor, or error will occur when
            // LLVM-codegen or deserialize symbols from Klib.
            // We will fix it in "SubstituteDecoyCallsTransformer" pass.
        }

        return original
    }

    private fun findFieldModifier(): Field? {
        if (fieldModifiers != null) {
            return fieldModifiers
        }

        try {
            fieldModifiers = Field::class.java.getDeclaredField("modifiers")
            return fieldModifiers
        } catch (t: Throwable) {
            try {
                val getDeclaredFields0 =
                    Class::class.java.getDeclaredMethod("getDeclaredFields0", Boolean::class.java)
                val accessible = getDeclaredFields0.canAccess(null)
                getDeclaredFields0.isAccessible = true

                fieldModifiers = (getDeclaredFields0.invoke(
                    Field::class.java,
                    false
                ) as List<Field>).firstOrNull { f -> f.name == "modifiers" }

                getDeclaredFields0.isAccessible = accessible

                return fieldModifiers
            } catch (t0: Throwable) {
                throw t0
            }
        }
    }

    private fun createExternalDecoyImplStub(
        function: IrFunction,
        factory: ((IrFunctionBuilder.() -> Unit) -> IrFunction)? = null
    ) {
        val newName = function.decoyImplementationName()
        val original = when (function) {
            is IrSimpleFunction -> super.visitSimpleFunction(function) as IrSimpleFunction
            is IrConstructor -> super.visitConstructor(function) as IrConstructor
            else -> throw IllegalArgumentException("Receive $function")
        }
        if (!stubDecoied.contains(original)) {
            original.createExternalDecoyImplStub(newName, factory)
            stubDecoied.add(function)
        }
    }

    private inline fun externalStubConstructorBuilder(builder: IrFunctionBuilder.() -> Unit) =
        context.irFactory.buildConstructor(builder).also {
            it.origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        }

    private fun IrFunction.createExternalDecoyImplStub(
        newName: Name,
        factory: ((IrFunctionBuilder.() -> Unit) -> IrFunction)? = null
    ): IrStatement {
        val copied = copyWithName(newName, factory ?: context.irFactory::buildFun, true)
        copied.parent = parent

        originalFunctions += copied to parent

        setDecoyAnnotation(newName.asString())

        valueParameters.forEach { it.defaultValue = null }
        if (body != null) {
            stubBody()
        }

        return this
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.decoyImplementationName(): Name {
        return dexSafeName(
            Name.identifier(name.asString() + IMPLEMENTATION_FUNCTION_SUFFIX)
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.copyWithName(
        newName: Name,
        factory: (IrFunctionBuilder.() -> Unit) -> IrFunction = context.irFactory::buildFun,
        externalStub: Boolean = false
    ): IrFunction {
        val original = this
        val newFunction = factory {
            updateFrom(original)
            name = newName
            returnType = original.returnType
            // In konan backend for K/N, only a primary constructor can has
            // IrInstanceInitializerCall inside, others MUST have IrDelegatingConstructorCall.
            // So if we building a decoy implementation constructor, we should check and transfer
            // "isPrimary" flag if need.
            isPrimary =
                if (original is IrConstructor && context.platform.isNative()) original.isPrimary
                else false
            isOperator = false
        }
        newFunction.annotations = original.annotations
        newFunction.metadata = original.metadata

        if (newFunction is IrSimpleFunction) {
            newFunction.overriddenSymbols = (original as IrSimpleFunction).overriddenSymbols
            newFunction.correspondingPropertySymbol = null
        }
        newFunction.origin =
            if (externalStub) IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
            else IrDeclarationOrigin.DEFINED

        // here generic value parameters will be applied
        newFunction.copyTypeParametersFrom(original)

        // ..but we need to remap the return type as well
        newFunction.returnType = newFunction.returnType.remapTypeParameters(
            source = original,
            target = newFunction
        )
        newFunction.valueParameters = original.valueParameters.map {
            val name = dexSafeName(it.name).asString()
            it.copyTo(
                newFunction,
                // remove leading $ in params to avoid confusing other transforms
                name = Name.identifier(name.dropWhile { it == '$' }),
                type = it.type.remapTypeParameters(original, newFunction),
                // remapping the type parameters explicitly
                defaultValue = it.defaultValue?.copyWithNewTypeParams(original, newFunction)
            )
        }
        newFunction.dispatchReceiverParameter =
            original.dispatchReceiverParameter?.copyTo(newFunction)
        newFunction.extensionReceiverParameter =
            original.extensionReceiverParameter?.copyWithNewTypeParams(original, newFunction)

        newFunction.body = original.moveBodyTo(newFunction)
            ?.copyWithNewTypeParams(original, newFunction)

        newFunction.addDecoyImplementationAnnotation(newName.asString(), original.getSignatureId())

        newFunction.valueParameters.forEach {
            it.defaultValue?.transformDefaultValue(
                originalFunction = original,
                newFunction = newFunction
            )
        }

        return newFunction
    }

    /**
     *  Expressions for default values can use other parameters.
     *  In such cases we need to ensure that default values expressions use parameters of the new
     *  function (new/copied value parameters).
     *
     *  Example:
     *  fun Foo(a: String, b: String = a) {...}
     */
    private fun IrExpressionBody.transformDefaultValue(
        originalFunction: IrFunction,
        newFunction: IrFunction
    ) {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val original = super.visitGetValue(expression)
                val valueParameter =
                    (expression.symbol.owner as? IrValueParameter) ?: return original

                val parameterIndex = valueParameter.index
                if (parameterIndex < 0 || valueParameter.parent != originalFunction) {
                    return super.visitGetValue(expression)
                }
                return irGet(newFunction.valueParameters[parameterIndex])
            }
        })
    }

    private fun IrFunction.stubBody() {
        body = DeclarationIrBuilder(context, symbol).irBlockBody {
            + irReturn(
                irCall(decoyStub).also { call ->
                    call.putValueArgument(0, irConst(name.asString()))
                }
            )
        }
    }

    private fun IrFunction.setDecoyAnnotation(implementationName: String) {
        annotations = listOf(
            IrConstructorCallImpl.fromSymbolOwner(
                type = decoyAnnotation.defaultType,
                constructorSymbol = decoyAnnotation.constructors.first().symbol
            ).also {
                it.putValueArgument(0, irConst(implementationName))
                it.putValueArgument(1, irVarargString(emptyList()))
            }
        )
    }

    private fun IrFunction.addDecoyImplementationAnnotation(name: String, signatureId: Long) {
        annotations = annotations +
            IrConstructorCallImpl.fromSymbolOwner(
                type = decoyImplementationAnnotation.defaultType,
                constructorSymbol = decoyImplementationAnnotation.constructors.first().symbol
            ).also {
                it.putValueArgument(0, irConst(name))
                it.putValueArgument(1, irConst(signatureId))
            }

        annotations = annotations +
            IrConstructorCallImpl.fromSymbolOwner(
                type = decoyImplementationDefaultsBitmaskAnnotation.defaultType,
                constructorSymbol =
                    decoyImplementationDefaultsBitmaskAnnotation.constructors.first().symbol
            ).also {
                val paramsWithDefaultsBitMask = bitMask(
                    *valueParameters.map { it.hasDefaultValue() }.toBooleanArray()
                )
                it.putValueArgument(0, irConst(paramsWithDefaultsBitMask))
            }
    }

    private fun IrFunction.shouldBeRemapped(): Boolean =
        !isLocalFunction() &&
            !isEnumConstructor() &&
            (hasComposableAnnotation() || hasComposableParameter())

    private fun IrFunction.isLocalFunction(): Boolean =
        origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA ||
            (isLocal && (this is IrSimpleFunction && !overridesComposable()))

    private fun IrSimpleFunction.overridesComposable() =
        overriddenSymbols.any {
            it.owner.isDecoy() || it.owner.shouldBeRemapped()
        }

    private fun IrFunction.hasComposableParameter() =
        valueParameters.any { it.type.hasComposable() } ||
            extensionReceiverParameter?.type?.hasComposable() == true

    private fun IrFunction.isEnumConstructor() =
        this is IrConstructor && parentAsClass.isEnumClass

    private fun IrType.hasComposable(): Boolean {
        if (hasAnnotation(ComposeFqNames.Composable)) {
            return true
        }

        return when (this) {
            is IrSimpleType -> arguments.any { (it as? IrType)?.hasComposable() == true }
            else -> false
        }
    }

    companion object {
        private const val IMPLEMENTATION_FUNCTION_SUFFIX = "\$composable"
    }
}
