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

import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.pm.ShortcutInfoChangeListener;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.google.shortcuts.builders.CapabilityBuilder;
import androidx.core.google.shortcuts.builders.ParameterBuilder;
import androidx.core.google.shortcuts.builders.ShortcutBuilder;

import com.google.crypto.tink.KeysetHandle;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;

import java.util.ArrayList;
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
    @Nullable private final KeysetHandle mKeysetHandle;

    /**
     * Create an instance of {@link ShortcutInfoChangeListenerImpl}.
     *
     * @param context The application context.
     * @return {@link ShortcutInfoChangeListenerImpl}.
     */
    @NonNull
    public static ShortcutInfoChangeListenerImpl getInstance(@NonNull Context context) {
        return new ShortcutInfoChangeListenerImpl(context, FirebaseAppIndex.getInstance(context),
                FirebaseUserActions.getInstance(context),
                ShortcutUtils.getOrCreateShortcutKeysetHandle(context));
    }

    @VisibleForTesting
    ShortcutInfoChangeListenerImpl(Context context, FirebaseAppIndex firebaseAppIndex,
            FirebaseUserActions firebaseUserActions, @Nullable KeysetHandle keysetHandle) {
        mContext = context;
        mFirebaseAppIndex = firebaseAppIndex;
        mFirebaseUserActions = firebaseUserActions;
        mKeysetHandle = keysetHandle;
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
        String shortcutUrl = ShortcutUtils.getIndexableShortcutUrl(mContext, shortcut.getIntent(),
                mKeysetHandle);
        String name = shortcut.getShortLabel().toString();

        ShortcutBuilder shortcutBuilder = new ShortcutBuilder()
                .setId(shortcut.getId())
                .setUrl(url)
                .setShortcutLabel(name)
                .setShortcutUrl(shortcutUrl);
        if (shortcut.getLongLabel() != null) {
            shortcutBuilder.setShortcutDescription(shortcut.getLongLabel().toString());
        }

        // Add capability binding
        if (Build.VERSION.SDK_INT >= 21) {
            if (shortcut.getCategories() != null) {
                List<CapabilityBuilder> capabilityList = new ArrayList<>();
                for (String capability : shortcut.getCategories()) {
                    if (!ShortcutUtils.isAppActionCapability(capability)) {
                        continue;
                    }

                    capabilityList.add(Api21Impl.buildCapability(capability, shortcut.getExtras()));
                }

                if (!capabilityList.isEmpty()) {
                    shortcutBuilder
                            .setCapability(capabilityList.toArray(new CapabilityBuilder[0]));
                }
            }
        }

        // By default, the indexable will be saved only on-device.
        return shortcutBuilder.build();
    }

    @RequiresApi(21)
    private static class Api21Impl {
        @NonNull
        static CapabilityBuilder buildCapability(@NonNull String capability,
                @Nullable PersistableBundle shortcutInfoExtras) {
            CapabilityBuilder capabilityBuilder = new CapabilityBuilder()
                    .setName(capability);
            if (shortcutInfoExtras == null) {
                return capabilityBuilder;
            }

            String[] params = shortcutInfoExtras.getStringArray(capability);
            if (params == null) {
                return capabilityBuilder;
            }

            List<ParameterBuilder> parameterBuilders = new ArrayList<>();
            for (String param : params) {
                ParameterBuilder parameterBuilder =
                        new ParameterBuilder()
                                .setName(param);
                String capabilityParamKey = capability + CAPABILITY_PARAM_SEPARATOR + param;
                String[] values = shortcutInfoExtras.getStringArray(capabilityParamKey);
                if (values == null || values.length == 0) {
                    // ignore this parameter since no values were given
                    continue;
                }

                parameterBuilder.setValue(values);
                parameterBuilders.add(parameterBuilder);
            }

            if (parameterBuilders.size() > 0) {
                capabilityBuilder
                        .setParameter(parameterBuilders.toArray(new ParameterBuilder[0]));
            }
            return capabilityBuilder;
        }

        private Api21Impl() {}
    }
}
