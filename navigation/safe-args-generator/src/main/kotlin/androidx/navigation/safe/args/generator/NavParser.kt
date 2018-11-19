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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.NavParserErrors.sameSanitizedNameActions
import androidx.navigation.safe.args.generator.NavParserErrors.sameSanitizedNameArguments
import androidx.navigation.safe.args.generator.ext.toCamelCase
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.IncludedDestination
import androidx.navigation.safe.args.generator.models.ResReference
import java.io.File
import java.io.FileReader

private const val TAG_NAVIGATION = "navigation"
private const val TAG_ACTION = "action"
private const val TAG_ARGUMENT = "argument"
private const val TAG_INCLUDE = "include"

private const val ATTRIBUTE_ID = "id"
private const val ATTRIBUTE_DESTINATION = "destination"
private const val ATTRIBUTE_DEFAULT_VALUE = "defaultValue"
private const val ATTRIBUTE_NAME = "name"
private const val ATTRIBUTE_TYPE = "argType"
private const val ATTRIBUTE_TYPE_DEPRECATED = "type"
private const val ATTRIBUTE_NULLABLE = "nullable"
private const val ATTRIBUTE_GRAPH = "graph"

const val VALUE_NULL = "@null"
private const val VALUE_TRUE = "true"
private const val VALUE_FALSE = "false"

private const val NAMESPACE_RES_AUTO = "http://schemas.android.com/apk/res-auto"
private const val NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"

internal class NavParser(
    private val parser: XmlPositionParser,
    private val context: Context,
    private val rFilePackage: String,
    private val applicationId: String
) {

    companion object {
        fun parseNavigationFile(
            navigationXml: File,
            rFilePackage: String,
            applicationId: String,
            context: Context
        ): Destination {
            FileReader(navigationXml).use { reader ->
                val parser = XmlPositionParser(navigationXml.path, reader, context.logger)
                parser.traverseStartTags { true }
                return NavParser(parser, context, rFilePackage, applicationId).parseDestination()
            }
        }
    }

    internal fun parseDestination(): Destination {
        val position = parser.xmlPosition()
        val type = parser.name()
        val name = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_NAME) ?: ""
        val idValue = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_ID)
        val args = mutableListOf<Argument>()
        val actions = mutableListOf<Action>()
        val nested = mutableListOf<Destination>()
        val included = mutableListOf<IncludedDestination>()
        parser.traverseInnerStartTags {
            when {
                parser.name() == TAG_ACTION -> actions.add(parseAction())
                parser.name() == TAG_ARGUMENT -> args.add(parseArgument())
                parser.name() == TAG_INCLUDE -> included.add(parseIncludeDestination())
                type == TAG_NAVIGATION -> nested.add(parseDestination())
            }
        }

        actions.groupBy { it.id.javaIdentifier.toCamelCase() }.forEach { (sanitizedName, actions) ->
            if (actions.size > 1) {
                context.logger.error(sameSanitizedNameActions(sanitizedName, actions), position)
            }
        }

        args.groupBy { it.sanitizedName }.forEach { (sanitizedName, args) ->
            if (args.size > 1) {
                context.logger.error(sameSanitizedNameArguments(sanitizedName, args), position)
            }
        }

        val id = idValue?.let { parseId(idValue, rFilePackage, position) }
        val className = Destination.createName(id, name, applicationId)
        if (className == null && (actions.isNotEmpty() || args.isNotEmpty())) {
            context.logger.error(NavParserErrors.UNNAMED_DESTINATION, position)
            return context.createStubDestination()
        }

        return Destination(id, className, type, args, actions, nested, included)
    }

    private fun parseIncludeDestination(): IncludedDestination {
        val xmlPosition = parser.xmlPosition()

        val graphValue = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_GRAPH)
        if (graphValue == null) {
            context.logger.error(NavParserErrors.MISSING_GRAPH_ATTR, xmlPosition)
            return context.createStubIncludedDestination()
        }

        val graphRef = parseReference(graphValue, rFilePackage)
        if (graphRef == null || graphRef.resType != "navigation") {
            context.logger.error(NavParserErrors.invalidNavReference(graphValue), xmlPosition)
            return context.createStubIncludedDestination()
        }

        return IncludedDestination(graphRef)
    }

    private fun parseArgument(): Argument {
        val xmlPosition = parser.xmlPosition()
        val name = parser.attrValueOrError(NAMESPACE_ANDROID, ATTRIBUTE_NAME)
        val defaultValue = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_DEFAULT_VALUE)
        val typeString = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_TYPE)
        val nullable = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_NULLABLE)?.let {
            it == VALUE_TRUE
        } ?: false

        if (name == null) return context.createStubArg()

        if (parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_TYPE_DEPRECATED) != null) {
            context.logger.error(NavParserErrors.deprecatedTypeAttrUsed(name), xmlPosition)
            return context.createStubArg()
        }

        if (typeString == null && defaultValue != null) {
            return inferArgument(name, defaultValue, rFilePackage)
        }

        val type = NavType.from(typeString, rFilePackage)
        if (nullable && !type.allowsNullable()) {
            context.logger.error(NavParserErrors.typeIsNotNullable(typeString), xmlPosition)
            return context.createStubArg()
        }

        if (defaultValue == null) {
            return Argument(name, type, null, nullable)
        }

        val defaultTypedValue = when (type) {
            IntType -> parseIntValue(defaultValue)
            LongType -> parseLongValue(defaultValue)
            FloatType -> parseFloatValue(defaultValue)
            BoolType -> parseBoolean(defaultValue)
            ReferenceType -> parseReference(defaultValue, rFilePackage)?.let {
                ReferenceValue(it)
            }
            StringType -> {
                if (defaultValue == VALUE_NULL) {
                    NullValue
                } else {
                    StringValue(defaultValue)
                }
            }
            IntArrayType, LongArrayType, FloatArrayType, StringArrayType,
            BoolArrayType, ReferenceArrayType, is ObjectArrayType -> {
                if (defaultValue == VALUE_NULL) {
                    NullValue
                } else {
                    context.logger.error(
                            NavParserErrors.defaultValueObjectType(typeString),
                            xmlPosition
                    )
                    return context.createStubArg()
                }
            }
            is ObjectType -> {
                if (defaultValue == VALUE_NULL) {
                    NullValue
                } else {
                    EnumValue(type.typeName(), defaultValue)
                }
            }
        }

        if (defaultTypedValue == null) {
            val errorMessage = when (type) {
                ReferenceType -> NavParserErrors.invalidDefaultValueReference(defaultValue)
                else -> NavParserErrors.invalidDefaultValue(defaultValue, type)
            }
            context.logger.error(errorMessage, xmlPosition)
            return context.createStubArg()
        }

        if (!nullable && defaultTypedValue == NullValue) {
            context.logger.error(NavParserErrors.defaultNullButNotNullable(name), xmlPosition)
            return context.createStubArg()
        }

        return Argument(name, type, defaultTypedValue, nullable)
    }

    private fun parseAction(): Action {
        val idValue = parser.attrValueOrError(NAMESPACE_ANDROID, ATTRIBUTE_ID)
        val destValue = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_DESTINATION)
        val args = mutableListOf<Argument>()
        val position = parser.xmlPosition()
        parser.traverseInnerStartTags {
            if (parser.name() == TAG_ARGUMENT) {
                args.add(parseArgument())
            }
        }

        args.groupBy { it.sanitizedName }.forEach { (sanitizedName, args) ->
            if (args.size > 1) {
                context.logger.error(sameSanitizedNameArguments(sanitizedName, args), position)
            }
        }

        val id = if (idValue != null) {
            parseId(idValue, rFilePackage, position)
        } else {
            context.createStubId()
        }
        val destination = destValue?.let { parseId(destValue, rFilePackage, position) }
        return Action(id, destination, args)
    }

    private fun parseId(
        xmlId: String,
        rFilePackage: String,
        xmlPosition: XmlPosition
    ): ResReference {
        val ref = parseReference(xmlId, rFilePackage)
        if (ref?.isId() == true) {
            return ref
        }
        context.logger.error(NavParserErrors.invalidId(xmlId), xmlPosition)
        return context.createStubId()
    }
}

