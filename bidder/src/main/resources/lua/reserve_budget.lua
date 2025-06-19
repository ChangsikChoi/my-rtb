-- KEYS[1] = budget_total key
-- KEYS[2] = budget_reserved key
-- KEYS[3] = reservation key
-- KEYS[4] = reservation_backup key
-- ARGV[1] = reserve amount micros
-- ARGV[2] = campaignId
-- ARGV[3] = currentTimeMillis
-- ARGV[4] = TTL in seconds

local total = tonumber(redis.call("GET", KEYS[1]) or "0")
local reserveAmount = tonumber(ARGV[1])

-- 남은 예산(total)이 예약할 예산보다 적으면 예약실패
if total < reserveAmount then
    return 0
end

-- 토탈 예산에서 예약할 예산만큼 차감, 예약 예산은 증가시킴
redis.call("DECRBY", KEYS[1], reserveAmount)
redis.call("INCRBY", KEYS[2], reserveAmount)
-- 예약 정보 임시 저장 (예약 데이터 키 = reservation:{requestId})
redis.call("HSET", KEYS[3], "campaignId", ARGV[2], "amount", reserveAmount, "timestamp", ARGV[3])
redis.call("EXPIRE", KEYS[3], tonumber(ARGV[4]))
-- 예약 정보 백업 저장 (키 만료 시 내용 조회에 사용)
redis.call("HSET", KEYS[4], "campaignId", ARGV[2], "amount",reserveAmount, "timestamp", ARGV[3])

-- 예약 성공
return 1