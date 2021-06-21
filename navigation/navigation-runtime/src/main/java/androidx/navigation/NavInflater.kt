/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.util.Xml
import androidx.annotation.NavigationRes
import androidx.annotation.RestrictTo
import androidx.core.content.res.use
import androidx.core.content.withStyledAttributes
import androidx.navigation.common.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * Class which translates a navigation XML file into a [NavGraph]
 */
public class NavInflater(
    private val context: Context,
    private val navigatorProvider: NavigatorProvider
) {
    /**
     * Inflate a NavGraph from the given XML resource id.
     *
     * @param graphResId
     * @return
     */
    @SuppressLint("ResourceType")
    public fun inflate(@NavigationRes graphResId: Int): NavGraph {
        val res = context.resources
        val parser = res.getXml(graphResId)
        val attrs = Xml.asAttributeSet(parser)
        return try {
            var type: Int
            while (parser.next().also { type = it } != XmlPullParser.START_TAG &&
                type != XmlPullParser.END_DOCUMENT
            ) { /* Empty loop */
            }
            if (type != XmlPullParser.START_TAG) {
                throw XmlPullParserException("No start tag found")
            }
            val rootElement = parser.name
            val destination = inflate(res, parser, attrs, graphResId)
            require(destination is NavGraph) {
                "Root element <$rootElement> did not inflate into a NavGraph"
            }
            destination
        } catch (e: Exception) {
            throw RuntimeException(
                "Exception inflating ${res.getResourceName(graphResId)} line ${parser.lineNumber}",
                e
            )
        } finally {
            parser.close()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun inflate(
        res: Resources,
        parser: XmlResourceParser,
        attrs: AttributeSet,
        graphResId: Int
    ): NavDestination {
        val navigator = navigatorProvider.getNavigator<Navigator<*>>(parser.name)
        val dest = navigator.createDestination()
        dest.onInflate(context, attrs)
        val innerDepth = parser.depth + 1
        var type: Int
        var depth = 0
        while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT &&
            (parser.depth.also { depth = it } >= innerDepth || type != XmlPullParser.END_TAG)
        ) {
            if (type != XmlPullParser.START_TAG) {
                continue
            }
            if (depth > innerDepth) {
                continue
            }
            val name = parser.name
            if (TAG_ARGUMENT == name) {
                inflateArgumentForDestination(res, dest, attrs, graphResId)
            } else if (TAG_DEEP_LINK == name) {
                inflateDeepLink(res, dest, attrs)
            } else if (TAG_ACTION == name) {
                inflateAction(res, dest, attrs, parser, graphResId)
            } else if (TAG_INCLUDE == name && dest is NavGraph) {
                res.obtainAttributes(attrs, androidx.navigation.R.styleable.NavInclude).use {
                    val id = it.getResourceId(androidx.navigation.R.styleable.NavInclude_graph, 0)
                    dest.addDestination(inflate(id))
                }
            } else if (dest is NavGraph) {
                dest.addDestination(inflate(res, parser, attrs, graphResId))
            }
        }
        return dest
    }

    @Throws(XmlPullParserException::class)
    private fun inflateArgumentForDestination(
        res: Resources,
        dest: NavDestination,
        attrs: AttributeSet,
        graphResId: Int
    ) {
        res.obtainAttributes(attrs, R.styleable.NavArgument).use { array ->
            val name = array.getString(R.styleable.NavArgument_android_name)
                ?: throw XmlPullParserException("Arguments must have a name")
            val argument = inflateArgument(array, res, graphResId)
            dest.addArgument(name, argument)
        }
    }

    @Throws(XmlPullParserException::class)
    private fun inflateArgumentForBundle(
        res: Resources,
        bundle: Bundle,
        attrs: AttributeSet,
        graphResId: Int
    ) {
        res.obtainAttributes(attrs, R.styleable.NavArgument).use { array ->
            val name = array.getString(R.styleable.NavArgument_android_name)
                ?: throw XmlPullParserException("Arguments must have a name")
            val argument = inflateArgument(array, res, graphResId)
            if (argument.isDefaultValuePresent) {
                argument.putDefaultValue(name, bundle)
            }
        }
    }

    @Throws(XmlPullParserException::class)
    private fun inflateArgument(a: TypedArray, res: Resources, graphResId: Int): NavArgument {
        val argumentBuilder = NavArgument.Builder()
        argumentBuilder.setIsNullable(a.getBoolean(R.styleable.NavArgument_nullable, false))
        var value = sTmpValue.get()
        if (value == null) {
            value = TypedValue()
            sTmpValue.set(value)
        }
        var defaultValue: Any? = null
        var navType: NavType<*>? = null
        val argType = a.getString(R.styleable.NavArgument_argType)
        if (argType != null) {
            navType = NavType.fromArgType(argType, res.getResourcePackageName(graphResId))
        }
        if (a.getValue(R.styleable.NavArgument_android_defaultValue, value)) {
            if (navType === NavType.ReferenceType) {
                defaultValue = if (value.resourceId != 0) {
                    value.resourceId
                } else if (value.type == TypedValue.TYPE_FIRST_INT && value.data == 0) {
                    // Support "0" as a default value for reference types
                    0
                } else {
                    throw XmlPullParserException(
                        "unsupported value '${value.string}' for ${navType.name}. Must be a " +
                            "reference to a resource."
                    )
                }
            } else if (value.resourceId != 0) {
                if (navType == null) {
                    navType = NavType.ReferenceType
                    defaultValue = value.resourceId
                } else {
                    throw XmlPullParserException(
                        "unsupported value '${value.string}' for ${navType.name}. You must use a " +
                            "\"${NavType.ReferenceType.name}\" type to reference other resources."
                    )
                }
            } else if (navType === NavType.StringType) {
                defaultValue = a.getString(R.styleable.NavArgument_android_defaultValue)
            } else {
                when (value.type) {
                    TypedValue.TYPE_STRING -> {
                        val stringValue = value.string.toString()
                        if (navType == null) {
                            navType = NavType.inferFromValue(stringValue)
                        }
                        defaultValue = navType.parseValue(stringValue)
                    }
                    TypedValue.TYPE_DIMENSION -> {
                        navType = checkNavType(
                            value, navType, NavType.IntType, argType, "dimension"
                        )
                        defaultValue = value.getDimension(res.displayMetrics).toInt()
                    }
                    TypedValue.TYPE_FLOAT -> {
                        navType = checkNavType(value, navType, NavType.FloatType, argType, "float")
                        defaultValue = value.float
                    }
                    TypedValue.TYPE_INT_BOOLEAN -> {
                        navType = checkNavType(value, navType, NavType.BoolType, argType, "boolean")
                        defaultValue = value.data != 0
                    }
                    else ->
                        if (value.type >= TypedValue.TYPE_FIRST_INT &&
                            value.type <= TypedValue.TYPE_LAST_INT
                        ) {
                            if (navType === NavType.FloatType) {
                                navType = checkNavType(
                                    value, navType, NavType.FloatType, argType, "float"
                                )
                                defaultValue = value.data.toFloat()
                            } else {
                                navType = checkNavType(
                                    value, navType, NavType.IntType, argType, "integer"
                                )
                                defaultValue = value.data
                            }
                        } else {
                            throw XmlPullParserException("unsupported argument type ${value.type}")
                        }
                }
            }
        }
        if (defaultValue != null) {
            argumentBuilder.setDefaultValue(defaultValue)
        }
        if (navType != null) {
            argumentBuilder.setType(navType)
        }
        return argumentBuilder.build()
    }

    @Throws(XmlPullParserException::class)
    private fun inflateDeepLink(res: Resources, dest: NavDestination, attrs: AttributeSet) {
        res.obtainAttributes(attrs, R.styleable.NavDeepLink).use { array ->
            val uri = array.getString(R.styleable.NavDeepLink_uri)
            val action = array.getString(R.styleable.NavDeepLink_action)
            val mimeType = array.getString(R.styleable.NavDeepLink_mimeType)
            if (uri.isNullOrEmpty() && action.isNullOrEmpty() && mimeType.isNullOrEmpty()) {
                throw XmlPullParserException(
                    "Every <$TAG_DEEP_LINK> must include at least one of app:uri, app:action, or " +
                        "app:mimeType"
                )
            }
            val builder = NavDeepLink.Builder()
            if (uri != null) {
                builder.setUriPattern(uri.replace(APPLICATION_ID_PLACEHOLDER, context.packageName))
            }
            if (!action.isNullOrEmpty()) {
                builder.setAction(action.replace(APPLICATION_ID_PLACEHOLDER, context.packageName))
            }
            if (mimeType != null) {
                builder.setMimeType(
                    mimeType.replace(
                        APPLICATION_ID_PLACEHOLDER,
                        context.packageName
                    )
                )
            }
            dest.addDeepLink(builder.build())
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun inflateAction(
        res: Resources,
        dest: NavDestination,
        attrs: AttributeSet,
        parser: XmlResourceParser,
        graphResId: Int
    ) {
        context.withStyledAttributes(attrs, R.styleable.NavAction) {
            val id = getResourceId(R.styleable.NavAction_android_id, 0)
            val destId = getResourceId(R.styleable.NavAction_destination, 0)
            val action = NavAction(destId)
            val builder = NavOptions.Builder()
            builder.setLaunchSingleTop(getBoolean(R.styleable.NavAction_launchSingleTop, false))
            builder.setRestoreState(getBoolean(R.styleable.NavAction_restoreState, false))
            builder.setPopUpTo(
                getResourceId(R.styleable.NavAction_popUpTo, -1),
                getBoolean(R.styleable.NavAction_popUpToInclusive, false),
                getBoolean(R.styleable.NavAction_popUpToSaveState, false)
            )
            builder.setEnterAnim(getResourceId(R.styleable.NavAction_enterAnim, -1))
            builder.setExitAnim(getResourceId(R.styleable.NavAction_exitAnim, -1))
            builder.setPopEnterAnim(getResourceId(R.styleable.NavAction_popEnterAnim, -1))
            builder.setPopExitAnim(getResourceId(R.styleable.NavAction_popExitAnim, -1))
            action.navOptions = builder.build()
            val args = Bundle()
            val innerDepth = parser.depth + 1
            var type: Int
            var depth = 0
            while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT &&
                (parser.depth.also { depth = it } >= innerDepth || type != XmlPullParser.END_TAG)
            ) {
                if (type != XmlPullParser.START_TAG) {
                    continue
                }
                if (depth > innerDepth) {
                    continue
                }
                val name = parser.name
                if (TAG_ARGUMENT == name) {
                    inflateArgumentForBundle(res, args, attrs, graphResId)
                }
            }
            if (!args.isEmpty) {
                action.defaultArguments = args
            }
            dest.putAction(id, action)
        }
    }

    public companion object {
        private const val TAG_ARGUMENT = "argument"
        private const val TAG_DEEP_LINK = "deepLink"
        private const val TAG_ACTION = "action"
        private const val TAG_INCLUDE = "include"

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val APPLICATION_ID_PLACEHOLDER: String = "\${applicationId}"
        private val sTmpValue = ThreadLocal<TypedValue>()
        @Throws(XmlPullParserException::class)
        internal fun checkNavType(
            value: TypedValue,
            navType: NavType<*>?,
            expectedNavType: NavType<*>,
            argType: String?,
            foundType: String
        ): NavType<*> {
            if (navType != null && navType !== expectedNavType) {
                throw XmlPullParserException("Type is $argType but found $foundType: ${value.data}")
            }
            return navType ?: expectedNavType
        }
    }
}