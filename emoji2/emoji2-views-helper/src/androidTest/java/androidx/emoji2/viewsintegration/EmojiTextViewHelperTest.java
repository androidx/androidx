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

package androidx.emoji2.viewsintegration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.widget.TextView;

import androidx.emoji2.text.EmojiCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.hamcrest.CoreMatchers;
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
        mTextView = new TextView(ApplicationProvider.getApplicationContext());
        mTextViewHelper = new EmojiTextViewHelper(mTextView);
        EmojiCompat.reset(mock(EmojiCompat.class));
    }

    @Test
    public void testUpdateTransformationMethod() {
        mTextView.setTransformationMethod(mock(TransformationMethod.class));

        mTextViewHelper.updateTransformationMethod();

        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));
    }

    @Test
    public void testUpdateTransformationMethod_whenDisabled_doesntWrap() {
        TransformationMethod mockTransform = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(mockTransform);

        mTextViewHelper.setEnabled(false);
        mTextViewHelper.updateTransformationMethod();

        assertThat(mTextView.getTransformationMethod(), is(mockTransform));
    }

    @Test
    public void testUpdateTransformation_whenNotConfigured_NoEffectWhenSkipConfig() {
        EmojiCompat.reset((EmojiCompat) null);
        mTextViewHelper = new EmojiTextViewHelper(mTextView, false);
        TransformationMethod mockTransform = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(mockTransform);
        mTextViewHelper.updateTransformationMethod();
        assertThat(mTextView.getTransformationMethod(), is(mockTransform));
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
        assertThat(Arrays.asList(newFilters), CoreMatchers.hasItem(emojiInputFilter));
    }

    @Test
    public void getFilter_addsFilter_whenEmptyArray() {
        InputFilter[] actual = mTextViewHelper.getFilters(new InputFilter[0]);
        EmojiInputFilter emojiInputFilter = findEmojiInputFilter(actual);
        assertNotNull(emojiInputFilter);
    }

    @Test
    public void getFilter_doesntAddFilter_whenDisabled() {
        mTextViewHelper.setEnabled(false);
        InputFilter[] expected = new InputFilter[0];
        InputFilter[] actual = mTextViewHelper.getFilters(expected);
        assertThat(expected, is(actual));
    }

    @Test
    public void setFilter_doesntAddFilter_whenNotConfiguredAndSkipping() {
        EmojiCompat.reset((EmojiCompat) null);
        mTextViewHelper = new EmojiTextViewHelper(mTextView, false);
        InputFilter[] expected = new InputFilter[0];
        InputFilter[] actual = mTextViewHelper.getFilters(expected);
        assertThat(expected, is(actual));
    }

    @Test
    public void getFilter_removesFilter_whenDisabled() {
        mTextViewHelper.setEnabled(false);
        InputFilter[] input = new InputFilter[1];
        input[0] = new EmojiInputFilter(mTextView);
        InputFilter[] actual = mTextViewHelper.getFilters(input);
        assertThat(actual, equalTo(new InputFilter[0]));
    }

    @Test
    public void getFilter_removesAllFilters_whenDisabled() {
        mTextViewHelper.setEnabled(false);
        InputFilter[] input = new InputFilter[3];
        input[0] = new EmojiInputFilter(mTextView);
        input[1] = mock(InputFilter.class);
        input[2] = new EmojiInputFilter(mTextView);
        InputFilter[] actual = mTextViewHelper.getFilters(input);
        InputFilter[] expected = new InputFilter[1];
        expected[0] = input[1];
        assertThat(actual, equalTo(expected));
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
    public void testWrapTransformationMethod_doesNotCreateNewInstance() {
        final TransformationMethod tm1 = mTextViewHelper.wrapTransformationMethod(null);
        final TransformationMethod tm2 = mTextViewHelper.wrapTransformationMethod(tm1);
        assertSame(tm1, tm2);
    }

    @Test
    public void wrapTransformationMethod_whenEnabled_andPassword_returnsOriginal() {
        TransformationMethod expected = new PasswordTransformationMethod();
        TransformationMethod actual = mTextViewHelper.wrapTransformationMethod(expected);
        assertThat(actual, is(expected));
    }

    @Test
    public void wrapTransformationMethod_whenDisabled_andPassword_returnsOriginal() {
        mTextViewHelper.setEnabled(false);
        TransformationMethod expected = new PasswordTransformationMethod();
        TransformationMethod actual = mTextViewHelper.wrapTransformationMethod(expected);
        assertThat(actual, is(expected));
    }

    @Test
    public void wrapTransformationMethod_whenEnabled_andEmoji_returnsOriginal() {
        TransformationMethod expected = new EmojiTransformationMethod(null);
        TransformationMethod actual = mTextViewHelper.wrapTransformationMethod(expected);
        assertThat(actual, is(expected));
    }

    @Test
    public void wrapTransformationMethod_whenDisabled_andEmoji_returnsUnwrapped() {
        TransformationMethod expected = mock(TransformationMethod.class);
        TransformationMethod input = new EmojiTransformationMethod(expected);
        mTextViewHelper.setEnabled(false);
        TransformationMethod actual = mTextViewHelper.wrapTransformationMethod(input);
        assertThat(actual, is(expected));
    }

    @Test
    public void wrapTransformationMethod_whenNull_andEnabled_returnsEmojiTransformationMethod() {
        TransformationMethod actual = mTextViewHelper.wrapTransformationMethod(null);
        assertThat(actual, instanceOf(EmojiTransformationMethod.class));
    }

    @Test
    public void wrapTransformationMethod_whenNull_andDisabled_returnsNull() {
        mTextViewHelper.setEnabled(false);
        TransformationMethod actual = mTextViewHelper.wrapTransformationMethod(null);
        assertNull(actual);
    }

    @Test
    public void wrapTransformationMethod_whenNotEmoji_andDisabled_returnsOriginal() {
        mTextViewHelper.setEnabled(false);
        TransformationMethod expected = mock(TransformationMethod.class);
        TransformationMethod actual = mTextViewHelper.wrapTransformationMethod(expected);
        assertThat(actual, is(expected));
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

    @Test
    public void setEnabled_fromEnabled_leavesTransformationMethod() {
        TransformationMethod mockTransform = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(mockTransform);
        mTextViewHelper.updateTransformationMethod();
        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));
        mTextViewHelper.setEnabled(true);
        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));
    }

    @Test
    public void setEnabled_fromEnabled_leavesFilter() {
        InputFilter[] expected = mTextViewHelper.getFilters(new InputFilter[0]);
        mTextView.setFilters(expected);
        mTextViewHelper.setEnabled(true);
        assertThat(mTextView.getFilters(), is(expected));
    }

    @Test
    public void setDisabled_fromEnabled_unWrapsTransformationMethod() {
        TransformationMethod mockTransform = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(mockTransform);
        mTextViewHelper.updateTransformationMethod();
        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));

        mTextViewHelper.setEnabled(false);
        assertThat(mTextView.getTransformationMethod(), is(mockTransform));
    }

    @Test
    public void setDisabled_fromEnabled_removesFilter() {
        InputFilter[] expected = new InputFilter[0];
        mTextView.setFilters(mTextViewHelper.getFilters(expected));
        mTextViewHelper.setEnabled(false);
        assertThat(mTextView.getFilters(), equalTo(expected));
    }

    @Test
    public void setEnabled_whenEnabling_reWrapsTransformationMethod() {
        TransformationMethod mockTransform = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(mockTransform);
        mTextViewHelper.updateTransformationMethod();
        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));

        mTextViewHelper.setEnabled(false);
        assertThat(mTextView.getTransformationMethod(), is(mockTransform));

        mTextViewHelper.setEnabled(true);
        assertThat(mTextView.getTransformationMethod(),
                instanceOf(EmojiTransformationMethod.class));
    }

    @Test
    public void setEnabled_whenEnabling_reAddsFilter() {
        InputFilter[] expected = mTextViewHelper.getFilters(new InputFilter[0]);
        mTextView.setFilters(expected);
        mTextViewHelper.setEnabled(false);
        assertThat(mTextView.getFilters(), equalTo(new InputFilter[0]));
        mTextViewHelper.setEnabled(true);
        assertThat(mTextView.getFilters(), equalTo(expected));
    }

    @Test
    public void setEnabled_whenNotConfigured_andSkipConfig_doesNothing() {
        EmojiCompat.reset((EmojiCompat) null);
        mTextViewHelper = new EmojiTextViewHelper(mTextView, false);

        InputFilter[] expectedInput = new InputFilter[1];
        expectedInput[0] = mock(InputFilter.class);
        mTextView.setFilters(expectedInput);
        TransformationMethod expectedTransform = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(expectedTransform);

        mTextViewHelper.setEnabled(true);
        InputFilter[] actualInput1 = mTextView.getFilters();
        TransformationMethod actualTransform1 = mTextView.getTransformationMethod();
        mTextViewHelper.setEnabled(false);
        InputFilter[] actualInput2 = mTextView.getFilters();
        TransformationMethod actualTransform2 = mTextView.getTransformationMethod();
        mTextViewHelper.setEnabled(true);
        InputFilter[] actualInput3 = mTextView.getFilters();
        TransformationMethod actualTransform3 = mTextView.getTransformationMethod();

        // use transitive reference equality to do a bunch of assertions
        assertSame(expectedInput, actualInput1);
        assertSame(actualInput1, actualInput2);
        assertSame(actualInput2, actualInput3);

        assertSame(expectedTransform, actualTransform1);
        assertSame(actualTransform1, actualTransform2);
        assertSame(actualTransform2, actualTransform3);
    }

}
