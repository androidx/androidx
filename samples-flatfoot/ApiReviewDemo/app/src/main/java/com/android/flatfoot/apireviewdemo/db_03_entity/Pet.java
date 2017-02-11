package com.android.flatfoot.apireviewdemo.db_03_entity;

import com.android.support.room.ColumnInfo;
import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;

@Entity
public class Pet {
    @PrimaryKey
    private int id;
    @ColumnInfo(name = "owner_id")
    private int ownerId;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
