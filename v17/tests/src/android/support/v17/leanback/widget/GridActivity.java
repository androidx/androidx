/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v17.leanback.widget;

import android.support.v17.leanback.tests.R;
import android.support.v7.widget.RecyclerView;
import android.support.v17.leanback.widget.BaseGridView;
import android.support.v17.leanback.widget.OnChildSelectedListener;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * @hide from javadoc
 */
public class GridActivity extends Activity {

    private static final String TAG = "GridActivity";

    public static final String EXTRA_LAYOUT_RESOURCE_ID = "layoutResourceId";
    public static final String EXTRA_NUM_ITEMS = "numItems";
    public static final String EXTRA_ITEMS = "items";
    public static final String EXTRA_ITEMS_FOCUSABLE = "itemsFocusable";
    public static final String EXTRA_STAGGERED = "staggered";
    public static final String EXTRA_REQUEST_LAYOUT_ONFOCUS = "requestLayoutOnFocus";
    public static final String EXTRA_REQUEST_FOCUS_ONLAYOUT = "requstFocusOnLayout";
    public static final String EXTRA_CHILD_LAYOUT_ID = "childLayoutId";
    public static final String EXTRA_SECONDARY_SIZE_ZERO = "secondarySizeZero";
    /**
     * Class that implements GridWidgetTest.ViewTypeProvider for creating different
     * view types for each position.
     */
    public static final String EXTRA_VIEWTYPEPROVIDER_CLASS = "viewtype_class";
    /**
     * Class that implements GridWidgetTest.ItemAlignmentFacetProvider for creating different
     * ItemAlignmentFacet for each ViewHolder.
     */
    public static final String EXTRA_ITEMALIGNMENTPROVIDER_CLASS = "itemalignment_class";
    /**
     * Class that implements GridWidgetTest.ItemAlignmentFacetProvider for creating different
     * ItemAlignmentFacet for a given viewType.
     */
    public static final String EXTRA_ITEMALIGNMENTPROVIDER_VIEWTYPE_CLASS =
            "itemalignment_viewtype_class";
    public static final String SELECT_ACTION = "android.test.leanback.widget.SELECT";

    static final int DEFAULT_NUM_ITEMS = 100;
    static final boolean DEFAULT_STAGGERED = true;
    static final boolean DEFAULT_REQUEST_LAYOUT_ONFOCUS = false;
    static final boolean DEFAULT_REQUEST_FOCUS_ONLAYOUT = false;

    private static final boolean DEBUG = false;

    int mLayoutId;
    int mOrientation;
    int mNumItems;
    int mChildLayout;
    boolean mStaggered;
    boolean mRequestLayoutOnFocus;
    boolean mRequestFocusOnLayout;
    boolean mSecondarySizeZero;
    GridWidgetTest.ViewTypeProvider mViewTypeProvider;
    GridWidgetTest.ItemAlignmentFacetProvider mAlignmentProvider;
    GridWidgetTest.ItemAlignmentFacetProvider mAlignmentViewTypeProvider;

    int[] mGridViewLayoutSize;
    BaseGridView mGridView;
    int[] mItemLengths;
    boolean[] mItemFocusables;

    private int mBoundCount;

