#!/usr/bin/env bash
#
# Manual smoke test for LedgerController endpoints.
#
# ASSUMPTIONS (adjust to match your actual DTOs if these are wrong):
#   TenantRegistrationDto  -> tenantName, accountNumber... (see registerTenant below)
#   LoginRequestDto        -> email, password
#   AuthResponseDto        -> { "token": "..." }
#   CreateAccountDto       -> accountNumber, name, type, currency
#   CreateTransactionDto   -> narration, lines: [{ accountId/accountNumber, type, amount }]
#
# Requires: curl, jq (for parsing the JWT out of the login response)
#   macOS:  brew install jq
#   Ubuntu: sudo apt-get install jq
#
# Usage:
#   chmod +x test-ledger-api.sh
#   ./test-ledger-api.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="$BASE_URL/api/v1"

echo "=================================================="
echo "1. Register a new tenant (unauthenticated)"
echo "=================================================="
curl -i -X POST "$API/onboarding/register" \
  -H "Content-Type: application/json" \
  -d '{
        "tenantName": "Acme Fintech",
        "email": "admin@acme.test",
        "password": "S3curePass!",
        "firstName": "Ada",
        "lastName": "Lovelace"
      }'
echo -e "\n"

echo "=================================================="
echo "2. Log in and capture the Bearer token"
echo "=================================================="
LOGIN_RESPONSE=$(curl -s -X POST "$API/onboarding/login" \
  -H "Content-Type: application/json" \
  -d '{
        "email": "admin@acme.test",
        "password": "S3curePass!"
      }')

echo "Login response: $LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')

if [[ "$TOKEN" == "null" || -z "$TOKEN" ]]; then
  echo "ERROR: Could not extract token from login response. Check the field name (assumed 'token')."
  exit 1
fi

echo "Captured token: $TOKEN"
echo -e "\n"

echo "=================================================="
echo "3. Create an account (authenticated)"
echo "=================================================="
curl -i -X POST "$API/ledgers/accounts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "accountNumber": "1000-CASH",
        "name": "Cash Account",
        "type": "ASSET",
        "currency": "USD"
      }'
echo -e "\n"

curl -i -X POST "$API/ledgers/accounts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "accountNumber": "4000-REV",
        "name": "Revenue Account",
        "type": "REVENUE",
        "currency": "USD"
      }'
echo -e "\n"

echo "=================================================="
echo "4. Post a balanced double-entry transaction (authenticated)"
echo "=================================================="
curl -i -X POST "$API/ledgers/transactions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "narration": "Initial cash sale",
        "lines": [
          { "accountNumber": "1000-CASH", "type": "DEBIT",  "amount": 2500 },
          { "accountNumber": "4000-REV",  "type": "CREDIT", "amount": 2500 }
        ]
      }'
echo -e "\n"

echo "=================================================="
echo "5. Negative test: missing Authorization header -> expect 401"
echo "=================================================="
curl -i -X POST "$API/ledgers/accounts" \
  -H "Content-Type: application/json" \
  -d '{
        "accountNumber": "9999-TEST",
        "name": "Should Be Rejected",
        "type": "ASSET",
        "currency": "USD"
      }'
echo -e "\n"

echo "=================================================="
echo "6. Negative test: unbalanced transaction -> expect 4xx/5xx"
echo "=================================================="
curl -i -X POST "$API/ledgers/transactions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "narration": "Unbalanced (should fail)",
        "lines": [
          { "accountNumber": "1000-CASH", "type": "DEBIT",  "amount": 1000 },
          { "accountNumber": "4000-REV",  "type": "CREDIT", "amount": 999  }
        ]
      }'
echo -e "\n"

echo "Done."
