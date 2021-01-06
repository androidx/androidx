/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.complications;

import android.content.ComponentName;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.wear.complications.data.ComplicationType;

import com.google.common.truth.Truth;

import org.junit.Test;

import java.util.List;

public class ComplicationHelperActivityTest {

    @Test
    public void createProviderChooserHelperIntent() {
        int complicationId = 1234;
        ComponentName watchFaceComponentName = new ComponentName("test.package", "test.class");
        List<ComplicationType> complicationTypes =
                List.of(ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT);
        Intent intent = ComplicationHelperActivity.createProviderChooserHelperIntent(
                ApplicationProvider.getApplicationContext(),
                watchFaceComponentName,
                complicationId,
                complicationTypes
        );

        int[] expectedSupportedTypes = {
                ComplicationType.SHORT_TEXT.asWireComplicationType(),
                ComplicationType.LONG_TEXT.asWireComplicationType()
        };

        ComponentName actualComponentName =
                intent.getParcelableExtra(ProviderChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME);
        Truth.assertThat(actualComponentName).isEqualTo(watchFaceComponentName);
        Truth.assertThat(intent.getIntExtra(ProviderChooserIntent.EXTRA_COMPLICATION_ID, -1))
                .isEqualTo(complicationId);
        Truth.assertThat(intent.getIntArrayExtra(ProviderChooserIntent.EXTRA_SUPPORTED_TYPES))
                .isEqualTo(expectedSupportedTypes);
        Truth.assertThat(intent.getAction())
                .isEqualTo(ComplicationHelperActivity.ACTION_START_PROVIDER_CHOOSER);
        Truth.assertThat(intent.getComponent().getClassName())
                .isEqualTo(ComplicationHelperActivity.class.getName());
    }
}
