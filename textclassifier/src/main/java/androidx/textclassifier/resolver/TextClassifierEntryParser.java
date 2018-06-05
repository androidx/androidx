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

package androidx.textclassifier.resolver;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.XmlRes;
import androidx.core.util.Preconditions;
import androidx.textclassifier.R;
import androidx.textclassifier.TextClassifier;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a xml resource file that specifies the preference of text classifiers.
 * <p>
 * Expects a file like this:
 * <pre class="prettyprint">
 * &lt;text-classifiers xmlns:app="http://schemas.android.com/apk/res-auto"/&gt;
 *    &lt;text-classifier app:packageName="first.package" app:signature="first.cert"/&gt;
 *    &lt;text-classifier app:packageName="second.package" app:signature="second.cert"/&gt;
 *    &lt;text-classifier /&gt;
 * &lt;/text-classifiers&gt;
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TextClassifierEntryParser {
    private static final String TAG = TextClassifier.DEFAULT_LOG_TAG;
    private static final String TEXT_CLASSIFIERS_TAG = "text-classifiers";
    private static final String TEXT_CLASSIFIER_TAG = "text-classifier";
    private static final String NAMESPACE = null;

    @NonNull
    private Context mContext;

    public TextClassifierEntryParser(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    /**
     * Parses the given xml file and returns a list of entries parsed. It skips invalid entries.
     */
    @NonNull
    public List<TextClassifierEntry> parse(@XmlRes int xmlRes)
            throws XmlPullParserException, IOException {
        XmlResourceParser parser = mContext.getResources().getXml(xmlRes);
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop.
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TEXT_CLASSIFIERS_TAG);
        List<TextClassifierEntry> entries = new ArrayList<>();
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            if (TEXT_CLASSIFIER_TAG.equals(tag)) {
                TextClassifierEntry entry = parseTextClassifier(parser);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    @Nullable
    private TextClassifierEntry parseTextClassifier(
            @NonNull XmlResourceParser xmlResourceParser) {
        AttributeSet attrs = Xml.asAttributeSet(xmlResourceParser);
        TypedArray array = mContext.getResources().obtainAttributes(
                attrs, R.styleable.TextClassifier);
        String packageName = array.getString(R.styleable.TextClassifier_packageName);
        String certificate = array.getString(R.styleable.TextClassifier_certificate);
        if (TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "parseTextClassifier: package name is missing, skip it");
            return null;
        }
        if (TextClassifierEntry.AOSP.equals(packageName)) {
            return TextClassifierEntry.createAospEntry();
        }
        if (TextClassifierEntry.OEM.equals(packageName)) {
            return TextClassifierEntry.createOemEntry();
        }
        if (TextUtils.isEmpty(certificate)) {
            Log.w(TAG, "parseTextClassifier: certificate is missing, skip it");
            return null;
        }
        return TextClassifierEntry.createPackageEntry(packageName, certificate);
    }
}
