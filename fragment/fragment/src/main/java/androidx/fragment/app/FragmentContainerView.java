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

package androidx.fragment.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.animation.LayoutTransition;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * FragmentContainerView is a customized Layout designed specifically for Fragments. It extends
 * {@link FrameLayout}, so it can reliably handle Fragment Transactions, and it also has additional
 * features to coordinate with fragment behavior.
 *
 * <p>Layout animations and transitions are disabled for FragmentContainerView. Animations should be
 * done through {@link FragmentTransaction#setCustomAnimations(int, int, int, int)}. If
 * animateLayoutChanges is set to <code>true</code> or
 * {@link #setLayoutTransition(LayoutTransition)} is called directly an
 * {@link UnsupportedOperationException} will be thrown.
 *
 * @hide -- remove once z ordering is implemented.
 */
@RestrictTo(LIBRARY)
public final class FragmentContainerView extends FrameLayout {

    public FragmentContainerView(@NonNull Context context) {
        super(context);
    }

    public FragmentContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FragmentContainerView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * When called, this method throws a {@link UnsupportedOperationException}. This can be called
     * either explicitly, or implicitly by setting animateLayoutChanges to <code>true</code>.
     *
     * <p>View animations and transitions are disabled for FragmentContainerView. Use
     * {@link FragmentTransaction#setCustomAnimations(int, int, int, int)} and
     * {@link FragmentTransaction#setTransition(int)}.
     *
     * @param transition The LayoutTransition object that will animated changes in layout. A value
     * of <code>null</code> means no transition will run on layout changes.
     * @attr ref android.R.styleable#ViewGroup_animateLayoutChanges
     */
    @Override
    public void setLayoutTransition(@Nullable LayoutTransition transition) {
        throw new UnsupportedOperationException(
                "FragmentContainerView does not support Layout Transitions or "
                        + "animateLayoutChanges=\"true\".");
    }
}
