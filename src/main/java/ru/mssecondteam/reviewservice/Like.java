package ru.mssecondteam.reviewservice;

import jakarta.persistence.*;
import lombok.*;
import ru.mssecondteam.reviewservice.model.Review;

@Entity
@Table(name = "likes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
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
