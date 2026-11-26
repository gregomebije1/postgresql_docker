# What was built
- The E-Wallet Orchestrator Engine: A customer-facing, assetless pass-through engine that leases balances, holds no physical cash custody, and delegates funding/payout actions to an external gateway.

- The Payment Gateway Clearing Engine: An infrastructure pipeline that manages incoming processor receivables, calculates basis-point transaction fee splits, and drives merchant settlements.


Charts of Accounts and Entry Templates

Book
wallet (Wallet Service)	
W010 Settlement Bank (A),
W100 Customer Available (L),  sub_ref: the customer's wallet id on W100
W110 Customer Held (L),  sub_ref: the customer's wallet id on W110
W200 Fee Revenue (R), 
W210 Gateway Fees Expense (E),
W300 Funding Receivable (A), 
W400 Vendor Payout Clearing (A)	
1230, 2210, 2220, 4210, 5210, 1210, 1220

gateway (Gateway Service)	
G100 Processor Receivable (A), 
G110 Gateway Bank (A), 
G200 Merchant Settlement Payable (L),  sub_ref: the merchant id on G200
G300 Processing Fee Revenue (R), 
G400 Interchange Expense (E)	
1110, 1120, 2110, 4110, 5110



Id	Flow step (link)	    Queued by	Book	Debit	Credit
e1	A3 Deposit captured	    Wallet	wallet	    W300 $103.00	W100[w1] $100.00; W200 $3.00
e2	A6 Settlement received	Wallet	wallet	    W010 $101.50; W210 $1.50	W300 $103.00
e4	A5 Settle and pay out	Gateway	gateway	    G110 $103.00; G200 $101.50	G100 $103.00; G110 $101.50
e3	A2 Charge captured	    Gateway	gateway	    G100 $103.00	G200 $101.50; G300 $1.50
e5	B P2P transfer	Wallet	wallet	W100[w1] $25.00	W100[w2] $25.00
e6	C1 Hold placed	Wallet	wallet	W100[w1] $10.00	W110[w1] $10.00
e13	G1 Vendor payout sent	Gateway	gateway	G200 $10.00	G110 $10.00
e7	C2a Payout done	Wallet	wallet	W110[w1] $10.00	W400 $10.00
e14	G2b Vendor payout reversed	Gateway	gateway	G110 $10.00	G200 $10.00
e8	C2b Hold released	    Wallet	wallet	W110[w1] $10.00	W100[w1] $10.00
e9	M1 Merchant capture	    Gateway	gateway	G100 $100.00	G200[m1] $97.20; G300 $2.80
e10	M2 Network settlement	Gateway	gateway	G110 $98.90; G400 $1.10	G100 $100.00
e11	M3 Merchant withdrawal	Gateway	gateway	G200[m1] $97.20	G110 $97.20
e12	M3b Withdrawal reversed Gateway	gateway	G110 $97.20	G200[m1] $97.20


\set w1   '''a1000000-0000-0000-0000-000000000001'''
\set w2   '''a1000000-0000-0000-0000-000000000002'''
\set dep1 '''d0000000-0000-0000-0000-000000000001'''
\set p2p1 '''d0000000-0000-0000-0000-000000000002'''
\set vas1 '''d0000000-0000-0000-0000-000000000003'''
\set msg1 '''e0000000-0000-0000-0000-000000000001'''
\set msg2 '''e0000000-0000-0000-0000-000000000002'''
\set msg3 '''e0000000-0000-0000-0000-000000000003'''
\set msg4 '''e0000000-0000-0000-0000-000000000004'''
\set e1   '''f0000000-0000-0000-0000-000000000001'''
-- ... \set e2 to \set e14 follow the same pattern (last digits 02 to 14); each entry id must be unique


# Ledger Service
curl -X POST "http://localhost:8080/api/ledger/v1/onboarding/register" \
-H "Content-Type: application/json" \
-d '{
"adminEmail": "johndoe@mangital.com",
"adminPassword": "password",
"firstName": "John",
"lastName": "Doe"
}'



curl -X POST "http://localhost:8080/api/ledger/v1/onboarding/tenant" \
-H "Content-Type: application/json" \
-d '{
"tenantId": "tenant-1",
"tenantName": "Mangital ltd",
"email": "johndoe@mangital.com"
}'



curl -X POST "http://localhost:8080/api/ledger/v1/onboarding/login" \
-H "Content-Type: application/json" \
-d '{
"tenantId": "tenant-1",
"email": "johndoe@mangital.com",
"password": "password"
}'


