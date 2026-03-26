package com.example.mysoftpos_backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TerminalDto {
    private Long id;
    private String terminalCode;
    private MerchantDto merchant;
    private Long branchId;
    private Long posAccountId;
    private String serverIp;
    private Integer serverPort;
}

