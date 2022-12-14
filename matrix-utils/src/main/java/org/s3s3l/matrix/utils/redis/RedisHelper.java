package org.s3s3l.matrix.utils.redis;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.s3s3l.matrix.utils.redis.base.IRedis;
import org.s3s3l.matrix.utils.redis.base.InitializableRedis;
import org.s3s3l.matrix.utils.redis.base.MessageHandler;
import org.s3s3l.matrix.utils.redis.configuration.RedisConfig.RedisMasterSlaveConfig;
import org.s3s3l.matrix.utils.redis.configuration.RedisConfig.RedisProps;
import org.s3s3l.matrix.utils.redis.exception.RedisExcuteException;
import org.s3s3l.matrix.utils.stuctural.StructuralHelper;
import org.s3s3l.matrix.utils.stuctural.jackson.JacksonUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;

@SuppressWarnings("deprecation")
@Slf4j
public class RedisHelper implements InitializableRedis<RedisMasterSlaveConfig> {
    private JedisPool master;
    private JedisPool slave;
    private List<Action> allowActions = Arrays.asList(Action.values());
    private final StructuralHelper json = JacksonUtils.create()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public RedisHelper() {

    }

    private RedisHelper(JedisPool master, JedisPool slave, Action... allowActions) {
        this.master = master;
        this.slave = slave;
        this.allowActions = Arrays.asList(allowActions);
    }

    @Override
    public void init(RedisMasterSlaveConfig configuration) {
        this.master = getJedisPool(configuration.getMaster());
        if (configuration.getSlave() == null) {
            this.slave = this.master;
        } else {
            this.slave = getJedisPool(configuration.getSlave());
        }
    }

    private JedisPool getJedisPool(RedisProps redisProp) {
        if (redisProp == null) {
            return null;
        }
        return new JedisPool(redisProp
                .getPoolConfig(), redisProp.getHost(), redisProp.getPort(), redisProp.getTimeout(),
                redisProp.getSoTimeout(), redisProp.getPassword(), redisProp.getDatabase(), null);
    }

    public static IRedis create(JedisPool master, JedisPool slave, Action... allowActions) {
        return new RedisHelper(master, slave, allowActions);
    }

    public enum Action {
        READ, WRITE, DELETE, UPDATE, CHECK, GEO_WRITE, GEO_READ
    }

    public <T> T execute(Function<Jedis, T> call, Action action) {

        checkAction(action);
        switch (action) {
            case CHECK:
            case READ:
            case GEO_READ:
                try (Jedis jedis = getConnectionForRead()) {
                    return call.apply(jedis);
                }
            case WRITE:
            case GEO_WRITE:
            case DELETE:
            case UPDATE:
            default:
                try (Jedis jedis = getConnectionForWrite()) {
                    return call.apply(jedis);
                }
        }
    }

    private void accept(Consumer<Jedis> action) {
        try (Jedis jedis = getConnectionForWrite()) {
            action.accept(jedis);
        } catch (Exception e) {
            log.warn("127.0.0.1||0||{}||jedis execute in order redis error:", e.getClass()
                    .getName(), e);
            throw e;
        }
    }

    private void checkAction(Action action) {
        if (allowActions == null || allowActions.isEmpty() || !allowActions.contains(action)) {
            throw new RedisExcuteException(
                    String.format("Action [%s] is not allowed in this instance. Allow actions [%s]", action,
                            allowActions == null ? ""
                                    : String.join(",", allowActions.stream()
                                            .map(Action::name)
                                            .collect(Collectors.toList()))));
        }
    }

    private Jedis getConnectionForRead() {
        if (null == master || null == slave) {
            throw new RedisExcuteException("Jedis pool not set.");
        }

        try {
            return slave.getResource();
        } catch (Exception e) {
            if (log != null) {
                log.warn("Can not get connection from slave. Trying master.", e);
            }
        }

        return master.getResource();
    }

    private Jedis getConnectionForWrite() {
        if (null == master) {
            throw new RedisExcuteException("Jedis pool not set.");
        }

        return master.getResource();
    }

    private boolean expire(Jedis jedis, String key, long seconds) {
        if (!jedis.exists(key) || seconds > Integer.MAX_VALUE) {
            return false;
        }
        jedis.expire(key, (int) seconds);
        return true;

    }

    @Override
    public boolean hasKey(String key) {
        return execute(jedis -> jedis.exists(key), Action.CHECK);
    }

    @Override
    public void set(String key, String value) {
        execute(jedis -> jedis.set(key, value), Action.WRITE);
    }

