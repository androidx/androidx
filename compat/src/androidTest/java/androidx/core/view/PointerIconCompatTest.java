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
package androidx.core.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.PointerIcon;
import android.view.View;

import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
public class PointerIconCompatTest extends BaseInstrumentationTestCase<ViewCompatActivity> {

    private View mView;
    private Activity mActivity;

    public PointerIconCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mView = mActivity.findViewById(R.id.view);
    }

    private void compareSystemIcon(int type, int compatType) {
        ViewCompat.setPointerIcon(mView, PointerIconCompat.getSystemIcon(mActivity, compatType));
        assertEquals(PointerIcon.getSystemIcon(mActivity, type), mView.getPointerIcon());
    }

    @Test
    @UiThreadTest
    public void testSystemIcon() {
        compareSystemIcon(PointerIcon.TYPE_ALIAS, PointerIconCompat.TYPE_ALIAS);
        compareSystemIcon(PointerIcon.TYPE_ALL_SCROLL, PointerIconCompat.TYPE_ALL_SCROLL);
        compareSystemIcon(PointerIcon.TYPE_ARROW, PointerIconCompat.TYPE_ARROW);
        compareSystemIcon(PointerIcon.TYPE_CELL, PointerIconCompat.TYPE_CELL);
        compareSystemIcon(PointerIcon.TYPE_CONTEXT_MENU, PointerIconCompat.TYPE_CONTEXT_MENU);
        compareSystemIcon(PointerIcon.TYPE_COPY, PointerIconCompat.TYPE_COPY);
        compareSystemIcon(PointerIcon.TYPE_CROSSHAIR, PointerIconCompat.TYPE_CROSSHAIR);
        compareSystemIcon(PointerIcon.TYPE_DEFAULT, PointerIconCompat.TYPE_DEFAULT);
        compareSystemIcon(PointerIcon.TYPE_GRAB, PointerIconCompat.TYPE_GRAB);
        compareSystemIcon(PointerIcon.TYPE_GRABBING, PointerIconCompat.TYPE_GRABBING);
        compareSystemIcon(PointerIcon.TYPE_HAND, PointerIconCompat.TYPE_HAND);
        compareSystemIcon(PointerIcon.TYPE_HELP, PointerIconCompat.TYPE_HELP);
        compareSystemIcon(PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW,
                PointerIconCompat.TYPE_HORIZONTAL_DOUBLE_ARROW);
        compareSystemIcon(PointerIcon.TYPE_NO_DROP, PointerIconCompat.TYPE_NO_DROP);
        compareSystemIcon(PointerIcon.TYPE_NULL, PointerIconCompat.TYPE_NULL);
        compareSystemIcon(PointerIcon.TYPE_TEXT, PointerIconCompat.TYPE_TEXT);
        compareSystemIcon(PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW,
                PointerIconCompat.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW);
        compareSystemIcon(PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW,
                PointerIconCompat.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW);
        compareSystemIcon(PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW,
                PointerIconCompat.TYPE_VERTICAL_DOUBLE_ARROW);
        compareSystemIcon(PointerIcon.TYPE_VERTICAL_TEXT,
                PointerIconCompat.TYPE_VERTICAL_TEXT);
        compareSystemIcon(PointerIcon.TYPE_WAIT, PointerIconCompat.TYPE_WAIT);
        compareSystemIcon(PointerIcon.TYPE_ZOOM_IN, PointerIconCompat.TYPE_ZOOM_IN);
        compareSystemIcon(PointerIcon.TYPE_ZOOM_OUT, PointerIconCompat.TYPE_ZOOM_OUT);
    }

    @Test
    @UiThreadTest
    public void testNullIcon() {
        ViewCompat.setPointerIcon(mView, null);
        assertNull(mView.getPointerIcon());
    }

    @Test
    @UiThreadTest
    public void testBitmapIcon() {
        Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        ViewCompat.setPointerIcon(mView, PointerIconCompat.create(bitmap, 0, 0));
        assertNotNull(mView.getPointerIcon());
    }

    @Test
    @UiThreadTest
    public void testResourceIcon() {
        ViewCompat.setPointerIcon(mView,
                PointerIconCompat.load(mActivity.getResources(), R.drawable.pointer_icon));
        assertNotNull(mView.getPointerIcon());
    }
}
