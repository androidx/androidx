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
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
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

private fun parseDestination(
        parser: XmlPullParser, rFilePackage: String,
        applicationId: String): Destination {
    val type = parser.name
    val name = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_NAME) ?: ""
    val idValue = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_ID)
    val args = mutableListOf<Argument>()
    val actions = mutableListOf<Action>()
    val nested = mutableListOf<Destination>()
    parser.traverseInnerStartTags {
        when {
            parser.name == TAG_ACTION -> actions.add(parseAction(parser, rFilePackage))
            parser.name == TAG_ARGUMENT -> args.add(parseArgument(parser, rFilePackage))
            type == TAG_NAVIGATION -> nested.add(parseDestination(parser, rFilePackage,
                    applicationId))
        }
    }

    val id = parseNullableId(idValue, rFilePackage)
    val className = Destination.createName(id, name, applicationId)
    if (className == null && (actions.isNotEmpty() || args.isNotEmpty())) {
        throw IllegalArgumentException("Destination with arguments or action mush have " +
                "either name either id attributes")
    }
    return Destination(id, className, type, args, actions, nested)
}

private fun parseArgument(parser: XmlPullParser, rFilePackage: String): Argument {
    val name = parser.attrValueOrThrow(NAMESPACE_ANDROID, ATTRIBUTE_NAME)
    val defaultValue = parser.attrValue(NAMESPACE_ANDROID, ATTRIBUTE_DEFAULT_VALUE)
    val typeString = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_TYPE)
    if (typeString == null && defaultValue != null) {
        return inferArgument(name, defaultValue, rFilePackage)
    }

    val (type, defaultTypedValue) = when (typeString) {
        "integer" -> NavType.INT to defaultValue?.let { parseIntValue(defaultValue) }
        "float" -> NavType.FLOAT to defaultValue?.let { parseFloatValue(defaultValue) }
        "boolean" -> NavType.BOOLEAN to defaultValue?.let { parseBooleanValue(defaultValue) }
        "reference" -> NavType.REFERENCE to defaultValue?.let {
            ReferenceValue(parseReference(defaultValue, rFilePackage))
        }
        else -> NavType.STRING to defaultValue?.let { StringValue(it) }
    }
    return Argument(name, type, defaultTypedValue)
}

internal fun inferArgument(name: String, defaultValue: String, rFilePackage: String): Argument {
    val reference = tryToParseReference(defaultValue, rFilePackage)
    if (reference != null) {
        return Argument(name, NavType.REFERENCE, ReferenceValue(reference))
    }
    val intValue = tryToParseIntValue(defaultValue)
    if (intValue != null) {
        return Argument(name, NavType.INT, intValue)
    }
    val floatValue = tryToParseFloatValue(defaultValue)
    if (floatValue != null) {
        return Argument(name, NavType.FLOAT, floatValue)
    }
    val boolValue = tryToParseBoolean(defaultValue)
    if (boolValue != null) {
        return Argument(name, NavType.BOOLEAN, boolValue)
    }
    return Argument(name, NavType.STRING, StringValue(defaultValue))
}

private fun parseAction(parser: XmlPullParser, rFilePackage: String): Action {
    val idValue = parser.attrValueOrThrow(NAMESPACE_ANDROID, ATTRIBUTE_ID)
    val destValue = parser.attrValue(NAMESPACE_RES_AUTO, ATTRIBUTE_DESTINATION)
    val args = mutableListOf<Argument>()
    parser.traverseInnerStartTags {
        if (parser.name == TAG_ARGUMENT) {
            args.add(parseArgument(parser, rFilePackage))
        }
    }
    return Action(parseId(idValue, rFilePackage),
            parseNullableId(destValue, rFilePackage), args)
}

fun parseNavigationFile(navigationXml: File, rFilePackage: String,
        applicationId: String): Destination {
    FileReader(navigationXml).use { reader ->
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(reader)
        }
        parser.traverseStartTags { true }
        return parseDestination(parser, rFilePackage, applicationId)
    }
}

// @[+][package:]id/resource_name -> package.R.id.resource_name
private val RESOURCE_REGEX = Regex("^@[+]?(.+?:)?(.+?)/(.+)$")

internal fun parseReference(xmlValue: String, rFilePackage: String): ResReference {
    return tryToParseReference(xmlValue, rFilePackage) ?:
            throw IllegalArgumentException("id should be in format: " +
                    "@[+][package:]res_type/resource_name, but is: $xmlValue")
}

internal fun tryToParseReference(xmlValue: String, rFilePackage: String): ResReference? {
    val matchEntire = RESOURCE_REGEX.matchEntire(xmlValue) ?: return null
    val groups = matchEntire.groupValues
    val resourceName = groups.last()
    val resType = groups[groups.size - 2]
    val packageName = if (groups[1].isNotEmpty()) groups[1].removeSuffix(":") else rFilePackage
    return ResReference(packageName, resType, resourceName)
}

internal fun parseId(xmlId: String, rFilePackage: String): ResReference {
    val ref = parseReference(xmlId, rFilePackage)
    if (!ref.isId()) {
        throw IllegalArgumentException("$xmlId was passed as id, but is ${ref.resType}")
    }
    return ref
}

internal fun parseNullableId(xmlId: String?, rFilePackage: String): ResReference? = xmlId?.let {
    parseId(it, rFilePackage)
}

private fun tryToParseIntValue(value: String): IntValue? {
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

internal fun parseIntValue(value: String): IntValue {
    return tryToParseIntValue(value)
            ?: throw IllegalArgumentException("Failed to parse $value as int")
}

private fun tryToParseFloatValue(value: String): FloatValue? =
        value.toFloatOrNull()?.let { FloatValue(value) }

internal fun parseFloatValue(value: String): FloatValue {
    return tryToParseFloatValue(value)
            ?: throw IllegalArgumentException("Failed to parse $value as float")
}

private fun tryToParseBoolean(value: String): BooleanValue? {
    if (value == "true" || value == "false") {
        return BooleanValue(value)
    }
    return null
}

internal fun parseBooleanValue(value: String): BooleanValue {
    return tryToParseBoolean(value)
            ?: throw IllegalArgumentException("Failed to parse $value as boolean")
}