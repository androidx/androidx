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

#include "rsContext.h"
#include "rsScriptIntrinsic.h"
#include <time.h>

using namespace android;
using namespace android::renderscript;

ScriptIntrinsic::ScriptIntrinsic(Context *rsc) : Script(rsc) {
}

ScriptIntrinsic::~ScriptIntrinsic() {
}

bool ScriptIntrinsic::init(Context *rsc, RsScriptIntrinsicID iid, Element *e) {
    mIntrinsicID = iid;
    mElement.set(e);
    mSlots = new ObjectBaseRef<Allocation>[2];
    mTypes = new ObjectBaseRef<const Type>[2];

    rsc->mHal.funcs.script.initIntrinsic(rsc, this, iid, e);


    return true;
}

bool ScriptIntrinsic::freeChildren() {
    return false;
}

void ScriptIntrinsic::setupScript(Context *rsc) {
}

uint32_t ScriptIntrinsic::run(Context *rsc) {
    rsAssert(!"ScriptIntrinsic::run - should not happen");
    return 0;
}


void ScriptIntrinsic::runForEach(Context *rsc,
                         uint32_t slot,
                         const Allocation * ain,
                         Allocation * aout,
                         const void * usr,
                         size_t usrBytes,
                         const RsScriptCall *sc) {

    rsc->mHal.funcs.script.invokeForEach(rsc, this, slot, ain, aout, usr, usrBytes, sc);
}

void ScriptIntrinsic::Invoke(Context *rsc, uint32_t slot, const void *data, size_t len) {
}

void ScriptIntrinsic::serialize(Context *rsc, OStream *stream) const {
}

RsA3DClassID ScriptIntrinsic::getClassId() const {
    return (RsA3DClassID)0;
}



namespace android {
namespace renderscript {


RsScript rsi_ScriptIntrinsicCreate(Context *rsc, uint32_t id, RsElement ve) {
    ScriptIntrinsic *si = new ScriptIntrinsic(rsc);
    ALOGE("rsi_ScriptIntrinsicCreate %i", id);
    if (!si->init(rsc, (RsScriptIntrinsicID)id, (Element *)ve)) {
        delete si;
        return NULL;
    }
    return si;
}

}
}