    private View createView() {

        View view = getLayoutInflater().inflate(mLayoutId, null, false);
        mGridView = (BaseGridView) view.findViewById(R.id.gridview);
        mOrientation = mGridView instanceof HorizontalGridView ? BaseGridView.HORIZONTAL :
                BaseGridView.VERTICAL;
        mGridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_BOTH_EDGE);
        mGridView.setWindowAlignmentOffsetPercent(35);
        mGridView.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(ViewGroup parent, View view, int position, long id) {
                if (DEBUG) Log.d(TAG, "onChildSelected position=" + position +  " id="+id);
            }
        });
        return view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();

        mLayoutId = intent.getIntExtra(EXTRA_LAYOUT_RESOURCE_ID, R.layout.horizontal_grid);
        mChildLayout = intent.getIntExtra(EXTRA_CHILD_LAYOUT_ID, -1);
        mStaggered = intent.getBooleanExtra(EXTRA_STAGGERED, DEFAULT_STAGGERED);
        mRequestLayoutOnFocus = intent.getBooleanExtra(EXTRA_REQUEST_LAYOUT_ONFOCUS,
                DEFAULT_REQUEST_LAYOUT_ONFOCUS);
        mRequestFocusOnLayout = intent.getBooleanExtra(EXTRA_REQUEST_FOCUS_ONLAYOUT,
                DEFAULT_REQUEST_FOCUS_ONLAYOUT);
        mSecondarySizeZero = intent.getBooleanExtra(EXTRA_SECONDARY_SIZE_ZERO, false);
        mItemLengths = intent.getIntArrayExtra(EXTRA_ITEMS);
        mItemFocusables = intent.getBooleanArrayExtra(EXTRA_ITEMS_FOCUSABLE);
        String alignmentClass = intent.getStringExtra(EXTRA_ITEMALIGNMENTPROVIDER_CLASS);
        String alignmentViewTypeClass =
                intent.getStringExtra(EXTRA_ITEMALIGNMENTPROVIDER_VIEWTYPE_CLASS);
        String viewTypeClass = intent.getStringExtra(EXTRA_VIEWTYPEPROVIDER_CLASS);
        try {
            if (alignmentClass != null) {
                mAlignmentProvider = (GridWidgetTest.ItemAlignmentFacetProvider)
                        Class.forName(alignmentClass).newInstance();
            }
            if (alignmentViewTypeClass != null) {
                mAlignmentViewTypeProvider = (GridWidgetTest.ItemAlignmentFacetProvider)
                        Class.forName(alignmentViewTypeClass).newInstance();
            }
            if (viewTypeClass != null) {
                mViewTypeProvider = (GridWidgetTest.ViewTypeProvider)
                        Class.forName(viewTypeClass).newInstance();
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        super.onCreate(savedInstanceState);

        if (DEBUG) Log.v(TAG, "onCreate " + this);

        RecyclerView.Adapter adapter = new MyAdapter();

        View view = createView();
        if (mItemLengths == null) {
            mNumItems = intent.getIntExtra(EXTRA_NUM_ITEMS, DEFAULT_NUM_ITEMS);
            mItemLengths = new int[mNumItems];
            for (int i = 0; i < mItemLengths.length; i++) {
                if (mOrientation == BaseGridView.HORIZONTAL) {
                    mItemLengths[i] = mStaggered ? (int)(Math.random() * 180) + 180 : 240;
                } else {
                    mItemLengths[i] = mStaggered ? (int)(Math.random() * 120) + 120 : 160;
                }
            }
        } else {
            mNumItems = mItemLengths.length;
        }

        mGridView.setAdapter(new MyAdapter());
        setContentView(view);
    }

    void rebindToNewAdapter() {
        mGridView.setAdapter(new MyAdapter());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.v(TAG, "onNewIntent " + intent+ " "+this);
        if (intent.getAction().equals(SELECT_ACTION)) {
            int position = intent.getIntExtra("SELECT_POSITION", -1);
            if (position >= 0) {
                mGridView.setSelectedPosition(position);
            }
        }
        super.onNewIntent(intent);
    }

    private OnFocusChangeListener mItemFocusChangeListener = new OnFocusChangeListener() {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                v.setBackgroundColor(Color.YELLOW);
            } else {
                v.setBackgroundColor(Color.LTGRAY);
            }
            if (mRequestLayoutOnFocus) {
                RecyclerView.ViewHolder vh = mGridView.getChildViewHolder(v);
                int position = vh.getAdapterPosition();
                updateSize(v, position);
            }
        }
    };

    private OnFocusChangeListener mSubItemFocusChangeListener = new OnFocusChangeListener() {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                v.setBackgroundColor(Color.YELLOW);
            } else {
                v.setBackgroundColor(Color.LTGRAY);
            }
        }
    };

    void resetBoundCount() {
        mBoundCount = 0;
    }

    int getBoundCount() {
       return mBoundCount;
    }

    void swap(int index1, int index2) {
        if (index1 == index2) {
            return;
        } else if (index1 > index2) {
            int index = index1;
            index1 = index2;
            index2 = index;
        }
        int value = mItemLengths[index1];
        mItemLengths[index1] = mItemLengths[index2];
        mItemLengths[index2] = value;
        mGridView.getAdapter().notifyItemMoved(index1, index2);
        mGridView.getAdapter().notifyItemMoved(index2 - 1, index1);
    }

    void changeArraySize(int length) {
        mNumItems = length;
        mGridView.getAdapter().notifyDataSetChanged();
    }

    int[] removeItems(int index, int length) {
        int[] removed = new int[length];
        System.arraycopy(mItemLengths, index, removed, 0, length);
        System.arraycopy(mItemLengths, index + length, mItemLengths, index,
                mNumItems - index - length);
        mNumItems -= length;
        mGridView.getAdapter().notifyItemRangeRemoved(index, length);
        return removed;
    }

    void addItems(int index, int[] items) {
        int length = items.length;
        if (mItemLengths.length < mNumItems + length) {
            int[] array = new int[mNumItems + length];
            System.arraycopy(mItemLengths, 0, array, 0, mNumItems);
            mItemLengths = array;
        }
        System.arraycopy(mItemLengths, index, mItemLengths, index + length, mNumItems - index);
        System.arraycopy(items, 0, mItemLengths, index, length);
        mNumItems += length;
        mGridView.getAdapter().notifyItemRangeInserted(index, length);
    }

    class MyAdapter extends RecyclerView.Adapter implements FacetProviderAdapter {

        @Override
        public int getItemViewType(int position) {
            if (mViewTypeProvider != null) {
                return mViewTypeProvider.getViewType(position);
            }
            return 0;
        }

        @Override
        public FacetProvider getFacetProvider(int viewType) {
            final Object alignmentFacet = mAlignmentViewTypeProvider != null?
                mAlignmentViewTypeProvider.getItemAlignmentFacet(viewType) : null;
            if (alignmentFacet != null) {
                return new FacetProvider() {
                    @Override
                    public Object getFacet(Class facetClass) {
                        if (facetClass.equals(ItemAlignmentFacet.class)) {
                            return alignmentFacet;
                        }
                        return null;
                    }
                };
            }
            return null;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (DEBUG) Log.v(TAG, "createViewHolder " + viewType);
            if (mChildLayout != -1) {
                final View view = getLayoutInflater().inflate(mChildLayout, null, false);
                ArrayList<View> focusables = new ArrayList<View>();
                view.addFocusables(focusables, View.FOCUS_UP);
                for (int i = 0; i < focusables.size(); i++) {
                    View f = focusables.get(i);
                    f.setBackgroundColor(Color.LTGRAY);
                    f.setOnFocusChangeListener(new OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
                                v.setBackgroundColor(Color.YELLOW);
                            } else {
                                v.setBackgroundColor(Color.LTGRAY);
                            }
                            if (mRequestLayoutOnFocus) {
                                if (v == view) {
                                    RecyclerView.ViewHolder vh = mGridView.getChildViewHolder(v);
                                    int position = vh.getAdapterPosition();
                                    updateSize(v, position);
                                }
                                view.requestLayout();
                            }
                        }
                    });
                }
                ViewHolder holder = new ViewHolder(view);
                return holder;
            }
            TextView textView = new TextView(parent.getContext()) {
                @Override
                protected void onLayout(boolean change, int left, int top, int right, int bottom) {
                    super.onLayout(change, left, top, right, bottom);
                    if (mRequestFocusOnLayout) {
                        if (hasFocus()) {
                            clearFocus();
                            requestFocus();
                        }
                    }
                }
            };
            textView.setTextColor(Color.BLACK);
            textView.setOnFocusChangeListener(mItemFocusChangeListener);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position) {
            if (DEBUG) Log.v(TAG, "bindViewHolder " + position + " " + baseHolder);
            mBoundCount++;
            ViewHolder holder = (ViewHolder) baseHolder;
            if (mAlignmentProvider != null) {
                holder.mItemAlignment = mAlignmentProvider.getItemAlignmentFacet(position);
            } else {
                holder.mItemAlignment = null;
            }
            if (mChildLayout == -1) {
                ((TextView) holder.itemView).setText("Item "+mItemLengths[position]
                        + " type=" + getItemViewType(position));
                boolean focusable = true;
                if (mItemFocusables != null) {
                    focusable = mItemFocusables[position];
                }
                ((TextView) holder.itemView).setFocusable(focusable);
                ((TextView) holder.itemView).setFocusableInTouchMode(focusable);
                holder.itemView.setBackgroundColor(Color.LTGRAY);
            } else {
                if (holder.itemView instanceof TextView) {
                    ((TextView) holder.itemView).setText("Item "+mItemLengths[position]
                            + " type=" + getItemViewType(position));
                }
            }
            updateSize(holder.itemView, position);
        }

        @Override
        public int getItemCount() {
            return mNumItems;
        }

    }

    void updateSize(View view, int position) {
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(0, 0);
        }
        if (mOrientation == BaseGridView.HORIZONTAL) {
            p.width = mItemLengths[position] + (mRequestLayoutOnFocus && view.hasFocus() ? 1 : 0);
            p.height = mSecondarySizeZero ? 0 : 80;
        } else {
            p.width = mSecondarySizeZero ? 0 : 240;
            p.height = mItemLengths[position] + (mRequestLayoutOnFocus && view.hasFocus() ? 1 : 0);
        }
        view.setLayoutParams(p);
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements FacetProvider {

        ItemAlignmentFacet mItemAlignment;
        public ViewHolder(View v) {
            super(v);
        }

        @Override
        public Object getFacet(Class facetClass) {
            if (facetClass.equals(ItemAlignmentFacet.class)) {
                return mItemAlignment;
            }
            return null;
        }
    }
}
