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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SmallTest
public class TextClassificationManagerTest {
    private static final String PACKAGE_NAME = "my.package";

    private TextClassificationManager mTextClassificationManager;
    private Object mPlatformTextClassificationMgr;
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mContext.getApplicationContext()).thenReturn(mContext);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mPlatformTextClassificationMgr =
                    ApplicationProvider.getApplicationContext().getSystemService(
                            android.view.textclassifier.TextClassificationManager.class);
            when(mContext.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE))
                    .thenReturn(mPlatformTextClassificationMgr);
        }
        mTextClassificationManager = new TextClassificationManager(mContext);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public void testGetTextClassifier_default_postO() throws Exception {
        TextClassifier textClassifier = mTextClassificationManager.getTextClassifier();

        assertThat(textClassifier).isInstanceOf(PlatformTextClassifierWrapper.class);
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    public void testGetTextClassifier_default_preO() throws Exception {
        TextClassifier textClassifier = mTextClassificationManager.getTextClassifier();

        assertThat(textClassifier).isInstanceOf(LegacyTextClassifier.class);
    }

    @Test
    public void testGetTextClassifier_custom() throws Exception {
        DummyTextClassifier dummyTextClassifier = new DummyTextClassifier();
        mTextClassificationManager.setTextClassifier(dummyTextClassifier);
        TextClassifier textClassifier = mTextClassificationManager.getTextClassifier();

        assertThat(textClassifier).isEqualTo(dummyTextClassifier);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public void testGetTextClassifier_custom_postO_platformTextClassifierIsSet() throws Exception {
        Assume.assumeNotNull(mPlatformTextClassificationMgr);

        mTextClassificationManager.setTextClassifier(new DummyTextClassifier());

        assertThat(
                ((android.view.textclassifier.TextClassificationManager)
                        mPlatformTextClassificationMgr).getTextClassifier())
                .isInstanceOf(PlatformTextClassifier.class);
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    public void testGetDefaultTextClassifier_preO() {
        TextClassifier textClassifier = mTextClassificationManager.getDefaultTextClassifier();

        assertThat(textClassifier).isInstanceOf(LegacyTextClassifier.class);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public void testGetDefaultTextClassifier_postO() {
        TextClassifier textClassifier = mTextClassificationManager.getDefaultTextClassifier();

        assertThat(textClassifier).isInstanceOf(PlatformTextClassifierWrapper.class);
    }

    private static class DummyTextClassifier extends TextClassifier {
        DummyTextClassifier() {
            super();
        }
    }
}
