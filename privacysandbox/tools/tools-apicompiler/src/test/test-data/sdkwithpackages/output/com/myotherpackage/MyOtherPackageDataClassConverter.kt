package com.myotherpackage

public object MyOtherPackageDataClassConverter {
    public fun fromParcelable(parcelable: ParcelableMyOtherPackageDataClass):
            MyOtherPackageDataClass {
        val annotatedValue = MyOtherPackageDataClass(
                query = parcelable.query)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: MyOtherPackageDataClass):
            ParcelableMyOtherPackageDataClass {
        val parcelable = ParcelableMyOtherPackageDataClass()
        parcelable.query = annotatedValue.query
        return parcelable
    }
}
