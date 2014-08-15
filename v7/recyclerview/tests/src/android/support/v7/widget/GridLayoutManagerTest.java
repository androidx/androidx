/*
 * Copyright (C) 2014 The Android Open Source Project
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

public class GridLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {
    public void testAnchorUpdate() throws InterruptedException {
        GridLayoutManager glm = new GridLayoutManager(getActivity(), 11);
        final GridLayoutManager.SpanSizeLookup spanSizeLookup
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 20) {
                    return 2;
                }
                return 1;
            }
        };
        glm.setSpanSizeLookup(spanSizeLookup);
        glm.mAnchorInfo.mPosition = 11;
        glm.onAnchorReady(glm.mAnchorInfo);
        assertEquals("gm should keep anchor in first span", 11, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 13;
        glm.onAnchorReady(glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 11, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 23;
        glm.onAnchorReady(glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 21, glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 35;
        glm.onAnchorReady(glm.mAnchorInfo);
        assertEquals("gm should move anchor to first span", 31, glm.mAnchorInfo.mPosition);
    }

    public void testSpanLookup() {
        final GridLayoutManager.SpanSizeLookup ssl
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 6) {
                    return 2;
                }
                return 1;
            }
        };
        assertEquals(0, ssl.getSpanIndex(0, 5));
        assertEquals(4, ssl.getSpanIndex(4, 5));
        assertEquals(0, ssl.getSpanIndex(5, 5));
        assertEquals(1, ssl.getSpanIndex(6, 5));
        assertEquals(2, ssl.getSpanIndex(7, 5));
        assertEquals(2, ssl.getSpanIndex(9, 5));
        assertEquals(0, ssl.getSpanIndex(8, 5));
    }
}
