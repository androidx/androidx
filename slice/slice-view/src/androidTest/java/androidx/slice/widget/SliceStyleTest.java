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

package androidx.slice.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Xml;

import androidx.slice.SliceItem;
import androidx.slice.view.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

/** Tests for {@link SliceView}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 19)
public class SliceStyleTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private SliceStyle mSliceStyle;

    @Before
    public void setup() {
        mContext.setTheme(R.style.AppTheme);
        // Empty XML file to initialize empty AttributeSet.
        XmlPullParser parser = mContext.getResources().getXml(R.xml.slice_style_test);
        AttributeSet attributes = Xml.asAttributeSet(parser);
        mSliceStyle = new SliceStyle(mContext, attributes, 0, R.style.Widget_SliceView);
    }

    @Test
    public void testGetRowStyle_noRowStyleFactory_noSliceItem_returnsDefaultRowStyle() {
        // RowStyle returns default colors.
        assertDefaultTheme(mSliceStyle.getRowStyle(/* sliceItem= */ null));
    }

    @Test
    public void testGetRowStyle_withRowStyleFactory_noSliceItem_returnsDefaultRowStyle() {
        mSliceStyle.setRowStyleFactory(item -> R.style.CheckedSliceRowStyle);

        // RowStyle returns default colors.
        assertDefaultTheme(mSliceStyle.getRowStyle(/* sliceItem= */ null));
    }

    @Test
    public void
            testGetRowStyle_withRowStyleFactory_withMatchingSliceItem_returnsDifferentRowStyle() {
        // Return a different style for every SliceItem.
        mSliceStyle.setRowStyleFactory(item -> R.style.CheckedSliceRowStyle);

        // RowStyle returns "checked" colors.
        SliceItem sliceItem = new SliceItem();
        assertEquals(
                mContext.getResources().getColor(R.color.checkedItemTitleColor),
                mSliceStyle.getRowStyle(sliceItem).getTitleColor());
        assertEquals(
                mContext.getResources().getColor(R.color.checkedItemSubtitleColor),
                mSliceStyle.getRowStyle(sliceItem).getSubtitleColor());
        assertEquals(
                mContext.getResources().getColor(R.color.checkedItemTintColor),
                mSliceStyle.getRowStyle(sliceItem).getTintColor());
    }

    @Test
    public void testGetRowStyle_withRowStyleFactory_returnsDifferentStyleForMatchingItem() {
        // Return a different style for every SliceItem.
        mSliceStyle.setRowStyleFactory(
                item -> {
                    if (item.getFormat() == android.app.slice.SliceItem.FORMAT_SLICE) {
                        return R.style.CheckedSliceRowStyle;
                    }
                    return 0;
                });

        // RowStyle returns default colors.
        SliceItem notMatchingSliceItem = new SliceItem();
        assertDefaultTheme(mSliceStyle.getRowStyle(notMatchingSliceItem));

        // RowStyle returns "checked" colors.
        SliceItem matchingSliceItem =
                new SliceItem(
                        (Object) null,
                        android.app.slice.SliceItem.FORMAT_SLICE,
                        /* subType= */ "",
                        /* hints= */ new String[] {});
        assertEquals(
                mContext.getResources().getColor(R.color.checkedItemTitleColor),
                mSliceStyle.getRowStyle(matchingSliceItem).getTitleColor());
        assertEquals(
                mContext.getResources().getColor(R.color.checkedItemSubtitleColor),
                mSliceStyle.getRowStyle(matchingSliceItem).getSubtitleColor());
        assertEquals(
                mContext.getResources().getColor(R.color.checkedItemTintColor),
                mSliceStyle.getRowStyle(matchingSliceItem).getTintColor());
    }

    private void assertDefaultTheme(RowStyle rowStyle) {
        int themeTitleColor = getThemeColor(android.R.attr.textColorPrimary);
        int themeSubtitleColor = getThemeColor(android.R.attr.textColorSecondary);
        // SliceStyle returns -1 by default.
        int themeTintColor = -1;

        assertEquals(themeTitleColor, rowStyle.getTitleColor());
        assertEquals(themeSubtitleColor, rowStyle.getSubtitleColor());
        assertEquals(themeTintColor, rowStyle.getTintColor());
    }

    private int getThemeColor(int colorRes) {
        TypedArray arr = mContext.getTheme().obtainStyledAttributes(new int[] {colorRes});
        assertTrue(arr.hasValue(0));
        int themeColor = arr.getColor(0, -1);
        arr.recycle();
        return themeColor;
    }
}