curl -X POST "http://localhost:8080/api/ledger/v1/accounts" \
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huZG9lQG1hbmdpdGFsLmNvbSIsInRlbmFudF9pZCI6InRlbmFudC0xIiwiaWF0IjoxNzkzNTk0MzkxLCJleHAiOjE3OTM1OTc5OTF9.Jhi9DCvnpaYgMYjt3aYh3ZZ_V2R9qe-8BPDgAoeyMJw" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"accountNumber": "1001", 
     "name": "Alice wallet",
     "type": "ASSET",
     "currency": "USD"}'


curl -X POST "http://localhost:8080/api/ledger/v1/accounts" \
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huZG9lQG1hbmdpdGFsLmNvbSIsInRlbmFudF9pZCI6InRlbmFudC0xIiwiaWF0IjoxNzkzNTk0MzkxLCJleHAiOjE3OTM1OTc5OTF9.Jhi9DCvnpaYgMYjt3aYh3ZZ_V2R9qe-8BPDgAoeyMJw" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"accountNumber": "1002", 
     "name": "Bob wallet",
     "type": "ASSET",
     "currency": "USD"}'


curl -X POST "http://localhost:8080/api/ledger/v1/accounts" \
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huZG9lQG1hbmdpdGFsLmNvbSIsInRlbmFudF9pZCI6InRlbmFudC0xIiwiaWF0IjoxNzkzNTk0MzkxLCJleHAiOjE3OTM1OTc5OTF9.Jhi9DCvnpaYgMYjt3aYh3ZZ_V2R9qe-8BPDgAoeyMJw" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"accountNumber": "3001", 
     "name": "Equity (USD)",
     "type": "EQUITY",
     "currency": "USD"}'



curl -X POST "http://localhost:8080/api/ledger/v1/transactions" \
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huZG9lQG1hbmdpdGFsLmNvbSIsInRlbmFudF9pZCI6InRlbmFudC0xIiwiaWF0IjoxNzkzNjAxNzQ3LCJleHAiOjE3OTM2MDUzNDd9.QXacmoPJV7pE9BHLoqbTp8dhEuEG8CT5Uhn6mW3MO8g" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{
  "description": "Fund Alice",
  "idempotencyKey": "fund-alice-1"
  "lines": [
    {
      "accountId": "d880a0fe-ce74-4877-98f9-73dbb2ffe46b",
      "",
      "debit": 100000,
      "credit": 0
    },
    {
      "accountId": "40eb54c1-291b-4986-a51e-00bf2e99a40e",
      "",
      "debit": 0,
      "credit": 100000
    }
  ],
  
}'
====
## POST /ledger/v1/entries, called by the Wallet (A3 queues it) and by the Gateway (A2 queues it). The book comes from the caller's identity.

-- from the Wallet (entry e1)
POST /ledger/v1/entries
Authorization: Bearer wallet-service-token
Idempotency-Key: f0000000-…-0001

{ "entry_id": "f0000000-…-0001", "book": "wallet", "memo": "Deposit captured",
  "lines": [ {"account": "W300", "debit": 10300},
             {"account": "W100", "sub_ref": "a1000000-…-0001", "credit": 10000},
             {"account": "W200", "credit": 300} ] }

201 Created  {"entry_id": "f0000000-…-0001", "status": "posted", "seq": 1042}

-- from the Gateway (entry e3)
POST /ledger/v1/entries
Authorization: Bearer gateway-service-token
Idempotency-Key: f0000000-…-0003

{ "entry_id": "f0000000-…-0003", "book": "gateway", "memo": "Wallet funding captured",
  "lines": [ {"account": "G100", "debit": 10300},
             {"account": "G200", "credit": 10150},
             {"account": "G300", "credit": 150} ] }

201 Created  {"entry_id": "f0000000-…-0003", "status": "posted", "seq": 1043}
Responses a caller must be ready for:

200 OK     header Idempotent-Replayed: true    -- same id, same body (a retry): same answer as before
409        {"error": {"code": "idempotency_conflict", "message": "entry id reused with a different body"}}
422        {"error": {"code": "unbalanced", "message": "debits 10300, credits 10000"}}
422        {"error": {"code": "unknown_account", "message": "no account W999 in book wallet"}}
403        {"error": {"code": "book_not_allowed", "message": "client may only write book gateway"}}

## POST /ledger/v1/entries:batch (up to 100 independent entries; one result per entry, never all-or-nothing).

POST /ledger/v1/entries:batch
Authorization: Bearer wallet-service-token

