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

package androidx.recyclerview.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.recyclerview.widget.StaggeredGridLayoutManager.HORIZONTAL;

import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(Parameterized.class)
public class StaggeredGridLayoutManagerWrapContentTest extends BaseWrapContentTest {
    int mOrientation = StaggeredGridLayoutManager.VERTICAL;
    public StaggeredGridLayoutManagerWrapContentTest(Rect padding) {
        super(new WrapContentConfig(false, false, padding));
    }

    @Parameterized.Parameters(name = "paddingRect={0}")
    public static List<Rect> params() {
        return Arrays.asList(
                new Rect(0, 0, 0, 0),
                new Rect(5, 0, 0, 0),
                new Rect(0, 3, 0, 0),
                new Rect(0, 0, 2, 0),
                new Rect(0, 0, 0, 7),
                new Rect(3, 5, 7, 11)
        );
    }

    @Test
    public void testUnspecifiedWithHint() throws Throwable {
        unspecifiedWithHintTest(mOrientation == StaggeredGridLayoutManager.HORIZONTAL);
    }

    @Test
    public void testSimple() throws Throwable {
        TestedFrameLayout.FullControlLayoutParams lp =
                mWrapContentConfig.toLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        BaseWrapContentWithAspectRatioTest.WrapContentAdapter
                adapter = new BaseWrapContentWithAspectRatioTest.WrapContentAdapter(
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 10, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 15, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 20, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(20, 10, WRAP_CONTENT, WRAP_CONTENT)
        );
        Rect[] expected = new Rect[] {
                new Rect(0, 0, 10, 10),
                new Rect(20, 0, 30, 15),
                new Rect(40, 0, 50, 20),
                new Rect(0, 10, 20, 20)
        };
        layoutAndCheck(lp, adapter, expected, 60, 20);
    }

    @Test
    public void testSimpleHorizontal() throws Throwable {
        mOrientation = HORIZONTAL;
        TestedFrameLayout.FullControlLayoutParams lp =
                mWrapContentConfig.toLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        BaseWrapContentWithAspectRatioTest.WrapContentAdapter
                adapter = new BaseWrapContentWithAspectRatioTest.WrapContentAdapter(
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 10, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(15, 10, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(20, 10, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 20, WRAP_CONTENT, WRAP_CONTENT)
        );
        Rect[] expected = new Rect[] {
                new Rect(0, 0, 10, 10),
                new Rect(0, 20, 15, 30),
                new Rect(0, 40, 20, 50),
                new Rect(10, 0, 20, 20)
        };
        layoutAndCheck(lp, adapter, expected, 20, 60);
    }

    @Test
    public void testUnspecifiedWidth() throws Throwable {
        TestedFrameLayout.FullControlLayoutParams lp =
                mWrapContentConfig.toLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        lp.wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        BaseWrapContentWithAspectRatioTest.WrapContentAdapter
                adapter = new BaseWrapContentWithAspectRatioTest.WrapContentAdapter(
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(2000, 10, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(500, 15, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(400, 20, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(50, 10, MATCH_PARENT, WRAP_CONTENT)
        );
        Rect[] expected = new Rect[] {
                new Rect(0, 0, 2000, 10),
                new Rect(2000, 0, 2500, 15),
                new Rect(4000, 0, 4400, 20),
                new Rect(0, 10, 2000, 20)
        };
        layoutAndCheck(lp, adapter, expected, 6000, 20);
    }

    @Test
    public void testUnspecifiedHeight() throws Throwable {
        TestedFrameLayout.FullControlLayoutParams lp =
                mWrapContentConfig.toLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        lp.hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        BaseWrapContentWithAspectRatioTest.WrapContentAdapter
                adapter = new BaseWrapContentWithAspectRatioTest.WrapContentAdapter(
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 4000, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 5500, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(10, 3000, WRAP_CONTENT, WRAP_CONTENT),
                new BaseWrapContentWithAspectRatioTest.MeasureBehavior(20, 100, WRAP_CONTENT, WRAP_CONTENT)
        );
        Rect[] expected = new Rect[] {
                new Rect(0, 0, 10, 4000),
                new Rect(20, 0, 30, 5500),
                new Rect(40, 0, 50, 3000),
                new Rect(40, 3000, 60, 3100)
        };
        layoutAndCheck(lp, adapter, expected, 60, 5500);
    }

    @Override
    RecyclerView.LayoutManager createLayoutManager() {
        return new StaggeredGridLayoutManager(3, mOrientation);
    }

    @Override
    protected int getVerticalGravity(RecyclerView.LayoutManager layoutManager) {
        return Gravity.TOP;
    }

    @Override
    protected int getHorizontalGravity(RecyclerView.LayoutManager layoutManager) {
        return Gravity.LEFT;
    }
}
