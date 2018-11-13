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

import androidx.annotation.NonNull;
import androidx.benchmark.BenchmarkRule;
import androidx.benchmark.BenchmarkState;
import androidx.slice.widget.SliceView;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@RunWith(Parameterized.class)
@MediumTest
@SdkSuppress(minSdkVersion = 19)
public class SliceViewMetrics {

    private final int mMode;

    @Parameterized.Parameters
    public static Iterable<? extends Object[]> data() {
        return Arrays.asList(new Object[][]{{SliceView.MODE_SHORTCUT}, {SliceView.MODE_SMALL},
                {SliceView.MODE_LARGE}});
    }

    public SliceViewMetrics(int mode) {
        mMode = mode;
    }

    @Rule
    public BenchmarkRule mBenchmarkRule = new BenchmarkRule() {
        @NonNull
        @Override
        public Statement apply(@NonNull Statement base, @NonNull Description description) {
            return super.apply(base, fixDescription(description));
        }

        private Description fixDescription(Description description) {
            // Copies the Description and modifies the method to be compatible with BenchmarkRule.
            return Description.createTestDescription(description.getClassName(),
                    fixMethodName(description.getMethodName()),
                    description.getAnnotations().toArray(new Annotation[0]));
        }

        private String fixMethodName(String methodName) {
            // Replace [int] with [string] for BenchmarkRule and readability.
            return methodName.replace("[0]", "[shortcut]")
                    .replace("[1]", "[small]")
                    .replace("[2]", "[large]");
        }
    };

    private final Context mContext = InstrumentationRegistry.getContext();

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
