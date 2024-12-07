package ru.mssecondteam.reviewservice.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Structure representing an error response.")
public record ErrorResponse(

        @Schema(description = "A map of error details where keys represent field names or error identifiers and values are error messages.")
        Map<String, String> errors,

        @Schema(description = "HTTP status code associated with the error.", example = "404")
        Integer status,

        @Schema(description = "Timestamp of when the error occurred.", example = "2024-12-07 14:30:00")
        LocalDateTime timestamp
) {
}