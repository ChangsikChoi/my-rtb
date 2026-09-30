// 시나리오 A: 동시 입찰 부하에서 예산 초과 예약이 발생하지 않는지 검증한다.
//
// 사전 조건:
//   1. perf/k6/warmup.js로 bidder를 워밍업한다 (seed 전에 실행해야 예산을 소모하지 않는다).
//   2. perf/scripts/seed.sh로 캠페인 1개를 작은 예산으로 생성한다.
//      CAMPAIGNS=1 BUDGET=100 TARGET_CPM=1000 perf/scripts/seed.sh
//   3. RESULT_DIR을 미리 만든다. k6는 디렉토리를 만들지 못해 요약 저장에 실패한다.
//      mkdir -p perf/results/<run>
//
// 실행:
//   k6 run perf/k6/oversell.js
//   RATE=1000 DURATION_SEC=20 WIN_RATIO=0.5 RESULT_DIR=perf/results/<run> k6 run perf/k6/oversell.js
//
// 판정:
//   - 성공한 입찰 수(bid_ok)가 예산 용량(capacityPerCampaign)을 넘으면 threshold 실패로 k6가 non-zero 종료한다.
//   - 확정·환불 결과는 HTTP 응답으로 알 수 없으므로(/dsp/win은 항상 204),
//     예약 TTL(30초)이 지난 뒤 Redis 잔액으로 검증한다. 기대값은 요약 JSON의 expected 항목에 남긴다.

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// bidder의 예약 TTL(BudgetReserveAdapter.BUDGET_RESERVATION_SECONDES)
const RESERVATION_TTL_SEC = 30;

const seed = JSON.parse(open(__ENV.SEED_FILE || '../.state/seed.json'));
if (seed.campaigns.length !== 1) {
  throw new Error(`oversell 시나리오는 활성 캠페인 1개가 필요합니다 (현재 ${seed.campaigns.length}개)`);
}

const BIDDER_URL = __ENV.BIDDER_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 500);
const DURATION_SEC = Number(__ENV.DURATION_SEC || 20);
const WIN_RATIO = Number(__ENV.WIN_RATIO || 0.5);
const RESULT_DIR = __ENV.RESULT_DIR || '';

// 부하 시간이 TTL을 넘으면 만료 환불된 예산으로 다시 입찰이 성공해
// "성공 입찰 수 ≤ 예산 용량" 판정이 성립하지 않는다.
if (DURATION_SEC >= RESERVATION_TTL_SEC) {
  throw new Error(`DURATION_SEC(${DURATION_SEC})는 예약 TTL(${RESERVATION_TTL_SEC}초)보다 짧아야 합니다`);
}
if (!(WIN_RATIO >= 0 && WIN_RATIO <= 1)) {
  throw new Error(`WIN_RATIO는 0~1 사이여야 합니다: ${WIN_RATIO}`);
}

const capacity = seed.capacityPerCampaign;

const bidOk = new Counter('bid_ok');
const bidNoBid = new Counter('bid_no_bid');
const bidError = new Counter('bid_error');
const winSent = new Counter('win_sent');
const winError = new Counter('win_error');

