package com.show.showticketingservice.service;

import com.show.showticketingservice.model.enumerations.UserType;
import com.show.showticketingservice.model.user.UserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserValidationTest {

    private Validator validator;

    @BeforeEach
    public void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private UserRequest getUserWithId(String userId) {
        return UserRequest.builder()
                .userId(userId)
                .password("password1234!")
                .name("name")
                .phoneNum("010-1234-5678")
                .email("test@example.com")
                .address("Seoul, South Korea")
                .userType(UserType.GENERAL)
                .build();
    }

    private UserRequest getUserWithPw(String password) {
        return UserRequest.builder()
                .userId("idExample123")
                .password(password)
                .name("name")
                .phoneNum("010-1234-5678")
                .email("test@example.com")
                .address("Seoul, South Korea")
                .userType(UserType.GENERAL)
                .build();
    }

    private UserRequest getUserWithPhoneNum(String phoneNum) {
        return UserRequest.builder()
                .userId("idExample123")
                .password("password1234!")
                .name("name")
                .phoneNum(phoneNum)
                .email("test@example.com")
                .address("Seoul, South Korea")
                .userType(UserType.GENERAL)
                .build();
    }

    private UserRequest getUserWithEmail(String email) {
        return UserRequest.builder()
                .userId("idExample123")
                .password("password1234!")
                .name("name")
                .phoneNum("010-1234-5678")
                .email(email)
                .address("Seoul, South Korea")
                .userType(UserType.GENERAL)
                .build();
    }

    public void checkValidation(UserRequest userRequest, boolean assertFlag) {
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(userRequest);
        if (assertFlag)
            assertTrue(violations.isEmpty());
        else
            assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("valid id: ??????/????????? ?????? ?????? ?????? 6~20??? ??????")
    public void validId() {
        String id = "abcd123";
        UserRequest userRequest = getUserWithId(id);
        checkValidation(userRequest, true);
    }

    @Test
    @DisplayName("5?????? ????????? id??? invalid")
    public void shortId() {
        String id = "ab12";
        UserRequest userRequest = getUserWithId(id);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("20?????? ????????? id??? invalid")
    public void longId() {
        String id = "aaaaabbbbbccccc123456";
        UserRequest userRequest = getUserWithId(id);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("????????? ????????? id??? invalid")
    public void idWithBlank() {
        String id = "abcd 123";
        UserRequest userRequest = getUserWithId(id);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("??????????????? ????????? id??? invalid")
    public void idWithSpecialChar() {
        String id = "abcd@123";
        UserRequest userRequest = getUserWithId(id);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("??????/?????????, ????????? ?????? ????????? ????????? id??? invalid")
    public void idWithOtherChar() {
        String id = "abcd?????????123";
        UserRequest userRequest = getUserWithId(id);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("null??? id??? invalid")
    public void nullId() {
        String id = null;
        UserRequest userRequest = getUserWithId(id);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("valid password: ??????/?????????, ??????, ???????????? ?????? ?????? 6~20??? ??????")
    public void validPw() {
        String password = "pwpw1234@";
        UserRequest userRequest = getUserWithPw(password);
        checkValidation(userRequest, true);
    }

    @Test
    @DisplayName("5?????? ????????? pw??? invalid")
    public void shortPw() {
        String password = "Pw12@";
        UserRequest userRequest = getUserWithPw(password);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("20?????? ????????? pw??? invalid")
    public void longPw() {
        String password = "aaaaabbbbbccccc123456!@#";
        UserRequest ususerRequest = getUserWithPw(password);
        checkValidation(ususerRequest, false);
    }

    @Test
    @DisplayName("????????? ????????? pw??? invalid")
    public void pwWithBlank() {
        String password = "abcd 123 !@#";
        UserRequest userRequest = getUserWithPw(password);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("??????????????? ????????? pw??? invalid")
    public void pwWithSpecialChar() {
        String password = "pwpw123";
        UserRequest userRequest = getUserWithPw(password);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("null??? pw??? invalid")
    public void nullPw() {
        String password = null;
        UserRequest userRequest = getUserWithPw(password);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("valid name: 15??? ??????")
    public void validName() {
        String name = "name example";
        UserRequest userRequest = UserRequest.builder()
                .userId("idExample123")
                .password("password123!@")
                .name(name)
                .phoneNum("010-1234-5678")
                .email("test@example.com")
                .address("Seoul, South Korea")
                .userType(UserType.GENERAL)
                .build();
        checkValidation(userRequest, true);
    }

    @Test
    @DisplayName("valid phoneNum: '-' ?????? '010-[4?????? ??????]-[4?????? ??????]' ?????????????????? ??????")
    public void validPhoneNum() {
        String phoneNum = "010-1234-5678";
        UserRequest userRequest = getUserWithPhoneNum(phoneNum);
        checkValidation(userRequest, true);
    }

    @Test
    @DisplayName("????????? ?????? ?????? ???????????? ??????")
    public void invalidPhoneNum() {
        String phoneNum = "010-12345-678";
        UserRequest userRequest = getUserWithPhoneNum(phoneNum);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("?????????(-)??? ?????? ??????????????? invalid")
    public void phoneNumWithOutHyphen() {
        String phoneNum = "01012345678";
        UserRequest userRequest = getUserWithPhoneNum(phoneNum);
        checkValidation(userRequest, false);
    }

    @Test
    public void validEmail() {
        String email = "user@example.com";
        UserRequest userRequest = getUserWithEmail(email);
        checkValidation(userRequest, true);
    }

    @Test
    public void invalidEmail() {
        String email = "user.example.com";
        UserRequest userRequest = getUserWithEmail(email);
        checkValidation(userRequest, false);
    }

    @Test
    @DisplayName("valid address: 100??? ??????")
    public void validAddress() {
        String address = "???????????? ????????? ????????? OO??? 134-2??????";
        UserRequest userRequest = UserRequest.builder()
                .userId("idExample123")
                .password("password123!@")
                .name("name")
                .phoneNum("010-1234-5678")
                .email("test@example.com")
                .address(address)
                .userType(UserType.GENERAL)
                .build();
        checkValidation(userRequest, true);
    }

}
