package com.sdkwithcallbacks

public object ResponseConverter {
    public fun fromParcelable(parcelable: ParcelableResponse): Response {
        val annotatedValue = Response(
                response = parcelable.response,
                uiInterface = MyUiInterfaceClientProxy(parcelable.uiInterface.binder,
                        parcelable.uiInterface.coreLibInfo))
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Response): ParcelableResponse {
        val parcelable = ParcelableResponse()
        parcelable.response = annotatedValue.response
        parcelable.uiInterface =
                IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable((annotatedValue.uiInterface
                as MyUiInterfaceClientProxy).coreLibInfo, annotatedValue.uiInterface.remote)
        return parcelable
    }
}
