/*
 * Copyright 2024 The Android Open Source Project
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
@file:OptIn(ExperimentalLibraryAbiReader::class)

package androidx.binarycompatibilityvalidator

import java.io.File
import org.jetbrains.kotlin.library.abi.AbiClass
import org.jetbrains.kotlin.library.abi.AbiClassifierReference.ClassReference
import org.jetbrains.kotlin.library.abi.AbiClassifierReference.TypeParameterReference
import org.jetbrains.kotlin.library.abi.AbiDeclaration
import org.jetbrains.kotlin.library.abi.AbiDeclarationContainer
import org.jetbrains.kotlin.library.abi.AbiEnumEntry
import org.jetbrains.kotlin.library.abi.AbiFunction
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiProperty
import org.jetbrains.kotlin.library.abi.AbiPropertyKind
import org.jetbrains.kotlin.library.abi.AbiType
import org.jetbrains.kotlin.library.abi.AbiTypeArgument
import org.jetbrains.kotlin.library.abi.AbiTypeArgument.StarProjection
import org.jetbrains.kotlin.library.abi.AbiTypeArgument.TypeProjection
import org.jetbrains.kotlin.library.abi.AbiTypeNullability
import org.jetbrains.kotlin.library.abi.AbiTypeParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameterKind
import org.jetbrains.kotlin.library.abi.AbiVariance
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbi
import org.jetbrains.kotlin.library.abi.LibraryAbiReader

@OptIn(ExperimentalLibraryAbiReader::class)
class BinaryCompatibilityChecker(
    private val newLibraryAbi: LibraryAbi,
    private val oldLibraryAbi: LibraryAbi,
    private val dependencies: Collection<File>,
) {

    private val declarationsProvidedByCompiler by lazy {
        FictionalFunctionAbiBuilder.build().allDeclarations().associateBy { it.asTypeString() }
    }
    private val dependencyDeclarations by lazy {
        declarationsProvidedByCompiler +
            dependencies
                .flatMap {
                    try {
                        LibraryAbiReader.readAbiInfo(it)
                            .allDeclarations()
                            .filterIsInstance<AbiClass>()
                    } catch (e: IllegalArgumentException) {
                        // Malformed library, probably missing IR and can't be used
                        listOf()
                    }
                }
                .associateBy { it.asTypeString() }
    }

    private val newLibraryDeclarations by lazy {
        dependencyDeclarations + newLibraryAbi.allDeclarations().associateBy { it.asTypeString() }
    }
    private val oldLibraryDeclarations by lazy {
        dependencyDeclarations + oldLibraryAbi.allDeclarations().associateBy { it.asTypeString() }
    }

    private fun checkBinariesAreCompatible(
        errors: CompatibilityErrors,
        validate: Boolean,
        shouldFreeze: Boolean = false,
    ): CompatibilityErrors {
        return newLibraryAbi.checkIsBinaryCompatibleWith(
            oldLibraryAbi,
            errors,
            validate,
            shouldFreeze,
        )
    }

    private fun LibraryAbi.checkIsBinaryCompatibleWith(
        olderLibraryAbi: LibraryAbi,
        errors: CompatibilityErrors,
        validate: Boolean,
        shouldFreeze: Boolean,
    ): CompatibilityErrors {
        topLevelDeclarations.isBinaryCompatibleWith(
            olderLibraryAbi.topLevelDeclarations,
            uniqueName,
            errors,
            shouldFreeze,
        )
        if (validate && errors.isNotEmpty()) {
            throw ValidationException(errors.toString())
        }
        return errors
    }

    private fun AbiDeclarationContainer.isBinaryCompatibleWith(
        oldContainer: AbiDeclarationContainer,
        parentQualifiedName: String,
        errors: CompatibilityErrors,
        shouldFreeze: Boolean,
    ) {
        val isBinaryCompatibleWith:
            AbiDeclaration.(AbiDeclaration, String, CompatibilityErrors) -> Unit =
            { other, /* parentQualifiedName */ _, errs ->
                isBinaryCompatibleWith(other, errs)
            }
        decoratedDeclarations()
            .isBinaryCompatibleWith(
                oldContainer.decoratedDeclarations(),
                entityName = "declaration",
                uniqueId = AbiDeclaration::asTypeString,
                isBinaryCompatibleWith = isBinaryCompatibleWith,
                parentQualifiedName = parentQualifiedName,
                errors = errors,
                isAllowedAddition = { !shouldFreeze },
            )
    }

    private fun AbiDeclarationContainer.decoratedDeclarations() =
        declarations.map {
            when (it) {
                is AbiFunction -> DecoratedAbiFunction(it, null)
                is AbiProperty -> DecoratedAbiProperty(it, null)
                else -> it
            }
        }

    private fun AbiDeclaration.isBinaryCompatibleWith(
        oldDeclaration: AbiDeclaration,
        @Suppress("UNUSED_PARAMETER") parentQualifiedName: String,
        errors: CompatibilityErrors,
    ) = isBinaryCompatibleWith(oldDeclaration, errors)

    private fun AbiDeclaration.isBinaryCompatibleWith(
        oldDeclaration: AbiDeclaration,
        errors: CompatibilityErrors,
    ) {
        // If we're comparing a class to a function, or any other type, they are not compatible and
        // it's not worth checking anything further. The code that calls this function should
        // generally make sure that doesn't happen, but if it does we fail early.
        if (this::class.java != oldDeclaration::class.java) {
            errors.add(
                "type changed from ${this::class.simpleName} to " +
                    "${oldDeclaration::class.simpleName} for $qualifiedName"
            )
            return
        }
        when (this) {
            is AbiClass -> isBinaryCompatibleWith(oldDeclaration as AbiClass, errors)
            is DecoratedAbiFunction ->
                isBinaryCompatibleWith(oldDeclaration as DecoratedAbiFunction, errors)
            is DecoratedAbiProperty ->
                isBinaryCompatibleWith(oldDeclaration as DecoratedAbiProperty, errors)
            is AbiEnumEntry -> Unit
            else ->
                throw IllegalStateException(
                    "All AbiFunctions and AbiProperties should be decorated"
                )
        }
    }

    private fun AbiClass.isBinaryCompatibleWith(oldClass: AbiClass, errors: CompatibilityErrors) {
        if (modality != oldClass.modality) {
            when {
                modality == AbiModality.OPEN && oldClass.modality == AbiModality.FINAL -> Unit
                modality == AbiModality.OPEN && oldClass.modality == AbiModality.SEALED -> Unit
                modality == AbiModality.OPEN && oldClass.modality == AbiModality.ABSTRACT -> Unit
                modality == AbiModality.ABSTRACT && oldClass.modality == AbiModality.SEALED -> Unit
                else ->
                    errors.add(
                        "modality changed from ${oldClass.modality} to $modality for $qualifiedName"
                    )
            }
        }
        if (kind != oldClass.kind) {
            errors.add("kind changed from ${oldClass.kind} to $kind for $qualifiedName")
        }
        if (isInner != oldClass.isInner) {
            errors.add("isInner changed from ${oldClass.isInner} to $isInner for $qualifiedName")
        }
        if (isValue != oldClass.isValue) {
            errors.add("isValue changed from ${oldClass.isValue} to $isValue for $qualifiedName")
        }
        if (isFunction != oldClass.isFunction) {
            when {
                isFunction && !oldClass.isFunction -> Unit
                else ->
                    errors.add(
                        "isFunction changed from ${oldClass.isFunction} to $isFunction for " +
                            "$qualifiedName"
                    )
            }
        }

        // Check that previous supertypes are still currently supertypes
        allSuperTypes(newLibraryDeclarations)
            .isBinaryCompatibleWith(
                oldClass.allSuperTypes(oldLibraryDeclarations),
                entityName = "superType",
                uniqueId = AbiType::asString,
                isBinaryCompatibleWith = AbiType::isBinaryCompatibleWith,
                parentQualifiedName = qualifiedName.toString(),
                errors = errors,
            )
        typeParameters.isBinaryCompatibleWith(
            oldClass.typeParameters,
            entityName = "typeParameter",
            uniqueId = AbiTypeParameter::tag,
            isBinaryCompatibleWith = AbiTypeParameter::isBinaryCompatibleWith,
            parentQualifiedName = qualifiedName.toString(),
            errors = errors,
            isAllowedAddition = { false },
        )
        val newDecs = allDeclarationsIncludingInherited(newLibraryDeclarations)
        val oldDecs = oldClass.allDeclarationsIncludingInherited(oldLibraryDeclarations)
        newDecs.isBinaryCompatibleWith(
            oldDecs,
            entityName = "declaration",
            uniqueId = AbiDeclaration::asUnqualifiedTypeString,
            isBinaryCompatibleWith = { other, parentName, errs ->
                isBinaryCompatibleWith(other, parentName, errs)
            },
            isAllowedAddition = {
                when {
                    this is AbiFunction -> modality != AbiModality.ABSTRACT
                    else -> true
                }
            },
            parentQualifiedName = qualifiedName.toString(),
            errors = errors,
        )
    }

    private fun AbiClass.allSuperTypes(declarations: Map<String, AbiDeclaration>): List<AbiType> {
        return superTypes + superTypes.flatMap { it.allSuperTypes(declarations) }
    }

    private fun AbiType.allSuperTypes(declarations: Map<String, AbiDeclaration>): List<AbiType> {
        val abiClass = declarations[asString()] as? AbiClass ?: return emptyList()
        val superTypes = abiClass.superTypes
        return superTypes + superTypes.flatMap { it.allSuperTypes(declarations) }
    }

    private fun AbiClass.allDeclarationsIncludingInherited(
        oldLibraryDeclarations: Map<String, AbiDeclaration>
    ): List<AbiDeclaration> {
        // Collect all the declarations directly on the class (without functions) +
        // + all functions, (including inherited). The filterNot is to avoid listing
        // functions directly on the class twice.
        return declarations.filterNot { it is AbiFunction }.filterNot { it is AbiProperty } +
            allMethodsIncludingInherited(oldLibraryDeclarations) +
            allPropertiesIncludingInherited(oldLibraryDeclarations)
    }

    private fun AbiClass.allPropertiesIncludingInherited(
        oldLibraryDeclarations: Map<String, AbiDeclaration>,
        baseClass: AbiClass = this,
    ): List<DecoratedAbiProperty> {
        val propertyMap =
            declarations
                .filterIsInstance<AbiProperty>()
                .associate { it.asUnqualifiedTypeString() to DecoratedAbiProperty(it, baseClass) }
                .toMutableMap()
        superTypes
            .map {
                // we should throw here if we can't find the class in the package/dependencies
                oldLibraryDeclarations[it.asString()]
            }
            .filterIsInstance<AbiClass>()
            .flatMap { it.allPropertiesIncludingInherited(oldLibraryDeclarations, baseClass) }
            .associateBy { it.asUnqualifiedTypeString() }
            .forEach { (key, prop) -> propertyMap.putIfAbsent(key, prop) }
        return propertyMap.values.toList()
    }

    private fun AbiClass.allMethodsIncludingInherited(
        oldLibraryDeclarations: Map<String, AbiDeclaration>,
        baseClass: AbiClass = this,
    ): List<DecoratedAbiFunction> {
        val functionMap =
            declarations
                .filterIsInstance<AbiFunction>()
                .associate { it.asUnqualifiedTypeString() to DecoratedAbiFunction(it, baseClass) }
                .toMutableMap()
        superTypes
            .map {
                oldLibraryDeclarations.getOrElse(it.className.toString()) {
                    throw IllegalStateException("Missing declaration ${it.asString()}")
                }
            }
            .filterIsInstance<AbiClass>()
            .flatMap { it.allMethodsIncludingInherited(oldLibraryDeclarations, baseClass) }
            .associateBy { it.asUnqualifiedTypeString() }
            .forEach { (key, func) -> functionMap.putIfAbsent(key, func) }
        return functionMap.values.toList()
    }

    private fun DecoratedAbiFunction.isBinaryCompatibleWith(
        otherFunction: DecoratedAbiFunction,
        errors: CompatibilityErrors,
    ) {
        if (isConstructor != otherFunction.isConstructor) {
            errors.add(
                "isConstructor changed from ${otherFunction.isConstructor} to " +
                    "$isConstructor for $qualifiedName"
            )
        }
        if (effectiveModality != otherFunction.effectiveModality) {
            when {
                effectiveModality == AbiModality.OPEN &&
                    otherFunction.effectiveModality == AbiModality.ABSTRACT -> Unit
                else ->
                    errors.add(
                        "modality changed from ${otherFunction.effectiveModality} to " +
                            "$effectiveModality for $qualifiedName"
                    )
            }
        }
        if (isInline != otherFunction.isInline) {
            when {
                isInline && !otherFunction.isInline -> Unit
                else ->
                    errors.add(
                        "isInline changed from ${otherFunction.isInline} to $isInline " +
                            "for $qualifiedName"
                    )
            }
        }
        if (isSuspend != otherFunction.isSuspend) {
            errors.add(
                "isSuspend changed from ${otherFunction.isSuspend} to $isSuspend for " +
                    "$qualifiedName"
            )
        }
        // We consider a function to be removed if the extension receiver parameter has changed
        // so we should never make it this far, but leave the check for correctness
        if (hasExtensionReceiverParameter() != otherFunction.hasExtensionReceiverParameter()) {
            errors.add(
                "hasExtensionReceiverParameter changed from " +
                    "${otherFunction.hasExtensionReceiverParameter()} to " +
                    "${hasExtensionReceiverParameter()} for $qualifiedName"
            )
        }
        // Same as with extension functions if the context receiver param count changes we won't
        // consider these to be the same function
        if (contextReceiverParametersCount() != otherFunction.contextReceiverParametersCount()) {
            errors.add(
                "contextReceiverParametersCount changed from " +
                    "${otherFunction.contextReceiverParametersCount()} to " +
                    "${contextReceiverParametersCount()} for $qualifiedName"
            )
        }
        returnType.isBinaryCompatibleWith(
            otherFunction.returnType,
            qualifiedName.toString(),
            errors,
            "Return type",
        )

        // bake the index into the data type for clearer reporting in error messages
        val decoratedValueParameters: List<DecoratedAbiValueParameter> =
            valueParameters.mapIndexed { index, valueParameter ->
                DecoratedAbiValueParameter(index, valueParameter)
            }
        // by the time we get here, we already know that there are the same number of value
        // parameters and that they have the same type. If they didn't they would be considered
        // to be different functions. The following check is to give more detailed compatibility
        // details on things like whether a param has a default, is vararg, etc
        decoratedValueParameters.isBinaryCompatibleWith(
            otherFunction.valueParameters.mapIndexed { index, valueParameter ->
                DecoratedAbiValueParameter(index, valueParameter)
            },
            entityName = "valueParameter",
            isAllowedAddition = { false },
            uniqueId = DecoratedAbiValueParameter::asString,
            isBinaryCompatibleWith = DecoratedAbiValueParameter::isBinaryCompatibleWith,
            parentQualifiedName = qualifiedName.toString(),
            errors = errors,
        )
        typeParameters.isBinaryCompatibleWith(
            otherFunction.typeParameters,
            entityName = "typeParameter",
            isAllowedAddition = { false },
            uniqueId = AbiTypeParameter::tag,
            isBinaryCompatibleWith = AbiTypeParameter::isBinaryCompatibleWith,
            parentQualifiedName = qualifiedName.toString(),
            errors = errors,
        )
    }

    private fun DecoratedAbiProperty.isBinaryCompatibleWith(
        oldProperty: DecoratedAbiProperty,
        errors: CompatibilityErrors,
    ) {
        if (kind != oldProperty.kind) {
            when {
                kind == AbiPropertyKind.CONST_VAL && oldProperty.kind == AbiPropertyKind.VAL -> Unit
                effectiveModality == AbiModality.FINAL &&
                    kind == AbiPropertyKind.VAR &&
                    oldProperty.kind == AbiPropertyKind.VAL -> Unit
                // changing var to val is allowed as long as the setter was private / internal (null
                // in dump)
                oldProperty.kind == AbiPropertyKind.VAR &&
                    kind == AbiPropertyKind.VAL &&
                    oldProperty.setter == null -> Unit
                else ->
                    errors.add(
                        "kind changed from ${oldProperty.kind} to $kind for ${this.asTypeString()}"
                    )
            }
        }
        val newGetter = getter?.let { DecoratedAbiFunction(it, parentClass) }
        val oldGetter =
            oldProperty.getter?.let { DecoratedAbiFunction(it, oldProperty.parentClass) }
        if (oldGetter != null && newGetter == null) {
            errors.add("removed getter from $qualifiedName")
        } else if (oldGetter != null && newGetter != null) {
            newGetter.isBinaryCompatibleWith(oldGetter, errors)
        }

        val newSetter = setter?.let { DecoratedAbiFunction(it, parentClass) }
        val oldSetter =
            oldProperty.setter?.let { DecoratedAbiFunction(it, oldProperty.parentClass) }
        if (oldSetter != null && newSetter == null) {
            errors.add("removed setter from $qualifiedName")
        } else if (oldSetter != null && newSetter != null) {
            newSetter.isBinaryCompatibleWith(oldSetter, errors)
        }
    }

    companion object {
        fun checkAllBinariesAreCompatible(
            newLibraries: Map<String, LibraryAbi>,
            oldLibraries: Map<String, LibraryAbi>,
            baselines: Set<String> = emptySet(),
            validate: Boolean = true,
            shouldFreeze: Boolean = false,
            dependencies: Map<String, Collection<File>> = mapOf(),
        ): List<CompatibilityError> {
            val errors = CompatibilityErrors(baselines, "meta")
            val removedTargets = oldLibraries.keys - newLibraries.keys
            val addedTargets = newLibraries.keys - oldLibraries.keys
            if (removedTargets.isNotEmpty()) {
                errors.addAll(
                    removedTargets.flatMap {
                        CompatibilityErrors(baselines, it).apply { add("Target was removed") }
                    }
                )
            }
            if (shouldFreeze && addedTargets.isNotEmpty()) {
                errors.addAll(
                    addedTargets.flatMap {
                        CompatibilityErrors(baselines, it).apply { add("Target was added") }
                    }
                )
            }
            if (errors.isNotEmpty()) {
                if (validate) {
                    throw ValidationException(errors.toString())
                }
                return errors
            }
            return oldLibraries.keys.flatMap { target ->
                val newLib =
                    newLibraries[target]
                        // We can't compare targets if they've been removed. We'll throw on removed
                        // targets but if that removal is baselined we can still make it here.
                        ?: return@flatMap emptyList()
                val oldLib = oldLibraries[target]!!
                val errorsForTarget = CompatibilityErrors(baselines, target)
                val dependenciesForTarget =
                    dependencies.getOrElse(target) {
                        throw IllegalStateException("Dependencies missing for target $target")
                    }
                BinaryCompatibilityChecker(newLib, oldLib, dependenciesForTarget)
                    .checkBinariesAreCompatible(errorsForTarget, validate, shouldFreeze)
            }
        }

        fun checkAllBinariesAreCompatible(
            newLibraries: Map<String, LibraryAbi>,
            oldLibraries: Map<String, LibraryAbi>,
            baselineFile: File?,
            validate: Boolean = true,
            shouldFreeze: Boolean = false,
            dependencies: Map<String, Collection<File>> = mapOf(),
        ) =
            checkAllBinariesAreCompatible(
                newLibraries,
                oldLibraries,
                baselineFile?.asBaselineErrors() ?: emptySet(),
                validate,
                shouldFreeze,
                dependencies,
            )
    }
}

