package com.android.flatfoot.apireviewdemo.db_03_entity;

import com.android.support.room.Dao;
import com.android.support.room.Query;

import java.util.List;

@Dao
public interface PetDao {
    @Query("select * from Pet where owner_id = :userId")
    public List<Pet> findPetsOfUser(int userId);
}
