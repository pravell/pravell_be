package com.pravell.route.domain.repository;

import com.pravell.route.domain.model.Route;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
}
