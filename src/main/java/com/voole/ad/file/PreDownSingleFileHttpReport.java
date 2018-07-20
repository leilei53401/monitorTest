package com.voole.ad.file;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.voole.ad.redis.ShardedRedisService;
import com.voole.ad.utils.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.functors.IdentityPredicate;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component("PreDownSingleFileHttpReport")
public class PreDownSingleFileHttpReport {

    final Logger logger = Logger.getLogger(PreDownSingleFileHttpReport.class);


    String recordePath = "/opt/webapps/predown_report/file";



    @Resource
    private ShardedRedisService shardedRedisService;

    public void parseFile() {
        long all = 0L;
        long fail = 0L;
        StringBuilder sb = new StringBuilder();
        StringBuilder predownreport = new StringBuilder();
        File theFile = new File("/opt/webapps/predown_report/file/20171101.txt");

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

                        //字段说明
                /*    starttime
                    mediaid
                    sessionid
                    oemid
                    creativeid
                    adpid
                    mac
                    planid
                    type
                    ip
                    provinceid
                    cityid
                    ts
                    duration
                    te
                    adsource
                    hourtime     */

                    /*20180120000334,900103,87146a2fd463e07da2c299efd9ebeadd1516377845417,900103101,1050001475,16,87146a2fd463e07da2c299efd9ebeadd,\N,0,114.240.1.9,110000,110100,\N,\N,0,1,0,90000010,20180120,\N,\N,\N,\N,\N,\N,\N,\N,20180120,90000010*/
                        //接口请求串：http://si.super-ssp.tv:8080/adPut.action?oemid=900103101&uid=&hid=67204eb8f95a39c0f16618d51bb299f9&width=&height=&fps=&mimeid=&isthird=1&isrt=0&areacode=0,0&adpos=10151010&ip=10.9.4.13&version=4.4&format=1

                        String[] data = StringUtils.split(line, ",");
                        if (data.length < 29) {
                            continue;
                        }

                        sb.append("http://si.super-ssp.tv:8080/adPut.action?");


                        String mediaid = data[1];

                        String oemid = data[3];
                        String mac = data[6];
                        String ip = data[9];
                        String provinceid = data[10];
                        String cityid = data[11];
                        String creativeid = data[4];
                        String adpos = data[5];
                        String planid = data[7];

                        sb = sb.append("oemid=").append(oemid)
                                .append("&hid=").append(mac)
                                .append("&areacode=")//.append(provinceid).append(",").append(cityid)
                                .append("&adpos=").append("10151010")
                                .append("&ip=").append(ip)
                                .append("&isrt=0&version=4.4&format=1");

                        //########################## 开始发送数据 ############################

                        try {
                            String res = HttpClientUtil.httpGet(sb.toString());
                            if (StringUtils.isNotBlank(res)) {

//                logger.info("resut: " + res);
                                JSONObject jsonRoot = JSONObject.fromObject(res);

                                //解析
                                String sessionid = jsonRoot.getString("sessionid");
//                logger.info("sessionid is : "+ sessionid);
                                JSONArray adinfos = jsonRoot.getJSONArray("adinfos");
                                Iterator adinfoIt = adinfos.iterator();

                                while (adinfoIt.hasNext()) {
                                    JSONObject adinfo = (JSONObject) adinfoIt.next();
//                                String invUrl = adinfo.getString("inventoryvalues");
                                    String pos = adinfo.getString("pos");
                                    //上报库存
//                                logger.info("invUrl is : "  + invUrl);
//                                logger.info("pos is : "  + pos);
                                    JSONArray invArray = adinfo.getJSONArray("inventoryvalues");
                                    Iterator invIt = invArray.iterator();
                                    while (invIt.hasNext()) {
                                        String invUrl = (String) invIt.next();
//                                    logger.info("invUrl is : "  + invUrl);

                                        //http://m.super-ssp.tv:8081/v1/b/1.gif?a=<MEDIAID>&b=<SESSIONID>&c=<OID>&d=<CREATIVEID>&
                                        // e=<ADPID>&f=<MAC>&g=<PLANID>&h=<TYPE>&i=<IP>&j=<PID>&k=<CID>&l=<TIMESTAMP>&
                                        // m=<DURATION>&n=<TE>&o=<AREATYPE>&p=<ADSOURCE>

                                        invUrl = invUrl.replaceAll("<MEDIAID>", mediaid).replaceAll("<SESSIONID>", sessionid).replaceAll("<OID>", oemid)
                                                .replaceAll("<CREATIVEID>", creativeid).replaceAll("<ADPID>", adpos).replaceAll("<MAC>", mac).replaceAll("<AREATYPE>", adpos)
                                                .replaceAll("<IP>", ip).replaceAll("<PLANID>","").replaceAll("<TYPE>","")
                                                .replaceAll("<PID>","").replaceAll("<CID>","").replaceAll("<TIMESTAMP>","")
                                                .replaceAll("<DURATION>","").replaceAll("<TE>","").replaceAll("<ADSOURCE>","");

                                        int code = 0;
                                        try {
                                            code = HttpClientUtil.httpGetAndCode(invUrl, 3000, 3000);
                                        } catch (IOException e) {
                                            logger.error("send error:", e);
                                        }
                                        String resutl = code + "," + invUrl.toString() + "\n";
                                        AdFileTools.writeLineToFile(recordePath, "out_1.txt", resutl, true);


                                        //解析广告
                                        JSONArray mediainfos = adinfo.getJSONArray("mediainfo");


                                        if (null != mediainfos && mediainfos.size() > 0) {

                                            Iterator mediainfoIt = mediainfos.iterator();

                                            while (mediainfoIt.hasNext()) {
                                                JSONObject mediainfoObj = (JSONObject) mediainfoIt.next();

                                                String amid = mediainfoObj.getString("amid");

//                                        String reportUrl = mediainfoObj.getString("reportvalues");

                                                //上报预下载成功

                                                String field = mac + "_" + amid;

                                                boolean sismember = shardedRedisService.sismember("predown", field);

                                                int downstatus = 1;

                                                if (sismember) {
                                                    //已下载，本地已存在
                                                    //m.super-ssp.tv/v1/g/1.gif?a=[stamp]&b=[mac]&c=[creativeid]&d=[planid]&e=[oemid]&f=[adposid]&g=[downstatus]

                                                    downstatus = 2;

                                                } else {
                                                    shardedRedisService.sadd("predown", field);
                                                    //上报预下载成功串。(有一定比例下载失败)
//                                            downstatus=1;
                                                    Random r = new Random();
                                                    downstatus = r.nextInt(2);
                                                }

                                                predownreport.append("http://192.168.3.4:8081/v1/g/1.gif?")
                                                        .append("a=")
                                                        .append("&b=").append(mac)
                                                        .append("&c=").append("1050001424")//creativeid 写死
                                                        .append("&d=").append("6272")//plan id 写死
                                                        .append("&e=").append(oemid)
                                                        .append("&f=").append("10151010")
                                                        .append("&g=").append(downstatus);


                                                int code3 = 0;
                                                try {
                                                    code3 = HttpClientUtil.httpGetAndCode(predownreport.toString(), 3000, 3000);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                String resutl3 = code3 + "," + predownreport.toString() + "\n";
                                                AdFileTools.writeLineToFile(recordePath, "out_3.txt", resutl3, true);

//                                        sendData(predownreport.toString(),3);


//                                        HttpClientUtil.httpGet(predownreport.toString());

//
//                                        logger.info("sismember is : " + sismember);
//
//                                        logger.info("amid is : " + amid);
//
//                                        logger.info("reportUrl is : " + reportUrl);

                                                //上报 曝光数据： reportUrl

                                                JSONArray reportArray = adinfo.getJSONArray("reportvalues");
                                                Iterator reportIt = reportArray.iterator();
                                                while (reportIt.hasNext()) {
                                                    String reportUrl = (String) reportIt.next();

                                                       reportUrl = reportUrl.replaceAll("<MEDIAID>", mediaid).replaceAll("<SESSIONID>", sessionid).replaceAll("<OID>", oemid)
                                                            .replaceAll("<CREATIVEID>", creativeid).replaceAll("<ADPID>", adpos).replaceAll("<MAC>", mac).replaceAll("<AREATYPE>", adpos)
                                                            .replaceAll("<IP>", ip).replaceAll("<PLANID>","").replaceAll("<TYPE>","")
                                                            .replaceAll("<PID>","").replaceAll("<CID>","").replaceAll("<TIMESTAMP>","")
                                                            .replaceAll("<DURATION>","").replaceAll("<TE>","").replaceAll("<ADSOURCE>","");

                                                    int code2 = 0;
                                                    try {
                                                        code2 = HttpClientUtil.httpGetAndCode(reportUrl, 3000, 3000);
                                                    } catch (IOException e) {
                                                        logger.error("send error:", e);
                                                    }
                                                    String resutl2 = code2 + "," + reportUrl + "\n";
                                                    AdFileTools.writeLineToFile(recordePath, "out_2.txt", resutl2, true);


                                                }
                                            }
                                        } else {
                                            logger.info("无广告信息!");
                                        }

                                    }


                                }
                            } else {
                                logger.warn("上报返回为空！");
                            }
                        } catch (IOException e) {
                            logger.error("发送出错：", e);
                        }

                        //######################## 发送数据结束 #############################


                        sb.setLength(0);
                        predownreport.setLength(0);



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
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
