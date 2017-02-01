/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import java.util.List;
import java.util.ArrayList;

import android.support.v17.leanback.widget.ParallaxSource.IntPropertyKeyValue;
import android.support.v17.leanback.widget.ParallaxSource.FloatPropertyKeyValue;
import android.support.v17.leanback.widget.ParallaxEffect.FloatEffect;
import android.support.v17.leanback.widget.ParallaxEffect.IntEffect;

/**
 * Parallax listens to {@link ParallaxSource} changes and invokes performMapping on each
 * {@link ParallaxEffect} object.
 * @hide
 */
public final class Parallax {

    private ParallaxSource mSource;
    private final List<ParallaxEffect> mEffects = new ArrayList<ParallaxEffect>(4);

    private final ParallaxSource.Listener mSourceListener = new ParallaxSource.Listener() {
        @Override
        public void onPropertiesChanged(ParallaxSource source) {
            for (int i = 0; i < mEffects.size(); i++) {
                mEffects.get(i).performMapping(source);
            }
        }
    };

    /**
     * Sets a {@link ParallaxSource} object and starts listening on it. Stops listening to the
     * previous {@link ParallaxSource} object if it exists.
     *
     * @param source New {@link ParallaxSource} object.
     */
    public void setSource(ParallaxSource source) {
        if (mSource != null) {
            mSource.setListener(null);
        }
        mSource = source;
        if (mSource != null) {
            mSource.setListener(mSourceListener);
        }
    }

    /**
     * Gets the current {@link ParallaxSource} object.
     *
     * @return The current {@link ParallaxSource} Object.
     */
    public ParallaxSource getSource() {
        return mSource;
    }

    /**
     * Adds a {@link ParallaxEffect} object which defines rules to perform mapping from
     * {@link ParallaxSource} to multiple {@link ParallaxTarget}s.
     *
     * @param effect A {@link ParallaxEffect} object.
     */
    public void addEffect(ParallaxEffect effect) {
        mEffects.add(effect);
    }

    /**
     * Returns a list of {@link ParallaxEffect} object which defines rules to perform mapping from
     * {@link ParallaxSource} to multiple {@link ParallaxTarget}s.
     *
     * @return A list of {@link ParallaxEffect} object.
     */
    public List<ParallaxEffect> getEffects() {
        return mEffects;
    }

    /**
     * Remove the {@link ParallaxEffect} object.
     *
     * @param effect The {@link ParallaxEffect} object to remove.
     */
    public void removeEffect(ParallaxEffect effect) {
        mEffects.remove(effect);
    }

    /**
     * Remove all {@link ParallaxEffect} objects.
     */
    public void removeAllEffects() {
        mEffects.clear();
    }

    /**
     * Create a {@link ParallaxEffect} object that will track source variable changes within a
     * provided set of ranges.
     *
     * @param ranges A list of key values that defines the ranges.
     * @return Newly created ParallaxEffect object.
     */
    public ParallaxEffect addEffect(IntPropertyKeyValue... ranges) {
        IntEffect effect = new IntEffect();
        effect.setPropertyRanges(ranges);
        addEffect(effect);
        return effect;
    }

    /**
     * Create a {@link ParallaxEffect} object that will track source variable changes within a
     * provided set of ranges.
     *
     * @param ranges A list of key values that defines the ranges.
     * @return Newly created ParallaxEffect object.
     */
    public ParallaxEffect addEffect(FloatPropertyKeyValue... ranges) {
        FloatEffect effect = new FloatEffect();
        effect.setPropertyRanges(ranges);
        addEffect(effect);
        return effect;
    }
}
