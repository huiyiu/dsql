package com.hyu.dynamic.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Getter
@Setter
@Table(name = "t_test")
@Entity
public class TestEntity {
    @Id
    private Integer id;
    @Column
    private String content;
    @Column
    private Date createDate;
}
