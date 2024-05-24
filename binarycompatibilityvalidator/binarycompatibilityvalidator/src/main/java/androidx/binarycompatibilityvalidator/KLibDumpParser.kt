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

// Need to access Impl classes from 'org.jetbrains.kotlin.library.abi.impl.'
// ideally the parser would also live alongside that project to access to impl classes
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(ExperimentalLibraryAbiReader::class)

package androidx.binarycompatibilityvalidator

import org.jetbrains.kotlin.library.abi.AbiClass
import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiDeclaration
import org.jetbrains.kotlin.library.abi.AbiEnumEntry
import org.jetbrains.kotlin.library.abi.AbiFunction
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiProperty
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbi
import org.jetbrains.kotlin.library.abi.LibraryManifest
import org.jetbrains.kotlin.library.abi.impl.AbiClassImpl
import org.jetbrains.kotlin.library.abi.impl.AbiConstructorImpl
import org.jetbrains.kotlin.library.abi.impl.AbiEnumEntryImpl
import org.jetbrains.kotlin.library.abi.impl.AbiFunctionImpl
import org.jetbrains.kotlin.library.abi.impl.AbiPropertyImpl
import org.jetbrains.kotlin.library.abi.impl.AbiSignaturesImpl
import org.jetbrains.kotlin.library.abi.impl.AbiTopLevelDeclarationsImpl
import org.jetbrains.kotlin.library.abi.impl.AbiValueParameterImpl

class MutableAbiInfo(
    val declarations: MutableList<AbiDeclaration> = mutableListOf(),
    var uniqueName: String = ""
)

@OptIn(ExperimentalLibraryAbiReader::class)
class KlibDumpParser(klibDump: String) {

    /** Cursor to keep track of current location within the dump */
    private val cursor = Cursor(klibDump)
    /** The set of targets that the declarations being parsed belong to */
    private val currentTargetNames = mutableSetOf<String>()
    private val currentTargets: List<MutableAbiInfo>
        get() =
            currentTargetNames.map {
                abiInfoByTarget[it]
                    ?: throw IllegalStateException("Expected target $it to exist in map")
            }

    /**
     * Map of all targets to the declarations that belong to them. Only update the
     * [currentTargetNames] when parsing declarations.
     */
    private val abiInfoByTarget = mutableMapOf<String, MutableAbiInfo>()

    /** Parse the klib dump tracked by [cursor] into a map of targets to [LibraryAbi]s */
    fun parse(): Map<String, LibraryAbi> {
        while (cursor.hasNextRow()) {
            parseDeclaration(parentQualifiedName = null)?.let { abiDeclaration ->
                // Find all the targets the current declaration belongs to
                currentTargets.forEach {
                    // and add the parsed declaration to the list for those targets
                    it.declarations.add(abiDeclaration)
                }
            }
        }
        return abiInfoByTarget
            .map { (target, abiInfo) ->
                target to
                    LibraryAbi(
                        uniqueName = abiInfo.uniqueName,
                        signatureVersions = emptySet(),
                        topLevelDeclarations = AbiTopLevelDeclarationsImpl(abiInfo.declarations),
                        manifest =
                            LibraryManifest(
                                platform = target,
                                // To be completed in follow up CLs. This information is currently
                                // not
                                // considered when checking for compatibility
                                nativeTargets = listOf(),
                                compilerVersion = "",
                                abiVersion = "",
                                libraryVersion = "",
                                irProviderName = ""
                            )
                    )
            }
            .toMap()
    }

    private fun parseDeclaration(parentQualifiedName: AbiQualifiedName?): AbiDeclaration? {
        // if the line begins with a comment, we may need to parse the current target list
        if (cursor.parseSymbol("^\\/\\/") != null) {
            if (cursor.parseSymbol("Targets: ") != null) {
                // There are never targets within targets, so when we encounter a new directive we
                // always reset our current targets
                val targets = cursor.parseTargets()
                targets.forEach { abiInfoByTarget.putIfAbsent(it, MutableAbiInfo()) }
                currentTargetNames.clear()
                currentTargetNames.addAll(targets)
            } else if (cursor.parseSymbol("Library unique name: ") != null) {
                cursor.parseSymbol("<")
                val uniqueName =
                    cursor.parseSymbol("[a-zA-Z\\-\\.:]+")
                        ?: throw ParseException(cursor, "Failed to parse library unique name")
                currentTargets.forEach { it.uniqueName = uniqueName }
            }
            cursor.nextLine()
        } else if (cursor.hasClassKind()) {
            return parseClass(parentQualifiedName)
        } else if (cursor.hasFunctionKind()) {
            return parseFunction(parentQualifiedName)
        } else if (cursor.hasPropertyKind()) {
            return parseProperty(parentQualifiedName)
        } else if (cursor.hasEnumEntry()) {
            return parseEnumEntry(parentQualifiedName)
        } else if (cursor.currentLine.isBlank()) {
            cursor.nextLine()
        } else {
            throw ParseException(cursor, "Unknown declaration")
        }
        return null
    }

