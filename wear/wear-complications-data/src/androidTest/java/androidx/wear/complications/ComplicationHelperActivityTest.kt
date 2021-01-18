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
package androidx.wear.complications

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.data.ComplicationType
import com.google.common.truth.Truth
import org.junit.Test

public class ComplicationHelperActivityTest {
    @Test
    public fun createProviderChooserHelperIntent() {
        val complicationId = 1234
        val watchFaceComponentName = ComponentName("test.package", "test.class")
        val complicationTypes = listOf(ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT)
        val intent = ComplicationHelperActivity.createProviderChooserHelperIntent(
            ApplicationProvider.getApplicationContext(),
            watchFaceComponentName,
            complicationId,
            complicationTypes
        )
        val expectedSupportedTypes = intArrayOf(
            ComplicationType.SHORT_TEXT.asWireComplicationType(),
            ComplicationType.LONG_TEXT.asWireComplicationType()
        )
        val actualComponentName = intent.getParcelableExtra<ComponentName>(
            ProviderChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME
        )
        Truth.assertThat(actualComponentName).isEqualTo(watchFaceComponentName)
        Truth.assertThat(intent.getIntExtra(ProviderChooserIntent.EXTRA_COMPLICATION_ID, -1))
            .isEqualTo(complicationId)
        Truth.assertThat(intent.getIntArrayExtra(ProviderChooserIntent.EXTRA_SUPPORTED_TYPES))
            .isEqualTo(expectedSupportedTypes)
        Truth.assertThat(intent.action)
            .isEqualTo(ComplicationHelperActivity.ACTION_START_PROVIDER_CHOOSER)
        Truth.assertThat(intent.component!!.className)
            .isEqualTo(ComplicationHelperActivity::class.java.name)
    }
}
