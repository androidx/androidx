package com.android.flatfoot.apireviewdemo.db_05_converters;

import com.android.support.room.Dao;
import com.android.support.room.Query;
import com.android.support.room.TypeConverters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public abstract class GameDao {
    @Query("select * from Game where `time` BETWEEN :from AND :to")
    abstract public List<Game> findGamesInRange(Date from, Date to);

    public List<Game> listGamesIn1Week() {
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        calendar.set(Calendar.DATE, 7);
        Date nextWeek = calendar.getTime();
        return findGamesInRange(today, nextWeek);
    }
}
