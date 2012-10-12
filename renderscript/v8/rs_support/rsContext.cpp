/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include "rs.h"
#include "rsDevice.h"
#include "rsContext.h"
#include "rsThreadIO.h"

#include <sys/types.h>
#include <sys/resource.h>
#include <sched.h>

#include <cutils/properties.h>

#include <sys/syscall.h>
#include <string.h>

using namespace android;
using namespace android::renderscript;

pthread_mutex_t Context::gInitMutex = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t Context::gLibMutex = PTHREAD_MUTEX_INITIALIZER;

bool Context::initGLThread() {
    pthread_mutex_lock(&gInitMutex);

    if (!mHal.funcs.initGraphics(this)) {
        pthread_mutex_unlock(&gInitMutex);
        ALOGE("%p initGraphics failed", this);
        return false;
    }

    pthread_mutex_unlock(&gInitMutex);
    return true;
}

void Context::deinitEGL() {
    //mHal.funcs.shutdownGraphics(this);
}

Context::PushState::PushState(Context *con) {
    mRsc = con;
}

Context::PushState::~PushState() {
}


uint32_t Context::runScript(Script *s) {
    PushState ps(this);

    uint32_t ret = s->run(this);
    return ret;
}

uint32_t Context::runRootScript() {
    timerSet(RS_TIMER_SCRIPT);
    watchdog.inRoot = true;
    uint32_t ret = runScript(mRootScript.get());
    watchdog.inRoot = false;

    return ret;
}

uint64_t Context::getTime() const {
#ifndef ANDROID_RS_SERIALIZE
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_nsec + ((uint64_t)t.tv_sec * 1000 * 1000 * 1000);
#else
    return 0;
#endif //ANDROID_RS_SERIALIZE
}

void Context::timerReset() {
    for (int ct=0; ct < _RS_TIMER_TOTAL; ct++) {
        mTimers[ct] = 0;
    }
}

void Context::timerInit() {
    mTimeLast = getTime();
    mTimeFrame = mTimeLast;
    mTimeLastFrame = mTimeLast;
    mTimerActive = RS_TIMER_INTERNAL;
    mAverageFPSFrameCount = 0;
    mAverageFPSStartTime = mTimeLast;
    mAverageFPS = 0;
    timerReset();
}

void Context::timerFrame() {
    mTimeLastFrame = mTimeFrame;
    mTimeFrame = getTime();
    // Update average fps
    const uint64_t averageFramerateInterval = 1000 * 1000000;
    mAverageFPSFrameCount ++;
    uint64_t inverval = mTimeFrame - mAverageFPSStartTime;
    if (inverval >= averageFramerateInterval) {
        inverval = inverval / 1000000;
        mAverageFPS = (mAverageFPSFrameCount * 1000) / inverval;
        mAverageFPSFrameCount = 0;
        mAverageFPSStartTime = mTimeFrame;
    }
}

void Context::timerSet(Timers tm) {
    uint64_t last = mTimeLast;
    mTimeLast = getTime();
    mTimers[mTimerActive] += mTimeLast - last;
    mTimerActive = tm;
}

void Context::timerPrint() {
    double total = 0;
    for (int ct = 0; ct < _RS_TIMER_TOTAL; ct++) {
        total += mTimers[ct];
    }
    uint64_t frame = mTimeFrame - mTimeLastFrame;
    mTimeMSLastFrame = frame / 1000000;
    mTimeMSLastScript = mTimers[RS_TIMER_SCRIPT] / 1000000;
    mTimeMSLastSwap = mTimers[RS_TIMER_CLEAR_SWAP] / 1000000;


    if (props.mLogTimes) {
        ALOGV("RS: Frame (%i),   Script %2.1f%% (%i),  Swap %2.1f%% (%i),  Idle %2.1f%% (%lli),  Internal %2.1f%% (%lli), Avg fps: %u",
             mTimeMSLastFrame,
             100.0 * mTimers[RS_TIMER_SCRIPT] / total, mTimeMSLastScript,
             100.0 * mTimers[RS_TIMER_CLEAR_SWAP] / total, mTimeMSLastSwap,
             100.0 * mTimers[RS_TIMER_IDLE] / total, mTimers[RS_TIMER_IDLE] / 1000000,
             100.0 * mTimers[RS_TIMER_INTERNAL] / total, mTimers[RS_TIMER_INTERNAL] / 1000000,
             mAverageFPS);
    }
}

