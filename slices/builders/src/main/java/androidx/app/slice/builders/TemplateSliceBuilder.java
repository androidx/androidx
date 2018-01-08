/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.app.slice.builders;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.util.Pair;

import java.util.Arrays;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceProvider;
import androidx.app.slice.SliceSpec;
import androidx.app.slice.SliceSpecs;
import androidx.app.slice.builders.impl.TemplateBuilderImpl;

/**
 * Base class of builders of various template types.
 */
public abstract class TemplateSliceBuilder {

    private static final String TAG = "TemplateSliceBuilder";

    private final Slice.Builder mBuilder;
    private final Context mContext;
    private final TemplateBuilderImpl mImpl;
    private List<SliceSpec> mSpecs;

    /**
     */
    public TemplateSliceBuilder(Uri uri) {
        this(null, uri);
        throw new RuntimeException("Stub, to be removed");
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    protected TemplateSliceBuilder(TemplateBuilderImpl impl) {
        mContext = null;
        mBuilder = null;
        mImpl = impl;
        setImpl(impl);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    protected TemplateSliceBuilder(Slice.Builder b, Context context) {
        mBuilder = b;
        mContext = context;
        mSpecs = getSpecs();
        mImpl = selectImpl();
        if (mImpl == null) {
            throw new IllegalArgumentException("No valid specs found");
        }
        setImpl(mImpl);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public TemplateSliceBuilder(Context context, Uri uri) {
        mBuilder = new Slice.Builder(uri);
        mContext = context;
        mSpecs = getSpecs();
        mImpl = selectImpl();
        if (mImpl == null) {
            throw new IllegalArgumentException("No valid specs found");
        }
        setImpl(mImpl);
    }

    /**
     * Construct the slice.
     */
    public Slice build() {
        return mImpl.build();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    protected Slice.Builder getBuilder() {
        return mBuilder;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    abstract void setImpl(TemplateBuilderImpl impl);

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    protected TemplateBuilderImpl selectImpl() {
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    protected boolean checkCompatible(SliceSpec candidate) {
        final int size = mSpecs.size();
        for (int i = 0; i < size; i++) {
            if (mSpecs.get(i).canRender(candidate)) {
                return true;
            }
        }
        return false;
    }

    private List<SliceSpec> getSpecs() {
        if (SliceProvider.getCurrentSpecs() != null) {
            return SliceProvider.getCurrentSpecs();
        }
        // TODO: Support getting specs from pinned info.
        Log.w(TAG, "Not currently bunding a slice");
        return Arrays.asList(SliceSpecs.BASIC);
    }

    /**
     * This is for typing, to clean up the code.
     * @hide
     */
    @RestrictTo(LIBRARY)
    static <T> Pair<SliceSpec, Class<? extends TemplateBuilderImpl>> pair(SliceSpec spec,
            Class<T> cls) {
        return new Pair(spec, cls);
    }
}
