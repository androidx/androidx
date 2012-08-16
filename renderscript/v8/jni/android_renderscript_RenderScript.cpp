/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

#define LOG_TAG "libRS_jni"

#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <math.h>
#include <utils/misc.h>
#include <android/bitmap.h>
#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include <rs.h>
#include <rsEnv.h>

//#define LOG_API ALOG
#define LOG_API(...)

using namespace android;

class AutoJavaStringToUTF8 {
public:
    AutoJavaStringToUTF8(JNIEnv* env, jstring str) : fEnv(env), fJStr(str) {
        fCStr = env->GetStringUTFChars(str, NULL);
        fLength = env->GetStringUTFLength(str);
    }
    ~AutoJavaStringToUTF8() {
        fEnv->ReleaseStringUTFChars(fJStr, fCStr);
    }
    const char* c_str() const { return fCStr; }
    jsize length() const { return fLength; }

private:
    JNIEnv*     fEnv;
    jstring     fJStr;
    const char* fCStr;
    jsize       fLength;
};

class AutoJavaStringArrayToUTF8 {
public:
    AutoJavaStringArrayToUTF8(JNIEnv* env, jobjectArray strings, jsize stringsLength)
    : mEnv(env), mStrings(strings), mStringsLength(stringsLength) {
        mCStrings = NULL;
        mSizeArray = NULL;
        if (stringsLength > 0) {
            mCStrings = (const char **)calloc(stringsLength, sizeof(char *));
            mSizeArray = (size_t*)calloc(stringsLength, sizeof(size_t));
            for (jsize ct = 0; ct < stringsLength; ct ++) {
                jstring s = (jstring)mEnv->GetObjectArrayElement(mStrings, ct);
                mCStrings[ct] = mEnv->GetStringUTFChars(s, NULL);
                mSizeArray[ct] = mEnv->GetStringUTFLength(s);
            }
        }
    }
    ~AutoJavaStringArrayToUTF8() {
        for (jsize ct=0; ct < mStringsLength; ct++) {
            jstring s = (jstring)mEnv->GetObjectArrayElement(mStrings, ct);
            mEnv->ReleaseStringUTFChars(s, mCStrings[ct]);
        }
        free(mCStrings);
        free(mSizeArray);
    }
    const char **c_str() const { return mCStrings; }
    size_t *c_str_len() const { return mSizeArray; }
    jsize length() const { return mStringsLength; }

private:
    JNIEnv      *mEnv;
    jobjectArray mStrings;
    const char **mCStrings;
    size_t      *mSizeArray;
    jsize        mStringsLength;
};

// ---------------------------------------------------------------------------

static jfieldID gContextId = 0;
static jfieldID gNativeBitmapID = 0;
static jfieldID gTypeNativeCache = 0;

static void _nInit(JNIEnv *_env, jclass _this)
{
    gContextId             = _env->GetFieldID(_this, "mContext", "I");

    jclass bitmapClass = _env->FindClass("android/graphics/Bitmap");
    gNativeBitmapID = _env->GetFieldID(bitmapClass, "mNativeBitmap", "I");
}

// ---------------------------------------------------------------------------

static void
nContextFinish(JNIEnv *_env, jobject _this, RsContext con)
{
    LOG_API("nContextFinish, con(%p)", con);
    rsContextFinish(con);
}

static void
nObjDestroy(JNIEnv *_env, jobject _this, RsContext con, jint obj)
{
    LOG_API("nObjDestroy, con(%p) obj(%p)", con, (void *)obj);
    rsObjDestroy(con, (void *)obj);
}

// ---------------------------------------------------------------------------

static jint
nDeviceCreate(JNIEnv *_env, jobject _this)
{
    LOG_API("nDeviceCreate");
    return (jint)rsDeviceCreate();
}

static void
nDeviceDestroy(JNIEnv *_env, jobject _this, jint dev)
{
    LOG_API("nDeviceDestroy");
    return rsDeviceDestroy((RsDevice)dev);
}

static void
nDeviceSetConfig(JNIEnv *_env, jobject _this, jint dev, jint p, jint value)
{
    LOG_API("nDeviceSetConfig  dev(%p), param(%i), value(%i)", (void *)dev, p, value);
    return rsDeviceSetConfig((RsDevice)dev, (RsDeviceParam)p, value);
}

