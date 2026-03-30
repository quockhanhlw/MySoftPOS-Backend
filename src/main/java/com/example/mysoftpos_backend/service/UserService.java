package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.CreatePosAccountRequest;
import com.example.mysoftpos_backend.dto.CreateUserRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import com.example.mysoftpos_backend.dto.UserDto;
import com.example.mysoftpos_backend.entity.PosAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Deprecated
public class UserService {

    private final PosAccountServiceCore posAccountServiceCore;

    public List<UserDto> getUsersByAdmin(Long adminId) {
        return posAccountServiceCore.getPosAccountsByAdmin(adminId).stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    // Legacy alias retained for compatibility.
    public List<UserDto> getPosAccountsByAdmin(Long adminId) {
        return getUsersByAdmin(adminId);
    }

    public UserDto createUser(Long adminId, CreateUserRequest req) {
        return toUserDto(posAccountServiceCore.createPosAccount(adminId, toCreatePosAccountRequest(req)));
    }

    public UserDto createPosAccount(Long adminId, CreateUserRequest req) {
        return toUserDto(posAccountServiceCore.createPosAccount(adminId, toCreatePosAccountRequest(req)));
    }

    public UserDto updateUser(Long adminId, Long userId, CreateUserRequest req) {
        return toUserDto(posAccountServiceCore.updatePosAccount(adminId, userId, toCreatePosAccountRequest(req)));
    }

    public UserDto updatePosAccount(Long adminId, Long accountId, CreateUserRequest req) {
        return toUserDto(posAccountServiceCore.updatePosAccount(adminId, accountId, toCreatePosAccountRequest(req)));
    }

    public void deleteUser(Long adminId, Long userId) {
        posAccountServiceCore.deletePosAccount(adminId, userId);
    }

    public void deletePosAccount(Long adminId, Long accountId) {
        deleteUser(adminId, accountId);
    }

    public void resetPassword(Long adminId, Long userId, String newPassword) {
        posAccountServiceCore.resetPosAccountPassword(adminId, userId, newPassword);
    }

    public void resetPosAccountPassword(Long adminId, Long accountId, String newPassword) {
        resetPassword(adminId, accountId, newPassword);
    }

    public UserDto toDto(PosAccount u) {
        return toUserDto(posAccountServiceCore.toPosAccountDto(u));
    }

    private CreatePosAccountRequest toCreatePosAccountRequest(CreateUserRequest req) {
        if (req == null) {
            return null;
        }
        return new CreatePosAccountRequest(
                req.getPhone(),
                req.getPassword(),
                req.getFullName(),
                req.getEmail(),
                req.getDob(),
                req.getGender(),
                req.getStoreName(),
                req.getBankName(),
                req.getBusinessType(),
                req.getStoreAddress(),
                req.getMerchantId(),
                req.getBranchId(),
                req.getTerminalId(),
                req.getServerIp(),
                req.getServerPort());
    }

    private UserDto toUserDto(PosAccountDto dto) {
        if (dto == null) {
            return null;
        }
        return UserDto.builder()
                .id(dto.getId())
                .merchantId(dto.getMerchantId())
                .branchId(dto.getBranchId())
                .branchCode(dto.getBranchCode())
                .branchName(dto.getBranchName())
                .merchantCode(dto.getMerchantCode())
                .role(dto.getRole())
                .fullName(dto.getFullName())
                .username(dto.getUsername())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .dob(dto.getDob())
                .gender(dto.getGender())
                .storeName(dto.getStoreName())
                .bankName(dto.getBankName())
                .businessType(dto.getBusinessType())
                .storeAddress(dto.getStoreAddress())
                .phoneVerified(dto.getPhoneVerified())
                .terminalId(dto.getTerminalId())
                .active(dto.isActive())
                .online(dto.isOnline())
                .build();
    }
}
