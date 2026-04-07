-- KEYS[1] = campaign hash key
-- KEYS[2] = budget_total key
-- KEYS[3] = budget_reserved key
-- KEYS[4] = campaign id set key
-- ARGV[1] = campaign id
-- ARGV[2] = initial total budget micros
-- ARGV[3] = initial reserved budget micros
-- ARGV[4..N] = campaign hash field/value pairs

-- 재활성화 시 기존 캠페인 해시 유지
if redis.call("EXISTS", KEYS[1]) == 0 then
  if #ARGV > 3 then
    redis.call("HSET", KEYS[1], unpack(ARGV, 4, #ARGV))
  end
end

redis.call("SETNX", KEYS[2], ARGV[2])
redis.call("SETNX", KEYS[3], ARGV[3])
redis.call("SADD", KEYS[4], ARGV[1])

return 1
