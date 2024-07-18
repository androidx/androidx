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

package androidx.media2.widget;

import android.view.accessibility.CaptioningManager;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

import java.util.Locale;

final class CaptioningManagerHelper {

    @RequiresApi(19)
    static final class Api19Impl {

        @DoNotInline
        static void addCaptioningChangeListener(CaptioningManager manager,
                CaptioningManager.CaptioningChangeListener listener) {
            manager.addCaptioningChangeListener(listener);
        }

        @DoNotInline
        static void removeCaptioningChangeListener(CaptioningManager manager,
                CaptioningManager.CaptioningChangeListener listener) {
            manager.removeCaptioningChangeListener(listener);
        }

        @DoNotInline
        static float getFontScale(CaptioningManager manager) {
            return manager.getFontScale();
        }

        @DoNotInline
        static Locale getLocale(CaptioningManager manager) {
            return manager.getLocale();
        }

        @DoNotInline
        static CaptioningManager.CaptionStyle getUserStyle(CaptioningManager manager) {
            return manager.getUserStyle();
        }

        @DoNotInline
        static boolean isEnabled(CaptioningManager manager) {
            return manager.isEnabled();
        }

        private Api19Impl() {}
    }

    private CaptioningManagerHelper() {}
}
