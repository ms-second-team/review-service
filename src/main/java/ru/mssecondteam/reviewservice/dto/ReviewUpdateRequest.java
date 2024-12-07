package ru.mssecondteam.reviewservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Request DTO for updating an existing review")
public record ReviewUpdateRequest(

        @Schema(description = "Title of the review", example = "Updated Review Title")
        @Size(min = 2, max = 100, message = "Title can not be blank and must contain between 2 and 100 symbols.")
        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ -.,!?]+$", message = "Title can not be blank and must contain between 2 and 100 symbols.")
        String title,

        @Schema(description = "Content of the review", example = "The updated review content with more details.")
        @Size(min = 2, max = 500, message = "Content can not be blank and must contain between 2 and 500 symbols.")
        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ -.,!?]+$", message = "Content can not be blank and must contain between 2 and 500 symbols.")
        String content,

        @Schema(description = "Username of the reviewer", example = "user_123")
        @Pattern(regexp = "^[a-z0-9_.]{2,30}$", message = "Username can not be blank and must contain between 2 and 30 symbols.")
        String username,

        @Schema(description = "Updated rating given to the review, between 1 and 10", example = "9")
        @Min(value = 1, message = "Mark must be between '1' and '10'")
        @Max(value = 10, message = "Mark must be between '1' and '10'")
        Integer mark
) {
}