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

package android.arch.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ViewModelStoreTest {

    @Test
    public void testClear() {
        ViewModelStore store = new ViewModelStore();
        TestViewModel viewModel1 = new TestViewModel();
        TestViewModel viewModel2 = new TestViewModel();
        store.put("a", viewModel1);
        store.put("b", viewModel2);
        assertThat(viewModel1.mCleared, is(false));
        assertThat(viewModel2.mCleared, is(false));
        store.clear();
        assertThat(viewModel1.mCleared, is(true));
        assertThat(viewModel2.mCleared, is(true));
        assertThat(store.get("a"), nullValue());
        assertThat(store.get("b"), nullValue());
    }

    static class TestViewModel extends ViewModel {
        boolean mCleared = false;

        @Override
        protected void onCleared() {
            mCleared = true;
        }
    }
}
