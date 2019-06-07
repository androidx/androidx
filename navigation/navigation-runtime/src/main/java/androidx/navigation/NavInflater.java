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

package androidx.navigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import androidx.annotation.NavigationRes;
import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Class which translates a navigation XML file into a {@link NavGraph}
 */
public final class NavInflater {
    private static final String TAG_ARGUMENT = "argument";
    private static final String TAG_DEEP_LINK = "deepLink";
    private static final String TAG_ACTION = "action";
    private static final String TAG_INCLUDE = "include";
    static final String APPLICATION_ID_PLACEHOLDER = "${applicationId}";

    private static final ThreadLocal<TypedValue> sTmpValue = new ThreadLocal<>();

    private Context mContext;
    private NavigatorProvider mNavigatorProvider;

    public NavInflater(@NonNull Context context, @NonNull NavigatorProvider navigatorProvider) {
        mContext = context;
        mNavigatorProvider = navigatorProvider;
    }

    /**
     * Inflate a NavGraph from the given XML resource id.
     *
     * @param graphResId
     * @return
     */
    @SuppressLint("ResourceType")
    @NonNull
    public NavGraph inflate(@NavigationRes int graphResId) {
        Resources res = mContext.getResources();
        XmlResourceParser parser = res.getXml(graphResId);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        try {
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                // Empty loop
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }

            String rootElement = parser.getName();
            NavDestination destination = inflate(res, parser, attrs, graphResId);
            if (!(destination instanceof NavGraph)) {
                throw new IllegalArgumentException("Root element <" + rootElement + ">"
                        + " did not inflate into a NavGraph");
            }
            return (NavGraph) destination;
        } catch (Exception e) {
            throw new RuntimeException("Exception inflating "
                    + res.getResourceName(graphResId) + " line "
                    + parser.getLineNumber(), e);
        } finally {
            parser.close();
        }
    }

    @NonNull
    private NavDestination inflate(@NonNull Resources res, @NonNull XmlResourceParser parser,
            @NonNull AttributeSet attrs, int graphResId)
            throws XmlPullParserException, IOException {
        Navigator navigator = mNavigatorProvider.getNavigator(parser.getName());
        final NavDestination dest = navigator.createDestination();

        dest.onInflate(mContext, attrs);

        final int innerDepth = parser.getDepth() + 1;
        int type;
        int depth;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth
                || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (depth > innerDepth) {
                continue;
            }

            final String name = parser.getName();
            if (TAG_ARGUMENT.equals(name)) {
                inflateArgumentForDestination(res, dest, attrs, graphResId);
            } else if (TAG_DEEP_LINK.equals(name)) {
                inflateDeepLink(res, dest, attrs);
            } else if (TAG_ACTION.equals(name)) {
                inflateAction(res, dest, attrs, parser, graphResId);
            } else if (TAG_INCLUDE.equals(name) && dest instanceof NavGraph) {
                final TypedArray a = res.obtainAttributes(attrs, R.styleable.NavInclude);
                final int id = a.getResourceId(R.styleable.NavInclude_graph, 0);
                ((NavGraph) dest).addDestination(inflate(id));
                a.recycle();
            } else if (dest instanceof NavGraph) {
                ((NavGraph) dest).addDestination(inflate(res, parser, attrs, graphResId));
            }
        }

        return dest;
    }

    private void inflateArgumentForDestination(@NonNull Resources res, @NonNull NavDestination dest,
            @NonNull AttributeSet attrs, int graphResId) throws XmlPullParserException {
        final TypedArray a = res.obtainAttributes(attrs, R.styleable.NavArgument);
        String name = a.getString(R.styleable.NavArgument_android_name);
        if (name == null) {
            throw new XmlPullParserException("Arguments must have a name");
        }
        NavArgument argument = inflateArgument(a, res, graphResId);
        dest.addArgument(name, argument);
        a.recycle();
    }

    private void inflateArgumentForBundle(@NonNull Resources res, @NonNull Bundle bundle,
            @NonNull AttributeSet attrs, int graphResId) throws XmlPullParserException {
        final TypedArray a = res.obtainAttributes(attrs, R.styleable.NavArgument);
        String name = a.getString(R.styleable.NavArgument_android_name);
        if (name == null) {
            throw new XmlPullParserException("Arguments must have a name");
        }
        NavArgument argument = inflateArgument(a, res, graphResId);
        if (argument.isDefaultValuePresent()) {
            argument.putDefaultValue(name, bundle);
        }
        a.recycle();
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private NavArgument inflateArgument(@NonNull TypedArray a, @NonNull Resources res,
            int graphResId) throws XmlPullParserException {
        NavArgument.Builder argumentBuilder = new NavArgument.Builder();
        argumentBuilder.setIsNullable(a.getBoolean(R.styleable.NavArgument_nullable, false));

        TypedValue value = sTmpValue.get();
        if (value == null) {
            value = new TypedValue();
            sTmpValue.set(value);
        }

        Object defaultValue = null;
        NavType navType = null;
        String argType = a.getString(R.styleable.NavArgument_argType);
        if (argType != null) {
            navType = NavType.fromArgType(argType, res.getResourcePackageName(graphResId));
        }

        if (a.getValue(R.styleable.NavArgument_android_defaultValue, value)) {
            if (navType == NavType.ReferenceType) {
                if (value.resourceId != 0) {
                    defaultValue = value.resourceId;
                } else if (value.type == TypedValue.TYPE_FIRST_INT && value.data == 0) {
                    // Support "0" as a default value for reference types
                    defaultValue = 0;
                } else {
                    throw new XmlPullParserException(
                            "unsupported value '" + value.string
                                    + "' for " + navType.getName()
                                    + ". Must be a reference to a resource.");
                }
            } else if (value.resourceId != 0) {
                if (navType == null) {
                    navType = NavType.ReferenceType;
                    defaultValue = value.resourceId;
                } else {
                    throw new XmlPullParserException(
                            "unsupported value '" + value.string
                                    + "' for " + navType.getName()
                                    + ". You must use a \"" + NavType.ReferenceType.getName()
                                    + "\" type to reference other resources.");
                }
            } else if (navType == NavType.StringType) {
                defaultValue = a.getString(R.styleable.NavArgument_android_defaultValue);
            } else {
                switch (value.type) {
                    case TypedValue.TYPE_STRING:
                        String stringValue = value.string.toString();
                        if (navType == null) {
                            navType = NavType.inferFromValue(stringValue);
                        }
                        defaultValue = navType.parseValue(stringValue);
                        break;
                    case TypedValue.TYPE_DIMENSION:
                        navType = checkNavType(value, navType, NavType.IntType,
                                argType, "dimension");
                        defaultValue = (int) value.getDimension(res.getDisplayMetrics());
                        break;
                    case TypedValue.TYPE_FLOAT:
                        navType = checkNavType(value, navType, NavType.FloatType,
                                argType, "float");
                        defaultValue = value.getFloat();
                        break;
                    case TypedValue.TYPE_INT_BOOLEAN:
                        navType = checkNavType(value, navType, NavType.BoolType,
                                argType, "boolean");
                        defaultValue = value.data != 0;
                        break;
                    default:
                        if (value.type >= TypedValue.TYPE_FIRST_INT
                                && value.type <= TypedValue.TYPE_LAST_INT) {
                            navType = checkNavType(value, navType, NavType.IntType,
                                    argType, "integer");
                            defaultValue = value.data;
                        } else {
                            throw new XmlPullParserException(
                                    "unsupported argument type " + value.type);
                        }
                }
            }
        }

        if (defaultValue != null) {
            argumentBuilder.setDefaultValue(defaultValue);
        }
        if (navType != null) {
            argumentBuilder.setType(navType);
        }
        return argumentBuilder.build();
    }

    private static NavType checkNavType(TypedValue value, NavType navType,
            NavType expectedNavType, String argType, String foundType)
            throws XmlPullParserException {
        if (navType != null && navType != expectedNavType) {
            throw new XmlPullParserException(
                    "Type is " + argType + " but found " + foundType + ": " + value.data);
        }
        return navType != null ? navType : expectedNavType;
    }

    private void inflateDeepLink(@NonNull Resources res, @NonNull NavDestination dest,
            @NonNull AttributeSet attrs) {
        final TypedArray a = res.obtainAttributes(attrs, R.styleable.NavDeepLink);
        String uri = a.getString(R.styleable.NavDeepLink_uri);
        if (TextUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("Every <" + TAG_DEEP_LINK
                    + "> must include an app:uri");
        }
        uri = uri.replace(APPLICATION_ID_PLACEHOLDER, mContext.getPackageName());
        dest.addDeepLink(uri);
        a.recycle();
    }

    private void inflateAction(@NonNull Resources res, @NonNull NavDestination dest,
            @NonNull AttributeSet attrs, XmlResourceParser parser, int graphResId)
            throws IOException, XmlPullParserException {
        final TypedArray a = res.obtainAttributes(attrs, R.styleable.NavAction);
        final int id = a.getResourceId(R.styleable.NavAction_android_id, 0);
        final int destId = a.getResourceId(R.styleable.NavAction_destination, 0);
        NavAction action = new NavAction(destId);

        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setLaunchSingleTop(a.getBoolean(R.styleable.NavAction_launchSingleTop, false));
        builder.setPopUpTo(a.getResourceId(R.styleable.NavAction_popUpTo, -1),
                a.getBoolean(R.styleable.NavAction_popUpToInclusive, false));
        builder.setEnterAnim(a.getResourceId(R.styleable.NavAction_enterAnim, -1));
        builder.setExitAnim(a.getResourceId(R.styleable.NavAction_exitAnim, -1));
        builder.setPopEnterAnim(a.getResourceId(R.styleable.NavAction_popEnterAnim, -1));
        builder.setPopExitAnim(a.getResourceId(R.styleable.NavAction_popExitAnim, -1));
        action.setNavOptions(builder.build());

        Bundle args = new Bundle();
        final int innerDepth = parser.getDepth() + 1;
        int type;
        int depth;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth
                || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (depth > innerDepth) {
                continue;
            }
            final String name = parser.getName();
            if (TAG_ARGUMENT.equals(name)) {
                inflateArgumentForBundle(res, args, attrs, graphResId);
            }
        }
        if (!args.isEmpty()) {
            action.setDefaultArguments(args);
        }
        dest.putAction(id, action);
        a.recycle();
    }
}
