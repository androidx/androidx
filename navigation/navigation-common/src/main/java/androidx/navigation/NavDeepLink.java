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

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NavDeepLink encapsulates the parsing and matching of a navigation deep link.
 */
class NavDeepLink {
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z]+[+\\w\\-.]*:");

    private final ArrayList<String> mArguments = new ArrayList<>();
    private final Pattern mPattern;
    private final boolean mExactDeepLink;
    private final boolean mIsParameterizedQuery;
    private final Map<String, ParamQuery> mParamArgMap = new HashMap<>();

    /**
     * NavDestinations should be created via {@link Navigator#createDestination}.
     */
    NavDeepLink(@NonNull String uri) {
        Uri parameterizedUri = Uri.parse(uri);
        mIsParameterizedQuery = parameterizedUri.getQuery() != null;
        StringBuilder uriRegex = new StringBuilder("^");

        if (!SCHEME_PATTERN.matcher(uri).find()) {
            uriRegex.append("http[s]?://");
        }
        Pattern fillInPattern = Pattern.compile("\\{(.+?)\\}");
        if (mIsParameterizedQuery) {
            Matcher matcher = Pattern.compile("(\\?)").matcher(uri);
            if (matcher.find()) {
                buildPathRegex(uri.substring(0, matcher.start()), uriRegex, fillInPattern);
                // Match either the end of string if all params are optional or match the
                // question mark and 0 or more characters after it
                // We do not use '.*' here because the finalregex would replace it with a quoted
                // version below.
                uriRegex.append("($|(\\?(.)*))");
            }
            mExactDeepLink = false;
            for (String paramName : parameterizedUri.getQueryParameterNames()) {
                StringBuilder argRegex = new StringBuilder();
                String queryParam = parameterizedUri.getQueryParameter(paramName);
                matcher = fillInPattern.matcher(queryParam);
                int appendPos = 0;
                ParamQuery param = new ParamQuery();
                // Build the regex for each query param
                while (matcher.find()) {
                    param.addArgumentName(matcher.group(1));
                    argRegex.append(Pattern.quote(queryParam.substring(appendPos,
                            matcher.start())));
                    argRegex.append("(.+?)?");
                    appendPos = matcher.end();
                }
                if (appendPos < queryParam.length()) {
                    argRegex.append(Pattern.quote(queryParam.substring(appendPos)));
                }
                // Save the regex with wildcards unquoted, and add the param to the map with its
                // name as the key
                param.setParamRegex(argRegex.toString().replace(".*", "\\E.*\\Q"));
                mParamArgMap.put(paramName, param);
            }
        } else {
            mExactDeepLink = buildPathRegex(uri, uriRegex, fillInPattern);
        }
        // Since we've used Pattern.quote() above, we need to
        // specifically escape any .* instances to ensure
        // they are still treated as wildcards in our final regex
        String finalRegex = uriRegex.toString().replace(".*", "\\E.*\\Q");
        mPattern = Pattern.compile(finalRegex);
    }

    private boolean buildPathRegex(@NonNull String uri, StringBuilder uriRegex,
            Pattern fillInPattern) {
        Matcher matcher = fillInPattern.matcher(uri);
        int appendPos = 0;
        // Track whether this is an exact deep link
        boolean exactDeepLink = !uri.contains(".*");
        while (matcher.find()) {
            String argName = matcher.group(1);
            mArguments.add(argName);
            // Use Pattern.quote() to treat the input string as a literal
            uriRegex.append(Pattern.quote(uri.substring(appendPos, matcher.start())));
            uriRegex.append("(.+?)");
            appendPos = matcher.end();
            exactDeepLink = false;
        }
        if (appendPos < uri.length()) {
            // Use Pattern.quote() to treat the input string as a literal
            uriRegex.append(Pattern.quote(uri.substring(appendPos)));
        }
        return exactDeepLink;
    }

    boolean matches(@NonNull Uri deepLink) {
        return mPattern.matcher(deepLink.toString()).matches();
    }

    boolean isExactDeepLink() {
        return mExactDeepLink;
    }

    @Nullable
    Bundle getMatchingArguments(@NonNull Uri deepLink,
            @NonNull Map<String, NavArgument> arguments) {
        Matcher matcher = mPattern.matcher(deepLink.toString());
        if (!matcher.matches()) {
            return null;
        }
        Bundle bundle = new Bundle();
        int size = mArguments.size();
        for (int index = 0; index < size; index++) {
            String argumentName = mArguments.get(index);
            String value = Uri.decode(matcher.group(index + 1));
            NavArgument argument = arguments.get(argumentName);
            if (parseArgument(bundle, argumentName, value, argument)) {
                return null;
            }
        }
        if (mIsParameterizedQuery) {
            for (String paramName : mParamArgMap.keySet()) {
                Matcher argMatcher = null;
                ParamQuery storedParam = mParamArgMap.get(paramName);
                String inputParams = deepLink.getQueryParameter(paramName);
                if (inputParams != null) {
                    // Match the input arguments with the saved regex
                    argMatcher = Pattern.compile(storedParam.getParamRegex()).matcher(inputParams);
                    if (!argMatcher.matches()) {
                        return null;
                    }
                }
                // Params could have multiple arguments, we need to handle them all
                for (int index = 0; index < storedParam.size(); index++) {
                    String value = null;
                    if (argMatcher != null) {
                        value = Uri.decode(argMatcher.group(index + 1));
                    }
                    String argName = storedParam.getArgumentName(index);
                    NavArgument argument = arguments.get(argName);
                    // Missing parameter so see if it has a default value or is Nullable
                    if (argument != null
                            && (value == null || value.replaceAll("[{}]", "").equals(argName))) {
                        if (argument.getDefaultValue() != null) {
                            value = argument.getDefaultValue().toString();
                        } else if (argument.isNullable()) {
                            value = null;
                        }
                    }
                    if (parseArgument(bundle, argName, value, argument)) {
                        return null;
                    }
                }
            }
        }
        return bundle;
    }

    private boolean parseArgument(Bundle bundle, String name, String value, NavArgument argument) {
        if (argument != null) {
            NavType<?> type = argument.getType();
            try {
                type.parseAndPut(bundle, name, value);
            } catch (IllegalArgumentException e) {
                // Failed to parse means this isn't a valid deep link
                // for the given URI - i.e., the URI contains a non-integer
                // value for an integer argument
                return true;
            }
        } else {
            bundle.putString(name, value);
        }
        return false;
    }

    /**
     * Used to maintain query parameters and the mArguments they match with.
     */
    private static class ParamQuery {
        private String mParamRegex;
        private ArrayList<String> mArguments;

        ParamQuery() {
            mArguments = new ArrayList<>();
        }

        void setParamRegex(String paramRegex) {
            this.mParamRegex = paramRegex;
        }

        String getParamRegex() {
            return mParamRegex;
        }

        void addArgumentName(String name) {
            mArguments.add(name);
        }

        String getArgumentName(int index) {
            return mArguments.get(index);
        }

        public int size() {
            return mArguments.size();
        }
    }
}
