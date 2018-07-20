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

@Component("ExpReportFromFile")
public class ExpReportFromFile {

    final Logger logger = Logger.getLogger(ExpReportFromFile.class);

    @Resource
    private ShardedRedisService shardedRedisService;

    String recordePath = "/opt/webapps/exp_file/log/";

    public void parseFile() {
        logger.info("=================开始解析数据==================");
        long starttime = System.currentTimeMillis();
        long all = 0L;
        long fail = 0L;

        File theFile = new File("/opt/webapps/exp_file/20180501_22_top10W.txt");

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

                        String param = StringUtils.substringBetween(line,"?","HTTP");
                        if(StringUtils.isBlank(param)){
                            param = line;
                        }
                        param = StringUtils.trimToEmpty(param);

                        String sendUrl = "http://192.168.3.5:8081/v1/a/1.gif?" + param;


                        //########################## 开始发送数据 ############################

                        try {

                            int code = 0;
                            try {
                                code = HttpClientUtil.httpGetAndCode(sendUrl, 3000, 3000);
                            } catch (IOException e) {
                                logger.error("send error:", e);
                            }
                            if(code!=200){
                                fail++;
                            }
                            String resutl = code + "," + sendUrl + "\n";
                            AdFileTools.writeLineToFile(recordePath, "out_20180515_10w_3.txt", resutl, true);


                        } catch (Exception e) {
                            logger.error("发送["+sendUrl+"]出错：", e);
                            fail++;
                            continue;
                        }

                        //######################## 发送数据结束 #############################



                    } catch (Exception e) {
                        logger.error("处理数据[" + line + "]出错：" + e);
                        fail++;
                        continue;
                    }
                    all++;

                    if (all % 600 == 0) {
                        try {
                            //线程等待0.5秒，防止加入数据过多。
                            Thread.sleep(500L);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    if (all % 5000 == 0) {
                        logger.info("已发送 " + all+" 条！");
                    }
                }

            } finally {
                LineIterator.closeQuietly(it);
            }

            long endtime = System.currentTimeMillis();

            logger.info("共处理数据[" + all + "]条,失败[" + fail + "]条,共耗时["+(endtime-starttime)+"]毫秒!");
        } catch (IOException e) {
            logger.error(e);
        }
    }


    public void sendUrlTest() {

        StringBuilder sb = new StringBuilder();


        sb.append("http://192.168.3.4:8081/v1/g/1.gif?")
                .append("b=").append("")
                .append("c=").append("")//creativeid 写死
                .append("d=").append("")//plan id 写死
                .append("e=").append("")
                .append("f=").append("")
                .append("g=").append("3");


        int code3 = 0;
        try {
            code3 = HttpClientUtil.httpGetAndCode(sb.toString(), 3000, 3000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String resutl3 = code3 + "," + sb.toString() + "\n";
        AdFileTools.writeLineToFile(recordePath, "out_3.txt", resutl3, true);
    }



    public void sendHttpTest() {
        String url = "http://si.super-ssp.tv:8080/adPut.action?oemid=900103101&uid=&hid=67204eb8f95a39c0f16618d51bb299f9&width=&height=&fps=&mimeid=&isthird=1&isrt=0&areacode=0,0&adpos=10151010&ip=10.9.4.13&version=4.4&format=1";
        try {
            String res = HttpClientUtil.httpGet(url);
            if (StringUtils.isNotBlank(res)) {

                String mac = "b13d4e879e35";
//                logger.info("resut: " + res);
                JSONObject jsonRoot = JSONObject.fromObject(res);

                //解析
                String sessionid = jsonRoot.getString("sessionid");
//                logger.info("sessionid is : "+ sessionid);
                JSONArray adinfos = jsonRoot.getJSONArray("adinfos");
                Iterator adinfoIt = adinfos.iterator();

                while (adinfoIt.hasNext()) {
                    JSONObject adinfo = (JSONObject) adinfoIt.next();
//                    String invUrl = adinfo.getString("inventoryvalues");
//                    logger.info("invUrl is : "  + invUrl);
                    String pos = adinfo.getString("pos");

                    JSONArray invArray = adinfo.getJSONArray("inventoryvalues");
                    Iterator invIt = invArray.iterator();
                    while (invIt.hasNext()) {
                        String invUrl = (String) invIt.next();
//                        String invUrl = invObj.toString();
                        logger.info("invUrl is : " + invUrl);
                    }

                    logger.info("pos is : " + pos);

                    JSONArray mediainfos = adinfo.getJSONArray("mediainfo");


                    if (null != mediainfos && mediainfos.size() > 0) {

                        Iterator mediainfoIt = mediainfos.iterator();

                        while (mediainfoIt.hasNext()) {
                            JSONObject mediainfoObj = (JSONObject) mediainfoIt.next();

                            String amid = mediainfoObj.getString("amid");

                            String reportUrl = mediainfoObj.getString("reportvalues");

                            String field = mac + "_" + amid;

                            shardedRedisService.sadd("predown", field);


                            boolean sismember = shardedRedisService.sismember("predown", field);

                            logger.info("sismember is : " + sismember);

                            logger.info("amid is : " + amid);

                            logger.info("reportUrl is : " + reportUrl);

                            StringBuilder sb = new StringBuilder();


                            sb.append("http://192.168.3.4:8081/v1/g/1.gif?")
                                    .append("b=").append("")
                                    .append("c=").append("")//creativeid 写死
                                    .append("d=").append("")//plan id 写死
                                    .append("e=").append("")
                                    .append("f=").append("")
                                    .append("g=").append("3");


////                            HttpClientUtil.httpGet(sb.toString());
//                            sendData(sb.toString(), 3);



                            int code3 = 0;
                            try {
                                code3 = HttpClientUtil.httpGetAndCode(sb.toString(), 3000, 3000);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String resutl3 = code3 + "," + sb.toString() + "\n";
                            AdFileTools.writeLineToFile(recordePath, "out_3.txt", resutl3, true);


                        }
                    } else {
                        logger.info("无广告信息!");
                    }

                }


            } else {
                logger.warn("上报返回为空！");
            }
        } catch (IOException e) {
            logger.error("发送出错：", e);
        }
    }


}
