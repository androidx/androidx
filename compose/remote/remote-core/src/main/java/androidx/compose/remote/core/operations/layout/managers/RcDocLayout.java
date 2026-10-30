/*
 * Copyright (C) 2026 The Android Open Source Project
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

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RcResolver;
import androidx.compose.remote.core.RemoteClock;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.RemoteComposeState;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.TouchListener;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.BinaryData;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.MapRcDocVariables;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.layout.CanvasOperations;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.TouchOperation;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.types.LongConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Layout component that loads, measures, paints, and forwards interactions to an embedded {@link
 * CoreDocument} whose bytes are resolved by the host via {@link RcResolver}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcDocLayout extends LayoutManager implements VariableSupport, TouchOperation {
    private static final int OP_CODE = Operations.LAYOUT_RC_DOC;

    private final int mUrlId;
    private @Nullable String mCurrentUrl;
    private @Nullable RcResolver mCurrentResolver;
    private @Nullable CoreDocument mChildDoc;
    private @Nullable ChildRemoteContext mChildContext;
    private int mIntrinsicWidth = 0;
    private int mIntrinsicHeight = 0;
    private final ArrayList<MapRcDocVariables> mMappings = new ArrayList<>();
    private final float[] mClickLoc = new float[2];

    private final CoreDocument.IdActionCallback mChildActionListener =
            (id, metadata) -> handleChildAction(id, metadata != null ? metadata : "");

    /**
     * Create an {@link RcDocLayout} component.
     *
     * @param parent the parent component
     * @param componentId the component ID
     * @param animationId the animation ID (-1 if not set)
     * @param x the initial X offset
     * @param y the initial Y offset
     * @param width the initial width
     * @param height the initial height
     * @param urlId the text ID of the URL or identifier string for the embedded document
     */
    public RcDocLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height,
            int urlId) {
        super(parent, componentId, animationId, x, y, width, height);
        mUrlId = urlId;
    }

    /**
     * Create an {@link RcDocLayout} component.
     *
     * @param parent the parent component
     * @param componentId the component ID
     * @param animationId the animation ID (-1 if not set)
     * @param urlId the text ID of the URL or identifier string for the embedded document
     */
    public RcDocLayout(@Nullable Component parent, int componentId, int animationId, int urlId) {
        this(parent, componentId, animationId, 0, 0, 0, 0, urlId);
    }

    /** Return the text ID of the embedded document URL. */
    public int getUrlId() {
        return mUrlId;
    }

    /** Return the currently loaded embedded {@link CoreDocument}, or {@code null}. */
    public @Nullable CoreDocument getChildDocument() {
        return mChildDoc;
    }

    /**
     * Register a {@link MapRcDocVariables} mapping with this component.
     *
     * @param mapping the variable or hostAction mapping to register
     */
    public void addVariableMapping(@NonNull MapRcDocVariables mapping) {
        if (!mMappings.contains(mapping)) {
            mMappings.add(mapping);
        }
    }

    @NonNull
    @Override
    protected String getSerializedName() {
        return "RC_DOC";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                getSerializedName()
                        + " ["
                        + mComponentId
                        + ":"
                        + mAnimationId
                        + "] ("
                        + mX
                        + ", "
                        + mY
                        + " - "
                        + mWidth
                        + " x "
                        + mHeight
                        + ") urlId="
                        + mUrlId);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer.add("urlId", mUrlId);
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (mUrlId > 0) {
            context.listensTo(mUrlId, this);
        }
        for (int i = 0; i < mMappings.size(); i++) {
            MapRcDocVariables mapping = mMappings.get(i);
            if (mapping.getMode() == MapRcDocVariables.MAP_FORWARD
                    || mapping.getMode() == MapRcDocVariables.MAP_BIDIRECTIONAL) {
                context.listensTo(mapping.getLocalId(), this);
            }
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        super.updateVariables(context);
        ensureDocumentLoaded(context);
        if (mChildDoc != null && mChildContext != null) {
            syncForwardVariables(context);
            syncReverseVariables(context);
        }
    }

    /**
     * Validate any shaders in the embedded child document using the provided {@link
     * CoreDocument.ShaderControl}.
     *
     * @param ctl the shader validation callback
     */
    public void checkShaders(CoreDocument.@NonNull ShaderControl ctl) {
        if (mChildDoc != null && mChildContext != null) {
            mChildDoc.checkShaders(mChildContext, ctl);
        }
    }

    /** URL prefix that refers to a {@link BinaryData} blob of the host document by id. */
    public static final String EMBEDDED_URL_PREFIX = "rc:embedded/";

    /**
     * Returns the {@link BinaryData#TYPE_RC_DOC} blob the url id refers to, directly (the id is the
     * blob's id) or through a {@code rc:embedded/<id>} url string, or null if the url is a regular
     * url for the host's {@link RcResolver}.
     */
    private static @Nullable BinaryData findEmbeddedDoc(
            @NonNull RemoteContext hostContext, int urlId, @Nullable String url) {
        Object direct = hostContext.getObject(urlId);
        if (direct instanceof BinaryData
                && ((BinaryData) direct).getType() == BinaryData.TYPE_RC_DOC) {
            return (BinaryData) direct;
        }
        if (url != null && url.startsWith(EMBEDDED_URL_PREFIX)) {
            try {
                int id = Integer.parseInt(url.substring(EMBEDDED_URL_PREFIX.length()).trim());
                Object o = hostContext.getObject(id);
                if (o instanceof BinaryData
                        && ((BinaryData) o).getType() == BinaryData.TYPE_RC_DOC) {
                    return (BinaryData) o;
                }
            } catch (NumberFormatException ignored) {
                // not an embedded url
            }
        }
        return null;
    }

    private void ensureDocumentLoaded(@NonNull RemoteContext hostContext) {
        String url = hostContext.getText(mUrlId);
        BinaryData embedded = findEmbeddedDoc(hostContext, mUrlId, url);
        if (embedded != null) {
            url = EMBEDDED_URL_PREFIX + embedded.getId();
        }
        if (url == null) {
            return;
        }
        RcResolver resolver = hostContext.getRcResolver();
        if (url.equals(mCurrentUrl) && (embedded != null || resolver == mCurrentResolver)) {
            return;
        }
        mCurrentUrl = url;
        mCurrentResolver = resolver;
        byte[] bytes;
        if (embedded != null) {
            bytes = embedded.getData();
        } else if (resolver == null) {
            mChildDoc = null;
            mChildContext = null;
            invalidateMeasure();
            return;
        } else {
            try {
                bytes = resolver.resolve(url);
            } catch (Exception ignored) {
                bytes = null;
            }
        }
        if (bytes == null || bytes.length == 0) {
            mChildDoc = null;
            mChildContext = null;
            invalidateMeasure();
            return;
        }
        CoreDocument loaded;
        try {
            RemoteComposeBuffer buffer =
                    RemoteComposeBuffer.fromInputStream(new ByteArrayInputStream(bytes));
            loaded = new CoreDocument(hostContext.getClock());
            loaded.initFromBuffer(buffer);
        } catch (Exception ignored) {
            mChildDoc = null;
            mChildContext = null;
            invalidateMeasure();
            return;
        }
        mChildDoc = loaded;
        int initialW = loaded.getWidth();
        int initialH = loaded.getHeight();
        mChildContext = new ChildRemoteContext(hostContext);
        mChildContext.setDocLoadTime();
        mChildContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, -Float.MAX_VALUE);
        mChildDoc.initializeContext(mChildContext);
        if (hostContext.getDocument() != null
                && hostContext.getDocument().getShaderControl() != null) {
            mChildDoc.checkShaders(mChildContext, hostContext.getDocument().getShaderControl());
        }
        mIntrinsicWidth = mChildDoc.getWidth() > 0 ? mChildDoc.getWidth() : initialW;
        mIntrinsicHeight = mChildDoc.getHeight() > 0 ? mChildDoc.getHeight() : initialH;
        if (!mChildDoc.getIdActionListeners().contains(mChildActionListener)) {
            mChildDoc.addIdActionListener(mChildActionListener);
        }
        for (int i = 0; i < mMappings.size(); i++) {
            mMappings.get(i).resetSyncState();
        }
        syncForwardVariables(hostContext);
        syncReverseVariables(hostContext);
        invalidateMeasure();
    }

    private void syncForwardVariables(@NonNull RemoteContext hostContext) {
        if (mChildDoc == null || mChildContext == null) {
            return;
        }
        boolean anyChanged = false;
        for (int i = 0; i < mMappings.size(); i++) {
            MapRcDocVariables mapping = mMappings.get(i);
            int mode = mapping.getMode();
            if (mode != MapRcDocVariables.MAP_FORWARD
                    && mode != MapRcDocVariables.MAP_BIDIRECTIONAL) {
                continue;
            }
            String remoteName = hostContext.getText(mapping.getRemoteTarget());
            if (remoteName == null) {
                continue;
            }
            ArrayList<VarEntry> entries = mChildContext.getVarEntries(remoteName);
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            int localId = mapping.getLocalId();
            for (int j = 0; j < entries.size(); j++) {
                VarEntry entry = entries.get(j);
                switch (entry.mType) {
                    case NamedVariable.FLOAT_TYPE: {
                        float hostVal = hostContext.getFloat(localId);
                        if (mode == MapRcDocVariables.MAP_FORWARD
                                || !mapping.isInitialSynced()
                                || Float.compare(hostVal, mapping.getLastSyncedFloat()) != 0) {
                            // Override (not load): the child's own writer of this id, e.g. the
                            // TouchExpression behind a slider, adopts an overridden value and
                            // then resumes owning it; a plain load would be overwritten.
                            mChildContext.overrideFloat(entry.mId, hostVal);
                            mapping.setLastSyncedFloat(hostVal);
                            mapping.setInitialSynced(true);
                            anyChanged = true;
                        }
                        break;
                    }
                    case NamedVariable.INT_TYPE: {
                        int hostVal = hostContext.getInteger(localId);
                        if (mode == MapRcDocVariables.MAP_FORWARD
                                || !mapping.isInitialSynced()
                                || hostVal != mapping.getLastSyncedInt()) {
                            mChildContext.mRemoteComposeState.clearIntegerOverride(entry.mId);
                            mChildContext.loadInteger(entry.mId, hostVal);
                            mapping.setLastSyncedInt(hostVal);
                            mapping.setInitialSynced(true);
                            anyChanged = true;
                        }
                        break;
                    }
                    case NamedVariable.COLOR_TYPE: {
                        int hostVal = hostContext.getColor(localId);
                        if (mode == MapRcDocVariables.MAP_FORWARD
                                || !mapping.isInitialSynced()
                                || hostVal != mapping.getLastSyncedColor()) {
                            mChildContext.loadColor(entry.mId, hostVal);
                            mapping.setLastSyncedColor(hostVal);
                            mapping.setInitialSynced(true);
                            anyChanged = true;
                        }
                        break;
                    }
                    case NamedVariable.STRING_TYPE: {
                        String hostVal = hostContext.getText(localId);
                        if (hostVal != null
                                && (mode == MapRcDocVariables.MAP_FORWARD
                                        || !mapping.isInitialSynced()
                                        || !hostVal.equals(mapping.getLastSyncedText()))) {
                            mChildContext.mRemoteComposeState.clearDataOverride(entry.mId);
                            mChildContext.loadText(entry.mId, hostVal);
                            mapping.setLastSyncedText(hostVal);
                            mapping.setInitialSynced(true);
                            anyChanged = true;
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        if (anyChanged) {
            mChildContext.updateOps();
        }
    }

    private void syncReverseVariables(@NonNull RemoteContext hostContext) {
        if (mChildDoc == null || mChildContext == null) {
            return;
        }
        boolean anyHostChanged = false;
        for (int i = 0; i < mMappings.size(); i++) {
            MapRcDocVariables mapping = mMappings.get(i);
            int mode = mapping.getMode();
            if (mode != MapRcDocVariables.MAP_REVERSE
                    && mode != MapRcDocVariables.MAP_BIDIRECTIONAL) {
                continue;
            }
            String remoteName = hostContext.getText(mapping.getRemoteTarget());
            if (remoteName == null) {
                continue;
            }
            ArrayList<VarEntry> entries = mChildContext.getVarEntries(remoteName);
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            int localId = mapping.getLocalId();
            VarEntry entry = entries.get(0);
            switch (entry.mType) {
                case NamedVariable.FLOAT_TYPE: {
                    float childVal = mChildContext.getFloat(entry.mId);
                    if (mode == MapRcDocVariables.MAP_REVERSE
                            || Float.compare(childVal, mapping.getLastSyncedFloat()) != 0) {
                        if (Float.compare(hostContext.getFloat(localId), childVal) != 0) {
                            hostContext.overrideFloat(localId, childVal);
                            anyHostChanged = true;
                        }
                        mapping.setLastSyncedFloat(childVal);
                        mapping.setInitialSynced(true);
                    }
                    break;
                }
                case NamedVariable.INT_TYPE: {
                    int childVal = mChildContext.getInteger(entry.mId);
                    if (mode == MapRcDocVariables.MAP_REVERSE
                            || childVal != mapping.getLastSyncedInt()) {
                        if (hostContext.getInteger(localId) != childVal) {
                            hostContext.overrideInteger(localId, childVal);
                            anyHostChanged = true;
                        }
                        mapping.setLastSyncedInt(childVal);
                        mapping.setInitialSynced(true);
                    }
                    break;
                }
                case NamedVariable.COLOR_TYPE: {
                    int childVal = mChildContext.getColor(entry.mId);
                    if (mode == MapRcDocVariables.MAP_REVERSE
                            || childVal != mapping.getLastSyncedColor()) {
                        if (hostContext.getColor(localId) != childVal) {
                            hostContext.loadColor(localId, childVal);
                            anyHostChanged = true;
                        }
                        mapping.setLastSyncedColor(childVal);
                        mapping.setInitialSynced(true);
                    }
                    break;
                }
                case NamedVariable.STRING_TYPE: {
                    String childVal = mChildContext.getText(entry.mId);
                    if (childVal != null
                            && (mode == MapRcDocVariables.MAP_REVERSE
                                    || !childVal.equals(mapping.getLastSyncedText()))) {
                        if (!childVal.equals(hostContext.getText(localId))) {
                            hostContext.loadText(localId, childVal);
                            anyHostChanged = true;
                        }
                        mapping.setLastSyncedText(childVal);
                        mapping.setInitialSynced(true);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        if (anyHostChanged) {
            hostContext.updateOps();
            hostContext.needsRepaint();
        }
    }

    private void handleChildAction(int actionId, @NonNull String metadata) {
        if (mChildContext == null) {
            return;
        }
        RemoteContext hostContext = mChildContext.mHostContext;
        boolean mapped = false;
        for (int i = 0; i < mMappings.size(); i++) {
            MapRcDocVariables mapping = mMappings.get(i);
            if (mapping.getMode() == MapRcDocVariables.MAP_HOST_ACTION
                    && mapping.getRemoteTarget() == actionId) {
                hostContext.loadText(mapping.getLocalId(), metadata);
                mapped = true;
            }
        }
        if (mapped) {
            hostContext.updateOps();
            hostContext.needsRepaint();
            needsRepaint();
        } else {
            hostContext.runAction(actionId, metadata);
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
        RemoteContext hostContext = context.getContext();
        ensureDocumentLoaded(hostContext);
        if (mChildDoc != null && mChildContext != null) {
            syncForwardVariables(hostContext);
            float w = mIntrinsicWidth;
            float h = mIntrinsicHeight;
            if ((w <= 0 || h <= 0) && mChildDoc.getRootLayoutComponent() != null) {
                context.setContext(mChildContext);
                try {
                    mChildDoc.measure(mChildContext, minWidth, maxWidth, minHeight, maxHeight);
                    w = mChildDoc.getRootLayoutComponent().getWidth();
                    h = mChildDoc.getRootLayoutComponent().getHeight();
                } finally {
                    context.setContext(hostContext);
                }
            }
            syncReverseVariables(hostContext);
            size.setWidth(Math.max(minWidth, Math.min(w, maxWidth)));
            size.setHeight(Math.max(minHeight, Math.min(h, maxHeight)));
        } else {
            size.setWidth(minWidth);
            size.setHeight(minHeight);
        }
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        RemoteContext hostContext = context.getContext();
        float modifiersWidth = computeModifierDefinedWidth(hostContext);
        float modifiersHeight = computeModifierDefinedHeight(hostContext);
        ComponentMeasure cm = measure.get(this);
        cm.setW(modifiersWidth);
        cm.setH(modifiersHeight);
        ensureDocumentLoaded(hostContext);
        if (mChildDoc != null && mChildContext != null) {
            float w = cm.getW() - getPaddingLeft() - getPaddingRight();
            float h = cm.getH() - getPaddingTop() - getPaddingBottom();
            // Modifiers that don't pin a dimension (fillMaxWidth, weights...) report
            // Float.MAX_VALUE here and the parent assigns the real size later. Measuring the child
            // document at an unbounded size would lay out its components (and their background /
            // border decorators) at Float.MAX_VALUE, and the bounds animation back to the real
            // size never produces a drawable rectangle. Only measure when both bounds are finite;
            // otherwise the child is laid out at paint time with its final content size.
            if (w >= Float.MAX_VALUE / 2f) {
                w = maxWidth - getPaddingLeft() - getPaddingRight();
            }
            if (h >= Float.MAX_VALUE / 2f) {
                h = maxHeight - getPaddingTop() - getPaddingBottom();
            }
            if (w <= 0f || h <= 0f || w >= Float.MAX_VALUE / 2f || h >= Float.MAX_VALUE / 2f) {
                return;
            }
            syncForwardVariables(hostContext);
            context.setContext(mChildContext);
            try {
                mChildContext.mWidth = w;
                mChildContext.mHeight = h;
                mChildDoc.measure(mChildContext, w, w, h, h);
            } finally {
                context.setContext(hostContext);
            }
            syncReverseVariables(hostContext);
        }
    }

    private float computeChildScale(float contentW, float contentH) {
        if (mChildDoc != null
                && mChildDoc.getContentSizing() == RootContentBehavior.SIZING_SCALE) {
            return 1f;
        }
        if (mIntrinsicWidth > 0
                && mIntrinsicHeight > 0
                && contentW > 0f
                && contentH > 0f
                && (contentW != mIntrinsicWidth || contentH != mIntrinsicHeight)) {
            return Math.min(contentW / mIntrinsicWidth, contentH / mIntrinsicHeight);
        }
        return 1f;
    }

    @Override
    public void paintingComponent(@NonNull PaintContext context) {
        if (mPreTranslate != null) {
            mPreTranslate.paint(context);
        }
        RemoteContext hostContext = context.getContext();
        Component prevLastComponent = hostContext.mLastComponent;
        hostContext.mLastComponent = this;

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

        mComponentModifiers.paint(context);

        float tx = mPaddingLeft + getScrollX();
        float ty = mPaddingTop + getScrollY();
        context.translate(tx, ty);

        ensureDocumentLoaded(hostContext);
        if (mChildDoc != null && mChildContext != null) {
            float contentW = mWidth - mPaddingLeft - mPaddingRight;
            float contentH = mHeight - mPaddingTop - mPaddingBottom;
            float scale = computeChildScale(contentW, contentH);
            mChildContext.currentTime = hostContext.currentTime;
            mChildContext.setAnimationTime(hostContext.getAnimationTime());
            mChildContext.setAnimationEnabled(hostContext.isAnimationEnabled());
            float animTime = hostContext.getFloat(RemoteContext.ID_ANIMATION_TIME);
            if (!Float.isNaN(animTime)) {
                mChildContext.loadFloat(RemoteContext.ID_ANIMATION_TIME, animTime);
            }
            float deltaTime = hostContext.getFloat(RemoteContext.ID_ANIMATION_DELTA_TIME);
            if (!Float.isNaN(deltaTime)) {
                mChildContext.loadFloat(RemoteContext.ID_ANIMATION_DELTA_TIME, deltaTime);
            }

            syncForwardVariables(hostContext);

            boolean prevNeedsRepaint = context.doesNeedsRepaint();
            int prevMeasureVersion = context.getMeasureVersion();
            context.save();
            if (contentW > 0f && contentH > 0f) {
                context.clipRect(0f, 0f, contentW, contentH);
            }
            if (scale != 1f) {
                context.scale(scale, scale);
                mChildContext.mWidth = contentW / scale;
                mChildContext.mHeight = contentH / scale;
                mChildContext.mViewportWidth = contentW / scale;
                mChildContext.mViewportHeight = contentH / scale;
            } else if (mChildDoc.getContentScroll() == RootContentBehavior.SCROLL_VERTICAL
                    && mIntrinsicWidth > 0
                    && mIntrinsicHeight > 0) {
                float scaledDocH = mIntrinsicHeight * (contentW / mIntrinsicWidth);
                mChildContext.mWidth = contentW;
                mChildContext.mHeight = Math.max(contentH, scaledDocH);
                mChildContext.mViewportWidth = contentW;
                mChildContext.mViewportHeight = contentH;
            } else {
                mChildContext.mWidth = contentW;
                mChildContext.mHeight = contentH;
                mChildContext.mViewportWidth = contentW;
                mChildContext.mViewportHeight = contentH;
            }
            context.setContext(mChildContext);
            try {
                mChildDoc.paint(mChildContext, hostContext.getTheme());
            } finally {
                context.setContext(hostContext);
                context.restore();
                context.setMeasureVersion(prevMeasureVersion);
                if (prevNeedsRepaint || mChildDoc.needsRepaint() > 0) {
                    context.needsRepaint();
                }
            }

            syncReverseVariables(hostContext);
        }

        CanvasOperations canvasOperations = getCanvasOperations();
        if (canvasOperations != null) {
            canvasOperations.paint(context);
        }

        context.translate(-tx, -ty);

        if (mGraphicsLayerModifier != null) {
            context.endGraphicsLayer();
        }

        context.restore();
        hostContext.mLastComponent = prevLastComponent;
    }

    private void toChildCoordinates(@NonNull RemoteContext context, float x, float y) {
        mClickLoc[0] = 0f;
        mClickLoc[1] = 0f;
        getLocationInWindow(context, mClickLoc, true);
        mClickLoc[0] = x - mClickLoc[0] - mPaddingLeft - getScrollX();
        mClickLoc[1] = y - mClickLoc[1] - mPaddingTop - getScrollY();
        float contentW = mWidth - mPaddingLeft - mPaddingRight;
        float contentH = mHeight - mPaddingTop - mPaddingBottom;
        float scale = computeChildScale(contentW, contentH);
        if (scale != 1f && scale > 0f) {
            mClickLoc[0] /= scale;
            mClickLoc[1] /= scale;
        }
    }

    @Override
    public boolean onClick(
            @NonNull RemoteContext context, @NonNull CoreDocument document, float x, float y) {
        boolean isUnconditional = x == -1 && y == -1;
        if (!isUnconditional && !contains(context, x, y)) {
            return false;
        }
        boolean handled = super.onClick(context, document, x, y);
        if (mChildDoc != null && mChildContext != null) {
            float lx = -1;
            float ly = -1;
            if (!isUnconditional) {
                toChildCoordinates(context, x, y);
                lx = mClickLoc[0];
                ly = mClickLoc[1];
            }
            syncForwardVariables(context);
            if (mChildDoc.onClick(mChildContext, lx, ly)) {
                handled = true;
            }
            mChildContext.updateOps();
            syncReverseVariables(context);
        }
        return handled;
    }

    @Override
    public boolean onLongPress(
            @NonNull RemoteContext context, @NonNull CoreDocument document, float x, float y) {
        boolean isUnconditional = x == -1 && y == -1;
        if (!isUnconditional && !contains(context, x, y)) {
            return false;
        }
        boolean handled = super.onLongPress(context, document, x, y);
        if (mChildDoc != null && mChildContext != null) {
            float lx = -1;
            float ly = -1;
            if (!isUnconditional) {
                toChildCoordinates(context, x, y);
                lx = mClickLoc[0];
                ly = mClickLoc[1];
            }
            syncForwardVariables(context);
            if (mChildDoc.onLongPress(mChildContext, lx, ly)) {
                handled = true;
            }
            mChildContext.updateOps();
            syncReverseVariables(context);
        }
        return handled;
    }

    @Override
    public boolean onDoubleClick(
            @NonNull RemoteContext context, @NonNull CoreDocument document, float x, float y) {
        boolean isUnconditional = x == -1 && y == -1;
        if (!isUnconditional && !contains(context, x, y)) {
            return false;
        }
        boolean handled = super.onDoubleClick(context, document, x, y);
        if (mChildDoc != null && mChildContext != null) {
            float lx = -1;
            float ly = -1;
            if (!isUnconditional) {
                toChildCoordinates(context, x, y);
                lx = mClickLoc[0];
                ly = mClickLoc[1];
            }
            syncForwardVariables(context);
            if (mChildDoc.onDoubleClick(mChildContext, lx, ly)) {
                handled = true;
            }
            mChildContext.updateOps();
            syncReverseVariables(context);
        }
        return handled;
    }

    @Override
    public boolean onTouchDown(
            @NonNull RemoteContext context, @NonNull CoreDocument document, float x, float y) {
        if (!contains(context, x, y)) {
            return false;
        }
        boolean handled = super.onTouchDown(context, document, x, y);
        if (mChildDoc != null && mChildContext != null) {
            toChildCoordinates(context, x, y);
            mChildContext.setAnimationTime(context.getAnimationTime());
            mChildContext.loadFloat(
                    RemoteContext.ID_TOUCH_EVENT_TIME, mChildContext.getAnimationTime());
            syncForwardVariables(context);
            if (mChildDoc.touchDown(mChildContext, mClickLoc[0], mClickLoc[1])) {
                document.appliedTouchOperation(this);
                handled = true;
            }
            mChildContext.updateOps();
            syncReverseVariables(context);
        }
        return handled;
    }

    @Override
    public boolean onTouchDrag(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            float x,
            float y,
            boolean force) {
        if (!force && !contains(context, x, y)) {
            return false;
        }
        boolean handled = super.onTouchDrag(context, document, x, y, force);
        if (mChildDoc != null && mChildContext != null) {
            toChildCoordinates(context, x, y);
            mChildContext.setAnimationTime(context.getAnimationTime());
            mChildContext.loadFloat(
                    RemoteContext.ID_TOUCH_EVENT_TIME, mChildContext.getAnimationTime());
            syncForwardVariables(context);
            if (mChildDoc.touchDrag(mChildContext, mClickLoc[0], mClickLoc[1])) {
                handled = true;
            }
            mChildContext.updateOps();
            syncReverseVariables(context);
        }
        return handled;
    }

    @Override
    public boolean onTouchUp(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            float x,
            float y,
            float dx,
            float dy,
            boolean force) {
        if (!force && !contains(context, x, y)) {
            return false;
        }
        boolean handled = super.onTouchUp(context, document, x, y, dx, dy, force);
        if (mChildDoc != null && mChildContext != null) {
            toChildCoordinates(context, x, y);
            float contentW = mWidth - mPaddingLeft - mPaddingRight;
            float contentH = mHeight - mPaddingTop - mPaddingBottom;
            float scale = computeChildScale(contentW, contentH);
            float sdx = (scale != 1f && scale > 0f) ? dx / scale : dx;
            float sdy = (scale != 1f && scale > 0f) ? dy / scale : dy;
            mChildContext.setAnimationTime(context.getAnimationTime());
            mChildContext.loadFloat(
                    RemoteContext.ID_TOUCH_EVENT_TIME, mChildContext.getAnimationTime());
            syncForwardVariables(context);
            if (mChildDoc.touchUp(mChildContext, mClickLoc[0], mClickLoc[1], sdx, sdy)) {
                handled = true;
            }
            mChildContext.updateOps();
            syncReverseVariables(context);
        }
        return handled;
    }

    @Override
    public boolean onTouchCancel(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            float x,
            float y,
            boolean force) {
        if (!force && !contains(context, x, y)) {
            return false;
        }
        boolean handled = super.onTouchCancel(context, document, x, y, force);
        if (mChildDoc != null && mChildContext != null) {
            toChildCoordinates(context, x, y);
            syncForwardVariables(context);
            if (mChildDoc.touchCancel(mChildContext, mClickLoc[0], mClickLoc[1], 0f, 0f)) {
                handled = true;
            }
            mChildContext.updateOps();
            syncReverseVariables(context);
        }
        return handled;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mComponentId, mAnimationId, mUrlId);
    }

    /**
     * Write the {@link RcDocLayout} operation to the buffer.
     *
     * @param buffer the WireBuffer to write to
     * @param componentId the component ID
     * @param animationId the animation ID (-1 if not set)
     * @param urlId the text ID of the URL or identifier string
     */
    public static void apply(
            @NonNull WireBuffer buffer, int componentId, int animationId, int urlId) {
        buffer.start(OP_CODE);
        buffer.writeInt(componentId);
        buffer.writeInt(animationId);
        buffer.writeInt(urlId);
    }

    /**
     * Read this operation from the buffer and add it to the list of operations.
     *
     * @param buffer the buffer to read
     * @param operations the list of operations to add to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int componentId = buffer.declareId();
        int animationId = buffer.declareId();
        int urlId = buffer.readId();
        operations.add(new RcDocLayout(null, componentId, animationId, urlId));
    }

    /**
     * Populate the documentation with a description of this operation.
     *
     * @param doc to append the description to
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", OP_CODE, "RcDocLayout")
                .description("Embedded RemoteCompose document layout component")
                .field(INT, "COMPONENT_ID", "Unique ID for this component")
                .field(INT, "ANIMATION_ID", "ID used to match components for animation purposes")
                .field(INT, "URL_ID", "Text ID of the document URL or identifier");
    }

    private static final class VarEntry {
        final int mId;
        final int mType;

        VarEntry(int id, int type) {
            mId = id;
            mType = type;
        }
    }

    private static final class ChildRemoteContext extends RemoteContext {
        final @NonNull RemoteContext mHostContext;
        private final HashMap<String, ArrayList<VarEntry>> mVarNameMap = new HashMap<>();

        ChildRemoteContext(@NonNull RemoteContext hostContext) {
            super(hostContext.getClock());
            mHostContext = hostContext;
            setDensity(hostContext.getDensity());
        }

        @Nullable ArrayList<VarEntry> getVarEntries(@NonNull String name) {
            return mVarNameMap.get(name);
        }

        @Override
        public void clearVariables() {
            mVarNameMap.clear();
        }

        @Override
        public @Nullable PaintContext getPaintContext() {
            return mHostContext.getPaintContext();
        }

        @Override
        public @NonNull RemoteClock getClock() {
            return mHostContext != null ? mHostContext.getClock() : super.getClock();
        }

        @Override
        public float getDensity() {
            return mHostContext.getDensity();
        }

        @Override
        public @Nullable RcResolver getRcResolver() {
            return mHostContext.getRcResolver();
        }

        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {
            mRemoteComposeState.putPathData(instanceId, floatPath);
            mRemoteComposeState.putPathWinding(instanceId, winding);
        }

        @Override
        public float @Nullable [] getPathData(int instanceId) {
            return mRemoteComposeState.getPathData(instanceId);
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {
            ArrayList<VarEntry> list = mVarNameMap.get(varName);
            if (list == null) {
                list = new ArrayList<>();
                mVarNameMap.put(varName, list);
            }
            for (int i = 0; i < list.size(); i++) {
                VarEntry existing = list.get(i);
                if (existing.mId == varId && existing.mType == varType) {
                    return;
                }
            }
            list.add(new VarEntry(varId, varType));
        }

        @Override
        public void loadColor(int id, int color) {
            mRemoteComposeState.updateColor(id, color);
        }


        @Override
        public int getColor(int id) {
            return mRemoteComposeState.getColor(id);
        }

        @Override
        public void setNamedColorOverride(@NonNull String colorName, int color) {
            ArrayList<VarEntry> list = mVarNameMap.get(colorName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    loadColor(list.get(i).mId, color);
                }
            }
        }

        @Override
        public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {
            ArrayList<VarEntry> list = mVarNameMap.get(stringName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    loadText(list.get(i).mId, value);
                }
            }
        }

        @Override
        public void clearNamedStringOverride(@NonNull String stringName) {
            ArrayList<VarEntry> list = mVarNameMap.get(stringName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    mRemoteComposeState.clearDataOverride(list.get(i).mId);
                }
            }
        }

        @Override
        public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {
            ArrayList<VarEntry> list = mVarNameMap.get(booleanName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    overrideInteger(list.get(i).mId, value ? 1 : 0);
                }
            }
        }

        @Override
        public void clearNamedBooleanOverride(@NonNull String booleanName) {
            ArrayList<VarEntry> list = mVarNameMap.get(booleanName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    mRemoteComposeState.clearIntegerOverride(list.get(i).mId);
                }
            }
        }

        @Override
        public void setNamedIntegerOverride(@NonNull String integerName, int value) {
            ArrayList<VarEntry> list = mVarNameMap.get(integerName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    overrideInteger(list.get(i).mId, value);
                }
            }
        }

        @Override
        public void clearNamedIntegerOverride(@NonNull String integerName) {
            ArrayList<VarEntry> list = mVarNameMap.get(integerName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    mRemoteComposeState.clearIntegerOverride(list.get(i).mId);
                }
            }
        }

        @Override
        public void setNamedFloatOverride(@NonNull String floatName, float value) {
            ArrayList<VarEntry> list = mVarNameMap.get(floatName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    overrideFloat(list.get(i).mId, value);
                }
            }
        }

        @Override
        public void clearNamedFloatOverride(@NonNull String floatName) {
            ArrayList<VarEntry> list = mVarNameMap.get(floatName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    mRemoteComposeState.clearFloatOverride(list.get(i).mId);
                }
            }
        }

        @Override
        public void setNamedLong(@NonNull String name, long value) {
            ArrayList<VarEntry> list = mVarNameMap.get(name);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Object obj = mRemoteComposeState.getObject(list.get(i).mId);
                    if (obj instanceof LongConstant) {
                        ((LongConstant) obj).setValue(value);
                    }
                }
            }
        }

        @Override
        public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {
            ArrayList<VarEntry> list = mVarNameMap.get(dataName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    mRemoteComposeState.overrideData(list.get(i).mId, value);
                }
            }
        }

        @Override
        public void clearNamedDataOverride(@NonNull String dataName) {
            ArrayList<VarEntry> list = mVarNameMap.get(dataName);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    mRemoteComposeState.clearDataOverride(list.get(i).mId);
                }
            }
        }

        @Override
        public void addCollection(int id, @NonNull ArrayAccess collection) {
            mRemoteComposeState.addCollection(id, collection);
        }

        @Override
        public void putDataMap(int id, @NonNull DataMap map) {
            mRemoteComposeState.putDataMap(id, map);
        }

        @Override
        public @Nullable DataMap getDataMap(int id) {
            return mRemoteComposeState.getDataMap(id);
        }

        @Override
        public void runAction(int id, @NonNull String metadata) {
            mDocument.performClick(this, id, metadata);
        }

        @Override
        public void runNamedAction(int id, @Nullable Object value) {
            String text = getText(id);
            if (text != null) {
                mDocument.runNamedAction(text, value);
            }
        }

        @Override
        public void putObject(int id, @NonNull Object value) {
            mRemoteComposeState.updateObject(id, value);
        }

        @Override
        public @Nullable Object getObject(int id) {
            return mRemoteComposeState.getObject(id);
        }

        @Override
        public void hapticEffect(int type) {
            mHostContext.hapticEffect(type);
        }

        @Override
        public void loadSound(int soundId, byte @NonNull [] data) {
            mHostContext.loadSound(soundId, data);
        }

        @Override
        public void playSound(int soundId) {
            mHostContext.playSound(soundId);
        }

        @Override
        public void loadBitmap(
                int imageId,
                short encoding,
                short type,
                int width,
                int height,
                byte @NonNull [] bitmap) {
            RemoteComposeState saved = mHostContext.mRemoteComposeState;
            mHostContext.mRemoteComposeState = mRemoteComposeState;
            try {
                mHostContext.loadBitmap(imageId, encoding, type, width, height, bitmap);
            } finally {
                mHostContext.mRemoteComposeState = saved;
            }
        }

        @Override
        public void loadText(int id, @NonNull String text) {
            if (!mRemoteComposeState.containsId(id)) {
                mRemoteComposeState.cacheData(id, text);
            } else {
                mRemoteComposeState.updateData(id, text);
            }
        }

        @Override
        public @Nullable String getText(int id) {
            Object v = mRemoteComposeState.getFromId(id);
            return v instanceof String ? (String) v : null;
        }

        @Override
        public void loadFloat(int id, float value) {
            mRemoteComposeState.updateFloat(id, value);
        }

        @Override
        public void overrideFloat(int id, float value) {
            mRemoteComposeState.overrideFloat(id, value);
        }

        @Override
        public void loadInteger(int id, int value) {
            mRemoteComposeState.updateInteger(id, value);
        }

        @Override
        public void overrideInteger(int id, int value) {
            mRemoteComposeState.overrideInteger(id, value);
        }

        @Override
        public void overrideText(int id, int valueId) {
            String text = getText(valueId);
            if (text != null) {
                loadText(id, text);
            }
        }

        @Override
        public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {
            mRemoteComposeState.cacheData(id, animatedFloat);
        }

        @Override
        public void loadShader(int id, @NonNull ShaderData value) {
            mRemoteComposeState.cacheData(id, value);
        }

        @Override
        public float getFloat(int id) {
            return mRemoteComposeState.getFloat(id);
        }

        @Override
        public int getInteger(int id) {
            return mRemoteComposeState.getInteger(id);
        }

        @Override
        public long getLong(int id) {
            Object obj = mRemoteComposeState.getObject(id);
            if (obj instanceof LongConstant) {
                return ((LongConstant) obj).getValue();
            }
            return 0L;
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {
            mRemoteComposeState.listenToVar(id, variableSupport);
        }

        @Override
        public @Nullable ArrayList<VariableSupport> getListeners(int id) {
            return mRemoteComposeState.getListeners(id);
        }

        @Override
        public int updateOps() {
            return mRemoteComposeState.getOpsToUpdate(this, currentTime);
        }

        @Override
        public void markVariableDirty(int id) {
            mRemoteComposeState.markVariableDirty(id);
        }

        @Override
        public @Nullable ShaderData getShader(int id) {
            return (ShaderData) mRemoteComposeState.getFromId(id);
        }

        @Override
        public void addTouchListener(@NonNull TouchListener touchExpression) {
            mDocument.addTouchListener(touchExpression);
        }

        @Override
        public void addClickArea(
                int id,
                int contentDescriptionId,
                float left,
                float top,
                float right,
                float bottom,
                int metadataId) {
            String cd = getText(contentDescriptionId);
            String meta = getText(metadataId);
            mDocument.addClickArea(id, cd, left, top, right, bottom, meta);
        }
    }
}
