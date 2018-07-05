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

import static androidx.textclassifier.TextClassificationManager.METADATA_XML_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;

import androidx.textclassifier.resolver.TextClassifierEntry;
import androidx.textclassifier.resolver.TextClassifierEntryParser;
import androidx.textclassifier.resolver.TextClassifierResolver;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

@SmallTest
public class TextClassificationManagerTest {
    private static final String PACKAGE_NAME = "my.package";

    private TextClassificationManager mTextClassificationManager;
    private TextClassificationContext mTextClassificationContext;
    @Mock
    private TextClassifierResolver mTextClassifierResolver;
    @Mock
    private TextClassifierEntryParser mTextClassifierEntryParser;
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTextClassificationManager = new TextClassificationManager(
                mContext, mTextClassifierEntryParser, mTextClassifierResolver);
        mTextClassificationContext = new TextClassificationContext.Builder(
                PACKAGE_NAME, TextClassifier.WIDGET_TYPE_TEXTVIEW).build();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mContext.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE)).thenReturn(
                InstrumentationRegistry.getTargetContext().getSystemService(
                        Context.TEXT_CLASSIFICATION_SERVICE)
        );
    }

    @Test
    public void testCreateTextClassifier_noMetaData() throws Exception {
        // Construct application info without metadata.
        ApplicationInfo applicationInfo = new ApplicationInfo();
        when(mPackageManager.getApplicationInfo(
                eq(PACKAGE_NAME), anyInt())).thenReturn(applicationInfo);

        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);

        // fallback should be used.
        assertThat(textClassifier).isNotNull();
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testCreateTextClassifier_platformTextClassifier() throws Exception {
        List<TextClassifierEntry> candidates =
                Collections.singletonList(TextClassifierEntry.createAospEntry());
        TextClassifierEntry bestMatch = candidates.get(0);
        setupEnvironment(candidates, bestMatch);

        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);

        assertThat(textClassifier).isInstanceOf(PlatformTextClassifierWrapper.class);
    }

    @Test
    public void testCreateTextClassifier_remoteServiceTextClassifier() throws Exception {
        List<TextClassifierEntry> candidates =
                Collections.singletonList(TextClassifierEntry.createPackageEntry("pkg", "cert"));
        TextClassifierEntry bestMatch = candidates.get(0);
        setupEnvironment(candidates, bestMatch);


        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);

        assertThat(textClassifier).isInstanceOf(RemoteServiceTextClassifier.class);
    }

    @Test
    public void testCreateTextClassifier_parserReturnNull() throws Exception {
        setupEnvironment(null, null);

        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);

        // fallback should be used.
        assertThat(textClassifier).isNotNull();
    }

    @Test
    public void testCreateTextClassifier_resolverReturnNull() throws Exception {
        setupEnvironment(
                Collections.singletonList(TextClassifierEntry.createAospEntry()), null);

        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);

        // fallback should be used.
        assertThat(textClassifier).isNotNull();
    }

    @Test
    public void testCreateTextClassifier() {
        mTextClassificationManager.setTextClassifierFactory(
                new TextClassificationManager.TextClassifierFactory() {
                    @Override
                    public TextClassifier create(TextClassificationContext
                            textClassificationContext) {
                        return new DummyTextClassifier(textClassificationContext);
                    }
                });
        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);
        assertThat(textClassifier).isInstanceOf(DummyTextClassifier.class);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void testFallback_P() throws Exception {
        setupEnvironment(Collections.<TextClassifierEntry>emptyList(), null);
        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);
        assertThat(textClassifier).isInstanceOf(PlatformTextClassifierWrapper.class);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    public void testFallback_beforeP() throws Exception {
        setupEnvironment(Collections.<TextClassifierEntry>emptyList(), null);
        TextClassifier textClassifier =
                mTextClassificationManager.createTextClassifier(mTextClassificationContext);
        assertThat(textClassifier).isInstanceOf(LegacyTextClassifier.class);
    }

    private void setupEnvironment(
            List<TextClassifierEntry> candidates, TextClassifierEntry bestMatch)
            throws Exception {
        final int xmlRes = 10;
        Bundle metadata = new Bundle();
        metadata.putInt(METADATA_XML_KEY, xmlRes);
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.metaData = metadata;
        when(mPackageManager.getApplicationInfo(
                eq(PACKAGE_NAME), anyInt())).thenReturn(applicationInfo);
        when(mTextClassifierEntryParser.parse(xmlRes)).thenReturn(candidates);
        when(mTextClassifierResolver.findBestMatch(candidates)).thenReturn(bestMatch);
    }

    private static class DummyTextClassifier extends TextClassifier {
        DummyTextClassifier(TextClassificationContext textClassificationContext) {
            super(textClassificationContext);
        }
    }
}
