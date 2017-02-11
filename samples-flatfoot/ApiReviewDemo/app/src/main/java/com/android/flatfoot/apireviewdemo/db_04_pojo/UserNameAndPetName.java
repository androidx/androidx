package com.android.flatfoot.apireviewdemo.db_04_pojo;

import com.android.support.room.ColumnInfo;

public class UserNameAndPetName {
    @ColumnInfo(name = "user_name")
    private String userName;
    @ColumnInfo(name = "pet_name")
    private String petName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }
}
