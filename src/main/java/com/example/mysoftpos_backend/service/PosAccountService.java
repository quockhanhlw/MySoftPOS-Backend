package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.CreatePosAccountRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosAccountService {

    private final UserService userService;

    public List<PosAccountDto> getPosAccountsByAdmin(Long adminId) {
        return userService.getPosAccountsByAdmin(adminId).stream()
                .map(PosAccountDto::fromUserDto)
                .toList();
    }

    public PosAccountDto createPosAccount(Long adminId, CreatePosAccountRequest req) {
        UserDto userDto = userService.createPosAccount(adminId, req);
        return PosAccountDto.fromUserDto(userDto);
    }

    public PosAccountDto updatePosAccount(Long adminId, Long accountId, CreatePosAccountRequest req) {
        UserDto userDto = userService.updatePosAccount(adminId, accountId, req);
        return PosAccountDto.fromUserDto(userDto);
    }

    public void deletePosAccount(Long adminId, Long accountId) {
        userService.deletePosAccount(adminId, accountId);
    }

    public void resetPosAccountPassword(Long adminId, Long accountId, String newPassword) {
        userService.resetPosAccountPassword(adminId, accountId, newPassword);
    }
}

