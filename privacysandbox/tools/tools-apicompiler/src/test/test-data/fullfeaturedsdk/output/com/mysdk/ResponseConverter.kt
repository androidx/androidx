package com.mysdk

import android.content.Context

public class ResponseConverter(
    public val context: Context,
) {
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
                MySecondInterfaceStubDelegate(annotatedValue.mySecondInterface, context)
        parcelable.maybeOtherInterface =
                MySecondInterfaceStubDelegate(annotatedValue.maybeOtherInterface, context)
        return parcelable
    }
}
