package ru.mssecondteam.reviewservice.model;

import java.util.List;

public record TopReviews(

        List<Review> bestReviews,

        List<Review> worstReviews
) {
}
