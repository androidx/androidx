package com.mysdk

public object RequestConverter {
    public fun fromParcelable(parcelable: ParcelableRequest): Request {
        val annotatedValue = Request(
                query = parcelable.query,
                extraValues = parcelable.extraValues.map {
                        com.mysdk.InnerValueConverter.fromParcelable(it) }.toList(),
                maybeValue = parcelable.maybeValue?.let { notNullValue ->
                        com.mysdk.InnerValueConverter.fromParcelable(notNullValue) },
                myInterface = (parcelable.myInterface as MyInterfaceStubDelegate).delegate)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Request): ParcelableRequest {
        val parcelable = ParcelableRequest()
        parcelable.query = annotatedValue.query
        parcelable.extraValues = annotatedValue.extraValues.map {
                com.mysdk.InnerValueConverter.toParcelable(it) }.toTypedArray()
        parcelable.maybeValue = annotatedValue.maybeValue?.let { notNullValue ->
                com.mysdk.InnerValueConverter.toParcelable(notNullValue) }
        parcelable.myInterface = MyInterfaceStubDelegate(annotatedValue.myInterface)
        return parcelable
    }
}
