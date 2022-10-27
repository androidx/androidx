package com.sdkwithvalues

public object InnerSdkValueConverter {
    public fun fromParcelable(parcelable: ParcelableInnerSdkValue): InnerSdkValue {
        val annotatedValue = InnerSdkValue(
                bigLong = parcelable.bigLong,
                floatingPoint = parcelable.floatingPoint,
                hugeNumber = parcelable.hugeNumber,
                id = parcelable.id,
                message = parcelable.message,
                myInterface = MyInterfaceClientProxy(parcelable.myInterface),
                separator = parcelable.separator,
                shouldBeAwesome = parcelable.shouldBeAwesome)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: InnerSdkValue): ParcelableInnerSdkValue {
        val parcelable = ParcelableInnerSdkValue()
        parcelable.bigLong = annotatedValue.bigLong
        parcelable.floatingPoint = annotatedValue.floatingPoint
        parcelable.hugeNumber = annotatedValue.hugeNumber
        parcelable.id = annotatedValue.id
        parcelable.message = annotatedValue.message
        parcelable.myInterface = (annotatedValue.myInterface as MyInterfaceClientProxy).remote
        parcelable.separator = annotatedValue.separator
        parcelable.shouldBeAwesome = annotatedValue.shouldBeAwesome
        return parcelable
    }
}
