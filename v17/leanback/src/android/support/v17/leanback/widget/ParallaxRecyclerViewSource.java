/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.LayoutManager;
import static android.support.v7.widget.RecyclerView.OnScrollListener;
import static android.support.v7.widget.RecyclerView.ViewHolder;

/**
 * Implementation of {@link ParallaxSource} class for {@link RecyclerView}. This class
 * allows users to track position of specific views inside {@link RecyclerView} relative to
 * itself. @see {@link VariableDef} for details. For reference implementation, you can look at
 * {@link android.support.v17.leanback.app.DetailsFragment}.
 */
public class ParallaxRecyclerViewSource extends ParallaxSource<ParallaxSource.IntVariable> {
    private final RecyclerView mRecylerView;
    private List<VariableDef> mVarDefs = new ArrayList<>();
    private List<ParallaxSource.IntVariable> mParallaxSourceVars = new ArrayList<>();
    private ParallaxSource.IntVariable mParentSize;
    private Listener mListener;

    /**
     * Generic container for storing view information that user is interested in tracking.
     * Using this class, users can track the view position relative to the view height i.e.
     *
     * tracking_pos = view.top + fraction * view.height() + offset
     *
     * This way we can track the start/end of the same variable just by changing the fraction
     * attribute.
     */
    static final class VariableDef {
        public int offset;
        public int viewId;
        public int adapterPosition;
        public float fraction;

        public VariableDef(int adapterPosition, int viewId, int offset, float fraction) {
            this.adapterPosition = adapterPosition;
            this.viewId = viewId;
            this.offset = offset;
            this.fraction = fraction;
        }
    }

    @Override
    public List<IntVariable> getVariables() {
        return mParallaxSourceVars;
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public IntVariable getMaxParentVisibleSize() {
        if (mParentSize == null) {
            mParentSize = new ParallaxSource.IntVariable(this);
        }
        mParentSize.setIntValue(mRecylerView.getHeight());
        return mParentSize;
    }

    /**
     * Adds the source variable to be tracked in relation to it's own height/width. This provides
     * more flexibility to users for expressing the position of source variable.
     * Following formulate is used for calculating the final position -
     * final_pos = view.top + fraction * view.height() + offset
     *
     * This way we can track the start/end of the same view by changing just the fraction
     * attribute.
     *
     * @param adapterPositon Adapter position used to find this view (given by viewId).
     * @param viewId Resource id of the view.
     * @param offset Offset used to alter the tracked position.
     * @param fraction Percentage of the view width/height to be used for calculating the
     *                 target position.
     */
    public IntVariable addVariable(int adapterPositon, int viewId, int offset, float fraction) {
        mVarDefs.add(new VariableDef(adapterPositon, viewId, offset, fraction));
        ParallaxSource.IntVariable intVariable = new ParallaxSource.IntVariable(this);
        mParallaxSourceVars.add(intVariable);
        return intVariable;
    }

    /**
     * Constructor.
     */
    public ParallaxRecyclerViewSource(RecyclerView parent) {
        this.mRecylerView = parent;
        setupScrollListener();
    }

    private void setupScrollListener() {
        mRecylerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                for (int i = 0; i < mVarDefs.size(); i++) {
                    VariableDef varDef = mVarDefs.get(i);
                    ParallaxSource.IntVariable parallaxSourceVar = mParallaxSourceVars.get(i);
                    ViewHolder viewHolder
                            = mRecylerView.findViewHolderForAdapterPosition(varDef.adapterPosition);
                    if (viewHolder == null) {
                        View firstChild = mRecylerView.getChildAt(0);
                        ViewHolder vh = mRecylerView.findContainingViewHolder(
                                firstChild);
                        int firstPosition = vh.getAdapterPosition();
                        if (firstPosition < varDef.adapterPosition) {
                            parallaxSourceVar.setIntValue(IntVariable.UNKNOWN_AFTER);
                        } else {
                            parallaxSourceVar.setIntValue(IntVariable.UNKNOWN_BEFORE);
                        }
                    } else {
                        View trackingView = viewHolder.itemView.findViewById(varDef.viewId);
                        if (trackingView == null) {
                            return;
                        }

                        Rect rect = new Rect(
                                0, 0, trackingView.getWidth(), trackingView.getHeight());
                        mRecylerView.offsetDescendantRectToMyCoords(trackingView, rect);

                        LayoutManager.Properties properties = mRecylerView.getLayoutManager()
                                .getProperties(mRecylerView.getContext(), null, 0, 0);
                        if (properties.orientation == RecyclerView.VERTICAL) {
                            parallaxSourceVar.setIntValue(rect.top + varDef.offset
                                    + (int) (varDef.fraction * rect.height()));
                        } else {
                            parallaxSourceVar.setIntValue(rect.left + varDef.offset
                                    + (int) (varDef.fraction * rect.width()));
                        }
                    }
                }

                mListener.onVariableChanged(ParallaxRecyclerViewSource.this);
            }
        });
    }
}
