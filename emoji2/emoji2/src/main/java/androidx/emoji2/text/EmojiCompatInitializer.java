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

package androidx.emoji2.text;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;

/**
 * Initializer for configuring EmojiCompat with the system installed downloadable font provider.
 *
 * <p>This is the recommended configuration for all apps that don't need specialized configuration,
 * and don't need to control the thread that initialization runs on. For more information see
 * {@link androidx.emoji2.text.DefaultEmojiCompatConfig}.</p>
 *
 * <p>In addition to the reasons listed in {@code DefaultEmojiCompatConfig} you may wish to disable
 * this automatic configuration if you intend to call initialization from an existing background
 * thread pool in your application.</p>
 *
 * <p></p>This is enabled by default by including the `:emoji2:emoji2` gradle artifact. To disable
 * the default configuration (and allow manual configuration) add this to your manifest:</p>
 *
 * <pre>
 *     <provider
 *         android:name="androidx.startup.InitializationProvider"
 *         android:authorities="${applicationId}.androidx-startup"
 *         android:exported="false"
 *         tools:node="merge">
 *         <meta-data android:name="androidx.emoji2.text.EmojiCompatInitializer"
 *                   tools:node="remove" />
 *     </provider>
 * </pre>
 *
 * @see androidx.emoji2.text.DefaultEmojiCompatConfig
 */
public class EmojiCompatInitializer implements Initializer<Boolean> {

    /**
     * Initialize EmojiCompat with the app's context.
     *
     * @param context application context
     * @return result of default init
     */
    @NonNull
    @Override
    public Boolean create(@NonNull Context context) {
        // note: super create requires this be non-null, share if the configuration was successful
        return EmojiCompat.init(context) != null;
    }

    /**
     * No dependencies
     */
    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
