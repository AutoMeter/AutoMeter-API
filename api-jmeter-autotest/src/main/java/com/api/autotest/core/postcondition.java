package com.api.autotest.core;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.File;
import java.util.Date;

/**
 * Created by fanseasn on 2020/12/8.
 */
/*
 @author Season
 @DESCRIPTION 
 @create 2020/12/8
*/
public class postcondition extends AbstractJavaSamplerClient {

    // 初始化方法，实际运行时每个线程仅执行一次，在测试方法运行前执行，类似于LoadRunner中的init方法
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
    }

    // 设置传入的参数，可以设置多个，已设置的参数会显示到Jmeter的参数列表中
    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        //定义一个参数，显示到Jmeter的参数列表中，第一个参数为参数默认的显示名称，第二个参数为默认值
        params.addArgument("testplanid", "11");
        params.addArgument("caseid", "15");
        params.addArgument("batchid", "11");
        params.addArgument("slaverid", "15");
        params.addArgument("batchname", "cornerservice2020-10-21-tag-100");
        params.addArgument("casetype", "/opt/");
        params.addArgument("casereportfolder", "/opt/");
        params.addArgument("testclass", "/opt/");
        params.addArgument("start", "1608107091283");

        return params;
    }

    // 测试执行的循环体，根据线程数和循环次数的不同可执行多次，类似于LoadRunner中的Action方法
    public SampleResult runTest(JavaSamplerContext ctx) {
//        SampleResult results = new SampleResult();
//        results.sampleEnd();
        return null;
    }

    //结束方法，实际运行时每个线程仅执行一次，在测试方法运行结束后执行，类似于LoadRunner中的end方法
    public void teardownTest(JavaSamplerContext ctx) {
        super.teardownTest(ctx);
        Testcore core = new Testcore(getLogger());
        String errorinfo = "";
        String status="";
        String caseid = ctx.getParameter("caseid");
        String testplanid = ctx.getParameter("testplanid");
        String batchid = ctx.getParameter("batchid");
        String batchname = ctx.getParameter("batchname");
        String slaverid = ctx.getParameter("slaverid");
        String casetype = ctx.getParameter("casetype");
        String casereportfolder = ctx.getParameter("casereportfolder");
        String testclass = ctx.getParameter("testclass");
        String start = ctx.getParameter("start");



        getLogger().info( "postcondition teardownTest 。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。:" );
        //后置条件
        try {
            core.fixpostcondition(testplanid,caseid);
            status="成功";
        } catch (Exception e) {
            status="失败";
            errorinfo ="后置条件处理异常："+ e.getMessage().replace("'","");
            getLogger().info(Testcore.logplannameandcasename + "后置条件处理发生异常:"+e.getMessage());
        }
        finally {
            String result= core.savetestcaseconditionresult(caseid,testplanid,batchid,batchname,slaverid,status,errorinfo,"后置",casetype);
            getLogger().info(Testcore.logplannameandcasename + "处理后置条件完成");
        }
        //更新调度表状态
        try {
            core.updatedispatchcasestatus(testplanid,batchid,slaverid,caseid);
            getLogger().info(Testcore.logplannameandcasename + "更新调度表状态完成");
        }
        catch (Exception ex)
        {
            getLogger().info(Testcore.logplannameandcasename + "更新调度表状态异常："+ex.getMessage());
        }
        //通知slaver性能测试解析报告，生成数据入库
        try {
            if(casetype.equals(new String("性能")))
            {
                File file1 = new File(casereportfolder+"/index.html");
                if(!file1.exists()) {
                    System.out.println("性能报告文件未生成。。。。。。。。。。。。。。。");
                }
                getLogger().info(Testcore.logplannameandcasename + "开始处理性能报告结果");

                getLogger().info(Testcore.logplannameandcasename + "处理性能报告出错获取的开始时间：" + start);

                long end = new Date().getTime();
                getLogger().info(Testcore.logplannameandcasename + "处理性能报告出错获取的结束时间：" + end);

                long starttime=Long.parseLong(start);
                getLogger().info(Testcore.logplannameandcasename + "处理性能报告出错获取的结束时间：" + end);

                double costtime=(double)(end-starttime)/1000;

                core.genealperformacestaticsreport(testclass,batchname,testplanid,batchid,slaverid,caseid,casereportfolder,costtime);
                getLogger().info(Testcore.logplannameandcasename + "处理性能报告结果完成");
            }
        } catch (Exception e) {
            getLogger().info(Testcore.logplannameandcasename + "处理性能报告出错：" + e.getMessage());
        }
    }

    // 本地调试
    public static void main(String[] args) {

        Arguments params = new Arguments();
        params.addArgument("testplanid", "12");
        params.addArgument("caseid", "1");
        params.addArgument("batchid", "1");
        params.addArgument("slaverid", "5");
        params.addArgument("batchname", "xxx10000");

        params.addArgument("casereportfolder", "/Users/fanseasn/Desktop/testresult/13-2-x100001");
        params.addArgument("casetype", "性能");
        params.addArgument("testclass", "retrySendSmsOrFindShortUrl");
        params.addArgument("start", "1608107091283");



//        long start=new Date().getTime();
//
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

//        long end=new Date().getTime();
//
//        long time=end-start;
//        System.out.println(time);

        JavaSamplerContext ctx = new JavaSamplerContext(params);
        postcondition test = new postcondition();
        test.setupTest(ctx);
        test.runTest(ctx);
        test.teardownTest(ctx);
        System.exit(0);

    }
}
