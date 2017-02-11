package com.android.flatfoot.apireviewdemo.db_02_dao;

import com.android.flatfoot.apireviewdemo.db_01_basic.User;
import com.android.support.room.Dao;
import com.android.support.room.Delete;
import com.android.support.room.Insert;
import com.android.support.room.Query;

import java.util.List;

@Dao
public interface UserCrudDao {
    @Query("select * from user")
    List<User> loadAllUsers();

    @Query("select * from user where id = ?")
    User loadUserById(int id);

    @Query("select * from user where name = :firstName and lastName = :lastName")
    List<User> findByNameAndLastName(String firstName, String lastName);

    @Insert
    void insertUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("delete from user where name like :badName OR lastName like :badName")
    int deleteUsersByName(String badName);

    @Insert(onConflict = Insert.REPLACE)
    void insertOrReplaceUsers(User... users);

    @Delete
    void deleteBothUsers(User user1, User user2);
}
