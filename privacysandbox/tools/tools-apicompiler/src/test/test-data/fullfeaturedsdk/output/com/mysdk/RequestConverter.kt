package com.mysdk

import android.content.Context
import androidx.privacysandbox.ui.provider.toCoreLibInfo

public class RequestConverter(
    public val context: Context,
) {
    public fun fromParcelable(parcelable: ParcelableRequest): Request {
        val annotatedValue = Request(
                query = parcelable.query,
                extraValues = parcelable.extraValues.map {
                        InnerValueConverter(context).fromParcelable(it) }.toList(),
                maybeValue = parcelable.maybeValue?.let { notNullValue ->
                        InnerValueConverter(context).fromParcelable(notNullValue) },
                myInterface = (parcelable.myInterface as MyInterfaceStubDelegate).delegate,
                myUiInterface = (parcelable.myUiInterface.binder as
                        MyUiInterfaceStubDelegate).delegate)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Request): ParcelableRequest {
        val parcelable = ParcelableRequest()
        parcelable.query = annotatedValue.query
        parcelable.extraValues = annotatedValue.extraValues.map {
                InnerValueConverter(context).toParcelable(it) }.toTypedArray()
        parcelable.maybeValue = annotatedValue.maybeValue?.let { notNullValue ->
                InnerValueConverter(context).toParcelable(notNullValue) }
        parcelable.myInterface = MyInterfaceStubDelegate(annotatedValue.myInterface, context)
        parcelable.myUiInterface =
                IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable(annotatedValue.myUiInterface.toCoreLibInfo(context),
                MyUiInterfaceStubDelegate(annotatedValue.myUiInterface, context))
        return parcelable
    }
}
