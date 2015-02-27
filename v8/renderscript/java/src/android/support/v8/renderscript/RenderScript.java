/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v8.renderscript;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;
import android.util.Log;
import android.view.Surface;

/**
 * This class provides access to a RenderScript context, which controls RenderScript
 * initialization, resource management, and teardown. An instance of the RenderScript
 * class must be created before any other RS objects can be created.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about creating an application that uses RenderScript, read the
 * <a href="{@docRoot}guide/topics/renderscript/index.html">RenderScript</a> developer guide.</p>
 * </div>
 **/
public class RenderScript {
    static final String LOG_TAG = "RenderScript_jni";
    static final boolean DEBUG  = false;
    @SuppressWarnings({"UnusedDeclaration", "deprecation"})
    static final boolean LOG_ENABLED = false;

    private Context mApplicationContext;
    private String mNativeLibDir;

    /*
     * We use a class initializer to allow the native code to cache some
     * field offsets.
     */
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    static boolean sInitialized;
    static boolean sUseGCHooks;
    static Object sRuntime;
    static Method registerNativeAllocation;
    static Method registerNativeFree;

    static Object lock = new Object();

    // Non-threadsafe functions.
    native boolean nLoadSO(boolean useNative);
    native boolean nLoadIOSO();
    native long nDeviceCreate();
    native void nDeviceDestroy(long dev);
    native void nDeviceSetConfig(long dev, int param, int value);
    native int nContextGetUserMessage(long con, int[] data);
    native String nContextGetErrorMessage(long con);
    native int  nContextPeekMessage(long con, int[] subID);
    native void nContextInitToClient(long con);
    native void nContextDeinitToClient(long con);

    static private int sNative = -1;
    static private int sSdkVersion = -1;
    static private boolean useIOlib = false;

    /*
     * Detect the bitness of the VM to allow FieldPacker to do the right thing.
     */
    static native int rsnSystemGetPointerSize();
    static int sPointerSize;

