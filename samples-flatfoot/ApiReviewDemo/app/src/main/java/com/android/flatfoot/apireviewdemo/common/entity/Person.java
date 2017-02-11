package com.android.flatfoot.apireviewdemo.common.entity;

import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;

@Entity
public class Person {
    @PrimaryKey
    private int id;
    private String login;
    private String name;
    private String lastName;
    private String company;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
