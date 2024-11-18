package ru.mssecondteam.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateReviewRequest(

        @Size(min = 2, max = 100, message = "Title can not be blank and must contain between 2 and 100 symbols.")
        @Pattern(regexp = "^[a-zA-zа-яА-ЯёЁ -]+$", message = "Title can not be blank and must contain between 2 and 100 symbols.")
        String title,

        @Size(min = 2, max = 500, message = "Content can not be blank and must contain between 2 and 500 symbols.")
        @Pattern(regexp = "^[a-zA-zа-яА-ЯёЁ -]+$", message = "Content can not be blank and must contain between 2 and 500 symbols.")
        String content,

        @Pattern(regexp = "^[a-z0-9]{2,30}$", message = "Username can not be blank and must contain between 2 and 30 symbols.")
        String username,

        @Min(value = 1, message = "Mark must be between '1' and '10")
        @Max(value = 10, message = "Mark must be between '1' and '10")
        Integer mark
) {
}
