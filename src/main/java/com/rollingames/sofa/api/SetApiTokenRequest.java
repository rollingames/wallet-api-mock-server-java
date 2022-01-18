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

import com.fasterxml.jackson.annotation.JsonProperty;

public class SetApiTokenRequest {

    @JsonProperty("api_code")
    private String apiCode;

    @JsonProperty("api_secret")
    private String apiSecret;

    @Override
    public String toString() {
        return String.format("ApiCode(%s, %s)", apiCode, apiSecret);
    }

    public String getApiCode() {
        return apiCode;
    }

    public String getApiSecret() {
        return apiSecret;
    }   
}