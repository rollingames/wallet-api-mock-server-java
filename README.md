<a name="table-of-contents"></a>
## Table of contents

- [Get Started](#get-started)
- [Deposit Wallet API](#deposit-wallet-api)
- [Withdraw Wallet API](#withdraw-wallet-api)
- [Query API](#query-api)
- Testing
	- [Mock Server](#mock-server)
	- [cURL Testing Commands](#curl-testing-commands)
	- [Other Language Versions](#other-language-versions)

<a name="get-started"></a>
# Get Started

Refer to [here](https://github.com/rollingames/wallet-api-mock-server-java#get-started) for detailed introduction.

<a name="deposit-wallet-api"></a>
# Deposit Wallet API

Refer to [here](https://github.com/rollingames/wallet-api-mock-server-java#create-deposit-wallet-addresses) for detailed API documentation.

<a name="withdraw-wallet-api"></a>
# Withdraw Wallet API

Refer to [here](https://github.com/rollingames/wallet-api-mock-server-java#withdraw) for detailed API documentation.

<a name="query-api"></a>
# Query API

Refer to [here](https://github.com/rollingames/wallet-api-mock-server-java#query-api-token-status) for detailed API documentation.

<a name="mock-server"></a>
# Mock Server

### How to compile
- ./mvnw spring-boot:run

### Setup configuration
>	Set following configuration in config/application.properties

```
api.server.url=
```

### Put wallet API code/secret into mock server
-	Get API code/secret on web console
	-	API-CODE, API-SECRET, WALLET-ID
- 	Put API code/secret to mock server's database

```
curl -X POST -H "Content-Type: application/json" -d '{"api_code":"API-CODE","api_secret":"API-SECRET"}' \
http://localhost:8889/v1/mock/wallets/{WALLET-ID}/apitoken
```

### Register mock server callback URL
>	Operate on web admin console

Notification Callback URL

```
http://localhost:8889/v1/mock/wallets/callback
```
Withdrawal Authentication Callback URL

```
http://localhost:8889/v1/mock/wallets/withdrawal/callback
```

> The withdrawal authentication callback URL once set, every withrawal request will callback this URL to get authentication to proceed withdrawal request.
> 
> Refer to *withdrawalCallback()* function in mock server MockController.java

<a name="curl-testing-commands"></a>
# cURL Testing Commands

Refer to [here](https://github.com/rollingames/wallet-api-mock-server-java#curl-testing-commands) for curl testing commands.

<a name="other-language-versions"></a>
# Other Language Versions
- [Go](https://github.com/rollingames/wallet-api-mock-server)
- [Javascript](https://github.com/rollingames/wallet-api-mock-server-javascript)
- [PHP](https://github.com/rollingames/wallet-api-mock-server-php)

