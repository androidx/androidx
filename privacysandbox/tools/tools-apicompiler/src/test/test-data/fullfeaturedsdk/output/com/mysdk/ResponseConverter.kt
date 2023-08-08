package com.mysdk

import android.content.Context
import androidx.privacysandbox.ui.provider.toCoreLibInfo

public class ResponseConverter(
    public val context: Context,
) {
    public fun fromParcelable(parcelable: ParcelableResponse): Response {
        val annotatedValue = Response(
                response = parcelable.response,
                mySecondInterface = (parcelable.mySecondInterface as
                        MySecondInterfaceStubDelegate).delegate,
                maybeOtherInterface = (parcelable.maybeOtherInterface as
                        MySecondInterfaceStubDelegate).delegate,
                myUiInterface = (parcelable.myUiInterface.binder as
                        MyUiInterfaceStubDelegate).delegate)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Response): ParcelableResponse {
        val parcelable = ParcelableResponse()
        parcelable.response = annotatedValue.response
        parcelable.mySecondInterface =
                MySecondInterfaceStubDelegate(annotatedValue.mySecondInterface, context)
        parcelable.maybeOtherInterface =
                MySecondInterfaceStubDelegate(annotatedValue.maybeOtherInterface, context)
        parcelable.myUiInterface =
                IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable(annotatedValue.myUiInterface.toCoreLibInfo(context),
                MyUiInterfaceStubDelegate(annotatedValue.myUiInterface, context))
        return parcelable
    }
}