{ "entries": [ { "entry_id": "f0000000-…-0006", "book": "wallet", "memo": "Hold placed",
                 "lines": [ {"account": "W100", "sub_ref": "a1000000-…-0001", "debit": 1000},
                            {"account": "W110", "sub_ref": "a1000000-…-0001", "credit": 1000} ] },
               { "entry_id": "f0000000-…-0099", "book": "wallet", "memo": "Broken",
                 "lines": [ {"account": "W100", "debit": 500} ] } ] }

200 OK
{ "results": [ {"entry_id": "f0000000-…-0006", "status": "posted", "seq": 1044},
               {"entry_id": "f0000000-…-0099", "status": "rejected", "error": "unbalanced"} ] }

## POST /ledger/v1/entries/{id}/reversal (11.7). The new entry id is supplied by the caller.

POST /ledger/v1/entries/f0000000-…-000b/reversal
Idempotency-Key: f0000000-…-000c

{ "entry_id": "f0000000-…-000c", "reason": "bank rejected the withdrawal" }

201 Created  {"entry_id": "f0000000-…-000c", "reversal_of": "f0000000-…-000b", "status": "posted", "seq": 1051}
409          {"error": {"code": "already_reversed", "message": "entry already has a reversal"}}
404          {"error": {"code": "not_found"}}   -- also returned for an entry in a book the caller may not access
GET /ledger/v1/entries/{id}

## GET /ledger/v1/entries/f0000000-…-0001

200 OK
{ "entry_id": "f0000000-…-0001", "book": "wallet", "memo": "Deposit captured",
  "seq": 1042, "posted_at": "2026-11-20T10:15:03Z", "reversal_of": null,
  "lines": [ {"account": "W300", "debit": 10300},
             {"account": "W100", "sub_ref": "a1000000-…-0001", "credit": 10000},
             {"account": "W200", "credit": 300} ] }
GET /ledger/v1/balances (reconciler; read access to both books).

GET /ledger/v1/balances?book=wallet&account=W100&sub_ref=a1000000-…-0001

200 OK
{ "book": "wallet", "account": "W100", "sub_ref": "a1000000-…-0001",
  "balance": 10000, "normal_side": "credit", "as_of_seq": 1042, "computed_from": "snapshot+delta" }

-- add &as_of=2026-11-19T23:59:59Z for a historical balance

## GET /ledger/v1/accounts/{code}/lines (statement, paged).

GET /ledger/v1/accounts/W100/lines?book=wallet&sub_ref=a1000000-…-0001&limit=2

200 OK
{ "lines": [ {"entry_id": "f0000000-…-0001", "seq": 1042, "credit": 10000, "debit": 0},
             {"entry_id": "f0000000-…-0005", "seq": 1047, "credit": 0, "debit": 2500} ],
  "next_cursor": "eyJzZXEiOjEwNDd9" }


# Gateway Service

curl -i -X POST http://localhost:8081/api/ledger/v1/onboarding/user \
  -H "Content-Type: application/json" \
  -d '{
    "email": "payment_gateway_tenant@mangital.com",
    "password": "password",
    "firstName": "John",
    "lastName": "Doe"
  }'
  

curl -i -X POST "http://localhost:8081/api/ledger/v1/onboarding/tenant" \
-H "Content-Type: application/json" \
-d '{
"tenantId": "payment_gateway_tenant",
"tenantName": "Mangital ltd",
"email": "payment_gateway_tenant@mangital.com"
}'


curl -X POST "http://localhost:8081/api/ledger/v1/onboarding/login" \
-H "Content-Type: application/json" \
-d '{
"tenantId": "payment_gateway_tenant",
"email": "payment_gateway_tenant@mangital.com",
"password": "password"
}'


curl -X GET "http://localhost:8082/api/gateway/v1/setup" 

curl -X POST "http://localhost:8082/api/v1/gateway/onboarding/register" \
-H "Content-Type: application/json" \
-d '{
    "tenantId": "wallet_tenant",
    "tenantName": "Wallet Tenant",
    "email": "payment_gateway_tenant@mangital.com",
    "webhookUrl": "http://localhost:8080/api/wallet/v1/webhooks/gateway",
    "token": "token-1",
}'

## Charge a card, 
curl -i -X POST "http://localhost:8082/api/gateway/v1/charges" \
-H "Authorization: Bearer wallet-service-token" \
-H "Content-Type: application/json" \
-H "idempotencyKey": "d0000000-0000-0000-0000-000000000001" \
-d '{ 
        "charge_id": "d0000000-…-0001", 
        "amount": 10300, 
        "currency": "USD", 
        "card_token": "tok_4242" 
    }'

