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

package androidx.viewpager2.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2.PageTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows for combining multiple {@link PageTransformer} objects.
 *
 * @see ViewPager2#setPageTransformer
 * @see MarginPageTransformer
 */
public final class CompositePageTransformer implements PageTransformer {
    private final List<PageTransformer> mTransformers = new ArrayList<>();

    /**
     * Adds a page transformer to the list.
     * <p>
     * Transformers will be executed in the order that they were added.
     */
    public void addTransformer(@NonNull PageTransformer transformer) {
        mTransformers.add(transformer);
    }

    /** Removes a page transformer from the list. */
    public void removeTransformer(@NonNull PageTransformer transformer) {
        mTransformers.remove(transformer);
    }

    @Override
    public void transformPage(@NonNull View page, float position) {
        for (PageTransformer transformer : mTransformers) {
            transformer.transformPage(page, position);
        }
    }
}
