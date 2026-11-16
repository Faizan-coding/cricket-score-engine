// k6 load test for the cached read path.
// Run: k6 run -e MATCH=1 loadtest/read-score.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 200 },
    { duration: '30s', target: 1000 },
    { duration: '10s', target: 0 },
  ],
  thresholds: { http_req_duration: ['p(95)<100'] },
};

export default function () {
  const res = http.get(`http://localhost:8080/api/matches/${__ENV.MATCH || 1}`);
  check(res, { 'status 200': (r) => r.status === 200 });
}