internal fun AbiTypeParameter.isBinaryCompatibleWith(
    otherTypeParam: AbiTypeParameter,
    parentQualifiedName: String,
    errors: CompatibilityErrors,
) {
    if (isReified != otherTypeParam.isReified) {
        when {
            !isReified && otherTypeParam.isReified -> Unit
            else ->
                errors.add(
                    "isReified changed from ${otherTypeParam.isReified} to $isReified " +
                        "for type param $tag on $parentQualifiedName"
                )
        }
    }
    if (variance != otherTypeParam.variance) {
        errors.add(
            "variance changed from ${otherTypeParam.variance} to $variance " +
                "for type param $tag on $parentQualifiedName"
        )
    }
    if (isUnbounded() && otherTypeParam.isUnbounded()) {
        return
    }
    if (upperBounds.asStrings() != otherTypeParam.upperBounds.asStrings()) {
        errors.add(
            "upper bounds changed from ${otherTypeParam.upperBounds.asString()} to " +
                "${upperBounds.asString()} for type param $tag on $parentQualifiedName"
        )
    }
}

private val AbiType?.asStringOrUnit: String
    get() = this?.asString() ?: "Unit / null"

private fun List<AbiType>.asStrings(): List<String> = map { it.asString() }

private fun List<AbiType>.asString(): String = joinToString(",") { it.asString() }

