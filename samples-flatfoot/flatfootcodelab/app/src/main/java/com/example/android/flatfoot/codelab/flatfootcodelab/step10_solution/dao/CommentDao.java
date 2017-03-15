package com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.dao;

import com.android.support.lifecycle.LiveData;
import com.android.support.room.Dao;
import com.android.support.room.Insert;
import com.android.support.room.OnConflictStrategy;
import com.android.support.room.Query;

import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyComment;

import java.util.List;

@Dao
public interface CommentDao {
    @Query("SELECT * FROM comments where productId = ?")
    LiveData<List<MyComment>> loadComments(int productId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MyComment> products);
}
