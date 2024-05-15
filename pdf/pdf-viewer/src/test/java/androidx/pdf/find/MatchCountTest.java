/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.find;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MatchCountTest {
    private final MatchCount mMatchCount1 =
            new MatchCount(2, 10, true);
    private final MatchCount mMatchCount2 =
            new MatchCount(3, 12, false);
    private final MatchCount mMatchCount3 =
            new MatchCount(2, 10, true);

    @Test
    public void equals_ComparingEqualObjects() {
        boolean equals = mMatchCount1.equals(mMatchCount3);
        assertThat(equals).isTrue();
        assertThat(mMatchCount1.toString()).isEqualTo("MatchCount(2 of 10, allPagesCounted=true)");
    }

    @Test
    public void equals_ComparingUnequalObjects() {
        boolean equals = mMatchCount1.equals(mMatchCount2);
        assertThat(equals).isFalse();
        assertThat(mMatchCount2.toString()).isEqualTo("MatchCount(3 of 12, allPagesCounted=false)");
    }

}