private fun AbiTypeParameter.isUnbounded(): Boolean =
    (upperBounds.isEmpty() ||
        (upperBounds.singleOrNull()?.let {
            it.className.toString() == "kotlin/Any" &&
                it.nullability == AbiTypeNullability.MARKED_NULLABLE
        } ?: false))

private fun DecoratedAbiValueParameter.isBinaryCompatibleWith(
    otherParam: DecoratedAbiValueParameter,
    parentQualifiedName: String,
    errors: CompatibilityErrors,
) {
    type.isBinaryCompatibleWith(otherParam.type, parentQualifiedName, errors)
    if (isVararg != otherParam.isVararg) {
        errors.add(
            "isVararg changed from ${otherParam.isVararg} to $isVararg for parameter " +
                "${asString()} of $parentQualifiedName"
        )
    }
    if (hasDefaultArg != otherParam.hasDefaultArg) {
        when {
            hasDefaultArg && !otherParam.hasDefaultArg -> Unit
            else ->
                errors.add(
                    "hasDefaultArg changed from ${otherParam.hasDefaultArg} to $hasDefaultArg for " +
                        "parameter ${asString()} of $parentQualifiedName"
                )
        }
    }
    if (isCrossinline && otherParam.isNoinline) {
        return
    }
    if (isNoinline != otherParam.isNoinline) {
        errors.add(
            "isNoinline changed from ${otherParam.isNoinline} to $isNoinline for " +
                "parameter ${asString()} of $parentQualifiedName"
        )
    }
    if (isCrossinline != otherParam.isCrossinline) {
        errors.add(
            "isCrossinline changed from ${otherParam.isCrossinline} to $isCrossinline for " +
                "parameter ${asString()} of $parentQualifiedName"
        )
    }
}

