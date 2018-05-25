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
    private val parser: XmlContextParser,
    private val rFilePackage: String,
    private val applicationId: String) {

    companion object {
        fun parseNavigationFile(navigationXml: File, rFilePackage: String,
                                applicationId: String): Destination {
            FileReader(navigationXml).use { reader ->
                val parser = XmlContextParser(navigationXml.name, reader)
                parser.traverseStartTags { true }
                return NavParser(parser, rFilePackage, applicationId).parseDestination()
            }
        }
    }

    internal fun parseDestination(): Destination {
        val context = parser.xmlContext()
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

        val id = parseNullableId(idValue, rFilePackage, context)
        val className = Destination.createName(id, name, applicationId)
        if (className == null && (actions.isNotEmpty() || args.isNotEmpty())) {
            throw context.createError(NavParserErrors.UNNAMED_DESTINATION)
        }
        return Destination(id, className, type, args, actions, nested)
    }

    private fun parseArgument(): Argument {
        val xmlContext = parser.xmlContext()
        val name = parser.attrValueOrThrow(NAMESPACE_ANDROID, ATTRIBUTE_NAME)
        val defaultValue = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_DEFAULT_VALUE)
        val typeString = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_TYPE)

        if (typeString == null && defaultValue != null) {
            return inferArgument(name, defaultValue, rFilePackage)
        }

        val type = NavType.from(typeString)
            ?: throw xmlContext.createError(NavParserErrors.unknownType(typeString))

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
            throw xmlContext.createError(errorMessage)
        }

        return Argument(name, type, defaultTypedValue)
    }

    private fun parseAction(): Action {
        val idValue = parser.attrValueOrThrow(NAMESPACE_ANDROID, ATTRIBUTE_ID)
        val destValue = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_DESTINATION)
        val args = mutableListOf<Argument>()
        val context = parser.xmlContext()
        parser.traverseInnerStartTags {
            if (parser.name() == TAG_ARGUMENT) {
                args.add(parseArgument())
            }
        }
        val id = parseId(idValue, rFilePackage) ?:
            throw context.createError(NavParserErrors.invalidId(idValue))
        return Action(id, parseNullableId(destValue, rFilePackage, context), args)
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

internal fun parseId(xmlId: String, rFilePackage: String): ResReference? {
    val ref = parseReference(xmlId, rFilePackage)
    if (ref?.isId() == true) {
        return ref
    }
    return null
}

internal fun parseNullableId(xmlId: String?, rFilePackage: String,
                             context: XmlContext): ResReference? = xmlId?.let {
    parseId(it, rFilePackage) ?: throw context.createError(NavParserErrors.invalidId(xmlId))
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
