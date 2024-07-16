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

package androidx.core.content.res;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.TypedValue;
import android.util.Xml;

import androidx.annotation.ArrayRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.R;
import androidx.core.provider.FontRequest;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for xml type font resources.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class FontResourcesParserCompat {
    private static final int NORMAL_WEIGHT = 400;
    private static final int ITALIC = 1;

    @IntDef({FETCH_STRATEGY_BLOCKING, FETCH_STRATEGY_ASYNC})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FetchStrategy {}

    public static final int FETCH_STRATEGY_BLOCKING = 0;
    public static final int FETCH_STRATEGY_ASYNC = 1;

    // A special timeout value for infinite blocking.
    public static final int INFINITE_TIMEOUT_VALUE = -1;

    private static final int DEFAULT_TIMEOUT_MILLIS = 500;

    /**
     * A class that represents a single entry of font-family in an xml file.
     */
    public interface FamilyResourceEntry {}

    /**
     * A class that represents a font provider based font-family element in an xml file.
     */
    public static final class ProviderResourceEntry implements FamilyResourceEntry {
        private final @NonNull FontRequest mRequest;
        private final @Nullable FontRequest mFallbackRequest;
        private final int mTimeoutMs;
        private final @FetchStrategy int mStrategy;
        private final @Nullable String mSystemFontFamilyName;

        @RestrictTo(LIBRARY)
        public ProviderResourceEntry(@NonNull FontRequest request,
                @Nullable FontRequest fallbackRequest, @FetchStrategy int strategy,
                int timeoutMs, @Nullable String systemFontFamilyName) {
            mRequest = request;
            mFallbackRequest = fallbackRequest;
            mStrategy = strategy;
            mTimeoutMs = timeoutMs;
            mSystemFontFamilyName = systemFontFamilyName;
        }

        public ProviderResourceEntry(@NonNull FontRequest request, @FetchStrategy int strategy,
                int timeoutMs) {
            this(request, null, strategy, timeoutMs, null /*systemFontFamilyName*/);
        }

        public @NonNull FontRequest getRequest() {
            return mRequest;
        }

        @Nullable
        public FontRequest getFallbackRequest() {
            return mFallbackRequest;
        }

        public @FetchStrategy int getFetchStrategy() {
            return mStrategy;
        }

        public int getTimeout() {
            return mTimeoutMs;
        }

        @RestrictTo(LIBRARY)
        public @Nullable String getSystemFontFamilyName() {
            return mSystemFontFamilyName;
        }
    }

    /**
     * A class that represents a font element in an xml file which points to a file in resources.
     */
    public static final class FontFileResourceEntry {
        private final @NonNull String mFileName;
        private final int mWeight;
        private final boolean mItalic;
        private final String mVariationSettings;
        private final int mTtcIndex;
        private final int mResourceId;

        public FontFileResourceEntry(@NonNull String fileName, int weight, boolean italic,
                @Nullable String variationSettings, int ttcIndex, int resourceId) {
            mFileName = fileName;
            mWeight = weight;
            mItalic = italic;
            mVariationSettings = variationSettings;
            mTtcIndex = ttcIndex;
            mResourceId = resourceId;
        }

        public @NonNull String getFileName() {
            return mFileName;
        }

        public int getWeight() {
            return mWeight;
        }

        public boolean isItalic() {
            return mItalic;
        }

        public @Nullable String getVariationSettings() {
            return mVariationSettings;
        }

        public int getTtcIndex() {
            return mTtcIndex;
        }

        public int getResourceId() {
            return mResourceId;
        }
    }

    /**
     * A class that represents a file based font-family element in an xml font file.
     */
    public static final class FontFamilyFilesResourceEntry implements FamilyResourceEntry {
        private final @NonNull FontFileResourceEntry[] mEntries;

        public FontFamilyFilesResourceEntry(@NonNull FontFileResourceEntry[] entries) {
            mEntries = entries;
        }

        public @NonNull FontFileResourceEntry[] getEntries() {
            return mEntries;
        }
    }

    /**
     * Parse an XML font resource. The result type will depend on the contents of the xml.
     */
    public static @Nullable FamilyResourceEntry parse(@NonNull XmlPullParser parser,
            @NonNull Resources resources) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop.
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        return readFamilies(parser, resources);
    }

    private static @Nullable FamilyResourceEntry readFamilies(XmlPullParser parser,
            Resources resources) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "font-family");
        String tag = parser.getName();
        if (tag.equals("font-family")) {
            return readFamily(parser, resources);
        } else {
            skip(parser);
            return null;
        }
    }

    private static @Nullable FamilyResourceEntry readFamily(XmlPullParser parser,
            Resources resources) throws XmlPullParserException, IOException {
        AttributeSet attrs = Xml.asAttributeSet(parser);
        TypedArray array = resources.obtainAttributes(attrs, R.styleable.FontFamily);
        String authority = array.getString(R.styleable.FontFamily_fontProviderAuthority);
        String providerPackage = array.getString(R.styleable.FontFamily_fontProviderPackage);
        String query = array.getString(R.styleable.FontFamily_fontProviderQuery);
        String fallbackQuery = array.getString(R.styleable.FontFamily_fontProviderFallbackQuery);
        int certsId = array.getResourceId(R.styleable.FontFamily_fontProviderCerts, 0);
        int strategy = array.getInteger(R.styleable.FontFamily_fontProviderFetchStrategy,
                FETCH_STRATEGY_ASYNC);
        int timeoutMs = array.getInteger(R.styleable.FontFamily_fontProviderFetchTimeout,
                DEFAULT_TIMEOUT_MILLIS);
        String systemFontFamilyName = array
                .getString(R.styleable.FontFamily_fontProviderSystemFontFamily);

        array.recycle();
        if (authority != null && providerPackage != null && query != null) {
            while (parser.next() != XmlPullParser.END_TAG) {
                skip(parser);
            }
            List<List<byte[]>> certs = readCerts(resources, certsId);
            FontRequest fallbackRequest;
            if (fallbackQuery != null) {
                fallbackRequest = new FontRequest(authority, providerPackage, fallbackQuery, certs);
            } else {
                fallbackRequest = null;
            }
            return new ProviderResourceEntry(
                    new FontRequest(authority, providerPackage, query, certs),
                    fallbackRequest,
                    strategy,
                    timeoutMs,
                    systemFontFamilyName
            );
        }
        List<FontFileResourceEntry> fonts = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String tag = parser.getName();
            if (tag.equals("font")) {
                fonts.add(readFont(parser, resources));
            } else {
                skip(parser);
            }
        }
        if (fonts.isEmpty()) {
            return null;
        }
        // Passing a zero-sized array is faster, safer, and more likely to be correct.
        return new FontFamilyFilesResourceEntry(fonts.toArray(
                new FontFileResourceEntry[0]));
    }

    @SuppressWarnings("SameParameterValue")
    private static int getType(TypedArray typedArray, int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Api21Impl.getType(typedArray, index);
        } else {
            TypedValue tv = new TypedValue();
            typedArray.getValue(index, tv);
            return tv.type;
        }
    }

    /**
     * Creates the necessary cert structure given a resources array. This method is capable of
     * loading one string array as well as an array of string arrays.
     *
     * Provider cert entry must be cert string array or array of cert string array.
     */
    @NonNull
    @SuppressWarnings("MixedMutabilityReturnType")
    public static List<List<byte[]>> readCerts(@NonNull Resources resources,
            @ArrayRes int certsId) {
        if (certsId == 0) {
            return Collections.emptyList();
        }
        final TypedArray typedArray = resources.obtainTypedArray(certsId);
        try {
            if (typedArray.length() == 0) {
                return Collections.emptyList();
            }

            final List<List<byte[]>> result = new ArrayList<>();
            // We support array of string or array of string-array.
            // Check the first item and if it is reference type, regard as array of string-array.
            if (getType(typedArray, 0) == TypedValue.TYPE_REFERENCE) {
                for (int i = 0; i < typedArray.length(); i++) {
                    final int certId = typedArray.getResourceId(i, 0);
                    if (certId != 0) {
                        result.add(toByteArrayList(resources.getStringArray(certId)));
                    }
                }
            } else {  // string array
                result.add(toByteArrayList(resources.getStringArray(certsId)));
            }
            return result;
        } finally {
            typedArray.recycle();
        }

    }

    private static List<byte[]> toByteArrayList(String[] stringArray) {
        List<byte[]> result = new ArrayList<>();
        for (String item : stringArray) {
            result.add(Base64.decode(item, Base64.DEFAULT));
        }
        return result;
    }

    private static FontFileResourceEntry readFont(XmlPullParser parser, Resources resources)
            throws XmlPullParserException, IOException {
        AttributeSet attrs = Xml.asAttributeSet(parser);
        TypedArray array = resources.obtainAttributes(attrs, R.styleable.FontFamilyFont);
        final int weightAttr = array.hasValue(R.styleable.FontFamilyFont_fontWeight)
                ? R.styleable.FontFamilyFont_fontWeight
                : R.styleable.FontFamilyFont_android_fontWeight;
        int weight = array.getInt(weightAttr, NORMAL_WEIGHT);
        final int styleAttr = array.hasValue(R.styleable.FontFamilyFont_fontStyle)
                ? R.styleable.FontFamilyFont_fontStyle
                : R.styleable.FontFamilyFont_android_fontStyle;
        boolean isItalic = ITALIC == array.getInt(styleAttr, 0);
        final int ttcIndexAttr = array.hasValue(R.styleable.FontFamilyFont_ttcIndex)
                ? R.styleable.FontFamilyFont_ttcIndex
                : R.styleable.FontFamilyFont_android_ttcIndex;
        final int variationSettingsAttr =
                array.hasValue(R.styleable.FontFamilyFont_fontVariationSettings)
                        ? R.styleable.FontFamilyFont_fontVariationSettings
                        : R.styleable.FontFamilyFont_android_fontVariationSettings;
        String variationSettings = array.getString(variationSettingsAttr);
        int ttcIndex = array.getInt(ttcIndexAttr, 0);
        final int resourceAttr = array.hasValue(R.styleable.FontFamilyFont_font)
                ? R.styleable.FontFamilyFont_font
                : R.styleable.FontFamilyFont_android_font;
        int resourceId = array.getResourceId(resourceAttr, 0);
        String filename = array.getString(resourceAttr);
        array.recycle();
        while (parser.next() != XmlPullParser.END_TAG) {
            skip(parser);
        }
        return new FontFileResourceEntry(filename, weight, isItalic, variationSettings, ttcIndex,
                resourceId);
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
            }
        }
    }

    private FontResourcesParserCompat() {
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        static int getType(TypedArray typedArray, int index) {
            return typedArray.getType(index);
        }
    }
}
