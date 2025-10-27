package com.firefly.psps.dtos.provider;
import lombok.*;
import java.util.Map;
@Data @Builder public class ProviderOperationResponse {
    private String operationName;
    private boolean success;
    private Map<String, Object> result;
    private String message;
    private Map<String, Object> metadata;
}
