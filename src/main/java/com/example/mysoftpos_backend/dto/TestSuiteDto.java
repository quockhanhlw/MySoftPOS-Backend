package com.example.mysoftpos_backend.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestSuiteDto {
    private Long id;
    private String name;
    private String description;
    private Long adminId;
    private String createdAt;
    private List<TestCaseDto> testCases;
}

