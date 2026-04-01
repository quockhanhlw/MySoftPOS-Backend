package com.example.mysoftpos_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PosAccountConnectionRequest {
    private String terminalId;
    private String serverIp;
    private Integer serverPort;
}

