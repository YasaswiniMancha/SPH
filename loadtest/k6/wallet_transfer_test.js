import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 200 },
    { duration: '2m', target: 500 },
    { duration: '2m', target: 1000 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083'; // Wallet service default port

export default function () {
  const payload = JSON.stringify({
    fromWalletId: '00000000-0000-0000-0000-000000000000',
    toWalletId: '11111111-1111-1111-1111-111111111111',
    amount: 1.00,
    currency: 'USD',
    description: 'k6-load-test'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${__ENV.TEST_TOKEN || 'dummy-token'}`
    },
  };

  const res = http.post(`${BASE_URL}/api/v1/wallet/transfer`, payload, params);
  check(res, {
    'status is 200': (r) => r.status === 200 || r.status === 201 || r.status === 202,
  });
  sleep(0.1);
}

