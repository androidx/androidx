/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.operations.layout.managers;

import static androidx.compose.remote.core.operations.layout.managers.Custom.CustomProperty.TEXT_RETURN;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.CustomContext;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * LayoutManager subclass designed to host and configure platform-specific native custom components.
 * Handles generic key-value configuration parameters and delegates native lifecycle and
 * layout actions to the platform-specific PaintContext via {@link CustomContext}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Custom extends LayoutManager implements VariableSupport {
    float mTouchLocationX = 0;
    float mTouchLocationY = 0;

    public static class CustomProperty {
        public final short mType;
        public final short mDataType;
        public final int mIntValue;
        public final float mFloatValue;
        private String mStringOutValue = "";
        private float mFloatOutValue;
        private boolean mNeedsUpdate = true;
        public static final short INT_PROP = 0;
        public static final short FLOAT_PROP = 1;
        public static final short STRING_PROP = 2;
        public static final short FLOAT_RETURN = 3;
        public static final short TEXT_RETURN = 4;

        public boolean isFloat() {
            return (mDataType & 1) == 1;
        }

        public boolean isString() {
            return (mDataType == STRING_PROP || mDataType == TEXT_RETURN);
        }


        public CustomProperty(short type, short dataType, int intValue) {
            mType = type;
            mDataType = dataType;
            mIntValue = intValue;
            mFloatValue = 0;
        }

        public CustomProperty(short type, short dataType, float value) {
            mType = type;
            mDataType = dataType;
            mFloatValue = value;
            mIntValue = 0;
        }

    }

    private int mConfigId = -1;
    private String mConfig;
    private String mConfigValue;

    @NonNull
    private final ArrayList<CustomProperty> mProperties = new ArrayList<>();
    private boolean mInitialized = false;

    public Custom(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height,
            int configId,
            @NonNull String config,
            @NonNull List<CustomProperty> properties) {
        super(parent, componentId, animationId, x, y, width, height);
        this.mConfigId = configId;
        this.mConfig = config;
        this.mConfigValue = config;
        this.mProperties.addAll(properties);
    }

    public Custom(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int configId,
            @NonNull String config,
            @NonNull List<CustomProperty> properties) {
        this(parent, componentId, animationId, 0, 0, 0, 0, configId, config, properties);
    }


    @Override
    public void registerListening(@NonNull RemoteContext context) {

        for (CustomProperty prop : mProperties) {
            if (prop.isFloat() && Float.isNaN(prop.mFloatValue)) {
                context.listensTo(Utils.idFromNan(prop.mFloatValue), this);
            } else if (prop.isString()) {
                context.listensTo(prop.mIntValue, this);
            }
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        super.updateVariables(context);
        if (mConfigId != -1) {
            mConfigValue = context.getText(mConfigId);
        } else {
            mConfigValue = mConfig;
        }

        // Resolve any dynamic string properties
        for (CustomProperty prop : mProperties) {
            if (prop.isString()) {
                String val = context.getText(prop.mIntValue);
                if (prop.mStringOutValue == null || !prop.mStringOutValue.equals(val)) {
                    prop.mStringOutValue = val;
                    prop.mNeedsUpdate = true;
                }
                prop.mStringOutValue = val;
            } else if (prop.isFloat()) {
                float tmp =
                        Float.isNaN(prop.mFloatValue)
                                ? context.getFloat(Utils.idFromNan(prop.mFloatValue))
                                : prop.mFloatValue;
                if (prop.mFloatOutValue != tmp) {
                    prop.mNeedsUpdate = true;
                    prop.mFloatOutValue = tmp;
                }

            }
        }
    }


    private void verifyAndInitializeCustomContext(@NonNull PaintContext context) {
        if (!(context instanceof CustomContext)) {
            throw new RuntimeException(
                    "PaintContext must implement CustomContext to support native Custom "
                            + "components!");
        }

        CustomContext customCtx = (CustomContext) context;
        if (!mInitialized) {
            customCtx.createCustom(mComponentId, mConfigValue);
            for (CustomProperty prop : mProperties) {
                if (prop.mDataType == CustomProperty.FLOAT_RETURN) { //
                    customCtx.configureCustom(mComponentId, prop.mType,
                            Utils.idFromNan(prop.mFloatValue));
                } else if (prop.mDataType == TEXT_RETURN) { // TEXT_RETURN
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mIntValue);
                } else if (prop.isString()) {
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mStringOutValue);
                } else if (prop.isFloat()) {
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mFloatOutValue);
                } else {
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mIntValue);
                }
                prop.mNeedsUpdate = false;
            }
            mInitialized = true;
        }
        for (CustomProperty prop : mProperties) {
            if (prop.mNeedsUpdate) {
                if (prop.mDataType == 3) { // FLOAT_RETURN
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mFloatOutValue);
                } else if (prop.mDataType == 4) { // TEXT_RETURN
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mStringOutValue);
                } else if (prop.isString()) {
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mStringOutValue);
                } else if (prop.isFloat()) {
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mFloatOutValue);
                } else {
                    customCtx.configureCustom(mComponentId, prop.mType, prop.mIntValue);
                }
                prop.mNeedsUpdate = false;
            }
        }
    }

    @Override
    public void computeWrapSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {
        verifyAndInitializeCustomContext(context);
        if (!(context instanceof CustomContext)) {
            throw new RuntimeException("Custom components not supported");
        }
        CustomContext customCtx = (CustomContext) context;

        float[] bounds = new float[]{minWidth, maxWidth, minHeight, maxHeight};
        customCtx.measureCustom(mComponentId, bounds);
        ComponentMeasure m = measure.get(this);
        m.setW(bounds[2]); // measured width output
        m.setH(bounds[3]); // measured height output
        size.setWidth(bounds[2]);
        size.setHeight(bounds[3]);
    }


    /** Subclasses can implement this when not in wrap sizing */
    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        verifyAndInitializeCustomContext(context);
        if (!(context instanceof CustomContext)) {
            throw new RuntimeException("Custom components not supported");
        }
        CustomContext customCtx = (CustomContext) context;
        float[] bounds = new float[]{minWidth, maxWidth, minHeight, maxHeight};
        customCtx.measureCustom(mComponentId, bounds);

        ComponentMeasure m = measure.get(this);
        m.setW(bounds[2]); // measured width output
        m.setH(bounds[3]); // measured height output
    }

    @Override
    public void layout(@NonNull RemoteContext context, @NonNull MeasurePass measure) {
        super.layout(context, measure);
        ComponentMeasure m = measure.get(this);
        PaintContext paintCtx = context.getPaintContext();
        if (!(paintCtx instanceof CustomContext)) {
            throw new RuntimeException("Custom components not supported");
        }
        CustomContext customCtx = (CustomContext) paintCtx;
        float[] bounds = new float[]{m.getX(), m.getY(), m.getW(), m.getH()};
        getLocationInWindow(context, bounds);
        bounds[0] -= m.getX();
        bounds[1] -= m.getY();
        customCtx.layoutCustom(mComponentId, bounds);

    }

    @Override
    public void paintingComponent(@NonNull PaintContext context) {
        verifyAndInitializeCustomContext(context);
        if (!(context instanceof CustomContext)) {
            throw new RuntimeException("Custom components not supported");
        }
        CustomContext customCtx = (CustomContext) context;
        context.save();
        context.translate(mX, mY);
        if (mGraphicsLayerModifier != null) {
            context.startGraphicsLayer((int) getWidth(), (int) getHeight());
            mCachedAttributes.clear();
            mGraphicsLayerModifier.fillInAttributes(mCachedAttributes);
            context.setGraphicsLayer(mCachedAttributes);
        }
        mComponentModifiers.paint(context);
        float tx = mPaddingLeft;
        float ty = mPaddingTop;
        context.translate(tx, ty);

        customCtx.drawCustom(mComponentId);
        context.restore();
//        context.savePaint();
//        context.applyPaint(mPb);
//        context.drawLine(mX, mY, mX + mWidth, mY + mHeight);
//        context.restorePaint();
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mComponentId, mAnimationId, mConfigId, mProperties);
    }

    /**
     * Write the Custom Component to the buffer.
     *
     * @param buffer      WireBuffer
     * @param componentId The id of the component
     * @param animationId The id for animation
     * @param configId    the id of the config
     * @param properties  the properties
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int componentId,
            int animationId,
            int configId,
            @NonNull List<CustomProperty> properties) {
        buffer.start(Operations.LAYOUT_CUSTOM);
        buffer.writeInt(componentId);
        buffer.writeInt(animationId);
        buffer.writeInt(configId);

        // Serialize properties array
        buffer.writeInt(properties.size());
        for (CustomProperty prop : properties) {
            buffer.writeShort(prop.mType);
            buffer.writeShort(prop.mDataType);
            if ((prop.mDataType & 1) == 0) {
                buffer.writeInt(prop.mIntValue);
            } else {
                buffer.writeFloat(prop.mFloatValue);
            }
        }
    }

    private boolean isInside(@NonNull RemoteContext context,
            float x, float y) {
        if (!contains(context, x, y)) {
            return false;
        }
        if (context.getTouchVersion() == LayoutManager.FIX_TOUCH_EVENT) {
            calcPos(context, x, y);
        }
        return true;
    }

    private void calcPos(@NonNull RemoteContext context,
            float x, float y) {
        mLocation[0] = 0f;
        mLocation[1] = 0f;
        getLocationInWindow(context, mLocation, true);
        mTouchLocationX = x - mLocation[0];
        mTouchLocationY = y - mLocation[1];
    }

    @Override
    public boolean onTouchDown(@NonNull RemoteContext context, @NonNull CoreDocument document,
            float x, float y) {
        if (!isInside(context, x, y)) {
            return false;
        }
        PaintContext paintCtx = context.getPaintContext();
        if (!(paintCtx instanceof CustomContext)) {
            throw new RuntimeException("Custom components not supported");
        }
        CustomContext cCtx = (CustomContext) paintCtx;

        document.appliedTouchOperation(this);

        calcPos(context, x, y);

        return cCtx.touchCustom(mComponentId, CustomContext.TOUCH_DOWN, mTouchLocationX,
                mTouchLocationY);
    }

    @Override
    public boolean onTouchDrag(@NonNull RemoteContext context, @NonNull CoreDocument document,
            float x, float y, boolean force) {
        PaintContext paintCtx = context.getPaintContext();
        if (!(paintCtx instanceof CustomContext)) {
            throw new RuntimeException("Custom components not supported");
        }

        CustomContext cCtx = (CustomContext) paintCtx;
        calcPos(context, x, y);

        return cCtx.touchCustom(mComponentId, CustomContext.TOUCH_DRAG, mTouchLocationX,
                mTouchLocationY);
    }

    @Override
    public boolean onTouchUp(@NonNull RemoteContext context, @NonNull CoreDocument document,
            float x, float y, float dx, float dy, boolean force) {
        PaintContext paintCtx = context.getPaintContext();
        if (!(paintCtx instanceof CustomContext)) {
            throw new RuntimeException("Custom components not supported");
        }
        CustomContext cCtx = (CustomContext) paintCtx;
        return cCtx.touchCustom(mComponentId, CustomContext.TOUCH_UP, mTouchLocationX,
                mTouchLocationY);
    }

    /**
     * Read the Custom Component from the buffer.
     *
     * @param buffer     WireBuffer
     * @param operations List of operations to add to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int componentId = buffer.readInt();
        int animationId = buffer.readInt();
        int configId = buffer.readId();

        int propCount = buffer.readInt();
        ArrayList<CustomProperty> properties = new ArrayList<>();
        for (int i = 0; i < propCount; i++) {
            short pType = (short) buffer.readShort();
            short pDataType = (short) buffer.readShort();

            if ((pDataType & 1) == 0) {
                int value = buffer.readInt();
                properties.add(new CustomProperty(pType, pDataType, value));
            } else {
                float value = buffer.readFloat();
                properties.add(new CustomProperty(pType, pDataType, value));
            }
        }

        operations.add(new Custom(null, componentId, animationId, configId, null, properties));
    }
}
