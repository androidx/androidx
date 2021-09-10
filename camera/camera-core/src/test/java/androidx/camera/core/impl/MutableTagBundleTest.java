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

package androidx.camera.core.impl;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class MutableTagBundleTest {

    private static final String TAG_0 = "Tag_00";
    private static final String TAG_1 = "Tag_01";
    private static final String TAG_2 = "Tag_02";

    private static final Integer TAG_VALUE_0 = 0;
    private static final Integer TAG_VALUE_1 = 1;

    @Test
    public void canCreateEmptyTagBundle() {
        MutableTagBundle mutableTagBundle = MutableTagBundle.create();

        assertThat(mutableTagBundle).isNotNull();
    }

    @Test
    public void canPutTag() {
        MutableTagBundle mutableTagBundle = MutableTagBundle.create();
        mutableTagBundle.putTag(TAG_0, TAG_VALUE_0);

        assertThat(mutableTagBundle.getTag(TAG_0)).isEqualTo(TAG_VALUE_0);
    }

    @Test
    public void canPutString() {
        MutableTagBundle mutableTagBundle = MutableTagBundle.create();
        mutableTagBundle.putTag(TAG_0, "String");

        assertThat(mutableTagBundle.getTag(TAG_0)).isEqualTo("String");
    }

    @Test
    public void canAddTagBundle() {
        MutableTagBundle mutableTagBundle = MutableTagBundle.create();
        mutableTagBundle.putTag(TAG_0, TAG_VALUE_0);

        MutableTagBundle anotherMutableTagBundle = MutableTagBundle.create();
        anotherMutableTagBundle.putTag(TAG_1, TAG_VALUE_1);

        anotherMutableTagBundle.addTagBundle(mutableTagBundle);

        assertThat(mutableTagBundle.listKeys()).containsExactly(TAG_0);
        assertThat(anotherMutableTagBundle.listKeys()).containsExactly(TAG_0, TAG_1);
        assertThat(anotherMutableTagBundle.getTag(TAG_0)).isEqualTo(TAG_VALUE_0);
        assertThat(anotherMutableTagBundle.getTag(TAG_1)).isEqualTo(TAG_VALUE_1);
    }
}