bool Context::setupCheck() {

    return true;
}

static uint32_t getProp(const char *str) {
    char buf[PROPERTY_VALUE_MAX];
    property_get(str, buf, "0");
    return atoi(buf);
}

void Context::displayDebugStats() {
}

void * Context::threadProc(void *vrsc) {
    Context *rsc = static_cast<Context *>(vrsc);
#ifndef ANDROID_RS_SERIALIZE
    rsc->mNativeThreadId = gettid();
    // TODO: Use proper ANDROID_PRIORITY_DISPLAY
    setpriority(PRIO_PROCESS, rsc->mNativeThreadId, /* ANDROID_PRIORITY_DISPLAY */ -4);
    rsc->mThreadPriority = /* ANDROID_PRIORITY_DISPLAY */ -4;
#endif //ANDROID_RS_SERIALIZE
    rsc->props.mLogTimes = getProp("debug.rs.profile") != 0;
    rsc->props.mLogScripts = getProp("debug.rs.script") != 0;
    rsc->props.mLogObjects = getProp("debug.rs.object") != 0;
    rsc->props.mLogShaders = getProp("debug.rs.shader") != 0;
    rsc->props.mLogShadersAttr = getProp("debug.rs.shader.attributes") != 0;
    rsc->props.mLogShadersUniforms = getProp("debug.rs.shader.uniforms") != 0;
    rsc->props.mLogVisual = getProp("debug.rs.visual") != 0;
    rsc->props.mDebugMaxThreads = getProp("debug.rs.max-threads");

    if (!rsdHalInit(rsc, 0, 0)) {
        rsc->setError(RS_ERROR_FATAL_DRIVER, "Failed initializing GL");
        ALOGE("Hal init failed");
        return NULL;
    }
    rsc->mHal.funcs.setPriority(rsc, rsc->mThreadPriority);

    rsc->mRunning = true;
    if (!rsc->mIsGraphicsContext) {
        while (!rsc->mExit) {
            rsc->mIO.playCoreCommands(rsc, -1);
        }
    }
    ALOGV("%p RS Thread exiting", rsc);

    ALOGV("%p RS Thread exited", rsc);
    return NULL;
}

void Context::destroyWorkerThreadResources() {
    //ALOGV("destroyWorkerThreadResources 1");
    ObjectBase::zeroAllUserRef(this);
    ObjectBase::freeAllChildren(this);
    mExit = true;
    //ALOGV("destroyWorkerThreadResources 2");
}

void Context::printWatchdogInfo(void *ctx) {
    Context *rsc = (Context *)ctx;
    if (rsc->watchdog.command && rsc->watchdog.file) {
        ALOGE("RS watchdog timeout: %i  %s  line %i %s", rsc->watchdog.inRoot,
             rsc->watchdog.command, rsc->watchdog.line, rsc->watchdog.file);
    } else {
        ALOGE("RS watchdog timeout: %i", rsc->watchdog.inRoot);
    }
}


void Context::setPriority(int32_t p) {
    // Note: If we put this in the proper "background" policy
    // the wallpapers can become completly unresponsive at times.
    // This is probably not what we want for something the user is actively
    // looking at.
    mThreadPriority = p;
    setpriority(PRIO_PROCESS, mNativeThreadId, p);
    mHal.funcs.setPriority(this, mThreadPriority);
}

