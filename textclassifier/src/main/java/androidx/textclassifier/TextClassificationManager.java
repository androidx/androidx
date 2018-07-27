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

package androidx.textclassifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.core.util.Preconditions;
import androidx.textclassifier.resolver.TextClassifierEntry;
import androidx.textclassifier.resolver.TextClassifierEntryParser;
import androidx.textclassifier.resolver.TextClassifierResolver;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Class to handle the creation of {@link TextClassifier}.
 * <p>
 * The choice of text classifier can be specified in the AndroidManifest.xml like this:
 * <pre class="prettyprint">
 * &lt;application&gt;
 *    &lt;meta-data android:name="androidx.textclassifier"
 *          android:resource="@xml/text_classifiers"&gt;
 * </pre>
 *
 * And the text_classifiers.xml is like this:
 * <pre class="prettyprint">
 * &lt;text-classifiers xmlns:app="http://schemas.android.com/apk/res-auto"/&gt;
 *    &lt;-- Text classifier implementation provided by OEM --/&gt;
 *    &lt;text-classifier app:packageName="oem"/&gt;
 *    &lt;-- Package that provides TextClassifierService, certificate should be the base64
 *    encoded sha256 hash of the apps's signing certificate --/&gt;
 *    &lt;text-classifier app:packageName="packageName" app:certificate="certificate"/&gt;
 *    &lt;-- Text classifier implementation provided by AOSP --/&gt;
 *    &lt;text-classifier app:packageName="aosp"/&gt;
 * &lt;/text-classifiers&gt;
 * </pre>
 */
public final class TextClassificationManager {
    private static final String TAG = TextClassifier.DEFAULT_LOG_TAG;
    private static final int NO_RES = -1;
    @VisibleForTesting
    static final String METADATA_XML_KEY = "androidx.textclassifier";

    private final Context mContext;
    private final TextClassifierEntryParser mTextClassifierEntryParser;
    private final TextClassifierResolver mTextClassifierResolver;
    @Nullable
    private List<TextClassifierEntry> mTextClassifierCandidates;
    @Nullable
    private static TextClassificationManager sInstance;
    private TextClassifierFactory mTextClassifierFactory;

    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    TextClassificationManager(
            @NonNull Context context,
            @NonNull TextClassifierEntryParser textClassifierEntryParser,
            @NonNull TextClassifierResolver textClassifierResolver) {
        mContext = Preconditions.checkNotNull(context);
        mTextClassifierEntryParser = Preconditions.checkNotNull(textClassifierEntryParser);
        mTextClassifierResolver = Preconditions.checkNotNull(textClassifierResolver);
    }

    /**
     * Return an instance of {@link TextClassificationManager}.
     */
    public static TextClassificationManager of(@NonNull Context context) {
        if (sInstance == null) {
            Context appContext = context.getApplicationContext();
            sInstance = new TextClassificationManager(
                    appContext,
                    new TextClassifierEntryParser(appContext),
                    new TextClassifierResolver(appContext));
        }
        return sInstance;
    }

    /**
     * Returns a newly created text classifier, which is session based. That means the text
     * classifier can't be reused once it is destroyed.
     */
    @SuppressLint("NewApi") // isAosp and isOem already tell us that we have high enough API level.
    @NonNull
    public TextClassifier createTextClassifier(
            @NonNull TextClassificationContext textClassificationContext) {
        Preconditions.checkNotNull(textClassificationContext);
        if (mTextClassifierFactory != null) {
            return mTextClassifierFactory.create(textClassificationContext);
        }
        if (mTextClassifierCandidates == null) {
            mTextClassifierCandidates = parseTextClassifierCandidates();
        }
        TextClassifierEntry bestMatch =
                mTextClassifierResolver.findBestMatch(mTextClassifierCandidates);
        if (bestMatch == null) {
            return defaultTextClassifier(textClassificationContext);
        }
        if (bestMatch.isAosp() || bestMatch.isOem()) {
            return PlatformTextClassifierWrapper.create(mContext, textClassificationContext);
        }
        return new RemoteServiceTextClassifier(
                mContext, textClassificationContext, bestMatch.packageName);
    }

    /**
     * Sets a factory that can create a preferred text classifier. This overrides the preferred
     * text classifiers specified in AndroidManifest.
     * <p>
     * To turn off the feature completely, you can set a factory that returns
     * {@link TextClassifier#NO_OP}.
     */
    public void setTextClassifierFactory(@Nullable TextClassifierFactory factory) {
        mTextClassifierFactory = factory;
    }

    @XmlRes
    private int getTextClassifiersXmlRes() {
        Bundle metaData = null;
        try {
            PackageManager packageManager = mContext.getPackageManager();
            metaData = packageManager.getApplicationInfo(
                    mContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            // can't happen
        }
        if (metaData == null) {
            return NO_RES;
        }
        return metaData.getInt(METADATA_XML_KEY, NO_RES);
    }

    @NonNull
    private List<TextClassifierEntry> parseTextClassifierCandidates() {
        int xmlRes = getTextClassifiersXmlRes();
        try {
            if (xmlRes != NO_RES) {
                return mTextClassifierEntryParser.parse(xmlRes);
            }
        } catch (XmlPullParserException | IOException ex) {
            Log.e(TAG, "parseTextClassifierCandidates: ", ex);
        }
        return Collections.emptyList();
    }

    /**
     * Returns the default text classifier when we need a fallback.
     */
    private TextClassifier defaultTextClassifier(
            @Nullable TextClassificationContext textClassificationContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return PlatformTextClassifierWrapper.create(mContext, textClassificationContext);
        }
        return LegacyTextClassifier.INSTANCE;
    }
}
