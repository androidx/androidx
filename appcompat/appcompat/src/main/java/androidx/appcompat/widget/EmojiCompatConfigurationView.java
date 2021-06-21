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

package androidx.appcompat.widget;

/**
 * Interface for Views that expose EmojiCompat configuration.
 */
public interface EmojiCompatConfigurationView {

    /**
     * Configure emoji fallback behavior using EmojiCompat.
     *
     * When enabled, this View will attempt to use EmojiCompat to enabled missing emojis.
     * When disabled, this View will not display missing emojis using EmojiCompat.
     *
     * EmojiCompat must be correctly configured on a device for this to have an effect, which
     * will happen by default if a correct downloadable fonts provider is installed on the device.
     *
     * If you manually configure EmojiCompat by calling EmojiCompat init after this View is
     * constructed, you may call this method again to enable EmojiCompat on this text view.
     *
     * For more information about EmojiCompat configuration see the emoji2 module.
     *
     * @param enabled if true, display missing emoji using EmojiCompat, otherwise display
     *                missing emoji using a fallback glyph "□" (known as tofu)
     */
    void setEmojiCompatEnabled(boolean enabled);

    /**
     * @return the current enabled state, set via
     * {@link EmojiCompatConfigurationView#setEmojiCompatEnabled(boolean)}
     */
    boolean isEmojiCompatEnabled();
}
