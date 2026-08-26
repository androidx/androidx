/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.mediarouter.media;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Test for {@link SelectionInfo}. */
@RunWith(AndroidJUnit4.class)
public class SelectionInfoTest {

    @Test
    @SmallTest
    public void defaultValues() {
        SelectionInfo.Builder builder = new SelectionInfo.Builder();

        SelectionInfo info = builder.build();

        assertEquals(MediaRouter.UNSELECT_REASON_UNKNOWN, info.getUnselectReason());
        assertEquals(SelectionInfo.SELECTION_SOURCE_UNKNOWN, info.getSelectionSource());
    }

    @Test
    @SmallTest
    public void builderSetters() {
        int unselectReason = MediaRouter.UNSELECT_REASON_DISCONNECTED;
        int selectionSource = SelectionInfo.SELECTION_SOURCE_SYSTEM;
        SelectionInfo.Builder builder =
                new SelectionInfo.Builder()
                        .setUnselectReason(unselectReason)
                        .setSelectionSource(selectionSource);

        SelectionInfo info = builder.build();

        assertEquals(unselectReason, info.getUnselectReason());
        assertEquals(selectionSource, info.getSelectionSource());
    }
}
