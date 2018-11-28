/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.preference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The basic building block that represents an individual setting displayed to a user in the
 * preference hierarchy. This class provides the data that will be displayed to the user and has
 * a reference to the {@link SharedPreferences} or {@link PreferenceDataStore} instance that
 * persists the preference's values.
 *
 * <p>When specifying a preference hierarchy in XML, each element can point to a subclass of
 * {@link Preference}, similar to the view hierarchy and layouts.
 *
 * <p>This class contains a {@code key} that that represents the key that is used to persist the
 * value to the device. It is up to the subclass to decide how to store the value.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about building a settings screen using the AndroidX Preference library, see
 * <a href="{@docRoot}guide/topics/ui/settings.html">Settings</a>.</p>
 * </div>
 *
 * @attr name android:icon
 * @attr name android:key
 * @attr name android:title
 * @attr name android:summary
 * @attr name android:order
 * @attr name android:fragment
 * @attr name android:layout
 * @attr name android:widgetLayout
 * @attr name android:enabled
 * @attr name android:selectable
 * @attr name android:dependency
 * @attr name android:persistent
 * @attr name android:defaultValue
 * @attr name android:shouldDisableView
 * @attr name android:singleLineTitle
 * @attr name android:iconSpaceReserved
 */
public class Preference implements Comparable<Preference> {
    /**
     * Specify for {@link #setOrder(int)} if a specific order is not required.
     */
    public static final int DEFAULT_ORDER = Integer.MAX_VALUE;

    private static final String CLIPBOARD_ID = "Preference";

    private Context mContext;

    @Nullable
    private PreferenceManager mPreferenceManager;

    /**
     * The data store that should be used by this preference to store / retrieve data. If {@code
     * null} then {@link PreferenceManager#getPreferenceDataStore()} needs to be checked. If that
     * one is {@code null} too it means that we are using {@link SharedPreferences} to store the
     * data.
     */
    @Nullable
    private PreferenceDataStore mPreferenceDataStore;

    /**
     * Set when added to hierarchy since we need a unique ID within that hierarchy.
     */
    private long mId;

    /**
     * Set true temporarily to keep {@link #onAttachedToHierarchy(PreferenceManager)} from
     * overwriting mId.
     */
    private boolean mHasId;

    private OnPreferenceChangeListener mOnChangeListener;
    private OnPreferenceClickListener mOnClickListener;

    private int mOrder = DEFAULT_ORDER;
    private int mViewId = 0;
    private CharSequence mTitle;
    private CharSequence mSummary;

    /**
     * mIconResId is overridden by mIcon, if mIcon is specified.
     */
    private int mIconResId;
    private Drawable mIcon;
    private String mKey;
    private Intent mIntent;
    private String mFragment;
    private Bundle mExtras;
    private boolean mEnabled = true;
    private boolean mSelectable = true;
    private boolean mRequiresKey;
    private boolean mPersistent = true;
    private String mDependencyKey;
    private Object mDefaultValue;
    private boolean mDependencyMet = true;
    private boolean mParentDependencyMet = true;
    private boolean mVisible = true;

    private boolean mAllowDividerAbove = true;
    private boolean mAllowDividerBelow = true;
    private boolean mHasSingleLineTitleAttr;
    private boolean mSingleLineTitle = true;
    private boolean mIconSpaceReserved;
    private boolean mCopyingEnabled;

    /**
     * @see #setShouldDisableView(boolean)
     */
    private boolean mShouldDisableView = true;

    private int mLayoutResId = R.layout.preference;
    private int mWidgetLayoutResId;

    private OnPreferenceChangeInternalListener mListener;

    private List<Preference> mDependents;
    private PreferenceGroup mParentGroup;

    private boolean mWasDetached;
    private boolean mBaseMethodCalled;

    private OnPreferenceCopyListener mOnCopyListener;

