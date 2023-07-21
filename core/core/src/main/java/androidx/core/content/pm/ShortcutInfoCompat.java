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
package androidx.core.content.pm;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;
import androidx.core.app.Person;
import androidx.core.content.LocusIdCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.net.UriCompat;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper for accessing features in {@link ShortcutInfo}.
 */
public class ShortcutInfoCompat {

    private static final String EXTRA_PERSON_COUNT = "extraPersonCount";
    private static final String EXTRA_PERSON_ = "extraPerson_";
    private static final String EXTRA_LOCUS_ID = "extraLocusId";
    private static final String EXTRA_LONG_LIVED = "extraLongLived";

    private static final String EXTRA_SLICE_URI = "extraSliceUri";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(flag = true, value = {SURFACE_LAUNCHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Surface {}

    /**
     * Indicates system surfaces managed by a launcher app. e.g. Long-Press Menu.
     */
    public static final int SURFACE_LAUNCHER = 1 << 0;

    Context mContext;
    String mId;
    String mPackageName;
    Intent[] mIntents;
    ComponentName mActivity;

    CharSequence mLabel;
    CharSequence mLongLabel;
    CharSequence mDisabledMessage;

    IconCompat mIcon;
    boolean mIsAlwaysBadged;

    Person[] mPersons;
    Set<String> mCategories;

    @Nullable
    LocusIdCompat mLocusId;
    // TODO: Support |auto| when the value of mIsLongLived is not set
    boolean mIsLongLived;

    int mRank;

    PersistableBundle mExtras;
    Bundle mTransientExtras;

    // Read-Only fields
    long mLastChangedTimestamp;
    UserHandle mUser;
    boolean mIsCached;
    boolean mIsDynamic;
    boolean mIsPinned;
    boolean mIsDeclaredInManifest;
    boolean mIsImmutable;
    boolean mIsEnabled = true;
    boolean mHasKeyFieldsOnly;
    int mDisabledReason;
    int mExcludedSurfaces;

    ShortcutInfoCompat() { }

    /**
     * @return {@link ShortcutInfo} object from this compat object.
     */
    @RequiresApi(25)
    public ShortcutInfo toShortcutInfo() {
        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(mContext, mId)
                .setShortLabel(mLabel)
                .setIntents(mIntents);
        if (mIcon != null) {
            builder.setIcon(mIcon.toIcon(mContext));
        }
        if (!TextUtils.isEmpty(mLongLabel)) {
            builder.setLongLabel(mLongLabel);
        }
        if (!TextUtils.isEmpty(mDisabledMessage)) {
            builder.setDisabledMessage(mDisabledMessage);
        }
        if (mActivity != null) {
            builder.setActivity(mActivity);
        }
        if (mCategories != null) {
            builder.setCategories(mCategories);
        }
        builder.setRank(mRank);
        if (mExtras != null) {
            builder.setExtras(mExtras);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            if (mPersons != null && mPersons.length > 0) {
                android.app.Person[] persons = new android.app.Person[mPersons.length];
                for (int i = 0; i < persons.length; i++) {
                    persons[i] = mPersons[i].toAndroidPerson();
                }
                builder.setPersons(persons);
            }
            if (mLocusId != null) {
                builder.setLocusId(mLocusId.toLocusId());
            }
            builder.setLongLived(mIsLongLived);
        } else {
            // ShortcutInfo.Builder#setPersons(...) and ShortcutInfo.Builder#setLongLived(...) are
            // introduced in API 29. On older API versions, we store mPersons and mIsLongLived in
            // the extras field of ShortcutInfo for backwards compatibility.
            builder.setExtras(buildLegacyExtrasBundle());
        }
        if (Build.VERSION.SDK_INT >= 33) {
            Api33Impl.setExcludedFromSurfaces(builder, mExcludedSurfaces);
        }
        return builder.build();
    }

    /**
     */
    @RequiresApi(22)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    private PersistableBundle buildLegacyExtrasBundle() {
        if (mExtras == null) {
            mExtras = new PersistableBundle();
        }
        if (mPersons != null && mPersons.length > 0) {
            mExtras.putInt(EXTRA_PERSON_COUNT, mPersons.length);
            for (int i = 0; i < mPersons.length; i++) {
                mExtras.putPersistableBundle(EXTRA_PERSON_ + (i + 1),
                        mPersons[i].toPersistableBundle());
            }
        }
        if (mLocusId != null) {
            mExtras.putString(EXTRA_LOCUS_ID, mLocusId.getId());
        }
        mExtras.putBoolean(EXTRA_LONG_LIVED, mIsLongLived);
        return mExtras;
    }

    Intent addToIntent(Intent outIntent) {
        outIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, mIntents[mIntents.length - 1])
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, mLabel.toString());
        if (mIcon != null) {
            Drawable badge = null;
            if (mIsAlwaysBadged) {
                PackageManager pm = mContext.getPackageManager();
                if (mActivity != null) {
                    try {
                        badge = pm.getActivityIcon(mActivity);
                    } catch (PackageManager.NameNotFoundException e) {
                        // Ignore
                    }
                }
                if (badge == null) {
                    badge = mContext.getApplicationInfo().loadIcon(pm);
                }
            }
            mIcon.addToShortcutIntent(outIntent, badge, mContext);
        }
        return outIntent;
    }