    @Override
    public void setnx(String key, String value) {
        execute(jedis -> jedis.setnx(key, value), Action.WRITE);
    }

    @Override
    public void set(String key, String value, long seconds) {
        execute(jedis -> {
            jedis.setex(key, (int) seconds, value);
            return null;
        }, Action.WRITE);

    }

    @Override
    public void set(byte[] key, byte[] value, long seconds) {
        execute(jedis -> {
            jedis.setex(key, (int) seconds, value);
            return null;
        }, Action.WRITE);
    }

    @Override
    public String set(final String key, final String value, final SetParams params) {
        return execute(jedis -> jedis.set(key, value, params), Action.WRITE);
    }

    @Override
    public boolean del(String key) {
        return execute(jedis -> {
            if (!jedis.exists(key)) {
                return false;
            }
            jedis.del(key);
            return true;
        }, Action.DELETE);
    }

    @Override
    public boolean expire(String key, long seconds) {
        return execute(jedis -> expire(jedis, key, seconds), Action.UPDATE);
    }

    @Override
    public boolean expire(byte[] key, long seconds) {
        return execute(jedis -> jedis.expire(key, (int) seconds) == 1, Action.UPDATE);
    }

    @Override
    public String get(String key) {
        return execute(jedis -> {
            if (!jedis.exists(key)) {
                return "";
            }
            return jedis.get(key);
        }, Action.READ);
    }

    @Override
    public List<String> lRange(String key) {
        return execute(jedis -> {
            if (!jedis.exists(key)) {
                return Collections.emptyList();
            }
            return jedis.lrange(key, 0, jedis.llen(key));
        }, Action.READ);
    }

    @Override
    public List<String> lRange(String key, long start, long stop) {
        return execute(jedis -> {
            if (!jedis.exists(key)) {
                return Collections.emptyList();
            }
            return jedis.lrange(key, start, stop);
        }, Action.READ);
    }

    @Override
    public long lPush(String key, String... values) {
        return execute(jedis -> jedis.lpush(key, values), Action.UPDATE);
    }

    @Override
    public long lPush(String key, long seconds, String... values) {
        return execute(jedis -> {
            long count = jedis.lpush(key, values);
            expire(jedis, key, seconds);
            return count;
        }, Action.UPDATE);
    }

    @Override
    public long lPushUnique(String key, long seconds, String... values) {
        return execute(jedis -> {
            long count = 0;
            Set<String> set = new HashSet<>();
            if (jedis.exists(key)) {
                set.addAll(jedis.lrange(key, 0, jedis.llen(key)));
            }
            set.addAll(Arrays.asList(values));
            jedis.del(key);
            count = jedis.lpush(key, set.toArray(new String[set.size()]));
            expire(jedis, key, seconds);
            return count;
        }, Action.UPDATE);
    }

    @Override
    public long lPushUnique(String key, String... values) {
        return execute(jedis -> {
            long count = 0;
            Set<String> set = new HashSet<>();
            if (jedis.exists(key)) {
                set.addAll(jedis.lrange(key, 0, jedis.llen(key)));
            }
            set.addAll(Arrays.asList(values));
            jedis.del(key);
            count = jedis.lpush(key, set.toArray(new String[set.size()]));
            return count;
        }, Action.UPDATE);
    }

    @Override
    public long lPushUnique(String key, List<String> value, long seconds) {
        return lPushUnique(key, seconds, value.toArray(new String[value.size()]));
    }

    @Override
    public boolean existValue(String key, String value) {
        return execute(jedis -> {

            boolean result = false;
            try {
                if (!jedis.exists(key)) {
                    return result;
                }

                long len = jedis.llen(key);
                List<String> list = jedis.lrange(key, 0, len);
                if (list.contains(value)) {
                    result = true;
                }
            } catch (Exception e) {
                result = false;
                throw e;
            }
            return result;
        }, Action.CHECK);
    }

    @Override
    public boolean popSingle(String key, String value) {
        return execute(jedis -> {

            boolean result = false;

            try {
                if (!jedis.exists(key)) {
                    return result;
                }

                long len = jedis.llen(key);
                List<String> list = jedis.lrange(key, 0, len);
                int index = list.indexOf(value);
                if (index < 0) {
                    return result;
                }
                list.remove(index);
                jedis.del(key);
                if (!list.isEmpty()) {
                    jedis.lpush(key, list.toArray(new String[list.size()]));
                }
                result = true;
            } catch (Exception e) {
                result = false;
                throw e;
            }
            return result;
        }, Action.UPDATE);
    }

