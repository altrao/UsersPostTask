import { check } from 'k6';
import http from 'k6/http';

const ipAddress = 'localhost'

export const options = {
  scenarios: {
    stress_test: {
      executor: 'ramping-arrival-rate',
      timeUnit: '1s',
      startRate: 300,
      preAllocatedVUs: 30,
      stages: [
        { duration: '30s', target: 1000 },
        { duration: '30s', target: 1000 },
        { duration: '10s', target: 0 }
      ],
    },
    per_vu_scenario: {
      executor: 'per-vu-iterations',
      vus: 35,
      iterations: 50,
      maxDuration: '10s'
    }
  }
}

export default function() {
  let post = Math.floor(Math.random() * 300) + 1
  let url = `http://${ipAddress}:8080/api/posts/${post}`;
  let response = http.get(url);

  check(response, {
    'Get status is correct': (r) => post <= 100 && response.status === 200 || post > 100 && response.status === 404,
    'Get ID is correct': (r) =>  post > 100 && response.status === 404 || response.json().post.id === post,
    'Get relation is valid': (r) => post > 100 && response.status === 404 || response.json().post.userId === response.json().user.id
  });
}
