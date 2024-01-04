/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.content;

import static androidx.core.util.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.strictmode.UnsafeIntentLaunchViolation;
import android.provider.MediaStore;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Consumer;
import androidx.core.util.Predicate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to make a sanitized copy of an {@link Intent}. This could be used when
 * {@link UnsafeIntentLaunchViolation} is detected.
 * This class is thread safe and the object created is safe to be reused.
 * Typical usage of the class:
 * <pre>
 * {@code
 * Intent intent = new  IntentSanitizer.Builder()
 *      .allowComponent(“com.example.ActivityA”)
 *      .allowData(“com.example”)
 *      .allowType(“text/plain”)
 *      .build()
 *      .sanitizeByThrowing(intent);
 * }
 * </pre>
 *
 * At least one of the allowPackage, allowComponent must be called unless implicit intent is
 * allowed. In which case, allowAnyComponent must be called and caution has to be taken to
 * protect your private data.
 */
public class IntentSanitizer {
    private static final String TAG = "IntentSanitizer";

    private int mAllowedFlags;
    private Predicate<String> mAllowedActions;
    private Predicate<Uri> mAllowedData;
    private Predicate<String> mAllowedTypes;
    private Predicate<String> mAllowedCategories;
    private Predicate<String> mAllowedPackages;
    private Predicate<ComponentName> mAllowedComponents;
    private boolean mAllowAnyComponent;
    private Map<String, Predicate<Object>> mAllowedExtras;
    private boolean mAllowClipDataText;
    private Predicate<Uri> mAllowedClipDataUri;
    private Predicate<ClipData> mAllowedClipData;
    private boolean mAllowIdentifier;
    private boolean mAllowSelector;
    private boolean mAllowSourceBounds;

    private IntentSanitizer() {
    }

    /**
     * Convenient method for filtering unwanted members from the input intent and log it.
     *
     * @param in input intent
     * @return a copy of the input intent after filtering out unwanted members.
     */
    @NonNull
    public Intent sanitizeByFiltering(@NonNull Intent in) {
        return sanitize(in, msg -> {});
    }

    /**
     * Convenient method for throwing a SecurityException when unwanted members of the input
     * intent is encountered.
     *
     * @param in input intent
     * @return a copy of the input intent if the input intent does not contain any unwanted members.
     * @throws SecurityException if the input intent contains any unwanted members.
     */
    @NonNull
    public Intent sanitizeByThrowing(@NonNull Intent in) {
        return sanitize(in, msg -> {
            throw new SecurityException(msg);
        });
    }