    /**
     * Returns the ID of a shortcut.
     *
     * <p>Shortcut IDs are unique within each publisher app and must be stable across
     * devices so that shortcuts will still be valid when restored on a different device.
     * See {@link android.content.pm.ShortcutManager} for details.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Return the package name of the publisher app.
     */
    @NonNull
    public String getPackage() {
        return mPackageName;
    }

    /**
     * Return the target activity.
     *
     * <p>This has nothing to do with the activity that this shortcut will launch.
     * Launcher apps should show the launcher icon for the returned activity alongside
     * this shortcut.
     *
     * @see Builder#setActivity(ComponentName)
     */
    @Nullable
    public ComponentName getActivity() {
        return mActivity;
    }

    /**
     * Return the short description of a shortcut.
     *
     * @see Builder#setShortLabel(CharSequence)
     */
    @NonNull
    public CharSequence getShortLabel() {
        return mLabel;
    }

    /**
     * Return the long description of a shortcut.
     *
     * @see Builder#setLongLabel(CharSequence)
     */
    @Nullable
    public CharSequence getLongLabel() {
        return mLongLabel;
    }

    /**
     * Return the message that should be shown when the user attempts to start a shortcut
     * that is disabled.
     *
     * @see Builder#setDisabledMessage(CharSequence)
     */
    @Nullable
    public CharSequence getDisabledMessage() {
        return mDisabledMessage;
    }

    /**
     * Returns why a shortcut has been disabled.
     */
    public int getDisabledReason() {
        return mDisabledReason;
    }

    /**
     * Returns the intent that is executed when the user selects this shortcut.
     * If setIntents() was used, then return the last intent in the array.
     *
     * @see Builder#setIntent(Intent)
     */
    @NonNull
    public Intent getIntent() {
        return mIntents[mIntents.length - 1];
    }

    /**
     * Return the intent set with {@link Builder#setIntents(Intent[])}.
     *
     * @see Builder#setIntents(Intent[])
     */
    @NonNull
    public Intent[] getIntents() {
        return Arrays.copyOf(mIntents, mIntents.length);
    }

    /**
     * Return the categories set with {@link Builder#setCategories(Set)}.
     *
     * @see Builder#setCategories(Set)
     */
    @Nullable
    public Set<String> getCategories() {
        return mCategories;
    }

    /**
     * Gets the {@link LocusIdCompat} associated with this shortcut.
     *
     * <p>Used by the device's intelligence services to correlate objects (such as
     * {@link androidx.core.app.NotificationCompat} and
     * {@link android.view.contentcapture.ContentCaptureContext}) that are correlated.
     */
    @Nullable
    public LocusIdCompat getLocusId() {
        return mLocusId;
    }

