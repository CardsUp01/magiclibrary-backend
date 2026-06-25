package com.magiclibrary;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("Demo123!"));
    }
}