    /**
     * This method sanitizes the given intent. If dirty members are found, the errors are consumed
     * by the penalty object. The penalty action could be called multiple times if multiple
     * issues exist.
     *
     * @param in      the given intent.
     * @param penalty consumer of the error message if dirty members are found.
     * @return a sanitized copy of the given intent.
     */
    @NonNull
    public Intent sanitize(@NonNull Intent in,
            @NonNull Consumer<String> penalty) {
        Intent intent = new Intent();

        ComponentName componentName = in.getComponent();
        if ((mAllowAnyComponent && componentName == null)
                || mAllowedComponents.test(componentName)) {
            intent.setComponent(componentName);
        } else {
            penalty.accept("Component is not allowed: " + componentName);
            intent.setComponent(new ComponentName("android", "java.lang.Void"));
        }

        String packageName = in.getPackage();
        if (packageName == null || mAllowedPackages.test(packageName)) {
            intent.setPackage(packageName);
        } else {
            penalty.accept(("Package is not allowed: " + packageName));
        }

        if ((mAllowedFlags | in.getFlags()) == mAllowedFlags) {
            intent.setFlags(in.getFlags());
        } else {
            intent.setFlags(mAllowedFlags & in.getFlags());
            penalty.accept("The intent contains flags that are not allowed: "
                    + "0x" + Integer.toHexString(in.getFlags() & ~mAllowedFlags));
        }

        String action = in.getAction();
        if (action == null || mAllowedActions.test(action)) {
            intent.setAction(action);
        } else {
            penalty.accept("Action is not allowed: " + action);
        }

        Uri data = in.getData();
        if (data == null || mAllowedData.test(data)) {
            intent.setData(data);
        } else {
            penalty.accept("Data is not allowed: " + data);
        }

        String type = in.getType();
        if (type == null || mAllowedTypes.test(type)) {
            intent.setDataAndType(intent.getData(), type);
        } else {
            penalty.accept("Type is not allowed: " + type);
        }

        Set<String> categories = in.getCategories();
        if (categories != null) {
            for (String category : categories) {
                if (mAllowedCategories.test(category)) {
                    intent.addCategory(category);
                } else {
                    penalty.accept("Category is not allowed: " + category);
                }
            }
        }

        Bundle extras = in.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                if (key.equals(Intent.EXTRA_STREAM)
                        && (mAllowedFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
                    penalty.accept(
                            "Allowing Extra Stream requires also allowing at least "
                                    + " FLAG_GRANT_READ_URI_PERMISSION Flag.");
                    continue;
                }
                if (key.equals(MediaStore.EXTRA_OUTPUT)
                        && (~mAllowedFlags
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) != 0) {
                    penalty.accept("Allowing Extra Output requires also allowing "
                            + "FLAG_GRANT_READ_URI_PERMISSION and FLAG_GRANT_WRITE_URI_PERMISSION"
                            + " Flags.");
                    continue;
                }
                Object value = extras.get(key);
                Predicate<Object> test = mAllowedExtras.get(key);
                if (test != null && test.test(value)) {
                    putExtra(intent, key, value);
                } else {
                    penalty.accept("Extra is not allowed. Key: " + key + ". Value: " + value);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Api16Impl.sanitizeClipData(in, intent, mAllowedClipData, mAllowClipDataText,
                    mAllowedClipDataUri, penalty);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mAllowIdentifier) {
                Api29Impl.setIdentifier(intent, Api29Impl.getIdentifier(in));
            } else if (Api29Impl.getIdentifier(in) != null) {
                penalty.accept("Identifier is not allowed: " + Api29Impl.getIdentifier(in));
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            if (mAllowSelector) {
                Api15Impl.setSelector(intent, Api15Impl.getSelector(in));
            } else if (Api15Impl.getSelector(in) != null) {
                penalty.accept("Selector is not allowed: " + Api15Impl.getSelector(in));
            }
        }

        if (mAllowSourceBounds) {
            intent.setSourceBounds(in.getSourceBounds());
        } else if (in.getSourceBounds() != null) {
            penalty.accept("SourceBounds is not allowed: " + in.getSourceBounds());
        }

        return intent;
    }

    private void putExtra(Intent intent, String key, Object value) {
        if (value == null) {
            intent.getExtras().putString(key, null);
        } else if (value instanceof Parcelable) {
            intent.putExtra(key, (Parcelable) value);
        } else if (value instanceof Parcelable[]) {
            intent.putExtra(key, (Parcelable[]) value);
        } else if (value instanceof Serializable) {
            intent.putExtra(key, (Serializable) value);
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass());
        }
    }

    /**
     * General strategy of building is to only offer additive “or” operations that are chained
     * together. Any more complex operations can be performed by the developer providing their
     * own custom Predicate.
     */
    public static final class Builder {
        private static final int HISTORY_STACK_FLAGS =
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_TASK_ON_HOME;

        private static final int RECEIVER_FLAGS =
                Intent.FLAG_RECEIVER_FOREGROUND
                        | Intent.FLAG_RECEIVER_NO_ABORT
                        | Intent.FLAG_RECEIVER_REGISTERED_ONLY
                        | Intent.FLAG_RECEIVER_REPLACE_PENDING
                        | Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS;

        private int mAllowedFlags;
        private Predicate<String> mAllowedActions = v -> false;
        private Predicate<Uri> mAllowedData = v -> false;
        private Predicate<String> mAllowedTypes = v -> false;
        private Predicate<String> mAllowedCategories = v -> false;
        private Predicate<String> mAllowedPackages = v -> false;
        private Predicate<ComponentName> mAllowedComponents = v -> false;
        private boolean mAllowAnyComponent;
        private boolean mAllowSomeComponents;
        private Map<String, Predicate<Object>> mAllowedExtras = new HashMap<>();
        private boolean mAllowClipDataText = false;
        private Predicate<Uri> mAllowedClipDataUri = v -> false;
        private Predicate<ClipData> mAllowedClipData = v -> false;
        private boolean mAllowIdentifier;
        private boolean mAllowSelector;
        private boolean mAllowSourceBounds;

