package com.android.flatfoot.apireviewdemo.db_01_basic;


import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey
    public int id;
    public String name;
    public String lastName;
}
