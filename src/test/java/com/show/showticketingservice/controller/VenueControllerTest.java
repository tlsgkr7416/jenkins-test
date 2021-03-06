package com.show.showticketingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.show.showticketingservice.model.enumerations.UserType;
import com.show.showticketingservice.model.showPlace.ShowPlace;
import com.show.showticketingservice.model.user.UserRequest;
import com.show.showticketingservice.model.user.UserSession;
import com.show.showticketingservice.model.venue.VenueRequest;
import com.show.showticketingservice.model.venueHall.VenueHallRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.ArrayList;
import java.util.List;
import static com.show.showticketingservice.tool.constants.UserConstant.USER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest
class VenueControllerTest {

    private UserRequest managerAccount;

    private UserRequest generalUser;

    private VenueRequest venueRequest;

    private VenueHallRequest venueHallRequestOne;

    private VenueHallRequest venueHallRequestTwo;

    private List<VenueHallRequest> venueHallRequests = new ArrayList<>();

    private ShowPlace showPlace;

    private MockMvc mvc;

    @Autowired
    WebApplicationContext context;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockHttpSession httpSession;

    @BeforeEach
    public void init() {
        managerAccount = new UserRequest("managerTest1", "testPW1234#", "Test Manager", "010-1111-1111", "manager1@example.com", "Seoul, South Korea", UserType.MANAGER);

        generalUser = new UserRequest("testId1", "testPW1234#", "Test User", "010-1111-1111", "user1@example.com", "Seoul, South Korea", UserType.GENERAL);

        venueRequest = new VenueRequest(0,"?????????1", "?????????", "02-1212-3434", "www.?????????1.co.kr");

        venueHallRequestOne = new VenueHallRequest("?????????A", 50, 60);

        venueHallRequestTwo = new VenueHallRequest("?????????B", 30, 50);

        venueHallRequests.add(venueHallRequestOne);

        venueHallRequests.add(venueHallRequestTwo);

        showPlace = new ShowPlace(venueRequest, venueHallRequests);

        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private void loginUser(UserRequest userRequest) throws Exception {

        int tmpUserNum = 1;

        UserSession userSession = new UserSession(tmpUserNum, userRequest.getUserType());

        httpSession.setAttribute(USER, userSession);
    }

    @Test
    @DisplayName("????????? ?????? ?????? ?????? (????????? ?????????) - Http Status 200 (OK) ")
    public void normalVenueInsertion() throws Exception {
        loginUser(managerAccount);

        String content = objectMapper.writeValueAsString(showPlace);

        mvc.perform(post("/venues")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(httpSession))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("????????? ?????? ?????? ????????? ????????? ?????? ?????? ?????? ??? ?????? - Http Status 401 (Unauthorized)")
    public void insertVenueWithoutLogin() throws Exception {
        String content = objectMapper.writeValueAsString(showPlace);

        mvc.perform(post("/venues")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("?????? ???????????? ????????? ??? ????????? ?????? ?????? ?????? ??? ?????? - Http Status 401 (Unauthorized)")
    public void insertVenueByGeneralUser() throws Exception {
        loginUser(generalUser);

        String content = objectMapper.writeValueAsString(showPlace);

        mvc.perform(post("/venues")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(httpSession))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DB??? ????????? ????????? ???????????? ????????? ??? ?????? ?????? ?????? ??? ?????? - Http Status 400 (Bad Request)")
    public void insertDuplicatedVenue() throws Exception {
        loginUser(managerAccount);

        String content = objectMapper.writeValueAsString(showPlace);

        mvc.perform(post("/venues")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(httpSession))
                .andDo(print())
                .andExpect(status().isOk());

        mvc.perform(post("/venues")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(httpSession))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("?????? ????????? ??????(ex. ????????????)??? ????????? ????????? ?????? ?????? ?????? ??? ?????? - Http Status 400 (Bad Request)")
    public void insertVenueWithMissingInfo() throws Exception {
        VenueRequest incompleteVenue = VenueRequest.builder()
                .name(venueRequest.getName())
                .address(venueRequest.getAddress())
                .tel("")
                .homepage(venueRequest.getHomepage())
                .build();

        showPlace = new ShowPlace(incompleteVenue, venueHallRequests);

        loginUser(managerAccount);

        String content = objectMapper.writeValueAsString(showPlace);

        mvc.perform(post("/venues")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(httpSession))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("????????? ????????? ?????? ?????? ????????? ?????? ??? ?????? - Http Status 400 (Bad Request)")
    public void insertDuplicatedVenueHalls() throws Exception {

        loginUser(managerAccount);

        VenueHallRequest venueHallRequestThree = new VenueHallRequest("?????????A", 50, 60);

        venueHallRequests.add(venueHallRequestThree);

        showPlace = new ShowPlace(venueRequest, venueHallRequests);

        String content = objectMapper.writeValueAsString(showPlace);

        mvc.perform(post("/venues")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(httpSession))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
