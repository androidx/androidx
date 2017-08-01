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

package android.support.v4.content.res;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.ArrayRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.compat.R;
import android.support.v4.provider.FontRequest;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Xml;

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
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
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
        private final int mTimeoutMs;
        private final @FetchStrategy int mStrategy;

        public ProviderResourceEntry(@NonNull FontRequest request, @FetchStrategy int strategy,
                int timeoutMs) {
            mRequest = request;
            mStrategy = strategy;
            mTimeoutMs = timeoutMs;
        }

        public @NonNull FontRequest getRequest() {
            return mRequest;
        }

        public @FetchStrategy int getFetchStrategy() {
            return mStrategy;
        }

        public int getTimeout() {
            return mTimeoutMs;
        }
    }

    /**
     * A class that represents a font element in an xml file which points to a file in resources.
     */
    public static final class FontFileResourceEntry {
        private final @NonNull String mFileName;
        private int mWeight;
        private boolean mItalic;
        private int mResourceId;

        public FontFileResourceEntry(@NonNull String fileName, int weight, boolean italic,
                int resourceId) {
            mFileName = fileName;
            mWeight = weight;
            mItalic = italic;
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
    public static @Nullable FamilyResourceEntry parse(XmlPullParser parser, Resources resources)
            throws XmlPullParserException, IOException {
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
        int certsId = array.getResourceId(R.styleable.FontFamily_fontProviderCerts, 0);
        int strategy = array.getInteger(R.styleable.FontFamily_fontProviderFetchStrategy,
                FETCH_STRATEGY_ASYNC);
        int timeoutMs = array.getInteger(R.styleable.FontFamily_fontProviderFetchTimeout,
                DEFAULT_TIMEOUT_MILLIS);
        array.recycle();
        if (authority != null && providerPackage != null && query != null) {
            while (parser.next() != XmlPullParser.END_TAG) {
                skip(parser);
            }
            List<List<byte[]>> certs = readCerts(resources, certsId);
            return new ProviderResourceEntry(
                    new FontRequest(authority, providerPackage, query, certs), strategy, timeoutMs);
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
        return new FontFamilyFilesResourceEntry(fonts.toArray(
                new FontFileResourceEntry[fonts.size()]));
    }

    /**
     * Creates the necessary cert structure given a resources array. This method is capable of
     * loading one string array as well as an array of string arrays.
     */
    public static List<List<byte[]>> readCerts(Resources resources, @ArrayRes int certsId) {
        List<List<byte[]>> certs = null;
        if (certsId != 0) {
            TypedArray typedArray = resources.obtainTypedArray(certsId);
            if (typedArray.length() > 0) {
                certs = new ArrayList<>();
                boolean isArrayOfArrays = typedArray.getResourceId(0, 0) != 0;
                if (isArrayOfArrays) {
                    for (int i = 0; i < typedArray.length(); i++) {
                        int certId = typedArray.getResourceId(i, 0);
                        String[] certsArray = resources.getStringArray(certId);
                        List<byte[]> certsList = toByteArrayList(certsArray);
                        certs.add(certsList);
                    }
                } else {
                    String[] certsArray = resources.getStringArray(certsId);
                    List<byte[]> certsList = toByteArrayList(certsArray);
                    certs.add(certsList);
                }
            }
            typedArray.recycle();
        }
        return certs != null ?  certs : Collections.<List<byte[]>>emptyList();
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
        int weight = array.getInt(R.styleable.FontFamilyFont_fontWeight, NORMAL_WEIGHT);
        boolean isItalic = ITALIC == array.getInt(R.styleable.FontFamilyFont_fontStyle, 0);
        int resourceId = array.getResourceId(R.styleable.FontFamilyFont_font, 0);
        String filename = array.getString(R.styleable.FontFamilyFont_font);
        array.recycle();
        while (parser.next() != XmlPullParser.END_TAG) {
            skip(parser);
        }
        return new FontFileResourceEntry(filename, weight, isItalic, resourceId);
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
}
