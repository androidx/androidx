package com.example.android.persistence.codelab.step5_solution.entity;

import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;

import com.example.android.persistence.codelab.step5.Comment;

import java.util.Date;

@Entity(tableName = "comments")
public class MyComment implements Comment {
    @PrimaryKey
    private int id;
    private int productId;
    private String text;
    private Date postedAt;

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    @Override
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Date getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Date postedAt) {
        this.postedAt = postedAt;
    }

    public MyComment() {
    }

    public MyComment(Comment comment) {
        id = comment.getId();
        productId = comment.getProductId();
        text = comment.getText();
        postedAt = comment.getPostedAt();
    }
}
