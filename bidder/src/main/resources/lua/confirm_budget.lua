-- KEYS[1] = reservation key
-- KEYS[2] = reservation_backup key

-- reservation 키 이용하여 campaignId, amount 조회
local reservedAmount = tonumber(redis.call("HGET", KEYS[1], "amount"))
local campaignId = redis.call("HGET", KEYS[1], "campaignId")
-- 조회 값 체크
if not reservedAmount or not campaignId then
    return 0
end

-- campaignId 이용하여 예약 예산 키 조합
local reservedBudgetKey = "campaign:" .. campaignId .. ":budget_reserved"

-- reserved 예산에서 차감
redis.call("DECRBY", reservedBudgetKey, reservedAmount)
-- 예약 정보 삭제
redis.call("DEL", KEYS[1])
redis.call("DEL", KEYS[2])

return 1