201 Created
{ "charge_id": "d0000000-…-0001", "status": "CAPTURED", "amount": 10300, "fee": 150, "net": 10150 }

402 Payment Required  {"error": {"code": "card_declined", "message": "insufficient funds on card"}}
-- replay: 200 OK with the original body and Idempotent-Replayed: true


## GET /gateway/v1/charges/{id} (called by the Wallet's sweeper, 7.3, when a charge's outcome is unknown).

GET /gateway/v1/charges/d0000000-…-0001

200 OK  {"charge_id": "d0000000-…-0001", "status": "SETTLED", "amount": 10300, "fee": 150}
404     {"error": {"code": "not_found"}}   -- the charge request never arrived: safe to mark FAILED


## POST /gateway/v1/payments (merchant checkout; M1).

POST /gateway/v1/payments
Authorization: Bearer merchant-api-key
Idempotency-Key: c0000000-…-0001

{ "merchant_id": "b0000000-…-0001", "amount": 10000, "currency": "USD", "card_token": "tok_4242" }

201 Created
{ "payment_id": "c0000000-…-0001", "status": "CAPTURED",
  "amount": 10000, "fee": 280, "merchant_net": 9720 }


## POST /gateway/v1/merchants/{merchant_id}/payouts (merchant withdrawal; M3).

POST /gateway/v1/merchants/b0000000-…-0001/payouts
Idempotency-Key: c0000000-…-0002

{ "amount": 9720, "currency": "USD", "bank_account_id": "ba_77" }

202 Accepted  {"payout_id": "c0000000-…-0002", "status": "PENDING", "amount": 9720}
422           {"error": {"code": "insufficient_funds", "message": "available 5000, required 9720"}}


## POST /gateway/v1/payouts (called by the Wallet relay to pay a vendor; G1).

POST /gateway/v1/payouts
Authorization: Bearer wallet-service-token
Idempotency-Key: d0000000-…-0003

{ "payout_id": "d0000000-…-0003", "amount": 1000, "currency": "USD",
  "destination": { "type": "vendor", "reference": "airtime-07700900123" } }

202 Accepted  {"payout_id": "d0000000-…-0003", "status": "PENDING"}


## POST /gateway/v1/webhooks/bank (called by the bank; G2a, G2b).

POST /gateway/v1/webhooks/bank
X-Signature: sha256=41ab…

{ "event": "transfer.paid", "transfer_id": "tr_5521", "reference": "d0000000-…-0003" }
{ "event": "transfer.failed", "transfer_id": "tr_5521", "reference": "d0000000-…-0003",
  "reason": "account_closed" }

200 OK {"received": true}    -- also 200 for a duplicate callback (status compare-and-set made it a n

## External dependency (not one of the three): the bank. Called from the Gateway's outbox in G1 and M3.

POST /bank/v1/transfers
Idempotency-Key: d0000000-…-0003

{ "payout_id": "d0000000-…-0003", "amount": 1000 }

202 Accepted  {"transfer_id": "tr_5521", "status": "PROCESSING"}   -- the result arrives via the webhook ab


# Wallet Service

curl -i -X POST http://localhost:8081/api/v1/onboarding/user \
  -H "Content-Type: application/json" \
  -d '{
    "email": "wallet_tenant@mangital.com",
    "password": "password",
    "firstName": "Wallet",
    "lastName": "Wallet"
  }'
  

curl -i -X POST "http://localhost:8081/api/v1/onboarding/tenant" \
-H "Content-Type: application/json" \
-d '{
"tenantId": "wallet_tenant",
"tenantName": "Wallet ltd",
"email": "wallet_tenant@mangital.com"
}'


curl -X POST "http://localhost:8081/api/v1/onboarding/login" \
-H "Content-Type: application/json" \
-d '{
"tenantId": "wallet_tenant",
"email": "wallet_tenant@mangital.com",
"password": "password"
}'


curl -X GET "http://localhost:8080/api/wallet/v1/setup" 


curl -i -X POST "http://localhost:8080/api/wallet/v1/register" \
-H "Content-Type: application/json" \
-d '{
    "email": "user_wallet_1@wallet.com",
    "password": "password"
}'

curl -i -X POST "http://localhost:8080/api/wallet/v1/login" \
-H "Content-Type: application/json" \
-d '{ 
        "email": "user_wallet_1@wallet.com",
        "password": "password"
}'