export const options = {
  scenarios: {
    oversell: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: `${DURATION_SEC}s`,
      preAllocatedVUs: Math.min(RATE, 500),
      maxVUs: Math.max(RATE * 2, 1000),
    },
  },
  thresholds: {
    // 핵심 판정: 예산 용량을 넘는 입찰 성공이 없어야 한다.
    bid_ok: [`count<=${capacity}`],
    bid_error: ['count<1'],
    win_error: ['count<1'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
};

const BID_HEADERS = { headers: { 'Content-Type': 'application/json' }, tags: { name: 'bid' } };
const AID_PATTERN = /[?&]aid=([^&]+)/;

export default function () {
  const body = JSON.stringify({
    id: `oversell-${__VU}-${__ITER}`,
    imp: { bidFloor: 0, placementId: 'perf-placement', width: 300, height: 250 },
    device: { ip: '127.0.0.1', country: 'KR', os: 'Android' },
    user: { age: 30 },
  });

  const res = http.post(`${BIDDER_URL}/dsp/bid`, body, BID_HEADERS);

  if (res.status === 204) {
    bidNoBid.add(1);
    return;
  }
  if (res.status !== 200) {
    bidError.add(1);
    return;
  }

  bidOk.add(1);

  const winUrl = res.json('winUrl');
  const match = typeof winUrl === 'string' ? winUrl.match(AID_PATTERN) : null;
  if (!check(match, { 'winUrl에 aid 포함': (m) => m !== null })) {
    bidError.add(1);
    return;
  }

  if (Math.random() < WIN_RATIO) {
    // 응답의 winUrl 호스트는 localhost:8080으로 고정되어 있으므로 aid만 꺼내 BIDDER_URL로 호출한다.
    const winRes = http.get(`${BIDDER_URL}/dsp/win?aid=${match[1]}`, { tags: { name: 'win' } });
    winSent.add(1);
    if (winRes.status !== 204) {
      winError.add(1);
    }
  }
}

function count(data, name) {
  const metric = data.metrics[name];
  return metric ? metric.values.count : 0;
}

export function handleSummary(data) {
  const ok = count(data, 'bid_ok');
  const noBid = count(data, 'bid_no_bid');
  const errors = count(data, 'bid_error');
  const wins = count(data, 'win_sent');
  const winErrors = count(data, 'win_error');
  const dropped = count(data, 'dropped_iterations');
  const duration = data.metrics['http_req_duration{name:bid}'] || data.metrics.http_req_duration;

  const expected = {
    // 부하 직후: 성공 입찰은 예산 용량 이하
    maxBidOk: capacity,
    // TTL 경과 후: 모든 예약은 확정 또는 환불되어야 한다
    finalReservedMicro: 0,
    // TTL 경과 후: 확정된 win만큼만 차감되어야 한다 (win 호출이 모두 확정됐다는 가정)
    finalTotalMicro: seed.budgetMicro - wins * seed.impressionPriceMicro,
  };

  const result = {
    scenario: 'oversell',
    runId: seed.runId,
    campaignId: seed.campaigns[0],
    config: { rate: RATE, durationSec: DURATION_SEC, winRatio: WIN_RATIO, bidderUrl: BIDDER_URL },
    seed: {
      budgetMicro: seed.budgetMicro,
      impressionPriceMicro: seed.impressionPriceMicro,
      capacity,
    },
    counts: { bidOk: ok, bidNoBid: noBid, bidError: errors, winSent: wins, winError: winErrors, droppedIterations: dropped },
    bidLatencyMs: duration ? duration.values : null,
    oversold: ok > capacity,
    expected,
  };

  const latency = duration
    ? `p50 ${duration.values['p(50)'].toFixed(1)}ms / p95 ${duration.values['p(95)'].toFixed(1)}ms / p99 ${duration.values['p(99)'].toFixed(1)}ms`
    : 'n/a';

  const lines = [
    '',
    '=== oversell 결과 ===',
    `부하        : ${RATE} req/s × ${DURATION_SEC}s (win 비율 ${WIN_RATIO})`,
    `예산 용량   : ${capacity}회`,
    `입찰 성공   : ${ok}  ${ok > capacity ? '❌ 초과 예약 발생' : ok === capacity ? '✅ 예산 정확히 소진' : '⚠️ 예산 미소진 (부하 부족 여부 확인)'}`,
    `입찰 없음   : ${noBid}`,
    `입찰 오류   : ${errors}`,
    `win 호출    : ${wins} (오류 ${winErrors})`,
    `드롭된 요청 : ${dropped}${dropped > 0 ? '  ⚠️ VU 부족으로 목표 RPS 미달' : ''}`,
    `입찰 지연   : ${latency}`,
    '',
    `다음 단계: ${RESERVATION_TTL_SEC}초 이상 기다린 뒤 Redis에서 확인`,
    `  campaign:${seed.campaigns[0]}:budget_reserved == ${expected.finalReservedMicro}`,
    `  campaign:${seed.campaigns[0]}:budget_total    == ${expected.finalTotalMicro}`,
    '',
  ];

  const output = { stdout: lines.join('\n') };
  if (RESULT_DIR) {
    output[`${RESULT_DIR}/oversell-summary.json`] = JSON.stringify(result, null, 2);
    output[`${RESULT_DIR}/k6-raw-summary.json`] = JSON.stringify(data, null, 2);
  }
  return output;
}
