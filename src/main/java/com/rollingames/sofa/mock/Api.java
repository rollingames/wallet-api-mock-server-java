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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.rollingames.sofa.api.BaseResponse;
import com.rollingames.sofa.mock.entity.ApiToken;
import com.rollingames.sofa.mock.repository.ApiTokenRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class Api {
    private static Logger logger = Logger.getLogger(Api.class.getName());

    @Value("${api.server.url}")
    String apiServerUrl;

    @Autowired
    ApiTokenRepository repository;

    static String getAlphaNumericString(final int n) {

        final String alphaNumericString = "abcdefghijklmnopqrstuvxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        final StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            final int index = (int) (alphaNumericString.length() * Math.random());
            sb.append(alphaNumericString.charAt(index));
        }

        return sb.toString();
    }

    static String sha256(final String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        digest.update(input.getBytes("utf8"));
        return String.format("%064x", new BigInteger(1, digest.digest()));
    }

    static String buildChecksum(final List<String> paramList, final String secret, final long time, final String r)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        paramList.add(String.format("t=%d", time));
        paramList.add(String.format("r=%s", r));
        Collections.sort(paramList);
        paramList.add(String.format("secret=%s", secret));
        return sha256(String.join("&", paramList));
    }

    public Response makeRequest(final Long targetId, final String method, final String api, final String[] params,
            final String postBody) {
        try {
            if (apiServerUrl == null || apiServerUrl.isEmpty()) {
                throw new Exception("Please config your api.server.url in config/application.properties");
            }

            ApiToken apiToken = findApiTokenByWalletId(targetId);
            if (apiToken.getWalletId() < 0) {
                return new Response(HttpStatus.BAD_REQUEST, "Missing api token");
            }
            if (method.length() <= 0) {
                return new Response(HttpStatus.BAD_REQUEST, "Empty method");
            }
            if (api.length() <= 0) {
                return new Response(HttpStatus.BAD_REQUEST, "Empty url");
            }

            final String r = getAlphaNumericString(8);
            if (r.length() <= 0) {
                return new Response(HttpStatus.BAD_REQUEST, "Fail to random r");
            }
            final long t = System.currentTimeMillis() / 1000;

            String path = String.format("%s%s?t=%d&r=%s", apiServerUrl, api, t, r);

            List<String> paramList = new ArrayList<String>();
            if (params != null) {
                for (String p : params) {
                    paramList.add(p);
                }
            }

            if (!paramList.isEmpty()) {
                path += String.format("&%s", String.join("&", paramList));
            }

            final URL url = new URL(path);
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod(method);
            if (postBody != null) {
                paramList.add(postBody);
            }

            con.setRequestProperty("X-API-CODE", apiToken.getApiCode());

            final String checkSum = buildChecksum(paramList, apiToken.getApiSecret(), t, r);
            con.setRequestProperty("X-CHECKSUM", checkSum);

            if (postBody != null) {
                con.setRequestProperty("Content-Type", "application/json");
            }
            con.setRequestProperty("User-Agent", "golang");

            con.setConnectTimeout(5000);
            con.setReadTimeout(90000);

            if (postBody != null && postBody.length() > 0) {
                // send post body
                con.setDoOutput(true);
                final OutputStream out = con.getOutputStream();
                out.write(postBody.getBytes());
                out.flush();
                out.close();
            } else {
                con.connect();
            }

            final int status = con.getResponseCode();

            InputStream inputStream;
            if (status != 200) {
                inputStream = con.getErrorStream();
            } else {
                inputStream = con.getInputStream();
            }

            if (inputStream == null) {
                throw new IOException("invalid input stream");
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine;
            final StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            if (status != 200) {
                logger.warning(String.format("ApiClient.Request, url: %s, status: %d", url, status));
            }

            return new Response(HttpStatus.resolve(status), content.toString());
        } catch (final Exception e) {
            e.printStackTrace();
            logger.warning(String.format("makeRequest with exception %s", e.toString()));
            try {
                final ObjectMapper mapper = new ObjectMapper();
                String errObject = mapper.writeValueAsString(new BaseResponse(e));
                return new Response(HttpStatus.SERVICE_UNAVAILABLE, errObject);
            } catch (final Exception e2) {
                return new Response(HttpStatus.SERVICE_UNAVAILABLE, e.toString());
            }
        }
    }

    private ApiToken findApiTokenByWalletId(long targetId) throws Exception {
        Optional<ApiToken> optApiToken = repository.findById(targetId);
        if (!optApiToken.isPresent()) {
            // try read-only API token
            optApiToken = repository.findById(0L);
            if (!optApiToken.isPresent()) {
                throw new Exception("Missing api token of wallet " + targetId);
            }
        }
        return optApiToken.get();
    }

    public static class Response {
        private static Logger logger = Logger.getLogger(Api.Response.class.getName());
        private final HttpStatus status;
        private final String content;

        Response(final HttpStatus status, final String content) {
            this.status = status;
            this.content = content;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getContent() {
            return content;
        }

        public <T> T deserialize(final Class<T> valueType) {
            String text = content;
            if (text.length() <= 0) {
                text = "{}";
            }
            try {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return mapper.readValue(text, valueType);
            } catch (Exception e) {
                logger.warning(String.format("Api.Response deserialize failed %s", e.toString()));
                return null;
            }
        }
    }
}