        /**
         * Sets allowed flags.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         * In most cases following grant URI permission related flags should
         * <b>not</b> be allowed:
         * <ul>
         * <li>FLAG_GRANT_PERSISTABLE_URI_PERMISSION</li>
         * <li>FLAG_GRANT_PREFIX_URI_PERMISSION</li>
         * <li>FLAG_GRANT_READ_URI_PERMISSION</li>
         * <li>FLAG_GRANT_WRITE_URI_PERMISSION</li>
         * </ul>
         * Setting these flags would allow others to access URIs only your
         * app has permission to access. These URIs could be set in intent's data, clipData
         * and/or, in certain circumstances, extras with key of {@link Intent#EXTRA_STREAM} or
         * {@link MediaStore#EXTRA_OUTPUT}.
         * When these flags are allowed, you should sanitize URIs. See
         * {@link #allowDataWithAuthority(String)},
         * {@link #allowData(Predicate)}, {@link #allowClipDataUriWithAuthority(String)},
         * {@link #allowClipDataUri(Predicate)}, {@link #allowExtraStreamUriWithAuthority(String)},
         * {@link #allowExtraStream(Predicate)}, {@link #allowExtraOutput(String)},
         * {@link #allowExtraOutput(Predicate)}
         *
         * @param flags allowed flags.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowFlags(int flags) {
            mAllowedFlags |= flags;
            return this;
        }

        /**
         * Adds all history stack flags into the allowed flags set. They are:
         * <ul>
         * <li>FLAG_ACTIVITY_BROUGHT_TO_FRONT
         * <li>FLAG_ACTIVITY_CLEAR_TASK
         * <li>FLAG_ACTIVITY_CLEAR_TOP
         * <li>FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
         * <li>FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
         * <li>FLAG_ACTIVITY_LAUNCH_ADJACENT
         * <li>FLAG_ACTIVITY_MULTIPLE_TASK
         * <li>FLAG_ACTIVITY_NEW_DOCUMENT
         * <li>FLAG_ACTIVITY_NEW_TASK
         * <li>FLAG_ACTIVITY_NO_ANIMATION
         * <li>FLAG_ACTIVITY_NO_HISTORY
         * <li>FLAG_ACTIVITY_PREVIOUS_IS_TOP
         * <li>FLAG_ACTIVITY_REORDER_TO_FRONT
         * <li>FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
         * <li>FLAG_ACTIVITY_RETAIN_IN_RECENTS
         * <li>FLAG_ACTIVITY_SINGLE_TOP
         * <li>FLAG_ACTIVITY_TASK_ON_HOME
         * </ul>
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowHistoryStackFlags() {
            mAllowedFlags |= HISTORY_STACK_FLAGS;
            return this;
        }

        /**
         * Adds all receiver flags into the allowed flags set. They are
         * <ul>
         * <li>FLAG_RECEIVER_FOREGROUND
         * <li>FLAG_RECEIVER_NO_ABORT
         * <li>FLAG_RECEIVER_REGISTERED_ONLY
         * <li>FLAG_RECEIVER_REPLACE_PENDING
         * <li>FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS
         * </ul>
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowReceiverFlags() {
            mAllowedFlags |= RECEIVER_FLAGS;
            return this;
        }

        /**
         * Add an action to the list of allowed actions.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param action the name of an action.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowAction(@NonNull String action) {
            checkNotNull(action);
            allowAction(action::equals);
            return this;
        }

        /**
         * Add a filter for allowed actions.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter a filter that tests if an action is allowed.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowAction(@NonNull Predicate<String> filter) {
            checkNotNull(filter);
            mAllowedActions = mAllowedActions.or(filter);
            return this;
        }

        /**
         * Convenient method to allow all data whose URI authority equals to the given.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param authority the URI's authority.
         * @return this builder
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowDataWithAuthority(@NonNull String authority) {
            checkNotNull(authority);
            allowData(v -> authority.equals(v.getAuthority()));
            return this;
        }

        /**
         * Allow data that passes the filter test.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter data filter.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowData(@NonNull Predicate<Uri> filter) {
            checkNotNull(filter);
            mAllowedData = mAllowedData.or(filter);
            return this;
        }

        /**
         * Add a data type to the allowed type list. Exact match is used to check the allowed
         * types. For example, if you pass in "image/*" here, it won't allow an intent with type of
         * "image/png".
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param type the data type that is allowed
         * @return this builder
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowType(@NonNull String type) {
            checkNotNull(type);
            return allowType(type::equals);
        }

        /**
         * Add a filter for allowed data types.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the data type filter.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowType(@NonNull Predicate<String> filter) {
            checkNotNull(filter);
            mAllowedTypes = mAllowedTypes.or(filter);
            return this;
        }

        /**
         * Add a category to the allowed category list.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param category the allowed category.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowCategory(@NonNull String category) {
            checkNotNull(category);
            return allowCategory(category::equals);
        }

        /**
         * Add a filter for allowed categories.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the category filter.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowCategory(@NonNull Predicate<String> filter) {
            checkNotNull(filter);
            mAllowedCategories = mAllowedCategories.or(filter);
            return this;
        }

        /**
         * Add a package to the allowed packages.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowPackage(@NonNull String packageName) {
            checkNotNull(packageName);
            return allowPackage(packageName::equals);
        }

        /**
         * Add a filter for allowed packages.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the package name filter.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowPackage(@NonNull Predicate<String> filter) {
            checkNotNull(filter);
            mAllowedPackages = mAllowedPackages.or(filter);
            return this;
        }

        /**
         * Add a component to the allowed components list.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param component the allowed component.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowComponent(@NonNull ComponentName component) {
            checkNotNull(component);
            return allowComponent(component::equals);
        }

        /**
         * Add a filter for allowed components.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the component filter.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowComponent(@NonNull Predicate<ComponentName> filter) {
            checkNotNull(filter);
            mAllowSomeComponents = true;
            mAllowedComponents = mAllowedComponents.or(filter);
            return this;
        }

        /**
         * Add a package to the allowed package list. Any component under this package is allowed.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowComponentWithPackage(@NonNull String packageName) {
            checkNotNull(packageName);
            return allowComponent(v -> packageName.equals(v.getPackageName()));
        }

        /**
         * Allow any components. Be cautious to call this method. When this method is called, you
         * should definitely disallow the 4 grant URI permission flags.
         * This method is useful in case the redirected intent is designed to support implicit
         * intent. This method is made mutually exclusive to the 4 methods that allow components
         * or packages.
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowAnyComponent() {
            mAllowAnyComponent = true;
            mAllowedComponents = v -> true;
            return this;
        }

        /**
         * Allows clipData that contains text.
         * overwrite each other.
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowClipDataText() {
            mAllowClipDataText = true;
            return this;
        }

        /**
         * Allows clipData whose items URIs authorities match the given authority.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param authority the given authority.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowClipDataUriWithAuthority(@NonNull String authority) {
            checkNotNull(authority);
            return allowClipDataUri(v -> authority.equals(v.getAuthority()));
        }


        /**
         * Allows clipData whose items URIs pass the given URI filter.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the given URI filter.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowClipDataUri(@NonNull Predicate<Uri> filter) {
            checkNotNull(filter);
            mAllowedClipDataUri = mAllowedClipDataUri.or(filter);
            return this;
        }


        /**
         * Allows clipData that passes the given filter.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the given clipData filter.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowClipData(@NonNull Predicate<ClipData> filter) {
            checkNotNull(filter);
            mAllowedClipData = mAllowedClipData.or(filter);
            return this;
        }

        /**
         * Allows an extra member whose key and type of value matches the given.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param key   the given extra key.
         * @param clazz the given class of the extra value.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowExtra(@NonNull String key, @NonNull Class<?> clazz) {
            return allowExtra(key, clazz, (v) -> true);
        }


        /**
         * Allows an extra member whose key matches the given key and whose value is of the type of
         * the given clazz and passes the value filter.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param key         given extra key.
         * @param clazz       given type of the extra value.
         * @param valueFilter the extra value filter.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public <T> Builder allowExtra(@NonNull String key, @NonNull Class<T> clazz,
                @NonNull Predicate<T> valueFilter) {
            checkNotNull(key);
            checkNotNull(clazz);
            checkNotNull(valueFilter);
            return allowExtra(key, v -> clazz.isInstance(v) && valueFilter.test(clazz.cast(v)));
        }

        /**
         * Allows an extra member whose key matches the given key and whose value passes the
         * filter test.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param key    the extra key.
         * @param filter the filter for the extra value.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowExtra(@NonNull String key, @NonNull Predicate<Object> filter) {
            checkNotNull(key);
            checkNotNull(filter);
            Predicate<Object> allowedExtra = mAllowedExtras.get(key);
            if (allowedExtra == null) allowedExtra = v -> false;
            allowedExtra = allowedExtra.or(filter);
            mAllowedExtras.put(key, allowedExtra);
            return this;
        }

        /**
         * Allows an extra member with the key Intent.EXTRA_STREAM. The value type has to be URI
         * and the authority matches the given parameter.
         * In order to use this method, user has to be explicitly allow the
         * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} flag. Otherwise, it will trigger penalty
         * during sanitization.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param uriAuthority the given URI authority.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowExtraStreamUriWithAuthority(@NonNull String uriAuthority) {
            checkNotNull(uriAuthority);
            allowExtra(Intent.EXTRA_STREAM, Uri.class,
                    (v) -> uriAuthority.equals(v.getAuthority()));
            return this;
        }

        /**
         * Allows an extra member with the key Intent.EXTRA_STREAM. The value type has to be URI
         * and the value also passes the given filter test.
         * In order to use this method, user has to be explicitly allow the
         * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} flag. Otherwise, it will trigger penalty
         * during sanitization.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the given URI authority.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowExtraStream(@NonNull Predicate<Uri> filter) {
            allowExtra(Intent.EXTRA_STREAM, Uri.class, filter);
            return this;
        }

        /**
         * Allows an extra member with the key MediaStore.EXTRA_OUTPUT. The value type has to be URI
         * and the authority matches the given parameter.
         * In order to use this method, user has to be explicitly allow the
         * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and
         * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION} flags. Otherwise, it will trigger penalty
         * during sanitization.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param uriAuthority the given URI authority.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowExtraOutput(@NonNull String uriAuthority) {
            allowExtra(MediaStore.EXTRA_OUTPUT, Uri.class,
                    (v) -> uriAuthority.equals(v.getAuthority()));
            return this;
        }

        /**
         * Allows an extra member with the key MediaStore.EXTRA_OUTPUT. The value type has to be URI
         * and the value also passes the given filter test.
         * In order to use this method, user has to be explicitly allow the
         * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and
         * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION} flags. Otherwise, it will trigger penalty
         * during sanitization.
         * This method can be called multiple times and the result is additive. They will not
         * overwrite each other.
         *
         * @param filter the given URI authority.
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowExtraOutput(@NonNull Predicate<Uri> filter) {
            allowExtra(MediaStore.EXTRA_OUTPUT, Uri.class, filter);
            return this;
        }

        /**
         * Allows any identifier.
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowIdentifier() {
            mAllowIdentifier = true;
            return this;
        }

        /**
         * Allow any selector.
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowSelector() {
            mAllowSelector = true;
            return this;
        }

        /**
         * Allow any source bounds.
         *
         * @return this builder.
         */
        @SuppressLint("BuilderSetStyle")
        @NonNull
        public Builder allowSourceBounds() {
            mAllowSourceBounds = true;
            return this;
        }