    private SummaryProvider mSummaryProvider;

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            performClick(v);
        }
    };

    /**
     * Perform inflation from XML and apply a class-specific base style. This constructor allows
     * subclasses to use their own base style when they are inflating. For example, a
     * {@link CheckBoxPreference} constructor calls this version of the super class constructor
     * and supplies {@code android.R.attr.checkBoxPreferenceStyle} for <var>defStyleAttr</var>.
     * This allows the theme's checkbox preference style to modify all of the base preference
     * attributes as well as the {@link CheckBoxPreference} class's attributes.
     *
     * @param context      The {@link Context} this is associated with, through which it can
     *                     access the current theme, resources, {@link SharedPreferences}, etc.
     * @param attrs        The attributes of the XML tag that is inflating the preference
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default values for the view. Can be 0 to not
     *                     look for defaults.
     * @param defStyleRes  A resource identifier of a style resource that supplies default values
     *                     for the view, used only if defStyleAttr is 0 or can not be found in the
     *                     theme. Can be 0 to not look for defaults.
     * @see #Preference(Context, android.util.AttributeSet)
     */
    public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.Preference, defStyleAttr, defStyleRes);

        mIconResId = TypedArrayUtils.getResourceId(a, R.styleable.Preference_icon,
                R.styleable.Preference_android_icon, 0);

        mKey = TypedArrayUtils.getString(a, R.styleable.Preference_key,
                R.styleable.Preference_android_key);

        mTitle = TypedArrayUtils.getText(a, R.styleable.Preference_title,
                R.styleable.Preference_android_title);

        mSummary = TypedArrayUtils.getText(a, R.styleable.Preference_summary,
                R.styleable.Preference_android_summary);

        mOrder = TypedArrayUtils.getInt(a, R.styleable.Preference_order,
                R.styleable.Preference_android_order, DEFAULT_ORDER);

        mFragment = TypedArrayUtils.getString(a, R.styleable.Preference_fragment,
                R.styleable.Preference_android_fragment);

        mLayoutResId = TypedArrayUtils.getResourceId(a, R.styleable.Preference_layout,
                R.styleable.Preference_android_layout, R.layout.preference);

        mWidgetLayoutResId = TypedArrayUtils.getResourceId(a, R.styleable.Preference_widgetLayout,
                R.styleable.Preference_android_widgetLayout, 0);

        mEnabled = TypedArrayUtils.getBoolean(a, R.styleable.Preference_enabled,
                R.styleable.Preference_android_enabled, true);

        mSelectable = TypedArrayUtils.getBoolean(a, R.styleable.Preference_selectable,
                R.styleable.Preference_android_selectable, true);

        mPersistent = TypedArrayUtils.getBoolean(a, R.styleable.Preference_persistent,
                R.styleable.Preference_android_persistent, true);

        mDependencyKey = TypedArrayUtils.getString(a, R.styleable.Preference_dependency,
                R.styleable.Preference_android_dependency);

        mAllowDividerAbove = TypedArrayUtils.getBoolean(a, R.styleable.Preference_allowDividerAbove,
                R.styleable.Preference_allowDividerAbove, mSelectable);

        mAllowDividerBelow = TypedArrayUtils.getBoolean(a, R.styleable.Preference_allowDividerBelow,
                R.styleable.Preference_allowDividerBelow, mSelectable);

        if (a.hasValue(R.styleable.Preference_defaultValue)) {
            mDefaultValue = onGetDefaultValue(a, R.styleable.Preference_defaultValue);
        } else if (a.hasValue(R.styleable.Preference_android_defaultValue)) {
            mDefaultValue = onGetDefaultValue(a, R.styleable.Preference_android_defaultValue);
        }

        mShouldDisableView =
                TypedArrayUtils.getBoolean(a, R.styleable.Preference_shouldDisableView,
                        R.styleable.Preference_android_shouldDisableView, true);

        mHasSingleLineTitleAttr = a.hasValue(R.styleable.Preference_singleLineTitle);
        if (mHasSingleLineTitleAttr) {
            mSingleLineTitle = TypedArrayUtils.getBoolean(a, R.styleable.Preference_singleLineTitle,
                    R.styleable.Preference_android_singleLineTitle, true);
        }

        mIconSpaceReserved = TypedArrayUtils.getBoolean(a, R.styleable.Preference_iconSpaceReserved,
                R.styleable.Preference_android_iconSpaceReserved, false);

        mVisible = TypedArrayUtils.getBoolean(a, R.styleable.Preference_isPreferenceVisible,
                R.styleable.Preference_isPreferenceVisible, true);

        mCopyingEnabled = TypedArrayUtils.getBoolean(a, R.styleable.Preference_enableCopying,
                R.styleable.Preference_enableCopying, false);

        a.recycle();
    }

    /**
     * Perform inflation from XML and apply a class-specific base style. This constructor allows
     * subclasses to use their own base style when they are inflating. For example, a
     * {@link CheckBoxPreference} constructor calls this version of the super class constructor
     * and supplies {@code android.R.attr.checkBoxPreferenceStyle} for <var>defStyleAttr</var>.
     * This allows the theme's checkbox preference style to modify all of the base preference
     * attributes as well as the {@link CheckBoxPreference} class's attributes.
     *
     * @param context      The Context this is associated with, through which it can access the
     *                     current theme, resources, {@link SharedPreferences}, etc.
     * @param attrs        The attributes of the XML tag that is inflating the preference
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default values for the view. Can be 0 to not
     *                     look for defaults.
     * @see #Preference(Context, AttributeSet)
     */
    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Constructor that is called when inflating a preference from XML. This is called when a
     * preference is being constructed from an XML file, supplying attributes that were specified
     * in the XML file. This version uses a default style of 0, so the only attribute values
     * applied are those in the Context's Theme and the given AttributeSet.
     *
     * @param context The Context this is associated with, through which it can access the
     *                current theme, resources, {@link SharedPreferences}, etc.
     * @param attrs   The attributes of the XML tag that is inflating the preference
     * @see #Preference(Context, AttributeSet, int)
     */
    public Preference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    /**
     * Constructor to create a preference.
     *
     * @param context The Context this is associated with, through which it can access the
     *                current theme, resources, {@link SharedPreferences}, etc.
     */
    public Preference(Context context) {
        this(context, null);
    }

    /**
     * Called when a preference is being inflated and the default value attribute needs to be
     * read. Since different preference types have different value types, the subclass should get
     * and return the default value which will be its value type.
     *
     * <p>For example, if the value type is String, the body of the method would proxy to
     * {@link TypedArray#getString(int)}.
     *
     * @param a     The set of attributes
     * @param index The index of the default value attribute
     * @return The default value of this preference type
     */
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return null;
    }

    /**
     * Sets an {@link Intent} to be used for {@link Context#startActivity(Intent)} when this
     * preference is clicked.
     *
     * @param intent The intent associated with this preference
     */
    public void setIntent(Intent intent) {
        mIntent = intent;
    }

    /**
     * Return the {@link Intent} associated with this preference.
     *
     * @return The {@link Intent} last set via {@link #setIntent(Intent)} or XML
     */
    public Intent getIntent() {
        return mIntent;
    }

    /**
     * Sets the class name of a fragment to be shown when this preference is clicked.
     *
     * @param fragment The class name of the fragment associated with this preference
     */
    public void setFragment(String fragment) {
        mFragment = fragment;
    }

    /**
     * Return the fragment class name associated with this preference.
     *
     * @return The fragment class name last set via {@link #setFragment} or XML
     */
    public String getFragment() {
        return mFragment;
    }

    /**
     * Sets a {@link PreferenceDataStore} to be used by this preference instead of using
     * {@link SharedPreferences}.
     *
     * <p>The data store will remain assigned even if the preference is moved around the preference
     * hierarchy. It will also override a data store propagated from the {@link PreferenceManager}
     * that owns this preference.
     *
     * @param dataStore The {@link PreferenceDataStore} to be used by this preference
     * @see PreferenceManager#setPreferenceDataStore(PreferenceDataStore)
     */
    public void setPreferenceDataStore(PreferenceDataStore dataStore) {
        mPreferenceDataStore = dataStore;
    }

    /**
     * Returns {@link PreferenceDataStore} used by this preference. Returns {@code null} if
     * {@link SharedPreferences} is used instead.
     *
     * <p>By default preferences always use {@link SharedPreferences}. To make this
     * preference to use the {@link PreferenceDataStore} you need to assign your implementation
     * to the preference itself via {@link #setPreferenceDataStore(PreferenceDataStore)} or to its
     * {@link PreferenceManager} via
     * {@link PreferenceManager#setPreferenceDataStore(PreferenceDataStore)}.
     *
     * @return The {@link PreferenceDataStore} used by this preference or {@code null} if none
     */
    @Nullable
    public PreferenceDataStore getPreferenceDataStore() {
        if (mPreferenceDataStore != null) {
            return mPreferenceDataStore;
        } else if (mPreferenceManager != null) {
            return mPreferenceManager.getPreferenceDataStore();
        }

        return null;
    }

    /**
     * Return the extras Bundle object associated with this preference, creating a new Bundle if
     * there currently isn't one. You can use this to get and set individual extra key/value pairs.
     */
    public Bundle getExtras() {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        return mExtras;
    }

    /**
     * Return the extras Bundle object associated with this preference, returning {@code null} if
     * there is not currently one.
     */
    public Bundle peekExtras() {
        return mExtras;
    }

    /**
     * Sets the layout resource that is inflated as the {@link View} to be shown for this
     * preference. In most cases, the default layout is sufficient for custom preference objects
     * and only the widget layout needs to be changed.
     *
     * <p>This layout should contain a {@link ViewGroup} with ID
     * {@link android.R.id#widget_frame} to be the parent of the specific widget for this
     * preference. It should similarly contain {@link android.R.id#title} and
     * {@link android.R.id#summary}.
     *
     * <p>It is an error to change the layout after adding the preference to a
     * {@link PreferenceGroup}.
     *
     * @param layoutResId The layout resource ID to be inflated and returned as a {@link View}
     * @see #setWidgetLayoutResource(int)
     */
    public void setLayoutResource(int layoutResId) {
        mLayoutResId = layoutResId;
    }

    /**
     * Gets the layout resource that will be shown as the {@link View} for this preference.
     *
     * @return The layout resource ID
     */
    public final int getLayoutResource() {
        return mLayoutResId;
    }

    /**
     * Sets the layout for the controllable widget portion of this preference. This is inflated
     * into the main layout. For example, a {@link CheckBoxPreference} would specify a custom
     * layout (consisting of just the CheckBox) here, instead of creating its own main layout.
     *
     * <p>It is an error to change the layout after adding the preference to a
     * {@link PreferenceGroup}.
     *
     * @param widgetLayoutResId The layout resource ID to be inflated into the main layout
     * @see #setLayoutResource(int)
     */
    public void setWidgetLayoutResource(int widgetLayoutResId) {
        mWidgetLayoutResId = widgetLayoutResId;
    }

    /**
     * Gets the layout resource for the controllable widget portion of this preference.
     *
     * @return The layout resource ID
     */
    public final int getWidgetLayoutResource() {
        return mWidgetLayoutResId;
    }

    /**
     * Binds the created View to the data for this preference.
     *
     * <p>This is a good place to grab references to custom Views in the layout and set
     * properties on them.
     *
     * <p>Make sure to call through to the superclass's implementation.
     *
     * @param holder The ViewHolder that provides references to the views to fill in. These views
     *               will be recycled, so you should not hold a reference to them after this method
     *               returns.
     */
    public void onBindViewHolder(PreferenceViewHolder holder) {
        holder.itemView.setOnClickListener(mClickListener);
        holder.itemView.setId(mViewId);

        final TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        if (titleView != null) {
            final CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                titleView.setText(title);
                titleView.setVisibility(View.VISIBLE);
                if (mHasSingleLineTitleAttr) {
                    titleView.setSingleLine(mSingleLineTitle);
                }
            } else {
                titleView.setVisibility(View.GONE);
            }
        }

        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView != null) {
            final CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                summaryView.setText(summary);
                summaryView.setVisibility(View.VISIBLE);
            } else {
                summaryView.setVisibility(View.GONE);
            }
        }

        final ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
        if (imageView != null) {
            if (mIconResId != 0 || mIcon != null) {
                if (mIcon == null) {
                    mIcon = AppCompatResources.getDrawable(mContext, mIconResId);
                }
                if (mIcon != null) {
                    imageView.setImageDrawable(mIcon);
                }
            }
            if (mIcon != null) {
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(mIconSpaceReserved ? View.INVISIBLE : View.GONE);
            }
        }

        View imageFrame = holder.findViewById(R.id.icon_frame);
        if (imageFrame == null) {
            imageFrame = holder.findViewById(AndroidResources.ANDROID_R_ICON_FRAME);
        }
        if (imageFrame != null) {
            if (mIcon != null) {
                imageFrame.setVisibility(View.VISIBLE);
            } else {
                imageFrame.setVisibility(mIconSpaceReserved ? View.INVISIBLE : View.GONE);
            }
        }

        if (mShouldDisableView) {
            setEnabledStateOnViews(holder.itemView, isEnabled());
        } else {
            setEnabledStateOnViews(holder.itemView, true);
        }

        final boolean selectable = isSelectable();
        holder.itemView.setFocusable(selectable);
        holder.itemView.setClickable(selectable);

        holder.setDividerAllowedAbove(mAllowDividerAbove);
        holder.setDividerAllowedBelow(mAllowDividerBelow);

        if (isCopyingEnabled()) {
            if (mOnCopyListener == null) {
                mOnCopyListener = new OnPreferenceCopyListener(this);
            }
            holder.itemView.setOnCreateContextMenuListener(mOnCopyListener);
        }
    }

    /**
     * Makes sure the view (and any children) get the enabled state changed.
     */
    private void setEnabledStateOnViews(View v, boolean enabled) {
        v.setEnabled(enabled);

        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setEnabledStateOnViews(vg.getChildAt(i), enabled);
            }
        }
    }

    /**
     * Sets the order of this preference with respect to other preference objects on the same
     * level. If this is not specified, the default behavior is to sort alphabetically. The
     * {@link PreferenceGroup#setOrderingAsAdded(boolean)} can be used to order preference
     * objects based on the order they appear in the XML.
     *
     * @param order The order for this preference. A lower value will be shown first. Use
     *              {@link #DEFAULT_ORDER} to sort alphabetically or allow ordering from XML.
     * @see PreferenceGroup#setOrderingAsAdded(boolean)
     * @see #DEFAULT_ORDER
     */
    public void setOrder(int order) {
        if (order != mOrder) {
            mOrder = order;

            // Reorder the list
            notifyHierarchyChanged();
        }
    }

    /**
     * Gets the order of this preference with respect to other preference objects on the same level.
     *
     * @return The order of this preference
     * @see #setOrder(int)
     */
    public int getOrder() {
        return mOrder;
    }

    /**
     * Set the ID that will be assigned to the overall View representing this preference, once
     * bound.
     *
     * @see View#setId(int)
     */
    public void setViewId(int viewId) {
        mViewId = viewId;
    }

    /**
     * Sets the title for this preference with a CharSequence. This title will be placed into the
     * ID {@link android.R.id#title} within the View bound by
     * {@link #onBindViewHolder(PreferenceViewHolder)}.
     *
     * @param title The title for this preference
     */
    public void setTitle(CharSequence title) {
        if ((title == null && mTitle != null) || (title != null && !title.equals(mTitle))) {
            mTitle = title;
            notifyChanged();
        }
    }

    /**
     * Sets the title for this preference with a resource ID.
     *
     * @param titleResId The title as a resource ID
     * @see #setTitle(CharSequence)
     */
    public void setTitle(int titleResId) {
        setTitle(mContext.getString(titleResId));
    }

    /**
     * Returns the title of this preference.
     *
     * @return The title
     * @see #setTitle(CharSequence)
     */
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Sets the icon for this preference with a Drawable. This icon will be placed into the ID
     * {@link android.R.id#icon} within the View created by
     * {@link #onBindViewHolder(PreferenceViewHolder)}.
     *
     * @param icon The optional icon for this preference
     */
    public void setIcon(Drawable icon) {
        if (mIcon != icon) {
            mIcon = icon;
            mIconResId = 0;
            notifyChanged();
        }
    }

    /**
     * Sets the icon for this preference with a resource ID.
     *
     * @param iconResId The icon as a resource ID
     * @see #setIcon(Drawable)
     */
    public void setIcon(int iconResId) {
        setIcon(AppCompatResources.getDrawable(mContext, iconResId));
        mIconResId = iconResId;
    }

    /**
     * Returns the icon of this preference.
     *
     * @return The icon
     * @see #setIcon(Drawable)
     */
    public Drawable getIcon() {
        if (mIcon == null && mIconResId != 0) {
            mIcon = AppCompatResources.getDrawable(mContext, mIconResId);
        }
        return mIcon;
    }

    /**
     * Returns the summary of this preference. If a {@link SummaryProvider} has been set for this
     * preference, it will be used to provide the summary returned by this method.
     *
     * @return The summary
     * @see #setSummary(CharSequence)
     * @see #setSummaryProvider(SummaryProvider)
     */
    public CharSequence getSummary() {
        if (getSummaryProvider() != null) {
            return getSummaryProvider().provideSummary(this);
        }
        return mSummary;
    }

    /**
     * Sets the summary for this preference with a CharSequence.
     *
     * <p>You can also use a {@link SummaryProvider} to dynamically configure the summary of this
     * preference.
     *
     * @param summary The summary for the preference
     * @throws IllegalStateException If a {@link SummaryProvider} has already been set.
     * @see #setSummaryProvider(SummaryProvider)
     */
    public void setSummary(CharSequence summary) {
        if (getSummaryProvider() != null) {
            throw new IllegalStateException("Preference already has a SummaryProvider set.");
        }
        if (!TextUtils.equals(mSummary, summary)) {
            mSummary = summary;
            notifyChanged();
        }
    }

    /**
     * Sets the summary for this preference with a resource ID.
     *
     * <p>You can also use a {@link SummaryProvider} to dynamically configure the summary of this
     * preference.
     *
     * @param summaryResId The summary as a resource
     * @see #setSummary(CharSequence)
     * @see #setSummaryProvider(SummaryProvider)
     */
    public void setSummary(int summaryResId) {
        setSummary(mContext.getString(summaryResId));
    }

    /**
     * Sets whether this preference is enabled. If disabled, it will not handle clicks.
     *
     * @param enabled Set true to enable it
     */
    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }

    /**
     * Checks whether this preference should be enabled in the list.
     *
     * @return {@code true} if this preference is enabled, false otherwise
     */
    public boolean isEnabled() {
        return mEnabled && mDependencyMet && mParentDependencyMet;
    }

    /**
     * Sets whether this preference is selectable.
     *
     * @param selectable Set true to make it selectable
     */
    public void setSelectable(boolean selectable) {
        if (mSelectable != selectable) {
            mSelectable = selectable;
            notifyChanged();
        }
    }

    /**
     * Checks whether this preference should be selectable in the list.
     *
     * @return {@code true} if it is selectable, false otherwise
     */
    public boolean isSelectable() {
        return mSelectable;
    }

    /**
     * Sets whether this preference should disable its view when it gets disabled.
     *
     * <p>For example, set this and {@link #setEnabled(boolean)} to false for preferences that
     * are only displaying information and 1) should not be clickable 2) should not have the view
     * set to the disabled state.
     *
     * @param shouldDisableView Set true if this preference should disable its view when the
     *                          preference is disabled.
     */
    public void setShouldDisableView(boolean shouldDisableView) {
        if (mShouldDisableView != shouldDisableView) {
            mShouldDisableView = shouldDisableView;
            notifyChanged();
        }
    }

    /**
     * Checks whether this preference should disable its view when it's action is disabled.
     *
     * @return {@code true} if it should disable the view
     * @see #setShouldDisableView(boolean)
     */
    public boolean getShouldDisableView() {
        return mShouldDisableView;
    }

    /**
     * Sets whether this preference should be visible to the user. If false, it is excluded from
     * the adapter, but can still be retrieved using
     * {@link PreferenceFragmentCompat#findPreference(CharSequence)}.
     *
     * <p>To show this preference to the user, its ancestors must also all be visible. If you make
     * a {@link PreferenceGroup} invisible, none of its children will be shown to the user until
     * the group is visible.
     *
     * @param visible Set false if this preference should be hidden from the user
     * @attr ref R.styleable#Preference_isPreferenceVisible
     * @see #isShown()
     */
    public final void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            if (mListener != null) {
                mListener.onPreferenceVisibilityChange(this);
            }
        }
    }

    /**
     * Checks whether this preference should be visible to the user.
     *
     * If this preference is visible, but one or more of its ancestors are not visible, then this
     * preference will not be shown until its ancestors are all visible.
     *
     * @return {@code true} if this preference should be displayed
     * @see #setVisible(boolean)
     * @see #isShown()
     */
    public final boolean isVisible() {
        return mVisible;
    }

    /**
     * Checks whether this preference is shown to the user in the hierarchy.
     *
     * For a preference to be shown in the hierarchy, it and all of its ancestors must be visible
     * and attached to the root {@link PreferenceScreen}.
     *
     * @return {@code true} if this preference is shown to the user in the hierarchy
     */
    public final boolean isShown() {
        if (!isVisible()) {
            return false;
        }

        if (getPreferenceManager() == null) {
            // We are not attached to the hierarchy
            return false;
        }

        if (this == getPreferenceManager().getPreferenceScreen()) {
            // We are at the root preference, so this preference and its ancestors are visible
            return true;
        }

        PreferenceGroup parent = getParent();
        if (parent == null) {
            // We are not attached to the hierarchy
            return false;
        }

        return parent.isShown();
    }

    /**
     * Returns a unique ID for this preference. This ID should be unique across all preference
     * objects in a hierarchy.
     *
     * @return A unique ID for this preference
     */
    long getId() {
        return mId;
    }

    /**
     * Processes a click on the preference. This includes saving the value to
     * the {@link SharedPreferences}. However, the overridden method should
     * call {@link #callChangeListener(Object)} to make sure the client wants to
     * update the preference's state with the new value.
     */
    protected void onClick() {}

    /**
     * Sets the key for this preference, which is used as a key to the {@link SharedPreferences} or
     * {@link PreferenceDataStore}. This should be unique for the package.
     *
     * @param key The key for the preference
     */
    public void setKey(String key) {
        mKey = key;

        if (mRequiresKey && !hasKey()) {
            requireKey();
        }
    }

    /**
     * Gets the key for this preference, which is also the key used for storing values into
     * {@link SharedPreferences} or {@link PreferenceDataStore}.
     *
     * @return The key
     */
    public String getKey() {
        return mKey;
    }

    /**
     * Checks whether the key is present, and if it isn't throws an exception. This should be called
     * by subclasses that persist their preferences.
     *
     * @throws IllegalStateException If there is no key assigned.
     */
    void requireKey() {
        if (TextUtils.isEmpty(mKey)) {
            throw new IllegalStateException("Preference does not have a key assigned.");
        }

        mRequiresKey = true;
    }

    /**
     * Checks whether this preference has a valid key.
     *
     * @return {@code true} if the key exists and is not a blank string, false otherwise
     */
    public boolean hasKey() {
        return !TextUtils.isEmpty(mKey);
    }

    /**
     * Checks whether this preference is persistent. If it is, it stores its value(s) into
     * the persistent {@link SharedPreferences} storage by default or into
     * {@link PreferenceDataStore} if assigned.
     *
     * @return {@code true} if persistent
     */
    public boolean isPersistent() {
        return mPersistent;
    }

    /**
     * Checks whether, at the given time this method is called, this preference should store/restore
     * its value(s) into the {@link SharedPreferences} or into {@link PreferenceDataStore} if
     * assigned. This, at minimum, checks whether this preference is persistent and it currently has
     * a key. Before you save/restore from the storage, check this first.
     *
     * @return {@code true} if it should persist the value
     */
    protected boolean shouldPersist() {
        return mPreferenceManager != null && isPersistent() && hasKey();
    }

    /**
     * Sets whether this preference is persistent. When persistent, it stores its value(s) into
     * the persistent {@link SharedPreferences} storage by default or into
     * {@link PreferenceDataStore} if assigned.
     *
     * @param persistent Set {@code true} if it should store its value(s) into the storage
     */
    public void setPersistent(boolean persistent) {
        mPersistent = persistent;
    }

    /**
     * Sets whether to constrain the title of this preference to a single line instead of
     * letting it wrap onto multiple lines.
     *
     * @param singleLineTitle Set {@code true} if the title should be constrained to one line
     * @attr ref R.styleable#Preference_android_singleLineTitle
     */
    public void setSingleLineTitle(boolean singleLineTitle) {
        mHasSingleLineTitleAttr = true;
        mSingleLineTitle = singleLineTitle;
    }

    /**
     * Gets whether the title of this preference is constrained to a single line.
     *
     * @return {@code true} if the title of this preference is constrained to a single line
     * @attr ref R.styleable#Preference_android_singleLineTitle
     * @see #setSingleLineTitle(boolean)
     */
    public boolean isSingleLineTitle() {
        return mSingleLineTitle;
    }

    /**
     * Sets whether to reserve the space of this preference icon view when no icon is provided. If
     * set to true, the preference will be offset as if it would have the icon and thus aligned with
     * other preferences having icons.
     *
     * @param iconSpaceReserved Set {@code true} if the space for the icon view should be reserved
     * @attr ref R.styleable#Preference_android_iconSpaceReserved
     */
    public void setIconSpaceReserved(boolean iconSpaceReserved) {
        if (mIconSpaceReserved != iconSpaceReserved) {
            mIconSpaceReserved = iconSpaceReserved;
            notifyChanged();
        }
    }

    /**
     * Returns whether the space of this preference icon view is reserved.
     *
     * @return {@code true} if the space of this preference icon view is reserved
     * @attr ref R.styleable#Preference_android_iconSpaceReserved
     * @see #setIconSpaceReserved(boolean)
     */
    public boolean isIconSpaceReserved() {
        return mIconSpaceReserved;
    }

    /**
     * Sets whether the summary of this preference can be copied to the clipboard by
     * long pressing on the preference.
     *
     * @param enabled Set true to enable copying the summary of this preference
     */
    public void setCopyingEnabled(boolean enabled) {
        if (mCopyingEnabled != enabled) {
            mCopyingEnabled = enabled;
            notifyChanged();
        }
    }

    /**
     * Returns whether the summary of this preference can be copied to the clipboard by
     * long pressing on the preference.
     *
     * @return {@code true} if copying is enabled, false otherwise
     */
    public boolean isCopyingEnabled() {
        return mCopyingEnabled;
    }

    /**
     * Set a {@link SummaryProvider} that will be invoked whenever the summary of this preference
     * is requested. Set {@code null} to remove the existing SummaryProvider.
     *
     * @param summaryProvider The {@link SummaryProvider} that will be invoked whenever the
     *                         summary of this preference is requested
     * @see SummaryProvider
     */
    public final void setSummaryProvider(@Nullable SummaryProvider summaryProvider) {
        mSummaryProvider = summaryProvider;
        notifyChanged();
    }


    /**
     * Returns the {@link SummaryProvider} used to configure the summary of this preference.
     *
     * @return The {@link SummaryProvider} used to configure the summary of this preference, or
     * {@code null} if there is no SummaryProvider set
     * @see SummaryProvider
     */
    @Nullable
    public final SummaryProvider getSummaryProvider() {
        return mSummaryProvider;
    }

    /**
     * Call this method after the user changes the preference, but before the internal state is
     * set. This allows the client to ignore the user value.
     *
     * @param newValue The new value of this preference
     * @return {@code true} if the user value should be set as the preference value (and persisted)
     */
    public boolean callChangeListener(Object newValue) {
        return mOnChangeListener == null || mOnChangeListener.onPreferenceChange(this, newValue);
    }

    /**
     * Sets the callback to be invoked when this preference is changed by the user (but before
     * the internal state has been updated).
     *
     * @param onPreferenceChangeListener The callback to be invoked
     */
    public void setOnPreferenceChangeListener(
            OnPreferenceChangeListener onPreferenceChangeListener) {
        mOnChangeListener = onPreferenceChangeListener;
    }

    /**
     * Returns the callback to be invoked when this preference is changed by the user (but before
     * the internal state has been updated).
     *
     * @return The callback to be invoked
     */
    public OnPreferenceChangeListener getOnPreferenceChangeListener() {
        return mOnChangeListener;
    }

    /**
     * Sets the callback to be invoked when this preference is clicked.
     *
     * @param onPreferenceClickListener The callback to be invoked
     */
    public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
        mOnClickListener = onPreferenceClickListener;
    }

    /**
     * Returns the callback to be invoked when this preference is clicked.
     *
     * @return The callback to be invoked
     */
    public OnPreferenceClickListener getOnPreferenceClickListener() {
        return mOnClickListener;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    protected void performClick(View view) {
        performClick();
    }

    /**
     * Called when a click should be performed.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void performClick() {

        if (!isEnabled()) {
            return;
        }

        onClick();

        if (mOnClickListener != null && mOnClickListener.onPreferenceClick(this)) {
            return;
        }

        PreferenceManager preferenceManager = getPreferenceManager();
        if (preferenceManager != null) {
            PreferenceManager.OnPreferenceTreeClickListener listener = preferenceManager
                    .getOnPreferenceTreeClickListener();
            if (listener != null && listener.onPreferenceTreeClick(this)) {
                return;
            }
        }

        if (mIntent != null) {
            Context context = getContext();
            context.startActivity(mIntent);
        }
    }

    /**
     * Returns the {@link Context} of this preference.
     * Each preference in a preference hierarchy can be from different Context (for example, if
     * multiple activities provide preferences into a single {@link PreferenceFragmentCompat}).
     * This Context will be used to save the preference values.
     *
     * @return The Context of this preference
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Returns the {@link SharedPreferences} where this preference can read its
     * value(s). Usually, it's easier to use one of the helper read methods:
     * {@link #getPersistedBoolean(boolean)}, {@link #getPersistedFloat(float)},
     * {@link #getPersistedInt(int)}, {@link #getPersistedLong(long)},
     * {@link #getPersistedString(String)}.
     *
     * @return The {@link SharedPreferences} where this preference reads its value(s). If this
     * preference is not attached to a preference hierarchy or if a
     * {@link PreferenceDataStore} has been set, this method returns {@code null}.
     * @see #setPreferenceDataStore(PreferenceDataStore)
     */
    public SharedPreferences getSharedPreferences() {
        if (mPreferenceManager == null || getPreferenceDataStore() != null) {
            return null;
        }

        return mPreferenceManager.getSharedPreferences();
    }

    /**
     * Compares preference objects based on order (if set), otherwise alphabetically on the titles.
     *
     * @param another The preference to compare to this one
     * @return 0 if the same; less than 0 if this preference sorts ahead of <var>another</var>;
     * greater than 0 if this preference sorts after <var>another</var>.
     */
    @Override
    public int compareTo(@NonNull Preference another) {
        if (mOrder != another.mOrder) {
            // Do order comparison
            return mOrder - another.mOrder;
        } else if (mTitle == another.mTitle) {
            // If titles are null or share same object comparison
            return 0;
        } else if (mTitle == null) {
            return 1;
        } else if (another.mTitle == null) {
            return -1;
        } else {
            // Do name comparison
            return mTitle.toString().compareToIgnoreCase(another.mTitle.toString());
        }
    }

    /**
     * Sets the internal change listener.
     *
     * @param listener The listener
     * @see #notifyChanged()
     */
    final void setOnPreferenceChangeInternalListener(OnPreferenceChangeInternalListener listener) {
        mListener = listener;
    }

    /**
     * Should be called when the data of this {@link Preference} has changed.
     */
    protected void notifyChanged() {
        if (mListener != null) {
            mListener.onPreferenceChange(this);
        }
    }

    /**
     * Should be called when a preference has been added/removed from this group, or the ordering
     * should be re-evaluated.
     */
    protected void notifyHierarchyChanged() {
        if (mListener != null) {
            mListener.onPreferenceHierarchyChange(this);
        }
    }

    /**
     * Gets the {@link PreferenceManager} that manages this preference object's tree.
     *
     * @return The {@link PreferenceManager}
     */
    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    /**
     * Called when this preference has been attached to a preference hierarchy. Make sure to call
     * the super implementation.
     *
     * @param preferenceManager The PreferenceManager of the hierarchy
     */
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        mPreferenceManager = preferenceManager;

        if (!mHasId) {
            mId = preferenceManager.getNextId();
        }

        dispatchSetInitialValue();
    }

    /**
     * Called from {@link PreferenceGroup} to pass in an ID for reuse.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager, long id) {
        mId = id;
        mHasId = true;
        try {
            onAttachedToHierarchy(preferenceManager);
        } finally {
            mHasId = false;
        }
    }

    /**
     * Assigns a {@link PreferenceGroup} as the parent of this preference. Set {@code null} to
     * remove the current parent.
     *
     * @param parentGroup Parent preference group of this preference or {@code null} if none
     *
     * @throws IllegalStateException If the preference already has a parent assigned.
     */
    void assignParent(@Nullable PreferenceGroup parentGroup) {
        if (parentGroup != null && mParentGroup != null) {
            throw new IllegalStateException(
                    "This preference already has a parent. You must remove the existing parent "
                            + "before assigning a new one.");
        }
        mParentGroup = parentGroup;
    }

    /**
     * Called when the preference hierarchy has been attached to the list of preferences. This
     * can also be called when this preference has been attached to a group that was already
     * attached to the list of preferences.
     */
    public void onAttached() {
        // At this point, the hierarchy that this preference is in is connected
        // with all other preferences.
        registerDependency();
    }

    /**
     * Called when the preference hierarchy has been detached from the list of preferences. This
     * can also be called when this preference has been removed from a group that was attached to
     * the list of preferences.
     */
    public void onDetached() {
        unregisterDependency();
        mWasDetached = true;
    }

    /**
     * Returns true if {@link #onDetached()} was called. Used for handling the case when a
     * preference was removed, modified, and re-added to a {@link PreferenceGroup}.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public final boolean wasDetached() {
        return mWasDetached;
    }

    /**
     * Clears the {@link #wasDetached()} status.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public final void clearWasDetached() {
        mWasDetached = false;
    }

    private void registerDependency() {

        if (TextUtils.isEmpty(mDependencyKey)) return;

        Preference preference = findPreferenceInHierarchy(mDependencyKey);
        if (preference != null) {
            preference.registerDependent(this);
        } else {
            throw new IllegalStateException("Dependency \"" + mDependencyKey
                    + "\" not found for preference \"" + mKey + "\" (title: \"" + mTitle + "\"");
        }
    }

    private void unregisterDependency() {
        if (mDependencyKey != null) {
            final Preference oldDependency = findPreferenceInHierarchy(mDependencyKey);
            if (oldDependency != null) {
                oldDependency.unregisterDependent(this);
            }
        }
    }

    /**
     * Finds a preference in this hierarchy (the whole thing, even above/below your
     * {@link PreferenceScreen} screen break) with the given key.
     *
     * <p>This only functions after we have been attached to a hierarchy.
     *
     * @param key The key of the preference to find
     * @return The preference that uses the given key
     */
    protected Preference findPreferenceInHierarchy(String key) {
        if (TextUtils.isEmpty(key) || mPreferenceManager == null) {
            return null;
        }

        return mPreferenceManager.findPreference(key);
    }

    /**
     * Adds a dependent preference on this preference so we can notify it. Usually, the dependent
     * preference registers itself (it's good for it to know it depends on something), so please
     * use {@link Preference#setDependency(String)} on the dependent preference.
     *
     * @param dependent The dependent preference that will be enabled/disabled
     *                  according to the state of this preference.
     */
    private void registerDependent(Preference dependent) {
        if (mDependents == null) {
            mDependents = new ArrayList<>();
        }

        mDependents.add(dependent);

        dependent.onDependencyChanged(this, shouldDisableDependents());
    }

    /**
     * Removes a dependent preference on this preference.
     *
     * @param dependent The dependent preference that will be enabled/disabled
     *                  according to the state of this preference.
     */
    private void unregisterDependent(Preference dependent) {
        if (mDependents != null) {
            mDependents.remove(dependent);
        }
    }

    /**
     * Notifies any listening dependents of a change that affects the dependency.
     *
     * @param disableDependents Whether this preference should disable
     *                          its dependents.
     */
    public void notifyDependencyChange(boolean disableDependents) {
        final List<Preference> dependents = mDependents;

        if (dependents == null) {
            return;
        }

        final int dependentsCount = dependents.size();
        for (int i = 0; i < dependentsCount; i++) {
            dependents.get(i).onDependencyChanged(this, disableDependents);
        }
    }

    /**
     * Called when the dependency changes.
     *
     * @param dependency       The preference that this preference depends on
     * @param disableDependent Set true to disable this preference
     */
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        if (mDependencyMet == disableDependent) {
            mDependencyMet = !disableDependent;

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }

    /**
     * Called when the implicit parent dependency changes.
     *
     * @param parent       The preference that this preference depends on
     * @param disableChild Set true to disable this preference
     */
    public void onParentChanged(Preference parent, boolean disableChild) {
        if (mParentDependencyMet == disableChild) {
            mParentDependencyMet = !disableChild;

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
    }

    /**
     * Checks whether this preference's dependents should currently be disabled.
     *
     * @return {@code true} if the dependents should be disabled, otherwise false
     */
    public boolean shouldDisableDependents() {
        return !isEnabled();
    }

    /**
     * Sets the key of a preference that this preference will depend on. If that preference is
     * not set or is off, this preference will be disabled.
     *
     * @param dependencyKey The key of the preference that this depends on
     */
    public void setDependency(String dependencyKey) {
        // Unregister the old dependency, if we had one
        unregisterDependency();

        // Register the new
        mDependencyKey = dependencyKey;
        registerDependency();
    }

    /**
     * Returns the key of the dependency on this preference.
     *
     * @return The key of the dependency
     * @see #setDependency(String)
     */
    public String getDependency() {
        return mDependencyKey;
    }

    /**
     * Returns the {@link PreferenceGroup} which is this preference assigned to or {@code null}
     * if this preference is not assigned to any group or is a root preference.
     *
     * @return The parent PreferenceGroup or {@code null} if not attached to any
     */
    @Nullable
    public PreferenceGroup getParent() {
        return mParentGroup;
    }

    /**
     * Called when this preference is being removed from the hierarchy. You should remove any
     * references to this preference that you know about. Make sure to call through to the
     * superclass implementation.
     */
    protected void onPrepareForRemoval() {
        unregisterDependency();
    }

    /**
     * Sets the default value for this preference, which will be set either if persistence is off
     * or persistence is on and the preference is not found in the persistent storage.
     *
     * @param defaultValue The default value
     */
    public void setDefaultValue(Object defaultValue) {
        mDefaultValue = defaultValue;
    }

    private void dispatchSetInitialValue() {
        if (getPreferenceDataStore() != null) {
            onSetInitialValue(true, mDefaultValue);
            return;
        }

        // By now, we know if we are persistent.
        final boolean shouldPersist = shouldPersist();
        if (!shouldPersist || !getSharedPreferences().contains(mKey)) {
            if (mDefaultValue != null) {
                onSetInitialValue(false, mDefaultValue);
            }
        } else {
            onSetInitialValue(true, null);
        }
    }

    /**
     * Implement this to set the initial value of the preference.
     *
     * <p>If <var>restorePersistedValue</var> is true, you should restore the preference value
     * from the {@link SharedPreferences}. If <var>restorePersistedValue</var> is
     * false, you should set the preference value to defaultValue that is given (and possibly
     * store to SharedPreferences if {@link #shouldPersist()} is true).
     *
     * <p>In case of using {@link PreferenceDataStore}, the <var>restorePersistedValue</var> is
     * always {@code true} but the default value (if provided) is set.
     *
     * <p>This may not always be called. One example is if it should not persist but there is no
     * default value given.
     *
     * @param restorePersistedValue True to restore the persisted value;
     *                              false to use the given <var>defaultValue</var>.
     * @param defaultValue          The default value for this preference. Only use this
     *                              if <var>restorePersistedValue</var> is false.
     *
     * @deprecated Use {@link #onSetInitialValue(Object)} instead.
     */
    @Deprecated
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        onSetInitialValue(defaultValue);
    }

    /**
     * Implement this to set the initial value of the preference.
     *
     * <p>If you are persisting values to {@link SharedPreferences} or a {@link PreferenceDataStore}
     * you should restore the saved value for the preference.
     *
     * <p>If you are not persisting values, or there is no value saved for the preference, you
     * should set the value of the preference to <var>defaultValue</var>.
     *
     * @param defaultValue The default value for the preference if set, otherwise {@code null}.
     */
    protected void onSetInitialValue(@Nullable Object defaultValue) {}

    private void tryCommit(@NonNull SharedPreferences.Editor editor) {
        if (mPreferenceManager.shouldCommit()) {
            editor.apply();
        }
    }

    /**
     * Attempts to persist a {@link String} if this preference is persistent.
     *
     * <p>The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist
     * @return {@code true} if the preference is persistent, {@code false} otherwise
     * @see #getPersistedString(String)
     */
    protected boolean persistString(String value) {
        if (!shouldPersist()) {
            return false;
        }

        // Shouldn't store null
        if (TextUtils.equals(value, getPersistedString(null))) {
            // It's already there, so the same as persisting
            return true;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putString(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putString(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    /**
     * Attempts to get a persisted set of Strings if this preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either the preference is not
     *                           persistent or the preference is not in the shared preferences.
     * @return The value from the storage or the default return value
     * @see #persistString(String)
     */
    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getString(mKey, defaultReturnValue);
        }

        return mPreferenceManager.getSharedPreferences().getString(mKey, defaultReturnValue);
    }

    /**
     * Attempts to persist a set of Strings if this preference is persistent.
     *
     * <p>The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param values The values to persist
     * @return {@code true} if the preference is persistent, {@code false} otherwise
     * @see #getPersistedStringSet(Set)
     */
    public boolean persistStringSet(Set<String> values) {
        if (!shouldPersist()) {
            return false;
        }

        // Shouldn't store null
        if (values.equals(getPersistedStringSet(null))) {
            // It's already there, so the same as persisting
            return true;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putStringSet(mKey, values);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putStringSet(mKey, values);
            tryCommit(editor);
        }
        return true;
    }

    /**
     * Attempts to get a persisted set of Strings if this preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this preference is not
     *                           persistent or this preference is not present.
     * @return The value from the storage or the default return value
     * @see #persistStringSet(Set)
     */
    public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getStringSet(mKey, defaultReturnValue);
        }

        return mPreferenceManager.getSharedPreferences().getStringSet(mKey, defaultReturnValue);
    }

    /**
     * Attempts to persist an {@link Integer} if this preference is persistent.
     *
     * <p>The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist
     * @return {@code true} if the preference is persistent, {@code false} otherwise
     * @see #persistString(String)
     * @see #getPersistedInt(int)
     */
    protected boolean persistInt(int value) {
        if (!shouldPersist()) {
            return false;
        }

        if (value == getPersistedInt(~value)) {
            // It's already there, so the same as persisting
            return true;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putInt(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putInt(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    /**
     * Attempts to get a persisted {@link Integer} if this preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this preference is not
     *                           persistent or this preference is not in the SharedPreferences.
     * @return The value from the storage or the default return value
     * @see #getPersistedString(String)
     * @see #persistInt(int)
     */
    protected int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getInt(mKey, defaultReturnValue);
        }

        return mPreferenceManager.getSharedPreferences().getInt(mKey, defaultReturnValue);
    }

    /**
     * Attempts to persist a {@link Float} if this preference is persistent.
     *
     * <p>The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist
     * @return {@code true} if the preference is persistent, {@code false} otherwise
     * @see #persistString(String)
     * @see #getPersistedFloat(float)
     */
    protected boolean persistFloat(float value) {
        if (!shouldPersist()) {
            return false;
        }

        if (value == getPersistedFloat(Float.NaN)) {
            // It's already there, so the same as persisting
            return true;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putFloat(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putFloat(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    /**
     * Attempts to get a persisted {@link Float} if this preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this preference is not
     *                           persistent or this preference is not saved.
     * @return The value from the storage or the default return value
     * @see #getPersistedString(String)
     * @see #persistFloat(float)
     */
    protected float getPersistedFloat(float defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getFloat(mKey, defaultReturnValue);
        }

        return mPreferenceManager.getSharedPreferences().getFloat(mKey, defaultReturnValue);
    }

    /**
     * Attempts to persist a {@link Long} if this preference is persistent.
     *
     * <p>The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist
     * @return {@code true} if the preference is persistent, {@code false} otherwise
     * @see #persistString(String)
     * @see #getPersistedLong(long)
     */
    protected boolean persistLong(long value) {
        if (!shouldPersist()) {
            return false;
        }

        if (value == getPersistedLong(~value)) {
            // It's already there, so the same as persisting
            return true;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putLong(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putLong(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    /**
     * Attempts to get a persisted {@link Long} if this preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this preference is not
     *                           persistent or this preference is not in the SharedPreferences.
     * @return The value from the storage or the default return value
     * @see #getPersistedString(String)
     * @see #persistLong(long)
     */
    protected long getPersistedLong(long defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getLong(mKey, defaultReturnValue);
        }

        return mPreferenceManager.getSharedPreferences().getLong(mKey, defaultReturnValue);
    }

    /**
     * Attempts to persist a {@link Boolean} if this preference is persistent.
     *
     * <p>The returned value doesn't reflect whether the given value was persisted, since we may not
     * necessarily commit if there will be a batch commit later.
     *
     * @param value The value to persist
     * @return {@code true} if the preference is persistent, {@code false} otherwise
     * @see #persistString(String)
     * @see #getPersistedBoolean(boolean)
     */
    protected boolean persistBoolean(boolean value) {
        if (!shouldPersist()) {
            return false;
        }

        if (value == getPersistedBoolean(!value)) {
            // It's already there, so the same as persisting
            return true;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putBoolean(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putBoolean(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    /**
     * Attempts to get a persisted {@link Boolean} if this preference is persistent.
     *
     * @param defaultReturnValue The default value to return if either this preference is not
     *                           persistent or this preference is not in the SharedPreferences.
     * @return The value from the storage or the default return value
     * @see #getPersistedString(String)
     * @see #persistBoolean(boolean)
     */
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getBoolean(mKey, defaultReturnValue);
        }

        return mPreferenceManager.getSharedPreferences().getBoolean(mKey, defaultReturnValue);
    }

    @Override
    public String toString() {
        return getFilterableStringBuilder().toString();
    }

    /**
     * Returns the text that will be used to filter this preference depending on user input.
     *
     * <p>If overriding and calling through to the superclass, make sure to prepend your
     * additions with a space.
     *
     * @return Text as a {@link StringBuilder} that will be used to filter this preference. By
     * default, this is the title and summary (concatenated with a space).
     */
    StringBuilder getFilterableStringBuilder() {
        StringBuilder sb = new StringBuilder();
        CharSequence title = getTitle();
        if (!TextUtils.isEmpty(title)) {
            sb.append(title).append(' ');
        }
        CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            sb.append(summary).append(' ');
        }
        if (sb.length() > 0) {
            // Drop the last space
            sb.setLength(sb.length() - 1);
        }
        return sb;
    }

    /**
     * Store this preference hierarchy's frozen state into the given container.
     *
     * @param container The Bundle in which to save the instance of this preference
     * @see #restoreHierarchyState
     * @see #onSaveInstanceState
     */
    public void saveHierarchyState(Bundle container) {
        dispatchSaveInstanceState(container);
    }

    /**
     * Called by {@link #saveHierarchyState} to store the instance for this preference and its
     * children. May be overridden to modify how the save happens for children. For example, some
     * preference objects may want to not store an instance for their children.
     *
     * @param container The Bundle in which to save the instance of this preference
     * @see #saveHierarchyState
     * @see #onSaveInstanceState
     */
    void dispatchSaveInstanceState(Bundle container) {
        if (hasKey()) {
            mBaseMethodCalled = false;
            Parcelable state = onSaveInstanceState();
            if (!mBaseMethodCalled) {
                throw new IllegalStateException(
                        "Derived class did not call super.onSaveInstanceState()");
            }
            if (state != null) {
                container.putParcelable(mKey, state);
            }
        }
    }

    /**
     * Hook allowing a preference to generate a representation of its internal state that can
     * later be used to create a new instance with that same state. This state should only
     * contain information that is not persistent or can be reconstructed later.
     *
     * @return A Parcelable object containing the current dynamic state of this preference, or
     * {@code null} if there is nothing interesting to save. The default implementation returns
     * {@code null}.
     * @see #onRestoreInstanceState
     * @see #saveHierarchyState
     */
    protected Parcelable onSaveInstanceState() {
        mBaseMethodCalled = true;
        return BaseSavedState.EMPTY_STATE;
    }

    /**
     * Restore this preference hierarchy's previously saved state from the given container.
     *
     * @param container The Bundle that holds the previously saved state
     * @see #saveHierarchyState
     * @see #onRestoreInstanceState
     */
    public void restoreHierarchyState(Bundle container) {
        dispatchRestoreInstanceState(container);
    }

    /**
     * Called by {@link #restoreHierarchyState} to retrieve the saved state for this preference
     * and its children. May be overridden to modify how restoring happens to the children of a
     * preference. For example, some preference objects may not want to save state for their
     * children.
     *
     * @param container The Bundle that holds the previously saved state
     * @see #restoreHierarchyState
     * @see #onRestoreInstanceState
     */
    void dispatchRestoreInstanceState(Bundle container) {
        if (hasKey()) {
            Parcelable state = container.getParcelable(mKey);
            if (state != null) {
                mBaseMethodCalled = false;
                onRestoreInstanceState(state);
                if (!mBaseMethodCalled) {
                    throw new IllegalStateException(
                            "Derived class did not call super.onRestoreInstanceState()");
                }
            }
        }
    }

    /**
     * Hook allowing a preference to re-apply a representation of its internal state that had
     * previously been generated by {@link #onSaveInstanceState}. This function will never be
     * called with a null state.
     *
     * @param state The saved state that had previously been returned by
     *              {@link #onSaveInstanceState}.
     * @see #onSaveInstanceState
     * @see #restoreHierarchyState
     */
    protected void onRestoreInstanceState(Parcelable state) {
        mBaseMethodCalled = true;
        if (state != BaseSavedState.EMPTY_STATE && state != null) {
            throw new IllegalArgumentException("Wrong state class -- expecting Preference State");
        }
    }

    /**
     * Initializes an {@link android.view.accessibility.AccessibilityNodeInfo} with information
     * about the View for this preference.
     */
    @CallSuper
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfoCompat info) {}

    /**
     * Interface definition for a callback to be invoked when the value of this
     * {@link Preference} has been changed by the user and is about to be set and/or persisted.
     * This gives the client a chance to prevent setting and/or persisting the value.
     */
    public interface OnPreferenceChangeListener {
        /**
         * Called when a preference has been changed by the user. This is called before the state
         * of the preference is about to be updated and before the state is persisted.
         *
         * @param preference The changed preference
         * @param newValue   The new value of the preference
         * @return {@code true} to update the state of the preference with the new value
         */
        boolean onPreferenceChange(Preference preference, Object newValue);
    }

    /**
     * Interface definition for a callback to be invoked when a {@link Preference} is clicked.
     */
    public interface OnPreferenceClickListener {
        /**
         * Called when a preference has been clicked.
         *
         * @param preference The preference that was clicked
         * @return {@code true} if the click was handled
         */
        boolean onPreferenceClick(Preference preference);
    }

    /**
     * Interface definition for a callback to be invoked when this {@link Preference} is changed
     * or, if this is a group, there is an addition/removal of {@link Preference}(s). This is
     * used internally.
     */
    interface OnPreferenceChangeInternalListener {
        /**
         * Called when this preference has changed.
         *
         * @param preference This preference
         */
        void onPreferenceChange(Preference preference);

        /**
         * Called when this group has added/removed {@link Preference}(s).
         *
         * @param preference This preference
         */
        void onPreferenceHierarchyChange(Preference preference);

        /**
         * Called when this preference has changed its visibility.
         *
         * @param preference This preference
         */
        void onPreferenceVisibilityChange(Preference preference);
    }

    /**
     * Interface definition for a callback to be invoked when the summary of this
     * {@link Preference} is requested (typically when this preference is added to the hierarchy
     * or its value is updated). Implement this to allow dynamically configuring a summary.
     *
     * <p> If a SummaryProvider is set, {@link #setSummary(CharSequence)} will throw an
     * exception, and any existing value for the summary will not be used. The value returned by
     * the SummaryProvider will be used instead whenever {@link #getSummary()} is called on this
     * preference.
     *
     * <p> Simple implementations are provided for {@link EditTextPreference} and
     * {@link ListPreference}. To enable these implementations, use
     * {@link #setSummaryProvider(SummaryProvider)} with
     * {@link EditTextPreference.SimpleSummaryProvider#getInstance()} or
     * {@link ListPreference.SimpleSummaryProvider#getInstance()}.
     *
     * @param <T> The Preference class that a summary is being requested for
     */
    public interface SummaryProvider<T extends Preference> {

        /**
         * Called whenever {@link #getSummary()} is called on this preference.
         *
         * @param preference This preference
         * @return A CharSequence that will be displayed as the summary for this preference
         */
        CharSequence provideSummary(T preference);
    }

    /**
     * A base class for managing the instance state of a {@link Preference}.
     */
    public static class BaseSavedState extends AbsSavedState {
        public static final Parcelable.Creator<BaseSavedState> CREATOR =
                new Parcelable.Creator<BaseSavedState>() {
                    @Override
                    public BaseSavedState createFromParcel(Parcel in) {
                        return new BaseSavedState(in);
                    }

                    @Override
                    public BaseSavedState[] newArray(int size) {
                        return new BaseSavedState[size];
                    }
                };

        public BaseSavedState(Parcel source) {
            super(source);
        }

        public BaseSavedState(Parcelable superState) {
            super(superState);
        }
    }

    /**
     * Handles creating a context menu to allow copying a {@link Preference} and copying the
     * summary of the preference to the clipboard.
     *
     * @see #setCopyingEnabled(boolean)
     */
    private static class OnPreferenceCopyListener implements View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener {

        private final Preference mPreference;

        OnPreferenceCopyListener(Preference preference) {
            mPreference = preference;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenu.ContextMenuInfo menuInfo) {
            CharSequence summary = mPreference.getSummary();
            if (!mPreference.isCopyingEnabled() || TextUtils.isEmpty(summary)) {
                return;
            }
            menu.setHeaderTitle(summary);
            menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.copy)
                    .setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            ClipboardManager clipboard =
                    (ClipboardManager) mPreference.getContext().getSystemService(
                            Context.CLIPBOARD_SERVICE);
            CharSequence summary = mPreference.getSummary();
            ClipData clip = ClipData.newPlainText(CLIPBOARD_ID, summary);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(mPreference.getContext(),
                    mPreference.getContext().getString(R.string.preference_copied,
                            summary),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}
