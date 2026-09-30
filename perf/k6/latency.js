// 시나리오 B: 고정 요청률(RATE)에서 입찰 지연시간을 측정한다.
//
// run_latency.sh가 요청률 단계마다 이 스크립트를 한 번씩 실행한다.
// 예산을 충분히 크게 시드하므로 모든 입찰은 예약에 성공하는 경로(200)를 탄다.
//
// 실행:
//   RATE=500 DURATION_SEC=60 RESULT_DIR=perf/results/<run>/step-r500 k6 run perf/k6/latency.js
//
// 환경 변수:
//   RATE          초당 입찰 요청 수 (기본 100)
//   DURATION_SEC  측정 시간 (기본 60)
//   WIN_RATIO     성공 입찰 중 win을 호출할 비율 (기본 0: 입찰 경로만 측정, 예약은 30초 뒤 만료 환불)
//   RESULT_DIR    요약 저장 디렉토리 (미리 만들어 둬야 한다)
//   BIDDER_URL    bidder 주소 (기본 http://localhost:8080)

import http from 'k6/http';
import { Counter } from 'k6/metrics';

const BIDDER_URL = __ENV.BIDDER_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 100);
const DURATION_SEC = Number(__ENV.DURATION_SEC || 60);
const WIN_RATIO = Number(__ENV.WIN_RATIO || 0);
const RESULT_DIR = __ENV.RESULT_DIR || '';

const bidOk = new Counter('bid_ok');
const bidNoBid = new Counter('bid_no_bid');
const bidError = new Counter('bid_error');
const winSent = new Counter('win_sent');

export const options = {
  scenarios: {
    latency: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: `${DURATION_SEC}s`,
      preAllocatedVUs: Math.min(Math.max(RATE, 50), 500),
      maxVUs: Math.max(RATE * 2, 1000),
    },
  },
  // 입찰 요청만의 지연 분포를 요약에 남기기 위한 서브 메트릭 선언
  thresholds: {
    'http_req_duration{name:bid}': [],
  },
  summaryTrendStats: ['avg', 'min', 'p(50)', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const BID_PARAMS = { headers: { 'Content-Type': 'application/json' }, tags: { name: 'bid' } };
const AID_PATTERN = /[?&]aid=([^&]+)/;

export default function () {
  // seed.sh의 캠페인 타겟(Android, KR, 20~40세)과 일치하는 요청
  const body = JSON.stringify({
    id: `latency-${__VU}-${__ITER}`,
    imp: { bidFloor: 0, placementId: 'perf-placement', width: 300, height: 250 },
    device: { ip: '127.0.0.1', country: 'KR', os: 'Android' },
    user: { age: 30 },
  });

  const res = http.post(`${BIDDER_URL}/dsp/bid`, body, BID_PARAMS);

  if (res.status === 204) {
    bidNoBid.add(1);
    return;
  }
  if (res.status !== 200) {
    bidError.add(1);
    return;
  }
  bidOk.add(1);

  if (WIN_RATIO > 0 && Math.random() < WIN_RATIO) {
    const match = String(res.json('winUrl') || '').match(AID_PATTERN);
    if (match) {
      http.get(`${BIDDER_URL}/dsp/win?aid=${match[1]}`, { tags: { name: 'win' } });
      winSent.add(1);
    }
  }
}

export function handleSummary(data) {
  const metric = (name) => data.metrics[name];
  const count = (name) => (metric(name) ? metric(name).values.count : 0);
  const bid = metric('http_req_duration{name:bid}') || metric('http_req_duration');
  const round = (v) => Math.round(v * 100) / 100;

  const ok = count('bid_ok');
  const noBid = count('bid_no_bid');
  const errors = count('bid_error');
  const bidRequests = ok + noBid + errors;
  const dropped = count('dropped_iterations');
  const expected = RATE * DURATION_SEC;

  const latency = {};
  for (const key of ['avg', 'min', 'p(50)', 'p(90)', 'p(95)', 'p(99)', 'max']) {
    latency[key] = round(bid.values[key]);
  }

  const result = {
    scenario: 'latency',
    rate: RATE,
    durationSec: DURATION_SEC,
    winRatio: WIN_RATIO,
    expectedRequests: expected,
    bidRequests,
    achievedRps: round(bidRequests / DURATION_SEC),
    counts: { bidOk: ok, bidNoBid: noBid, bidError: errors, winSent: count('win_sent'), droppedIterations: dropped },
    droppedRatio: round(dropped / expected),
    bidLatencyMs: latency,
  };

  const line =
    `rate ${RATE}/s → 실제 ${result.achievedRps}/s | ` +
    `p50 ${latency['p(50)']}ms p95 ${latency['p(95)']}ms p99 ${latency['p(99)']}ms max ${latency.max}ms | ` +
    `200:${ok} 204:${noBid} err:${errors} drop:${dropped}`;

  const output = { stdout: `\n=== latency 결과 ===\n${line}\n` };
  if (RESULT_DIR) {
    output[`${RESULT_DIR}/k6-summary.json`] = JSON.stringify(result, null, 2);
    output[`${RESULT_DIR}/k6-raw-summary.json`] = JSON.stringify(data, null, 2);
  }
  return output;
}
