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

package androidx.loader.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.support.test.filters.SmallTest;

import androidx.loader.app.test.DummyLoader;
import androidx.loader.content.Loader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class LoaderViewModelTest {

    @Test
    public void testHasRunningLoaders() {
        LoaderManagerImpl.LoaderViewModel loaderViewModel = new LoaderManagerImpl.LoaderViewModel();
        assertFalse("LoaderViewModel should not be running with before putLoader",
                loaderViewModel.hasRunningLoaders());

        AlwaysRunningLoaderInfo info = new AlwaysRunningLoaderInfo(mock(Context.class));
        loaderViewModel.putLoader(0, info);
        assertTrue("LoaderViewModel should be running after a running LoaderInfo is added",
                loaderViewModel.hasRunningLoaders());

        loaderViewModel.removeLoader(0);
        assertFalse("LoaderViewModel should not be running after all LoaderInfos are removed",
                loaderViewModel.hasRunningLoaders());
    }

    @Test
    public void testOnCleared() {
        LoaderManagerImpl.LoaderViewModel loaderViewModel = new LoaderManagerImpl.LoaderViewModel();
        AlwaysRunningLoaderInfo info = new AlwaysRunningLoaderInfo(mock(Context.class));
        loaderViewModel.putLoader(0, info);

        assertFalse("LoaderInfo shouldn't be destroyed before onCleared", info.mDestroyed);
        loaderViewModel.onCleared();
        assertTrue("LoaderInfo should be destroyed after onCleared", info.mDestroyed);
        assertNull("LoaderInfo should be removed from LoaderViewModel after onCleared",
                loaderViewModel.getLoader(0));
    }

    private class AlwaysRunningLoaderInfo extends LoaderManagerImpl.LoaderInfo<Boolean> {
        boolean mDestroyed = false;

        AlwaysRunningLoaderInfo(Context context) {
            super(0, null, new DummyLoader(context), null);
        }

        @Override
        boolean isCallbackWaitingForData() {
            return true;
        }

        @Override
        Loader<Boolean> destroy(boolean reset) {
            mDestroyed = true;
            return super.destroy(reset);
        }
    }
}
