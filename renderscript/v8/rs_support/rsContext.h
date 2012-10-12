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

#ifndef ANDROID_RS_CONTEXT_H
#define ANDROID_RS_CONTEXT_H

#include "rsUtils.h"
#include "rs_hal.h"

#include "rsThreadIO.h"
#include "rsScriptC.h"
#include "rsScriptGroup.h"
#include "rsSampler.h"
#include <string.h>

// ---------------------------------------------------------------------------
namespace android {

namespace renderscript {

class Device;

#if 0
#define CHECK_OBJ(o) { \
    GET_TLS(); \
    if (!ObjectBase::isValid(rsc, (const ObjectBase *)o)) {  \
        ALOGE("Bad object %p at %s, %i", o, __FILE__, __LINE__);  \
    } \
}
#define CHECK_OBJ_OR_NULL(o) { \
    GET_TLS(); \
    if (o && !ObjectBase::isValid(rsc, (const ObjectBase *)o)) {  \
        ALOGE("Bad object %p at %s, %i", o, __FILE__, __LINE__);  \
    } \
}
#else
#define CHECK_OBJ(o)
#define CHECK_OBJ_OR_NULL(o)
#endif

class Context {
public:
    struct Hal {
        void * drv;

        RsdHalFunctions funcs;
    };
    Hal mHal;

    static Context * createContext(Device *, const RsSurfaceConfig *sc);
    static Context * createContextLite();
    ~Context();

    static pthread_mutex_t gInitMutex;
    // Library mutex (for providing thread-safe calls from the runtime)
    static pthread_mutex_t gLibMutex;

    class PushState {
    public:
        PushState(Context *);
        ~PushState();

    private:
        Context *mRsc;
    };

    RsSurfaceConfig mUserSurfaceConfig;

    ElementState mStateElement;
    TypeState mStateType;
    SamplerState mStateSampler;

    ScriptCState mScriptC;

    bool setupCheck();

    void setPriority(int32_t p);
    void destroyWorkerThreadResources();

    void assignName(ObjectBase *obj, const char *name, uint32_t len);
    void removeName(ObjectBase *obj);

    RsMessageToClientType peekMessageToClient(size_t *receiveLen, uint32_t *subID);
    RsMessageToClientType getMessageToClient(void *data, size_t *receiveLen, uint32_t *subID, size_t bufferLen);
    bool sendMessageToClient(const void *data, RsMessageToClientType cmdID, uint32_t subID, size_t len, bool waitForSpace) const;
    uint32_t runScript(Script *s);

    void initToClient();
    void deinitToClient();

    mutable ThreadIO mIO;

    // Timers
    enum Timers {
        RS_TIMER_IDLE,
        RS_TIMER_INTERNAL,
        RS_TIMER_SCRIPT,
        RS_TIMER_CLEAR_SWAP,
        _RS_TIMER_TOTAL
    };
    uint64_t getTime() const;
    void timerInit();
    void timerReset();
    void timerSet(Timers);
    void timerPrint();
    void timerFrame();

    struct {
        bool mLogTimes;
        bool mLogScripts;
        bool mLogObjects;
        bool mLogShaders;
        bool mLogShadersAttr;
        bool mLogShadersUniforms;
        bool mLogVisual;
        uint32_t mDebugMaxThreads;
    } props;

    mutable struct {
        bool inRoot;
        const char *command;
        const char *file;
        uint32_t line;
    } watchdog;
    static void printWatchdogInfo(void *ctx);

    void dumpDebug() const;
    void setError(RsError e, const char *msg = NULL) const;

    mutable const ObjectBase * mObjHead;

    uint32_t getDPI() const {return mDPI;}
    void setDPI(uint32_t dpi) {mDPI = dpi;}

    uint32_t getTargetSdkVersion() const {return mTargetSdkVersion;}
    void setTargetSdkVersion(uint32_t sdkVer) {mTargetSdkVersion = sdkVer;}

    Device *mDev;
protected:

    uint32_t mTargetSdkVersion;
    uint32_t mDPI;
    uint32_t mWidth;
    uint32_t mHeight;
    int32_t mThreadPriority;
    bool mIsGraphicsContext;

    bool mRunning;
    bool mExit;
    bool mPaused;
    mutable RsError mError;

    pthread_t mThreadId;
    pid_t mNativeThreadId;

    ObjectBaseRef<Script> mRootScript;

    void displayDebugStats();

private:
    Context();
    bool initContext(Device *, const RsSurfaceConfig *sc);


    bool initGLThread();
    void deinitEGL();

    uint32_t runRootScript();

    static void * threadProc(void *);
    static void * helperThreadProc(void *);

    bool mHasSurface;
    bool mIsContextLite;

    Vector<ObjectBase *> mNames;

    uint64_t mTimers[_RS_TIMER_TOTAL];
    Timers mTimerActive;
    uint64_t mTimeLast;
    uint64_t mTimeFrame;
    uint64_t mTimeLastFrame;
    uint32_t mTimeMSLastFrame;
    uint32_t mTimeMSLastScript;
    uint32_t mTimeMSLastSwap;
    uint32_t mAverageFPSFrameCount;
    uint64_t mAverageFPSStartTime;
    uint32_t mAverageFPS;
};

} // renderscript
} // android
#endif