    @Override
    public long geoadd(String geoSetName, double lng, double lat, String itemKey) {
        return execute(jedis -> jedis.geoadd(geoSetName, lng, lat, itemKey), Action.GEO_WRITE);
    }

    @Override
    public long removeGeoKey(String geoSetName, String itemKey) {
        return execute(jedis -> jedis.zrem(geoSetName, itemKey), Action.GEO_WRITE);
    }

    @Override
    public void hset(String key, String hash, String value) {
        execute(jedis -> jedis.hset(key, hash, value), Action.WRITE);
    }

    @Override
    public void hset(String key, String hash, List<String> value) {
        execute(jedis -> jedis.hset(key, hash, json.toStructuralString(value)), Action.WRITE);
    }

    @Override
    public String hget(String key, String hash) {
        return execute(jedis -> jedis.hget(key, hash), Action.READ);
    }

    @Override
    public List<String> hgetList(String key, String hash) {
        String data = hget(key, hash);
        if (StringUtils.isEmpty(data)) {
            return Collections.emptyList();
        }
        return json.toObject(data, new TypeReference<List<String>>() {
        });
    }

    @Override
    public void hmset(String key, Map<String, String> map) {
        execute(jedis -> jedis.hmset(key, map), Action.WRITE);
    }

    @Override
    public List<String> hmget(String key, String... field) {
        return execute(jedis -> jedis.hmget(key, field), Action.READ);
    }

