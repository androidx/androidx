package com.android.flatfoot.apireviewdemo.db_04_pojo;

import com.android.flatfoot.apireviewdemo.db_01_basic.User;
import com.android.support.room.ColumnInfo;

public class UserWithPetCount extends User {
    @ColumnInfo(name = "COUNT(*)")
    public int petCount;
}
