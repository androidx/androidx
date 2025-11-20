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

package androidx.appcompat.graphics.drawable;

import static androidx.core.content.res.TypedArrayUtils.obtainAttributes;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.StateSet;

import androidx.appcompat.resources.R;
import androidx.appcompat.widget.ResourceManagerInternal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Lets you assign a number of graphic images to a single Drawable and swap out the visible item by
 * a string ID value.
 * <p>
 * It can be defined in an XML file with the <code>&lt;selector></code> element.
 * Each state Drawable is defined in a nested <code>&lt;item></code> element. For more
 * information, see the guide to
 * <a href="{@docRoot}guide/topics/resources/drawable-resource.html">Drawable Resources</a>.
 *
 * {@link android.R.attr#visible}
 * {@link android.R.attr#variablePadding}
 * {@link android.R.attr#constantSize}
 * {@link android.R.attr#state_focused}
 * {@link android.R.attr#state_window_focused}
 * {@link android.R.attr#state_enabled}
 * {@link android.R.attr#state_checkable}
 * {@link android.R.attr#state_checked}
 * {@link android.R.attr#state_selected}
 * {@link android.R.attr#state_activated}
 * {@link android.R.attr#state_active}
 * {@link android.R.attr#state_single}
 * {@link android.R.attr#state_first}
 * {@link android.R.attr#state_middle}
 * {@link android.R.attr#state_last}
 * {@link android.R.attr#state_pressed}
 * <p>
 * Adapted from platform class; altered with API level checks as necessary & uses
 * {@code ResourceManagerInternal} for <code>Drawable</code> inflation.
 */
public class StateListDrawableCompat extends DrawableContainerCompat {
    private static final String TAG = "StateListDrawableCompat";
    private static final boolean DEBUG = false;
    private StateListState mStateListState;
    private boolean mMutated;

    /**
     * Creates an empty state list drawable.
     */
    public StateListDrawableCompat() {
        this(null, null);
    }

    /**
     * Add a new image/string ID to the set of images.
     *
     * @param stateSet - An array of resource Ids to associate with the image.
     *                 Switch to this image by calling setState().
     * @param drawable -The image to show.
     */
    public void addState(int[] stateSet, Drawable drawable) {
        if (drawable != null) {
            mStateListState.addStateSet(stateSet, drawable);
            // in case the new state matches our current state...
            onStateChange(getState());
        }
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int @NonNull [] stateSet) {
        final boolean changed = super.onStateChange(stateSet);
        int idx = mStateListState.indexOfStateSet(stateSet);
        if (DEBUG) {
            android.util.Log.i(TAG, "onStateChange " + this + " states "
                    + Arrays.toString(stateSet) + " found " + idx);
        }
        if (idx < 0) {
            idx = mStateListState.indexOfStateSet(StateSet.WILD_CARD);
        }
        return selectDrawable(idx) || changed;
    }

    /**
     * Inflate this Drawable from an XML resource optionally styled by a theme.
     * This can't be called more than once for each Drawable.
     *
     * @param context The context in which the inflation takes place
     * @param r Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this Drawable
     * @param attrs Base set of attribute values
     * @param theme Theme to apply, may be null
     * @throws XmlPullParserException
     * @throws IOException
     */
    public void inflate(
            @NonNull Context context,
            @NonNull Resources r,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.StateListDrawable);
        setVisible(a.getBoolean(R.styleable.StateListDrawable_android_visible, true), true);
        updateStateFromTypedArray(a);
        updateDensity(r);
        a.recycle();
        inflateChildElements(context, r, parser, attrs, theme);
        onStateChange(getState());
    }

    /**
     * Updates the constant state from the values in the typed array.
     */
    private void updateStateFromTypedArray(TypedArray a) {
        final StateListState state = mStateListState;
        // Account for any configuration changes.
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mVariablePadding = a.getBoolean(
                R.styleable.StateListDrawable_android_variablePadding, state.mVariablePadding);
        state.mConstantSize = a.getBoolean(
                R.styleable.StateListDrawable_android_constantSize, state.mConstantSize);
        state.mEnterFadeDuration = a.getInt(
                R.styleable.StateListDrawable_android_enterFadeDuration, state.mEnterFadeDuration);
        state.mExitFadeDuration = a.getInt(
                R.styleable.StateListDrawable_android_exitFadeDuration, state.mExitFadeDuration);
        state.mDither = a.getBoolean(
                R.styleable.StateListDrawable_android_dither, state.mDither);
    }

    /**
     * Inflates child elements from XML.
     */
    private void inflateChildElements(Context context, Resources r,
            XmlPullParser parser, AttributeSet attrs,
            Theme theme) throws XmlPullParserException, IOException {
        final StateListState state = mStateListState;
        final int innerDepth = parser.getDepth() + 1;
        int type;
        int depth;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth
                || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            if (depth > innerDepth || !parser.getName().equals("item")) {
                continue;
            }
            // This allows state list drawable item elements to be themed at
            // inflation time but does NOT make them work for Zygote preload.
            final TypedArray a = obtainAttributes(r, theme, attrs,
                    R.styleable.StateListDrawableItem);
            Drawable dr = null;
            final int drawableId = a.getResourceId(
                    R.styleable.StateListDrawableItem_android_drawable, -1);
            if (drawableId > 0) {
                dr = ResourceManagerInternal.get().getDrawable(context, drawableId);
            }
            a.recycle();
            final int[] states = extractStateSet(attrs);
            // Loading child elements modifies the state of the AttributeSet's
            // underlying parser, so it needs to happen after obtaining
            // attributes and extracting states.
            if (dr == null) {
                while ((type = parser.next()) == XmlPullParser.TEXT) {
                    // no-op
                }
                if (type != XmlPullParser.START_TAG) {
                    throw new XmlPullParserException(
                            parser.getPositionDescription()
                                    + ": <item> tag requires a 'drawable' attribute or "
                                    + "child tag defining a drawable");
                }
                dr = Drawable.createFromXmlInner(r, parser, attrs, theme);
            }
            state.addStateSet(states, dr);
        }
    }

    /**
     * Extracts state_ attributes from an attribute set.
     *
     * @param attrs The attribute set.
     * @return An array of state_ attributes.
     */
    int[] extractStateSet(AttributeSet attrs) {
        int j = 0;
        final int numAttrs = attrs.getAttributeCount();
        int[] states = new int[numAttrs];
        for (int i = 0; i < numAttrs; i++) {
            final int stateResId = attrs.getAttributeNameResource(i);
            switch (stateResId) {
                case 0:
                    break;
                case android.R.attr.drawable:
                case android.R.attr.id:
                    // Ignore attributes from StateListDrawableItem and
                    // AnimatedStateListDrawableItem.
                    continue;
                default:
                    states[j++] = attrs.getAttributeBooleanValue(i, false)
                            ? stateResId : -stateResId;
            }
        }
        states = StateSet.trimStateSet(states, j);
        return states;
    }

    StateListState getStateListState() {
        return mStateListState;
    }

    /**
     * Gets the number of states contained in this drawable.
     *
     * @return The number of states contained in this drawable.
     * @see #getStateSet(int)
     * @see #getStateDrawable(int)
     */
    int getStateCount() {
        return mStateListState.getChildCount();
    }

    /**
     * Gets the state set at an index.
     *
     * @param index The index of the state set.
     * @return The state set at the index.
     * @see #getStateCount()
     * @see #getStateDrawable(int)
     */
    int[] getStateSet(int index) {
        return mStateListState.mStateSets[index];
    }

    /**
     * Gets the drawable at an index.
     *
     * @param index The index of the drawable.
     * @return The drawable at the index.
     * @see #getStateCount()
     * @see #getStateSet(int)
     */
    Drawable getStateDrawable(int index) {
        return mStateListState.getChild(index);
    }

    /**
     * Gets the index of the drawable with the provided state set.
     *
     * @param stateSet the state set to look up
     * @return the index of the provided state set, or -1 if not found
     * @see #getStateDrawable(int)
     * @see #getStateSet(int)
     */
    int getStateDrawableIndex(int[] stateSet) {
        return mStateListState.indexOfStateSet(stateSet);
    }

    @Override
    public @NonNull Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mStateListState.mutate();
            mMutated = true;
        }
        return this;
    }

    @Override
    StateListState cloneConstantState() {
        return new StateListState(mStateListState, this, null);
    }

    @Override
    void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    static class StateListState extends DrawableContainerState {
        int[][] mStateSets;

        StateListState(StateListState orig, StateListDrawableCompat owner, Resources res) {
            super(orig, owner, res);
            if (orig != null) {
                // Perform a shallow copy and rely on mutate() to deep-copy.
                mStateSets = orig.mStateSets;
            } else {
                mStateSets = new int[getCapacity()][];
            }
        }

        @Override
        void mutate() {
            final int[][] stateSets = new int[mStateSets.length][];
            for (int i = mStateSets.length - 1; i >= 0; i--) {
                stateSets[i] = mStateSets[i] != null ? mStateSets[i].clone() : null;
            }
            mStateSets = stateSets;
        }

        int addStateSet(int[] stateSet, Drawable drawable) {
            final int pos = addChild(drawable);
            mStateSets[pos] = stateSet;
            return pos;
        }

        int indexOfStateSet(int[] stateSet) {
            final int[][] stateSets = mStateSets;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (StateSet.stateSetMatches(stateSets[i], stateSet)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public @NonNull Drawable newDrawable() {
            return new StateListDrawableCompat(this, null);
        }

        @Override
        public @NonNull Drawable newDrawable(Resources res) {
            return new StateListDrawableCompat(this, res);
        }

        @Override
        public void growArray(int oldSize, int newSize) {
            super.growArray(oldSize, newSize);
            final int[][] newStateSets = new int[newSize][];
            System.arraycopy(mStateSets, 0, newStateSets, 0, oldSize);
            mStateSets = newStateSets;
        }
    }

    @Override
    public void applyTheme(@NonNull Theme theme) {
        super.applyTheme(theme);
        onStateChange(getState());
    }

    @Override
    void setConstantState(@NonNull DrawableContainerState state) {
        super.setConstantState(state);
        if (state instanceof StateListState) {
            mStateListState = (StateListState) state;
        }
    }

    StateListDrawableCompat(StateListState state, Resources res) {
        // Every state list drawable has its own constant state.
        final StateListState newState = new StateListState(state, this, res);
        setConstantState(newState);
        onStateChange(getState());
    }

    /**
     * This constructor exists so subclasses can avoid calling the default
     * constructor and setting up a StateListDrawable-specific constant state.
     */
    StateListDrawableCompat(@Nullable StateListState state) {
        if (state != null) {
            setConstantState(state);
        }
    }
}
