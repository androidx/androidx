#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <rsEnv.h>
#include "rsDispatch.h"
#define LOG_API(...)

extern "C" void AllocationSetSurface(JNIEnv *_env, jobject _this, RsContext con, RsAllocation alloc, jobject sur, dispatchTable dispatchTab)
{
    LOG_API("nAllocationSetSurface, con(%p), alloc(%p), surface(%p)",
            con, alloc, sur);

    ANativeWindow* s = NULL;
    if (sur != 0) {
        s = ANativeWindow_fromSurface(_env, sur);
    }
    dispatchTab.AllocationSetSurface(con, alloc, s);
}

