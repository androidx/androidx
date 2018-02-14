/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.navigation.safe.args.generator

import android.arch.navigation.safe.args.generator.models.Action
import android.arch.navigation.safe.args.generator.models.Argument
import android.arch.navigation.safe.args.generator.models.Destination
import android.arch.navigation.safe.args.generator.models.ResReference
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

private fun parseDestination(parser: XmlPullParser, rFilePackage: String,
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

    val (type, defaultTypedValue) = when (typeString) {
        "integer" -> NavType.INT to parseIntValue(defaultValue)
        "reference" -> NavType.REFERENCE to parseReferenceValue(defaultValue, rFilePackage)
        else -> NavType.STRING to defaultValue?.let { StringValue(it) }
    }
    return Argument(name, type, defaultTypedValue)
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
internal fun parseReference(xmlValue: String, rFilePackage: String): ResReference {
    val split = xmlValue.removePrefix("@").removePrefix("+").split(":", "/")
    if (split.size != 2 && split.size != 3) {
        throw IllegalArgumentException("id should be in format: " +
                "@[+][package:]res_type/resource_name, but is: $xmlValue")
    }
    val resourceName = split.last()
    val resType = split[split.size - 2]
    val packageName = if (split.size == 3) split[0] else rFilePackage
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

internal fun parseIntValue(value: String?): IntValue? {
    if (value == null) {
        return null
    }
    try {
        Integer.parseInt(value)
        return IntValue(value)
    } catch (ex: NumberFormatException) {
        throw IllegalArgumentException("Failed to parse $value as int")
    }
}

internal fun parseReferenceValue(value: String?, rFilePackage: String) =
        value?.let { ReferenceValue(parseReference(value, rFilePackage)) }
