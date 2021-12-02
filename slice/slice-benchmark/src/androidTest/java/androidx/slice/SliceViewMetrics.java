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

package androidx.slice;


import android.content.Context;
import android.net.Uri;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.slice.widget.SliceView;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
@SmallTest
@SdkSuppress(minSdkVersion = 19)
public class SliceViewMetrics {

    private final int mMode;

    @Parameterized.Parameters(name = "{1}")
    public static Iterable<? extends Object[]> data() {
        return Arrays.asList(new Object[][]{
                {SliceView.MODE_SHORTCUT, "shortcut"},
                {SliceView.MODE_SMALL, "small"},
                {SliceView.MODE_LARGE, "large"}
        });
    }

    public SliceViewMetrics(int mode, @SuppressWarnings("unused") String ignored) {
        mMode = mode;
    }

    @Rule
    public BenchmarkRule mBenchmarkRule = new BenchmarkRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testCreate() {
        // Since using parameterized, UiThreadTest isn't supported
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final BenchmarkState state = mBenchmarkRule.getState();
                while (state.keepRunning()) {
                    new SliceView(mContext).setMode(mMode);
                }
            }
        });
    }

    @Test
    @UiThreadTest
    public void testCreateAndSet() {
        // Since using parameterized, UiThreadTest isn't supported
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final BenchmarkState state = mBenchmarkRule.getState();
                Uri uri = Uri.parse("content:///androidx.slice.benchmark");
                Slice s = SliceSerializeMetrics.createSlice(mContext, uri, 3, 3, 6);
                while (state.keepRunning()) {
                    SliceView v = new SliceView(mContext);
                    v.setMode(mMode);
                    v.setSlice(s);
                }
            }
        });
    }

    @Test
    @UiThreadTest
    public void testSet() {
        // Since using parameterized, UiThreadTest isn't supported
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final BenchmarkState state = mBenchmarkRule.getState();
                Uri uri = Uri.parse("content:///androidx.slice.benchmark");
                Slice s = SliceSerializeMetrics.createSlice(mContext, uri, 3, 3, 6);
                SliceView v = new SliceView(mContext);
                v.setMode(mMode);
                v.setSlice(s);
                while (state.keepRunning()) {
                    v.setSlice(s);
                }
            }
        });
    }
}