    internal fun parseClass(parentQualifiedName: AbiQualifiedName? = null): AbiClass {
        val modality =
            cursor.parseAbiModality()
                ?: throw ParseException(cursor, "Failed to parse class modality")
        val modifiers = cursor.parseClassModifiers()
        val isInner = modifiers.contains("inner")
        val isValue = modifiers.contains("value")
        val isFunction = modifiers.contains("fun")
        val kind =
            cursor.parseClassKind() ?: throw ParseException(cursor, "Failed to parse class kind")
        val typeParams = cursor.parseTypeParams() ?: emptyList()
        // if we are a nested class the name won't be qualified and we will need to use the
        // [parentQualifiedName] to complete it
        val abiQualifiedName = parseAbiQualifiedName(parentQualifiedName)
        val superTypes = cursor.parseSuperTypes()

        val childDeclarations =
            if (cursor.parseSymbol("^\\{") != null) {
                cursor.nextLine()
                parseChildDeclarations(abiQualifiedName)
            } else {
                emptyList()
            }
        return AbiClassImpl(
            qualifiedName = abiQualifiedName,
            signatures = fakeSignatures,
            annotations = emptySet(), // annotations aren't part of klib dumps
            modality = modality,
            kind = kind,
            isInner = isInner,
            isValue = isValue,
            isFunction = isFunction,
            superTypes = superTypes.toList(),
            declarations = childDeclarations,
            typeParameters = typeParams
        )
    }

    internal fun parseFunction(
        parentQualifiedName: AbiQualifiedName? = null,
        isGetterOrSetter: Boolean = false
    ): AbiFunction {
        val modality = cursor.parseAbiModality()
        val isConstructor = cursor.parseFunctionKind(peek = true) == "constructor"
        return when {
            isConstructor -> parseConstructor(parentQualifiedName)
            else ->
                parseNonConstructorFunction(
                    parentQualifiedName,
                    isGetterOrSetter,
                    modality
                        ?: throw ParseException(
                            cursor,
                            "Non constructor function must have modality"
                        ),
                )
        }
    }

    internal fun parseProperty(parentQualifiedName: AbiQualifiedName? = null): AbiProperty {
        val modality =
            cursor.parseAbiModality()
                ?: throw ParseException(cursor, "Unable to parse modality for property")
        val kind =
            cursor.parsePropertyKind()
                ?: throw ParseException(cursor, "Unable to parse kind for property")
        val qualifiedName = parseAbiQualifiedName(parentQualifiedName)

        cursor.nextLine()
        var getter: AbiFunction? = null
        var setter: AbiFunction? = null
        while (cursor.hasGetterOrSetter()) {
            if (cursor.hasGetter()) {
                getter = parseFunction(qualifiedName, isGetterOrSetter = true)
            } else {
                setter = parseFunction(qualifiedName, isGetterOrSetter = true)
            }
            if (cursor.isFinished()) {
                break
            }
        }
        return AbiPropertyImpl(
            qualifiedName = qualifiedName,
            signatures = fakeSignatures,
            annotations = emptySet(), // annotations aren't part of klib dumps
            modality = modality,
            kind = kind,
            getter = getter,
            setter = setter,
        )
    }

    internal fun parseEnumEntry(parentQualifiedName: AbiQualifiedName?): AbiEnumEntry {
        cursor.parseEnumEntryKind()
        val enumName =
            cursor.parseEnumName() ?: throw ParseException(cursor, "Failed to parse enum name")
        val relativeName =
            parentQualifiedName?.let { it.relativeName.value + "." + enumName }
                ?: throw ParseException(cursor, "Enum entry must have parent qualified name")
        val qualifiedName =
            AbiQualifiedName(parentQualifiedName.packageName, AbiCompoundName(relativeName))
        cursor.nextLine()
        return AbiEnumEntryImpl(
            qualifiedName = qualifiedName,
            signatures = fakeSignatures,
            annotations = emptySet()
        )
    }

    /** Parse all declarations which belong to a parent such as a class */
    private fun parseChildDeclarations(
        parentQualifiedName: AbiQualifiedName?
    ): List<AbiDeclaration> {
        val childDeclarations = mutableListOf<AbiDeclaration>()
        // end of parent container is marked by a closing bracket, collect all declarations
        // until we see one.
        while (cursor.parseSymbol("^(\\s+)?\\}", peek = true) == null) {
            parseDeclaration(parentQualifiedName)?.let { childDeclarations.add(it) }
        }
        cursor.nextLine()
        return childDeclarations
    }

