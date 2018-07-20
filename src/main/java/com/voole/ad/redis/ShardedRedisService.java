package com.voole.ad.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.comparator.BooleanComparator;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;

import java.util.List;

@Service
public class ShardedRedisService implements IRedisService
{
	 private Logger log = LoggerFactory.getLogger(ShardedRedisService.class);
//        private Logger    log = Logger.getLogger(ShardedRedisService.class);
     
        @Autowired
        private ShardedJedisPool shardedJedisPool;
     
        /* (非 Javadoc)
        * <p>Title: setStr</p>
        * <p>Description: </p>
        * @param key
        * @param value
        * @param seconds
        * @param override
        * @return
        * @see com.shangying.service.business.test#setStr(java.lang.String, java.lang.String, int, boolean)
        */
        @Override
        public boolean setStr(String key, String value, int seconds, boolean override) {
     
            ShardedJedis shardedJedis = null;
            boolean ok = false;
     
            try {
                shardedJedis = shardedJedisPool.getResource();
     
                if (override == false && shardedJedis.exists(key)) {
                    ok = false;
                } else {
                    if (seconds < 1) {
                        shardedJedis.set(key, value);
                    } else {
                        shardedJedis.setex(key, seconds, value);
                    }
                    ok = true;
                }
            } catch (Exception e) {
                log.info(e.getMessage());
                returnBrokenResource(shardedJedis);
            } finally {
                returnResource(shardedJedis);
            }
            return ok;
        }
     
        /* (非 Javadoc)
        * <p>Title: getStr</p>
        * <p>Description: </p>
        * @param key
        * @return
        * @see com.shangying.service.business.test#getStr(java.lang.String)
        */
        @Override
        public String getStr(String key) {
     
            String value = null;
            ShardedJedis shardedJedis = null;
     
            try {
                shardedJedis = shardedJedisPool.getResource();
                value = shardedJedis.get(key);
            } catch (Exception e) {
                log.info(e.getMessage());
                returnBrokenResource(shardedJedis);
            } finally {
                returnResource(shardedJedis);
            }
            return value;
        }
        
        @Override
        public Long incrStr(String key) {
        	Long value = 0L;
            ShardedJedis shardedJedis = null;
     
            try {
                shardedJedis = shardedJedisPool.getResource();
                value = shardedJedis.incr(key);
            } catch (Exception e) {
                log.error(e.getMessage());
                returnBrokenResource(shardedJedis);
            } finally {
                returnResource(shardedJedis);
            }
            return value;
        }
     
     
        /* (非 Javadoc)
        * <p>Title: del</p>
        * <p>Description: </p>
        * @param key
        * @return
        * @see com.shangying.service.business.test#del(java.lang.String)
        */
        @Override
        public boolean del(String key) {
     
            boolean ok = false;
            ShardedJedis shardedJedis = null;
     
            try {
                shardedJedis = shardedJedisPool.getResource();
                shardedJedis.del(key);
                ok = true;
            } catch (Exception e) {
                log.info(e.getMessage());
                returnBrokenResource(shardedJedis);
            } finally {
                returnResource(shardedJedis);
            }
            return ok;
        }
     
        /* (非 Javadoc)
        * <p>Title: exists</p>
        * <p>Description: </p>
        * @param key
        * @return
        * @see com.shangying.service.business.test#exists(java.lang.String)
        */
        @Override
        public boolean exists(String key) {
     
            boolean ok = false;
            ShardedJedis shardedJedis = null;
     
            try {
                shardedJedis = shardedJedisPool.getResource();
                ok = shardedJedis.exists(key);
            } catch (Exception e) {
                log.info(e.getMessage());
                returnBrokenResource(shardedJedis);
            } finally {
                returnResource(shardedJedis);
            }
            return ok;
        }
     
        /* (非 Javadoc)
        * <p>Title: expire</p>
        * <p>Description: </p>
        * @param key
        * @param seconds
        * @return
        * @see com.shangying.service.business.test#expire(java.lang.String, int)
        */
        @Override
        public boolean expire(String key, int seconds) {
     
            long ok = 0;
            ShardedJedis shardedJedis = null;
     
            try {
                shardedJedis = shardedJedisPool.getResource();
                ok = shardedJedis.expire(key, seconds);
            } catch (Exception e) {
                log.info(e.getMessage());
                returnBrokenResource(shardedJedis);
            } finally {
                returnResource(shardedJedis);
            }
            return ok == 1;
        }
     
        
     
        private void returnBrokenResource(ShardedJedis shardedJedis) {
            try {
                shardedJedisPool.returnBrokenResource(shardedJedis);
            } catch (Exception e) {
                log.error("returnBrokenResource error.", e);
            }
        }
     
        private void returnResource(ShardedJedis shardedJedis) {
            try {
                shardedJedisPool.returnResource(shardedJedis);
            } catch (Exception e) {
                log.error("returnResource error.", e);
            }
        }


    /**
     * 记录自增值到hash域中。
     * @param key
     * @param field
     * @return
     */
    public Long IncMapValuePipeline(String key,String field){
        Long result = 0l;
        ShardedJedis shardedJedis = shardedJedisPool.getResource();

        if(shardedJedis != null){
//            boolean broken = false;
            try {
                ShardedJedisPipeline pipe = shardedJedis.pipelined();
                Response<Long> hincrBy = pipe.hincrBy(key, field, 1l);
//                pipe.expire(key, ts);
                pipe.sync();
                result = hincrBy.get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
//               broken = true;
            } finally {
                shardedJedisPool.returnResource(shardedJedis);
            }
        }
        return result;
    }


    /**
     * 增加set add
     * @param key
     * @param field
     * @return
     */
    public Long sadd(String key,String field){
        Long result = 0l;
        ShardedJedis shardedJedis = shardedJedisPool.getResource();

        if(shardedJedis != null){
//            boolean broken = false;
            try {
                return shardedJedis.sadd(key,field);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
//               broken = true;
            } finally {
                shardedJedisPool.returnResource(shardedJedis);
            }
        }
        return result;
    }



    public Long lpush(String key,String field){
        Long result = 0l;
        ShardedJedis shardedJedis = shardedJedisPool.getResource();

        if(shardedJedis != null){
//            boolean broken = false;
            try {
                return shardedJedis.lpush(key,field);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
//               broken = true;
            } finally {
                shardedJedisPool.returnResource(shardedJedis);
            }
        }
        return result;
    }


    public void printListHead(String key){
        Long result = 0l;
        ShardedJedis shardedJedis = shardedJedisPool.getResource();

        if(shardedJedis != null){
//            boolean broken = false;
            try {
                List<String> list  = shardedJedis.lrange(key,0,10);
                for(int i=0; i<list.size(); i++) {
                    System.out.println("列表项为: "+list.get(i));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
//               broken = true;
            } finally {
                shardedJedisPool.returnResource(shardedJedis);
            }
        }
    }


    /**
     * 增加set add
     * @param key
     * @param field
     * @return
     */
    public Boolean sismember(String key, String field){

        ShardedJedis shardedJedis = shardedJedisPool.getResource();
        Boolean result = false;
        if(shardedJedis != null){
//            boolean broken = false;
            try {
                result =  shardedJedis.sismember(key,field);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
//               broken = true;
            } finally {
                shardedJedisPool.returnResource(shardedJedis);
            }
        }
        return result;
    }

}