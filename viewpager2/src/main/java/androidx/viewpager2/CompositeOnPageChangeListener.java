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

package androidx.viewpager2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeListener;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Dispatches {@link OnPageChangeListener} events to subscribers.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class CompositeOnPageChangeListener implements OnPageChangeListener {

    @NonNull
    private final List<OnPageChangeListener> mListeners;

    public CompositeOnPageChangeListener(int initialCapacity) {
        mListeners = new ArrayList<>(initialCapacity);
    }

    /**
     * Adds the given listener to the list of subscribers
     */
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes the given listener from the list of subscribers
     */
    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Removes all listeners from the list of subscribers
     */
    public void clearOnPageChangeListeners() {
        mListeners.clear();
    }

    /**
     * @see OnPageChangeListener#onPageScrolled(int, float, int)
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, @Px int positionOffsetPixels) {
        try {
            for (OnPageChangeListener listener : mListeners) {
                listener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        } catch (ConcurrentModificationException ex) {
            throwListenerListModifiedWhileInUse(ex);
        }
    }

    /**
     * @see OnPageChangeListener#onPageSelected(int)
     */
    @Override
    public void onPageSelected(int position) {
        try {
            for (OnPageChangeListener listener : mListeners) {
                listener.onPageSelected(position);
            }
        } catch (ConcurrentModificationException ex) {
            throwListenerListModifiedWhileInUse(ex);
        }
    }

    /**
     * @see OnPageChangeListener#onPageScrollStateChanged(int)
     */
    @Override
    public void onPageScrollStateChanged(@ViewPager2.ScrollState int state) {
        try {
            for (OnPageChangeListener listener : mListeners) {
                listener.onPageScrollStateChanged(state);
            }
        } catch (ConcurrentModificationException ex) {
            throwListenerListModifiedWhileInUse(ex);
        }
    }

    private void throwListenerListModifiedWhileInUse(ConcurrentModificationException parent) {
        throw new IllegalStateException(
                "Adding and removing listeners during dispatch to listeners is not supported",
                parent
        );
    }

}
