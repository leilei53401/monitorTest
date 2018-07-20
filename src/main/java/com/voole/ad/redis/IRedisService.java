package com.voole.ad.redis;


public interface IRedisService {
	
    
       public boolean setStr(String key, String value, int seconds, boolean override);
    
       /* (非 Javadoc)
       * <p>Title: getStr</p>
       * <p>Description: </p>
       * @param key
       * @return
       * @see com.shangying.service.business.test#getStr(java.lang.String)
       */
    
       public String getStr(String key);
       
       public Long incrStr(String key) ;    
    
       public boolean del(String key) ;
    
       /* (非 Javadoc)
       * <p>Title: exists</p>
       * <p>Description: </p>
       * @param key
       * @return
       * @see com.shangying.service.business.test#exists(java.lang.String)
       */
     
       public boolean exists(String key);
    
       /* (非 Javadoc)
       * <p>Title: expire</p>
       * <p>Description: </p>
       * @param key
       * @param seconds
       * @return
       * @see com.shangying.service.business.test#expire(java.lang.String, int)
       */
   
       public boolean expire(String key, int seconds);
    

}
