// Copyright (c) 2019-2022 The Rollin.Games developers
// All Rights Reserved.
// NOTICE: All information contained herein is, and remains
// the property of Rollin.Games and its suppliers,
// if any. The intellectual and technical concepts contained
// herein are proprietary to Rollin.Games
// Dissemination of this information or reproduction of this materia
// is strictly forbidden unless prior written permission is obtained
// from Rollin.Games.

package com.rollingames.sofa.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_DEFAULT)
public class BaseResponse {

    @JsonProperty("error_code")
    private int errorCode;

    @JsonProperty("error")
    private String errorMessage;

    @JsonProperty("message")
    private String errorSubMessage;

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorSubMessage() {
        return errorSubMessage;
    }

    public BaseResponse() {
    }

    public BaseResponse(Exception e) {
        this.errorMessage = e.toString();
    }
}