/*
 * Copyright (C) 2012 The Android Open Source Project
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


#include "rsdCore.h"
#include "rsdIntrinsics.h"
#include "rsdAllocation.h"

using namespace android;
using namespace android::renderscript;

void * rsdIntrinsic_InitBlur(const Context *, Script *, RsdIntriniscFuncs_t *);
void * rsdIntrinsic_InitConvolve3x3(const Context *, Script *, RsdIntriniscFuncs_t *);
void * rsdIntrinsic_InitConvolve5x5(const Context *, Script *, RsdIntriniscFuncs_t *);
void * rsdIntrinsic_InitColorMatrix(const Context *, Script *, RsdIntriniscFuncs_t *);
void * rsdIntrinsic_InitLUT(const Context *, Script *, RsdIntriniscFuncs_t *);
void * rsdIntrinsic_InitYuvToRGB(const Context *, Script *, RsdIntriniscFuncs_t *);
void * rsdIntrinsic_InitBlend(const Context *, Script *, RsdIntriniscFuncs_t *);

static void Bind(const Context *, const Script *, void *, uint32_t, Allocation *) {
    rsAssert(!"Intrinsic_Bind unexpectedly called");
}

static void SetVar(const Context *, const Script *, void *, uint32_t, void *, size_t) {
    rsAssert(!"Intrinsic_Bind unexpectedly called");
}

static void Destroy(const Context *dc, const Script *script, void * intrinsicData) {
    free(intrinsicData);
}

void * rsdIntrinsic_Init(const android::renderscript::Context *dc,
                       android::renderscript::Script *script,
                       RsScriptIntrinsicID iid,
                       RsdIntriniscFuncs_t *funcs) {

    funcs->bind = Bind;
    funcs->setVar = SetVar;
    funcs->destroy = Destroy;

    switch(iid) {
    case RS_SCRIPT_INTRINSIC_ID_CONVOLVE_3x3:
        return rsdIntrinsic_InitConvolve3x3(dc, script, funcs);
    case RS_SCRIPT_INTRINSIC_ID_CONVOLVE_5x5:
        return rsdIntrinsic_InitConvolve5x5(dc, script, funcs);
    case RS_SCRIPT_INTRINSIC_ID_COLOR_MATRIX:
        return rsdIntrinsic_InitColorMatrix(dc, script, funcs);
    case RS_SCRIPT_INTRINSIC_ID_LUT:
        return rsdIntrinsic_InitLUT(dc, script, funcs);
    case RS_SCRIPT_INTRINSIC_ID_BLUR:
        return rsdIntrinsic_InitBlur(dc, script, funcs);
    case RS_SCRIPT_INTRINSIC_ID_YUV_TO_RGB:
        return rsdIntrinsic_InitYuvToRGB(dc, script, funcs);
    case RS_SCRIPT_INTRINSIC_ID_BLEND:
        return rsdIntrinsic_InitBlend(dc, script, funcs);

    default:
        return NULL;
    }
    return NULL;
}



