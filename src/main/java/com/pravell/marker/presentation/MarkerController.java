package com.pravell.marker.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.marker.application.MarkerFacade;
import com.pravell.marker.application.dto.response.MarkerResponse;
import com.pravell.marker.application.dto.response.FindMarkersResponse;
import com.pravell.marker.presentation.request.CreateMarkerRequest;
import com.pravell.marker.presentation.request.UpdateMarkerRequest;
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
@RequestMapping("/api/v1/markers")
@RequiredArgsConstructor
public class MarkerController {

    private final CommonJwtUtil commonJwtUtil;
    private final MarkerFacade markerFacade;

    @PostMapping
    public ResponseEntity<MarkerResponse> createMarker(@RequestHeader("authorization") String header,
                                                       @Valid @RequestBody CreateMarkerRequest createMarkerRequest) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        MarkerResponse response = markerFacade.createMarker(id, createMarkerRequest.toApplicationRequest());
        return ResponseEntity.created(URI.create("/markers/" + response.getMarkerId())).body(response);
    }

    @GetMapping("/{planId}")
    public ResponseEntity<List<FindMarkersResponse>> findMarkers(@RequestHeader("authorization") String header,
                                                                 @PathVariable UUID planId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(markerFacade.findMarkers(id, planId));
    }

    @PatchMapping("/{markerId}")
    public ResponseEntity<MarkerResponse> updateMarker(@RequestHeader("authorization") String header,
                                                       @PathVariable Long markerId,
                                                       @Valid @RequestBody UpdateMarkerRequest updateMarkerRequest) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(markerFacade.updateMarker(id, markerId, updateMarkerRequest.toApplicationRequest()));
    }

    @DeleteMapping("/{markerId}")
    public ResponseEntity<Void> deleteMarker(@RequestHeader("authorization") String header,
                                                       @PathVariable Long markerId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        markerFacade.deleteMarker(id, markerId);
        return ResponseEntity.noContent().build();
    }
}