private fun AbiType?.isBinaryCompatibleWith(
    otherType: AbiType?,
    parentQualifiedName: String,
    errors: CompatibilityErrors,
) = isBinaryCompatibleWith(otherType, parentQualifiedName, errors, "Type")

private fun AbiType?.isBinaryCompatibleWith(
    otherType: AbiType?,
    parentQualifiedName: String,
    errors: CompatibilityErrors,
    kind: String = "type",
) {
    if (asStringOrUnit != otherType.asStringOrUnit) {
        errors.add(
            "$kind changed from ${otherType.asStringOrUnit} to " +
                "$asStringOrUnit for $parentQualifiedName"
        )
    }
}

private fun AbiDeclaration.asTypeString() =
    when (this) {
        is AbiFunction -> asTypeString()
        is AbiProperty -> asTypeString()
        else -> qualifiedName.toString()
    }

private fun AbiFunction.asTypeString(name: String = qualifiedName.toString()): String {
    return (typeParametersString() +
        contextReceiverParametersString() +
        extensionReceiverParameterString() +
        name +
        regularValueParametersString())
}

private fun AbiFunction.typeParametersString(): String {
    if (typeParameters.isEmpty()) {
        return ""
    }
    return typeParameters.joinToString(", ", "<", ">") { it.asString() }
}