        /**
         * Build the IntentSanitizer.
         *
         * @return the IntentSanitizer
         */
        @NonNull
        public IntentSanitizer build() {
            if ((mAllowAnyComponent && mAllowSomeComponents)
                    || (!mAllowAnyComponent && !mAllowSomeComponents)) {
                throw new SecurityException(
                        "You must call either allowAnyComponent or one or more "
                                + "of the allowComponent methods; but not both.");
            }

            IntentSanitizer sanitizer = new IntentSanitizer();
            sanitizer.mAllowedFlags = this.mAllowedFlags;
            sanitizer.mAllowedActions = this.mAllowedActions;
            sanitizer.mAllowedData = this.mAllowedData;
            sanitizer.mAllowedTypes = this.mAllowedTypes;
            sanitizer.mAllowedCategories = this.mAllowedCategories;
            sanitizer.mAllowedPackages = this.mAllowedPackages;
            sanitizer.mAllowAnyComponent = this.mAllowAnyComponent;
            sanitizer.mAllowedComponents = this.mAllowedComponents;
            sanitizer.mAllowedExtras = this.mAllowedExtras;
            sanitizer.mAllowClipDataText = this.mAllowClipDataText;
            sanitizer.mAllowedClipDataUri = this.mAllowedClipDataUri;
            sanitizer.mAllowedClipData = this.mAllowedClipData;
            sanitizer.mAllowIdentifier = this.mAllowIdentifier;
            sanitizer.mAllowSelector = this.mAllowSelector;
            sanitizer.mAllowSourceBounds = this.mAllowSourceBounds;
            return sanitizer;
        }
    }

