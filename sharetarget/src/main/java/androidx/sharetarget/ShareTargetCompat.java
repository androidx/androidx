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

package androidx.sharetarget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

/**
 * Represents a Share Target definition read from the app's manifest.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
class ShareTargetCompat {
    static class TargetData {
        final String mScheme;
        final String mHost;
        final String mPort;
        final String mPath;
        final String mPathPattern;
        final String mPathPrefix;
        final String mMimeType;

        TargetData(String scheme, String host, String port, String path, String pathPattern,
                String pathPrefix, String mimeType) {
            mScheme = scheme;
            mHost = host;
            mPort = port;
            mPath = path;
            mPathPattern = pathPattern;
            mPathPrefix = pathPrefix;
            mMimeType = mimeType;
        }
    }

    final TargetData[] mTargetData;
    final String mTargetClass;
    final String[] mCategories;

    ShareTargetCompat(TargetData[] data, String targetClass, String[] categories) {
        mTargetData = data;
        mTargetClass = targetClass;
        mCategories = categories;
    }
}
