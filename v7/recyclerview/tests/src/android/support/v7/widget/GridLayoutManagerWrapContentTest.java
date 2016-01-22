/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.v7.widget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.support.v7.widget.BaseWrapContentWithAspectRatioTest.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class GridLayoutManagerWrapContentTest extends BaseWrapContentTest {

    public GridLayoutManagerWrapContentTest() {
        super(new WrapContentConfig(false, false));
    }

    @Override
    RecyclerView.LayoutManager createLayoutManager() {
        return new GridLayoutManager(getActivity(), 3);
    }

    @Test
    public void testHandleSecondLineChangingBorders() throws Throwable {
        TestedFrameLayout.FullControlLayoutParams lp =
                mWrapContentConfig.toLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        WrapContentAdapter adapter = new WrapContentAdapter(
                new MeasureBehavior(10, 10, WRAP_CONTENT, WRAP_CONTENT),
                new MeasureBehavior(10, 10, WRAP_CONTENT, WRAP_CONTENT),
                new MeasureBehavior(10, 10, WRAP_CONTENT, WRAP_CONTENT),
                new MeasureBehavior(20, 10, WRAP_CONTENT, WRAP_CONTENT)
        );
        Rect[] expected = new Rect[] {
                new Rect(0, 0, 10, 10),
                new Rect(20, 0, 30, 10),
                new Rect(40, 0, 50, 10),
                new Rect(0, 10, 20, 20)
        };
        layoutAndCheck(lp, adapter, expected, 60, 20);
    }

    @Test
    public void testSecondLineAffectingBordersWithAspectRatio() throws Throwable {
        TestedFrameLayout.FullControlLayoutParams lp =
                mWrapContentConfig.toLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        WrapContentAdapter adapter = new WrapContentAdapter(
                new AspectRatioMeasureBehavior(10, 5, MATCH_PARENT, WRAP_CONTENT)
                        .aspectRatio(HORIZONTAL, .5f),
                new MeasureBehavior(10, 5, WRAP_CONTENT, WRAP_CONTENT),
                new MeasureBehavior(10, 5, MATCH_PARENT, WRAP_CONTENT),
                new MeasureBehavior(20, 10, WRAP_CONTENT, WRAP_CONTENT)
        );
        Rect[] expected = new Rect[] {
                new Rect(0, 0, 20, 10),
                new Rect(20, 0, 30, 10),
                new Rect(40, 0, 60, 10),
                new Rect(0, 10, 20, 20)
        };
        layoutAndCheck(lp, adapter, expected, 60, 20);
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
