/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.content;

import static com.google.common.truth.Truth.assertThat;

import android.content.UriMatcher;
import android.net.Uri;

import androidx.core.util.Predicate;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link UriMatcherCompat}
 */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class UriMatcherCompatTest {
    static final int ROOT = 0;
    static final int PEOPLE = 1;

    @Test
    public void testAsPredicate() {
        UriMatcher matcher = new UriMatcher(ROOT);
        matcher.addURI("people", "/", PEOPLE);
        Predicate<Uri> uriPredicate = UriMatcherCompat.asPredicate(matcher);

        assertThat(uriPredicate.test(Uri.parse("content://people"))).isTrue();
        assertThat(uriPredicate.test(Uri.parse("content://asdf"))).isFalse();
    }
}
