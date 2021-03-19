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

package androidx.core.google.shortcuts;


import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.core.google.shortcuts.ShortcutUtils.CAPABILITY_PARAM_SEPARATOR;
import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_DESCRIPTION_KEY;
import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_LABEL_KEY;
import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_URL_KEY;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.pm.ShortcutInfoChangeListener;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.IndexableBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a listener on changes to shortcuts in ShortcutInfoCompat.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ShortcutInfoChangeListenerImpl extends ShortcutInfoChangeListener {
    private final Context mContext;
    private final FirebaseAppIndex mFirebaseAppIndex;
    private final FirebaseUserActions mFirebaseUserActions;

    /**
     * Create an instance of {@link ShortcutInfoChangeListenerImpl}.
     *
     * @param context The application context.
     * @return {@link ShortcutInfoChangeListenerImpl}.
     */
    @NonNull
    public static ShortcutInfoChangeListenerImpl getInstance(@NonNull Context context) {
        return new ShortcutInfoChangeListenerImpl(context, FirebaseAppIndex.getInstance(context),
                FirebaseUserActions.getInstance(context));
    }

    @VisibleForTesting
    ShortcutInfoChangeListenerImpl(Context context, FirebaseAppIndex firebaseAppIndex,
            FirebaseUserActions firebaseUserActions) {
        mContext = context;
        mFirebaseAppIndex = firebaseAppIndex;
        mFirebaseUserActions = firebaseUserActions;
    }

    /**
     * Called when shortcut is added by {@link androidx.core.content.pm.ShortcutManagerCompat}.
     *
     * @param shortcuts list of shortcuts added
     */
    @Override
    public void onShortcutAdded(@NonNull List<ShortcutInfoCompat> shortcuts) {
        mFirebaseAppIndex.update(buildIndexables(shortcuts));
    }

    /**
     * Called when shortcut is updated by {@link androidx.core.content.pm.ShortcutManagerCompat}.
     *
     * @param shortcuts list of shortcuts updated
     */
    @Override
    public void onShortcutUpdated(@NonNull List<ShortcutInfoCompat> shortcuts) {
        mFirebaseAppIndex.update(buildIndexables(shortcuts));
    }

    /**
     * Called when shortcut is removed by {@link androidx.core.content.pm.ShortcutManagerCompat}.
     *
     * @param shortcutIds list of shortcut ids removed
     */
    @Override
    public void onShortcutRemoved(@NonNull List<String> shortcutIds) {
        List<String> urls = new ArrayList<>();
        for (String shortcutId : shortcutIds) {
            urls.add(ShortcutUtils.getIndexableUrl(mContext, shortcutId));
        }
        mFirebaseAppIndex.remove(urls.toArray(new String[0]));
    }

    /**
     * Called when shortcut is used by {@link androidx.core.content.pm.ShortcutManagerCompat}.
     *
     * @param shortcutIds list of shortcut ids used
     */
    @Override
    public void onShortcutUsageReported(@NonNull List<String> shortcutIds) {
        for (String shortcutId : shortcutIds) {
            // Actions reported here is only on-device due to setUpload(false) in buildAction
            // method.
            mFirebaseUserActions.end(buildAction(ShortcutUtils.getIndexableUrl(mContext,
                    shortcutId)));
        }
    }

    /**
     * Called when all shortcuts are removed
     * by {@link androidx.core.content.pm.ShortcutManagerCompat}.
     */
    @Override
    public void onAllShortcutsRemoved() {
        mFirebaseAppIndex.removeAll();
    }

    @NonNull
    private Action buildAction(@NonNull String url) {
        // The reported action isn't uploaded to the server.
        Action.Metadata.Builder metadataBuilder = new Action.Metadata.Builder().setUpload(false);
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                // Empty label as placeholder.
                .setObject("", url)
                .setMetadata(metadataBuilder)
                .build();
    }

    @NonNull
    private Indexable[] buildIndexables(@NonNull List<ShortcutInfoCompat> shortcuts) {
        List<Indexable> indexables = new ArrayList<>();
        for (ShortcutInfoCompat shortcut : shortcuts) {
            indexables.add(buildIndexable(shortcut));
        }
        return indexables.toArray(new Indexable[0]);
    }

    @NonNull
    private Indexable buildIndexable(@NonNull ShortcutInfoCompat shortcut) {
        String url = ShortcutUtils.getIndexableUrl(mContext, shortcut.getId());
        String shortcutUrl = ShortcutUtils.getIndexableShortcutUrl(mContext, shortcut.getIntent());

        Indexable.Builder builder = new Indexable.Builder()
                .setId(shortcut.getId())
                .setUrl(url)
                .setName(shortcut.getShortLabel().toString())
                .put(SHORTCUT_URL_KEY, shortcutUrl)
                .put(SHORTCUT_LABEL_KEY, shortcut.getShortLabel().toString());

        if (shortcut.getLongLabel() != null) {
            builder.put(SHORTCUT_DESCRIPTION_KEY, shortcut.getLongLabel().toString());
        }

        if (shortcut.getIcon() != null && shortcut.getIcon().getType() == IconCompat.TYPE_URI) {
            builder.setImage(shortcut.getIcon().getUri().toString());
        }

        // Add capability binding
        if (shortcut.getCategories() != null) {
            List<Indexable.Builder> partOfList = new ArrayList<>();
            for (String capability : shortcut.getCategories()) {
                if (!ShortcutUtils.isAppActionCapability(capability)) {
                    continue;
                }

                if (shortcut.getExtras() == null
                        || shortcut.getExtras().getStringArray(capability) == null
                        || shortcut.getExtras().getStringArray(capability).length == 0) {
                    // Shortcut has a capability binding without any parameter binding.
                    partOfList.add(buildPartOfIndexable(capability, null));
                } else {
                    String[] params = shortcut.getExtras().getStringArray(capability);
                    for (String param : params) {
                        String capabilityParam = capability + CAPABILITY_PARAM_SEPARATOR + param;
                        partOfList.add(buildPartOfIndexable(capabilityParam,
                                shortcut.getExtras().getStringArray(capabilityParam)));
                    }
                }
            }

            if (!partOfList.isEmpty()) {
                builder.setIsPartOf(partOfList.toArray(new IndexableBuilder[0]));
            }
        }

        // By default, the indexable will be saved only on-device.
        return builder.build();
    }

    @NonNull
    private Indexable.Builder buildPartOfIndexable(@NonNull String capabilityParam,
            @Nullable String[] values) {
        Indexable.Builder partOfBuilder = new Indexable.Builder()
                .setId(capabilityParam);
        if (values == null) {
            return partOfBuilder;
        }

        if (values.length > 0) {
            partOfBuilder.setName(values[0]);
        }
        if (values.length > 1) {
            partOfBuilder.setAlternateName(Arrays.copyOfRange(values, 1, values.length));
        }
        return partOfBuilder;
    }
}