## POST /wallet/v1/deposits (client; handled by A1). The card token is passed through to the Gateway charge request (the A1 SQL omits it for brevity).

curl -i -X POST "http://localhost:8080/api/wallet/v1/deposit" \
-H "Content-Type: application/json" \
-H "Authorization: Bearer user-token" \
-H "X-Idempotency-Key: f0000000-0000-0000-0000-000000000001" \
-d '{ 
        "walletId": "4cc2317a-32dd-41a0-a61a-ba41f9f38a84", 
        "amount": 10000, 
        "currency": "USD", 
        "cardToken": "tok_4242" 
}'

 
202 Accepted
{ "transaction_id": "d0000000-…-0001", "type": "DEPOSIT", "status": "PENDING",
  "amount": 10000, "fee": 300, "total_charged": 10300, "created_at": "2026-11-20T10:15:00Z" }

-- same request again:  202 Accepted, header Idempotent-Replayed: true, identical body
-- amount 0:            422 {"error": {"code": "invalid_amount", "message": "amount must be positive"}}
-- card declined later: the transaction ends FAILED (see GET below), not an error here

## GET /wallet/v1/transactions/{id} (client polling; a read of wallet_txns).

GET /wallet/v1/transactions/d0000000-…-0001

200 OK  (after the charge.captured webhook was processed)
{ "transaction_id": "d0000000-…-0001", "type": "DEPOSIT", "status": "SUCCESS",
  "amount": 10000, "fee": 300, "wallet_id": "a1000000-…-0001" }

404 Not Found  {"error": {"code": "not_found", "message": "unknown transaction"}}

## POST /wallet/v1/transfers (client; Flow B, 4.5). Completes synchronously because both wallets are in the Wallet DB.

 POST /wallet/v1/transfers
Idempotency-Key: d0000000-…-0002

{ "from_wallet_id": "a1000000-…-0001", "to_wallet_id": "a1000000-…-0002",
  "amount": 2500, "currency": "USD" }

201 Created
{ "transaction_id": "d0000000-…-0002", "type": "P2P", "status": "SUCCESS", "amount": 2500 }

422 Unprocessable  (balance too low)
{ "error": {"code": "insufficient_funds", "message": "available 1800, required 2500"} }


## POST /wallet/v1/purchases (client; Flow C, C1). Money is held immediately; the vendor payout completes later.

POST /wallet/v1/purchases
Idempotency-Key: d0000000-…-0003

{ "wallet_id": "a1000000-…-0001", "amount": 1000, "currency": "USD",
  "vendor": { "type": "airtime", "reference": "07700900123" } }

202 Accepted
{ "transaction_id": "d0000000-…-0003", "type": "VAS", "status": "PENDING", "held": 1000 }

-- later, GET /wallet/v1/transactions/d0000000-…-0003 returns "status": "SUCCESS" or "FAILED"
422  {"error": {"code": "insufficient_funds", "message": "available 400, required 1000"}}


## POST /wallet/v1/webhooks/gateway (called by the Gateway; handled by A3, A6, C2a, C2b). The Idempotency-Key is the message id (the inbox key). The signature proves the sender.

POST /wallet/v1/webhooks/gateway
Idempotency-Key: e0000000-…-0001
X-Signature: sha256=9f2c…

{ "type": "charge.captured", "charge_id": "d0000000-…-0001" }          -- msg1, step A3
{ "type": "settlement.paid", "charge_id": "d0000000-…-0001",
  "gross": 10300, "fee": 150, "net": 10150 }                           -- msg2, step A6
{ "type": "payout.succeeded", "payout_id": "d0000000-…-0003" }          -- msg3, step C2a
{ "type": "payout.failed", "payout_id": "d0000000-…-0003",
  "reason": "vendor_unreachable" }                                       -- msg4, step C2b

200 OK  {"received": true}
-- duplicate delivery: 200 OK {"received": true, "duplicate": true}   (inbox guard, nothing changes)
-- bad signature:      401  {"error": {"code": "invalid_signature"}}
-- database error:     503  (Gateway's relay retries with the same key)
GET /wallet/v1/balances (reconciler only; compares with the Ledger, 4.8 and 5.2).

## GET /wallet/v1/balances?as_of=now

200 OK
{ "as_of": "2026-11-20T10:20:00Z",
  "wallets": [ { "wallet_id": "a1000000-…-0001", "available": 8000, "held": 0 },
               { "wallet_id": "a1000000-…-0002", "available": 2500, "held": 0 } ] }