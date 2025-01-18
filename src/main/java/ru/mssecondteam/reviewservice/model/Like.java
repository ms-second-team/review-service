package ru.mssecondteam.reviewservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.mssecondteam.reviewservice.dto.like.LikeDto;

@Entity
@Table(name = "likes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@NamedNativeQueries({
        @NamedNativeQuery(
                name = "like_dto",
                query = "SELECT review_id AS reviewId, " +
                        "COUNT(CASE WHEN is_positive = TRUE THEN 1 END) AS likes, " +
                        "COUNT(CASE WHEN is_positive = FALSE THEN 1 END) AS dislikes " +
                        "FROM likes " +
                        "WHERE review_id = :reviewId " +
                        "GROUP BY review_id",
                resultSetMapping = "like_query_dto"
        ),
        @NamedNativeQuery(
                name = "like_dto_list",
                query = "SELECT review_id AS reviewId, " +
                        "COUNT(CASE WHEN is_positive = TRUE THEN 1 END) AS likes, " +
                        "COUNT(CASE WHEN is_positive = FALSE THEN 1 END) AS dislikes " +
                        "FROM likes " +
                        "WHERE review_id IN (:reviewsIds) " +
                        "GROUP BY review_id",
                resultSetMapping = "like_query_dto"
        )
})
@SqlResultSetMapping(
        name = "like_query_dto",
        classes = @ConstructorResult(
                targetClass = LikeDto.class,
                columns = {
                        @ColumnResult(name = "reviewId", type = Long.class),
                        @ColumnResult(name = "likes", type = Long.class),
                        @ColumnResult(name = "dislikes", type = Long.class)
                }
        )
)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(name = "is_positive")
    private Boolean isPositive;
}