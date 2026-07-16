package com.app.ecom_application.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "addresses")
@Entity
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String street;
    private String city;
    private String state;
    private String country;
    private String zipcode;
}
