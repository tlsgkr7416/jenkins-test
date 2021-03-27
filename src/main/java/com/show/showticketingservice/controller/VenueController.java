package com.show.showticketingservice.controller;

import com.show.showticketingservice.model.enumerations.AccessRoles;
import com.show.showticketingservice.model.venue.Venue;
import com.show.showticketingservice.model.venueHall.VenueHall;
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
    public void insertVenueHall(@RequestBody @Valid List<VenueHall> venueHalls, @PathVariable String venueId) {
        venueHallService.insertVenueHall(venueHalls, venueId);
    }

    @PutMapping("{venueId}/halls/{hallId}")
    public void updateVenueHall(@RequestBody @Valid VenueHall venueHall,
                                @PathVariable String venueId,
                                @PathVariable String hallId) {

        venueHallService.updateVenueHall(venueHall, venueId, hallId);
    }

    @DeleteMapping("{venueId}/halls")
    public void deleteVenueHall(@PathVariable String venueId, @RequestBody List<String> hallIds) {
        venueHallService.deleteVenueHall(venueId, hallIds);
    }

    @GetMapping("{venueId}/halls")
    public List<VenueHallResponse> getVenueHalls(@PathVariable String venueId) {
        return venueHallService.getVenueHalls(venueId);
    }
}
