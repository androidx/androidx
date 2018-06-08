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

import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.ResReference
import java.io.File
import java.io.FileReader

private const val TAG_NAVIGATION = "navigation"
private const val TAG_ACTION = "action"
private const val TAG_ARGUMENT = "argument"

private const val ATTRIBUTE_ID = "id"
private const val ATTRIBUTE_DESTINATION = "destination"
private const val ATTRIBUTE_DEFAULT_VALUE = "defaultValue"
private const val ATTRIBUTE_NAME = "name"
private const val ATTRIBUTE_TYPE = "type"

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
        parser.traverseInnerStartTags {
            when {
                parser.name() == TAG_ACTION -> actions.add(parseAction())
                parser.name() == TAG_ARGUMENT -> args.add(parseArgument())
                type == TAG_NAVIGATION -> nested.add(parseDestination())
            }
        }

        val id = idValue?.let { parseId(idValue, rFilePackage, position) }
        val className = Destination.createName(id, name, applicationId)
        if (className == null && (actions.isNotEmpty() || args.isNotEmpty())) {
            context.logger.error(NavParserErrors.UNNAMED_DESTINATION, position)
            return context.createStubDestination()
        }

        return Destination(id, className, type, args, actions, nested)
    }

    private fun parseArgument(): Argument {
        val xmlPosition = parser.xmlPosition()
        val name = parser.attrValueOrError(NAMESPACE_ANDROID, ATTRIBUTE_NAME)
        val defaultValue = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_DEFAULT_VALUE)
        val typeString = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_TYPE)
        if (name == null) return context.createStubArg()

        if (typeString == null && defaultValue != null) {
            return inferArgument(name, defaultValue, rFilePackage)
        }

        val type = NavType.from(typeString)
        if (type == null) {
            context.logger.error(NavParserErrors.unknownType(typeString), xmlPosition)
            return context.createStubArg()
        }

        if (defaultValue == null) {
            return Argument(name, type, null)
        }

        val defaultTypedValue = when (type) {
            NavType.INT -> parseIntValue(defaultValue)
            NavType.FLOAT -> parseFloatValue(defaultValue)
            NavType.BOOLEAN -> parseBoolean(defaultValue)
            NavType.REFERENCE -> parseReference(defaultValue, rFilePackage)?.let {
                ReferenceValue(it)
            }
            NavType.STRING -> StringValue(defaultValue)
        }

        if (defaultTypedValue == null) {
            val errorMessage = when (type) {
                NavType.REFERENCE -> NavParserErrors.invalidDefaultValueReference(defaultValue)
                else -> NavParserErrors.invalidDefaultValue(defaultValue, type)
            }
            context.logger.error(errorMessage, xmlPosition)
            return context.createStubArg()
        }

        return Argument(name, type, defaultTypedValue)
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
        return Argument(name, NavType.REFERENCE, ReferenceValue(reference))
    }
    val intValue = parseIntValue(defaultValue)
    if (intValue != null) {
        return Argument(name, NavType.INT, intValue)
    }
    val floatValue = parseFloatValue(defaultValue)
    if (floatValue != null) {
        return Argument(name, NavType.FLOAT, floatValue)
    }
    val boolValue = parseBoolean(defaultValue)
    if (boolValue != null) {
        return Argument(name, NavType.BOOLEAN, boolValue)
    }
    return Argument(name, NavType.STRING, StringValue(defaultValue))
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

private fun parseFloatValue(value: String): FloatValue? =
        value.toFloatOrNull()?.let { FloatValue(value) }

private fun parseBoolean(value: String): BooleanValue? {
    if (value == "true" || value == "false") {
        return BooleanValue(value)
    }
    return null
}
