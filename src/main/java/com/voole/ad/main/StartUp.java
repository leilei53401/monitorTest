package com.voole.ad.main;

import java.io.IOException;

import com.voole.ad.file.ExpReportFromFile;
import com.voole.ad.file.Md5MacInRedisTest;
import com.voole.ad.file.PreDownSingleFileHttpReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class StartUp {

	public static void main(String[] args) throws IOException {
		final Logger log = LoggerFactory.getLogger(StartUp.class);
		log.info("程序开始启动...");
		long beginTime = System.currentTimeMillis();
        ApplicationContext app = null;
		try {
		       app = new ClassPathXmlApplicationContext("applicationContext.xml");

		} catch (Exception e) {
			e.printStackTrace();
			log.error("加载spring配置文件失败！");
			System.exit(0);
		}
		long startUpTime = System.currentTimeMillis() - beginTime;
		log.info("程序启动成功,耗时" + startUpTime + "ms.");

        //发送预下载上报测试任务
        log.info("启动预下载上报测试任务...");

//        PreDownSingleFileHttpReport preDownSingleFileHttpReport = app.getBean(PreDownSingleFileHttpReport.class);
//        preDownSingleFileHttpReport.parseFile();
//        preDownSingleFileHttpReport.sendHttpTest();
//        preDownSingleFileHttpReport.sendUrlTest();


        //md5MacInRedisTest
//        Md5MacInRedisTest md5MacInRedisTest = app.getBean(Md5MacInRedisTest.class);
//        md5MacInRedisTest.parseFile();


        ExpReportFromFile expReportFromFile = app.getBean(ExpReportFromFile.class);
        expReportFromFile.parseFile();



        log.info("结束预下载上报测试任务...");


    }
}