    private fun parseNonConstructorFunction(
        parentQualifiedName: AbiQualifiedName? = null,
        isGetterOrSetter: Boolean = false,
        modality: AbiModality
    ): AbiFunction {
        val modifiers = cursor.parseFunctionModifiers()
        val isInline = modifiers.contains("inline")
        val isSuspend = modifiers.contains("suspend")
        cursor.parseFunctionKind()
        val typeParams = cursor.parseTypeParams() ?: emptyList()
        val functionReceiver = cursor.parseFunctionReceiver()
        val abiQualifiedName =
            if (isGetterOrSetter) {
                parseAbiQualifiedNameForGetterOrSetter(parentQualifiedName)
            } else {
                parseAbiQualifiedName(parentQualifiedName)
            }
        val valueParameters =
            cursor.parseValueParameters()
                ?: throw ParseException(cursor, "Couldn't parse value params")
        val allValueParameters =
            if (null != functionReceiver) {
                val functionReceiverAsValueParam =
                    AbiValueParameterImpl(
                        type = functionReceiver,
                        isVararg = false,
                        hasDefaultArg = false,
                        isNoinline = false,
                        isCrossinline = false
                    )
                listOf(functionReceiverAsValueParam) + valueParameters
            } else {
                valueParameters
            }
        val returnType = cursor.parseReturnType()
        cursor.nextLine()
        return AbiFunctionImpl(
            qualifiedName = abiQualifiedName,
            signatures = fakeSignatures,
            annotations = emptySet(), // annotations aren't part of klib dumps
            modality = modality,
            isInline = isInline,
            isSuspend = isSuspend,
            typeParameters = typeParams,
            hasExtensionReceiverParameter = null != functionReceiver,
            contextReceiverParametersCount = 0, // TODO
            valueParameters = allValueParameters,
            returnType = returnType
        )
    }

    private fun parseConstructor(parentQualifiedName: AbiQualifiedName?): AbiFunction {
        val abiQualifiedName =
            parentQualifiedName?.let {
                AbiQualifiedName(
                    parentQualifiedName.packageName,
                    AbiCompoundName(parentQualifiedName.relativeName.value + ".<init>")
                )
            } ?: throw ParseException(cursor, "Cannot parse constructor outside of class context")
        cursor.parseSymbol("constructor")
        cursor.parseSymbol("<init>")
        val valueParameters =
            cursor.parseValueParameters()
                ?: throw ParseException(cursor, "Couldn't parse value parameters for constructor")
        cursor.nextLine()
        return AbiConstructorImpl(
            qualifiedName = abiQualifiedName,
            signatures = fakeSignatures,
            annotations = emptySet(), // annotations aren't part of klib dumps
            isInline = false, // TODO
            contextReceiverParametersCount = 0, // TODO
            valueParameters = valueParameters,
        )
    }

    private fun parseAbiQualifiedName(parentQualifiedName: AbiQualifiedName?): AbiQualifiedName {
        val hasQualifiedName = cursor.parseAbiQualifiedName(peek = true) != null
        return if (hasQualifiedName) {
            cursor.parseAbiQualifiedName()!!
        } else {
            if (parentQualifiedName == null) {
                throw ParseException(cursor, "Failed to parse qName")
            }
            val identifier = cursor.parseValidIdentifier()
            val relativeName = parentQualifiedName.relativeName.value + "." + identifier
            return AbiQualifiedName(parentQualifiedName.packageName, AbiCompoundName(relativeName))
        }
    }

    private fun parseAbiQualifiedNameForGetterOrSetter(
        parentQualifiedName: AbiQualifiedName?
    ): AbiQualifiedName {
        if (parentQualifiedName == null) {
            throw ParseException(cursor, "Failed to parse qName")
        }
        val identifier =
            cursor.parseGetterOrSetterName()
                ?: throw ParseException(cursor, "Failed to parse qName")
        val relativeName = parentQualifiedName.relativeName.value + "." + identifier
        return AbiQualifiedName(parentQualifiedName.packageName, AbiCompoundName(relativeName))
    }

    companion object {
        // placeholder signatures, currently not considered during parsing / compatibility checking
        // https://github.com/JetBrains/kotlin/blob/master/compiler/util-klib-abi/ReadMe.md
        private val fakeSignatures = AbiSignaturesImpl(signatureV1 = null, signatureV2 = null)
    }
}

/** Exception which uses the cursor to include the location of the failure */
class ParseException(cursor: Cursor, message: String) :
    RuntimeException("$message ${cursor.rowIndex + 1}: ${cursor.currentLine}")
