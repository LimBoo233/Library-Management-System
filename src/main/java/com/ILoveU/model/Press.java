package com.ILoveU.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "presses")
public class Press {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "press_id")
    private Integer pressId;

    @Column(name = "name", nullable = false)
    private String name;
}
