package com.sporty.bettask.dto;

import java.util.List;

public record ErrorResponse(String code, String message, List<ValidationError> details) {
}
