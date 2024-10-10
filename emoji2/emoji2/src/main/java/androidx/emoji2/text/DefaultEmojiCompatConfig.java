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
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.provider.FontRequest;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The default config will use downloadable fonts to fetch the emoji compat font file.
 *
 * <p>It will automatically fetch the emoji compat font from a {@code ContentProvider} that is
 * installed on the devices system image, if present.</p>
 *
 * <p>You should use this if you want the default emoji font from a system installed
 * downloadable fonts provider. This is the recommended behavior for all applications unless
 * they install a custom emoji font.</p>
 *
 * <p>You may need to specialize the configuration beyond this default config in some
 * situations:</p>
 * <ul>
 *  <li>If you are trying to install a custom emoji font through downloadable fonts use
 *  {@link FontRequestEmojiCompatConfig} instead of this method.</li>
 *  <li>If you're trying to bundle an emoji font with your APK use {@code
 *  BundledEmojiCompatConfig} in the {@code emoji2-bundled} artifact.</li>
 *  <li>If you are building an APK that will be installed on devices that won't have a
 *  downloadable fonts provider, use {@code BundledEmojiCompatConfig}.</li>
 * </ul>
 *
 * <p>The downloadable font provider used by {@code DefaultEmojiCompatConfig} always satisfies
 * the following contract:</p>
 * <ol>
 *  <li>It <i>MUST</i> provide an intent filter for {@code androidx.content.action.LOAD_EMOJI_FONT}.
 *  </li>
 *  <li>It <i>MUST</i> respond to the query {@code emojicompat-emoji-font} with a valid emoji compat
 *  font file including metadata.</li>
 *  <li>It <i>MUST</i> provide fonts via the same contract as downloadable fonts.</li>
 *  <li>It <i>MUST</i> be installed in the system image.</li>
 * </ol>
 */
public final class DefaultEmojiCompatConfig {
    /**
     * This class cannot be instantiated.
     *
     * @see DefaultEmojiCompatConfig#create
     */
    private DefaultEmojiCompatConfig() {
    }

    /**
     * Get the default emoji compat config for this device.
     *
     * You may further configure the returned config before passing it to {@link EmojiCompat#init}.
     *
     * Each call to this method will return a new EmojiCompat.Config, so changes to the returned
     * object will not modify future return values.
     *
     * @param context context for lookup
     * @return A valid config for downloading the emoji compat font, or null if no font provider
     * could be found.
     */
    public static @Nullable FontRequestEmojiCompatConfig create(@NonNull Context context) {
        return (FontRequestEmojiCompatConfig) new DefaultEmojiCompatConfigFactory(null)
                .create(context);
    }

    /**
     * Actual factory for generating default emoji configs, does service locator lookup internally.
     *
     * @see DefaultEmojiCompatConfig#create
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class DefaultEmojiCompatConfigFactory {
        private static final @NonNull String TAG = "emoji2.text.DefaultEmojiConfig";
        private static final @NonNull String INTENT_LOAD_EMOJI_FONT =
                "androidx.content.action.LOAD_EMOJI_FONT";
        private static final @NonNull String DEFAULT_EMOJI_QUERY = "emojicompat-emoji-font";
        private final DefaultEmojiCompatConfigHelper mHelper;

        /**
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public DefaultEmojiCompatConfigFactory(@Nullable DefaultEmojiCompatConfigHelper helper) {
            mHelper = helper != null ? helper : getHelperForApi();
        }

        /**
         * @see DefaultEmojiCompatConfig#create
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public EmojiCompat.@Nullable Config create(@NonNull Context context) {
            return configOrNull(context, (FontRequest) queryForDefaultFontRequest(context));
        }

        /**
         * Create a new Config if fontRequest is not null
         * @param context context for the config
         * @param fontRequest optional font request
         * @return a new config if fontRequest is not null
         */
        private EmojiCompat.@Nullable Config configOrNull(@NonNull Context context,
                @Nullable FontRequest fontRequest) {
            if (fontRequest == null) {
                return null;
            } else {
                return new FontRequestEmojiCompatConfig(context, fontRequest);
            }
        }

        /**
         * Find the installed font provider and return a FontInfo that describes it.
         * @param context context for getting package manager
         * @return valid FontRequest, or null if no provider could be found
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @VisibleForTesting
        @Nullable FontRequest queryForDefaultFontRequest(@NonNull Context context) {
            PackageManager packageManager = context.getPackageManager();
            // throw here since the developer has provided an atypical Context
            Preconditions.checkNotNull(packageManager,
                    "Package manager required to locate emoji font provider");
            ProviderInfo providerInfo = queryDefaultInstalledContentProvider(packageManager);
            if (providerInfo == null) return null;

            try {
                return generateFontRequestFrom(providerInfo, packageManager);
            } catch (PackageManager.NameNotFoundException e) {
                Log.wtf(TAG, e);
                return null;
            }
        }

        /**
         * Look up a ContentProvider that provides emoji fonts that's installed with the system.
         *
         * @param packageManager package manager from a Context
         * @return a ResolveInfo for a system installed content provider, or null if none found
         */
        private @Nullable ProviderInfo queryDefaultInstalledContentProvider(
                @NonNull PackageManager packageManager) {
            List<ResolveInfo> providers = mHelper.queryIntentContentProviders(packageManager,
                    new Intent(INTENT_LOAD_EMOJI_FONT), 0);

            for (ResolveInfo resolveInfo : providers) {
                ProviderInfo providerInfo = mHelper.getProviderInfo(resolveInfo);
                if (hasFlagSystem(providerInfo)) {
                    return providerInfo;
                }
            }
            return null;
        }

