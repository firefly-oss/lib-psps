package com.firefly.psps.dtos.provider;
import lombok.*;
import java.util.Map;
@Data @Builder public class ProviderOperationRequest {
    private String operationName;
    private Map<String, Object> parameters;
    private Map<String, String> metadata;
}