static jint
nContextCreate(JNIEnv *_env, jobject _this, jint dev, jint ver, jint sdkVer)
{
    LOG_API("nContextCreate");
    return (jint)rsContextCreate((RsDevice)dev, ver, sdkVer);
}


static void
nContextSetPriority(JNIEnv *_env, jobject _this, RsContext con, jint p)
{
    LOG_API("ContextSetPriority, con(%p), priority(%i)", con, p);
    rsContextSetPriority(con, p);
}



static void
nContextDestroy(JNIEnv *_env, jobject _this, RsContext con)
{
    LOG_API("nContextDestroy, con(%p)", con);
    rsContextDestroy(con);
}

static void
nContextDump(JNIEnv *_env, jobject _this, RsContext con, jint bits)
{
    LOG_API("nContextDump, con(%p)  bits(%i)", (RsContext)con, bits);
    rsContextDump((RsContext)con, bits);
}


static jstring
nContextGetErrorMessage(JNIEnv *_env, jobject _this, RsContext con)
{
    LOG_API("nContextGetErrorMessage, con(%p)", con);
    char buf[1024];

    size_t receiveLen;
    uint32_t subID;
    int id = rsContextGetMessage(con,
                                 buf, sizeof(buf),
                                 &receiveLen, sizeof(receiveLen),
                                 &subID, sizeof(subID));
    if (!id && receiveLen) {
        ALOGV("message receive buffer too small.  %i", receiveLen);
    }
    return _env->NewStringUTF(buf);
}

static jint
nContextGetUserMessage(JNIEnv *_env, jobject _this, RsContext con, jintArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nContextGetMessage, con(%p), len(%i)", con, len);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    size_t receiveLen;
    uint32_t subID;
    int id = rsContextGetMessage(con,
                                 ptr, len * 4,
                                 &receiveLen, sizeof(receiveLen),
                                 &subID, sizeof(subID));
    if (!id && receiveLen) {
        ALOGV("message receive buffer too small.  %i", receiveLen);
    }
    _env->ReleaseIntArrayElements(data, ptr, 0);
    return id;
}

static jint
nContextPeekMessage(JNIEnv *_env, jobject _this, RsContext con, jintArray auxData)
{
    LOG_API("nContextPeekMessage, con(%p)", con);
    jint *auxDataPtr = _env->GetIntArrayElements(auxData, NULL);
    size_t receiveLen;
    uint32_t subID;
    int id = rsContextPeekMessage(con, &receiveLen, sizeof(receiveLen),
                                  &subID, sizeof(subID));
    auxDataPtr[0] = (jint)subID;
    auxDataPtr[1] = (jint)receiveLen;
    _env->ReleaseIntArrayElements(auxData, auxDataPtr, 0);
    return id;
}

static void nContextInitToClient(JNIEnv *_env, jobject _this, RsContext con)
{
    LOG_API("nContextInitToClient, con(%p)", con);
    rsContextInitToClient(con);
}

static void nContextDeinitToClient(JNIEnv *_env, jobject _this, RsContext con)
{
    LOG_API("nContextDeinitToClient, con(%p)", con);
    rsContextDeinitToClient(con);
}


static jint
nElementCreate(JNIEnv *_env, jobject _this, RsContext con, jint type, jint kind, jboolean norm, jint size)
{
    LOG_API("nElementCreate, con(%p), type(%i), kind(%i), norm(%i), size(%i)", con, type, kind, norm, size);
    return (jint)rsElementCreate(con, (RsDataType)type, (RsDataKind)kind, norm, size);
}

static jint
nElementCreate2(JNIEnv *_env, jobject _this, RsContext con,
                jintArray _ids, jobjectArray _names, jintArray _arraySizes)
{
    int fieldCount = _env->GetArrayLength(_ids);
    LOG_API("nElementCreate2, con(%p)", con);

    jint *ids = _env->GetIntArrayElements(_ids, NULL);
    jint *arraySizes = _env->GetIntArrayElements(_arraySizes, NULL);

    AutoJavaStringArrayToUTF8 names(_env, _names, fieldCount);

    const char **nameArray = names.c_str();
    size_t *sizeArray = names.c_str_len();

    jint id = (jint)rsElementCreate2(con,
                                     (RsElement *)ids, fieldCount,
                                     nameArray, fieldCount * sizeof(size_t),  sizeArray,
                                     (const uint32_t *)arraySizes, fieldCount);

    _env->ReleaseIntArrayElements(_ids, ids, JNI_ABORT);
    _env->ReleaseIntArrayElements(_arraySizes, arraySizes, JNI_ABORT);
    return (jint)id;
}



