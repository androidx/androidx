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

package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */

public class StateSet {
    public static final String TAG = "ConstraintLayoutStates";
    private static final boolean DEBUG = false;
    int mDefaultState = -1;

    int mCurrentStateId = -1; // default
    int mCurrentConstraintNumber = -1; // default
    private SparseArray<State> mStateList = new SparseArray<>();
    @SuppressWarnings("unused")
    private ConstraintsChangedListener mConstraintsChangedListener = null;

    /**
     * Parse a StateSet
     * @param context
     * @param parser
     */
    public StateSet(Context context, XmlPullParser parser) {
        load(context, parser);
    }

    /**
     * Load a constraint set from a constraintSet.xml file
     *
     * @param context    the context for the inflation
     * @param parser  mId of xml file in res/xml/
     */
    private void load(Context context, XmlPullParser parser) {
        if (DEBUG) {
            Log.v(TAG, "#########load stateSet###### ");
        }
        // Parse the stateSet attributes
        AttributeSet attrs = Xml.asAttributeSet(parser);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StateSet);
        final int count = a.getIndexCount();

        for (int i = 0; i < count; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.StateSet_defaultState) {
                mDefaultState = a.getResourceId(attr, mDefaultState);
            }
        }
        a.recycle();

        try {
            Variant match;
            State state = null;
            for (int eventType = parser.getEventType();
                    eventType != XmlResourceParser.END_DOCUMENT;
                    eventType = parser.next()) {

                switch (eventType) {
                    case XmlResourceParser.START_DOCUMENT:
                    case XmlResourceParser.TEXT:
                        break;
                    case XmlResourceParser.START_TAG:
                        String tagName = parser.getName();
                        switch(tagName) {
                            case "LayoutDescription":
                                break;
                            case "StateSet":
                                break;
                            case "State":
                                state = new State(context, parser);
                                mStateList.put(state.mId, state);
                                break;
                            case "Variant":
                                match = new Variant(context, parser);
                                if (state != null) {
                                    state.add(match);
                                }
                                break;

                            default:
                                if (DEBUG) {
                                    Log.v(TAG, "unknown tag " + tagName);
                                }
                        }

                        break;
                    case XmlResourceParser.END_TAG:
                        if ("StateSet".equals(parser.getName())) {
                            if (DEBUG) {
                                Log.v(TAG, "############ finished parsing state set");
                            }
                            return;
                        }
                        break;
                }
            }

        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        } catch (IOException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        }
    }

    /**
     * will the layout need to change
     * @param id
     * @param width
     * @param height
     * @return
     */
    public boolean needsToChange(int id, float width, float height) {
        if (mCurrentStateId != id) {
            return true;
        }

        State state = (id == -1) ? mStateList.valueAt(0) : mStateList.get(mCurrentStateId);

        if (mCurrentConstraintNumber != -1) {
            if (state.mVariants.get(mCurrentConstraintNumber).match(width, height)) {
                return false;
            }
        }

        if (mCurrentConstraintNumber == state.findMatch(width, height)) {
            return false;
        }
        return true;
    }

    /**
     * listen for changes in constraintSet
     * @param constraintsChangedListener
     */
    public void setOnConstraintsChanged(ConstraintsChangedListener constraintsChangedListener) {
        this.mConstraintsChangedListener = constraintsChangedListener;
    }

    /**
     * Get the constraint id for a state
     * @param id
     * @param width
     * @param height
     * @return
     */
    public int stateGetConstraintID(int id, int width, int height) {
        return updateConstraints(-1, id, width, height);
    }

    /**
     * converts a state to a constraintSet
     *
     * @param currentConstrainSettId
     * @param stateId
     * @param width
     * @param height
     * @return
     */
    public int convertToConstraintSet(int currentConstrainSettId,
                                      int stateId,
                                      float width,
                                      float height) {
        State state = mStateList.get(stateId);
        if (state == null) {
            return stateId;
        }
        if (width == -1 || height == -1) {            // for the case without width/height matching
            if (state.mConstraintID == currentConstrainSettId) {
                return currentConstrainSettId;
            }
            for (Variant mVariant : state.mVariants) {
                if (currentConstrainSettId == mVariant.mConstraintID) {
                    return currentConstrainSettId;
                }
            }
            return state.mConstraintID;
        } else {
            Variant match = null;
            for (Variant mVariant : state.mVariants) {
                if (mVariant.match(width, height)) {
                    if (currentConstrainSettId == mVariant.mConstraintID) {
                        return currentConstrainSettId;
                    }
                    match = mVariant;
                }
            }
            if (match != null) {
                return match.mConstraintID;
            }

            return state.mConstraintID;
        }
    }

    /**
     * Update the Constraints
     * @param currentId
     * @param id
     * @param width
     * @param height
     * @return
     */
    public int updateConstraints(int currentId, int id, float width, float height) {
        if (currentId == id) {
            State state;
            if (id == -1) {
                state = mStateList.valueAt(0); // id not being used take the first
            } else {
                state = mStateList.get(mCurrentStateId);

            }
            if (state == null) {
                return -1;
            }
            if (mCurrentConstraintNumber != -1) {
                if (state.mVariants.get(currentId).match(width, height)) {
                    return currentId;
                }
            }
            int match = state.findMatch(width, height);
            if (currentId == match) {
                return currentId;
            }

            return (match == -1) ? state.mConstraintID : state.mVariants.get(match).mConstraintID;

        } else  {
            State state = mStateList.get(id);
            if (state == null) {
                return  -1;
            }
            int match = state.findMatch(width, height);
            return (match == -1) ? state.mConstraintID :  state.mVariants.get(match).mConstraintID;
        }

    }

    /////////////////////////////////////////////////////////////////////////
    //      This represents one state
    /////////////////////////////////////////////////////////////////////////
    static class State {
        int mId;
        ArrayList<Variant> mVariants = new ArrayList<>();
        int mConstraintID = -1;
        boolean mIsLayout = false;

        State(Context context, XmlPullParser parser) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.State);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.State_android_id) {
                    mId = a.getResourceId(attr, mId);
                } else if (attr == R.styleable.State_constraints) {
                    mConstraintID = a.getResourceId(attr, mConstraintID);
                    String type = context.getResources().getResourceTypeName(mConstraintID);
                    @SuppressWarnings("unused")
                    String name = context.getResources().getResourceName(mConstraintID);

                    if ("layout".equals(type)) {
                        mIsLayout = true;
                    }
                }
            }
            a.recycle();
        }

        void add(Variant size) {
            mVariants.add(size);
        }

        public int findMatch(float width, float height) {
            for (int i = 0; i < mVariants.size(); i++) {
                if (mVariants.get(i).match(width, height)) {
                    return i;
                }
            }
            return -1;
        }
    }

    static class Variant {
        int mId;
        float mMinWidth = Float.NaN;
        float mMinHeight = Float.NaN;
        float mMaxWidth = Float.NaN;
        float mMaxHeight = Float.NaN;
        int mConstraintID = -1;
        boolean mIsLayout = false;

        Variant(Context context, XmlPullParser parser) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Variant);
            final int count = a.getIndexCount();
            if (DEBUG) {
                Log.v(TAG, "############### Variant");
            }

            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.Variant_constraints) {
                    mConstraintID = a.getResourceId(attr, mConstraintID);
                    String type = context.getResources().getResourceTypeName(mConstraintID);
                    @SuppressWarnings("unused")
                    String name = context.getResources().getResourceName(mConstraintID);

                    if ("layout".equals(type)) {
                        mIsLayout = true;
                    }
                } else if (attr == R.styleable.Variant_region_heightLessThan) {
                    mMaxHeight = a.getDimension(attr, mMaxHeight);
                } else if (attr == R.styleable.Variant_region_heightMoreThan) {
                    mMinHeight = a.getDimension(attr, mMinHeight);
                } else if (attr == R.styleable.Variant_region_widthLessThan) {
                    mMaxWidth = a.getDimension(attr, mMaxWidth);
                } else if (attr == R.styleable.Variant_region_widthMoreThan) {
                    mMinWidth = a.getDimension(attr, mMinWidth);
                } else {
                    Log.v(TAG, "Unknown tag");
                }
            }
            a.recycle();
            if (DEBUG) {
                Log.v(TAG, "############### Variant");
                if (!Float.isNaN(mMinWidth)) {
                    Log.v(TAG, "############### Variant mMinWidth " + mMinWidth);
                }
                if (!Float.isNaN(mMinHeight)) {
                    Log.v(TAG, "############### Variant mMinHeight " + mMinHeight);
                }
                if (!Float.isNaN(mMaxWidth)) {
                    Log.v(TAG, "############### Variant mMaxWidth " + mMaxWidth);
                }
                if (!Float.isNaN(mMaxHeight)) {
                    Log.v(TAG, "############### Variant mMinWidth " + mMaxHeight);
                }
            }
        }

        boolean match(float widthDp, float heightDp) {
            if (DEBUG) {
                Log.v(TAG, "width = " + (int) widthDp
                        + " < " + mMinWidth + " && " + (int) widthDp + " > " + mMaxWidth
                        + " height = " + (int) heightDp
                        + " < " + mMinHeight + " && " + (int) heightDp + " > " + mMaxHeight);
            }
            if (!Float.isNaN(mMinWidth)) {
                if (widthDp < mMinWidth) return false;
            }
            if (!Float.isNaN(mMinHeight)) {
                if (heightDp < mMinHeight) return false;
            }
            if (!Float.isNaN(mMaxWidth)) {
                if (widthDp > mMaxWidth) return false;
            }
            if (!Float.isNaN(mMaxHeight)) {
                if (heightDp > mMaxHeight) return false;
            }
            return true;
        }
    }

}
