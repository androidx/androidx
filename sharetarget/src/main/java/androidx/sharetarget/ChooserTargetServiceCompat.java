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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides direct share items to the system, by cross checking dynamic shortcuts from
 * ShortcutManagerCompat and share target definitions from a Xml resource. Used for backward
 * compatibility to push share targets to shortcut manager on older SDKs.
 *
 * @hide
 */
@RequiresApi(23)
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class ChooserTargetServiceCompat extends ChooserTargetService {

    static final String TAG = "ChooserServiceCompat";

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
            IntentFilter matchedFilter) {
        Context context = getApplicationContext();

        // Retrieve share targets
        List<ShareTargetCompat> targets = ShareTargetXmlParser.getShareTargets(context);
        List<ShareTargetCompat> matchedTargets = new ArrayList<>();
        for (ShareTargetCompat target : targets) {
            if (!target.mTargetClass.equals(targetActivityName.getClassName())) {
                continue;
            }
            for (ShareTargetCompat.TargetData data : target.mTargetData) {
                if (matchedFilter.hasDataType(data.mMimeType)) {
                    // Matched at least with one data type (OR operation)
                    matchedTargets.add(target);
                    break;
                }
            }
        }
        if (matchedTargets.isEmpty()) {
            return Collections.emptyList();
        }

        // Retrieve shortcuts
        ShortcutInfoCompatSaverImpl shortcutSaver =
                ShortcutInfoCompatSaverImpl.getInstance(context);
        List<ShortcutInfoCompat> shortcuts;
        try {
            shortcuts = shortcutSaver.getShortcuts();
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve shortcuts: ", e);
            return Collections.emptyList();
        }
        if (shortcuts == null || shortcuts.isEmpty()) {
            return Collections.emptyList();
        }

        // List of matched shortcuts with their target component names
        List<ShortcutHolder> matchedShortcuts = new ArrayList<>();
        for (ShortcutInfoCompat shortcut : shortcuts) {
            for (ShareTargetCompat item : matchedTargets) {
                // Shortcut must have all the share target's categories (AND operation)
                if (shortcut.getCategories().containsAll(Arrays.asList(item.mCategories))) {
                    matchedShortcuts.add(new ShortcutHolder(shortcut,
                            new ComponentName(context.getPackageName(), item.mTargetClass)));
                    break;
                }
            }
        }
        return convertShortcutsToChooserTargets(shortcutSaver, matchedShortcuts);
    }

    @VisibleForTesting
    @NonNull
    static List<ChooserTarget> convertShortcutsToChooserTargets(
            @NonNull ShortcutInfoCompatSaverImpl shortcutSaver,
            @NonNull List<ShortcutHolder> matchedShortcuts) {
        if (matchedShortcuts.isEmpty()) {
            return new ArrayList<>();
        }
        Collections.sort(matchedShortcuts);

        ArrayList<ChooserTarget> chooserTargets = new ArrayList<>();
        float currentScore = 1.0f;
        int lastRank = matchedShortcuts.get(0).getShortcut().getRank();
        for (ShortcutHolder holder : matchedShortcuts) {
            final ShortcutInfoCompat shortcut = holder.getShortcut();
            IconCompat shortcutIcon;
            try {
                shortcutIcon = shortcutSaver.getShortcutIcon(shortcut.getId());
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve shortcut icon: ", e);
                shortcutIcon = null;
            }

            Bundle extras = new Bundle();
            extras.putString(ShortcutManagerCompat.EXTRA_SHORTCUT_ID, shortcut.getId());

            if (lastRank != shortcut.getRank()) {
                currentScore -= 0.01f;
                lastRank = shortcut.getRank();
            }
            chooserTargets.add(new ChooserTarget(
                    shortcut.getShortLabel(),
                    shortcutIcon == null ? null : shortcutIcon.toIcon(),
                    currentScore,
                    holder.getTargetClass(),
                    extras));
        }

        return chooserTargets;
    }

    static class ShortcutHolder implements Comparable<ShortcutHolder> {
        private final ShortcutInfoCompat mShortcut;
        private final ComponentName mTargetClass;

        ShortcutHolder(ShortcutInfoCompat shortcut, ComponentName targetClass) {
            mShortcut = shortcut;
            mTargetClass = targetClass;
        }

        ShortcutInfoCompat getShortcut() {
            return mShortcut;
        }

        ComponentName getTargetClass() {
            return mTargetClass;
        }

        @Override
        public int compareTo(ShortcutHolder other) {
            return this.getShortcut().getRank() - other.getShortcut().getRank();
        }
    }
}