static void
nElementGetSubElements(JNIEnv *_env, jobject _this, RsContext con, jint id,
                       jintArray _IDs,
                       jobjectArray _names,
                       jintArray _arraySizes)
{
    int dataSize = _env->GetArrayLength(_IDs);
    LOG_API("nElementGetSubElements, con(%p)", con);

    uint32_t *ids = (uint32_t *)malloc((uint32_t)dataSize * sizeof(uint32_t));
    const char **names = (const char **)malloc((uint32_t)dataSize * sizeof(const char *));
    uint32_t *arraySizes = (uint32_t *)malloc((uint32_t)dataSize * sizeof(uint32_t));

    rsaElementGetSubElements(con, (RsElement)id, ids, names, arraySizes, (uint32_t)dataSize);

    for(jint i = 0; i < dataSize; i++) {
        _env->SetObjectArrayElement(_names, i, _env->NewStringUTF(names[i]));
        _env->SetIntArrayRegion(_IDs, i, 1, (const jint*)&ids[i]);
        _env->SetIntArrayRegion(_arraySizes, i, 1, (const jint*)&arraySizes[i]);
    }

    free(ids);
    free(names);
    free(arraySizes);
}

// -----------------------------------

static int
nTypeCreate(JNIEnv *_env, jobject _this, RsContext con, RsElement eid,
            jint dimx, jint dimy, jint dimz, jboolean mips, jboolean faces)
{
    LOG_API("nTypeCreate, con(%p) eid(%p), x(%i), y(%i), z(%i), mips(%i), faces(%i)",
            con, eid, dimx, dimy, dimz, mips, faces);

    jint id = (jint)rsTypeCreate(con, (RsElement)eid, dimx, dimy, dimz, mips, faces);
    return (jint)id;
}

// -----------------------------------

static jint
nAllocationCreateTyped(JNIEnv *_env, jobject _this, RsContext con, jint type, jint mips, jint usage, jint pointer)
{
    LOG_API("nAllocationCreateTyped, con(%p), type(%p), mip(%i), usage(%i), ptr(%p)", con, (RsElement)type, mips, usage, (void *)pointer);
    return (jint) rsAllocationCreateTyped(con, (RsType)type, (RsAllocationMipmapControl)mips, (uint32_t)usage, (uint32_t)pointer);
}

static void
nAllocationSyncAll(JNIEnv *_env, jobject _this, RsContext con, jint a, jint bits)
{
    LOG_API("nAllocationSyncAll, con(%p), a(%p), bits(0x%08x)", con, (RsAllocation)a, bits);
    rsAllocationSyncAll(con, (RsAllocation)a, (RsAllocationUsageType)bits);
}

static void
nAllocationGenerateMipmaps(JNIEnv *_env, jobject _this, RsContext con, jint alloc)
{
    LOG_API("nAllocationGenerateMipmaps, con(%p), a(%p)", con, (RsAllocation)alloc);
    rsAllocationGenerateMipmaps(con, (RsAllocation)alloc);
}

static size_t GetBitmapSize(JNIEnv *env, jobject jbitmap) {
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(env, jbitmap, &info);
    size_t s = info.width * info.height;
    switch (info.format) {
        case ANDROID_BITMAP_FORMAT_RGBA_8888: s *= 4; break;
        case ANDROID_BITMAP_FORMAT_RGB_565: s *= 2; break;
        case ANDROID_BITMAP_FORMAT_RGBA_4444: s *= 2; break;
    }
    return s;
}

