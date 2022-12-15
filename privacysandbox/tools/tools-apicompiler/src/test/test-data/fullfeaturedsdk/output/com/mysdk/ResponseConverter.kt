package com.mysdk

public object ResponseConverter {
    public fun fromParcelable(parcelable: ParcelableResponse): Response {
        val annotatedValue = Response(
                response = parcelable.response,
                mySecondInterface = (parcelable.mySecondInterface as
                        MySecondInterfaceStubDelegate).delegate,
                maybeOtherInterface = (parcelable.maybeOtherInterface as
                        MySecondInterfaceStubDelegate).delegate)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Response): ParcelableResponse {
        val parcelable = ParcelableResponse()
        parcelable.response = annotatedValue.response
        parcelable.mySecondInterface =
                MySecondInterfaceStubDelegate(annotatedValue.mySecondInterface)
        parcelable.maybeOtherInterface =
                MySecondInterfaceStubDelegate(annotatedValue.maybeOtherInterface)
        return parcelable
    }
}
