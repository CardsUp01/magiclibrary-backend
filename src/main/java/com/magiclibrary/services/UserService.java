package com.magiclibrary.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.magiclibrary.dto.user.UserCreateDTO;
import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.dto.user.UserUpdateDTO;

public interface UserService {

    UserResponseDTO createUser(UserCreateDTO userCreateDTO);

    UserResponseDTO getAuthenticatedUser(Integer userId);

    UserResponseDTO getUserById(Integer userId);

    UserResponseDTO updateAuthenticatedUser(Integer userId, UserUpdateDTO userUpdateDTO);

    UserResponseDTO updateUserByAdmin(Integer userId, UserUpdateDTO userUpdateDTO);

    List<UserResponseDTO> getAllUsers();

    List<UserResponseDTO> getFilteredUsers(String search, String role, String status, String sort);

    Page<UserResponseDTO> getFilteredUsersPaged(String search, String role, String status, String sort, int page, int size);

    List<UserResponseDTO> suggestUsers(String query);
}