    /**
     * Determines whether or not we should be thunking into the native
     * RenderScript layer or actually using the compatibility library.
     */
    static private boolean setupNative(int sdkVersion, Context ctx) {
        if (sNative == -1) {

            // get the value of the debug.rs.forcecompat property
            int forcecompat = 0;
            try {
                Class<?> sysprop = Class.forName("android.os.SystemProperties");
                Class[] signature = {String.class, Integer.TYPE};
                Method getint = sysprop.getDeclaredMethod("getInt", signature);
                Object[] args = {"debug.rs.forcecompat", new Integer(0)};
                forcecompat = ((java.lang.Integer)getint.invoke(null, args)).intValue();
            } catch (Exception e) {

            }

            if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
                     && forcecompat == 0) {
                sNative = 1;
            } else {
                sNative = 0;
            }


            if (sNative == 1) {
                // Workarounds that may disable thunking go here
                ApplicationInfo info;
                try {
                    info = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(),
                                                                      PackageManager.GET_META_DATA);
                } catch (PackageManager.NameNotFoundException e) {
                    // assume no workarounds needed
                    return true;
                }
                long minorVersion = 0;

                // load minorID from reflection
                try {
                    Class<?> javaRS = Class.forName("android.renderscript.RenderScript");
                    Method getMinorID = javaRS.getDeclaredMethod("getMinorID");
                    minorVersion = ((java.lang.Long)getMinorID.invoke(null)).longValue();
                } catch (Exception e) {
                    // minor version remains 0 on devices with no possible WARs
                }

                if (info.metaData != null) {
                    // asynchronous teardown: minor version 1+
                    if (info.metaData.getBoolean("com.android.support.v8.renderscript.EnableAsyncTeardown") == true) {
                        if (minorVersion == 0) {
                            sNative = 0;
                        }
                    }

                    // blur issues on some drivers with 4.4
                    if (info.metaData.getBoolean("com.android.support.v8.renderscript.EnableBlurWorkaround") == true) {
                        if (android.os.Build.VERSION.SDK_INT <= 19) {
                            //android.util.Log.e("rs", "war on");
                            sNative = 0;
                        }
                    }
                }
                // end of workarounds
            }
        }

        if (sNative == 1) {
            return true;
        }
        return false;
    }

    /**
     * Name of the file that holds the object cache.
     */
    private static final String CACHE_PATH = "com.android.renderscript.cache";
    static String mCachePath;

     /**
     * Sets the directory to use as a persistent storage for the
     * renderscript object file cache.
     *
     * @hide
     * @param cacheDir A directory the current process can write to
     */
    public static void setupDiskCache(File cacheDir) {
        File f = new File(cacheDir, CACHE_PATH);
        mCachePath = f.getAbsolutePath();
        f.mkdirs();
    }

    /**
     * ContextType specifies the specific type of context to be created.
     *
     */
    public enum ContextType {
        /**
         * NORMAL context, this is the default and what shipping apps should
         * use.
         */
        NORMAL (0),

        /**
         * DEBUG context, perform extra runtime checks to validate the
         * kernels and APIs are being used as intended.  Get and SetElementAt
         * will be bounds checked in this mode.
         */
        DEBUG (1),

        /**
         * PROFILE context, Intended to be used once the first time an
         * application is run on a new device.  This mode allows the runtime to
         * do additional testing and performance tuning.
         */
        PROFILE (2);

        int mID;
        ContextType(int id) {
            mID = id;
        }
    }

    // Methods below are wrapped to protect the non-threadsafe
    // lockless fifo.

    native long  rsnContextCreate(long dev, int ver, int sdkVer, int contextType, String nativeLibDir);
    synchronized long nContextCreate(long dev, int ver, int sdkVer, int contextType, String nativeLibDir) {
        return rsnContextCreate(dev, ver, sdkVer, contextType, nativeLibDir);
    }
    native void rsnContextDestroy(long con);
    synchronized void nContextDestroy() {
        validate();

        // take teardown lock
        // teardown lock can only be taken when no objects are being destroyed
        ReentrantReadWriteLock.WriteLock wlock = mRWLock.writeLock();
        wlock.lock();

        long curCon = mContext;
        // context is considered dead as of this point
        mContext = 0;

        wlock.unlock();
        rsnContextDestroy(curCon);
    }
    native void rsnContextSetPriority(long con, int p);
    synchronized void nContextSetPriority(int p) {
        validate();
        rsnContextSetPriority(mContext, p);
    }
    native void rsnContextDump(long con, int bits);
    synchronized void nContextDump(int bits) {
        validate();
        rsnContextDump(mContext, bits);
    }
    native void rsnContextFinish(long con);
    synchronized void nContextFinish() {
        validate();
        rsnContextFinish(mContext);
    }

    native void rsnContextSendMessage(long con, int id, int[] data);
    synchronized void nContextSendMessage(int id, int[] data) {
        validate();
        rsnContextSendMessage(mContext, id, data);
    }

    // nObjDestroy is explicitly _not_ synchronous to prevent crashes in finalizers
    native void rsnObjDestroy(long con, long id);
    void nObjDestroy(long id) {
        // There is a race condition here.  The calling code may be run
        // by the gc while teardown is occuring.  This protects againts
        // deleting dead objects.
        if (mContext != 0) {
            rsnObjDestroy(mContext, id);
        }
    }

    native long  rsnElementCreate(long con, long type, int kind, boolean norm, int vecSize);
    synchronized long nElementCreate(long type, int kind, boolean norm, int vecSize) {
        validate();
        return rsnElementCreate(mContext, type, kind, norm, vecSize);
    }
    native long  rsnElementCreate2(long con, long[] elements, String[] names, int[] arraySizes);
    synchronized long nElementCreate2(long[] elements, String[] names, int[] arraySizes) {
        validate();
        return rsnElementCreate2(mContext, elements, names, arraySizes);
    }
    native void rsnElementGetNativeData(long con, long id, int[] elementData);
    synchronized void nElementGetNativeData(long id, int[] elementData) {
        validate();
        rsnElementGetNativeData(mContext, id, elementData);
    }
    native void rsnElementGetSubElements(long con, long id,
                                         long[] IDs, String[] names, int[] arraySizes);
    synchronized void nElementGetSubElements(long id, long[] IDs, String[] names, int[] arraySizes) {
        validate();
        rsnElementGetSubElements(mContext, id, IDs, names, arraySizes);
    }

    native long rsnTypeCreate(long con, long eid, int x, int y, int z, boolean mips, boolean faces, int yuv);
    synchronized long nTypeCreate(long eid, int x, int y, int z, boolean mips, boolean faces, int yuv) {
        validate();
        return rsnTypeCreate(mContext, eid, x, y, z, mips, faces, yuv);
    }

    native void rsnTypeGetNativeData(long con, long id, long[] typeData);
    synchronized void nTypeGetNativeData(long id, long[] typeData) {
        validate();
        rsnTypeGetNativeData(mContext, id, typeData);
    }

    native long  rsnAllocationCreateTyped(long con, long type, int mip, int usage, long pointer);
    synchronized long nAllocationCreateTyped(long type, int mip, int usage, long pointer) {
        validate();
        return rsnAllocationCreateTyped(mContext, type, mip, usage, pointer);
    }
    native long  rsnAllocationCreateFromBitmap(long con, long type, int mip, Bitmap bmp, int usage);
    synchronized long nAllocationCreateFromBitmap(long type, int mip, Bitmap bmp, int usage) {
        validate();
        return rsnAllocationCreateFromBitmap(mContext, type, mip, bmp, usage);
    }

    native long  rsnAllocationCreateBitmapBackedAllocation(long con, long type, int mip, Bitmap bmp, int usage);
    synchronized long nAllocationCreateBitmapBackedAllocation(long type, int mip, Bitmap bmp, int usage) {
        validate();
        return rsnAllocationCreateBitmapBackedAllocation(mContext, type, mip, bmp, usage);
    }


    native long  rsnAllocationCubeCreateFromBitmap(long con, long type, int mip, Bitmap bmp, int usage);
    synchronized long nAllocationCubeCreateFromBitmap(long type, int mip, Bitmap bmp, int usage) {
        validate();
        return rsnAllocationCubeCreateFromBitmap(mContext, type, mip, bmp, usage);
    }
    native long  rsnAllocationCreateBitmapRef(long con, long type, Bitmap bmp);
    synchronized long nAllocationCreateBitmapRef(long type, Bitmap bmp) {
        validate();
        return rsnAllocationCreateBitmapRef(mContext, type, bmp);
    }
    native long  rsnAllocationCreateFromAssetStream(long con, int mips, int assetStream, int usage);
    synchronized long nAllocationCreateFromAssetStream(int mips, int assetStream, int usage) {
        validate();
        return rsnAllocationCreateFromAssetStream(mContext, mips, assetStream, usage);
    }

    native void  rsnAllocationCopyToBitmap(long con, long alloc, Bitmap bmp);
    synchronized void nAllocationCopyToBitmap(long alloc, Bitmap bmp) {
        validate();
        rsnAllocationCopyToBitmap(mContext, alloc, bmp);
    }


    native void rsnAllocationSyncAll(long con, long alloc, int src);
    synchronized void nAllocationSyncAll(long alloc, int src) {
        validate();
        rsnAllocationSyncAll(mContext, alloc, src);
    }

    native void rsnAllocationSetSurface(long con, long alloc, Surface sur);
    synchronized void nAllocationSetSurface(long alloc, Surface sur) {
        validate();
        rsnAllocationSetSurface(mContext, alloc, sur);
    }

    native void rsnAllocationIoSend(long con, long alloc);
    synchronized void nAllocationIoSend(long alloc) {
        validate();
        rsnAllocationIoSend(mContext, alloc);
    }
    native void rsnAllocationIoReceive(long con, long alloc);
    synchronized void nAllocationIoReceive(long alloc) {
        validate();
        rsnAllocationIoReceive(mContext, alloc);
    }


    native void rsnAllocationGenerateMipmaps(long con, long alloc);
    synchronized void nAllocationGenerateMipmaps(long alloc) {
        validate();
        rsnAllocationGenerateMipmaps(mContext, alloc);
    }
    native void  rsnAllocationCopyFromBitmap(long con, long alloc, Bitmap bmp);
    synchronized void nAllocationCopyFromBitmap(long alloc, Bitmap bmp) {
        validate();
        rsnAllocationCopyFromBitmap(mContext, alloc, bmp);
    }


    native void rsnAllocationData1D(long con, long id, int off, int mip, int count, Object d, int sizeBytes, int dt,
                                    int mSize, boolean usePadding);
    synchronized void nAllocationData1D(long id, int off, int mip, int count, Object d, int sizeBytes, Element.DataType dt,
                                        int mSize, boolean usePadding) {
        validate();
        rsnAllocationData1D(mContext, id, off, mip, count, d, sizeBytes, dt.mID, mSize, usePadding);
    }

    native void rsnAllocationElementData1D(long con,long id, int xoff, int mip, int compIdx, byte[] d, int sizeBytes);
    synchronized void nAllocationElementData1D(long id, int xoff, int mip, int compIdx, byte[] d, int sizeBytes) {
        validate();
        rsnAllocationElementData1D(mContext, id, xoff, mip, compIdx, d, sizeBytes);
    }
    /*
    native void rsnAllocationElementData(long con,long id, int xoff, int yoff, int zoff, int mip, int compIdx, byte[] d, int sizeBytes);
    synchronized void nAllocationElementData(long id, int xoff, int yoff, int zoff, int mip, int compIdx, byte[] d, int sizeBytes) {
        validate();
        rsnAllocationElementData(mContext, id, xoff, yoff, zoff, mip, compIdx, d, sizeBytes);
    }
    */

    native void rsnAllocationData2D(long con,
                                    long dstAlloc, int dstXoff, int dstYoff,
                                    int dstMip, int dstFace,
                                    int width, int height,
                                    long srcAlloc, int srcXoff, int srcYoff,
                                    int srcMip, int srcFace);
    synchronized void nAllocationData2D(long dstAlloc, int dstXoff, int dstYoff,
                                        int dstMip, int dstFace,
                                        int width, int height,
                                        long srcAlloc, int srcXoff, int srcYoff,
                                        int srcMip, int srcFace) {
        validate();
        rsnAllocationData2D(mContext,
                            dstAlloc, dstXoff, dstYoff,
                            dstMip, dstFace,
                            width, height,
                            srcAlloc, srcXoff, srcYoff,
                            srcMip, srcFace);
    }

    native void rsnAllocationData2D(long con, long id, int xoff, int yoff, int mip, int face,
                                    int w, int h, Object d, int sizeBytes, int dt,
                                    int mSize, boolean usePadding);
    synchronized void nAllocationData2D(long id, int xoff, int yoff, int mip, int face,
                                        int w, int h, Object d, int sizeBytes, Element.DataType dt,
                                        int mSize, boolean usePadding) {
        validate();
        rsnAllocationData2D(mContext, id, xoff, yoff, mip, face, w, h, d, sizeBytes, dt.mID, mSize, usePadding);
    }

    native void rsnAllocationData2D(long con, long id, int xoff, int yoff, int mip, int face, Bitmap b);
    synchronized void nAllocationData2D(long id, int xoff, int yoff, int mip, int face, Bitmap b) {
        validate();
        rsnAllocationData2D(mContext, id, xoff, yoff, mip, face, b);
    }

    native void rsnAllocationData3D(long con,
                                    long dstAlloc, int dstXoff, int dstYoff, int dstZoff,
                                    int dstMip,
                                    int width, int height, int depth,
                                    long srcAlloc, int srcXoff, int srcYoff, int srcZoff,
                                    int srcMip);
    synchronized void nAllocationData3D(long dstAlloc, int dstXoff, int dstYoff, int dstZoff,
                                        int dstMip,
                                        int width, int height, int depth,
                                        long srcAlloc, int srcXoff, int srcYoff, int srcZoff,
                                        int srcMip) {
        validate();
        rsnAllocationData3D(mContext,
                            dstAlloc, dstXoff, dstYoff, dstZoff,
                            dstMip, width, height, depth,
                            srcAlloc, srcXoff, srcYoff, srcZoff, srcMip);
    }


    native void rsnAllocationData3D(long con, long id, int xoff, int yoff, int zoff, int mip,
                                    int w, int h, int depth, Object d, int sizeBytes, int dt,
                                    int mSize, boolean usePadding);
    synchronized void nAllocationData3D(long id, int xoff, int yoff, int zoff, int mip,
                                        int w, int h, int depth, Object d, int sizeBytes, Element.DataType dt,
                                        int mSize, boolean usePadding) {
        validate();
        rsnAllocationData3D(mContext, id, xoff, yoff, zoff, mip, w, h, depth, d, sizeBytes,
                            dt.mID, mSize, usePadding);
    }

    native void rsnAllocationRead(long con, long id, Object d, int dt, int mSize, boolean usePadding);
    synchronized void nAllocationRead(long id, Object d, Element.DataType dt, int mSize, boolean usePadding) {
        validate();
        rsnAllocationRead(mContext, id, d, dt.mID, mSize, usePadding);
    }

    native void rsnAllocationRead1D(long con, long id, int off, int mip, int count, Object d,
                                    int sizeBytes, int dt, int mSize, boolean usePadding);
    synchronized void nAllocationRead1D(long id, int off, int mip, int count, Object d,
                                        int sizeBytes, Element.DataType dt, int mSize, boolean usePadding) {
        validate();
        rsnAllocationRead1D(mContext, id, off, mip, count, d, sizeBytes, dt.mID, mSize, usePadding);
    }
    
    /*
    native void rsnAllocationElementRead(long con,long id, int xoff, int yoff, int zoff,
                                         int mip, int compIdx, Object d, int sizeBytes, int dt);
    synchronized void nAllocationElementRead(long id, int xoff, int yoff, int zoff,
                                             int mip, int compIdx, Object d, int sizeBytes,
                                             Element.DataType dt) {
        validate();
        rsnAllocationElementRead(mContext, id, xoff, yoff, zoff, mip, compIdx, d, sizeBytes, dt.mID);
    }
    */

    native void rsnAllocationRead2D(long con, long id, int xoff, int yoff, int mip, int face,
                                    int w, int h, Object d, int sizeBytes, int dt,
                                    int mSize, boolean usePadding);
    synchronized void nAllocationRead2D(long id, int xoff, int yoff, int mip, int face,
                                        int w, int h, Object d, int sizeBytes, Element.DataType dt,
                                        int mSize, boolean usePadding) {
        validate();
        rsnAllocationRead2D(mContext, id, xoff, yoff, mip, face, w, h, d, sizeBytes, dt.mID, mSize, usePadding);
    }

    /*
    native void rsnAllocationRead3D(long con, long id, int xoff, int yoff, int zoff, int mip,
                                    int w, int h, int depth, Object d, int sizeBytes, int dt,
                                    int mSize, boolean usePadding);
    synchronized void nAllocationRead3D(long id, int xoff, int yoff, int zoff, int mip,
                                        int w, int h, int depth, Object d, int sizeBytes, Element.DataType dt,
                                        int mSize, boolean usePadding) {
        validate();
        rsnAllocationRead3D(mContext, id, xoff, yoff, zoff, mip, w, h, depth, d, sizeBytes, dt.mID, mSize, usePadding);
    }
    */

    native long  rsnAllocationGetType(long con, long id);
    synchronized long nAllocationGetType(long id) {
        validate();
        return rsnAllocationGetType(mContext, id);
    }

    native void rsnAllocationResize1D(long con, long id, int dimX);
    synchronized void nAllocationResize1D(long id, int dimX) {
        validate();
        rsnAllocationResize1D(mContext, id, dimX);
    }
    native void rsnAllocationResize2D(long con, long id, int dimX, int dimY);
    synchronized void nAllocationResize2D(long id, int dimX, int dimY) {
        validate();
        rsnAllocationResize2D(mContext, id, dimX, dimY);
    }

    native void rsnScriptBindAllocation(long con, long script, long alloc, int slot);
    synchronized void nScriptBindAllocation(long script, long alloc, int slot) {
        validate();
        rsnScriptBindAllocation(mContext, script, alloc, slot);
    }
    native void rsnScriptSetTimeZone(long con, long script, byte[] timeZone);
    synchronized void nScriptSetTimeZone(long script, byte[] timeZone) {
        validate();
        rsnScriptSetTimeZone(mContext, script, timeZone);
    }
    native void rsnScriptInvoke(long con, long id, int slot);
    synchronized void nScriptInvoke(long id, int slot) {
        validate();
        rsnScriptInvoke(mContext, id, slot);
    }
    native void rsnScriptForEach(long con, long id, int slot, long ain, long aout, byte[] params);
    native void rsnScriptForEach(long con, long id, int slot, long ain, long aout);
    native void rsnScriptForEachClipped(long con, long id, int slot, long ain, long aout, byte[] params,
                                        int xstart, int xend, int ystart, int yend, int zstart, int zend);
    native void rsnScriptForEachClipped(long con, long id, int slot, long ain, long aout,
                                        int xstart, int xend, int ystart, int yend, int zstart, int zend);
    synchronized void nScriptForEach(long id, int slot, long ain, long aout, byte[] params) {
        validate();
        if (params == null) {
            rsnScriptForEach(mContext, id, slot, ain, aout);
        } else {
            rsnScriptForEach(mContext, id, slot, ain, aout, params);
        }
    }

    synchronized void nScriptForEachClipped(long id, int slot, long ain, long aout, byte[] params,
                                            int xstart, int xend, int ystart, int yend, int zstart, int zend) {
        validate();
        if (params == null) {
            rsnScriptForEachClipped(mContext, id, slot, ain, aout, xstart, xend, ystart, yend, zstart, zend);
        } else {
            rsnScriptForEachClipped(mContext, id, slot, ain, aout, params, xstart, xend, ystart, yend, zstart, zend);
        }
    }

    native void rsnScriptInvokeV(long con, long id, int slot, byte[] params);
    synchronized void nScriptInvokeV(long id, int slot, byte[] params) {
        validate();
        rsnScriptInvokeV(mContext, id, slot, params);
    }
    native void rsnScriptSetVarI(long con, long id, int slot, int val);
    synchronized void nScriptSetVarI(long id, int slot, int val) {
        validate();
        rsnScriptSetVarI(mContext, id, slot, val);
    }
    native void rsnScriptSetVarJ(long con, long id, int slot, long val);
    synchronized void nScriptSetVarJ(long id, int slot, long val) {
        validate();
        rsnScriptSetVarJ(mContext, id, slot, val);
    }
    native void rsnScriptSetVarF(long con, long id, int slot, float val);
    synchronized void nScriptSetVarF(long id, int slot, float val) {
        validate();
        rsnScriptSetVarF(mContext, id, slot, val);
    }
    native void rsnScriptSetVarD(long con, long id, int slot, double val);
    synchronized void nScriptSetVarD(long id, int slot, double val) {
        validate();
        rsnScriptSetVarD(mContext, id, slot, val);
    }
    native void rsnScriptSetVarV(long con, long id, int slot, byte[] val);
    synchronized void nScriptSetVarV(long id, int slot, byte[] val) {
        validate();
        rsnScriptSetVarV(mContext, id, slot, val);
    }
    native void rsnScriptSetVarVE(long con, long id, int slot, byte[] val,
                                  long e, int[] dims);
    synchronized void nScriptSetVarVE(long id, int slot, byte[] val,
                                      long e, int[] dims) {
        validate();
        rsnScriptSetVarVE(mContext, id, slot, val, e, dims);
    }
    native void rsnScriptSetVarObj(long con, long id, int slot, long val);
    synchronized void nScriptSetVarObj(long id, int slot, long val) {
        validate();
        rsnScriptSetVarObj(mContext, id, slot, val);
    }

    native long  rsnScriptCCreate(long con, String resName, String cacheDir,
                                 byte[] script, int length);
    synchronized long nScriptCCreate(String resName, String cacheDir, byte[] script, int length) {
        validate();
        return rsnScriptCCreate(mContext, resName, cacheDir, script, length);
    }

    native long  rsnScriptIntrinsicCreate(long con, int id, long eid);
    synchronized long nScriptIntrinsicCreate(int id, long eid) {
        validate();
        return rsnScriptIntrinsicCreate(mContext, id, eid);
    }

    native long  rsnScriptKernelIDCreate(long con, long sid, int slot, int sig);
    synchronized long nScriptKernelIDCreate(long sid, int slot, int sig) {
        validate();
        return rsnScriptKernelIDCreate(mContext, sid, slot, sig);
    }

    native long  rsnScriptInvokeIDCreate(long con, long sid, int slot);
    synchronized long nScriptInvokeIDCreate(long sid, int slot) {
        validate();
        return rsnScriptInvokeIDCreate(mContext, sid, slot);
    }

    native long  rsnScriptFieldIDCreate(long con, long sid, int slot);
    synchronized long nScriptFieldIDCreate(long sid, int slot) {
        validate();
        return rsnScriptFieldIDCreate(mContext, sid, slot);
    }

    native long  rsnScriptGroupCreate(long con, long[] kernels, long[] src, long[] dstk, long[] dstf, long[] types);
    synchronized long nScriptGroupCreate(long[] kernels, long[] src, long[] dstk, long[] dstf, long[] types) {
        validate();
        return rsnScriptGroupCreate(mContext, kernels, src, dstk, dstf, types);
    }

    native void rsnScriptGroupSetInput(long con, long group, long kernel, long alloc);
    synchronized void nScriptGroupSetInput(long group, long kernel, long alloc) {
        validate();
        rsnScriptGroupSetInput(mContext, group, kernel, alloc);
    }

    native void rsnScriptGroupSetOutput(long con, long group, long kernel, long alloc);
    synchronized void nScriptGroupSetOutput(long group, long kernel, long alloc) {
        validate();
        rsnScriptGroupSetOutput(mContext, group, kernel, alloc);
    }

    native void rsnScriptGroupExecute(long con, long group);
    synchronized void nScriptGroupExecute(long group) {
        validate();
        rsnScriptGroupExecute(mContext, group);
    }

    native long  rsnSamplerCreate(long con, int magFilter, int minFilter,
                                 int wrapS, int wrapT, int wrapR, float aniso);
    synchronized long nSamplerCreate(int magFilter, int minFilter,
                                 int wrapS, int wrapT, int wrapR, float aniso) {
        validate();
        return rsnSamplerCreate(mContext, magFilter, minFilter, wrapS, wrapT, wrapR, aniso);
    }




    long     mDev;
    long     mContext;
    ReentrantReadWriteLock mRWLock;
    @SuppressWarnings({"FieldCanBeLocal"})
    MessageThread mMessageThread;

    Element mElement_U8;
    Element mElement_I8;
    Element mElement_U16;
    Element mElement_I16;
    Element mElement_U32;
    Element mElement_I32;
    Element mElement_U64;
    Element mElement_I64;
    Element mElement_F32;
    Element mElement_F64;
    Element mElement_BOOLEAN;

    Element mElement_ELEMENT;
    Element mElement_TYPE;
    Element mElement_ALLOCATION;
    Element mElement_SAMPLER;
    Element mElement_SCRIPT;

    Element mElement_A_8;
    Element mElement_RGB_565;
    Element mElement_RGB_888;
    Element mElement_RGBA_5551;
    Element mElement_RGBA_4444;
    Element mElement_RGBA_8888;

    Element mElement_FLOAT_2;
    Element mElement_FLOAT_3;
    Element mElement_FLOAT_4;

    Element mElement_DOUBLE_2;
    Element mElement_DOUBLE_3;
    Element mElement_DOUBLE_4;

    Element mElement_UCHAR_2;
    Element mElement_UCHAR_3;
    Element mElement_UCHAR_4;

    Element mElement_CHAR_2;
    Element mElement_CHAR_3;
    Element mElement_CHAR_4;

    Element mElement_USHORT_2;
    Element mElement_USHORT_3;
    Element mElement_USHORT_4;

    Element mElement_SHORT_2;
    Element mElement_SHORT_3;
    Element mElement_SHORT_4;

    Element mElement_UINT_2;
    Element mElement_UINT_3;
    Element mElement_UINT_4;

    Element mElement_INT_2;
    Element mElement_INT_3;
    Element mElement_INT_4;

    Element mElement_ULONG_2;
    Element mElement_ULONG_3;
    Element mElement_ULONG_4;

    Element mElement_LONG_2;
    Element mElement_LONG_3;
    Element mElement_LONG_4;

    Element mElement_MATRIX_4X4;
    Element mElement_MATRIX_3X3;
    Element mElement_MATRIX_2X2;

    Sampler mSampler_CLAMP_NEAREST;
    Sampler mSampler_CLAMP_LINEAR;
    Sampler mSampler_CLAMP_LINEAR_MIP_LINEAR;
    Sampler mSampler_WRAP_NEAREST;
    Sampler mSampler_WRAP_LINEAR;
    Sampler mSampler_WRAP_LINEAR_MIP_LINEAR;
    Sampler mSampler_MIRRORED_REPEAT_NEAREST;
    Sampler mSampler_MIRRORED_REPEAT_LINEAR;
    Sampler mSampler_MIRRORED_REPEAT_LINEAR_MIP_LINEAR;


    ///////////////////////////////////////////////////////////////////////////////////
    //

    /**
     * The base class from which an application should derive in order
     * to receive RS messages from scripts. When a script calls {@code
     * rsSendToClient}, the data fields will be filled, and the run
     * method will be called on a separate thread.  This will occur
     * some time after {@code rsSendToClient} completes in the script,
     * as {@code rsSendToClient} is asynchronous. Message handlers are
     * not guaranteed to have completed when {@link
     * android.support.v8.renderscript.RenderScript#finish} returns.
     *
     */
    public static class RSMessageHandler implements Runnable {
        protected int[] mData;
        protected int mID;
        protected int mLength;
        public void run() {
        }
    }
    /**
     * If an application is expecting messages, it should set this
     * field to an instance of {@link RSMessageHandler}.  This
     * instance will receive all the user messages sent from {@code
     * sendToClient} by scripts from this context.
     *
     */
    RSMessageHandler mMessageCallback = null;

    public void setMessageHandler(RSMessageHandler msg) {
        mMessageCallback = msg;
    }
    public RSMessageHandler getMessageHandler() {
        return mMessageCallback;
    }

    /**
     * Place a message into the message queue to be sent back to the message
     * handler once all previous commands have been executed.
     *
     * @hide
     *
     * @param id
     * @param data
     */
    public void sendMessage(int id, int[] data) {
        nContextSendMessage(id, data);
    }

    /**
     * The runtime error handler base class.  An application should derive from this class
     * if it wishes to install an error handler.  When errors occur at runtime,
     * the fields in this class will be filled, and the run method will be called.
     *
     */
    public static class RSErrorHandler implements Runnable {
        protected String mErrorMessage;
        protected int mErrorNum;
        public void run() {
        }
    }

    /**
     * Application Error handler.  All runtime errors will be dispatched to the
     * instance of RSAsyncError set here.  If this field is null a
     * {@link RSRuntimeException} will instead be thrown with details about the error.
     * This will cause program termaination.
     *
     */
    RSErrorHandler mErrorCallback = null;

    public void setErrorHandler(RSErrorHandler msg) {
        mErrorCallback = msg;
    }
    public RSErrorHandler getErrorHandler() {
        return mErrorCallback;
    }

    /**
     * RenderScript worker thread priority enumeration.  The default value is
     * NORMAL.  Applications wishing to do background processing should set
     * their priority to LOW to avoid starving forground processes.
     */
    public enum Priority {
        LOW (Process.THREAD_PRIORITY_BACKGROUND + (5 * Process.THREAD_PRIORITY_LESS_FAVORABLE)),
        NORMAL (Process.THREAD_PRIORITY_DISPLAY);

        int mID;
        Priority(int id) {
            mID = id;
        }
    }

    void validate() {
        if (mContext == 0) {
            throw new RSInvalidStateException("Calling RS with no Context active.");
        }
    }

    /**
     * check if IO support lib is available.
     */
    boolean usingIO() {
        return useIOlib;
    }
    /**
     * Change the priority of the worker threads for this context.
     *
     * @param p New priority to be set.
     */
    public void setPriority(Priority p) {
        validate();
        nContextSetPriority(p.mID);
    }

    static class MessageThread extends Thread {
        RenderScript mRS;
        boolean mRun = true;
        int[] mAuxData = new int[2];

        static final int RS_MESSAGE_TO_CLIENT_NONE = 0;
        static final int RS_MESSAGE_TO_CLIENT_EXCEPTION = 1;
        static final int RS_MESSAGE_TO_CLIENT_RESIZE = 2;
        static final int RS_MESSAGE_TO_CLIENT_ERROR = 3;
        static final int RS_MESSAGE_TO_CLIENT_USER = 4;

        static final int RS_ERROR_FATAL_UNKNOWN = 0x1000;

        MessageThread(RenderScript rs) {
            super("RSMessageThread");
            mRS = rs;

        }

        public void run() {
            // This function is a temporary solution.  The final solution will
            // used typed allocations where the message id is the type indicator.
            int[] rbuf = new int[16];
            mRS.nContextInitToClient(mRS.mContext);
            while(mRun) {
                rbuf[0] = 0;
                int msg = mRS.nContextPeekMessage(mRS.mContext, mAuxData);
                int size = mAuxData[1];
                int subID = mAuxData[0];

                if (msg == RS_MESSAGE_TO_CLIENT_USER) {
                    if ((size>>2) >= rbuf.length) {
                        rbuf = new int[(size + 3) >> 2];
                    }
                    if (mRS.nContextGetUserMessage(mRS.mContext, rbuf) !=
                        RS_MESSAGE_TO_CLIENT_USER) {
                        throw new RSDriverException("Error processing message from RenderScript.");
                    }

                    if(mRS.mMessageCallback != null) {
                        mRS.mMessageCallback.mData = rbuf;
                        mRS.mMessageCallback.mID = subID;
                        mRS.mMessageCallback.mLength = size;
                        mRS.mMessageCallback.run();
                    } else {
                        throw new RSInvalidStateException("Received a message from the script with no message handler installed.");
                    }
                    continue;
                }

                if (msg == RS_MESSAGE_TO_CLIENT_ERROR) {
                    String e = mRS.nContextGetErrorMessage(mRS.mContext);

                    if (subID >= RS_ERROR_FATAL_UNKNOWN) {
                        throw new RSRuntimeException("Fatal error " + subID + ", details: " + e);
                    }

                    if(mRS.mErrorCallback != null) {
                        mRS.mErrorCallback.mErrorMessage = e;
                        mRS.mErrorCallback.mErrorNum = subID;
                        mRS.mErrorCallback.run();
                    } else {
                        android.util.Log.e(LOG_TAG, "non fatal RS error, " + e);
                        // Do not throw here. In these cases, we do not have
                        // a fatal error.
                    }
                    continue;
                }

                // 2: teardown.
                // But we want to avoid starving other threads during
                // teardown by yielding until the next line in the destructor
                // can execute to set mRun = false
                try {
                    sleep(1, 0);
                } catch(InterruptedException e) {
                }
            }
            //Log.d(LOG_TAG, "MessageThread exiting.");
        }
    }

    RenderScript(Context ctx) {
        if (ctx != null) {
            mApplicationContext = ctx.getApplicationContext();
            mNativeLibDir = mApplicationContext.getApplicationInfo().nativeLibraryDir;
        }
        mRWLock = new ReentrantReadWriteLock();
    }

    /**
     * Gets the application context associated with the RenderScript context.
     *
     * @return The application context.
     */
    public final Context getApplicationContext() {
        return mApplicationContext;
    }

    /**
     * @hide
     */
    public static RenderScript create(Context ctx, int sdkVersion) {
        return create(ctx, sdkVersion, ContextType.NORMAL);
    }

    /**
     * Create a RenderScript context.
     *
     * @hide
     * @param ctx The context.
     * @return RenderScript
     */
    public static RenderScript create(Context ctx, int sdkVersion, ContextType ct) {
        RenderScript rs = new RenderScript(ctx);

        if (sSdkVersion == -1) {
            sSdkVersion = sdkVersion;
        } else if (sSdkVersion != sdkVersion) {
            throw new RSRuntimeException("Can't have two contexts with different SDK versions in support lib");
        }
        boolean useNative = setupNative(sSdkVersion, ctx);
        synchronized(lock) {
            if (sInitialized == false) {
                try {
                    Class<?> vm_runtime = Class.forName("dalvik.system.VMRuntime");
                    Method get_runtime = vm_runtime.getDeclaredMethod("getRuntime");
                    sRuntime = get_runtime.invoke(null);
                    registerNativeAllocation = vm_runtime.getDeclaredMethod("registerNativeAllocation", Integer.TYPE);
                    registerNativeFree = vm_runtime.getDeclaredMethod("registerNativeFree", Integer.TYPE);
                    sUseGCHooks = true;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "No GC methods");
                    sUseGCHooks = false;
                }
                try {
                    System.loadLibrary("rsjni");
                    sInitialized = true;
                    sPointerSize = rsnSystemGetPointerSize();
                } catch (UnsatisfiedLinkError e) {
                    Log.e(LOG_TAG, "Error loading RS jni library: " + e);
                    throw new RSRuntimeException("Error loading RS jni library: " + e);
                }
            }
        }

        if (useNative) {
            android.util.Log.v(LOG_TAG, "RS native mode");
        } else {
            android.util.Log.v(LOG_TAG, "RS compat mode");
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            useIOlib = true;
        }
        if (!rs.nLoadSO(useNative)) {
            if (useNative) {
                android.util.Log.v(LOG_TAG, "Unable to load libRS.so, falling back to compat mode");
            }
            try {
                System.loadLibrary("RSSupport");
            } catch (UnsatisfiedLinkError e) {
                Log.e(LOG_TAG, "Error loading RS Compat library: " + e);
                throw new RSRuntimeException("Error loading RS Compat library: " + e);
            }
            if (!rs.nLoadSO(false)) {
                throw new RSRuntimeException("Error loading libRSSupport library");
            }
        }

        if (useIOlib) {
            try {
                System.loadLibrary("RSSupportIO");
            } catch (UnsatisfiedLinkError e) {
                useIOlib = false;
            }
            if (!useIOlib || !rs.nLoadIOSO()) {
                android.util.Log.v(LOG_TAG, "Unable to load libRSSupportIO.so, USAGE_IO not supported");
                useIOlib = false;
            }
        }

        rs.mDev = rs.nDeviceCreate();
        rs.mContext = rs.nContextCreate(rs.mDev, 0, sdkVersion, ct.mID, rs.mNativeLibDir);
        if (rs.mContext == 0) {
            throw new RSDriverException("Failed to create RS context.");
        }
        rs.mMessageThread = new MessageThread(rs);
        rs.mMessageThread.start();
        return rs;
    }

    /**
     * Create a RenderScript context.
     *
     * @param ctx The context.
     * @return RenderScript
     */
    public static RenderScript create(Context ctx) {
        return create(ctx, ContextType.NORMAL);
    }

    /**
     * Create a RenderScript context.
     *
     * @hide
     *
     * @param ctx The context.
     * @param ct The type of context to be created.
     * @return RenderScript
     */
    public static RenderScript create(Context ctx, ContextType ct) {
        int v = ctx.getApplicationInfo().targetSdkVersion;
        return create(ctx, v, ct);
    }

    /**
     * Print the currently available debugging information about the state of
     * the RS context to the log.
     *
     */
    public void contextDump() {
        validate();
        nContextDump(0);
    }

    /**
     * Wait for any pending asynchronous opeations (such as copies to a RS
     * allocation or RS script executions) to complete.
     *
     */
    public void finish() {
        nContextFinish();
    }

    /**
     * Destroys this RenderScript context.  Once this function is called,
     * using this context or any objects belonging to this context is
     * illegal.
     *
     */
    public void destroy() {
        validate();
        nContextFinish();
        nContextDeinitToClient(mContext);
        mMessageThread.mRun = false;
        try {
            mMessageThread.join();
        } catch(InterruptedException e) {
        }

        nContextDestroy();
        nDeviceDestroy(mDev);
        mDev = 0;
    }

    boolean isAlive() {
        return mContext != 0;
    }

    long safeID(BaseObj o) {
        if(o != null) {
            return o.getID(this);
        }
        return 0;
    }
}