fun AbiTypeParameter.asString(): String {
    val builder = StringBuilder()
    builder.append(tag)
    builder.append(" : ")
    if (upperBounds.isEmpty()) {
        builder.append("kotlin/Any?")
    }
    builder.append(upperBounds.asString())
    return builder.toString()
}

private fun AbiProperty.asTypeString(name: String = qualifiedName.toString()): String {
    val getterFunc = getter ?: return name
    return (getterFunc.contextReceiverParametersString() +
        getterFunc.extensionReceiverParameterString() +
        name)
}

private fun AbiFunction.contextReceiverParameters(): List<AbiValueParameter> {
    return valueParameters.take(contextReceiverParametersCount())
}

private fun AbiFunction.contextReceiverParametersString(): String {
    if (contextReceiverParametersCount() == 0) {
        return ""
    }
    return "context(" + contextReceiverParameters().joinToString(", ") { it.type.asString() } + ") "
}

private fun AbiFunction.extensionReceiverParameter(): AbiValueParameter? {
    if (!hasExtensionReceiverParameter()) {
        return null
    }
    return valueParameters[contextReceiverParametersCount()]
}

private fun AbiFunction.extensionReceiverParameterString(): String =
    extensionReceiverParameter()?.let { "(${it.type.asString()})." } ?: ""

