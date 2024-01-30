package com.sdkwithcallbacks

import android.os.Bundle

public object IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter {
    public fun toParcelable(coreLibInfo: Bundle, `interface`: IMyUiInterface):
            IMyUiInterfaceCoreLibInfoAndBinderWrapper {
        val parcelable = IMyUiInterfaceCoreLibInfoAndBinderWrapper()
        parcelable.coreLibInfo = coreLibInfo
        parcelable.binder = `interface`
        return parcelable
    }
}
