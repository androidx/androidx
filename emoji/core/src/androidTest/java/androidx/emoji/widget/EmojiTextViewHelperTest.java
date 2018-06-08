/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.emoji.widget;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiTextViewHelperTest {
    EmojiTextViewHelper mTextViewHelper;
    TextView mTextView;

    @Before
    public void setup() {
        mTextView = new TextView(InstrumentationRegistry.getTargetContext());
        mTextViewHelper = new EmojiTextViewHelper(mTextView);
    }

    @Test
    public void testUpdateTransformationMethod() {
        mTextView.setTransformationMethod(mock(TransformationMethod.class));

        mTextViewHelper.updateTransformationMethod();

        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));
    }

    @Test
    public void testUpdateTransformationMethod_doesNotUpdateForPasswordTransformation() {
        final PasswordTransformationMethod transformationMethod =
                new PasswordTransformationMethod();
        mTextView.setTransformationMethod(transformationMethod);

        mTextViewHelper.updateTransformationMethod();

        assertEquals(transformationMethod, mTextView.getTransformationMethod());
    }

    @Test
    public void testUpdateTransformationMethod_doesNotCreateNewInstance() {
        mTextView.setTransformationMethod(mock(TransformationMethod.class));

        mTextViewHelper.updateTransformationMethod();
        final TransformationMethod tm = mTextView.getTransformationMethod();
        assertThat(tm, instanceOf(EmojiTransformationMethod.class));

        // call the function again
        mTextViewHelper.updateTransformationMethod();
        assertSame(tm, mTextView.getTransformationMethod());
    }

    @Test
    public void testGetFilters() {
        final InputFilter existingFilter = mock(InputFilter.class);
        final InputFilter[] filters = new InputFilter[]{existingFilter};

        final InputFilter[] newFilters = mTextViewHelper.getFilters(filters);

        assertEquals(2, newFilters.length);
        assertThat(Arrays.asList(newFilters), hasItem(existingFilter));
        assertNotNull(findEmojiInputFilter(newFilters));
    }

    @Test
    public void testGetFilters_doesNotAddSecondInstance() {
        final InputFilter existingFilter = mock(InputFilter.class);
        final InputFilter[] filters = new InputFilter[]{existingFilter};

        InputFilter[] newFilters = mTextViewHelper.getFilters(filters);
        EmojiInputFilter emojiInputFilter = findEmojiInputFilter(newFilters);
        assertNotNull(emojiInputFilter);

        // run it again with the updated filters and see that it does not add new filter
        newFilters = mTextViewHelper.getFilters(newFilters);

        assertEquals(2, newFilters.length);
        assertThat(Arrays.asList(newFilters), hasItem(existingFilter));
        assertThat(Arrays.asList(newFilters), hasItem(emojiInputFilter));
    }

    private EmojiInputFilter findEmojiInputFilter(final InputFilter[] filters) {
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] instanceof EmojiInputFilter) {
                return (EmojiInputFilter) filters[i];
            }
        }
        return null;
    }

    @Test
    public void testWrapTransformationMethod() {
        assertThat(mTextViewHelper.wrapTransformationMethod(null),
                instanceOf(EmojiTransformationMethod.class));
    }

    @Test
    public void testWrapTransformationMethod_doesNotCreateNewInstance() {
        final TransformationMethod tm1 = mTextViewHelper.wrapTransformationMethod(null);
        final TransformationMethod tm2 = mTextViewHelper.wrapTransformationMethod(tm1);
        assertSame(tm1, tm2);
    }

    @Test
    public void testSetAllCaps_withTrueSetsTransformationMethod() {
        mTextView.setTransformationMethod(mock(TransformationMethod.class));
        mTextViewHelper.setAllCaps(true);
        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));
    }

    @Test
    public void testSetAllCaps_withFalseDoesNotSetTransformationMethod() {
        mTextView.setTransformationMethod(null);
        mTextViewHelper.setAllCaps(false);
        assertNull(mTextView.getTransformationMethod());
    }

    @Test
    public void testSetAllCaps_withPasswordTransformationDoesNotSetTransformationMethod() {
        final PasswordTransformationMethod transformationMethod =
                new PasswordTransformationMethod();
        mTextView.setTransformationMethod(transformationMethod);
        mTextViewHelper.setAllCaps(true);
        assertSame(transformationMethod, mTextView.getTransformationMethod());
    }
}
