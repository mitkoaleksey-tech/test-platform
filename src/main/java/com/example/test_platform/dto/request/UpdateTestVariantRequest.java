package com.example.test_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateTestVariantRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotEmpty
    private List<Long> taskIds;
}
