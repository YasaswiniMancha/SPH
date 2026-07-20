import http from 'k6/http';
import { check } from 'k6';

// This script uses the arrival-rate executor to target 10,000 RPS
// Total transactions target: 1,000,000 -> sustain 10,000 RPS for 100 seconds (approx)

export const options = {
  scenarios: {
    constant_10k: {
      executor: 'constant-arrival-rate',
      rate: 10000, // iterations per second
      timeUnit: '1s',
      duration: '120s', // run a bit longer than 100s for warmup/teardown
      preAllocatedVUs: 800, // tune based on runner capacity
      maxVUs: 2000,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.02'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';
const AUTH_TOKEN = __ENV.TEST_TOKEN || 'dummy-token';

export default function () {
  const payload = JSON.stringify({
    fromWalletId: '00000000-0000-0000-0000-000000000000',
    toWalletId: '11111111-1111-1111-1111-111111111111',
    amount: 1.00,
    currency: 'USD',
    description: 'k6-10k-1M'
  });

  const res = http.post(`${BASE_URL}/api/v1/wallet/transfer`, payload, {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${AUTH_TOKEN}` },
    timeout: '60s',
  });

  check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });
}

