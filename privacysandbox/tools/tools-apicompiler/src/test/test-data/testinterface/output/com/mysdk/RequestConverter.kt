package com.mysdk

public object RequestConverter {
    public fun fromParcelable(parcelable: ParcelableRequest): Request {
        val annotatedValue = Request(
                query = parcelable.query,
                myInterface = (parcelable.myInterface as MyInterfaceStubDelegate).delegate)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Request): ParcelableRequest {
        val parcelable = ParcelableRequest()
        parcelable.query = annotatedValue.query
        parcelable.myInterface = MyInterfaceStubDelegate(annotatedValue.myInterface)
        return parcelable
    }
}
