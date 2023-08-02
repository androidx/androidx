package com.sdkwithvalues

public object InnerSdkValueConverter {
    public fun fromParcelable(parcelable: ParcelableInnerSdkValue): InnerSdkValue {
        val annotatedValue = InnerSdkValue(
                id = parcelable.id,
                bigLong = parcelable.bigLong,
                shouldBeAwesome = parcelable.shouldBeAwesome,
                separator = parcelable.separator,
                message = parcelable.message,
                floatingPoint = parcelable.floatingPoint,
                hugeNumber = parcelable.hugeNumber,
                myInterface = MyInterfaceClientProxy(parcelable.myInterface),
                myUiInterface = MyUiInterfaceClientProxy(parcelable.myUiInterface.binder,
                        parcelable.myUiInterface.coreLibInfo),
                numbers = parcelable.numbers.toList(),
                maybeNumber = parcelable.maybeNumber.firstOrNull(),
                maybeInterface = parcelable.maybeInterface?.let { notNullValue ->
                        MyInterfaceClientProxy(notNullValue) })
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: InnerSdkValue): ParcelableInnerSdkValue {
        val parcelable = ParcelableInnerSdkValue()
        parcelable.id = annotatedValue.id
        parcelable.bigLong = annotatedValue.bigLong
        parcelable.shouldBeAwesome = annotatedValue.shouldBeAwesome
        parcelable.separator = annotatedValue.separator
        parcelable.message = annotatedValue.message
        parcelable.floatingPoint = annotatedValue.floatingPoint
        parcelable.hugeNumber = annotatedValue.hugeNumber
        parcelable.myInterface = (annotatedValue.myInterface as MyInterfaceClientProxy).remote
        parcelable.myUiInterface =
                IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable((annotatedValue.myUiInterface
                as MyUiInterfaceClientProxy).coreLibInfo, annotatedValue.myUiInterface.remote)
        parcelable.numbers = annotatedValue.numbers.toIntArray()
        parcelable.maybeNumber = if (annotatedValue.maybeNumber == null) intArrayOf() else
                intArrayOf(annotatedValue.maybeNumber)
        parcelable.maybeInterface = annotatedValue.maybeInterface?.let { notNullValue ->
                (notNullValue as MyInterfaceClientProxy).remote }
        return parcelable
    }
}
