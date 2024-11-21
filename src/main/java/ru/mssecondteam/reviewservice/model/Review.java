package ru.mssecondteam.reviewservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    private String title;

    private String content;

    @Column(name = "author_id")
    private Long authorId;

    private String username;

    private Integer mark;

    @Column(name = "event_id")
    private Long eventId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedDateTime;

    private Integer likes;

    private Integer dislikes;
}
