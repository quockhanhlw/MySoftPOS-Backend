package com.example.mysoftpos_backend.dto;

import lombok.NoArgsConstructor;

/**
 * @deprecated Use {@link CreatePosAccountRequest}. Kept for /api/users compatibility.
 */
@Deprecated
@NoArgsConstructor
public class CreateUserRequest extends CreatePosAccountRequest {

    public CreateUserRequest(String phone,
                             String password,
                             String fullName,
                             String email,
                             String dob,
                             String gender,
                             String storeName,
                             String bankName,
                             String businessType,
                             String storeAddress,
                             Long merchantId,
                             Long branchId,
                             String terminalId,
                             String serverIp,
                             Integer serverPort) {
        super(phone, password, fullName, email, dob, gender, storeName, bankName, businessType,
                storeAddress, merchantId, branchId, terminalId, serverIp, serverPort);
    }
}
