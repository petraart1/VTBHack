package com.bank.entity.dto;

import java.time.LocalDate;

public record Request_RegisterDTO(String firstName, String lastName, LocalDate birthOfDate, String email, String password) { }
