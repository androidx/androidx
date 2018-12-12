/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.sharetarget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides direct share items to the system, by cross checking dynamic shortcuts from
 * ShortcutManagerCompat and share target definitions from a Xml resource. Used for backward
 * compatibility to push share targets to shortcut manager on older SDKs.
 *
 * @hide
 */
@RequiresApi(23)
@RestrictTo(LIBRARY_GROUP)
public class ChooserTargetServiceCompat extends ChooserTargetService {

    static final String TAG = "ChooserServiceCompat";

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
            IntentFilter matchedFilter) {
        Context context = getApplicationContext();
        ArrayList<ChooserTarget> chooserTargets = new ArrayList<>();

        // Retrieve share targets
        List<ShareTargetCompat> targets = ShareTargetXmlParser.getShareTargets(context);
        List<ShareTargetCompat> matchedTargets = new ArrayList<>();
        for (ShareTargetCompat target : targets) {
            for (ShareTargetCompat.TargetData data : target.mTargetData) {
                if (matchedFilter.hasDataType(data.mMimeType)) {
                    // Matched at least with one data type (OR operation)
                    matchedTargets.add(target);
                    break;
                }
            }
        }
        if (matchedTargets.isEmpty()) {
            return chooserTargets;
        }

        // Retrieve shortcuts
        ShortcutInfoCompatSaverImpl shortcutSaver = ShortcutInfoCompatSaverImpl.getInstance(
                context);
        List<ShortcutInfoCompat> shortcuts;
        try {
            shortcuts = shortcutSaver.getShortcuts();
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve shortcuts: ", e);
            return chooserTargets;
        }
        if (shortcuts == null || shortcuts.isEmpty()) {
            return chooserTargets;
        }

        for (ShortcutInfoCompat shortcut : shortcuts) {
            ShareTargetCompat target = null;
            for (ShareTargetCompat item : matchedTargets) {
                // Shortcut must have all share target categories (AND operation)
                if (shortcut.getCategories().containsAll(Arrays.asList(item.mCategories))) {
                    target = item;
                    break;
                }
            }
            if (target == null) {
                continue;
            }

            IconCompat icon;
            try {
                icon = shortcutSaver.getShortcutIcon(shortcut.getId());
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve shortcut icon: ", e);
                continue;
            }
            Bundle extras = new Bundle();
            extras.putCharSequence(ShortcutManagerCompat.EXTRA_SHORTCUT_ID, shortcut.getId());
            chooserTargets.add(new ChooserTarget(
                    // The name of this target.
                    shortcut.getShortLabel(),
                    // The icon to represent this target.
                    icon != null ? icon.toIcon() : null,
                    // The ranking score for this target (0.0-1.0); the system will omit items with
                    // low scores when there are too many Direct Share items.
                    0.5f,
                    // The name of the component to be launched if this target is chosen.
                    new ComponentName(context.getPackageName(), target.mTargetClass),
                    // The extra values here will be merged into the Intent when this target is
                    // chosen.
                    extras));
        }
        return chooserTargets;
    }
}
