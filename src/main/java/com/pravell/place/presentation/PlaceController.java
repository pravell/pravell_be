package com.pravell.place.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.place.application.SearchPlaceService;
import com.pravell.place.application.dto.response.SearchPlaceResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/search")
    public ResponseEntity<List<SearchPlaceResponse>> searchPlace(@RequestParam String keyword,
                                                                 @RequestHeader("authorization") String authorizationHeader){
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(searchPlaceService.search(keyword, id));
    }

}
