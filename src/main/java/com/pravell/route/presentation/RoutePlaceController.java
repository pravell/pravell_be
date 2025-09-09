package com.pravell.route.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.route.application.RoutePlaceFacade;
import com.pravell.route.application.dto.response.RoutePlaceResponse;
import com.pravell.route.presentation.request.SaveRoutePlaceRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routes/{routeId}/places")
public class RoutePlaceController {

    private final CommonJwtUtil commonJwtUtil;
    private final RoutePlaceFacade routePlaceFacade;

    @PostMapping
    public ResponseEntity<RoutePlaceResponse> saveRoutePlace(@RequestHeader("authorization") String header,
                                                             @Valid @RequestBody SaveRoutePlaceRequest saveRoutePlaceRequest,
                                                             @PathVariable UUID routeId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        RoutePlaceResponse response = routePlaceFacade.savePlace(id, routeId,
                saveRoutePlaceRequest.toApplicationRequest());
        return ResponseEntity.created(URI.create("/routes/" + response.getRoutePlaceId() + "/places")).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoutePlaceResponse>> findRoutePlaces(@RequestHeader("authorization") String header,
                                                                    @PathVariable UUID routeId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(routePlaceFacade.findPlaces(id, routeId));
    }

}
