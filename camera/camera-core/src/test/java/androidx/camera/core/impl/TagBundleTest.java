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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class TagBundleTest {

    private static final String TAG_0 = "Tag_00";
    private static final String TAG_1 = "Tag_01";
    private static final String TAG_2 = "Tag_02";

    private static final Integer TAG_VALUE_0 = 0;
    private static final Integer TAG_VALUE_1 = 1;
    private static final Integer TAG_VALUE_2 = 2;

    TagBundle mTagBundle;
    private static final List<String> KEY_LIST = new ArrayList<>();

    static {
        KEY_LIST.add(TAG_0);
        KEY_LIST.add(TAG_1);
        KEY_LIST.add(TAG_2);
    }

    @Before
    public void setUp() {
        MutableTagBundle mutableTagBundle = MutableTagBundle.create();
        mutableTagBundle.putTag(TAG_0, TAG_VALUE_0);
        mutableTagBundle.putTag(TAG_1, TAG_VALUE_1);
        mutableTagBundle.putTag(TAG_2, TAG_VALUE_2);

        mTagBundle = TagBundle.from(mutableTagBundle);
    }

    @Test
    public void canCreateFromAnotherTagBundle() {
        TagBundle tagBundle = TagBundle.from(mTagBundle);

        assertThat(tagBundle.getTag(TAG_0)).isEqualTo(TAG_VALUE_0);
        assertThat(tagBundle.getTag(TAG_1)).isEqualTo(TAG_VALUE_1);
        assertThat(tagBundle.getTag(TAG_2)).isEqualTo(TAG_VALUE_2);
    }

    @Test
    public void canGetTagValue() {
        assertThat(mTagBundle.getTag(TAG_0)).isEqualTo(TAG_VALUE_0);
        assertThat(mTagBundle.getTag(TAG_1)).isEqualTo(TAG_VALUE_1);
        assertThat(mTagBundle.getTag(TAG_2)).isEqualTo(TAG_VALUE_2);
    }

    @Test
    public void canGetKeyList() {
        Set<String> keyList = mTagBundle.listKeys();

        assertThat(keyList).containsExactly(TAG_0, TAG_1, TAG_2);
    }
}
