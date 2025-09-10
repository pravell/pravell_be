package com.pravell.route.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.route.application.RouteFacade;
import com.pravell.route.application.dto.response.CreateRouteResponse;
import com.pravell.route.application.dto.response.RouteResponse;
import com.pravell.route.presentation.request.CreateRouteRequest;
import com.pravell.route.presentation.request.DeleteRouteRequest;
import com.pravell.route.presentation.request.UpdateRouteRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteFacade routeFacade;
    private final CommonJwtUtil commonJwtUtil;

    @PostMapping
    public ResponseEntity<CreateRouteResponse> createRoute(@RequestHeader("authorization") String header,
                                                           @Valid @RequestBody CreateRouteRequest createRouteRequest) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        CreateRouteResponse response = routeFacade.createRoute(id, createRouteRequest.toApplicationRequest());
        return ResponseEntity.created(URI.create("/" + response.getRouteId().toString())).body(response);
    }

    @GetMapping("/{planId}")
    public ResponseEntity<List<RouteResponse>> findRoutes(@RequestHeader("authorization") String header,
                                                          @PathVariable UUID planId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(routeFacade.findRoutes(id, planId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteRoute(@RequestHeader("authorization") String header,
                                            @RequestBody DeleteRouteRequest deleteRouteRequest) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        routeFacade.deleteRoutes(id, deleteRouteRequest.toApplicationRequest());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{routeId}")
    public ResponseEntity<RouteResponse> updateRoutes(@RequestHeader("authorization") String header,
                                                      @Valid @RequestBody UpdateRouteRequest updateRouteRequest,
                                                      @PathVariable UUID routeId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(routeFacade.updateRoute(id, routeId, updateRouteRequest.toApplicationRequest()));
    }

}
