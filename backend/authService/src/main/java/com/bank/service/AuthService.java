package com.bank.service;

import com.bank.entity.dto.Request_LoginDTO;
import com.bank.entity.dto.Request_RegisterDTO;
import com.bank.entity.dto.Response_LoginDTO;
import com.bank.entity.dto.Response_RegisterDTO;
import com.bank.exception.BadCredentialsException;
import com.bank.exception.UserAlreadyExistsException;

public interface AuthService {
    Response_RegisterDTO register(Request_RegisterDTO dto);
    Response_LoginDTO login(Request_LoginDTO dto) throws BadCredentialsException;
}
