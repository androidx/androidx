/**
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.content.pm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ShortcutManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

@RequiresApi(26)
class ShortcutManagerCompatApi26 {

    public static boolean isRequestPinShortcutSupported(Context context) {
        return context.getSystemService(ShortcutManager.class).isRequestPinShortcutSupported();
    }

    public static boolean requestPinShortcut(final Context context,
            @NonNull ShortcutInfoCompat shortcut, @Nullable final IntentSender callback) {
        return context.getSystemService(ShortcutManager.class).requestPinShortcut(
                shortcut.toShortcutInfo(), callback);
    }

    @Nullable
    public static Intent createShortcutResultIntent(Context context,
            @NonNull ShortcutInfoCompat shortcut) {
        return context.getSystemService(ShortcutManager.class)
                .createShortcutResultIntent(shortcut.toShortcutInfo());
    }
}
