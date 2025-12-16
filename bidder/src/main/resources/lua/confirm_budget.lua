-- KEYS[1] = reservation key
-- KEYS[2] = reservation_backup key
-- KEYS[3] = budget_reserved key

-- reservation 키 이용하여 campaignId, amount 조회
local reservedAmount = tonumber(redis.call("HGET", KEYS[1], "amount"))
-- 조회 값 체크
if not reservedAmount then
    return 0
end

-- reserved 예산에서 차감
redis.call("DECRBY", KEYS[3], reservedAmount)
-- 예약 정보 삭제
redis.call("DEL", KEYS[1])
redis.call("DEL", KEYS[2])

return 1
