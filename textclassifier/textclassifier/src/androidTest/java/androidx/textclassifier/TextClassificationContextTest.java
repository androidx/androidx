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

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumentation unit tests for {@link TextClassificationContext}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
public class TextClassificationContextTest {
    private static final String PKG_NAME = "pkg_name";
    private static final String WIDGET_TYPE = TextClassifier.WIDGET_TYPE_TEXTVIEW;
    private static final String WIDGET_VERSION = "version";

    @Test
    public void testToBundle() {
        TextClassificationContext textClassificationContext =
                new TextClassificationContext.Builder(PKG_NAME, WIDGET_TYPE)
                        .setWidgetVersion(WIDGET_VERSION)
                        .build();
        Bundle bundle = textClassificationContext.toBundle();
        TextClassificationContext restored = TextClassificationContext.createFromBundle(bundle);
        assertThat(restored.getPackageName()).isEqualTo(PKG_NAME);
        assertThat(restored.getWidgetType()).isEqualTo(WIDGET_TYPE);
        assertThat(restored.getWidgetVersion()).isEqualTo(WIDGET_VERSION);
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testToPlatform() {
        TextClassificationContext textClassificationContext =
                new TextClassificationContext.Builder(PKG_NAME, WIDGET_TYPE)
                        .setWidgetVersion(WIDGET_VERSION)
                        .build();
        android.view.textclassifier.TextClassificationContext platformContext =
                (android.view.textclassifier.TextClassificationContext)
                        textClassificationContext.toPlatform();
        assertThat(platformContext.getPackageName()).isEqualTo(PKG_NAME);
        assertThat(platformContext.getWidgetType()).isEqualTo(WIDGET_TYPE);
        assertThat(platformContext.getWidgetVersion()).isEqualTo(WIDGET_VERSION);
    }
}
