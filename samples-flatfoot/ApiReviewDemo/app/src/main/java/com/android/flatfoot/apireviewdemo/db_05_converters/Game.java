package com.android.flatfoot.apireviewdemo.db_05_converters;

import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;
import com.android.support.room.TypeConverters;

import java.util.Date;

@Entity
// could be here as well @TypeConverters(DateConverter.class)
public class Game {
    @PrimaryKey
    private String home;
    @PrimaryKey
    private String away;
    @PrimaryKey
    @TypeConverters(DateConverter.class)
    private Date time;
    private int homeScore;
    private int awayScore;

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getAway() {
        return away;
    }

    public void setAway(String away) {
        this.away = away;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(int homeScore) {
        this.homeScore = homeScore;
    }

    public int getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(int awayScore) {
        this.awayScore = awayScore;
    }
}
