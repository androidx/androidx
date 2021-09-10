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
package androidx.fragment.app;

import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.app.SharedElementCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains the Fragment Transition functionality.
 */
class FragmentTransition {
    static final FragmentTransitionImpl PLATFORM_IMPL = Build.VERSION.SDK_INT >= 21
            ? new FragmentTransitionCompat21()
            : null;

    static final FragmentTransitionImpl SUPPORT_IMPL = resolveSupportImpl();

    private static FragmentTransitionImpl resolveSupportImpl() {
        try {
            @SuppressWarnings("unchecked")
            Class<FragmentTransitionImpl> impl = (Class<FragmentTransitionImpl>) Class.forName(
                    "androidx.transition.FragmentTransitionSupport");
            return impl.getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
            // support-transition is not loaded; ignore
        }
        return null;
    }

    /**
     * Utility to find the String key in {@code map} that maps to {@code value}.
     */
    static String findKeyForValue(ArrayMap<String, String> map, String value) {
        final int numElements = map.size();
        for (int i = 0; i < numElements; i++) {
            if (value.equals(map.valueAt(i))) {
                return map.keyAt(i);
            }
        }
        return null;
    }

    /**
     * A utility to retain only the mappings in {@code nameOverrides} that have a value
     * that has a key in {@code namedViews}. This is a useful equivalent to
     * {@link ArrayMap#retainAll(Collection)} for values.
     */
    static void retainValues(@NonNull ArrayMap<String, String> nameOverrides,
            @NonNull ArrayMap<String, View> namedViews) {
        for (int i = nameOverrides.size() - 1; i >= 0; i--) {
            final String targetName = nameOverrides.valueAt(i);
            if (!namedViews.containsKey(targetName)) {
                nameOverrides.removeAt(i);
            }
        }
    }

    /**
     * Calls the {@link SharedElementCallback#onSharedElementStart(List, List, List)} or
     * {@link SharedElementCallback#onSharedElementEnd(List, List, List)} on the appropriate
     * incoming or outgoing fragment.
     *
     * @param inFragment The incoming fragment
     * @param outFragment The outgoing fragment
     * @param isPop Is the incoming fragment part of a pop transaction?
     * @param sharedElements The shared element Views
     * @param isStart Call the start or end call on the SharedElementCallback
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void callSharedElementStartEnd(Fragment inFragment, Fragment outFragment,
            boolean isPop, ArrayMap<String, View> sharedElements, boolean isStart) {
        SharedElementCallback sharedElementCallback = isPop
                ? outFragment.getEnterTransitionCallback()
                : inFragment.getEnterTransitionCallback();
        if (sharedElementCallback != null) {
            ArrayList<View> views = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();
            final int count = sharedElements == null ? 0 : sharedElements.size();
            for (int i = 0; i < count; i++) {
                names.add(sharedElements.keyAt(i));
                views.add(sharedElements.valueAt(i));
            }
            if (isStart) {
                sharedElementCallback.onSharedElementStart(names, views, null);
            } else {
                sharedElementCallback.onSharedElementEnd(names, views, null);
            }
        }
    }

    /**
     * Sets the visibility of all Views in {@code views} to {@code visibility}.
     */
    static void setViewVisibility(ArrayList<View> views, int visibility) {
        if (views == null) {
            return;
        }
        for (int i = views.size() - 1; i >= 0; i--) {
            final View view = views.get(i);
            view.setVisibility(visibility);
        }
    }

    static boolean supportsTransition() {
        return PLATFORM_IMPL != null || SUPPORT_IMPL != null;
    }

    private FragmentTransition() {
    }
}
