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

import com.rollingames.sofa.api.BaseResponse;
import com.rollingames.sofa.api.CallbackStruct;
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
public class MockController {
	private static Logger logger = Logger.getLogger(MockController.class.getName());

	@Autowired
	Api apiClient;

	@Autowired
	ApiTokenRepository repository;

	@PostMapping("/v1/mock/wallets/{walletId}/apitoken")
	public HttpEntity<CommonResponse> setAPIToken(@PathVariable("walletId") long walletId,
			@RequestBody SetApiTokenRequest request) {

		ApiToken newToken = new ApiToken(walletId, request.getApiCode(), request.getApiSecret());
		repository.save(newToken);

		return new ResponseEntity<>(new CommonResponse(1L), HttpStatus.OK);
	}

	@PostMapping("/v1/mock/wallets/{walletId}/addresses")
	public HttpEntity<String> createDepositWalletAddresses(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/addresses", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/addresses")
	public HttpEntity<String> getDepositWalletAddresses(@PathVariable("walletId") long walletId,
			@RequestParam(name = "start_index", defaultValue = "0") Integer startIndex,
			@RequestParam(name = "request_number", defaultValue = "0") Integer requestNumber) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/addresses", walletId),
				new String[] { String.format("start_index=%d", startIndex),
						String.format("request_number=%d", requestNumber), },
				null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/pooladdress")
	public HttpEntity<String> getDepositWalletPoolAddresses(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/pooladdress", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/callback")
	public HttpEntity<Object> callback(@RequestHeader("X-CHECKSUM") String checkSum, @RequestBody String bodyString) {
		try {

			final ObjectMapper mapper = new ObjectMapper();
			CallbackStruct request = mapper.readValue(bodyString, CallbackStruct.class);

			Optional<ApiToken> optApiToken = repository.findById(request.walletId);
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
			// DepositCallback  = 1
			// WithdrawCallback = 2
			// CollectCallback  = 3
			// AirdropCallback  = 4
			
			// Processing State
			// ProcessingStateInPool  = 0
			// ProcessingStateInChain = 1
			// ProcessingStateDone    = 2
			
			// Callback State
			// CallbackStateHolding         = 1
			// CallbackStateInPool          = 2
			// CallbackStateInChain         = 3
			// CallbackStateFailed          = 5
			// CallbackStateCancelled       = 8
			// CallbackStateDropped         = 10
			// CallbackStateInChainFailed   = 11
			if (request.type == 1) { // DepositCallback
				//
				// deposit unique ID
				String uniqueID = String.format("%s_%d", request.txId, request.vOutIndex);
				//
				if (request.processingState == 2) { // ProcessingStateDone
					// deposit succeeded, use the deposit unique ID to update your business logic
				}
			} else if (request.type == 2) { // WithdrawCallback
				//
				// withdrawal unique ID
				String uniqueID = request.orderID;
				//
				if (request.state == 3 && request.processingState == 2) { // CallbackStateInChain && ProcessingStateDone
					// withdrawal succeeded, use the withdrawal uniqueID to update your business logic
				} else if (request.state == 5 || request.state == 11) { // CallbackStateFailed || CallbackStateInChainFailed
						// withdrawal failed, use the withdrawal unique ID to update your business logic
				}
			} else if (request.type == 4) { // AirdropCallback
				//
				// airdrop unique ID
				String uniqueID = String.format("%s_%d", request.txId, request.vOutIndex);
				//
				if (request.processingState == 2) { // ProcessingStateDone
						// airdrop succeeded, use the airdrop unique ID to update your business logic
				}
			}
			// reply 200 OK to confirm the callback has been processed
			return new ResponseEntity<>("OK", HttpStatus.OK);
		} catch (Exception e) {
			logger.warning(String.format("callback failed with exception %s", e.toString()));
			return new ResponseEntity<>(new BaseResponse(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/v1/mock/wallets/withdrawal/callback")
	public HttpEntity<Object> withdrawalCallback(@RequestHeader("X-CHECKSUM") String checkSum,
			@RequestBody String bodyString) {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			WithdrawTransaction.Request request = mapper.readValue(bodyString, WithdrawTransaction.Request.class);

			// How to verify:
			// 1. Try to find corresponding API secret by request.Requests[0].orderID
			// 2. Calculate checksum then compare to X-CHECKSUM header (refer to sample code
			// bellow)
			// 3. If these two checksums match and the request is valid in your system,
			// reply 200, "OK" otherwise reply 400 to decline the withdrawal

			// sample code to calculate checksum and verify
			// final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			// digest.reset();
			// digest.update(payload.getBytes("utf8"));
			// final Base64.Encoder encoder = Base64.getUrlEncoder();
			// final String checksumVerf = encoder.encodeToString(digest.digest());

			// if (!checksumVerf.equals(checkSum)) {
			// logger.info(String.format("callback expect checkSum %s, got %s",
			// checksumVerf, checkSum));
			// throw new Exception("Bad checksum");
			// }

			logger.info(String.format("Withdraw Callback %s", bodyString));
			return new ResponseEntity<>("OK", HttpStatus.OK);
		} catch (Exception e) {
			logger.warning(String.format("callback failed with exception %s", e.toString()));
			return new ResponseEntity<>(new BaseResponse(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/v1/mock/wallets/{walletId}/collection/notifications/manual")
	public HttpEntity<String> resendDepositCollectionCallbacks(@PathVariable("walletId") long walletId,
			@RequestBody String request) {
		try {
			Api.Response response = apiClient.makeRequest(walletId, "POST",
					String.format("/v1/sofa/wallets/%d/collection/notifications/manual", walletId), null, request);

			return new ResponseEntity<String>(response.getContent(), response.getStatus());

		} catch (Exception e) {
			logger.warning(String.format("resendDepositCollectionCallbacks of wallet %d failed %s", walletId, e.toString()));
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/v1/mock/wallets/{walletId}/sender/transactions")
	public HttpEntity<String> withdrawTransactions(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/sender/transactions", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/sender/transactions/{orderId}/cancel")
	public HttpEntity<String> cancelWithdrawTransactions(@PathVariable("walletId") long walletId,
			@PathVariable("orderId") String orderId, @RequestBody String request) {
		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/sender/transactions/%s/cancel", walletId, orderId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/sender/transactions/{orderId}")
	public HttpEntity<String> getWithdrawTransactionState(@PathVariable("walletId") long walletId,
			@PathVariable("orderId") String orderId) {
		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/sender/transactions/%s", walletId, orderId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/sender/transactions/{orderId}/all")
	public HttpEntity<String> getWithdrawTransactionStateAll(@PathVariable("walletId") long walletId,
			@PathVariable("orderId") String orderId) {
		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/sender/transactions/%s/all", walletId, orderId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/sender/balance")
	public HttpEntity<String> getWithdrawalWalletBalance(@PathVariable("walletId") long walletId) {
		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/sender/balance", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/apisecret")
	public HttpEntity<String> getTxAPITokenStatus(@PathVariable("walletId") long walletId) {
		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/apisecret", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/notifications")
	public HttpEntity<String> getNotifications(@PathVariable("walletId") Long walletId,
			@RequestParam("from_time") Long fromTime, @RequestParam("to_time") Long toTime,
			@RequestParam("type") Integer type) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/notifications", walletId),
				new String[] { String.format("from_time=%d", fromTime), String.format("to_time=%d", toTime),
						String.format("type=%d", type) },
				null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/notifications/get_by_id")
	public HttpEntity<String> getNotificationsById(@PathVariable("walletId") Long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/notifications/get_by_id", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/receiver/notifications/txid/{txId}/{voutIndex}")
	public HttpEntity<String> getDepositCallback(@PathVariable("walletId") Long walletId,
			@PathVariable("txId") String txId, @PathVariable("voutIndex") Integer voutIndex) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/receiver/notifications/txid/%s/%d", walletId, txId, voutIndex), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/sender/notifications/order_id/{orderId}")
	public HttpEntity<String> getWithdrawalCallback(@PathVariable("walletId") Long walletId,
			@PathVariable("orderId") String orderId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/sender/notifications/order_id/%s", walletId, orderId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/transactions")
	public HttpEntity<String> getTransactionHistory(@PathVariable("walletId") long walletId,
			@RequestParam("from_time") Long fromTime, @RequestParam("to_time") Long toTime,
			@RequestParam(name = "start_index", defaultValue = "0") Integer startIndex,
			@RequestParam(name = "request_number", defaultValue = "0") Integer requestNumber,
			@RequestParam(name = "state", defaultValue = "-1") Integer state) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/transactions", walletId),
				new String[] { String.format("from_time=%d", fromTime), String.format("to_time=%d", toTime),
						String.format("start_index=%d", startIndex), String.format("request_number=%d", requestNumber),
						String.format("state=%d", state) },
				null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/blocks")
	public HttpEntity<String> getWalletBlockInfo(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/blocks", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/addresses/invalid-deposit")
	public HttpEntity<String> getInvalidDepositAddresses(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/addresses/invalid-deposit", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/info")
	public HttpEntity<String> getWalletInfo(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/info", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/addresses/verify")
	public HttpEntity<String> verifyAddresses(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/addresses/verify", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/autofee")
	public HttpEntity<String> getAutoFee(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/autofee", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/pooladdress/balance")
	public HttpEntity<String> getPoolAddressBalance(@PathVariable("walletId") Long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/pooladdress/balance", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/apisecret/activate")
	public HttpEntity<String> activateAPIToken(@PathVariable("walletId") Long walletId) {

		String url;
		if (walletId == 0) {
			url = "/v1/sofa/wallets/readonly/apisecret/activate";
		} else {
			url = String.format("/v1/sofa/wallets/%d/apisecret/activate", walletId);
		}
		Api.Response response = apiClient.makeRequest(walletId, "POST", url, null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/receiver/balance")
	public HttpEntity<String> getDepositWalletBalance(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/receiver/balance", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/vault/balance")
	public HttpEntity<String> getVaultWalletBalance(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/vault/balance", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/addresses/contract_txid")
	public HttpEntity<String> getDeployedContractCollectionAddresses(@PathVariable("walletId") long walletId,
			@RequestParam("txids") String txids) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/addresses/contract_txid", walletId),
				new String[] { String.format("txids=%s", txids) }, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
	
	@PostMapping("/v1/mock/wallets/{walletId}/sender/transactions/acl")
	public HttpEntity<String> setWithdrawalACL(@PathVariable("walletId") Long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/sender/transactions/acl", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
	
	@PostMapping("/v1/mock/wallets/{walletId}/sender/notifications/manual")
	public HttpEntity<String> resendWithdrawalCallbacks(@PathVariable("walletId") long walletId,
			@RequestBody String request) {
		try {
			Api.Response response = apiClient.makeRequest(walletId, "POST",
					String.format("/v1/sofa/wallets/%d/sender/notifications/manual", walletId), null, request);

			return new ResponseEntity<String>(response.getContent(), response.getStatus());

		} catch (Exception e) {
			logger.warning(String.format("resendWithdrawalCallbacks of wallet %d failed %s", walletId, e.toString()));
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/v1/mock/wallets/{walletId}/refreshsecret")
	public HttpEntity<String> refreshSecret(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/refreshsecret", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/sender/whitelist")
	public HttpEntity<String> getSenderWhitelist(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/sender/whitelist", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/sender/whitelist")
	public HttpEntity<String> addSenderWhitelist(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/sender/whitelist", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@DeleteMapping("/v1/mock/wallets/{walletId}/sender/whitelist")
	public HttpEntity<String> removeSenderWhitelist(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "DELETE",
				String.format("/v1/sofa/wallets/%d/sender/whitelist", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/sender/whitelist/config")
	public HttpEntity<String> querySenderWhitelistConfig(@PathVariable("walletId") long walletId) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/sender/whitelist/config", walletId), null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/sender/whitelist/check")
	public HttpEntity<String> checkSenderWhitelist(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/sender/whitelist/check", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/addresses/label")
	public HttpEntity<String> updateDepositWalletAddressLabel(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/addresses/label", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/addresses/get_labels")
	public HttpEntity<String> getDepositWalletAddressesLabel(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/addresses/get_labels", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/readonly/walletlist")
	public HttpEntity<String> getReadOnlyWalletList() {

		Api.Response response = apiClient.makeRequest(0L, "GET",
				"/v1/sofa/wallets/readonly/walletlist", null, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
	
	@PostMapping("/v1/mock/wallets/{walletId}/notifications/inspect")
	public HttpEntity<String> notificationsInspect(@PathVariable("walletId") long walletId,
			@RequestBody String request) {
		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/notifications/inspect", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/sender/transactions")
	public HttpEntity<String> getSenderTransactionHistory(@PathVariable("walletId") long walletId,
			@RequestParam(name = "start_index", defaultValue = "0") Integer startIndex,
			@RequestParam(name = "request_number", defaultValue = "10") Integer requestNumber,
			@RequestParam(name = "from_time", defaultValue = "0") Long fromTime,
			@RequestParam(name = "to_time", defaultValue = "") Long toTime) {

		Api.Response response = apiClient
				.makeRequest(
						walletId, "GET", String.format("/v1/sofa/wallets/%d/sender/transactions", walletId), new String[] {
								String.format("start_index=%d", startIndex), String.format("request_number=%d", requestNumber),
								String.format("from_time=%d", fromTime), String.format("to_time=%d", toTime),},
						null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
	
	@PostMapping("/v1/mock/wallets/{walletId}/autofees")
	public HttpEntity<String> getAutoFees(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/autofees", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/signmessage")
	public HttpEntity<String> signMessage(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/signmessage", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/{walletId}/contract/read")
	public HttpEntity<String> callContractRead(@PathVariable("walletId") long walletId,
			@RequestParam(name = "contract", defaultValue = "") String contract,
			@RequestParam(name = "data", defaultValue = "") String data) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/contract/read", walletId),
				new String[] { String.format("contract=%s", contract), String.format("data=%s", data) }, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
	
	@GetMapping("/v1/mock/wallets/{walletId}/sender/transactions/eventlog")
	public HttpEntity<String> callContractRead(@PathVariable("walletId") long walletId,
			@RequestParam(name = "txid", defaultValue = "") String txid) {

		Api.Response response = apiClient.makeRequest(walletId, "GET",
				String.format("/v1/sofa/wallets/%d/sender/transactions/eventlog", walletId), new String[] {
					String.format("txid=%s", txid) },
				null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@GetMapping("/v1/mock/wallets/readonly/walletlist/balances")
	public HttpEntity<String> getReadOnlyWalletListBalances(
		@RequestParam(name = "type", defaultValue = "-1") Integer type,
		@RequestParam(name = "start_index", defaultValue = "0") Integer startIndex,
		@RequestParam(name = "request_number", defaultValue = "0") Integer requestNumber) {

		Api.Response response = apiClient.makeRequest(0L, "GET",
				"/v1/sofa/wallets/readonly/walletlist/balances",
				new String[] { String.format("type=%d", type),
					String.format("start_index=%d", startIndex),
					String.format("request_number=%d", requestNumber),}, null);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}

	@PostMapping("/v1/mock/wallets/{walletId}/receiver/addresses/verify")
	public HttpEntity<String> verifyDepositAddresses(@PathVariable("walletId") long walletId,
			@RequestBody String request) {

		Api.Response response = apiClient.makeRequest(walletId, "POST",
				String.format("/v1/sofa/wallets/%d/receiver/addresses/verify", walletId), null, request);

		return new ResponseEntity<String>(response.getContent(), response.getStatus());
	}
}
