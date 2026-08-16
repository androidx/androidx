/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core.operations;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.VariableProvider;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Operation to deal with bitmap data On getting an Image during a draw call the bitmap is
 * compressed and saved in playback the image is decompressed
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BitmapData extends Operation
        implements SerializableToString, Serializable, ComponentData, VariableProvider {
    private static final int OP_CODE = Operations.DATA_BITMAP;
    private static final String CLASS_NAME = "BitmapData";
    public int mImageId;
    int mImageWidth;
    int mImageHeight;
    short mType;
    short mEncoding;
    byte @NonNull [] mBitmap;

    /** The data is encoded in the file (default) */
    public static final short ENCODING_INLINE = 0;

    /** The data is encoded in the url */
    public static final short ENCODING_URL = 1;

    /** The data is encoded as a reference to file */
    public static final short ENCODING_FILE = 2;

    /** allocates a new bitmap data with value = 0 */
    public static final short ENCODING_EMPTY = 3;

    /** allocates an offscreen buffer dynamically sized to a target component */
    public static final short ENCODING_COMPONENT_OFFSCREEN_BUFFER = 4;

    /** The data is encoded as PNG_8888 (default) */
    public static final short TYPE_PNG_8888 = 0;

    /** The data is encoded as Generic PNG
     *  Not to be used with ENCODING_INLINE, which requires a specific type
    */
    public static final short TYPE_PNG = 1;

    /** The data is encoded as RAW 8 bit */
    public static final short TYPE_RAW8 = 2;

    /** The data is encoded as RAW 8888 bit */
    public static final short TYPE_RAW8888 = 3;

    /** The data is encoded as PNG_8888 but decoded as ALPHA_8 */
    public static final short TYPE_PNG_ALPHA_8 = 4;

    @Override
    public int getId() {
        return mImageId;
    }

    @Override
    public void setId(int id) {
        mImageId = id;
    }

    /**
     * create a bitmap structure
     *
     * @param imageId the id to store the image
     * @param width the width of the image
     * @param height the height of the image
     * @param bitmap the data
     */
    public BitmapData(int imageId, int width, int height, byte @NonNull [] bitmap) {
        this.mImageId = imageId;
        this.mImageWidth = width;
        this.mImageHeight = height;
        this.mBitmap = bitmap;
    }

    /**
     * create a bitmap structure
     *
     * @param imageId the id to store the image
     * @param width the width of the image
     * @param height the height of the image
     * @param bitmap the data
     */
    public BitmapData(
            int imageId,
            short type,
            short width,
            short encoding,
            short height,
            byte @NonNull [] bitmap) {
        this.mImageId = imageId;
        this.mType = type;
        this.mImageWidth = width;
        this.mEncoding = encoding;
        this.mImageHeight = height;
        this.mBitmap = bitmap;
    }

    /**
     * Update the bitmap data
     *
     * @param from the bitmap to copy
     */
    public void update(@NonNull BitmapData from) {
        this.mImageWidth = from.mImageWidth;
        this.mImageHeight = from.mImageHeight;
        this.mBitmap = from.mBitmap;
        this.mType = from.mType;
        this.mEncoding = from.mEncoding;
    }

    /**
     * The width of the image
     *
     * @return the width
     */
    public int getWidth() {
        return mImageWidth;
    }

    /**
     * The height of the image
     *
     * @return the height
     */
    public int getHeight() {
        return mImageHeight;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mImageId,
                mType,
                (short) mImageWidth,
                mEncoding,
                (short) mImageHeight,
                mBitmap);
    }

    @NonNull
    @Override
    public String toString() {
        return "BITMAP DATA " + mImageId;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * The type of the image
     *
     * @return the type of the image
     */
    public int getType() {
        return mType;
    }

    /**
     * Add the image to the document
     *
     * @param buffer document to write to
     * @param imageId the id the image will be stored under
     * @param width the width of the image
     * @param height the height of the image
     * @param bitmap the data used to store/encode the image
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int imageId,
            int width,
            int height,
            byte @NonNull [] bitmap) {
        buffer.start(OP_CODE);
        buffer.writeInt(imageId);
        buffer.writeInt(width);
        buffer.writeInt(height);
        buffer.writeBuffer(bitmap);
    }

    /**
     * Add the image to the document (using the enhanced encoding)
     *
     * @param buffer document to write to
     * @param imageId the id the image will be stored under
     * @param type the type of image
     * @param width the width of the image
     * @param encoding the encoding
     * @param height the height of the image
     * @param bitmap the data used to store/encode the image
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int imageId,
            short type,
            short width,
            short encoding,
            short height,
            byte @NonNull [] bitmap) {
        buffer.start(OP_CODE);
        buffer.writeInt(imageId);
        int w = (((int) type) << 16) | width;
        int h = (((int) encoding) << 16) | height;
        buffer.writeInt(w);
        buffer.writeInt(h);
        buffer.writeBuffer(bitmap);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int imageId = buffer.readId();
        int width = buffer.readInt();
        int height = buffer.readInt();
        int type;
        if (width > 0xffff) {
            type = width >> 16;
            width = width & 0xffff;
        } else {
            type = TYPE_PNG_8888;
        }

        int encoding;
        if (height > 0xffff) {
            encoding = height >> 16;
            height = height & 0xffff;
        } else {
            encoding = ENCODING_INLINE;
        }
        if (!Limits.ENABLE_IMAGE_URLS) {
            if (ENCODING_URL == encoding) {
                throw new RuntimeException("URL image not supported [" + imageId + "]");
            }
        }
        if (!Limits.ENABLE_IMAGE_FILES) {
            if (ENCODING_FILE == encoding) {
                throw new RuntimeException("File image not supported [" + imageId + "]");
            }
        }
        if (width < 1
                || height < 1
                || height > Limits.MAX_IMAGE_DIMENSION
                || width > Limits.MAX_IMAGE_DIMENSION
                || width * height > Limits.MAX_BITMAP_MEMORY) {
            throw new RuntimeException("Dimension of image is invalid " + width + "x" + height);
        }
        // This can be reading a JPEG, GIF, PNG or RAW image. Make sure the size is reasonable.
        byte[] bitmap = buffer.readBuffer(width * height * 4 + Limits.MAX_IMAGE_HEADER_SIZE);
        BitmapData bitmapData = new BitmapData(imageId, width, height, bitmap);
        bitmapData.mType = (short) type;
        bitmapData.mEncoding = (short) encoding;
        operations.add(bitmapData);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Embed or reference bitmap image data")
                .field(DocumentedOperation.INT, "imageId", "The ID of the bitmap")
                .field(DocumentedOperation.INT, "widthAndType", "Encoded width and image type")
                .field(
                        DocumentedOperation.INT,
                        "heightAndEncoding",
                        "Encoded height and data encoding")
                .field(DocumentedOperation.BYTE_ARRAY, "bitmap", "The raw or encoded bitmap data");
    }

    /**
     * Returns the encoding type of this bitmap operation.
     *
     * @return the encoding constant (e.g. {@link #ENCODING_INLINE} or {@link
     *     #ENCODING_COMPONENT_OFFSCREEN_BUFFER})
     */
    public short getEncoding() {
        return mEncoding;
    }

    /**
     * Returns the component ID encoded in the payload when {@link #getEncoding()} is {@link
     * #ENCODING_COMPONENT_OFFSCREEN_BUFFER}, or 0 otherwise.
     *
     * @return the associated component ID or variable ID, or 0
     */
    public int getComponentId() {
        if (mEncoding == ENCODING_COMPONENT_OFFSCREEN_BUFFER && mBitmap.length >= 4) {
            return ((mBitmap[0] & 0xFF) << 24)
                    | ((mBitmap[1] & 0xFF) << 16)
                    | ((mBitmap[2] & 0xFF) << 8)
                    | (mBitmap[3] & 0xFF);
        }
        return 0;
    }

    /**
     * Updates the width of this bitmap operation.
     *
     * @param width the new width in pixels
     */
    public void setWidth(int width) {
        mImageWidth = width;
    }

    /**
     * Updates the height of this bitmap operation.
     *
     * @param height the new height in pixels
     */
    public void setHeight(int height) {
        mImageHeight = height;
    }

    private static final byte[] EMPTY_BITMAP_BYTES = new byte[0];

    private static final class PooledBitmap {
        final Object mBitmap;
        final int mWidth;
        final int mHeight;

        PooledBitmap(@NonNull Object bitmap, int width, int height) {
            mBitmap = bitmap;
            mWidth = width;
            mHeight = height;
        }
    }

    private static final class SavedBinding {
        final BitmapData mBitmapData;
        final PooledBitmap mPrevBitmap;
        final int mPrevWidth;
        final int mPrevHeight;

        SavedBinding(
                @NonNull BitmapData bitmapData,
                @NonNull PooledBitmap prevBitmap,
                int prevWidth,
                int prevHeight) {
            mBitmapData = bitmapData;
            mPrevBitmap = prevBitmap;
            mPrevWidth = prevWidth;
            mPrevHeight = prevHeight;
        }
    }

    private static final class ScopeFrame {
        final java.util.ArrayList<PooledBitmap> mAcquired = new java.util.ArrayList<>();
        final java.util.ArrayList<SavedBinding> mSaved = new java.util.ArrayList<>();
    }

    private static final class OffscreenBitmapPool {
        private int mNextPoolBitmapId = 0x70000000;
        private final java.util.ArrayList<PooledBitmap> mFreeBitmapPool =
                new java.util.ArrayList<>();
        private final java.util.ArrayList<PooledBitmap> mInUseBitmaps =
                new java.util.ArrayList<>();
        private final java.util.HashMap<Integer, PooledBitmap> mActiveIdToBitmap =
                new java.util.HashMap<>();
        private final java.util.ArrayList<ScopeFrame> mScopeStack =
                new java.util.ArrayList<>();
        private int mMaxPooledWidth = 0;
        private int mMaxPooledHeight = 0;

        private void returnBitmapToPool(PooledBitmap entry) {
            if (entry == null || mFreeBitmapPool.contains(entry)) {
                return;
            }
            if (mFreeBitmapPool.size() >= Limits.MAX_BITMAP_POOL_SIZE) {
                int smallestIdx = 0;
                int smallestArea =
                        mFreeBitmapPool.get(0).mWidth * mFreeBitmapPool.get(0).mHeight;
                for (int i = 1; i < mFreeBitmapPool.size(); i++) {
                    PooledBitmap b = mFreeBitmapPool.get(i);
                    int area = b.mWidth * b.mHeight;
                    if (area < smallestArea) {
                        smallestArea = area;
                        smallestIdx = i;
                    }
                }
                mFreeBitmapPool.remove(smallestIdx);
            }
            mFreeBitmapPool.add(entry);
        }

        private PooledBitmap acquireBitmapFromPool(
                @NonNull RemoteContext context, int reqWidth, int reqHeight) {
            mMaxPooledWidth = Math.max(mMaxPooledWidth, reqWidth);
            mMaxPooledHeight = Math.max(mMaxPooledHeight, reqHeight);
            int bestIdx = -1;
            int bestArea = Integer.MAX_VALUE;
            for (int i = 0; i < mFreeBitmapPool.size(); i++) {
                PooledBitmap candidate = mFreeBitmapPool.get(i);
                if (candidate.mWidth >= reqWidth && candidate.mHeight >= reqHeight) {
                    int area = candidate.mWidth * candidate.mHeight;
                    if (area < bestArea) {
                        bestArea = area;
                        bestIdx = i;
                    }
                }
            }
            if (bestIdx >= 0) {
                PooledBitmap reused = mFreeBitmapPool.remove(bestIdx);
                mInUseBitmaps.add(reused);
                return reused;
            }
            if (!mFreeBitmapPool.isEmpty()) {
                int smallestIdx = 0;
                int smallestArea =
                        mFreeBitmapPool.get(0).mWidth * mFreeBitmapPool.get(0).mHeight;
                for (int i = 1; i < mFreeBitmapPool.size(); i++) {
                    PooledBitmap b = mFreeBitmapPool.get(i);
                    int area = b.mWidth * b.mHeight;
                    if (area < smallestArea) {
                        smallestArea = area;
                        smallestIdx = i;
                    }
                }
                mFreeBitmapPool.remove(smallestIdx);
            }
            int allocW = Math.max(reqWidth, mMaxPooledWidth);
            int allocH = Math.max(reqHeight, mMaxPooledHeight);
            int slotId = mNextPoolBitmapId++;
            context.loadBitmap(
                    slotId, ENCODING_EMPTY, TYPE_RAW8888, allocW, allocH, EMPTY_BITMAP_BYTES);
            Object platformBitmap =
                    context.mRemoteComposeState != null
                            ? context.mRemoteComposeState.getFromId(slotId)
                            : null;
            if (platformBitmap == null) {
                return null;
            }
            PooledBitmap created = new PooledBitmap(platformBitmap, allocW, allocH);
            mInUseBitmaps.add(created);
            return created;
        }

        void ensureBitmap(
                @NonNull RemoteContext context,
                @NonNull BitmapData bd,
                int prevW,
                int prevH,
                int reqW,
                int reqH) {
            int imageId = bd.mImageId;
            PooledBitmap current = mActiveIdToBitmap.get(imageId);
            ScopeFrame topScope =
                    mScopeStack.isEmpty() ? null : mScopeStack.get(mScopeStack.size() - 1);
            boolean ownedByCurrentScope =
                    topScope == null || topScope.mAcquired.contains(current);
            boolean keepExisting =
                    current != null
                            && ownedByCurrentScope
                            && current.mWidth >= reqW
                            && current.mHeight >= reqH;
            if (!keepExisting) {
                if (current != null) {
                    if (ownedByCurrentScope) {
                        mActiveIdToBitmap.remove(imageId);
                        mInUseBitmaps.remove(current);
                        if (topScope != null) {
                            topScope.mAcquired.remove(current);
                        }
                        returnBitmapToPool(current);
                    } else {
                        topScope.mSaved.add(new SavedBinding(bd, current, prevW, prevH));
                        mActiveIdToBitmap.remove(imageId);
                    }
                }
                PooledBitmap acquired = acquireBitmapFromPool(context, reqW, reqH);
                if (acquired != null && context.mRemoteComposeState != null) {
                    if (topScope != null) {
                        topScope.mAcquired.add(acquired);
                    }
                    mActiveIdToBitmap.put(imageId, acquired);
                    if (context.mRemoteComposeState.containsId(imageId)) {
                        context.mRemoteComposeState.updateData(imageId, acquired.mBitmap);
                    } else {
                        context.mRemoteComposeState.cacheData(imageId, acquired.mBitmap);
                    }
                }
            }
        }

        void pushScope() {
            mScopeStack.add(new ScopeFrame());
        }

        void popScope(@NonNull RemoteContext context) {
            if (mScopeStack.isEmpty()) {
                releaseAll();
                return;
            }
            ScopeFrame frame = mScopeStack.remove(mScopeStack.size() - 1);
            for (int i = 0; i < frame.mAcquired.size(); i++) {
                PooledBitmap b = frame.mAcquired.get(i);
                mInUseBitmaps.remove(b);
                returnBitmapToPool(b);
                mActiveIdToBitmap.values().remove(b);
            }
            for (int i = frame.mSaved.size() - 1; i >= 0; i--) {
                SavedBinding saved = frame.mSaved.get(i);
                saved.mBitmapData.mImageWidth = saved.mPrevWidth;
                saved.mBitmapData.mImageHeight = saved.mPrevHeight;
                mActiveIdToBitmap.put(saved.mBitmapData.mImageId, saved.mPrevBitmap);
                if (context.mRemoteComposeState != null) {
                    context.mRemoteComposeState.updateData(
                            saved.mBitmapData.mImageId, saved.mPrevBitmap.mBitmap);
                }
            }
        }

        void resumeTargetCanvas(@NonNull PaintContext paintContext, int bitmapId) {
            RemoteContext context = paintContext.getContext();
            Object prevObj = null;
            if (!mScopeStack.isEmpty()) {
                ScopeFrame frame = mScopeStack.get(mScopeStack.size() - 1);
                for (int i = frame.mSaved.size() - 1; i >= 0; i--) {
                    SavedBinding saved = frame.mSaved.get(i);
                    if (saved.mBitmapData.mImageId == bitmapId) {
                        prevObj = saved.mPrevBitmap.mBitmap;
                        break;
                    }
                }
            }
            if (prevObj != null && context.mRemoteComposeState != null) {
                Object curObj = context.mRemoteComposeState.getFromId(bitmapId);
                context.mRemoteComposeState.updateData(bitmapId, prevObj);
                paintContext.drawToBitmap(bitmapId, 1, 0);
                if (curObj != null) {
                    context.mRemoteComposeState.updateData(bitmapId, curObj);
                }
            } else {
                paintContext.drawToBitmap(bitmapId, 1, 0);
            }
        }

        void releaseAll() {
            for (int i = 0; i < mInUseBitmaps.size(); i++) {
                returnBitmapToPool(mInUseBitmaps.get(i));
            }
            mInUseBitmaps.clear();
            mActiveIdToBitmap.clear();
            mScopeStack.clear();
        }
    }

    private static final java.util.WeakHashMap<RemoteContext, OffscreenBitmapPool> sPools =
            new java.util.WeakHashMap<>();

    private static @NonNull OffscreenBitmapPool getPool(@NonNull RemoteContext context) {
        synchronized (sPools) {
            OffscreenBitmapPool pool = sPools.get(context);
            if (pool == null) {
                pool = new OffscreenBitmapPool();
                sPools.put(context, pool);
            }
            return pool;
        }
    }

    /**
     * Pushes a new offscreen bitmap pool scope for the given {@link RemoteContext}.
     *
     * @param context the current RemoteContext
     */
    public static void pushOffscreenScope(@NonNull RemoteContext context) {
        synchronized (sPools) {
            getPool(context).pushScope();
        }
    }

    /**
     * Pops the current offscreen bitmap pool scope, returning bitmaps acquired in that scope to the
     * pool and restoring any overwritten {@link BitmapData} bindings.
     *
     * @param context the current RemoteContext
     */
    public static void popOffscreenScope(@NonNull RemoteContext context) {
        synchronized (sPools) {
            OffscreenBitmapPool pool = sPools.get(context);
            if (pool != null) {
                pool.popScope(context);
            }
        }
    }

    /**
     * Switches the active {@link PaintContext} canvas back to an enclosing offscreen target bitmap
     * without erasing it or losing the current scope's bitmap binding.
     *
     * @param paintContext the current PaintContext
     * @param bitmapId the enclosing offscreen target bitmap ID
     */
    public static void resumeOffscreenTargetCanvas(
            @NonNull PaintContext paintContext, int bitmapId) {
        synchronized (sPools) {
            OffscreenBitmapPool pool = sPools.get(paintContext.getContext());
            if (pool != null) {
                pool.resumeTargetCanvas(paintContext, bitmapId);
            } else {
                paintContext.drawToBitmap(bitmapId, 1, 0);
            }
        }
    }

    /**
     * Releases all active offscreen bitmaps back to the offscreen bitmap pool for the given {@link
     * RemoteContext}.
     *
     * @param context the current RemoteContext
     */
    public static void releaseOffscreenBitmaps(@NonNull RemoteContext context) {
        synchronized (sPools) {
            OffscreenBitmapPool pool = sPools.get(context);
            if (pool != null) {
                pool.releaseAll();
            }
        }
    }

    /**
     * Resolves the target component for a {@link #ENCODING_COMPONENT_OFFSCREEN_BUFFER} bitmap,
     * updates its width and height to match the component bounds, and acquires a backing bitmap
     * from the offscreen bitmap pool.
     *
     * @param context the current RemoteContext
     */
    public void ensureOffscreenBitmap(@NonNull RemoteContext context) {
        if (mEncoding != ENCODING_COMPONENT_OFFSCREEN_BUFFER) {
            return;
        }
        androidx.compose.remote.core.operations.layout.Component targetComp = null;
        int compId = getComponentId();
        if (compId != 0) {
            int resolved = context.getInteger(compId);
            if (resolved != 0) {
                compId = resolved;
            } else {
                float fVal = context.getFloat(compId);
                if (!Float.isNaN(fVal) && fVal != 0f) {
                    compId = (int) fVal;
                }
            }
            if (context.getDocument() != null) {
                targetComp = context.getDocument().getComponent(compId);
            }
        }
        if (targetComp == null && context.getPaintContext() != null) {
            targetComp = context.getPaintContext().getOffscreenComponent();
        }
        if (targetComp == null) {
            targetComp = context.mLastComponent;
        }
        if (targetComp == null) {
            return;
        }
        int prevW = mImageWidth;
        int prevH = mImageHeight;
        float x = (float) Math.floor(targetComp.getX());
        float y = (float) Math.floor(targetComp.getY());
        int reqW = Math.max(1, (int) Math.ceil((targetComp.getX() - x) + targetComp.getWidth()));
        int reqH = Math.max(1, (int) Math.ceil((targetComp.getY() - y) + targetComp.getHeight()));
        mImageWidth = reqW;
        mImageHeight = reqH;
        getPool(context).ensureBitmap(context, this, prevW, prevH, reqW, reqH);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.putObject(mImageId, this);
        if (mEncoding == ENCODING_COMPONENT_OFFSCREEN_BUFFER) {
            ensureOffscreenBitmap(context);
        } else {
            context.loadBitmap(mImageId, mEncoding, mType, mImageWidth, mImageHeight, mBitmap);
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                CLASS_NAME + " id " + mImageId + " (" + mImageWidth + "x" + mImageHeight + ")");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("imageId", mImageId)
                .add("imageWidth", mImageWidth)
                .add("imageHeight", mImageHeight)
                .add("imageType", getImageTypeString(mType))
                .add("encoding", getEncodingString(mEncoding));
    }

    private String getEncodingString(short encoding) {
        switch (encoding) {
            case ENCODING_INLINE:
                return "ENCODING_INLINE";
            case ENCODING_URL:
                return "ENCODING_URL";
            case ENCODING_FILE:
                return "ENCODING_FILE";
            case ENCODING_COMPONENT_OFFSCREEN_BUFFER:
                return "ENCODING_COMPONENT_OFFSCREEN_BUFFER";
            default:
                return "ENCODING_INVALID";
        }
    }

    private String getImageTypeString(short type) {
        switch (type) {
            case TYPE_PNG_8888:
                return "TYPE_PNG_8888";
            case TYPE_PNG:
                return "TYPE_PNG";
            case TYPE_RAW8:
                return "TYPE_RAW8";
            case TYPE_RAW8888:
                return "TYPE_RAW8888";
            case TYPE_PNG_ALPHA_8:
                return "TYPE_PNG_ALPHA_8";
            default:
                return "TYPE_INVALID";
        }
    }
}
