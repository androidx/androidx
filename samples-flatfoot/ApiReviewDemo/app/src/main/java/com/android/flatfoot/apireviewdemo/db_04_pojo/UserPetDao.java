package com.android.flatfoot.apireviewdemo.db_04_pojo;

import com.android.support.room.Dao;
import com.android.support.room.Query;

import java.util.List;

@Dao
public interface UserPetDao {
    @Query("select user.*, COUNT(*) from user, pet where user.id = pet.owner_id GROUP BY user.id")
    List<UserWithPetCount> getUsersAndNumberOfPets();

    @Query("select user.name as user_name, pet.name as pet_name FROM user, pet WHERE"
            + " user.id = pet.owner_id")
    List<UserNameAndPetName> getNameTuples();
}
