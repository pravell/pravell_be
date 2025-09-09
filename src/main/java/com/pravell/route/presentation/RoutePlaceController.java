package com.pravell.route.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.route.application.RoutePlaceFacade;
import com.pravell.route.application.dto.response.SaveRoutePlaceResponse;
import com.pravell.route.presentation.request.SaveRoutePlaceRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SaveRoutePlaceResponse> saveRoutePlace(@RequestHeader("authorization") String header,
                                                                 @Valid @RequestBody SaveRoutePlaceRequest saveRoutePlaceRequest,
                                                                 @PathVariable UUID routeId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        SaveRoutePlaceResponse response = routePlaceFacade.savePlace(id, routeId,
                saveRoutePlaceRequest.toApplicationRequest());
        return ResponseEntity.created(URI.create("/routes/" + response.getRoutePlaceId() + "/places")).body(response);
    }

}
