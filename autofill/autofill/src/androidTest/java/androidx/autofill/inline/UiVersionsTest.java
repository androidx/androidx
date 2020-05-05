/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Bundle;

import androidx.autofill.inline.v1.InlineSuggestionUi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29) // Needed only on 29 and above
public class UiVersionsTest {

    @Test
    public void testStylesBuilder_validStyles() {
        UiVersions.StylesBuilder builder = UiVersions.newStylesBuilder();
        builder.addStyle(InlineSuggestionUi.newStyleBuilder().build());
        Bundle stylesBundle = builder.build();
        assertThat(UiVersions.getVersions(stylesBundle)).containsExactly(
                UiVersions.INLINE_UI_VERSION_1);
        Bundle styleV1Bundle = VersionUtils.readStyleByVersion(stylesBundle,
                UiVersions.INLINE_UI_VERSION_1);
        InlineSuggestionUi.Style style = InlineSuggestionUi.fromBundle(styleV1Bundle);
        assertTrue(style.isValid());
    }

    @Test
    public void testStylesBuilder_emptyStyles_exception() {
        UiVersions.StylesBuilder builder = UiVersions.newStylesBuilder();
        try {
            builder.build();
            fail(); // this line should not be executed
        } catch (IllegalStateException e) {

        }
    }
}