        /**
         * @param providerInfo optional ProviderInfo that describes a content provider
         * @return true if this provider info is from an application with
         * {@link ApplicationInfo#FLAG_SYSTEM}
         */
        private boolean hasFlagSystem(@Nullable ProviderInfo providerInfo) {
            return providerInfo != null
                    && providerInfo.applicationInfo != null
                    && (providerInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                    == ApplicationInfo.FLAG_SYSTEM;
        }

        /**
         * Generate a full FontRequest from a ResolveInfo that describes a ContentProvider.
         *
         * @param providerInfo description of content provider to generate a FontRequest from
         * @return a valid font request
         * @throws NullPointerException if the passed resolveInfo has a null providerInfo.
         */
        private @NonNull FontRequest generateFontRequestFrom(
                @NonNull ProviderInfo providerInfo,
                @NonNull PackageManager packageManager
        ) throws PackageManager.NameNotFoundException {
            String providerAuthority = providerInfo.authority;
            String providerPackage = providerInfo.packageName;

            Signature[] signingSignatures = mHelper.getSigningSignatures(packageManager,
                    providerPackage);
            List<List<byte[]>> signatures = convertToByteArray(signingSignatures);
            return new FontRequest(providerAuthority, providerPackage, DEFAULT_EMOJI_QUERY,
                    signatures);
        }

        /**
         * Convert signatures into a form usable by a FontConfig
         */
        private @NonNull List<List<byte[]>> convertToByteArray(Signature @NonNull [] signatures) {
            List<byte[]> shaList = new ArrayList<>();
            for (Signature signature : signatures) {
                shaList.add(signature.toByteArray());
            }
            return Collections.singletonList(shaList);
        }

        /**
         * @return the right DefaultEmojiCompatConfigHelper for the device API
         */
        private static @NonNull DefaultEmojiCompatConfigHelper getHelperForApi() {
            if (Build.VERSION.SDK_INT >= 28) {
                return new DefaultEmojiCompatConfigHelper_API28();
            } else {
                return new DefaultEmojiCompatConfigHelper();
            }
        }
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class DefaultEmojiCompatConfigHelper {
        /**
         * Get the content provider by intent.
         */
        @SuppressWarnings("deprecation")
        public @NonNull List<ResolveInfo> queryIntentContentProviders(
                @NonNull PackageManager packageManager, @NonNull Intent intent, int flags) {
            return packageManager.queryIntentContentProviders(intent, flags);
        }

        /**
         * Get a ProviderInfo, if present, from a ResolveInfo
         * @param resolveInfo the subject
         * @return resolveInfo.providerInfo above API 19
         */
        public @Nullable ProviderInfo getProviderInfo(@NonNull ResolveInfo resolveInfo) {
            return resolveInfo.providerInfo;
        }

        /**
         * Get the signing signatures for a package in package manager.
         */
        @SuppressWarnings("deprecation") // using deprecated API to match exact behavior in core
        public Signature @NonNull [] getSigningSignatures(@NonNull PackageManager packageManager,
                @NonNull String providerPackage) throws PackageManager.NameNotFoundException {
            PackageInfo packageInfoForSignatures = packageManager.getPackageInfo(providerPackage,
                    PackageManager.GET_SIGNATURES);
            return packageInfoForSignatures.signatures;
        }
    }

    /**
     * Helper to lookup signatures in package manager > API 28
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(28)
    public static class DefaultEmojiCompatConfigHelper_API28
            extends DefaultEmojiCompatConfigHelper {
        @SuppressWarnings("deprecation") // using deprecated API to match exact behavior in core
        @Override
        public Signature @NonNull [] getSigningSignatures(@NonNull PackageManager packageManager,
                @NonNull String providerPackage)
                throws PackageManager.NameNotFoundException {
            // This uses the deprecated GET_SIGNATURES currently to match the behavior in Core.
            // When that behavior changes, we will need to update this method.

            // Alternatively, you may at that time introduce a new config option that allows
            // skipping signature validations to avoid this code sync.
            PackageInfo packageInfoForSignatures = packageManager.getPackageInfo(providerPackage,
                    PackageManager.GET_SIGNATURES);
            return packageInfoForSignatures.signatures;
        }
    }
}
