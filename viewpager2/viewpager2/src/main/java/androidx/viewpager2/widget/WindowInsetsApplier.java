/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * An {@link OnApplyWindowInsetsListener} that applies {@link WindowInsetsCompat WindowInsets} to
 * all children of a {@link ViewPager2}, making sure they all receive the same insets regardless
 * of whether any of them consumed any insets.
 *
 * <p>To prevent the ViewPager2 itself from dispatching the insets incorrectly, this listener will
 * consume all insets it applies. As a consequence, siblings of ViewPager2, or siblings of its
 * parents, to whom the WindowInsets haven't yet been dispatched, won't receive them at all. If
 * you require those views to receive the WindowInsets, do not set this listener on ViewPager2
 * and do not consume insets in any of the pages.
 *
 * <p>Call {@link #install(ViewPager2)} to install this listener in ViewPager2.
 *
 * <p>When running on API 30 or higher and the targetSdkVersion is set to API 30 or higher, the
 * fix is not needed and {@link #install(ViewPager2)} will do nothing. None of the above described
 * effects will happen.
 */
public final class WindowInsetsApplier implements OnApplyWindowInsetsListener {
    private WindowInsetsApplier() {
        // private constructor, only we get to instantiate this fix to ensure type safety.
    }

    /**
     * Installs a {@link WindowInsetsApplier} into the given ViewPager2, but only when window
     * insets dispatching hasn't been fixed in the current run configuration. It will return
     * whether or not the WindowInsetsApplier was installed.
     *
     * <p>Window insets dispatching is fixed on Android SDK R, but the targetSdk of the app also
     * needs to be set to R or higher. If both these conditions hold, the WindowInsetsApplier
     * won't be installed. If either we're running on SDK < R, or the targetSdk of the app is set
     * to < R, then the WindowInsetsApplier will be installed.
     *
     * @param viewPager The ViewPager2 to install the WindowInsetsApplier into
     * @return Whether or not the WindowInsetsApplier was installed
     */
    public static boolean install(@NonNull ViewPager2 viewPager) {
        // From R onwards, insets dispatching has been fixed in the framework, but only if the
        // targetSdk is R or later. If we're running on a fixed version (SDK >= R && target >= R),
        // don't install our fix.
        ApplicationInfo appInfo = viewPager.getContext().getApplicationInfo();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && appInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            return false;
        }
        // We're not running on a fixed version. Apply the fix.
        ViewCompat.setOnApplyWindowInsetsListener(viewPager, new WindowInsetsApplier());
        return true;
    }

    @NonNull
    @Override
    public WindowInsetsCompat onApplyWindowInsets(@NonNull View v,
            @NonNull WindowInsetsCompat insets) {
        ViewPager2 viewPager = (ViewPager2) v;

        // First let the ViewPager2 itself try and consume them...
        final WindowInsetsCompat applied = ViewCompat.onApplyWindowInsets(viewPager, insets);

        if (applied.isConsumed()) {
            // If the ViewPager2 consumed all insets, return now
            return applied;
        }

        // Now we'll manually dispatch the insets to our children. Since ViewPager2
        // children are always full-height, we do not want to use the standard
        // ViewGroup dispatchApplyWindowInsets since if child 0 consumes them, the
        // rest of the children will not receive any insets. To workaround this we
        // manually dispatch the applied insets, not allowing children to consume
        // them from each other, making a copy for every invocation

        final RecyclerView rv = viewPager.mRecyclerView;
        for (int i = 0, count = rv.getChildCount(); i < count; i++) {
            // We don't care about b/168984101 here, as we're not using the return value
            ViewCompat.dispatchApplyWindowInsets(rv.getChildAt(i), new WindowInsetsCompat(applied));
        }

        // Now return a new WindowInsets where we consume all insets to prevent the
        // platform from dispatching the insets to ViewPager2's children, because the platform's
        // dispatch is broken (it will leak insets consumed by one child to other children).
        // There is a trade off here: by consuming all insets, we fix insets dispatching for our
        // children, but we break it for siblings. By not consuming all insets, it would work for
        // siblings but we break it for children.
        return consumeAllInsets(applied);
    }

    @SuppressWarnings("deprecation") // consumeSystemWindowInsets, consumeStableInsets
    private WindowInsetsCompat consumeAllInsets(@NonNull WindowInsetsCompat insets) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (WindowInsetsCompat.CONSUMED.toWindowInsets() != null) {
                return WindowInsetsCompat.CONSUMED;
            }
            // On API < 29, WindowInsetsCompat.CONSUMED can fail initialization because it uses
            // reflection to create the CONSUMED WindowInsets.
            // When that happens, fall back to consuming everything in the given insets. The given
            // insets is guaranteed to hold non-null WindowInsets because those were created by the
            // platform. We only have to consume insets that were in API < 29.
            return insets.consumeSystemWindowInsets().consumeStableInsets();
        }
        return insets;
    }
}