Context::Context() {
    mDev = NULL;
    mRunning = false;
    mExit = false;
    mPaused = false;
    mObjHead = NULL;
    mError = RS_ERROR_NONE;
    mTargetSdkVersion = 14;
    mDPI = 96;
    mIsContextLite = false;
    memset(&watchdog, 0, sizeof(watchdog));
}

Context * Context::createContext(Device *dev, const RsSurfaceConfig *sc) {
    Context * rsc = new Context();

    if (!rsc->initContext(dev, sc)) {
        delete rsc;
        return NULL;
    }
    return rsc;
}

Context * Context::createContextLite() {
    Context * rsc = new Context();
    rsc->mIsContextLite = true;
    return rsc;
}

bool Context::initContext(Device *dev, const RsSurfaceConfig *sc) {
    pthread_mutex_lock(&gInitMutex);

    mIO.init();
    mIO.setTimeoutCallback(printWatchdogInfo, this, 2e9);

    dev->addContext(this);
    mDev = dev;
    if (sc) {
        mUserSurfaceConfig = *sc;
    } else {
        memset(&mUserSurfaceConfig, 0, sizeof(mUserSurfaceConfig));
    }

    mIsGraphicsContext = sc != NULL;

    int status;
    pthread_attr_t threadAttr;

    pthread_mutex_unlock(&gInitMutex);

    // Global init done at this point.

    status = pthread_attr_init(&threadAttr);
    if (status) {
        ALOGE("Failed to init thread attribute.");
        return false;
    }

    mHasSurface = false;

    timerInit();
    timerSet(RS_TIMER_INTERNAL);

    status = pthread_create(&mThreadId, &threadAttr, threadProc, this);
    if (status) {
        ALOGE("Failed to start rs context thread.");
        return false;
    }
    while (!mRunning && (mError == RS_ERROR_NONE)) {
        usleep(100);
    }

    if (mError != RS_ERROR_NONE) {
        ALOGE("Errors during thread init");
        return false;
    }

    pthread_attr_destroy(&threadAttr);
    return true;
}

Context::~Context() {
    ALOGV("%p Context::~Context", this);

    if (!mIsContextLite) {
        mPaused = false;
        void *res;

        mIO.shutdown();
        int status = pthread_join(mThreadId, &res);
        rsAssert(mExit);

        if (mHal.funcs.shutdownDriver) {
            mHal.funcs.shutdownDriver(this);
        }

        // Global structure cleanup.
        pthread_mutex_lock(&gInitMutex);
        if (mDev) {
            mDev->removeContext(this);
            mDev = NULL;
        }
        pthread_mutex_unlock(&gInitMutex);
    }
    ALOGV("%p Context::~Context done", this);
}

void Context::assignName(ObjectBase *obj, const char *name, uint32_t len) {
    rsAssert(!obj->getName());
    obj->setName(name, len);
    mNames.add(obj);
}

void Context::removeName(ObjectBase *obj) {
    for (size_t ct=0; ct < mNames.size(); ct++) {
        if (obj == mNames[ct]) {
            mNames.removeAt(ct);
            return;
        }
    }
}

RsMessageToClientType Context::peekMessageToClient(size_t *receiveLen, uint32_t *subID) {
    return (RsMessageToClientType)mIO.getClientHeader(receiveLen, subID);
}

RsMessageToClientType Context::getMessageToClient(void *data, size_t *receiveLen, uint32_t *subID, size_t bufferLen) {
    return (RsMessageToClientType)mIO.getClientPayload(data, receiveLen, subID, bufferLen);
}

bool Context::sendMessageToClient(const void *data, RsMessageToClientType cmdID,
                                  uint32_t subID, size_t len, bool waitForSpace) const {

    return mIO.sendToClient(cmdID, subID, data, len, waitForSpace);
}

void Context::initToClient() {
    while (!mRunning) {
        usleep(100);
    }
}

void Context::deinitToClient() {
    mIO.clientShutdown();
}

