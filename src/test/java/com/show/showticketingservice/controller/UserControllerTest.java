package com.show.showticketingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.show.showticketingservice.model.enumerations.UserType;
import com.show.showticketingservice.model.user.UserLoginRequest;
import com.show.showticketingservice.model.user.UserRequest;
import com.show.showticketingservice.model.user.UserSession;
import com.show.showticketingservice.tool.response.UserAuthorityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static com.show.showticketingservice.tool.constants.UserConstant.USER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest
class UserControllerTest {

    private UserRequest testUser;
    private UserRequest managerAccount;
    private UserSession userSession;
    private MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WebApplicationContext webApplicationContext;

    @BeforeEach
    public void init() {
        testUser = new UserRequest("testId1", "testPW1234#", "Test User", "010-1111-1111", "user1@example.com", "Seoul, South Korea", UserType.GENERAL);
        managerAccount = new UserRequest("managerTest1", "testPW1234#", "Test Manager", "010-1111-1111", "user1@example.com", "Seoul, South Korea", UserType.MANAGER);
        userSession = new UserSession(1, testUser.getUserType());

        this.mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    }

    private void insertTestUser(UserRequest user) throws Exception {
        String content = objectMapper.writeValueAsString(user);

        mvc.perform(post("/users")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("????????? ?????? ??? Http Status 200 (OK) ??????")
    public void login() throws Exception {
        insertTestUser(testUser);

        UserLoginRequest userLoginRequest = new UserLoginRequest(testUser.getUserId(), testUser.getPassword());

        String content = objectMapper.writeValueAsString(userLoginRequest);

        mvc.perform(post("/login")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

    }

    @Test
    @DisplayName("?????? ???????????? ????????? ?????? ??? Http Status 200 (OK) ??? GENERAL ??????(1) ??????")
    public void loginWithGeneralAccount() throws Exception {
        insertTestUser(testUser);

        UserLoginRequest userLoginRequest = new UserLoginRequest(testUser.getUserId(), testUser.getPassword());

        String content = objectMapper.writeValueAsString(userLoginRequest);
        String response = objectMapper.writeValueAsString(UserAuthorityResponse.OK_GENERAL.getBody());

        mvc.perform(post("/login")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(response))
                .andDo(print());
    }

    @Test
    @DisplayName("????????? ???????????? ????????? ?????? ??? Http Status 200 (OK) ??? MANAGER ??????(2) ??????")
    public void loginWithManager() throws Exception {
        insertTestUser(managerAccount);

        UserLoginRequest userLoginRequest = new UserLoginRequest(managerAccount.getUserId(), managerAccount.getPassword());

        String content = objectMapper.writeValueAsString(userLoginRequest);
        String response = objectMapper.writeValueAsString(UserAuthorityResponse.OK_MANAGER.getBody());

        mvc.perform(post("/login")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(response))
                .andDo(print());
    }

    @Test
    @DisplayName("???????????? ?????? ID??? ????????? ??? Http Status 400 (Bad Request) ??????")
    public void loginWithInvalidId() throws Exception {
        insertTestUser(testUser);

        UserLoginRequest userLoginRequest = new UserLoginRequest("invalidUserId1234", testUser.getPassword());

        String content = objectMapper.writeValueAsString(userLoginRequest);

        mvc.perform(post("/login")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("?????? ??????????????? ????????? ??? Http Status 400 (Bad Request) ??????")
    public void loginWithInvalidPassword() throws Exception {
        insertTestUser(testUser);

        UserLoginRequest userLoginRequest = new UserLoginRequest(testUser.getUserId(), "wrongPw1234@");

        String content = objectMapper.writeValueAsString(userLoginRequest);

        mvc.perform(post("/login")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("?????? ????????? ????????? ??? ????????? ?????? ??? Http 409 Status (Conflict) ??????")
    public void loginDuplicated() throws Exception {
        login();

        UserLoginRequest userLoginRequest = new UserLoginRequest(testUser.getUserId(), testUser.getPassword());

        String content = objectMapper.writeValueAsString(userLoginRequest);

        mvc.perform(post("/login")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .sessionAttr(USER, userSession))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    @Test
    @DisplayName("???????????? ?????? ??? Http Status 201 (Created) ??????")
    public void signUp() throws Exception {
        insertTestUser(testUser);
    }

    @Test
    @DisplayName("?????? ????????? ID??? ???????????? ?????? ??? Http Status 409 (Conflict) ??????")
    public void duplicatedIdSignUp() throws Exception {
        insertTestUser(testUser);

        UserRequest newUser = new UserRequest("testId1", "testPW1111#", "New UserRequest", "010-1111-1234", "user2@example.com", "Seoul, South Korea", UserType.GENERAL);

        String content = objectMapper.writeValueAsString(newUser);

        mvc.perform(post("/users")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    @Test
    @DisplayName("ID ???????????? ??? ???????????? ?????? ??? Http Status 200 (Ok) ??????")
    public void idNotDuplicated() throws Exception {
        mvc.perform(get("/user-exists/newId1"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("ID ???????????? ??? ????????? ??? Http Status 409 (Conflict) ??????")
    public void idDuplicated() throws Exception {
        insertTestUser(testUser);

        mvc.perform(get("/user-exists/testId1"))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    @Test
    @DisplayName("???????????? ?????? ??? Http Status 200 (OK) ??????")
    public void logout() throws Exception {
        login();

        mvc.perform(get("/logout")
                .sessionAttr(USER, userSession))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("????????? ?????? ?????? ???????????? ???????????? ?????? ??? Http Status 401 (Unauthorized) ??????")
    public void nullUserLogout() throws Exception {
        mvc.perform(get("/logout"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

}
