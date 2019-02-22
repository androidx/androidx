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

    /**
     * NavDestinations should be created via {@link Navigator#createDestination}.
     */
    NavDeepLink(@NonNull String uri) {
        StringBuilder uriRegex = new StringBuilder("^");

        if (!SCHEME_PATTERN.matcher(uri).find()) {
            uriRegex.append("http[s]?://");
        }
        Pattern fillInPattern = Pattern.compile("\\{(.+?)\\}");
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
        // Since we've used Pattern.quote() above, we need to
        // specifically escape any .* instances to ensure
        // they are still treated as wildcards in our final regex
        String finalRegex = uriRegex.toString().replace(".*", "\\E.*\\Q");
        mPattern = Pattern.compile(finalRegex);
        mExactDeepLink = exactDeepLink;
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
            if (argument != null) {
                NavType type = argument.getType();
                try {
                    type.parseAndPut(bundle, argumentName, value);
                } catch (IllegalArgumentException e) {
                    // Failed to parse means this isn't a valid deep link
                    // for the given URI - i.e., the URI contains a non-integer
                    // value for an integer argument
                    return null;
                }
            } else {
                bundle.putString(argumentName, value);
            }
        }
        return bundle;
    }
}
