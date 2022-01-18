// Copyright (c) 2019-2022 The Rollin.Games developers
// All Rights Reserved.
// NOTICE: All information contained herein is, and remains
// the property of Rollin.Games and its suppliers,
// if any. The intellectual and technical concepts contained
// herein are proprietary to Rollin.Games
// Dissemination of this information or reproduction of this materia
// is strictly forbidden unless prior written permission is obtained
// from Rollin.Games.

package com.rollingames.sofa.mock;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;
import java.net.URLEncoder;
import java.net.URLDecoder;

import com.rollingames.sofa.api.BaseResponse;
import com.rollingames.sofa.api.MerchantCallbackStruct;
import com.rollingames.sofa.api.PaymentOrder;
import com.rollingames.sofa.api.CommonResponse;
import com.rollingames.sofa.api.SetApiTokenRequest;
import com.rollingames.sofa.api.WithdrawTransaction;
import com.rollingames.sofa.mock.entity.ApiToken;
import com.rollingames.sofa.mock.repository.ApiTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
public class MerchantController {
	private static Logger logger = Logger.getLogger(MerchantController.class.getName());

	@Autowired
	Api apiClient;

	@Autowired
	ApiTokenRepository repository;

	@PostMapping("/v1/mock/merchant/{merchantId}/apitoken")
	public HttpEntity<CommonResponse> setAPIToken(@PathVariable("merchantId") long merchantId,
			@RequestBody SetApiTokenRequest request) {

		ApiToken newToken = new ApiToken(merchantId, request.getApiCode(), request.getApiSecret());
		repository.save(newToken);

		return new ResponseEntity<>(new CommonResponse(1L), HttpStatus.OK);
	}

	@PostMapping("/v1/mock/merchant/{merchantId}/order")
	public HttpEntity<Object> requestPaymentOrder(@PathVariable("merchantId") long merchantId,
			@RequestBody String bodyString) {

		try {
			final ObjectMapper mapper = new ObjectMapper();
			PaymentOrder.RequestPaymentOrderRequest pr = mapper.readValue(bodyString,
					PaymentOrder.RequestPaymentOrderRequest.class);
			if (pr.redirectUrl != null && !pr.redirectUrl.isEmpty()) {
				pr.redirectUrl = URLEncoder.encode(pr.redirectUrl, "UTF-8");
			}

			String request = mapper.writeValueAsString(pr);
			Api.Response response = apiClient.makeRequest(merchantId, "POST",
				String.format("/v1/merchant/%d/order", merchantId), null, request);
			return new ResponseEntity<>(response.getContent(), response.getStatus());
		} catch (Exception e) {
			return new ResponseEntity<>(new BaseResponse(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/v1/mock/merchant/{merchantId}/order")
	public HttpEntity<Object> queryPaymentOrder(@PathVariable("merchantId") long merchantId,
			@RequestParam(name = "token") String token,
			@RequestParam(name = "order") String order) {

		Api.Response response = apiClient.makeRequest(merchantId, "GET", String.format("/v1/merchant/%d/order", merchantId),
				new String[] { String.format("token=%s", token), String.format("order=%s", order), }, null);

		try {
				final ObjectMapper mapper = new ObjectMapper();
				PaymentOrder.QueryPaymentOrderResponse res = mapper.readValue(response.getContent(),
						PaymentOrder.QueryPaymentOrderResponse.class);
				if (res.redirectUrl != null && !res.redirectUrl.isEmpty()) {
					res.redirectUrl = URLDecoder.decode(res.redirectUrl, "UTF-8");
				}

				String resDecoded = mapper.writeValueAsString(res);
				return new ResponseEntity<>(resDecoded, response.getStatus());
			} catch (Exception e) {
				return new ResponseEntity<>(new BaseResponse(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping("/v1/mock/merchant/{merchantId}/order/duration")
	public HttpEntity<String> updateOrderDuration(@PathVariable("merchantId") long merchantId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(merchantId, "POST",
				String.format("/v1/merchant/%d/order/duration", merchantId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@DeleteMapping("/v1/mock/merchant/{merchantId}/order")
	public HttpEntity<String> cancelPaymentOrder(@PathVariable("merchantId") long merchantId,
			@RequestParam(name = "token") String token,
			@RequestParam(name = "order") String order) {

		Api.Response response = apiClient.makeRequest(merchantId, "DELETE",
				String.format("/v1/merchant/%d/order", merchantId),
				new String[] { String.format("token=%s", token), String.format("order=%s", order), }, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
	
	@GetMapping("/v1/mock/merchant/{merchantId}/apisecret")
	public HttpEntity<String> getMerchantAPITokenStatus(@PathVariable("merchantId") long merchantId) {
		Api.Response response = apiClient.makeRequest(merchantId, "GET",
				String.format("/v1/merchant/%d/apisecret", merchantId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/merchant/{merchantId}/apisecret/activate")
	public HttpEntity<String> activateMerchantAPIToken(@PathVariable("merchantId") Long merchantId) {
		Api.Response response = apiClient.makeRequest(merchantId, "POST",
				String.format("/v1/merchant/%d/apisecret/activate", merchantId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
	
	@PostMapping("/v1/mock/merchant/{merchantId}/apisecret/refreshsecret")
	public HttpEntity<String> refreshMerchantSecret(@PathVariable("merchantId") long merchantId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(merchantId, "POST",
				String.format("/v1/merchant/%d/apisecret/refreshsecret", merchantId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/merchant/{merchantId}/notifications/manual")
	public HttpEntity<String> resendFailedMerchantCallbacks(@PathVariable("merchantId") long merchantId) {

		Api.Response response = apiClient.makeRequest(merchantId, "POST",
				String.format("/v1/merchant/%d/notifications/manual", merchantId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/merchant/callback")
	public HttpEntity<Object> callback(@RequestHeader("X-CHECKSUM") String checkSum, @RequestBody String bodyString) {
		try {

			final ObjectMapper mapper = new ObjectMapper();
			MerchantCallbackStruct request = mapper.readValue(bodyString, MerchantCallbackStruct.class);

			Optional<ApiToken> optApiToken = repository.findById(request.merchantId);
			String payload = bodyString + optApiToken.get().getApiSecret();

			// Generate check sum
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.reset();
			digest.update(payload.getBytes("utf8"));
			final Base64.Encoder encoder = Base64.getUrlEncoder();
			final String checksumVerf = encoder.encodeToString(digest.digest());

			if (!checksumVerf.equals(checkSum)) {
				logger.info(String.format("callback expect checkSum %s, got %s", checksumVerf, checkSum));
				throw new Exception("Bad checksum");
			}

			logger.info(String.format("callback %s", bodyString));

			// Callback Type
			// Merchant Order State
			// Success      = 0
			// Expired      = 1
			// Insufficient = 2
			// Excess       = 3
			// Cancel       = 4

			if (request.state == 0) {
			} else if (request.state == 1) {
			} else if (request.state == 2) {
			} else if (request.state == 3) {
			} else if (request.state == 4) {
			}

			// reply 200 OK to confirm the callback has been processed
			return new ResponseEntity<>("OK", HttpStatus.OK);

		} catch (Exception e) {
			logger.warning(String.format("callback failed with exception %s", e.toString()));
			return new ResponseEntity<>(new BaseResponse(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
