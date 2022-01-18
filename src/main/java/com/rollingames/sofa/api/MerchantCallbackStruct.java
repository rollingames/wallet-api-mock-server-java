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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantCallbackStruct {
    @JsonProperty("merchant_id")
    public Long merchantId;

    @JsonProperty("order_id")
    public String orderId;

    @JsonProperty("currency")
    public String currency;

    @JsonProperty("txid")
    public String txId;

    @JsonProperty("recv_amount")
    public String recvAmount;

    @JsonProperty("broadcast_at")
    public Long broadcastAt;

    @JsonProperty("block_height")
    public Long blockHeight;

    @JsonProperty("from_address")
    public String fromAddress;

    @JsonProperty("to_address")
    public String toAddress;

    @JsonProperty("state")
    public Long state;

    @JsonProperty("currency_bip44")
    public Integer currencyBip44;

    @JsonProperty("token_address")
    public String tokenAddress;

    @JsonProperty("addon")
    public Map<String, Object> addon;

    @JsonProperty("fee")
    public String fee;

    @JsonProperty("decimal")
    public Long decimals;

    @JsonProperty("fee_decimal")
    public Long feeDecimals;
}
