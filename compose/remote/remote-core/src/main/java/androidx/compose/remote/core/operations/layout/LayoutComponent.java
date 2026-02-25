/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.OperationInterface;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.ScrollingEdgeEffect;
import androidx.compose.remote.core.TouchListener;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.ComponentData;
import androidx.compose.remote.core.operations.ComponentValue;
import androidx.compose.remote.core.operations.MatrixRestore;
import androidx.compose.remote.core.operations.MatrixSave;
import androidx.compose.remote.core.operations.MatrixTranslate;
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec;
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout;
import androidx.compose.remote.core.operations.layout.managers.LayoutManager;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation;
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.LayoutComputeOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

/** Component with modifiers and children */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LayoutComponent extends Component {

    @Nullable
    protected WidthModifierOperation mWidthModifier = null;
    @Nullable
    protected HeightModifierOperation mHeightModifier = null;
    @Nullable
    protected ZIndexModifierOperation mZIndexModifier = null;
    @Nullable
    protected GraphicsLayerModifierOperation mGraphicsLayerModifier = null;

    protected float mPaddingLeft = 0f;
    protected float mPaddingRight = 0f;
    protected float mPaddingTop = 0f;
    protected float mPaddingBottom = 0f;

    float mScrollX = 0f;
    float mScrollY = 0f;

    @Nullable
    protected ScrollDelegate mHorizontalScrollDelegate = null;
    @Nullable
    protected ScrollDelegate mVerticalScrollDelegate = null;

    @NonNull
    protected ComponentModifiers mComponentModifiers = new ComponentModifiers();

    @NonNull
    protected ArrayList<Component> mChildrenComponents = new ArrayList<>(); // members are not null

    protected boolean mChildrenHaveZIndex = false;
    private CanvasOperations mDrawContentOperations;

    /**
     * Get the horizontal scroll delegate
     */
    public @Nullable ScrollDelegate getHorizontalScrollDelegate() {
        return mHorizontalScrollDelegate;
    }

    /**
     * Get the vertical scroll delegate
     */
    public @Nullable ScrollDelegate getVerticalScrollDelegate() {
        return mVerticalScrollDelegate;
    }

    public LayoutComponent(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height) {
        super(parent, componentId, animationId, x, y, width, height);
    }

    public float getPaddingLeft() {
        return mPaddingLeft;
    }

    public float getPaddingTop() {
        return mPaddingTop;
    }

    public float getPaddingRight() {
        return mPaddingRight;
    }

    public float getPaddingBottom() {
        return mPaddingBottom;
    }

    @Nullable
    ArrayList<LayoutComputeOperation> mComputedLayoutModifiers = null;

    @Override
    public boolean hasComputedLayout() {
        return mComputedLayoutModifiers != null;
    }

    @Override
    public boolean applyComputedLayout(int type, @NonNull PaintContext context,
            @NonNull ComponentMeasure m, @NonNull ComponentMeasure parent) {
        if (mComputedLayoutModifiers != null) {
            boolean needsMeasure = false;
            for (LayoutComputeOperation modifier : mComputedLayoutModifiers) {
                if (modifier.getType() == type) {
                    needsMeasure |= modifier.applyToMeasure(context, m, parent);
                }
            }
            return needsMeasure;
        }
        return false;
    }

    @Nullable
    public WidthModifierOperation getWidthModifier() {
        return mWidthModifier;
    }

    @Nullable
    public HeightModifierOperation getHeightModifier() {
        return mHeightModifier;
    }

    @Override
    public float getZIndex() {
        if (mZIndexModifier != null) {
            return mZIndexModifier.getValue();
        }
        return mZIndex;
    }

    @Nullable
    protected LayoutComponentContent mContent = null;

    // Should be removed after ImageLayout is in
    private static final boolean USE_IMAGE_TEMP_FIX = false;

    /**
     * Set canvas operations op on this component
     */
    public void setCanvasOperations(@Nullable CanvasOperations operations) {
        mDrawContentOperations = operations;
    }

    /**
     * Allow override of the behavior
     */
    protected void getComponentsData(@NonNull LayoutComponentContent content,
            @NonNull ArrayList<Operation> data) {
        content.getData(data);
    }

    @Override
    public void inflate() {
        ArrayList<Operation> data = new ArrayList<>();
        ArrayList<Operation> supportedOperations = new ArrayList<>();

        for (Operation op : mList) {
            if (op instanceof LayoutComponentContent) {
                mContent = (LayoutComponentContent) op;
                mContent.mParent = this;
                mChildrenComponents.clear();
                LayoutComponentContent content = (LayoutComponentContent) op;
                content.getComponents(mChildrenComponents);
                if (USE_IMAGE_TEMP_FIX) {
                    if (mChildrenComponents.isEmpty() && !mContent.mList.isEmpty()) {
                        CanvasContent canvasContent =
                                new CanvasContent(-1, 0f, 0f, 0f, 0f, this, -1);
                        for (Operation opc : mContent.mList) {
                            if (opc instanceof BitmapData) {
                                canvasContent.mList.add(opc);
                                int w = ((BitmapData) opc).getWidth();
                                int h = ((BitmapData) opc).getHeight();
                                canvasContent.setWidth(w);
                                canvasContent.setHeight(h);
                            } else {
                                if (!((opc instanceof MatrixTranslate)
                                        || (opc instanceof MatrixSave)
                                        || (opc instanceof MatrixRestore))) {
                                    canvasContent.mList.add(opc);
                                }
                            }
                        }
                        if (!canvasContent.mList.isEmpty()) {
                            mContent.mList.clear();
                            mChildrenComponents.add(canvasContent);
                            canvasContent.inflate();
                        }
                    } else {
                        getComponentsData(content, data);
                    }
                } else {
                    getComponentsData(content, data);
                }
            } else if (op instanceof ModifierOperation) {
                // TODO: refactor to introduce a common interface
                // for the setParent() calls
                if (op instanceof ComponentVisibilityOperation) {
                    ((ComponentVisibilityOperation) op).setParent(this);
                }
                if (op instanceof AlignByModifierOperation) {
                    ((AlignByModifierOperation) op).setParent(this);
                }
                if (op instanceof LayoutComputeOperation) {
                    if (mComputedLayoutModifiers == null) {
                        mComputedLayoutModifiers = new ArrayList<>();
                    }
                    ((LayoutComputeOperation) op).setParent(this);
                    mComputedLayoutModifiers.add((LayoutComputeOperation) op);
                }
                if (op instanceof ScrollModifierOperation) {
                    ((ScrollModifierOperation) op).inflate(this);
                }
                mComponentModifiers.add((ModifierOperation) op);
            } else if (op instanceof ComponentData) {
                supportedOperations.add(op);
                // TODO: remove
                if (op instanceof TouchListener) {
                    ((TouchListener) op).setComponent(this);
                }
                if (op instanceof LayoutComputeOperation) {
                    ((LayoutComputeOperation) op).setParent(this);
                }
            } else {
                // nothing
            }
        }

        mList.clear();
        mList.addAll(data);
        mList.addAll(supportedOperations);
        for (Operation op : mList) {
            // TODO: this probably should be moved to a setParent call
            if (op instanceof ComponentValue) {
                ComponentValue componentValue = (ComponentValue) op;
                componentValue.setComponentId(getComponentId());
            }
            if (op instanceof TouchListener) {
                ((TouchListener) op).setComponent(this);
            }
            if (op instanceof LayoutComputeOperation) {
                ((LayoutComputeOperation) op).setParent(this);
            }
        }
        mList.add(mComponentModifiers);
        for (Component c : mChildrenComponents) {
            c.mParent = this;
            mList.add(c);
            if (c instanceof LayoutComponent && ((LayoutComponent) c).mZIndexModifier != null) {
                mChildrenHaveZIndex = true;
            }
        }

        mX = 0f;
        mY = 0f;
        mPaddingLeft = 0f;
        mPaddingTop = 0f;
        mPaddingRight = 0f;
        mPaddingBottom = 0f;

        WidthInModifierOperation widthInConstraints = null;
        HeightInModifierOperation heightInConstraints = null;

        for (OperationInterface op : mComponentModifiers.getList()) {
            if (op instanceof PaddingModifierOperation) {
                // We are accumulating padding modifiers to compute the margin
                // until we hit a dimension; the computed padding for the
                // content simply accumulate all the padding modifiers.
                float left = ((PaddingModifierOperation) op).getLeft();
                float right = ((PaddingModifierOperation) op).getRight();
                float top = ((PaddingModifierOperation) op).getTop();
                float bottom = ((PaddingModifierOperation) op).getBottom();
                mPaddingLeft += left;
                mPaddingTop += top;
                mPaddingRight += right;
                mPaddingBottom += bottom;
            } else if (op instanceof WidthModifierOperation && mWidthModifier == null) {
                mWidthModifier = (WidthModifierOperation) op;
            } else if (op instanceof HeightModifierOperation && mHeightModifier == null) {
                mHeightModifier = (HeightModifierOperation) op;
            } else if (op instanceof WidthInModifierOperation) {
                widthInConstraints = (WidthInModifierOperation) op;
            } else if (op instanceof HeightInModifierOperation) {
                heightInConstraints = (HeightInModifierOperation) op;
            } else if (op instanceof ZIndexModifierOperation) {
                mZIndexModifier = (ZIndexModifierOperation) op;
            } else if (op instanceof GraphicsLayerModifierOperation) {
                mGraphicsLayerModifier = (GraphicsLayerModifierOperation) op;
            } else if (op instanceof AnimationSpec) {
                mAnimationSpec = (AnimationSpec) op;
            } else if (op instanceof ScrollDelegate) {
                ScrollDelegate scrollDelegate = (ScrollDelegate) op;
                if (scrollDelegate.handlesHorizontalScroll()) {
                    mHorizontalScrollDelegate = scrollDelegate;
                }
                if (scrollDelegate.handlesVerticalScroll()) {
                    mVerticalScrollDelegate = scrollDelegate;
                }
            }
        }
        if (mWidthModifier == null) {
            mWidthModifier = new WidthModifierOperation(DimensionModifierOperation.Type.WRAP);
        }
        if (mHeightModifier == null) {
            mHeightModifier = new HeightModifierOperation(DimensionModifierOperation.Type.WRAP);
        }
        if (widthInConstraints != null) {
            mWidthModifier.setWidthIn(widthInConstraints);
        }
        if (heightInConstraints != null) {
            mHeightModifier.setHeightIn(heightInConstraints);
        }

        if (mAnimationSpec != AnimationSpec.DEFAULT) {
            for (int i = 0; i < mChildrenComponents.size(); i++) {
                Component c = mChildrenComponents.get(i);
                if (c != null && c.getAnimationSpec() == AnimationSpec.DEFAULT) {
                    c.setAnimationSpec(mAnimationSpec);
                }
            }
        }

        setWidth(computeModifierDefinedWidth(null, true));
        setHeight(computeModifierDefinedHeight(null, true));
    }

    @NonNull
    @Override
    public String toString() {
        return "UNKNOWN LAYOUT_COMPONENT";
    }

    @Override
    public void getLocationInWindow(@NonNull RemoteContext context, float @NonNull [] value,
            boolean forSelf) {
        if (context.getTouchVersion() == LayoutManager.FIX_TOUCH_EVENT) {
            value[0] += mX;
            value[1] += mY;
            if (!forSelf) {
                value[0] += mPaddingLeft + getScrollX();
                value[1] += mPaddingTop + getScrollY();
            }
        } else {
            value[0] += mX + mPaddingLeft;
            value[1] += mY + mPaddingTop;
        }
        if (mParent != null) {
            mParent.getLocationInWindow(context, value, false);
        }
    }

    @Override
    public float getScrollX() {
        if (mHorizontalScrollDelegate != null) {
            return mHorizontalScrollDelegate.getScrollX(mScrollX);
        }
        return mScrollX;
    }

    public void setScrollX(float value) {
        mScrollX = value;
    }

    @Override
    public float getScrollY() {
        if (mVerticalScrollDelegate != null) {
            return mVerticalScrollDelegate.getScrollY(mScrollY);
        }
        return mScrollY;
    }

    public void setScrollY(float value) {
        mScrollY = value;
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (mDrawContentOperations != null) {
            context.save();
            context.translate(mX, mY);
            mDrawContentOperations.paint(context);
            context.restore();
            return;
        }
        super.paint(context);
    }

    /**
     * Paint the component content. Used by the DrawContent operation. (back out mX/mY -- TODO:
     * refactor paintingComponent instead, to not include mX/mY etc.)
     *
     * @param context painting context
     */
    public void drawContent(@NonNull PaintContext context) {
        context.save();
        context.translate(-mX, -mY);
        paintingComponent(context);
        context.restore();
    }

    protected final HashMap<Integer, Object> mCachedAttributes = new HashMap<>();

    /**
     * This allow subclasses to handle the list of operations differently
     */
    protected void handleOperations(@NonNull RemoteContext context,
            @NonNull ArrayList<Operation> operations) {
        // nothing here
    }

    @Override
    public void paintingComponent(@NonNull PaintContext context) {
        Component prev = context.getContext().mLastComponent;
        RemoteContext remoteContext = context.getContext();

        remoteContext.mLastComponent = this;
        context.save();
        context.translate(mX, mY);
        if (context.isVisualDebug()) {
            debugBox(this, context);
        }
        if (mGraphicsLayerModifier != null) {
            context.startGraphicsLayer((int) getWidth(), (int) getHeight());
            mCachedAttributes.clear();
            mGraphicsLayerModifier.fillInAttributes(mCachedAttributes);
            context.setGraphicsLayer(mCachedAttributes);
        }
        // Canvas already does its own handling of internal operations
        if (!(this instanceof CanvasLayout)) {
            for (Operation op : mList) {
                if (op instanceof ComponentModifiers) {
                    continue;
                }
                if (!(op instanceof ComponentData)) {
                    continue;
                }
                if (op instanceof VariableSupport && op.isDirty()) {
                    ((VariableSupport) op).updateVariables(remoteContext);
                }
                op.apply(remoteContext);
            }
        }
        mComponentModifiers.paint(context);
        float tx = mPaddingLeft + getScrollX();
        float ty = mPaddingTop + getScrollY();
        context.translate(tx, ty);
        handleOperations(remoteContext, mList);
        if (mHorizontalScrollDelegate != null) {
            context.save();
            mHorizontalScrollDelegate.applyEdgeEffect(context, this,
                    ScrollingEdgeEffect.PRE_DRAW);
        }
        if (mVerticalScrollDelegate != null) {
            context.save();
            mVerticalScrollDelegate.applyEdgeEffect(context, this,
                    ScrollingEdgeEffect.PRE_DRAW);
        }
        if (mChildrenHaveZIndex) {
            // TODO -- should only sort when something has changed
            ArrayList<Component> sorted = new ArrayList<Component>(mChildrenComponents);
            sorted.sort((a, b) -> (int) (a.getZIndex() - b.getZIndex()));
            for (Component child : sorted) {
                if (child.isDirty() && child instanceof VariableSupport) {
                    child.updateVariables(context.getContext());
                    child.markNotDirty();
                }
                remoteContext.incrementOpCount();
                child.paint(context);
            }
        } else {
            for (Component child : mChildrenComponents) {
                if (child.isDirty() && child instanceof VariableSupport) {
                    child.updateVariables(context.getContext());
                    child.markNotDirty();
                }
                remoteContext.incrementOpCount();
                child.paint(context);
            }
        }
        if (mGraphicsLayerModifier != null) {
            context.endGraphicsLayer();
        }
        if (mHorizontalScrollDelegate != null) {
            mHorizontalScrollDelegate.applyEdgeEffect(context, this,
                    ScrollingEdgeEffect.POST_DRAW);
            context.restore();
        }
        if (mVerticalScrollDelegate != null) {
            mVerticalScrollDelegate.applyEdgeEffect(context, this,
                    ScrollingEdgeEffect.POST_DRAW);
            context.restore();
        }
        context.translate(-tx, -ty);
        context.restore();
        context.getContext().mLastComponent = prev;
    }

    /** Traverse the modifiers to compute indicated dimension */
    public float computeModifierDefinedWidth(@Nullable RemoteContext context) {
        return computeModifierDefinedWidth(context, false);
    }

    /** Traverse the modifiers to compute indicated dimension */
    public float computeModifierDefinedWidth(@Nullable RemoteContext context, boolean isMin) {
        float s = 0f;
        float e = 0f;
        float w = 0f;
        for (OperationInterface c : mComponentModifiers.getList()) {
            if (context != null && c.isDirty() && c instanceof VariableSupport) {
                ((VariableSupport) c).updateVariables(context);
                c.markNotDirty();
            }
            if (c instanceof WidthModifierOperation) {
                WidthModifierOperation o = (WidthModifierOperation) c;
                if (o.getType() == DimensionModifierOperation.Type.EXACT
                        || o.getType() == DimensionModifierOperation.Type.EXACT_DP) {
                    w = o.getValue();
                } else if (o.getType() == DimensionModifierOperation.Type.FILL
                        || o.getType() == DimensionModifierOperation.Type.FILL_PARENT_MAX_WIDTH) {
                    w = isMin ? 0f : Float.MAX_VALUE;
                }
                WidthInModifierOperation widthIn = o.getWidthIn();
                if (widthIn != null) {
                    w = Math.max(w, widthIn.getMin());
                }
                break;
            }
            if (c instanceof PaddingModifierOperation) {
                PaddingModifierOperation pop = (PaddingModifierOperation) c;
                s += pop.getLeft();
                e += pop.getRight();
            }
        }
        return s + w + e;
    }

    /**
     * Traverse the modifiers to compute padding width
     *
     * @param padding output start and end padding values
     * @return padding width
     */
    public float computeModifierDefinedPaddingWidth(float @NonNull [] padding) {
        float s = 0f;
        float e = 0f;
        for (OperationInterface c : mComponentModifiers.getList()) {
            if (c instanceof PaddingModifierOperation) {
                PaddingModifierOperation pop = (PaddingModifierOperation) c;
                s += pop.getLeft();
                e += pop.getRight();
            }
        }
        padding[0] = s;
        padding[1] = e;
        return s + e;
    }

    /** Traverse the modifiers to compute indicated dimension */
    public float computeModifierDefinedHeight(@Nullable RemoteContext context) {
        return computeModifierDefinedHeight(context, false);
    }

    /** Traverse the modifiers to compute indicated dimension */
    public float computeModifierDefinedHeight(@Nullable RemoteContext context, boolean isMin) {
        float t = 0f;
        float b = 0f;
        float h = 0f;
        for (OperationInterface c : mComponentModifiers.getList()) {
            if (context != null && c.isDirty() && c instanceof VariableSupport) {
                ((VariableSupport) c).updateVariables(context);
                c.markNotDirty();
            }
            if (c instanceof HeightModifierOperation) {
                HeightModifierOperation o = (HeightModifierOperation) c;
                if (o.getType() == DimensionModifierOperation.Type.EXACT
                        || o.getType() == DimensionModifierOperation.Type.EXACT_DP) {
                    h = o.getValue();
                } else if (o.getType() == DimensionModifierOperation.Type.FILL
                        || o.getType() == DimensionModifierOperation.Type.FILL_PARENT_MAX_HEIGHT) {
                    h = isMin ? 0f : Float.MAX_VALUE;
                }
                HeightInModifierOperation heightIn = o.getHeightIn();
                if (heightIn != null) {
                    h = Math.max(h, heightIn.getMin());
                }
                break;
            }
            if (c instanceof PaddingModifierOperation) {
                PaddingModifierOperation pop = (PaddingModifierOperation) c;
                t += pop.getTop();
                b += pop.getBottom();
            }
        }
        return t + h + b;
    }

    /**
     * Traverse the modifiers to compute padding height
     *
     * @param padding output top and bottom padding values
     * @return padding height
     */
    public float computeModifierDefinedPaddingHeight(float @NonNull [] padding) {
        float t = 0f;
        float b = 0f;
        for (OperationInterface c : mComponentModifiers.getList()) {
            if (c instanceof PaddingModifierOperation) {
                PaddingModifierOperation pop = (PaddingModifierOperation) c;
                t += pop.getTop();
                b += pop.getBottom();
            }
        }
        padding[0] = t;
        padding[1] = b;
        return t + b;
    }

    @NonNull
    public ComponentModifiers getComponentModifiers() {
        return mComponentModifiers;
    }

    @NonNull
    public ArrayList<Component> getChildrenComponents() {
        return mChildrenComponents;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer
                .addTags(SerializeTags.LAYOUT_COMPONENT)
                .add("paddingLeft", mPaddingLeft)
                .add("paddingRight", mPaddingRight)
                .add("paddingTop", mPaddingTop)
                .add("paddingBottom", mPaddingBottom);
    }

    @Override
    public <T> @Nullable T selfOrModifier(@NonNull Class<T> operationClass) {
        if (operationClass.isInstance(this)) {
            return operationClass.cast(this);
        }

        for (ModifierOperation op : mComponentModifiers.getList()) {
            if (operationClass.isInstance(op)) {
                return operationClass.cast(op);
            }
        }

        return null;
    }

    @Override
    public void registerVariables(@NonNull RemoteContext context) {
        if (mDrawContentOperations != null) {
            mDrawContentOperations.registerListening(context);
        }

        for (Operation operation : mList) {
            if (operation instanceof VariableSupport) {
                VariableSupport variableSupport = (VariableSupport) operation;
                variableSupport.registerListening(context);
            }
            if (operation instanceof ComponentValue) {
                ComponentValue v = (ComponentValue) operation;
                this.addComponentValue(v);
            }
        }
    }
}
