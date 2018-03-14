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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.support.test.filters.SmallTest;

import androidx.loader.app.test.DummyLoaderCallbacks;
import androidx.loader.content.Loader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class LoaderObserverTest {

    @Test
    public void testOnChanged() {
        DummyLoaderCallbacks callback = new DummyLoaderCallbacks(mock(Context.class));
        @SuppressWarnings("unchecked")
        LoaderManagerImpl.LoaderObserver<Boolean> observer = new LoaderManagerImpl.LoaderObserver<>(
                mock(Loader.class), callback);
        assertFalse("LoaderObserver should not have delivered data before onChanged",
                observer.hasDeliveredData());
        assertFalse("onLoadFinished should not be called before onChanged",
                callback.mOnLoadFinished);


        observer.onChanged(true);
        assertTrue("LoaderObserver should have delivered data after onChanged",
                observer.hasDeliveredData());
        assertTrue("onLoadFinished should be called after onChanged",
                callback.mOnLoadFinished);
    }

    @Test
    public void testReset() {
        DummyLoaderCallbacks callback = new DummyLoaderCallbacks(mock(Context.class));
        @SuppressWarnings("unchecked")
        LoaderManagerImpl.LoaderObserver<Boolean> observer = new LoaderManagerImpl.LoaderObserver<>(
                mock(Loader.class), callback);
        assertFalse("onLoaderReset shouldn't be called before onChanged+reset",
                callback.mOnLoaderReset);

        observer.reset();
        assertFalse("onLoaderReset should not be called after only reset",
                callback.mOnLoaderReset);
    }

    @Test
    public void testResetWithOnChanged() {
        DummyLoaderCallbacks callback = new DummyLoaderCallbacks(mock(Context.class));
        @SuppressWarnings("unchecked")
        LoaderManagerImpl.LoaderObserver<Boolean> observer = new LoaderManagerImpl.LoaderObserver<>(
                mock(Loader.class), callback);
        assertFalse("onLoaderReset shouldn't be called before onChanged+reset",
                callback.mOnLoaderReset);

        observer.onChanged(true);
        observer.reset();
        assertTrue("onLoaderReset should be called after onChanged+reset",
                callback.mOnLoaderReset);
    }
}
