package com.hyu.dynamic.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Table(name = "t_test")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class TestEntity {
    @Id
    private Integer id;
    @Column
    private String content;
    @Column
    private Date createDate;
}
