/*
 * Copyright (C) 2009-2012 The Android Open Source Project
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

#include "rsContext.h"
#include <time.h>

using namespace android;
using namespace android::renderscript;

Script::Script(Context *rsc) : ObjectBase(rsc) {
    memset(&mEnviroment, 0, sizeof(mEnviroment));
    memset(&mHal, 0, sizeof(mHal));

    mSlots = NULL;
    mTypes = NULL;
    mInitialized = false;
}

Script::~Script() {
    if (mSlots) {
        delete [] mSlots;
        mSlots = NULL;
    }
    if (mTypes) {
        delete [] mTypes;
        mTypes = NULL;
    }
}

void Script::setSlot(uint32_t slot, Allocation *a) {
    //ALOGE("setSlot %i %p", slot, a);
    if (slot >= mHal.info.exportedVariableCount) {
        ALOGE("Script::setSlot unable to set allocation, invalid slot index");
        return;
    }

    mSlots[slot].set(a);
    mRSC->mHal.funcs.script.setGlobalBind(mRSC, this, slot, a);
}

void Script::setVar(uint32_t slot, const void *val, size_t len) {
    //ALOGE("setVar %i %p %i", slot, val, len);
    if (slot >= mHal.info.exportedVariableCount) {
        ALOGE("Script::setVar unable to set allocation, invalid slot index");
        return;
    }
    mRSC->mHal.funcs.script.setGlobalVar(mRSC, this, slot, (void *)val, len);
}

void Script::setVar(uint32_t slot, const void *val, size_t len, Element *e,
                    const size_t *dims, size_t dimLen) {
    if (slot >= mHal.info.exportedVariableCount) {
        ALOGE("Script::setVar unable to set allocation, invalid slot index");
        return;
    }
    mRSC->mHal.funcs.script.setGlobalVarWithElemDims(mRSC, this, slot,
            (void *)val, len, e, dims, dimLen);
}

void Script::setVarObj(uint32_t slot, ObjectBase *val) {
    //ALOGE("setVarObj %i %p", slot, val);
    if (slot >= mHal.info.exportedVariableCount) {
        ALOGE("Script::setVarObj unable to set allocation, invalid slot index");
        return;
    }
    //ALOGE("setvarobj  %i %p", slot, val);
    mRSC->mHal.funcs.script.setGlobalObj(mRSC, this, slot, val);
}

bool Script::freeChildren() {
    incSysRef();
    mRSC->mHal.funcs.script.invokeFreeChildren(mRSC, this);
    return decSysRef();
}

ScriptKernelID::ScriptKernelID(Context *rsc, Script *s, int slot, int sig)
        : ObjectBase(rsc) {

    mScript = s;
    mSlot = slot;
    mHasKernelInput = (sig & 1) != 0;
    mHasKernelOutput = (sig & 2) != 0;
}

ScriptKernelID::~ScriptKernelID() {

}

void ScriptKernelID::serialize(Context *rsc, OStream *stream) const {

}

RsA3DClassID ScriptKernelID::getClassId() const {
    return RS_A3D_CLASS_ID_SCRIPT_KERNEL_ID;
}

ScriptFieldID::ScriptFieldID(Context *rsc, Script *s, int slot) : ObjectBase(rsc) {
    mScript = s;
    mSlot = slot;
}

ScriptFieldID::~ScriptFieldID() {

}

void ScriptFieldID::serialize(Context *rsc, OStream *stream) const {

}

RsA3DClassID ScriptFieldID::getClassId() const {
    return RS_A3D_CLASS_ID_SCRIPT_FIELD_ID;
}


namespace android {
namespace renderscript {

RsScriptKernelID rsi_ScriptKernelIDCreate(Context *rsc, RsScript vs, int slot, int sig) {
    return new ScriptKernelID(rsc, (Script *)vs, slot, sig);
}

RsScriptFieldID rsi_ScriptFieldIDCreate(Context *rsc, RsScript vs, int slot) {
    return new ScriptFieldID(rsc, (Script *)vs, slot);
}

void rsi_ScriptBindAllocation(Context * rsc, RsScript vs, RsAllocation va, uint32_t slot) {
    Script *s = static_cast<Script *>(vs);
    Allocation *a = static_cast<Allocation *>(va);
    s->setSlot(slot, a);
}

void rsi_ScriptSetTimeZone(Context * rsc, RsScript vs, const char * timeZone, size_t length) {
    // We unfortunately need to make a new copy of the string, since it is
    // not NULL-terminated. We then use setenv(), which properly handles
    // freeing/duplicating the actual string for the environment.
    char *tz = (char *) malloc(length + 1);
    if (!tz) {
        ALOGE("Couldn't allocate memory for timezone buffer");
        return;
    }
    strncpy(tz, timeZone, length);
    tz[length] = '\0';
    if (setenv("TZ", tz, 1) == 0) {
        tzset();
    } else {
        ALOGE("Error setting timezone");
    }
    free(tz);
}

void rsi_ScriptForEach(Context *rsc, RsScript vs, uint32_t slot,
                       RsAllocation vain, RsAllocation vaout,
                       const void *params, size_t paramLen) {
    Script *s = static_cast<Script *>(vs);
    s->runForEach(rsc, slot,
                  static_cast<const Allocation *>(vain), static_cast<Allocation *>(vaout),
                  params, paramLen);

}

void rsi_ScriptInvoke(Context *rsc, RsScript vs, uint32_t slot) {
    Script *s = static_cast<Script *>(vs);
    s->Invoke(rsc, slot, NULL, 0);
}


void rsi_ScriptInvokeData(Context *rsc, RsScript vs, uint32_t slot, void *data) {
    Script *s = static_cast<Script *>(vs);
    s->Invoke(rsc, slot, NULL, 0);
}

void rsi_ScriptInvokeV(Context *rsc, RsScript vs, uint32_t slot, const void *data, size_t len) {
    Script *s = static_cast<Script *>(vs);
    s->Invoke(rsc, slot, data, len);
}

void rsi_ScriptSetVarI(Context *rsc, RsScript vs, uint32_t slot, int value) {
    Script *s = static_cast<Script *>(vs);
    s->setVar(slot, &value, sizeof(value));
}

void rsi_ScriptSetVarObj(Context *rsc, RsScript vs, uint32_t slot, RsObjectBase value) {
    Script *s = static_cast<Script *>(vs);
    ObjectBase *o = static_cast<ObjectBase *>(value);
    s->setVarObj(slot, o);
}

void rsi_ScriptSetVarJ(Context *rsc, RsScript vs, uint32_t slot, long long value) {
    Script *s = static_cast<Script *>(vs);
    s->setVar(slot, &value, sizeof(value));
}

void rsi_ScriptSetVarF(Context *rsc, RsScript vs, uint32_t slot, float value) {
    Script *s = static_cast<Script *>(vs);
    s->setVar(slot, &value, sizeof(value));
}

void rsi_ScriptSetVarD(Context *rsc, RsScript vs, uint32_t slot, double value) {
    Script *s = static_cast<Script *>(vs);
    s->setVar(slot, &value, sizeof(value));
}

void rsi_ScriptSetVarV(Context *rsc, RsScript vs, uint32_t slot, const void *data, size_t len) {
    Script *s = static_cast<Script *>(vs);
    s->setVar(slot, data, len);
}

void rsi_ScriptSetVarVE(Context *rsc, RsScript vs, uint32_t slot,
                        const void *data, size_t len, RsElement ve,
                        const size_t *dims, size_t dimLen) {
    Script *s = static_cast<Script *>(vs);
    Element *e = static_cast<Element *>(ve);
    s->setVar(slot, data, len, e, dims, dimLen);
}

}
}