    @Override
    public void hmsetList(String key, Map<String, List<String>> map) {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            stringMap.put(entry.getKey(), json.toStructuralString(entry.getValue()));
        }
        hmset(key, stringMap);
    }

    @Override
    public long incr(String key) {
        return execute(jedis -> jedis.incr(key), Action.UPDATE);
    }

    @Override
    public long incr(String key, int seconds) {
        return execute(jedis -> {
            jedis.watch(key);
            boolean hasKey = jedis.exists(key);
            String currentStr = jedis.get(key);
            long current = hasKey ? Long.parseLong(currentStr) : 0;

            Transaction transaction = jedis.multi();

            transaction.incrBy(key, 1L);
            if (!hasKey) {
                transaction.expire(key, seconds);
            }
            transaction.exec();
            return current + 1;
        }, Action.UPDATE);
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
            GeoUnit unit) {
        return execute(jedis -> jedis.georadius(key, longitude, latitude, radius, unit), Action.GEO_READ);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByParam(String key,
            double longitude,
            double latitude,
            double radius,
            GeoUnit unit,
            GeoRadiusParam params) {
        return execute(jedis -> jedis.georadius(key, longitude, latitude, radius, unit, params), Action.GEO_READ);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return execute(jedis -> jedis.georadiusByMember(key, member, radius, unit), Action.GEO_READ);
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return execute(jedis -> jedis.geodist(key, member1, member2, unit), Action.GEO_READ);
    }

    @Override
    public Map<String, String> getMap(String key) {
        return execute(jedis -> jedis.hgetAll(key), Action.READ);
    }

    @Override
    public long time() {
        return execute(jedis -> Long.valueOf(jedis
                .eval("redis.replicate_commands();" + "local times = redis.call('time');"
                        + "local time = string.format(\"%s%s\",times[1],string.sub(times[2],0,3));" + "return time;")
                .toString()), Action.READ);
    }

    @Override
    public void set(byte[] key, byte[] value) {
        execute(jedis -> jedis.set(key, value), Action.WRITE);
    }

    @Override
    public byte[] getBytes(byte[] key) {
        return execute(jedis -> jedis.get(key), Action.READ);
    }

    @Override
    public boolean del(byte[] key) {
        return execute(jedis -> jedis.del(key) > 0, Action.WRITE);
    }

    @Override
    public long setToCurrent(String key) {
        return execute(jedis -> Long.valueOf(jedis
                .eval("redis.replicate_commands();" + "local times = redis.call('time');"
                        + "local time = string.format(\"%s%s\",times[1],string.sub(times[2],0,3));"
                        + "redis.call('set',\"".concat(key)
                                .concat("\",time);return time;"))
                .toString()), Action.WRITE);
    }

    @Override
    public Object eval(String lua) {
        return execute(jedis -> jedis.eval(lua), Action.WRITE);
    }

    @Override
    public <T> T multi(Function<Transaction, T> call) {
        return execute(jedis -> {
            try (Transaction transaction = jedis.multi()) {
                return call.apply(transaction);
            }
        }, Action.WRITE);
    }

    @Override
    public List<GeoCoordinate> geoPos(String key, String... members) {
        return execute(jedis -> jedis.geopos(key, members), Action.GEO_READ);
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        accept(jedis -> jedis.psubscribe(jedisPubSub, patterns));
    }

    @Override
    public void psubscribe(String routingKey, MessageHandler<String> handler, String... patterns) {
        accept(redis -> redis.psubscribe(new JedisPubSub() {
            @Override
            public void onPMessage(String pattern, String channel, String message) {
                handler.handle(message, pattern);
            }
        }, patterns));
    }

    @Override
    public long ttl(String key) {
        return execute(jedis -> jedis.ttl(key), Action.READ);
    }

    @Override
    public long sadd(String key, String... members) {
        return execute(jedis -> jedis.sadd(key, members), Action.WRITE);
    }

    @Override
    public long srem(String key, String... members) {
        return execute(jedis -> jedis.srem(key, members), Action.DELETE);
    }

    @Override
    public Set<String> smembers(String key) {
        return execute(jedis -> jedis.smembers(key), Action.READ);
    }

    @Override
    public long hdel(String key, String... field) {
        return execute(jedis -> jedis.hdel(key, field), Action.WRITE);
    }

    @Override
    public String lpop(String key) {
        return execute(jedis -> jedis.lpop(key), Action.WRITE);
    }

    @Override
    public List<String> blpop(int timeout, String... keys) {
        return execute(jedis -> jedis.blpop(timeout, keys), Action.WRITE);
    }

    @Override
    public long decr(String key) {
        return execute(jedis -> jedis.decr(key), Action.WRITE);
    }

    @Override
    public String rpop(String key) {
        return execute(jedis -> jedis.rpop(key), Action.WRITE);
    }

    @Override
    public List<String> brpop(int timeout, String... keys) {
        return execute(jedis -> jedis.brpop(timeout, keys), Action.WRITE);
    }

    @Override
    public long llen(String key) {
        return execute(jedis -> jedis.llen(key), Action.READ);
    }

    @Override
    public long scard(String key) {
        return execute(jedis -> jedis.scard(key), Action.READ);
    }

    @Override
    public long hlen(String key) {
        return execute(jedis -> jedis.hlen(key), Action.READ);
    }

    @Override
    public long zadd(String key, double score, String member) {
        return execute(jedis -> jedis.zadd(key, score, member), Action.WRITE);
    }

    @Override
    public Set<String> zrange(String key, long start, long stop) {
        return execute(jedis -> jedis.zrange(key, start, stop), Action.READ);
    }

    @Override
    public long zrem(String key, String... member) {
        return execute(jedis -> jedis.zrem(key, member), Action.WRITE);
    }

    @Override
    public long zremRangeByScore(String key, double min, double max) {
        return execute(jedis -> jedis.zremrangeByScore(key, min, max), Action.WRITE);
    }

    @Override
    public Set<Tuple> zrangeByScore(String key, double min, double max) {
        return execute(jedis -> jedis.zrangeByScoreWithScores(key, min, max), Action.READ);
    }

    @Override
    public Double zscoreBymember(String key, String member) {
        return execute(jedis -> jedis.zscore(key, member), Action.READ);
    }

    @Override
    public Set<String> hkeys(String key) {
        return execute(jedis -> jedis.hkeys(key), Action.READ);
    }

    @Override
    public void hset(String key, String hash, byte[] value) {
        execute(jedis -> jedis.hset(key.getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8), value),
                Action.WRITE);
    }

    @Override
    public byte[] hget(byte[] key, byte[] hash) {
        return execute(jedis -> jedis.hget(key, hash), Action.READ);
    }

    @Override
    public void hset(byte[] key, byte[] hash, byte[] value) {
        execute(jedis -> jedis.hset(key, hash, value), Action.WRITE);
    }

    @Override
    public long hdel(byte[] key, byte[]... field) {
        return execute(jedis -> jedis.hdel(key, field), Action.WRITE);
    }

    @Override
    public Object eval(String lua, String key) {
        return execute(jedis -> jedis.eval(lua, 1, key), Action.WRITE);
    }

    @Override
    public boolean sismember(byte[] key, byte[] member) {
        return execute(jedis -> jedis.sismember(key, member), Action.READ);
    }

    @Override
    public boolean sismember(String key, String member) {
        return execute(jedis -> jedis.sismember(key, member), Action.READ);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return execute(jedis -> jedis.hgetAll(key), Action.READ);
    }

    @Override
    public void close() {
        if (slave != null) {
            slave.close();
        }
        if (master != null) {
            master.close();
        }
    }
}
