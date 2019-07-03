/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.recyclerview.selection;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class StableIdKeyProviderTest {

    private Context mContext;
    private RecyclerView mRecyclerView;
    private StableIdKeyProvider mKeyProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mRecyclerView = new RecyclerView(mContext);
        mKeyProvider = new StableIdKeyProvider(mRecyclerView);
    }

    @Test
    public void testOnAttached_NullViewHolder() {
        View v = new View(mContext);
        mKeyProvider.onAttached(v);
    }

    @Test
    public void testOnDetatched_NullViewHolder() {
        View v = new View(mContext);
        mKeyProvider.onDetached(v);
    }
}