private fun AbiFunction.regularValueParameters(): List<AbiValueParameter> {
    return valueParameters.drop(contextReceiverParametersCount() + extensionReceiverParameterCount)
}

private fun AbiFunction.regularValueParametersString(): String =
    "(" + regularValueParameters().joinToString(", ") { it.type.asString() } + ")"

private val AbiFunction.extensionReceiverParameterCount: Int
    get() =
        if (hasExtensionReceiverParameter()) 1
        else {
            0
        }

private fun AbiDeclaration.asUnqualifiedTypeString(): String {
    val name = qualifiedName.relativeName.nameSegments.last().value
    return when (this) {
        is AbiFunction -> asTypeString(name)
        is AbiProperty -> asTypeString(name)
        else -> name
    }
}

// Based on implementation from AbiRendererImpl
// https://github.com/JetBrains/kotlin/blob/e7edef36c6110276cb076d4bda3a780b49742022/compiler/util-klib-abi/src/org/jetbrains/kotlin/library/abi/impl/LibraryAbiRendererImpl.kt#L195
fun AbiType.asString(): String =
    when (this) {
        is AbiType.Dynamic -> "dynamic"
        is AbiType.Error -> "error"
        is AbiType.Simple -> {
            val builder = StringBuilder()
            when (val classifier = classifierReference) {
                is ClassReference -> {
                    builder.append(classifier.className)
                    if (arguments.isNotEmpty()) {
                        builder.append("<")
                        builder.append(
                            arguments.joinToString(separator = ", ") { typeArgument ->
                                typeArgument.asString()
                            }
                        )
                        builder.append(">")
                    }
                    // We only care about marked nullable and not here, since unspecified
                    // only applies to type parameters
                    // https://github.com/JetBrains/kotlin/blob/e7edef36c6110276cb076d4bda3a780b49742022/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/types/IrType.kt#L63
                    if (nullability == AbiTypeNullability.MARKED_NULLABLE) {
                        builder.append('?')
                    }
                }
                is TypeParameterReference -> {
                    builder.append('#').append(classifier.tag)
                    builder.append(nullability.asString())
                }
            }
            builder.toString()
        }
    }

