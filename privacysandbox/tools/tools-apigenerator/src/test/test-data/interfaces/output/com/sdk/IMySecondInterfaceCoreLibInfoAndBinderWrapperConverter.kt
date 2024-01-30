package com.sdk

import android.os.Bundle

public object IMySecondInterfaceCoreLibInfoAndBinderWrapperConverter {
    public fun toParcelable(coreLibInfo: Bundle, `interface`: IMySecondInterface):
            IMySecondInterfaceCoreLibInfoAndBinderWrapper {
        val parcelable = IMySecondInterfaceCoreLibInfoAndBinderWrapper()
        parcelable.coreLibInfo = coreLibInfo
        parcelable.binder = `interface`
        return parcelable
    }
}
