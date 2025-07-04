package com.sdkwithcallbacks

import android.os.Bundle

public object IMySharedUiInterfaceCoreLibInfoAndBinderWrapperConverter {
    public fun toParcelable(coreLibInfo: Bundle, `interface`: IMySharedUiInterface): IMySharedUiInterfaceCoreLibInfoAndBinderWrapper {
        val parcelable = IMySharedUiInterfaceCoreLibInfoAndBinderWrapper()
        parcelable.coreLibInfo = coreLibInfo
        parcelable.binder = `interface`
        return parcelable
    }
}
