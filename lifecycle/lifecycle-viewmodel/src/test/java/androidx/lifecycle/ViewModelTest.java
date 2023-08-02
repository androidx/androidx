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

package androidx.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.Closeable;

@RunWith(JUnit4.class)
public class ViewModelTest {

    static class CloseableImpl implements Closeable {
        boolean mWasClosed;
        @Override
        public void close() {
            mWasClosed = true;
        }
    }

    static class ViewModel extends androidx.lifecycle.ViewModel {
    }

    static class ConstructorArgViewModel extends androidx.lifecycle.ViewModel {
        ConstructorArgViewModel(@NonNull Closeable closeable) {
            super(closeable);
        }
    }

    @Test
    public void testCloseableTag() {
        ViewModel vm = new ViewModel();
        CloseableImpl impl = new CloseableImpl();
        vm.setTagIfAbsent("totally_not_coroutine_context", impl);
        vm.clear();
        assertTrue(impl.mWasClosed);
    }

    @Test
    public void testCloseableTagAlreadyClearedVM() {
        ViewModel vm = new ViewModel();
        vm.clear();
        CloseableImpl impl = new CloseableImpl();
        vm.setTagIfAbsent("key", impl);
        assertTrue(impl.mWasClosed);

    }

    @Test
    public void testAlreadyAssociatedKey() {
        ViewModel vm = new ViewModel();
        assertThat(vm.setTagIfAbsent("key", "first"), is("first"));
        assertThat(vm.setTagIfAbsent("key", "second"), is("first"));
    }

    @Test
    public void testMockedGetTag() {
        ViewModel vm = Mockito.mock(ViewModel.class);
        assertThat(vm.getTag("Careless mocks =|"), nullValue());
    }

    @Test
    public void testAddCloseable() {
        ViewModel vm = new ViewModel();
        CloseableImpl impl = new CloseableImpl();
        vm.addCloseable(impl);
        vm.clear();
        assertTrue(impl.mWasClosed);
    }

    @Test
    public void testConstructorCloseable() {
        CloseableImpl impl = new CloseableImpl();
        ConstructorArgViewModel vm = new ConstructorArgViewModel(impl);
        vm.clear();
        assertTrue(impl.mWasClosed);
    }

    @Test
    public void testMockedAddCloseable() {
        ViewModel vm = Mockito.mock(ViewModel.class);
        CloseableImpl impl = new CloseableImpl();
        // This shouldn't crash, even on a mocked object
        vm.addCloseable(impl);
    }
}
