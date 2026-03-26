package com.example.mysoftpos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBranchRequest {
    @NotBlank
    @Size(max = 32)
    private String branchCode;

    @Size(max = 100)
    private String branchName;

    @Size(max = 255)
    private String branchAddress;
}