static int
nAllocationCreateFromBitmap(JNIEnv *_env, jobject _this, RsContext con, jint type, jint mip, jobject jbitmap, jint usage)
{
    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    jint id = 0;
    if (pixels != NULL) {
        id = (jint)rsAllocationCreateFromBitmap(con, (RsType)type,
                                                (RsAllocationMipmapControl)mip, pixels,
                                                GetBitmapSize(_env, jbitmap), usage);
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
    return id;
}

static int
nAllocationCubeCreateFromBitmap(JNIEnv *_env, jobject _this, RsContext con, jint type, jint mip, jobject jbitmap, jint usage)
{
    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    jint id = 0;
    if (pixels != NULL) {
        id = (jint)rsAllocationCubeCreateFromBitmap(con,
                                                    (RsType)type, (RsAllocationMipmapControl)mip,
                                                    pixels, GetBitmapSize(_env, jbitmap), usage);
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
    return id;
}

static void
nAllocationCopyFromBitmap(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jobject jbitmap)
{
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(_env, jbitmap, &info);

    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    if (pixels != NULL) {
        rsAllocation2DData(con, (RsAllocation)alloc, 0, 0,
                           0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X,
                           info.width, info.height, pixels, GetBitmapSize(_env, jbitmap));
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
}

static void
nAllocationCopyToBitmap(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jobject jbitmap)
{
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(_env, jbitmap, &info);

    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    if (pixels != NULL) {
        rsAllocationCopyToBitmap(con, (RsAllocation)alloc, pixels, GetBitmapSize(_env, jbitmap));
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
    //bitmap.notifyPixelsChanged();
}


static void
nAllocationData1D_i(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint offset, jint lod, jint count, jintArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_i, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)", con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    rsAllocation1DData(con, (RsAllocation)alloc, offset, lod, count, ptr, sizeBytes);
    _env->ReleaseIntArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData1D_s(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint offset, jint lod, jint count, jshortArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_s, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)", con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jshort *ptr = _env->GetShortArrayElements(data, NULL);
    rsAllocation1DData(con, (RsAllocation)alloc, offset, lod, count, ptr, sizeBytes);
    _env->ReleaseShortArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData1D_b(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint offset, jint lod, jint count, jbyteArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_b, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)", con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    rsAllocation1DData(con, (RsAllocation)alloc, offset, lod, count, ptr, sizeBytes);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData1D_f(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint offset, jint lod, jint count, jfloatArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_f, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)", con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jfloat *ptr = _env->GetFloatArrayElements(data, NULL);
    rsAllocation1DData(con, (RsAllocation)alloc, offset, lod, count, ptr, sizeBytes);
    _env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
}

static void
//    native void rsnAllocationElementData1D(int con, int id, int xoff, int compIdx, byte[] d, int sizeBytes);
nAllocationElementData1D(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint offset, jint lod, jint compIdx, jbyteArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationElementData1D, con(%p), alloc(%p), offset(%i), comp(%i), len(%i), sizeBytes(%i)", con, (RsAllocation)alloc, offset, compIdx, len, sizeBytes);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    rsAllocation1DElementData(con, (RsAllocation)alloc, offset, lod, ptr, sizeBytes, compIdx);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_s(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint xoff, jint yoff, jint lod, jint face,
                    jint w, jint h, jshortArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_s, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)", con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jshort *ptr = _env->GetShortArrayElements(data, NULL);
    rsAllocation2DData(con, (RsAllocation)alloc, xoff, yoff, lod, (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes);
    _env->ReleaseShortArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_b(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint xoff, jint yoff, jint lod, jint face,
                    jint w, jint h, jbyteArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_b, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)", con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    rsAllocation2DData(con, (RsAllocation)alloc, xoff, yoff, lod, (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_i(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint xoff, jint yoff, jint lod, jint face,
                    jint w, jint h, jintArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_i, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)", con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    rsAllocation2DData(con, (RsAllocation)alloc, xoff, yoff, lod, (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes);
    _env->ReleaseIntArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_f(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint xoff, jint yoff, jint lod, jint face,
                    jint w, jint h, jfloatArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_i, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)", con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jfloat *ptr = _env->GetFloatArrayElements(data, NULL);
    rsAllocation2DData(con, (RsAllocation)alloc, xoff, yoff, lod, (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes);
    _env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_alloc(JNIEnv *_env, jobject _this, RsContext con,
                        jint dstAlloc, jint dstXoff, jint dstYoff,
                        jint dstMip, jint dstFace,
                        jint width, jint height,
                        jint srcAlloc, jint srcXoff, jint srcYoff,
                        jint srcMip, jint srcFace)
{
    LOG_API("nAllocation2DData_s, con(%p), dstAlloc(%p), dstXoff(%i), dstYoff(%i),"
            " dstMip(%i), dstFace(%i), width(%i), height(%i),"
            " srcAlloc(%p), srcXoff(%i), srcYoff(%i), srcMip(%i), srcFace(%i)",
            con, (RsAllocation)dstAlloc, dstXoff, dstYoff, dstMip, dstFace,
            width, height, (RsAllocation)srcAlloc, srcXoff, srcYoff, srcMip, srcFace);

    rsAllocationCopy2DRange(con,
                            (RsAllocation)dstAlloc,
                            dstXoff, dstYoff,
                            dstMip, dstFace,
                            width, height,
                            (RsAllocation)srcAlloc,
                            srcXoff, srcYoff,
                            srcMip, srcFace);
}

static void
nAllocationRead_i(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jintArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_i, con(%p), alloc(%p), len(%i)", con, (RsAllocation)alloc, len);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    rsAllocationRead(con, (RsAllocation)alloc, ptr, length * sizeof(int));
    _env->ReleaseIntArrayElements(data, ptr, 0);
}

static void
nAllocationRead_s(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jshortArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_i, con(%p), alloc(%p), len(%i)", con, (RsAllocation)alloc, len);
    jshort *ptr = _env->GetShortArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    rsAllocationRead(con, (RsAllocation)alloc, ptr, length * sizeof(short));
    _env->ReleaseShortArrayElements(data, ptr, 0);
}

static void
nAllocationRead_b(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jbyteArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_i, con(%p), alloc(%p), len(%i)", con, (RsAllocation)alloc, len);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    rsAllocationRead(con, (RsAllocation)alloc, ptr, length * sizeof(char));
    _env->ReleaseByteArrayElements(data, ptr, 0);
}

static void
nAllocationRead_f(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jfloatArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_f, con(%p), alloc(%p), len(%i)", con, (RsAllocation)alloc, len);
    jfloat *ptr = _env->GetFloatArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    rsAllocationRead(con, (RsAllocation)alloc, ptr, length * sizeof(float));
    _env->ReleaseFloatArrayElements(data, ptr, 0);
}

static jint
nAllocationGetType(JNIEnv *_env, jobject _this, RsContext con, jint a)
{
    LOG_API("nAllocationGetType, con(%p), a(%p)", con, (RsAllocation)a);
    return (jint) rsaAllocationGetType(con, (RsAllocation)a);
}

static void
nAllocationResize1D(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint dimX)
{
    LOG_API("nAllocationResize1D, con(%p), alloc(%p), sizeX(%i)", con, (RsAllocation)alloc, dimX);
    rsAllocationResize1D(con, (RsAllocation)alloc, dimX);
}

static void
nAllocationResize2D(JNIEnv *_env, jobject _this, RsContext con, jint alloc, jint dimX, jint dimY)
{
    LOG_API("nAllocationResize1D, con(%p), alloc(%p), sizeX(%i), sizeY(%i)", con, (RsAllocation)alloc, dimX, dimY);
    rsAllocationResize2D(con, (RsAllocation)alloc, dimX, dimY);
}

// -----------------------------------

static void
nScriptBindAllocation(JNIEnv *_env, jobject _this, RsContext con, jint script, jint alloc, jint slot)
{
    LOG_API("nScriptBindAllocation, con(%p), script(%p), alloc(%p), slot(%i)", con, (RsScript)script, (RsAllocation)alloc, slot);
    rsScriptBindAllocation(con, (RsScript)script, (RsAllocation)alloc, slot);
}

static void
nScriptSetVarI(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, jint val)
{
    LOG_API("nScriptSetVarI, con(%p), s(%p), slot(%i), val(%i)", con, (void *)script, slot, val);
    rsScriptSetVarI(con, (RsScript)script, slot, val);
}

static void
nScriptSetVarObj(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, jint val)
{
    LOG_API("nScriptSetVarObj, con(%p), s(%p), slot(%i), val(%i)", con, (void *)script, slot, val);
    rsScriptSetVarObj(con, (RsScript)script, slot, (RsObjectBase)val);
}

static void
nScriptSetVarJ(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, jlong val)
{
    LOG_API("nScriptSetVarJ, con(%p), s(%p), slot(%i), val(%lli)", con, (void *)script, slot, val);
    rsScriptSetVarJ(con, (RsScript)script, slot, val);
}

static void
nScriptSetVarF(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, float val)
{
    LOG_API("nScriptSetVarF, con(%p), s(%p), slot(%i), val(%f)", con, (void *)script, slot, val);
    rsScriptSetVarF(con, (RsScript)script, slot, val);
}

static void
nScriptSetVarD(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, double val)
{
    LOG_API("nScriptSetVarD, con(%p), s(%p), slot(%i), val(%lf)", con, (void *)script, slot, val);
    rsScriptSetVarD(con, (RsScript)script, slot, val);
}

static void
nScriptSetVarV(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, jbyteArray data)
{
    LOG_API("nScriptSetVarV, con(%p), s(%p), slot(%i)", con, (void *)script, slot);
    jint len = _env->GetArrayLength(data);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    rsScriptSetVarV(con, (RsScript)script, slot, ptr, len);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nScriptSetVarVE(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, jbyteArray data, jint elem, jintArray dims)
{
    LOG_API("nScriptSetVarVE, con(%p), s(%p), slot(%i)", con, (void *)script, slot);
    jint len = _env->GetArrayLength(data);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    jint dimsLen = _env->GetArrayLength(dims) * sizeof(int);
    jint *dimsPtr = _env->GetIntArrayElements(dims, NULL);
    rsScriptSetVarVE(con, (RsScript)script, slot, ptr, len, (RsElement)elem,
                     (const size_t*) dimsPtr, dimsLen);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
    _env->ReleaseIntArrayElements(dims, dimsPtr, JNI_ABORT);
}


static void
nScriptSetTimeZone(JNIEnv *_env, jobject _this, RsContext con, jint script, jbyteArray timeZone)
{
    LOG_API("nScriptCSetTimeZone, con(%p), s(%p), timeZone(%s)", con, (void *)script, (const char *)timeZone);

    jint length = _env->GetArrayLength(timeZone);
    jbyte* timeZone_ptr;
    timeZone_ptr = (jbyte *) _env->GetPrimitiveArrayCritical(timeZone, (jboolean *)0);

    rsScriptSetTimeZone(con, (RsScript)script, (const char *)timeZone_ptr, length);

    if (timeZone_ptr) {
        _env->ReleasePrimitiveArrayCritical(timeZone, timeZone_ptr, 0);
    }
}

static void
nScriptInvoke(JNIEnv *_env, jobject _this, RsContext con, jint obj, jint slot)
{
    LOG_API("nScriptInvoke, con(%p), script(%p)", con, (void *)obj);
    rsScriptInvoke(con, (RsScript)obj, slot);
}

static void
nScriptInvokeV(JNIEnv *_env, jobject _this, RsContext con, jint script, jint slot, jbyteArray data)
{
    LOG_API("nScriptInvokeV, con(%p), s(%p), slot(%i)", con, (void *)script, slot);
    jint len = _env->GetArrayLength(data);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    rsScriptInvokeV(con, (RsScript)script, slot, ptr, len);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nScriptForEach(JNIEnv *_env, jobject _this, RsContext con,
               jint script, jint slot, jint ain, jint aout)
{
    LOG_API("nScriptForEach, con(%p), s(%p), slot(%i)", con, (void *)script, slot);
    rsScriptForEach(con, (RsScript)script, slot, (RsAllocation)ain, (RsAllocation)aout, NULL, 0);
}
static void
nScriptForEachV(JNIEnv *_env, jobject _this, RsContext con,
                jint script, jint slot, jint ain, jint aout, jbyteArray params)
{
    LOG_API("nScriptForEach, con(%p), s(%p), slot(%i)", con, (void *)script, slot);
    jint len = _env->GetArrayLength(params);
    jbyte *ptr = _env->GetByteArrayElements(params, NULL);
    rsScriptForEach(con, (RsScript)script, slot, (RsAllocation)ain, (RsAllocation)aout, ptr, len);
    _env->ReleaseByteArrayElements(params, ptr, JNI_ABORT);
}


// -----------------------------------

static jint
nScriptCCreate(JNIEnv *_env, jobject _this, RsContext con,
               jstring resName, jstring cacheDir,
               jbyteArray scriptRef, jint length)
{
    LOG_API("nScriptCCreate, con(%p)", con);

    AutoJavaStringToUTF8 resNameUTF(_env, resName);
    AutoJavaStringToUTF8 cacheDirUTF(_env, cacheDir);
    jint ret = 0;
    jbyte* script_ptr = NULL;
    jint _exception = 0;
    jint remaining;
    if (!scriptRef) {
        _exception = 1;
        //jniThrowException(_env, "java/lang/IllegalArgumentException", "script == null");
        goto exit;
    }
    if (length < 0) {
        _exception = 1;
        //jniThrowException(_env, "java/lang/IllegalArgumentException", "length < 0");
        goto exit;
    }
    remaining = _env->GetArrayLength(scriptRef);
    if (remaining < length) {
        _exception = 1;
        //jniThrowException(_env, "java/lang/IllegalArgumentException",
        //        "length > script.length - offset");
        goto exit;
    }
    script_ptr = (jbyte *)
        _env->GetPrimitiveArrayCritical(scriptRef, (jboolean *)0);

    //rsScriptCSetText(con, (const char *)script_ptr, length);

    ret = (jint)rsScriptCCreate(con,
                                resNameUTF.c_str(), resNameUTF.length(),
                                cacheDirUTF.c_str(), cacheDirUTF.length(),
                                (const char *)script_ptr, length);

exit:
    if (script_ptr) {
        _env->ReleasePrimitiveArrayCritical(scriptRef, script_ptr,
                _exception ? JNI_ABORT: 0);
    }

    return ret;
}

// ---------------------------------------------------------------------------

static jint
nSamplerCreate(JNIEnv *_env, jobject _this, RsContext con, jint magFilter, jint minFilter,
               jint wrapS, jint wrapT, jint wrapR, jfloat aniso)
{
    LOG_API("nSamplerCreate, con(%p)", con);
    return (jint)rsSamplerCreate(con,
                                 (RsSamplerValue)magFilter,
                                 (RsSamplerValue)minFilter,
                                 (RsSamplerValue)wrapS,
                                 (RsSamplerValue)wrapT,
                                 (RsSamplerValue)wrapR,
                                 aniso);
}

// ---------------------------------------------------------------------------


static const char *classPathName = "android/support/v8/renderscript/RenderScript";

static JNINativeMethod methods[] = {
{"_nInit",                         "()V",                                     (void*)_nInit },

{"nDeviceCreate",                  "()I",                                     (void*)nDeviceCreate },
{"nDeviceDestroy",                 "(I)V",                                    (void*)nDeviceDestroy },
{"nDeviceSetConfig",               "(III)V",                                  (void*)nDeviceSetConfig },
{"nContextGetUserMessage",         "(I[I)I",                                  (void*)nContextGetUserMessage },
{"nContextGetErrorMessage",        "(I)Ljava/lang/String;",                   (void*)nContextGetErrorMessage },
{"nContextPeekMessage",            "(I[I)I",                                  (void*)nContextPeekMessage },

{"nContextInitToClient",           "(I)V",                                    (void*)nContextInitToClient },
{"nContextDeinitToClient",         "(I)V",                                    (void*)nContextDeinitToClient },


// All methods below are thread protected in java.
{"rsnContextCreate",                 "(III)I",                                (void*)nContextCreate },
{"rsnContextFinish",                 "(I)V",                                  (void*)nContextFinish },
{"rsnContextSetPriority",            "(II)V",                                 (void*)nContextSetPriority },
{"rsnContextDestroy",                "(I)V",                                  (void*)nContextDestroy },
{"rsnContextDump",                   "(II)V",                                 (void*)nContextDump },
{"rsnObjDestroy",                    "(II)V",                                 (void*)nObjDestroy },

{"rsnElementCreate",                 "(IIIZI)I",                              (void*)nElementCreate },
{"rsnElementCreate2",                "(I[I[Ljava/lang/String;[I)I",           (void*)nElementCreate2 },
{"rsnElementGetSubElements",         "(II[I[Ljava/lang/String;[I)V",          (void*)nElementGetSubElements },

{"rsnTypeCreate",                    "(IIIIIZZ)I",                            (void*)nTypeCreate },

{"rsnAllocationCreateTyped",         "(IIIII)I",                               (void*)nAllocationCreateTyped },
{"rsnAllocationCreateFromBitmap",    "(IIILandroid/graphics/Bitmap;I)I",      (void*)nAllocationCreateFromBitmap },
{"rsnAllocationCubeCreateFromBitmap","(IIILandroid/graphics/Bitmap;I)I",      (void*)nAllocationCubeCreateFromBitmap },

{"rsnAllocationCopyFromBitmap",      "(IILandroid/graphics/Bitmap;)V",        (void*)nAllocationCopyFromBitmap },
{"rsnAllocationCopyToBitmap",        "(IILandroid/graphics/Bitmap;)V",        (void*)nAllocationCopyToBitmap },

{"rsnAllocationSyncAll",             "(III)V",                                (void*)nAllocationSyncAll },
{"rsnAllocationData1D",              "(IIIII[II)V",                           (void*)nAllocationData1D_i },
{"rsnAllocationData1D",              "(IIIII[SI)V",                           (void*)nAllocationData1D_s },
{"rsnAllocationData1D",              "(IIIII[BI)V",                           (void*)nAllocationData1D_b },
{"rsnAllocationData1D",              "(IIIII[FI)V",                           (void*)nAllocationData1D_f },
{"rsnAllocationElementData1D",       "(IIIII[BI)V",                           (void*)nAllocationElementData1D },
{"rsnAllocationData2D",              "(IIIIIIII[II)V",                        (void*)nAllocationData2D_i },
{"rsnAllocationData2D",              "(IIIIIIII[SI)V",                        (void*)nAllocationData2D_s },
{"rsnAllocationData2D",              "(IIIIIIII[BI)V",                        (void*)nAllocationData2D_b },
{"rsnAllocationData2D",              "(IIIIIIII[FI)V",                        (void*)nAllocationData2D_f },
{"rsnAllocationData2D",              "(IIIIIIIIIIIII)V",                      (void*)nAllocationData2D_alloc },
{"rsnAllocationRead",                "(II[I)V",                               (void*)nAllocationRead_i },
{"rsnAllocationRead",                "(II[S)V",                               (void*)nAllocationRead_s },
{"rsnAllocationRead",                "(II[B)V",                               (void*)nAllocationRead_b },
{"rsnAllocationRead",                "(II[F)V",                               (void*)nAllocationRead_f },
{"rsnAllocationGetType",             "(II)I",                                 (void*)nAllocationGetType},
{"rsnAllocationResize1D",            "(III)V",                                (void*)nAllocationResize1D },
{"rsnAllocationResize2D",            "(IIII)V",                               (void*)nAllocationResize2D },
{"rsnAllocationGenerateMipmaps",     "(II)V",                                 (void*)nAllocationGenerateMipmaps },

{"rsnScriptBindAllocation",          "(IIII)V",                               (void*)nScriptBindAllocation },
{"rsnScriptSetTimeZone",             "(II[B)V",                               (void*)nScriptSetTimeZone },
{"rsnScriptInvoke",                  "(III)V",                                (void*)nScriptInvoke },
{"rsnScriptInvokeV",                 "(III[B)V",                              (void*)nScriptInvokeV },
{"rsnScriptForEach",                 "(IIIII)V",                              (void*)nScriptForEach },
{"rsnScriptForEach",                 "(IIIII[B)V",                            (void*)nScriptForEachV },
{"rsnScriptSetVarI",                 "(IIII)V",                               (void*)nScriptSetVarI },
{"rsnScriptSetVarJ",                 "(IIIJ)V",                               (void*)nScriptSetVarJ },
{"rsnScriptSetVarF",                 "(IIIF)V",                               (void*)nScriptSetVarF },
{"rsnScriptSetVarD",                 "(IIID)V",                               (void*)nScriptSetVarD },
{"rsnScriptSetVarV",                 "(III[B)V",                              (void*)nScriptSetVarV },
{"rsnScriptSetVarVE",                "(III[BI[I)V",                           (void*)nScriptSetVarVE },
{"rsnScriptSetVarObj",               "(IIII)V",                               (void*)nScriptSetVarObj },

{"rsnScriptCCreate",                 "(ILjava/lang/String;Ljava/lang/String;[BI)I",  (void*)nScriptCCreate },

{"rsnSamplerCreate",                 "(IIIIIIF)I",                            (void*)nSamplerCreate },

};

static int registerFuncs(JNIEnv *_env)
{
    return android::AndroidRuntime::registerNativeMethods(
            _env, classPathName, methods, NELEM(methods));
}

// ---------------------------------------------------------------------------

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (registerFuncs(env) < 0) {
        ALOGE("ERROR: MediaPlayer native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
