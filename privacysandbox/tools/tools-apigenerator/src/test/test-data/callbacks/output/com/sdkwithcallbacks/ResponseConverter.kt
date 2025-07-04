package com.sdkwithcallbacks

public object ResponseConverter {
    public fun fromParcelable(parcelable: ParcelableResponse): Response {
        val annotatedValue = Response(
                response = parcelable.response,
                uiInterface = MyUiInterfaceClientProxy(parcelable.uiInterface.binder, parcelable.uiInterface.coreLibInfo),
                sharedUiInterface = MySharedUiInterfaceClientProxy(parcelable.sharedUiInterface.binder, parcelable.sharedUiInterface.coreLibInfo),
                myEnum = com.sdkwithcallbacks.MyEnumConverter.fromParcelable(parcelable.myEnum))
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Response): ParcelableResponse {
        val parcelable = ParcelableResponse()
        parcelable.response = annotatedValue.response
        parcelable.uiInterface = IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable((annotatedValue.uiInterface as MyUiInterfaceClientProxy).coreLibInfo, annotatedValue.uiInterface.remote)
        parcelable.sharedUiInterface = IMySharedUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable((annotatedValue.sharedUiInterface as MySharedUiInterfaceClientProxy).coreLibInfo, annotatedValue.sharedUiInterface.remote)
        parcelable.myEnum = com.sdkwithcallbacks.MyEnumConverter.toParcelable(annotatedValue.myEnum)
        return parcelable
    }
}