    @RequiresApi(15)
    private static class Api15Impl {
        private Api15Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setSelector(Intent intent, Intent selector) {
            intent.setSelector(selector);
        }

        @DoNotInline
        static Intent getSelector(Intent intent) {
            return intent.getSelector();
        }
    }

    @RequiresApi(16)
    private static class Api16Impl {
        private Api16Impl() {
        }

        @DoNotInline
        static void sanitizeClipData(@NonNull Intent in, Intent out,
                Predicate<ClipData> mAllowedClipData,
                boolean mAllowClipDataText,
                Predicate<Uri> mAllowedClipDataUri, Consumer<String> penalty) {
            ClipData clipData = in.getClipData();

            if (clipData == null) return;

            ClipData newClipData = null;
            if (mAllowedClipData != null && mAllowedClipData.test(clipData)) {
                out.setClipData(clipData);
            } else {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Api31Impl.checkOtherMembers(i, item, penalty);
                    } else {
                        checkOtherMembers(i, item, penalty);
                    }

                    CharSequence itemText = null;
                    if (mAllowClipDataText) {
                        itemText = item.getText();
                    } else {
                        if (item.getText() != null) {
                            penalty.accept(
                                    "Item text cannot contain value. Item position: " + i + "."
                                            + " Text: " + item.getText());
                        }
                    }

                    Uri itemUri = null;
                    if (mAllowedClipDataUri == null) {
                        if (item.getUri() != null) {
                            penalty.accept(
                                    "Item URI is not allowed. Item position: " + i + ". URI: "
                                            + item.getUri());
                        }
                    } else {
                        if (item.getUri() == null || mAllowedClipDataUri.test(item.getUri())) {
                            itemUri = item.getUri();
                        } else {
                            penalty.accept(
                                    "Item URI is not allowed. Item position: " + i + ". URI: "
                                            + item.getUri());
                        }
                    }

                    if (itemText != null || itemUri != null) {
                        if (newClipData == null) {
                            newClipData = new ClipData(clipData.getDescription(),
                                    new ClipData.Item(itemText, null, itemUri));
                        } else {
                            newClipData.addItem(new ClipData.Item(itemText, null, itemUri));
                        }
                    }
                }
                if (newClipData != null) {
                    out.setClipData(newClipData);
                }
            }
        }

        private static void checkOtherMembers(int i, ClipData.Item item, Consumer<String> penalty) {
            if (item.getHtmlText() != null || item.getIntent() != null) {
                penalty.accept("ClipData item at position " + i + " contains htmlText, "
                        + "textLinks or intent: " + item);
            }
        }

        @RequiresApi(31)
        private static class Api31Impl {
            private Api31Impl() {
            }

            @DoNotInline
            static void checkOtherMembers(int i, ClipData.Item item, Consumer<String> penalty) {
                if (item.getHtmlText() != null || item.getIntent() != null
                        || item.getTextLinks() != null) {
                    penalty.accept("ClipData item at position " + i + " contains htmlText, "
                            + "textLinks or intent: " + item);
                }
            }
        }
    }

    @RequiresApi(29)
    private static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Intent setIdentifier(Intent intent, String identifier) {
            return intent.setIdentifier(identifier);
        }

        @DoNotInline
        static String getIdentifier(Intent intent) {
            return intent.getIdentifier();
        }
    }
}

