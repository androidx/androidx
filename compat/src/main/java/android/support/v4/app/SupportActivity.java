/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.app;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ReportFragment;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.util.SimpleArrayMap;

/**
 * Base class for composing together compatibility functionality
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SupportActivity extends Activity implements LifecycleOwner {
    /**
     * Storage for {@link ExtraData} instances.
     *
     * <p>Note that these objects are not retained across configuration changes</p>
     */
    private SimpleArrayMap<Class<? extends ExtraData>, ExtraData> mExtraDataMap =
            new SimpleArrayMap<>();

    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    /**
     * Store an instance of {@link ExtraData} for later retrieval by class name
     * via {@link #getExtraData}.
     *
     * <p>Note that these objects are not retained across configuration changes</p>
     *
     * @see #getExtraData
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void putExtraData(ExtraData extraData) {
        mExtraDataMap.put(extraData.getClass(), extraData);
    }

    @Override
    @SuppressWarnings("RestrictedApi")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReportFragment.injectIfNeededIn(this);
    }

    @CallSuper
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mLifecycleRegistry.markState(Lifecycle.State.CREATED);
        super.onSaveInstanceState(outState);
    }

    /**
     * Retrieves a previously set {@link ExtraData} by class name.
     *
     * @see #putExtraData
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public <T extends ExtraData> T getExtraData(Class<T> extraDataClass) {
        return (T) mExtraDataMap.get(extraDataClass);
    }

    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class ExtraData {
    }
}