    /**
     * Returns the rank of the shortcut set with {@link Builder#setRank(int)}.
     *
     * @see Builder#setRank(int)
     */
    public int getRank() {
        return mRank;
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public IconCompat getIcon() {
        return mIcon;
    }

    /**
     */
    @RequiresApi(25)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @VisibleForTesting
    @Nullable
    static Person[] getPersonsFromExtra(@NonNull PersistableBundle bundle) {
        if (bundle == null || !bundle.containsKey(EXTRA_PERSON_COUNT)) {
            return null;
        }

        int personsLength = bundle.getInt(EXTRA_PERSON_COUNT);
        Person[] persons = new Person[personsLength];
        for (int i = 0; i < personsLength; i++) {
            persons[i] = Person.fromPersistableBundle(
                    bundle.getPersistableBundle(EXTRA_PERSON_ + (i + 1)));
        }
        return persons;
    }

    /**
     */
    @RequiresApi(25)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @VisibleForTesting
    static boolean getLongLivedFromExtra(@Nullable PersistableBundle bundle) {
        if (bundle == null || !bundle.containsKey(EXTRA_LONG_LIVED)) {
            return false;
        }
        return bundle.getBoolean(EXTRA_LONG_LIVED);
    }

    /**
     */
    @RequiresApi(25)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    static List<ShortcutInfoCompat> fromShortcuts(@NonNull final Context context,
            @NonNull final List<ShortcutInfo> shortcuts) {
        final List<ShortcutInfoCompat> results = new ArrayList<>(shortcuts.size());
        for (ShortcutInfo s : shortcuts) {
            results.add(new ShortcutInfoCompat.Builder(context, s).build());
        }
        return results;
    }

    @Nullable
    public PersistableBundle getExtras() {
        return mExtras;
    }

    /**
     * Get additional extras from the shortcut, which will not be persisted anywhere once the
     * shortcut is published.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Nullable
    public Bundle getTransientExtras() {
        return mTransientExtras;
    }

    /**
     * {@link UserHandle} on which the publisher created this shortcut.
     */
    @Nullable
    public UserHandle getUserHandle() {
        return mUser;
    }

    /**
     * Last time when any of the fields was updated.
     */
    public long getLastChangedTimestamp() {
        return mLastChangedTimestamp;
    }

    /** Return whether a shortcut is cached. */
    public boolean isCached() {
        return mIsCached;
    }

    /** Return whether a shortcut is dynamic. */
    public boolean isDynamic() {
        return mIsDynamic;
    }

    /** Return whether a shortcut is pinned. */
    public boolean isPinned() {
        return mIsPinned;
    }

    /**
     * Return whether a shortcut is static; that is, whether a shortcut is
     * published from AndroidManifest.xml.  If {@code true}, the shortcut is
     * also {@link #isImmutable()}.
     *
     * <p>When an app is upgraded and a shortcut is no longer published from AndroidManifest.xml,
     * this will be set to {@code false}.  If the shortcut is not pinned, then it'll disappear.
     * However, if it's pinned, it will still be visible, {@link #isEnabled()} will be
     * {@code false} and {@link #isImmutable()} will be {@code true}.
     */
    public boolean isDeclaredInManifest() {
        return mIsDeclaredInManifest;
    }

    /**
     * Return if a shortcut is immutable, in which case it cannot be modified with any of
     * {@link ShortcutManagerCompat} APIs.
     *
     * <p>All static shortcuts are immutable.  When a static shortcut is pinned and is then
     * disabled because it doesn't appear in AndroidManifest.xml for a newer version of the
     * app, {@link #isDeclaredInManifest} returns {@code false}, but the shortcut is still
     * immutable.
     *
     * <p>All shortcuts originally published via the {@link ShortcutManager} APIs
     * are all mutable.
     */
    public boolean isImmutable() {
        return mIsImmutable;
    }

    /**
     * Returns {@code false} if a shortcut is disabled with
     * {@link ShortcutManagerCompat#disableShortcuts}.
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * Return whether a shortcut only contains "key" information only or not.  If true, only the
     * following fields are available.
     * <ul>
     *     <li>{@link #getId()}
     *     <li>{@link #getPackage()}
     *     <li>{@link #getActivity()}
     *     <li>{@link #getLastChangedTimestamp()}
     *     <li>{@link #isDynamic()}
     *     <li>{@link #isPinned()}
     *     <li>{@link #isDeclaredInManifest()}
     *     <li>{@link #isImmutable()}
     *     <li>{@link #isEnabled()}
     *     <li>{@link #getUserHandle()}
     * </ul>
     */
    public boolean hasKeyFieldsOnly() {
        return mHasKeyFieldsOnly;
    }

    @RequiresApi(25)
    @Nullable
    static LocusIdCompat getLocusId(@NonNull final ShortcutInfo shortcutInfo) {
        if (Build.VERSION.SDK_INT >= 29) {
            if (shortcutInfo.getLocusId() == null) return null;
            return LocusIdCompat.toLocusIdCompat(shortcutInfo.getLocusId());
        } else {
            return getLocusIdFromExtra(shortcutInfo.getExtras());
        }
    }

    /**
     * Return true if the shortcut is excluded from specified surface.
     */
    public boolean isExcludedFromSurfaces(@Surface int surface) {
        return (mExcludedSurfaces & surface) != 0;
    }

    /**
     * Returns a bitmask of all surfaces this shortcut is excluded from.
     *
     * @see ShortcutInfo.Builder#setExcludedFromSurfaces(int)
     */
    @Surface
    public int getExcludedFromSurfaces() {
        return mExcludedSurfaces;
    }

    /**
     */
    @RequiresApi(25)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Nullable
    private static LocusIdCompat getLocusIdFromExtra(@Nullable PersistableBundle bundle) {
        if (bundle == null) return null;
        final String locusId = bundle.getString(EXTRA_LOCUS_ID);
        return locusId == null ? null : new LocusIdCompat(locusId);
    }

    /**
     * Builder class for {@link ShortcutInfoCompat} objects.
     */
    public static class Builder {

        private final ShortcutInfoCompat mInfo;
        private boolean mIsConversation;
        private Set<String> mCapabilityBindings;
        private Map<String, Map<String, List<String>>> mCapabilityBindingParams;
        private Uri mSliceUri;

        public Builder(@NonNull Context context, @NonNull String id) {
            mInfo = new ShortcutInfoCompat();
            mInfo.mContext = context;
            mInfo.mId = id;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public Builder(@NonNull ShortcutInfoCompat shortcutInfo) {
            mInfo = new ShortcutInfoCompat();
            mInfo.mContext = shortcutInfo.mContext;
            mInfo.mId = shortcutInfo.mId;
            mInfo.mPackageName = shortcutInfo.mPackageName;
            mInfo.mIntents = Arrays.copyOf(shortcutInfo.mIntents, shortcutInfo.mIntents.length);
            mInfo.mActivity = shortcutInfo.mActivity;
            mInfo.mLabel = shortcutInfo.mLabel;
            mInfo.mLongLabel = shortcutInfo.mLongLabel;
            mInfo.mDisabledMessage = shortcutInfo.mDisabledMessage;
            mInfo.mDisabledReason = shortcutInfo.mDisabledReason;
            mInfo.mIcon = shortcutInfo.mIcon;
            mInfo.mIsAlwaysBadged = shortcutInfo.mIsAlwaysBadged;
            mInfo.mUser = shortcutInfo.mUser;
            mInfo.mLastChangedTimestamp = shortcutInfo.mLastChangedTimestamp;
            mInfo.mIsCached = shortcutInfo.mIsCached;
            mInfo.mIsDynamic = shortcutInfo.mIsDynamic;
            mInfo.mIsPinned = shortcutInfo.mIsPinned;
            mInfo.mIsDeclaredInManifest = shortcutInfo.mIsDeclaredInManifest;
            mInfo.mIsImmutable = shortcutInfo.mIsImmutable;
            mInfo.mIsEnabled = shortcutInfo.mIsEnabled;
            mInfo.mLocusId = shortcutInfo.mLocusId;
            mInfo.mIsLongLived = shortcutInfo.mIsLongLived;
            mInfo.mHasKeyFieldsOnly = shortcutInfo.mHasKeyFieldsOnly;
            mInfo.mRank = shortcutInfo.mRank;
            if (shortcutInfo.mPersons != null) {
                mInfo.mPersons = Arrays.copyOf(shortcutInfo.mPersons, shortcutInfo.mPersons.length);
            }
            if (shortcutInfo.mCategories != null) {
                mInfo.mCategories = new HashSet<>(shortcutInfo.mCategories);
            }
            if (shortcutInfo.mExtras != null) {
                mInfo.mExtras = shortcutInfo.mExtras;
            }
            mInfo.mExcludedSurfaces = shortcutInfo.mExcludedSurfaces;
        }

        /**
         */
        @RequiresApi(25)
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public Builder(@NonNull Context context, @NonNull ShortcutInfo shortcutInfo) {
            mInfo = new ShortcutInfoCompat();
            mInfo.mContext = context;
            mInfo.mId = shortcutInfo.getId();
            mInfo.mPackageName = shortcutInfo.getPackage();
            Intent[] intents = shortcutInfo.getIntents();
            mInfo.mIntents = Arrays.copyOf(intents, intents.length);
            mInfo.mActivity = shortcutInfo.getActivity();
            mInfo.mLabel = shortcutInfo.getShortLabel();
            mInfo.mLongLabel = shortcutInfo.getLongLabel();
            mInfo.mDisabledMessage = shortcutInfo.getDisabledMessage();
            if (Build.VERSION.SDK_INT >= 28) {
                mInfo.mDisabledReason = shortcutInfo.getDisabledReason();
            } else {
                mInfo.mDisabledReason = shortcutInfo.isEnabled()
                        ? ShortcutInfo.DISABLED_REASON_NOT_DISABLED
                        : ShortcutInfo.DISABLED_REASON_UNKNOWN;
            }
            mInfo.mCategories = shortcutInfo.getCategories();
            mInfo.mPersons = ShortcutInfoCompat.getPersonsFromExtra(shortcutInfo.getExtras());
            mInfo.mUser = shortcutInfo.getUserHandle();
            mInfo.mLastChangedTimestamp = shortcutInfo.getLastChangedTimestamp();
            if (Build.VERSION.SDK_INT >= 30) {
                mInfo.mIsCached = shortcutInfo.isCached();
            }
            mInfo.mIsDynamic = shortcutInfo.isDynamic();
            mInfo.mIsPinned = shortcutInfo.isPinned();
            mInfo.mIsDeclaredInManifest = shortcutInfo.isDeclaredInManifest();
            mInfo.mIsImmutable = shortcutInfo.isImmutable();
            mInfo.mIsEnabled = shortcutInfo.isEnabled();
            mInfo.mHasKeyFieldsOnly = shortcutInfo.hasKeyFieldsOnly();
            mInfo.mLocusId = ShortcutInfoCompat.getLocusId(shortcutInfo);
            mInfo.mRank = shortcutInfo.getRank();
            mInfo.mExtras = shortcutInfo.getExtras();
        }

        /**
         * Sets the short title of a shortcut.
         *
         * <p>This is a mandatory field when publishing a new shortcut.
         *
         * <p>This field is intended to be a concise description of a shortcut.
         *
         * <p>The recommended maximum length is 10 characters.
         */
        @NonNull
        public Builder setShortLabel(@NonNull CharSequence shortLabel) {
            mInfo.mLabel = shortLabel;
            return this;
        }

        /**
         * Sets the text of a shortcut.
         *
         * <p>This field is intended to be more descriptive than the shortcut title. The launcher
         * shows this instead of the short title when it has enough space.
         *
         * <p>The recommend maximum length is 25 characters.
         */
        @NonNull
        public Builder setLongLabel(@NonNull CharSequence longLabel) {
            mInfo.mLongLabel = longLabel;
            return this;
        }

        /**
         * Sets the message that should be shown when the user attempts to start a shortcut that
         * is disabled.
         *
         * @see ShortcutInfo#getDisabledMessage()
         */
        @NonNull
        public Builder setDisabledMessage(@NonNull CharSequence disabledMessage) {
            mInfo.mDisabledMessage = disabledMessage;
            return this;
        }

        /**
         * Sets the intent of a shortcut.  Alternatively, {@link #setIntents(Intent[])} can be used
         * to launch an activity with other activities in the back stack.
         *
         * <p>This is a mandatory field when publishing a new shortcut.
         *
         * <p>The given {@code intent} can contain extras, but these extras must contain values
         * of primitive types in order for the system to persist these values.
         */
        @NonNull
        public Builder setIntent(@NonNull Intent intent) {
            return setIntents(new Intent[]{intent});
        }

        /**
         * Sets multiple intents instead of a single intent, in order to launch an activity with
         * other activities in back stack.  Use {@link android.app.TaskStackBuilder} to build
         * intents. The last element in the list represents the only intent that doesn't place
         * an activity on the back stack.
         */
        @NonNull
        public Builder setIntents(@NonNull Intent[] intents) {
            mInfo.mIntents = intents;
            return this;
        }

        /**
         * Sets an icon of a shortcut.
         */
        @NonNull
        public Builder setIcon(IconCompat icon) {
            mInfo.mIcon = icon;
            return this;
        }

        /**
         * Sets the {@link LocusIdCompat} associated with this shortcut.
         *
         * <p>This method should be called when the {@link LocusIdCompat} is used in other places
         * (such as {@link androidx.core.app.NotificationCompat} and
         * {@link android.view.contentcapture.ContentCaptureContext}) so the device's intelligence
         * services can correlate them.
         */
        @NonNull
        public Builder setLocusId(@Nullable final LocusIdCompat locusId) {
            mInfo.mLocusId = locusId;
            return this;
        }

        /**
         * Sets the corresponding fields indicating this shortcut is aimed for conversation.
         *
         * <p>
         * If the shortcut is not associated with a {@link LocusIdCompat}, a {@link LocusIdCompat}
         * based on {@link ShortcutInfoCompat#getId()} will be added upon {@link #build()}
         * <p>
         * Additionally, the shortcut will be long-lived.
         * @see #setLongLived(boolean)
         */
        @NonNull
        public Builder setIsConversation() {
            mIsConversation = true;
            return this;
        }

        /**
         * Sets the target activity. A shortcut will be shown along with this activity's icon
         * on the launcher.
         *
         * @see ShortcutInfo#getActivity()
         * @see ShortcutInfo.Builder#setActivity(ComponentName)
         */
        @NonNull
        public Builder setActivity(@NonNull ComponentName activity) {
            mInfo.mActivity = activity;
            return this;
        }

        /**
         * Badges the icon before passing it over to the Launcher.
         * <p>
         * Launcher automatically badges {@link ShortcutInfo}, so only the legacy shortcut icon,
         * {@link Intent.ShortcutIconResource} is badged. This field is ignored when using
         * {@link ShortcutInfo} on API 25 and above.
         * <p>
         * If the shortcut is associated with an activity, the activity icon is used as the badge,
         * otherwise application icon is used.
         *
         * @see #setActivity(ComponentName)
         */
        @NonNull
        public Builder setAlwaysBadged() {
            mInfo.mIsAlwaysBadged = true;
            return this;
        }

        /**
         * Associate a person to a shortcut. Alternatively, {@link #setPersons(Person[])} can be
         * used to add multiple persons to a shortcut.
         *
         * <p>This is an optional field when publishing a new shortcut.
         *
         * @see Person
         */
        @NonNull
        public Builder setPerson(@NonNull Person person) {
            return setPersons(new Person[]{person});
        }

        /**
         * Sets multiple persons instead of a single person.
         */
        @NonNull
        public Builder setPersons(@NonNull Person[] persons) {
            mInfo.mPersons = persons;
            return this;
        }

        /**
         * Sets categories for a shortcut.
         * <ul>
         * <li>Launcher apps may use this information to categorize shortcuts
         * <li> Used by the system to associate a published Sharing Shortcut with supported
         * mimeTypes. Required for published Sharing Shortcuts with a matching category
         * declared in share targets, defined in the app's manifest linked shortcuts xml file.
         * </ul>
         *
         * @see ShortcutInfo#getCategories()
         */
        @NonNull
        public Builder setCategories(@NonNull Set<String> categories) {
            ArraySet<String> set = new ArraySet<>();
            set.addAll(categories);
            mInfo.mCategories = set;
            return this;
        }

        /**
         * @deprecated Use {@ink #setLongLived(boolean)) instead.
         */
        @Deprecated
        @NonNull
        public Builder setLongLived() {
            mInfo.mIsLongLived = true;
            return this;
        }

        /**
         * Sets if a shortcut would be valid even if it has been unpublished/invisible by the app
         * (as a dynamic or pinned shortcut). If it is long lived, it can be cached by various
         * system services even after it has been unpublished as a dynamic shortcut.
         */
        @NonNull
        public Builder setLongLived(boolean longLived) {
            mInfo.mIsLongLived = longLived;
            return this;
        }

        /**
         * Sets which surfaces a shortcut will be excluded from.
         *
         * If the shortcut is set to be excluded from {@link #SURFACE_LAUNCHER}, shortcuts will be
         * excluded from the search result of {@link android.content.pm.LauncherApps#getShortcuts(
         * android.content.pm.LauncherApps.ShortcutQuery, UserHandle)} and
         * {@link android.content.pm.ShortcutManager#getShortcuts(int)}. This generally means the
         * shortcut would not be displayed by a launcher app (e.g. in Long-Press menu), while
         * remain visible in other surfaces such as assistant or on-device-intelligence.
         *
         * <p>On API <= 31, shortcuts that are excluded from {@link #SURFACE_LAUNCHER} are not
         * actually sent to {@link ShortcutManager}. These shortcuts might still be made
         * available to other surfaces via alternative means.
         */
        @NonNull
        public Builder setExcludedFromSurfaces(final int surfaces) {
            mInfo.mExcludedSurfaces = surfaces;
            return this;
        }

        /**
         * Sets rank of a shortcut, which is a non-negative value that's used by the system to sort
         * shortcuts. Lower value means higher importance.
         *
         * @see ShortcutInfo#getRank() for details.
         */
        @NonNull
        public Builder setRank(int rank) {
            mInfo.mRank = rank;
            return this;
        }

        /**
         * Extras that the app can set for any purpose.
         *
         * <p>Apps can store arbitrary shortcut metadata in extras and retrieve the
         * metadata later using {@link ShortcutInfo#getExtras()}.
         *
         * @see ShortcutInfo#getExtras
         */
        @NonNull
        public Builder setExtras(@NonNull PersistableBundle extras) {
            mInfo.mExtras = extras;
            return this;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @NonNull
        public Builder setTransientExtras(@NonNull final Bundle transientExtras) {
            mInfo.mTransientExtras = Preconditions.checkNotNull(transientExtras);
            return this;
        }

        /**
         * Associates a shortcut with a capability without any parameters. Used when the shortcut is
         * an instance of a capability.
         *
         * <P>This method can be called multiple times to associate multiple capabilities with
         * this shortcut.
         *
         * @param capability capability associated with the shortcut. e.g. actions.intent
         *                   .START_EXERCISE.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addCapabilityBinding(@NonNull String capability) {
            if (mCapabilityBindings == null) {
                mCapabilityBindings = new HashSet<>();
            }
            mCapabilityBindings.add(capability);
            return this;
        }

        /**
         * Associates a shortcut with a capability, and a parameter of that capability. Used when
         * the shortcut is an instance of a capability.
         *
         * <P>This method can be called multiple times to associate multiple capabilities with
         * this shortcut, or add multiple parameters to the same capability.
         *
         * @param capability capability associated with the shortcut. e.g. actions.intent
         *                   .START_EXERCISE.
         * @param parameter the parameter associated with the capability. e.g. exercise.name.
         * @param parameterValues a list of values for that parameters. The first value will be
         *                        the primary name, while the rest will be alternative names. If
         *                        the values are empty, then the parameter will not be saved in
         *                        the shortcut.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addCapabilityBinding(@NonNull String capability,
                @NonNull String parameter, @NonNull List<String> parameterValues) {
            addCapabilityBinding(capability);

            if (!parameterValues.isEmpty()) {
                if (mCapabilityBindingParams == null) {
                    mCapabilityBindingParams = new HashMap<>();
                }
                if (mCapabilityBindingParams.get(capability) == null) {
                    mCapabilityBindingParams.put(capability, new HashMap<String, List<String>>());
                }

                mCapabilityBindingParams.get(capability).put(parameter, parameterValues);
            }
            return this;
        }

        /**
         * Sets the slice uri for a shortcut. The uri will be used if this shortcuts represents a
         * slice, instead of an intent.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setSliceUri(@NonNull Uri sliceUri) {
            mSliceUri = sliceUri;
            return this;
        }

        /**
         * Creates a {@link ShortcutInfoCompat} instance.
         */
        @NonNull
        public ShortcutInfoCompat build() {
            // Verify the arguments
            if (TextUtils.isEmpty(mInfo.mLabel)) {
                throw new IllegalArgumentException("Shortcut must have a non-empty label");
            }
            if (mInfo.mIntents == null || mInfo.mIntents.length == 0) {
                throw new IllegalArgumentException("Shortcut must have an intent");
            }
            if (mIsConversation) {
                if (mInfo.mLocusId == null) {
                    mInfo.mLocusId = new LocusIdCompat(mInfo.mId);
                }
                mInfo.mIsLongLived = true;
            }

            if (mCapabilityBindings != null) {
                if (mInfo.mCategories == null) {
                    mInfo.mCategories = new HashSet<>();
                }
                mInfo.mCategories.addAll(mCapabilityBindings);
            }
            if (Build.VERSION.SDK_INT >= 21) {
                if (mCapabilityBindingParams != null) {
                    if (mInfo.mExtras == null) {
                        mInfo.mExtras = new PersistableBundle();
                    }
                    for (String capability : mCapabilityBindingParams.keySet()) {
                        final Map<String, List<String>> params =
                                mCapabilityBindingParams.get(capability);
                        final Set<String> paramNames = params.keySet();
                        // Persist the mapping of <Capability1> -> [<Param1>, <Param2> ... ]
                        mInfo.mExtras.putStringArray(
                                capability, paramNames.toArray(new String[0]));
                        // Persist the capability param in respect to capability
                        // i.e. <Capability1/Param1> -> [<Value1>, <Value2> ... ]
                        for (String paramName : params.keySet()) {
                            final List<String> value = params.get(paramName);
                            mInfo.mExtras.putStringArray(capability + "/" + paramName,
                                    value == null ? new String[0] : value.toArray(new String[0]));
                        }
                    }
                }
                if (mSliceUri != null) {
                    if (mInfo.mExtras == null) {
                        mInfo.mExtras = new PersistableBundle();
                    }
                    mInfo.mExtras.putString(EXTRA_SLICE_URI, UriCompat.toSafeString(mSliceUri));
                }
            }
            return mInfo;
        }
    }

    @RequiresApi(33)
    private static class Api33Impl {
        static void setExcludedFromSurfaces(@NonNull final ShortcutInfo.Builder builder,
                final int surfaces) {
            builder.setExcludedFromSurfaces(surfaces);
        }
    }
}
