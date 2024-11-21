ALTER TABLE reviews
ADD column likes BIGINT DEFAULT 0;

ALTER TABLE reviews
ADD column dislikes BIGINT DEFAULT 0;

CREATE TABLE IF NOT EXISTS likes (
    like_id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    is_positive boolean NOT NULL,
    CONSTRAINT dislikes_fk FOREIGN KEY (review_id) REFERENCES PUBLIC.reviews(review_id)
);