void Context::setError(RsError e, const char *msg) const {
    mError = e;
    sendMessageToClient(msg, RS_MESSAGE_TO_CLIENT_ERROR, e, strlen(msg) + 1, true);
}


void Context::dumpDebug() const {
    ALOGE("RS Context debug %p", this);
    ALOGE("RS Context debug");

    ALOGE(" RS width %i, height %i", mWidth, mHeight);
    ALOGE(" RS running %i, exit %i, paused %i", mRunning, mExit, mPaused);
    ALOGE(" RS pThreadID %li, nativeThreadID %i", (long int)mThreadId, mNativeThreadId);
}

///////////////////////////////////////////////////////////////////////////////////////////
//

namespace android {
namespace renderscript {

void rsi_ContextFinish(Context *rsc) {
}

void rsi_ContextBindRootScript(Context *rsc, RsScript vs) {
    Script *s = static_cast<Script *>(vs);
    //rsc->setRootScript(s);
}

void rsi_ContextBindSampler(Context *rsc, uint32_t slot, RsSampler vs) {
    Sampler *s = static_cast<Sampler *>(vs);

    if (slot > RS_MAX_SAMPLER_SLOT) {
        ALOGE("Invalid sampler slot");
        return;
    }

    s->bindToContext(&rsc->mStateSampler, slot);
}

void rsi_AssignName(Context *rsc, RsObjectBase obj, const char *name, size_t name_length) {
    ObjectBase *ob = static_cast<ObjectBase *>(obj);
    rsc->assignName(ob, name, name_length);
}

void rsi_ObjDestroy(Context *rsc, void *optr) {
    ObjectBase *ob = static_cast<ObjectBase *>(optr);
    rsc->removeName(ob);
    ob->decUserRef();
}

void rsi_ContextSetPriority(Context *rsc, int32_t p) {
    rsc->setPriority(p);
}

void rsi_ContextDump(Context *rsc, int32_t bits) {
    ObjectBase::dumpAll(rsc);
}

void rsi_ContextDestroyWorker(Context *rsc) {
    rsc->destroyWorkerThreadResources();
}

void rsi_ContextDestroy(Context *rsc) {
    ALOGV("%p rsContextDestroy", rsc);
    rsContextDestroyWorker(rsc);
    delete rsc;
    ALOGV("%p rsContextDestroy done", rsc);
}


RsMessageToClientType rsi_ContextPeekMessage(Context *rsc,
                                           size_t * receiveLen, size_t receiveLen_length,
                                           uint32_t * subID, size_t subID_length) {
    return rsc->peekMessageToClient(receiveLen, subID);
}

RsMessageToClientType rsi_ContextGetMessage(Context *rsc, void * data, size_t data_length,
                                          size_t * receiveLen, size_t receiveLen_length,
                                          uint32_t * subID, size_t subID_length) {
    rsAssert(subID_length == sizeof(uint32_t));
    rsAssert(receiveLen_length == sizeof(size_t));
    return rsc->getMessageToClient(data, receiveLen, subID, data_length);
}

void rsi_ContextInitToClient(Context *rsc) {
    rsc->initToClient();
}

void rsi_ContextDeinitToClient(Context *rsc) {
    rsc->deinitToClient();
}

}
}

RsContext rsContextCreate(RsDevice vdev, uint32_t version,
                          uint32_t sdkVersion) {
    ALOGV("rsContextCreate dev=%p", vdev);
    Device * dev = static_cast<Device *>(vdev);
    Context *rsc = Context::createContext(dev, NULL);
    if (rsc) {
        rsc->setTargetSdkVersion(sdkVersion);
    }
    return rsc;
}

// Only to be called at a3d load time, before object is visible to user
// not thread safe
void rsaGetName(RsContext con, void * obj, const char **name) {
    ObjectBase *ob = static_cast<ObjectBase *>(obj);
    (*name) = ob->getName();
}
