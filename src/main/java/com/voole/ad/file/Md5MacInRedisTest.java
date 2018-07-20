package com.voole.ad.file;

import com.voole.ad.redis.ShardedRedisService;
import com.voole.ad.utils.AdFileTools;
import com.voole.ad.utils.HttpClientUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

@Component("Md5MacInRedisTest")
public class Md5MacInRedisTest {

    final Logger logger = Logger.getLogger(Md5MacInRedisTest.class);


    @Resource
    private ShardedRedisService shardedRedisService;

    public void parseFile() {
        long all = 0L;
        long fail = 0L;
        StringBuilder sb = new StringBuilder();
        StringBuilder predownreport = new StringBuilder();
        File theFile = new File("/opt/redis/md5mac/mac_out.txt");

        try {
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            String line = "";
            try {
                while (it.hasNext()) {

                    try {
                        line = it.nextLine();
                        if (StringUtils.isBlank(line)) {
                            continue;
                        }

                        shardedRedisService.lpush("expday_20180432_test",line);


                    } catch (Exception e) {
                        System.err.println("处理数据[" + line + "]出错：" + e);
                        fail++;
                    } finally {
                        sb.setLength(0);
                        predownreport.setLength(0);
                    }
                    all++;

                    if (all % 10000 == 0) {
                        System.out.println((new Date()) + ",allnum=" + all);
                    }
                }

            } finally {
                LineIterator.closeQuietly(it);
            }

            System.out.println("共处理数据[" + all + "]条,失败[" + fail + "]条");

            shardedRedisService.printListHead("expday_20180432_test");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
