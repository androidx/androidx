/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_IN_DIRECTION;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static com.google.common.truth.Truth.assertThat;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class GridLayoutManagerUiAutomationTests extends BaseGridLayoutManagerTest {

    private static final int DEFAULT_ACCESSIBILITY_EVENT_TIMEOUT_MILLIS = 5000;

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_withoutSpecifyingDirection()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(6, HORIZONTAL);
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        final boolean[] returnValue = {false};
        mActivityRule.runOnUiThread(
                () -> {
                    returnValue[0] = mRecyclerView.getLayoutManager().performAccessibilityAction(
                            ACTION_SCROLL_IN_DIRECTION.getId(), null);
                });
        assertThat(returnValue[0]).isFalse();
        assertThat(mGlm.mRowWithAccessibilityFocus).isEqualTo(-1);
        assertThat(mGlm.mColumnWithAccessibilityFocus).isEqualTo(-1);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_withInvalidDirection()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(6, HORIZONTAL);
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndFail(-1, Pair.create(-1, -1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_withoutSettingAccessibilityFocus()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        // Return value of this call is not used.
        setUpGridLayoutManagerAccessibilityTest(6, HORIZONTAL);
        runScrollInDirectionAndFail(View.FOCUS_RIGHT, Pair.create(-1, -1));

    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_vertical_scrollTargetOnTheSameRow()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (3)",
                Pair.create(0, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_vertical_scrollTargetOnTheNextRow()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4   5
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(2));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (4)",
                Pair.create(1, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_vertical_traversingThroughASpan()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        mRecyclerView = setupBasic(new Config(4, 4));
        mGlm.setOrientation(VERTICAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 1) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   2   2   3
        4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (2)" ,
                Pair.create(0, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (3)" ,
                Pair.create(0, 3));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_vertical_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4   5
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(4));
        runScrollInDirectionAndFail(View.FOCUS_RIGHT, Pair.create(1, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_horizontal_scrollTargetOnTheSameRow()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (4)" ,
                Pair.create(0, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_horizontal_traversingThroughASpan()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        setUpRecyclerViewAndGridLayoutManager(8, HORIZONTAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 4) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   4   6
        2   5   7
        3   5   8
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(2));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (5)" ,
                Pair.create(2, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(4));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (8)" ,
                Pair.create(2, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_horizontal_withWrapAround()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        setUpRecyclerViewAndGridLayoutManager(8, HORIZONTAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   3   6
        1   4   7
        2   5   8
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(5));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (1)" ,
                Pair.create(1, 0));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_RIGHT, "Item (4)" ,
                Pair.create(1, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_horizontal_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(2));
        runScrollInDirectionAndFail(View.FOCUS_RIGHT);
        assertThat(mGlm.mRowWithAccessibilityFocus).isEqualTo(2);
        assertThat(mGlm.mColumnWithAccessibilityFocus).isEqualTo(0);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_vertical_scrollTargetOnTheSameRow()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (1)",
                Pair.create(0, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_vertical_scrollTargetOnAPreviousRow()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4   5
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(3));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (3)",
                Pair.create(0, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_vertical_traversingThroughASpan()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        setUpRecyclerViewAndGridLayoutManager(4, VERTICAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 1) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   2   2
        3   4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(2));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (2)",
                Pair.create(0, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_vertical_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4   5
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndFail(View.FOCUS_LEFT, Pair.create(0, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_horizontal_scrollTargetOnTheSameRow()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(4));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (2)" ,
                Pair.create(1, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_horizontal_traversingThroughASpan()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        setUpRecyclerViewAndGridLayoutManager(8, HORIZONTAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 4) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   4   6
        2   5   7
        3   5   8
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(7));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (5)" ,
                Pair.create(2, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(4));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (3)" ,
                Pair.create(2, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_horizontal_withWrapAround()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        setUpRecyclerViewAndGridLayoutManager(8, HORIZONTAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 6) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   4   7
        2   5   7
        3   6   8
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(2));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (7)" ,
                Pair.create(1, 2));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(6));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_LEFT, "Item (5)" ,
                Pair.create(1, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_horizontal_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndFail(View.FOCUS_LEFT, Pair.create(0, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_vertical_scrollTargetOnTheSameColumn()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(3));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_UP, "Item (1)",
                Pair.create(0, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_vertical_traversingThroughASpan()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        mRecyclerView = setupBasic(new Config(3, 8));
        mGlm.setOrientation(VERTICAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 3) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   2   3
        4   4   5
        6   7   8
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(6));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_UP, "Item (4)" ,
                Pair.create(1, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(3));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_UP, "Item (2)" ,
                Pair.create(0, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_vertical_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndFail(View.FOCUS_UP, Pair.create(0, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_horizontal_scrollTargetOnTheSameColumn()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_UP, "Item (1)" ,
                Pair.create(0, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_horizontal_traversingThroughASpan()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        mRecyclerView = setupBasic(new Config(4, 9));
        mGlm.setOrientation(HORIZONTAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 5) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   5   8
        2   6   9
        3   6
        4   7
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(6));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_UP, "Item (6)" ,
                Pair.create(2, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(5));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_UP, "Item (5)" ,
                Pair.create(0, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_horizontal_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(3));
        runScrollInDirectionAndFail(View.FOCUS_UP, Pair.create(0, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusDown_vertical_scrollTargetOnTheSameColumn()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_DOWN, "Item (4)", Pair.create(1,
                0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusDown_vertical_traversingThroughASpan()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        mRecyclerView = setupBasic(new Config(3, 8));
        mGlm.setOrientation(VERTICAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 3) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   2   3
        4   4   5
        6   7   8
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_DOWN, "Item (4)" ,
                Pair.create(1, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(3));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_DOWN, "Item (7)" ,
                Pair.create(2, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusDown_vertical_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndFail(View.FOCUS_DOWN, Pair.create(0, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(3));
        runScrollInDirectionAndFail(View.FOCUS_DOWN, Pair.create(1, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusDown_horizontal_scrollTargetOnTheSameColumn()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(1));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_DOWN, "Item (3)" ,
                Pair.create(2, 0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusDown_horizontal_traversingThroughASpan()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        mRecyclerView = setupBasic(new Config(4, 9));
        mGlm.setOrientation(HORIZONTAL);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 5) {
                    return 2;
                }
                return 1;
            }
        });
        waitForFirstLayout(mRecyclerView);
        /*
        This generates the following grid:
        1   5   8
        2   6   9
        3   6
        4   7
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(4));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_DOWN, "Item (6)" ,
                Pair.create(1, 1));

        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(5));
        runScrollInDirectionAndSucceed(uiAutomation, View.FOCUS_DOWN, "Item (7)" ,
                Pair.create(3, 1));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusDown_horizontal_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(2));
        runScrollInDirectionAndFail(View.FOCUS_DOWN, Pair.create(2, 0));
    }

    /**
     * Verifies that a scroll successfully occurs in the specified {@code direction}.
     *
     * @param uiAutomation  UiAutomation instance.
     * @param direction The direction of the scroll.
     * @param scrollTargetText The text of the view targeted by the scroll.
     * @throws TimeoutException Exception thrown when an action times out.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionAndSucceed(UiAutomation uiAutomation, int direction,
            String scrollTargetText)
            throws TimeoutException {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final boolean[] returnValue = {false};
        AccessibilityEvent awaitedEvent = uiAutomation.executeAndWaitForEvent(
                () -> mActivityRule.runOnUiThread(() -> {
                    returnValue[0] =
                            mRecyclerView.getLayoutManager().performAccessibilityAction(
                                    ACTION_SCROLL_IN_DIRECTION.getId(),
                                    bundleWithDirectionArg(direction));
                }),
                event -> event.getEventType() == AccessibilityEvent.TYPE_VIEW_TARGETED_BY_SCROLL,
                DEFAULT_ACCESSIBILITY_EVENT_TIMEOUT_MILLIS);

        assertThat(scrollTargetText).isEqualTo(awaitedEvent.getSource().getText());
        assertThat(returnValue[0]).isTrue();
    }

    /**
     * Verifies that a scroll successfully occurs in the specified {@code direction} and that the
     * values of {@code mRowIndexForAccessibility} and {@code mColumnIndexForAccessibility} are
     * currectly set.
     *
     * @param uiAutomation  UiAutomation instance.
     * @param direction The direction of the scroll.
     * @param scrollTargetText The text of the view targeted by the scroll.
     * @param rowColumn The values for the row and column with accessibility focus.
     *
     * @throws TimeoutException Exception thrown when an action times out.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionAndSucceed(UiAutomation uiAutomation, int direction,
            String scrollTargetText, Pair<Integer, Integer> rowColumn)
            throws TimeoutException {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        runScrollInDirectionAndSucceed(uiAutomation, direction, scrollTargetText);
        assertThat(mGlm.mRowWithAccessibilityFocus).isEqualTo(rowColumn.first);
        assertThat(mGlm.mColumnWithAccessibilityFocus).isEqualTo(rowColumn.second);
    }

    /**
     * Verifies that a scroll does not occur in the specified {@code direction}.
     *
     * @param direction The direction of the scroll.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionAndFail(int direction) {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final boolean[] returnValue = {false};

        mActivityRule.runOnUiThread(
                () -> {
                    returnValue[0] = mRecyclerView.getLayoutManager().performAccessibilityAction(
                            ACTION_SCROLL_IN_DIRECTION.getId(), bundleWithDirectionArg(direction));
                });

        assertThat(returnValue[0]).isFalse();
    }

    /**
     * Verifies that a scroll does not occur in the specified {@code direction}.
     *
     * @param direction The direction of the scroll.
     * @param rowColumn The values for the row and column with accessibility focus.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionAndFail(int direction, Pair<Integer, Integer> rowColumn) {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final boolean[] returnValue = {false};

        mActivityRule.runOnUiThread(
                () -> {
                    returnValue[0] = mRecyclerView.getLayoutManager().performAccessibilityAction(
                            ACTION_SCROLL_IN_DIRECTION.getId(), bundleWithDirectionArg(direction));
                });

        assertThat(returnValue[0]).isFalse();
        assertThat(mGlm.mRowWithAccessibilityFocus).isEqualTo(rowColumn.first);
        assertThat(mGlm.mColumnWithAccessibilityFocus).isEqualTo(rowColumn.second);
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    private UiAutomation setUpGridLayoutManagerAccessibilityTest(int itemCount, int orientation)
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        setUpRecyclerViewAndGridLayoutManager(itemCount, orientation);
        waitForFirstLayout(mRecyclerView);
        return uiAutomation;
    }

    private Bundle bundleWithDirectionArg(int direction) {
        Bundle bundle = new Bundle();
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_DIRECTION_INT, direction);
        return bundle;
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private UiAutomation setUpAndReturnUiAutomation() {
        UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
        final AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        uiAutomation.setServiceInfo(info);
        return uiAutomation;
    }

    private void setAccessibilityFocus(UiAutomation uiAutomation, View source)
            throws TimeoutException {
        AccessibilityEvent awaitedEvent = null;
        awaitedEvent = uiAutomation.executeAndWaitForEvent(
                () -> {
                    try {
                        mActivityRule.runOnUiThread(() -> source.performAccessibilityAction(
                                ACTION_ACCESSIBILITY_FOCUS.getId(), null));
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                event -> event.getEventType()
                        == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
                DEFAULT_ACCESSIBILITY_EVENT_TIMEOUT_MILLIS);
        assertThat(awaitedEvent.getSource().isAccessibilityFocused()).isTrue();
    }

    private void setUpRecyclerViewAndGridLayoutManager(int itemCount, int orientation)
            throws Throwable {
        mRecyclerView = setupBasic(new Config(3, itemCount));
        mGlm.setOrientation(orientation);
    }
}
