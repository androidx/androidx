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

package android.arch.paging;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.support.test.filters.SmallTest;
import android.support.v7.recyclerview.extensions.ListAdapterConfig;
import android.support.v7.util.ListUpdateCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SmallTest
@RunWith(JUnit4.class)
public class PagedListAdapterHelperTest {
    @Test
    public void simpleStatic() {
        ListUpdateCallback callback = mock(ListUpdateCallback.class);
        PagedListAdapterHelper<String> helper = new PagedListAdapterHelper<>(callback,
                new ListAdapterConfig.Builder<String>()
                        .setDiffCallback(StringPagedList.DIFF_CALLBACK)
                        .build());

        assertEquals(0, helper.getItemCount());

        helper.setList(new StringPagedList(2, 2, "a", "b"));

        verify(callback).onInserted(0, 6);
        verifyNoMoreInteractions(callback);
        assertEquals(6, helper.getItemCount());

        assertNull(helper.getItem(0));
        assertNull(helper.getItem(1));
        assertEquals("a", helper.getItem(2));
        assertEquals("b", helper.getItem(3));
        assertNull(helper.getItem(4));
        assertNull(helper.getItem(5));
    }
}
