package androidx.room3.integration.autovaluetestapp.vo;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.Override;
import java.lang.String;

final class AutoValue_ParcelableEntity extends $AutoValue_ParcelableEntity {
  public static final Parcelable.Creator<AutoValue_ParcelableEntity> CREATOR = new Parcelable.Creator<AutoValue_ParcelableEntity>() {
    @Override
    public AutoValue_ParcelableEntity createFromParcel(Parcel in) {
      return new AutoValue_ParcelableEntity(
          in.readString()
      );
    }
    @Override
    public AutoValue_ParcelableEntity[] newArray(int size) {
      return new AutoValue_ParcelableEntity[size];
    }
  };

  AutoValue_ParcelableEntity(String value) {
    super(value);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(getValue());
  }

  @Override
  public int describeContents() {
    return 0;
  }
}