internal fun inferArgument(name: String, defaultValue: String, rFilePackage: String): Argument {
    val reference = parseReference(defaultValue, rFilePackage)
    if (reference != null) {
        return Argument(name, ReferenceType, ReferenceValue(reference))
    }
    val longValue = parseLongValue(defaultValue)
    if (longValue != null) {
        return Argument(name, LongType, longValue)
    }
    val intValue = parseIntValue(defaultValue)
    if (intValue != null) {
        return Argument(name, IntType, intValue)
    }
    val floatValue = parseFloatValue(defaultValue)
    if (floatValue != null) {
        return Argument(name, FloatType, floatValue)
    }
    val boolValue = parseBoolean(defaultValue)
    if (boolValue != null) {
        return Argument(name, BoolType, boolValue)
    }
    return Argument(name, StringType, StringValue(defaultValue))
}

// @[+][package:]id/resource_name -> package.R.id.resource_name
private val RESOURCE_REGEX = Regex("^@[+]?(.+?:)?(.+?)/(.+)$")

internal fun parseReference(xmlValue: String, rFilePackage: String): ResReference? {
    val matchEntire = RESOURCE_REGEX.matchEntire(xmlValue) ?: return null
    val groups = matchEntire.groupValues
    val resourceName = groups.last()
    val resType = groups[groups.size - 2]
    val packageName = if (groups[1].isNotEmpty()) groups[1].removeSuffix(":") else rFilePackage
    return ResReference(packageName, resType, resourceName)
}

internal fun parseIntValue(value: String): IntValue? {
    try {
        if (value.startsWith("0x")) {
            Integer.parseUnsignedInt(value.substring(2), 16)
        } else {
            Integer.parseInt(value)
        }
    } catch (ex: NumberFormatException) {
        return null
    }
    return IntValue(value)
}

internal fun parseLongValue(value: String): LongValue? {
    if (!value.endsWith('L')) {
        return null
    }
    try {
        val normalizedValue = value.substringBeforeLast('L')
        if (normalizedValue.startsWith("0x")) {
            normalizedValue.substring(2).toLong(16)
        } else {
            normalizedValue.toLong()
        }
    } catch (ex: NumberFormatException) {
        return null
    }
    return LongValue(value)
}

private fun parseFloatValue(value: String): FloatValue? =
        value.toFloatOrNull()?.let { FloatValue(value) }

private fun parseBoolean(value: String): BooleanValue? {
    if (value == VALUE_TRUE || value == VALUE_FALSE) {
        return BooleanValue(value)
    }
    return null
}
