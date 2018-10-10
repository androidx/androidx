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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NavDeepLink encapsulates the parsing and matching of a navigation deep link.
 */
class NavDeepLink {
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z]+[+\\w\\-.]*:");

    private final ArrayList<String> mArguments = new ArrayList<>();
    private final Pattern mPattern;

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
        while (matcher.find()) {
            String argName = matcher.group(1);
            mArguments.add(argName);
            // Use Pattern.quote() to treat the input string as a literal
            uriRegex.append(Pattern.quote(uri.substring(appendPos, matcher.start())));
            uriRegex.append("(.+?)");
            appendPos = matcher.end();
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
    }

    boolean matches(@NonNull Uri deepLink) {
        return mPattern.matcher(deepLink.toString()).matches();
    }

    @Nullable
    Bundle getMatchingArguments(@NonNull Uri deepLink) {
        Matcher matcher = mPattern.matcher(deepLink.toString());
        if (!matcher.matches()) {
            return null;
        }
        Bundle bundle = new Bundle();
        int size = mArguments.size();
        for (int index = 0; index < size; index++) {
            String argument = mArguments.get(index);
            bundle.putString(argument, Uri.decode(matcher.group(index + 1)));
        }
        return bundle;
    }
}
