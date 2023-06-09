package com.dev.controllers;

import com.dev.objects.User;
import com.dev.responses.BasicResponse;
import com.dev.responses.LoginResponse;
import com.dev.persists.Persist;
import com.dev.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static com.dev.utils.Constants.NOTIFY_ALL_ADMINS_NEW_USER_ADDED;
import static com.dev.utils.Errors.*;

@RestController
public class LoginController {


    @Autowired
    private Utils utils;

    @Autowired
    private Persist persist;

    @Autowired
    private LiveUpdatesController liveUpdatesController;

    @RequestMapping(value = "check-username", method = RequestMethod.POST)
    public BasicResponse checkUsername(String username) {
        BasicResponse checkUsernameResponse;
        User fromDb = persist.getUserByUsername(username);
        if (fromDb == null) {
            checkUsernameResponse = new BasicResponse(true, NO_ERROR);
        }
        else {
            checkUsernameResponse = new BasicResponse(false, ERROR_USERNAME_ALREADY_EXISTS);
        }
        return checkUsernameResponse;
    }

    @RequestMapping(value = "sign-up", method = RequestMethod.POST)
    public BasicResponse signUp (String username, String password) {
        BasicResponse basicResponse = new BasicResponse();
        boolean success = false;
        Integer errorCode = null;
        if (username != null) {
            if (password != null) {
                if (utils.isStrongPassword(password)) {
                    User fromDb = persist.getUserByUsername(username);
                    if (fromDb == null) {
                        String token;
                        token = utils.createHash(username, password);
                        User toAdd = new User(
                                username,
                                token,
                                1000d
                        );
                        persist.saveUser(toAdd);
                        success = true;
                        basicResponse = new LoginResponse(true, NO_ERROR, toAdd);
                        liveUpdatesController.notifyAllAdmins(NOTIFY_ALL_ADMINS_NEW_USER_ADDED);

                    } else {
                        errorCode = ERROR_USERNAME_ALREADY_EXISTS;
                    }
                } else {
                    errorCode = ERROR_WEAK_PASSWORD;
                }
            } else {
                errorCode = ERROR_MISSING_PASSWORD;
            }
        } else {
            errorCode = ERROR_MISSING_USERNAME;
        }
        basicResponse.setSuccess(success);
        basicResponse.setErrorCode(errorCode);
        return basicResponse;
    }

    @RequestMapping (value = "login",method = RequestMethod.POST)
    public BasicResponse login (String username, String password) {
        BasicResponse basicResponse = new BasicResponse();
        boolean success = false;
        Integer errorCode = null;
        if (username != null) {
            if (password != null) {
                String token = utils.createHash(username, password);
                User fromDb = persist.getUserByUsernameAndToken(username, token);
                if (fromDb != null) {
                    success = true;
                    basicResponse = new LoginResponse(true, NO_ERROR, fromDb);
                } else {
                    errorCode = ERROR_WRONG_LOGIN_CREDS;
                }
            } else {
                errorCode = ERROR_MISSING_PASSWORD;
            }
        } else {
            errorCode = ERROR_MISSING_USERNAME;
        }
        basicResponse.setSuccess(success);
        basicResponse.setErrorCode(errorCode);
        return basicResponse;
    }
}
