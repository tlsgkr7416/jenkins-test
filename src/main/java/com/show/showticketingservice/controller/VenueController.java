package com.show.showticketingservice.controller;

import com.show.showticketingservice.model.enumerations.AccessRoles;
import com.show.showticketingservice.model.venue.Venue;
import com.show.showticketingservice.model.venueHall.VenueHallRequest;
import com.show.showticketingservice.model.venueHall.VenueHallResponse;
import com.show.showticketingservice.service.VenueHallService;
import com.show.showticketingservice.service.VenueService;
import com.show.showticketingservice.tool.annotation.UserAuthenticationNecessary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    private final VenueHallService venueHallService;

    @PostMapping
    @UserAuthenticationNecessary(role = AccessRoles.MANAGER)
    public void insertVenue(@RequestBody @Valid Venue venue) {
        venueService.insertVenue(venue);
    }

    @PostMapping("{venueId}/halls")
    public void insertVenueHalls(@RequestBody @Valid List<VenueHallRequest> venueHallRequests, @PathVariable String venueId) {
        venueHallService.insertVenueHalls(venueHallRequests, venueId);
    }

    @PutMapping("{venueId}/halls/{hallId}")
    public void updateVenueHall(@RequestBody @Valid VenueHallRequest venueHallRequest,
                                @PathVariable String venueId,
                                @PathVariable String hallId) {

        venueHallService.updateVenueHall(venueHallRequest, venueId, hallId);
    }

    @DeleteMapping("{venueId}/halls")
    public void deleteVenueHalls(@PathVariable String venueId, @RequestBody List<String> hallIds) {
        venueHallService.deleteVenueHalls(venueId, hallIds);
    }

    @GetMapping("{venueId}/halls")
    public List<VenueHallResponse> getVenueHalls(@PathVariable String venueId) {
        return venueHallService.getVenueHalls(venueId);
    }
}
