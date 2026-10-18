// JVM 워밍업: 측정 전에 bidder의 입찰 경로를 JIT 컴파일시킨다.
//
// 기동 직후 워밍업 없이 부하를 주면 JIT 컴파일이 요청 처리와 CPU를 나눠 써서
// p95가 약 1초까지 튄다(2 vCPU 기준, 워밍업 후에는 약 2ms).
//
// 반드시 활성 캠페인이 없는 상태(seed.sh 실행 전)에서 실행한다.
// 모든 입찰이 204로 끝나므로 예산을 소모하지 않고 HTTP 처리·JSON 파싱·Redis 조회·캠페인 필터링 경로를 데운다.
//
// 실행:
//   k6 run perf/k6/warmup.js
//   RATE=500 DURATION_SEC=30 k6 run perf/k6/warmup.js

import http from 'k6/http';
import { Counter } from 'k6/metrics';

const BIDDER_URL = __ENV.BIDDER_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 500);
const DURATION_SEC = Number(__ENV.DURATION_SEC || 30);

// 200이 나오면 활성 캠페인이 있다는 뜻이다. 워밍업이 예산을 소모하므로 실패로 처리한다.
const unexpectedBid = new Counter('warmup_unexpected_bid');
const bidError = new Counter('warmup_bid_error');

export const options = {
  scenarios: {
    warmup: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: `${DURATION_SEC}s`,
      preAllocatedVUs: Math.min(RATE, 500),
      maxVUs: Math.max(RATE * 2, 1000),
    },
  },
  thresholds: {
    warmup_unexpected_bid: ['count<1'],
    warmup_bid_error: ['count<1'],
  },
  summaryTrendStats: ['p(50)', 'p(95)', 'p(99)', 'max'],
};

const HEADERS = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  const body = JSON.stringify({
    id: `warmup-${__VU}-${__ITER}`,
    imp: { bidFloor: 0, placementId: 'perf-placement', width: 300, height: 250 },
    device: { ip: '127.0.0.1', country: 'KR', os: 'Android' },
    user: { age: 30 },
  });

  const res = http.post(`${BIDDER_URL}/dsp/bid`, body, HEADERS);
  if (res.status === 200) {
    unexpectedBid.add(1);
  } else if (res.status !== 204) {
    bidError.add(1);
  }
}

export function handleSummary(data) {
  const d = data.metrics.http_req_duration.values;
  const count = (name) => (data.metrics[name] ? data.metrics[name].values.count : 0);
  const unexpected = count('warmup_unexpected_bid');
  const dropped = count('dropped_iterations');

  const lines = [
    '',
    '=== warmup 결과 ===',
    `부하       : ${RATE} req/s × ${DURATION_SEC}s`,
    `지연       : p50 ${d['p(50)'].toFixed(1)}ms / p95 ${d['p(95)'].toFixed(1)}ms / p99 ${d['p(99)'].toFixed(1)}ms`,
    `드롭된 요청: ${dropped}`,
    unexpected > 0
      ? `❌ 입찰 성공 ${unexpected}건: 활성 캠페인이 있어 예산을 소모했습니다. seed.sh 전에 실행하세요.`
      : '✅ 예산 소모 없이 워밍업 완료',
    '',
  ];
  return { stdout: lines.join('\n') };
}
