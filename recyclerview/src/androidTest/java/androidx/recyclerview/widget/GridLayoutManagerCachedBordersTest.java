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


import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.junit.Assert.assertEquals;

import android.support.test.filters.MediumTest;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(Parameterized.class)
public class GridLayoutManagerCachedBordersTest extends BaseGridLayoutManagerTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<Config> params() {
        List<Config> testConfigurations = createBaseVariations();
        testConfigurations.addAll(cachedBordersTestConfigs());
        return testConfigurations;
    }

    private final Config mConfig;

    public GridLayoutManagerCachedBordersTest(Config config) {
        mConfig = config;
    }


    @Test
    public void gridCachedBorderstTest() throws Throwable {
        RecyclerView recyclerView = setupBasic(mConfig);
        mGlm.expectLayout(1);
        setRecyclerView(recyclerView);
        mGlm.waitForLayout(10);
        final boolean vertical = mConfig.mOrientation == GridLayoutManager.VERTICAL;
        final int expectedSizeSum = vertical ? recyclerView.getWidth() : recyclerView.getHeight();
        final int lastVisible = mGlm.findLastVisibleItemPosition();
        for (int i = 0; i < lastVisible; i += mConfig.mSpanCount) {
            if ((i + 1) * mConfig.mSpanCount - 1 < lastVisible) {
                int childrenSizeSum = 0;
                for (int j = 0; j < mConfig.mSpanCount; j++) {
                    View child = recyclerView.getChildAt(i * mConfig.mSpanCount + j);
                    childrenSizeSum += vertical ? child.getWidth() : child.getHeight();
                }
                assertEquals(expectedSizeSum, childrenSizeSum);
            }
        }
    }

    private static List<Config> cachedBordersTestConfigs() {
        ArrayList<Config> configs = new ArrayList<>();
        final int[] spanCounts = new int[]{88, 279, 741};
        final int[] spanPerItem = new int[]{11, 9, 13};
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (int i = 0; i < spanCounts.length; i++) {
                    Config config = new Config(spanCounts[i], orientation, reverseLayout);
                    config.mSpanPerItem = spanPerItem[i];
                    configs.add(config);
                }
            }
        }
        return configs;
    }
}
