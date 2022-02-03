package com.api.autotest.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.autotest.common.utils.HttpHeader;
import com.api.autotest.common.utils.HttpParamers;
import com.api.autotest.dto.ApicasesAssert;
import com.api.autotest.dto.RequestObject;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TestCaseData {
    public static String logplannameandcasename = "";
    private Logger logger = null;
    TestMysqlHelp testMysqlHelp=null;
    public TestCaseData(Logger log,TestMysqlHelp mysqlHelp)
    {
        testMysqlHelp=mysqlHelp;
        logger=log;
    }

    //性能初始化数据根据jmeter传递下来的数据拼装用例请求的数据
    public RequestObject InitHttpDatabyJmeter(JavaSamplerContext context) throws Exception {
        RequestObject newob = new RequestObject();
        String casename = context.getParameter("casename");
        String executeplanname = context.getParameter("executeplanname");
        logplannameandcasename = executeplanname + "--" + casename + " :";
        logger.info(logplannameandcasename + "用例数据casename is :  " + casename);
        logger.info(logplannameandcasename + "用例数据executeplanname is :  " + executeplanname);

        String testplanid = context.getParameter("testplanid");
        String caseid = context.getParameter("caseid");
        String slaverid = context.getParameter("slaverid");
        String batchid = context.getParameter("batchid");
        String batchname = context.getParameter("batchname");

        String RequestmMthod = context.getParameter("RequestmMthod");
        logger.info(logplannameandcasename + "用例数据 RequestmMthod is :  " + RequestmMthod);

        String resource = context.getParameter("resource");
        logger.info(logplannameandcasename + "用例数据 resource is :  " + resource);

        String apistyle = context.getParameter("apistyle");
        logger.info(logplannameandcasename + "用例数据 apistyle is :  " + apistyle);

        String requestcontenttype = context.getParameter("requestcontenttype");
        logger.info(logplannameandcasename + "用例数据 requestcontenttype is :  " + requestcontenttype);

        String responecontenttype = context.getParameter("responecontenttype");
        logger.info(logplannameandcasename + "用例数据 responecontenttype is :  " + responecontenttype);

        String headjson = context.getParameter("headjson");
        logger.info(logplannameandcasename + "用例数据 headjson is :  " + headjson);

        String paramsjson = context.getParameter("paramsjson");
        logger.info(logplannameandcasename + "用例数据 paramsjson is :  " + paramsjson);

        String bodyjson = context.getParameter("bodyjson");
        String dubbo = context.getParameter("dubbojson");
        String expect = context.getParameter("expect");

        List<ApicasesAssert> apicasesAssertList = new ArrayList<>();
        if (expect.isEmpty()) {
            newob.setApicasesAssertList(apicasesAssertList);
        } else {
            apicasesAssertList = JSONObject.parseArray(expect, ApicasesAssert.class);
            newob.setApicasesAssertList(apicasesAssertList);
        }
        expect = GetAssertInfo(apicasesAssertList);
        logger.info(logplannameandcasename + "用例数据 expect is :  " + expect);

        String casetype = context.getParameter("casetype");
        logger.info(logplannameandcasename + "用例数据 casetype is :  " + casetype);

        String protocal = context.getParameter("protocal");
        logger.info(logplannameandcasename + "用例数据 protocal is :  " + protocal);

        //GetExpectMap(expect);
        newob.setCaseid(caseid);
        newob.setCasename(casename);
        newob.setTestplanid(testplanid);
        newob.setSlaverid(slaverid);
        newob.setBatchname(batchname);
        newob.setBatchid(batchid);
        newob.setExpect(expect);
        newob.setCasetype(casetype);

        newob.setResponecontenttype(responecontenttype);
        if (resource == null) {
            throw new Exception("测试集合对应的环境中未部署发布单元，请在测试环境中完成部署发布单元后再运行");
        }
        newob.setResource(resource);
        newob.setRequestmMthod(RequestmMthod);
        newob.setRequestcontenttype(requestcontenttype);
        newob.setApistyle(apistyle);
        newob.setProtocal(protocal);

        newob.setHeadjson(headjson);
        newob.setBodyjson(bodyjson);
        newob.setParamjson(paramsjson);

        return newob;
    }

    // 功能用例拼装请求需要的用例数据
    public RequestObject GetCaseRequestData(String PlanId, String TestCaseId, String SlaverId, String BatchId, String BatchName, String ExecPlanName) {
        RequestObject ro = new RequestObject();
        //ArrayList<HashMap<String, String>> planlist = getcaseData("select * from executeplan where id=" + PlanId);
        ArrayList<HashMap<String, String>> deployunitmachineiplist = testMysqlHelp.getcaseData("select m.ip,a.domain,a.visittype from macdepunit a INNER JOIN apicases b INNER JOIN executeplan c JOIN machine m on a.depunitid=b.deployunitid and  a.envid=c.envid and  m.id=a.machineid where b.id=" + TestCaseId + " and c.id=" + PlanId);
        ArrayList<HashMap<String, String>> caselist = testMysqlHelp.getcaseData("select * from apicases where id=" + TestCaseId);
        ArrayList<HashMap<String, String>> caseassertlist = testMysqlHelp.getcaseData("select * from apicases_assert where caseid=" + TestCaseId);
        ArrayList<HashMap<String, String>> apilist = testMysqlHelp.getcaseData("select b.visittype,b.apistyle,b.path,b.requestcontenttype,b.responecontenttype from apicases a inner join api b on a.apiid=b.id where a.id=" + TestCaseId);
        ArrayList<HashMap<String, String>> deployunitlist = testMysqlHelp.getcaseData("select b.protocal,b.port,b.id from apicases a inner join deployunit b on a.deployunitid=b.id where a.id=" + TestCaseId);

        //获取断言记录
        TestAssert testAssert = new TestAssert(logger);
        List<ApicasesAssert> apicasesAssertList = testAssert.GetApicasesAssertList(caseassertlist);
        ro.setApicasesAssertList(apicasesAssertList);
        // url请求资源路径
        String path = testMysqlHelp.getcaseValue("path", apilist);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String casetype = testMysqlHelp.getcaseValue("casetype", caselist);

        String CaseName = testMysqlHelp.getcaseValue("casename", caselist);

        String expect = GetAssertInfo(apicasesAssertList); //getcaseValue("expect", caselist);

        String Apiid = testMysqlHelp.getcaseValue("apiid", caselist);

        //获取请求响应的数据格式
        String requestcontenttype = testMysqlHelp.getcaseValue("requestcontenttype", apilist);
        String responecontenttype = testMysqlHelp.getcaseValue("responecontenttype", apilist);
        // http请求方式 get，post
        String method = testMysqlHelp.getcaseValue("visittype", apilist).toLowerCase();
        // api风格
        String apistyle = testMysqlHelp.getcaseValue("apistyle", apilist).toLowerCase();
        // 协议 http,https,rpc
        String protocal = testMysqlHelp.getcaseValue("protocal", deployunitlist).toLowerCase();
        // 发布单元端口
        String port = testMysqlHelp.getcaseValue("port", deployunitlist);
        String deployunitid = testMysqlHelp.getcaseValue("id", deployunitlist);

        // 获取发布单元访问方式，ip或者域名
        String deployunitvisittype = testMysqlHelp.getcaseValue("visittype", deployunitmachineiplist);
        // 根据访问方式来确定ip还是域名
        String testserver = "";
        String resource = "";
        if (deployunitvisittype.equals(new String("ip"))) {
            testserver = testMysqlHelp.getcaseValue("ip", deployunitmachineiplist);
            resource = protocal + "://" + testserver + ":" + port + path;
        } else {
            testserver = testMysqlHelp.getcaseValue("domain", deployunitmachineiplist);
            resource = protocal + "://" + testserver + path;
        }
        logger.info(logplannameandcasename + "用例数据 resource is :  " + resource + "   protocal  is:   " + protocal + "  expect is :      " + expect + "  visittype is: " + method + "   path is: " + path + " casetype is: " + casetype);

        ro.setCaseid(TestCaseId);
        ro.setCasename(CaseName);
        ro.setDeployunitid(deployunitid);
        ro.setTestplanid(PlanId);
        ro.setSlaverid(SlaverId);
        ro.setBatchname(BatchName);
        ro.setTestplanname(ExecPlanName);
        ro.setBatchid(BatchId);
        ro.setExpect(expect);
        ro.setCasetype(casetype);
        ro.setProtocal(protocal);
        ro.setApistyle(apistyle);
        ro.setRequestcontenttype(requestcontenttype);
        ro.setRequestmMthod(method);
        ro.setResource(resource);
        ro.setResponecontenttype(responecontenttype);
        return ro;
    }


    //获取断言信息
    private String GetAssertInfo(List<ApicasesAssert> apicasesAssertList) {
        String expectValue = "";
        if (apicasesAssertList.size() > 0) {
            for (ApicasesAssert apicasesAssert : apicasesAssertList) {
                if (apicasesAssert.getAsserttype().equals(new String("Respone"))) {
                    expectValue = expectValue + "【断言类型：" + apicasesAssert.getAsserttype() + "， 断言子类型：" + apicasesAssert.getAssertsubtype() + "， 断言条件：" + apicasesAssert.getAssertcondition() + "， 断言值：" + apicasesAssert.getAssertvalues() + "， 断言值类型：" + apicasesAssert.getAssertvaluetype() + "】";
                } else {
                    expectValue = expectValue + "【断言类型：" + apicasesAssert.getAsserttype() + "， 断言表达式：" + apicasesAssert.getExpression() + "， 断言条件：" + apicasesAssert.getAssertcondition() + "， 断言值：" + apicasesAssert.getAssertvalues() + "， 断言值类型：" + apicasesAssert.getAssertvaluetype() + "】";
                }
            }
        }
        return expectValue;
    }
}