private fun AbiVariance.asString(): String =
    when (this) {
        AbiVariance.INVARIANT -> ""
        AbiVariance.IN -> "in "
        AbiVariance.OUT -> "out "
    }

private fun AbiTypeNullability.asString() =
    when (this) {
        AbiTypeNullability.MARKED_NULLABLE -> "?"
        AbiTypeNullability.NOT_SPECIFIED -> ""
        AbiTypeNullability.DEFINITELY_NOT_NULL -> "!!"
    }

private fun DecoratedAbiValueParameter.asString() = "$index: ${type.asString()}"

private fun AbiTypeArgument.asString(): String =
    when (this) {
        is StarProjection -> "*"
        is TypeProjection -> variance.asString() + type.asString()
    }

class ValidationException(errorMessage: String) : RuntimeException(errorMessage)

private fun LibraryAbi.allDeclarations(): List<AbiDeclaration> {
    val allDeclarations = mutableListOf<AbiDeclaration>()
    topLevelDeclarations.declarations.forEach { allDeclarations.addAll(it.allDeclarations()) }
    return allDeclarations
}

private fun AbiDeclaration.allDeclarations(): List<AbiDeclaration> {
    return listOf(this) +
        when (this) {
            is AbiEnumEntry -> emptyList()
            is AbiFunction -> emptyList()
            is AbiProperty -> listOfNotNull(getter, setter)
            is AbiClass -> declarations.flatMap { it.allDeclarations() }
        }
}

