package com.firefly.psps.dtos.disputes;
import lombok.*;
import java.util.Map;
@Data @Builder public class SubmitEvidenceRequest {
    private String disputeId;
    private Map<String, Object> evidence;
}
