package com.pravell.route.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.route.application.RouteFacade;
import com.pravell.route.application.dto.response.CreateRouteResponse;
import com.pravell.route.presentation.request.CreateRouteRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
