-- KEYS[1] = reservation_backup key

-- reservation_backup 키 이용하여 campaignId, amount
local amount = redis.call("HGET", KEYS[1], "amount")
local campaignId = redis.call("HGET", KEYS[1], "campaignId")
-- 조회 값 체크
if not amount or not campaignId then
    return 0
end

-- 예산 타입 변경
local amountNum = tonumber(amount)
if not amountNum or amountNum <= 0 then
    return 0
end

-- campaignId 이용하여 예산 키 조합
local reservedBudgetKey = "campaign:" .. campaignId .. ":budget_reserved"
local totalBudgetKey = "campaign:" .. campaignId .. ":budget_total"

-- reserved 예산에서 차감
redis.call("DECRBY", reservedBudgetKey, amountNum)
redis.call("INCRBY", totalBudgetKey, amountNum)
-- 예약 정보 삭제
redis.call("DEL", KEYS[1])

return 1