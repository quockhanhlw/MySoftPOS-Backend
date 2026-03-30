package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.CreatePosAccountRequest;
import com.example.mysoftpos_backend.dto.PosAccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosAccountService {

    private final PosAccountServiceCore posAccountServiceCore;

    public List<PosAccountDto> getPosAccountsByAdmin(Long adminId) {
        return posAccountServiceCore.getPosAccountsByAdmin(adminId);
    }

    public PosAccountDto createPosAccount(Long adminId, CreatePosAccountRequest req) {
        return posAccountServiceCore.createPosAccount(adminId, req);
    }

    public PosAccountDto updatePosAccount(Long adminId, Long accountId, CreatePosAccountRequest req) {
        return posAccountServiceCore.updatePosAccount(adminId, accountId, req);
    }

    public void deletePosAccount(Long adminId, Long accountId) {
        posAccountServiceCore.deletePosAccount(adminId, accountId);
    }

    public void resetPosAccountPassword(Long adminId, Long accountId, String newPassword) {
        posAccountServiceCore.resetPosAccountPassword(adminId, accountId, newPassword);
    }
}