/**
 * Checks if any list of items which may be binary compatible with their previous version is
 * compatible. We have to do this a lot (methods, value parameters, super types, type args) and
 * although they have slightly different rules about when things can be added / removed they follow
 * the same basic pattern.
 */
private fun <T> List<T>.isBinaryCompatibleWith(
    oldEntitiesList: List<T>,
    entityName: String,
    uniqueId: T.() -> String,
    isBinaryCompatibleWith: T.(T, String, CompatibilityErrors) -> Unit,
    isAllowedAddition: T.() -> Boolean = { true },
    parentQualifiedName: String,
    errors: CompatibilityErrors,
) {
    val oldEntities = oldEntitiesList.associateBy { it.uniqueId() }
    val newEntities = associateBy { it.uniqueId() }
    val removedEntities = oldEntities.keys - newEntities.keys
    removedEntities.forEach { errors.add("Removed $entityName $it from $parentQualifiedName") }
    val addedEntities = newEntities.keys - oldEntities.keys
    val disallowedAdditions = addedEntities.filterNot { newEntities[it]!!.isAllowedAddition() }
    disallowedAdditions.forEach { errors.add("Added $entityName $it to $parentQualifiedName") }
    for ((id, oldEntity) in oldEntities) {
        // if the entity is missing we'll add an error for that above, but we continue to compare
        // entities to show as many violations at once as possible
        val newEntity: T = newEntities[id] ?: continue
        newEntity.isBinaryCompatibleWith(oldEntity, parentQualifiedName, errors)
    }
}

class CompatibilityErrors(private val baselines: Set<String>, val target: String) :
    MutableList<CompatibilityError> by mutableListOf() {
    fun add(
        message: String,
        severity: CompatibilityErrorSeverity = CompatibilityErrorSeverity.ERROR,
    ) {
        val error = CompatibilityError(message, target, severity)
        if (baselines.contains(error.toString())) {
            return
        }
        add(error)
    }

    override fun toString(): String = joinToString("\n")
}

data class CompatibilityError(
    private val message: String,
    val target: String,
    val severity: CompatibilityErrorSeverity,
) {
    override fun toString(): String {
        return "[$target]: $message"
    }
}

enum class CompatibilityErrorSeverity() {
    ERROR
}

private fun File.asBaselineErrors(): Set<String> =
    readLines().toMutableList().let {
        val formatVersion =
            try {
                it.removeFirst().split(":").last().trim()
            } catch (_: Exception) {
                throw RuntimeException("Failed to parse baseline version from '${this.path}'")
            }
        return when (formatVersion) {
            "1.0" -> it.toSet()
            else -> throw RuntimeException("Unrecognized baseline format: '$formatVersion'")
        }
    }

private class DecoratedAbiFunction(abiFunction: AbiFunction, val parentClass: AbiClass?) :
    AbiFunction by abiFunction {
    val effectiveModality
        get() =
            when (parentClass?.modality) {
                AbiModality.FINAL -> AbiModality.FINAL
                else -> modality
            }
}

private class DecoratedAbiProperty(abiProperty: AbiProperty, val parentClass: AbiClass?) :
    AbiProperty by abiProperty {
    val effectiveModality
        get() =
            when (parentClass?.modality) {
                AbiModality.FINAL -> AbiModality.FINAL
                else -> modality
            }
}

private class DecoratedAbiValueParameter(val index: Int, param: AbiValueParameter) :
    AbiValueParameter by param

fun AbiFunction.contextReceiverParametersCount(): Int =
    valueParameters.count { it.kind == AbiValueParameterKind.CONTEXT }

private fun AbiFunction.hasExtensionReceiverParameter() =
    valueParameters.any { it.kind == AbiValueParameterKind.EXTENSION_RECEIVER }
