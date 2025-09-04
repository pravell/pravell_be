package com.pravell.place.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.place.application.PlaceFacade;
import com.pravell.place.application.SearchPlaceService;
import com.pravell.place.application.dto.response.FindPlanPlacesResponse;
import com.pravell.place.application.dto.response.PlaceResponse;
import com.pravell.place.application.dto.response.SavePlaceResponse;
import com.pravell.place.application.dto.response.SearchPlaceResponse;
import com.pravell.place.presentation.request.DeletePlacesRequest;
import com.pravell.place.presentation.request.SavePlaceRequest;
import com.pravell.place.presentation.request.UpdatePlaceRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final CommonJwtUtil commonJwtUtil;
    private final SearchPlaceService searchPlaceService;
    private final PlaceFacade placeFacade;

    @GetMapping("/search")
    public ResponseEntity<List<SearchPlaceResponse>> searchPlace(@RequestParam String keyword,
                                                                 @RequestHeader("authorization") String authorizationHeader) {
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(searchPlaceService.search(keyword, id));
    }

    @PostMapping
    public ResponseEntity<SavePlaceResponse> savePlace(@RequestHeader("authorization") String authorizationHeader,
                                                       @Valid @RequestBody SavePlaceRequest savePlaceRequest) {
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(placeFacade.savePlace(id, savePlaceRequest.toApplicationRequest()));
    }

    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<FindPlanPlacesResponse>> findPlanPlaces(@RequestHeader("authorization") String header,
                                                                       @PathVariable UUID planId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(placeFacade.findPlanPlaces(id, planId));
    }

    @PatchMapping("{placeId}")
    public ResponseEntity<PlaceResponse> updatePlace(@RequestHeader("authorization") String header,
                                                     @PathVariable Long placeId,
                                                     @Valid @RequestBody UpdatePlaceRequest updatePlaceRequest) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(placeFacade.updatePlan(id, placeId, updatePlaceRequest.toApplicationRequest()));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePlaces(@RequestHeader("authorization") String header,
                                             @RequestBody DeletePlacesRequest deletePlacesRequest) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        placeFacade.deletePlan(id, deletePlacesRequest.toApplicationRequest());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<PlaceResponse> findPlan(@RequestHeader("authorization") String header,
                                                  @PathVariable Long placeId) {
        UUID id = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(placeFacade.findPlan(id, placeId));
    }

}
