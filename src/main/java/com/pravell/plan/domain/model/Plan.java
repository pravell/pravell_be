package com.pravell.plan.domain.model;

import com.pravell.common.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plans")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends AggregateRoot {

    @Id
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean isPublic;

    @Column(nullable = false)
    private Boolean isDeleted;

    public static Plan create(String name, Boolean isPublic) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name(name)
                .isPublic(isPublic)
                .isDeleted(false)
                .build();
    }

    public void delete() {
        isDeleted = true;
    }

}
