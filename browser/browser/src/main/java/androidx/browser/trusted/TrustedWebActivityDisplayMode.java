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

package androidx.browser.trusted;

import android.os.Bundle;

import org.jspecify.annotations.NonNull;

/**
 * Represents display mode of a Trusted Web Activity.
 */
public interface TrustedWebActivityDisplayMode {

    /** Bundle key for the integer identifying the mode. */
    String KEY_ID = "androidx.browser.trusted.displaymode.KEY_ID";

    /** Unpacks the object from a {@link Bundle}. */
    static @NonNull TrustedWebActivityDisplayMode fromBundle(@NonNull Bundle bundle) {
        switch (bundle.getInt(KEY_ID)) {
            case ImmersiveMode.ID:
                return ImmersiveMode.fromBundle(bundle);
            case BrowserMode.ID:
                return new BrowserMode();
            case MinimalUiMode.ID:
                return new MinimalUiMode();
            case TabbedMode.ID:
                return new TabbedMode();
            case WindowControlsOverlayMode.ID:
                return new WindowControlsOverlayMode();
            case DefaultMode.ID: // fallthrough
            default:
                return new DefaultMode();
        }
    }

    /** Packs the object into a {@link Bundle}. */
    @NonNull Bundle toBundle();

    /**
     * Default mode: the system UI (status bar, navigation bar) is shown, and the browser
     * toolbar is hidden while the user is on a verified origin.
     */
    class DefaultMode implements TrustedWebActivityDisplayMode {
        private static final int ID = 0;

        @Override
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, ID);
            return bundle;
        }
    }

    /**
     * Immersive mode: both the browser controls and the system UI (status bar, navigation bar) is
     * hidden while the user is on a verified origin.
     *
     * See https://developer.android.com/training/system-ui/immersive
     *
     * Using this mode is different from using the JavaScript Fullscreen API in that the immersive
     * mode will be entered immediately, before the page has loaded. If the app uses a splash
     * screen, it will be displayed in immersive mode. It is not recommended to use both this API
     * and the JavaScript API simultaneously.
     */
    class ImmersiveMode implements TrustedWebActivityDisplayMode {
        private static final int ID = 1;

        /** Bundle key for {@link #isSticky}. */
        public static final String KEY_STICKY = "androidx.browser.trusted.displaymode.KEY_STICKY";

        /** Bundle key for {@link #layoutInDisplayCutoutMode}. */
        public static final String KEY_CUTOUT_MODE =
                "androidx.browser.trusted.displaymode.KEY_CUTOUT_MODE";

        private final boolean mIsSticky;

        private final int mLayoutInDisplayCutoutMode;

        /**
         * Constructor.
         * @param isSticky Whether the Trusted Web Activity should be in sticky immersive mode, see
         *                 https://developer.android.com/training/system-ui/immersive#sticky-immersive
         *
         * @param layoutInDisplayCutoutMode The constant defining how to deal with display cutouts.
         *             See {@link android.view.WindowManager.LayoutParams#layoutInDisplayCutoutMode}
         *             and https://developer.android.com/guide/topics/display-cutout
         */
        public ImmersiveMode(boolean isSticky, int layoutInDisplayCutoutMode) {
            this.mIsSticky = isSticky;
            this.mLayoutInDisplayCutoutMode = layoutInDisplayCutoutMode;
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        static @NonNull TrustedWebActivityDisplayMode fromBundle(@NonNull Bundle bundle) {
            return new ImmersiveMode(bundle.getBoolean(KEY_STICKY),
                    bundle.getInt(KEY_CUTOUT_MODE));
        }

        @Override
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, ID);
            bundle.putBoolean(KEY_STICKY, mIsSticky);
            bundle.putInt(KEY_CUTOUT_MODE, mLayoutInDisplayCutoutMode);
            return bundle;
        }

        /** Returns whether sticky immersive mode is enabled. */
        public boolean isSticky() {
            return mIsSticky;
        }

        /** Returns the cutout mode. */
        public int layoutInDisplayCutoutMode() {
            return mLayoutInDisplayCutoutMode;
        }
    }


    /**
     * Browser Mode: Lets the browser determine the display mode - this may be a Trusted Web
     * Activity or a browser tab.
     *
     * {@see https://www.w3.org/TR/appmanifest/#dfn-browser}
     */
    class BrowserMode implements TrustedWebActivityDisplayMode {

        private static final int ID = 2;

        @Override
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, ID);
            return bundle;
        }
    }

    /**
     * Minimal UI Mode: Hides away most controls that aren't essential for navigation like Back
     * or Reload, and hides the toolbar if possible.
     *
     * {@see https://www.w3.org/TR/appmanifest/#dfn-minimal-ui}
     */
    class MinimalUiMode implements TrustedWebActivityDisplayMode {

        private static final int ID = 3;

        @Override
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, ID);
            return bundle;
        }
    }

    /**
     * Tabbed mode: Experimental display mode where the application can contain multiple web
     * pages inside a single window, commonly used to create a tab bar UI.
     *
     * {@see https://github.com/WICG/manifest-incubations/blob/gh-pages/tabbed-mode-explainer.md}
     * {@see https://wicg.github.io/manifest-incubations/#tab_strip-member}
     */
    final class TabbedMode implements TrustedWebActivityDisplayMode {

        private static final int ID = 4;

        @Override
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, ID);
            return bundle;
        }
    }

    /**
     * Window controls overlay mode: Experimental display mode where the entire window surface
     * is be available for the app's web content. System controls (maximize, minimize, close, etc)
     * will be displayed as overlays on top of the web contents.
     *
     * This is not the same as immersive mode because the web activity can be in a free-floating
     * window with window controls overlay enabled. In immersive mode, the web activity will
     * occupy the entire screen.
     *
     * {@see https://github.com/WICG/window-controls-overlay/blob/main/explainer.md}
     * {@see https://wicg.github.io/window-controls-overlay/}
     */
    final class WindowControlsOverlayMode implements TrustedWebActivityDisplayMode {

        private static final int ID = 5;

        @Override
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, ID);
            return bundle;
        }
    }
}
