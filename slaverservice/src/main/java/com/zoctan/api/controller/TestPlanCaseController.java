package com.zoctan.api.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.zoctan.api.core.response.Result;
import com.zoctan.api.core.response.ResultGenerator;
import com.zoctan.api.dto.Testplanandbatch;
import com.zoctan.api.entity.*;
import com.zoctan.api.entity.Dictionary;
import com.zoctan.api.mapper.*;
import com.zoctan.api.service.*;
import com.zoctan.api.service.TestPlanCaseService;
import com.zoctan.api.service.impl.TestPlanCaseServiceImpl;
import com.zoctan.api.util.IPHelpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.ResolverUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.entity.Condition;
import com.alibaba.fastjson.JSON;


import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Zoctan
 * @date 2020/04/17
 */
@Slf4j
@RestController
@RequestMapping("/exectestplancase")
public class TestPlanCaseController {
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Autowired
    private ExecuteplanbatchMapper executeplanbatchMapper;
    @Autowired
    private ExecuteplanTestcaseMapper executeplanTestcaseMapper;
    @Autowired
    private ExecuteplanMapper executeplanMapper;
    @Autowired
    private ExecuteplanService executeplanService;
    @Autowired
    private SlaverMapper slaverMapper;
    @Autowired(required = false)
    private DictionaryMapper dictionaryMapper;
    @Autowired
    private DispatchMapper dispatchMapper;
    @Autowired(required = false)
    private DeployunitService deployunitService;
    @Autowired(required = false)
    private ApicasesMapper apicasesMapper;
    @Autowired(required = false)
    private TestPlanCaseService tpcservice;
    @Autowired(required = false)
    private ApicasesReportService apicasereportservice;
    @Autowired(required = false)
    private ApicasesService apicasesService;
    @Autowired(required = false)
    private ApicasesVariablesService apicasesVariablesService;
    @Autowired(required = false)
    private TestvariablesValueService testvariablesValueService;
    @Autowired(required = false)
    private ApiCasedataService apiCasedataService;
    @Autowired(required = false)
    private TestvariablesService testvariablesService;
    @Autowired(required = false)
    private ApiParamsService apiParamsService;
    @Autowired(required = false)
    private ApiService apiService;
    @Autowired(required = false)
    private ExecuteplanService epservice;
    @Autowired(required = false)
    private MacdepunitService macdepunitService;
    @Autowired(required = false)
    private MachineService machineService;
    @Autowired(required = false)
    private ApicasesAssertService apicasesAssertService;
    @Autowired(required = false)
    private ExecuteplanParamsService executeplanParamsService;
    @Autowired(required = false)
    private ApicasesReportPerformanceMapper apicasesReportPerformanceMapper;

    @Autowired(required = false)
    private VariablesService variablesService;

    @Autowired(required = false)
    private DbvariablesService dbvariablesService;

    @Autowired(required = false)
    private GlobalheaderuseService globalheaderuseService;

    @Autowired(required = false)
    private GlobalheaderParamsService globalheaderParamsService;

    @Autowired(required = false)
    private GlobalvariablesService globalvariablesService;

    @PostMapping("/exec")
    //    public Result exec(@RequestBody List<TestplanCase> plancaseList) {
    public Result exec(@RequestBody Testplanandbatch planbatch) throws Exception {
        // ??????testcenter???????????????admin???????????????Request URL: http://localhost:8080/account/token  {name: "admin", password: "admin123"}
        // ????????????????????????Authorization = token
        Long execplanid = planbatch.getPlanid();
        String batchname = planbatch.getBatchname();
        Executeplanbatch epb = executeplanbatchMapper.getbatchidbyplanidandbatchname(execplanid, batchname);
        // ??????plan?????????????????????????????????new???stop???finish????????????
        Executeplan ep = executeplanMapper.findexplanWithid(execplanid);
        List<ExecuteplanTestcase> caselist = executeplanTestcaseMapper.findcasebytestplanid(execplanid);
        TestPlanCaseController.log.info("??????id" + execplanid + " ????????????" + batchname + " ??????????????????" + caselist.size());
//        InetAddress address = InetAddress.getLocalHost();
//        String ip = address.getHostAddress();
        String ip = IPHelpUtils.getInet4Address();
        TestPlanCaseController.log.info("???????????????IP??????" + ip);
        List<Slaver> slaverlist = slaverMapper.findslaverbyip(ip);
        List<Dispatch> dispatchList = new ArrayList<>();
        if (slaverlist.size() == 0) {
            TestPlanCaseController.log.info("??????????????????IP??????" + ip + " ?????????????????????????????????");
            throw new Exception("??????????????????IP??????" + ip + " ?????????????????????????????????");
        } else {
            Long slaverid = slaverlist.get(0).getId();
            String slavername = slaverlist.get(0).getSlavername();
            TestPlanCaseController.log.info("slaverid???" + slaverid + " slavername ???" + slavername);
            for (ExecuteplanTestcase testcase : caselist) {
                //????????????????????????????????????????????????????????????????????????
                Dispatch dis = new Dispatch();
                dis.setExpect(testcase.getExpect());
                dis.setExecplanid(execplanid);
                dis.setTestcaseid(testcase.getTestcaseid());
                dis.setDeployunitname(testcase.getDeployunitname());
                dis.setStatus("?????????");
                dis.setBatchname(batchname);
                dis.setBatchid(epb.getId());
                dis.setCasejmxname(testcase.getCasejmxname());
                dis.setExecplanname(ep.getExecuteplanname());
                dis.setSlaverid(slaverid);
                dis.setSlavername(slavername);
                dis.setTestcasename(testcase.getCasename());
                dis.setPlantype(ep.getUsetype());
                dis.setThreadnum(testcase.getThreadnum());
                dis.setLoops(testcase.getLoops());
                dispatchList.add(dis);
                //dispatchService.save(dis);
            }
        }
        dispatchMapper.insertBatchDispatch(dispatchList);
        TestPlanCaseController.log.info("?????????????????????????????????" + dispatchList.size());
        return ResultGenerator.genOkResult();
    }

    public boolean jmeterclassexistornot(String jarpath, String jmeterclassname) {
        boolean flag = false;
        try {
            //???????????????????????????????????????????????????????????????????????????File??????
            File f = new File(jarpath);
            URL url1 = f.toURI().toURL();
            URLClassLoader myClassLoader = new URLClassLoader(new URL[]{url1}, Thread.currentThread().getContextClassLoader());

            //??????jarFile???JarEntry??????????????????
            JarFile jar = new JarFile(jarpath);
            //??????zip?????????????????????
            Enumeration<JarEntry> enumFiles = jar.entries();
            JarEntry entry;

            //??????????????????????????????????????????
            while (enumFiles.hasMoreElements()) {
                entry = (JarEntry) enumFiles.nextElement();
                if (entry.getName().indexOf("META-INF") < 0) {
                    String classFullName = entry.getName();
                    if (classFullName.indexOf(".class") > 0) {
                        //????????????.class
                        String className = classFullName.substring(0, classFullName.length() - 6).replace("/", ".");
                        if (className.equals(jmeterclassname)) {
                            flag = true;
                        }
                        //????????????
                        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~????????????:~~~~~~~~~~~~~~~~~~~~~~~~~" + className);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    @PostMapping("/test")
    public Result gettest(@RequestBody ExecuteplanTestcase plancase) {
        //tpcservice.executeplancase(plancase.getExecuteplanid(),plancase.getTestcaseid());
        return ResultGenerator.genOkResult();
    }

    @PostMapping("/execperformancetest")
    public Result execperformancetest(@RequestBody Dispatch dispatch) throws Exception {
        long Execplanid=dispatch.getExecplanid();
        Executeplan executeplan=executeplanService.getBy("id",Execplanid);
        String property = System.getProperty("os.name");
        String ip = null;
        ip = IPHelpUtils.getInet4Address();
        List<Slaver> slaverlist = slaverMapper.findslaverbyip(ip);
        if (slaverlist.size() == 0) {
            TestPlanCaseController.log.error("????????????-????????????slaver????????????????????????" + "?????????ip??????" + ip + "???slaver????????????????????????-????????????");
            return ResultGenerator.genFailedResult("?????????ip??????" + ip + "???slaver");
        }
        Long SlaverId = slaverlist.get(0).getId();
        String ProjectPath = System.getProperty("user.dir");
        String JmeterPath = "";
        String JmxPath = "";
        String JmeterPerformanceReportPath = "";
        String JmeterPerformanceReportLogFilePath = "";

        if (ProjectPath.contains("slaverservice")) {
            if(property.toLowerCase().startsWith("win")){
                JmeterPath = ProjectPath + "\\apache-jmeter-5.3\\bin";
                JmxPath = ProjectPath + "\\servicejmxcase";
                JmeterPerformanceReportPath = ProjectPath + "\\performancereport";
                JmeterPerformanceReportLogFilePath = ProjectPath + "\\performancereportlogfile";
            }else {
                JmeterPath = ProjectPath + "/apache-jmeter-5.3/bin";
                JmxPath = ProjectPath + "/servicejmxcase";
                JmeterPerformanceReportPath = ProjectPath + "/performancereport";
                JmeterPerformanceReportLogFilePath = ProjectPath + "/performancereportlogfile";
            }
//            JmeterPath = ProjectPath + "\\apache-jmeter-5.3\\bin";
//            JmxPath = ProjectPath + "\\servicejmxcase";
//            JmeterPerformanceReportPath = ProjectPath + "\\performancereport";
//            JmeterPerformanceReportLogFilePath = ProjectPath + "\\performancereportlogfile";

        } else {
            if(property.toLowerCase().startsWith("win")){
                JmeterPath = ProjectPath + "\\slaverservice\\apache-jmeter-5.3\\bin";
                JmxPath = ProjectPath + "\\slaverservice\\servicejmxcase";
                JmeterPerformanceReportPath = ProjectPath + "\\slaverservice\\performancereport";
                JmeterPerformanceReportLogFilePath = ProjectPath + "\\slaverservice\\performancereportlogfile";
            }else {
                JmeterPath = ProjectPath + "/slaverservice/apache-jmeter-5.3/bin";
                JmxPath = ProjectPath + "/slaverservice/servicejmxcase";
                JmeterPerformanceReportPath = ProjectPath + "/slaverservice/performancereport";
                JmeterPerformanceReportLogFilePath = ProjectPath + "/slaverservice/performancereportlogfile";
            }
        }

        File dir = new File(JmeterPerformanceReportPath);
        if (!dir.exists()) {// ????????????????????????
            dir.mkdir();
            TestPlanCaseController.log.info("????????????????????????performancereport?????? :" + JmeterPerformanceReportPath);
        }
        File dirlog = new File(JmeterPerformanceReportLogFilePath);
        if (!dirlog.exists()) {// ????????????????????????
            dirlog.mkdir();
            TestPlanCaseController.log.info("??????????????????????????????performancereport?????? :" + JmeterPerformanceReportLogFilePath);
        }
        String JmxCaseName = dispatch.getCasejmxname();
        String DeployUnitName = dispatch.getDeployunitname();
        String CaseName = dispatch.getTestcasename();
        TestPlanCaseController.log.info("????????????-????????????????????????????????? is......." + CaseName);
        Deployunit Deployunit = deployunitService.findDeployNameValueWithCode(DeployUnitName,executeplan.getProjectid());
        if (Deployunit == null) {
            return ResultGenerator.genFailedResult("????????????????????????" + DeployUnitName);
        }
        String Protocal = Deployunit.getProtocal();
        //?????????http,https???????????????httpapitestcase??????functionhttpapi??????performancehttpapi???????????????
        String JmeterClassName = "";
        String ClassName = "";
        String DeployUnitNameForJmeter = "";
        if (Protocal.equals("http") || Protocal.equals("https")) {
            DeployUnitNameForJmeter = "httpapitestcase";
            JmeterClassName = "HttpApiPerformance";
            ClassName = "com.api.autotest.test." + DeployUnitNameForJmeter + "." + JmeterClassName;
        }
        if (Protocal.equals("rpc")) {
            DeployUnitNameForJmeter = dispatch.getDeployunitname();
            JmeterClassName = DeployUnitName;
            ClassName = "com.api.autotest.test." + DeployUnitName + "." + JmxCaseName;
        }
        TestPlanCaseController.log.info("????????????-DeployUnitNameForJmeter is......." + DeployUnitNameForJmeter + " JmeterClassName is........" + JmeterClassName);
        if (!JmeterClassExist(ClassName, JmeterPath)) {
            JmeterClassNotExist(dispatch, ClassName, CaseName);
            String memo = CaseName + "??????????????????JmeterClass???" + ClassName;
            dispatchMapper.updatedispatchstatusandmemo("????????????", memo, dispatch.getSlaverid(), dispatch.getExecplanid(), dispatch.getBatchid(), dispatch.getTestcaseid());
        }
        else
        {
            JmeterPerformanceObject jmeterPerformanceObject = null;
            try {
                jmeterPerformanceObject = GetJmeterPerformance(dispatch);
                if (jmeterPerformanceObject != null) {
                    // ???????????? ???????????????????????????????????????stop???????????????????????????,return ???
                    tpcservice.ExecuteHttpPerformancePlanCase(jmeterPerformanceObject, DeployUnitNameForJmeter, JmeterPath, JmxPath, JmeterClassName, JmeterPerformanceReportPath, JmeterPerformanceReportLogFilePath, dispatch.getThreadnum(), dispatch.getLoops(), dispatch.getCreator());
                    // ?????????????????????????????????????????????
                    dispatchMapper.updatedispatchstatus("?????????", dispatch.getSlaverid(), dispatch.getExecplanid(), dispatch.getBatchid(), dispatch.getTestcaseid());
                    TestPlanCaseController.log.info("????????????-??????????????????????????????????????????????????????????????????????????????????????????????????????dispatch??????????????????.....????????????jmeter..???????????????????????????????????????????????????????????????????????????" + dispatch.getId());
                    slaverMapper.updateSlaverStaus(SlaverId, "?????????");
                    executeplanbatchMapper.updatebatchstatus(dispatch.getExecplanid(), dispatch.getBatchname(), "?????????");
                    TestPlanCaseController.log.info("????????????-??????????????????????????????????????????????????????????????????????????????????????????????????????jmeter??????..???????????????????????????????????????????????????????????????????????????" + dispatch.getId());
                }
            } catch (Exception ex) {
                dispatchMapper.updatedispatchstatusandmemo("????????????", "?????????Slaver???????????????????????????" + ex.getMessage(), dispatch.getSlaverid(), dispatch.getExecplanid(), dispatch.getBatchid(), dispatch.getTestcaseid());
                ex.printStackTrace();
                TestPlanCaseController.log.info("????????????-?????????????????????????????????????????????????????????????????????????????????????????????????????????????????? JmeterPerformanceObject??????????????????..???????????????????????????????????????????????????????????????????????????" + ex.getMessage());
            }
        }
        return ResultGenerator.genOkResult();
    }

    @PostMapping("/execfunctiontest")
    public Result execfunctiontest(@RequestBody List<Dispatch> dispatchList) throws Exception {
        String property = System.getProperty("os.name");
        String ip = null;
        ip = IPHelpUtils.getInet4Address();
        List<Slaver> slaverlist = slaverMapper.findslaverbyip(ip);
        if (slaverlist.size() == 0) {
            TestPlanCaseController.log.error("????????????-????????????slaver????????????????????????" + "?????????ip??????" + ip + "???slaver????????????????????????-????????????");
            return ResultGenerator.genFailedResult("?????????IP??????" + ip + "???slaver");
        }
        Long SlaverId = slaverlist.get(0).getId();
        try {
            String ProjectPath = System.getProperty("user.dir");
            String JmeterPath = "";
            String JmxPath = "";
            if (ProjectPath.contains("slaverservice")) {
                if(property.toLowerCase().startsWith("win")){
                    JmeterPath = ProjectPath + "\\apache-jmeter-5.3\\bin";
                    JmxPath = ProjectPath + "\\servicejmxcase";
                }else {
                    JmeterPath = ProjectPath + "/apache-jmeter-5.3/bin";
                    JmxPath = ProjectPath + "/servicejmxcase";
                }

            } else {
                if(property.toLowerCase().startsWith("win")){
                    JmeterPath = ProjectPath + "\\slaverservice\\apache-jmeter-5.3\\bin";
                    JmxPath = ProjectPath + "\\slaverservice\\servicejmxcase";
                }else {
                    JmeterPath = ProjectPath + "/slaverservice/apache-jmeter-5.3/bin";
                    JmxPath = ProjectPath + "/slaverservice/servicejmxcase";
                }

            }
            TestPlanCaseController.log.info("????????????-????????????????????????????????????????????????????????????????????????????????????????????????????????????" + dispatchList.size());
            int FunctionJmeter = 1;// GetJmeterProcess("FunctionJmeterProcess", "??????");
            HashMap<String, List<Dispatch>> ProtocolDispatchRun = GetProtocolDispatch(dispatchList);
            for (String Protocol : ProtocolDispatchRun.keySet()) {
                int ProtocolJmeterNum = 0;
                if (FunctionJmeter == 1) {
                    ProtocolJmeterNum = 1;
                } else {
                    ProtocolJmeterNum = FunctionJmeter / ProtocolDispatchRun.size();
                }
                TestPlanCaseController.log.info("????????????-ProtocolJmeterNum???????????????????????????" + ProtocolJmeterNum);
                List<List<Dispatch>> last = FunctionDispatch(ProtocolJmeterNum, ProtocolDispatchRun.get(Protocol));
                if (Protocol.equalsIgnoreCase("http") || Protocol.equalsIgnoreCase("https")) {
                    for (int i = 0; i < last.size(); i++) {
                        List<Dispatch> JmeterList = last.get(i);
                        String DispatchIDs = "";
                        for (Dispatch dis : JmeterList) {
                            DispatchIDs = DispatchIDs + dis.getId() + ",";
                        }
                        if (!DispatchIDs.isEmpty()) {
                            DispatchIDs = DispatchIDs.substring(0, DispatchIDs.length() - 1);
                            TestPlanCaseController.log.info("????????????-DispatchIDs:=======================" + DispatchIDs);
                            try {
                                tpcservice.ExecuteHttpPlanFunctionCase(SlaverId, JmeterPath, JmxPath, DispatchIDs, url, username, password, i);
                                for (Dispatch dis : JmeterList) {
                                    dispatchMapper.updatedispatchstatus("?????????", dis.getSlaverid(), dis.getExecplanid(), dis.getBatchid(), dis.getTestcaseid());
                                    TestPlanCaseController.log.info("????????????-?????????????????????????????????????????????????????????" + dis.getId());
                                }
                                slaverMapper.updateSlaverStaus(SlaverId, "?????????");
                            } catch (Exception ex) {
                                TestPlanCaseController.log.info("??????JmeterCMD?????????????????????????????????" + ex.getMessage());
                            }
                        }
                    }
                }
                if (Protocol.equals(new String("rpc"))) {
                    String JmeterClassName = "";
                    String DeployUnitNameForJmeter = "";
                }
            }
        } catch (Exception ex) {
            slaverMapper.updateSlaverStaus(SlaverId, "??????");
            TestPlanCaseController.log.error("????????????-execfunctiontest ??????:=======================" + ex.getMessage());
            return ResultGenerator.genFailedResult(ex.getMessage());
        }
        return ResultGenerator.genOkResult();
    }


    public JmeterPerformanceObject GetJmeterPerformanceCaseData(Dispatch dispatch) throws Exception {
        JmeterPerformanceObject jmeterPerformanceObject = new JmeterPerformanceObject();
        jmeterPerformanceObject.setTestplanid(dispatch.getExecplanid());
        jmeterPerformanceObject.setCaseid(dispatch.getTestcaseid());
        jmeterPerformanceObject.setSlaverid(dispatch.getSlaverid());
        jmeterPerformanceObject.setBatchid(dispatch.getBatchid());
        jmeterPerformanceObject.setCasename(dispatch.getTestcasename());
        jmeterPerformanceObject.setBatchname(dispatch.getBatchname());
        jmeterPerformanceObject.setExecuteplanname(dispatch.getExecplanname());
        Apicases apicases = apicasesService.getBy("id", dispatch.getTestcaseid());
        if (apicases == null) {
            throw new Exception("?????????????????????????????????????????????");
        }
        jmeterPerformanceObject.setCasetype(apicases.getCasetype());
        Api api = apiService.getBy("id", apicases.getApiid());
        if (api == null) {
            throw new Exception("??????????????????API??????????????????????????????");
        }
        jmeterPerformanceObject.setApistyle(api.getApistyle());
        jmeterPerformanceObject.setRequestmMthod(api.getVisittype());
        jmeterPerformanceObject.setRequestcontenttype(api.getRequestcontenttype());
        jmeterPerformanceObject.setResponecontenttype(api.getResponecontenttype());
        Deployunit deployunit = deployunitService.getBy("id", api.getDeployunitid());
        if (deployunit == null) {
            throw new Exception("??????????????????API????????????????????????????????????????????????");
        }
        jmeterPerformanceObject.setProtocal(deployunit.getProtocal());

        Executeplan executeplan = epservice.getBy("id", dispatch.getExecplanid());
        if (executeplan == null) {
            throw new Exception("????????????????????????????????????????????????????????????");
        }
        Long EnvID = executeplan.getEnvid();
        Macdepunit macdepunit = macdepunitService.getmacdepbyenvidanddepid(EnvID, deployunit.getId());
        if (macdepunit != null) {
            String testserver = "";
            String resource = "";
            String BaseUrl = deployunit.getBaseurl();
            if (macdepunit.getVisittype().equalsIgnoreCase("ip")) {
                Long MachineID = macdepunit.getMachineid();
                Machine machine = machineService.getBy("id", MachineID);
                if (machine == null) {
                    throw new Exception("??????????????????????????????????????????????????????????????????");
                }
                jmeterPerformanceObject.setMachineip(machine.getIp());
                testserver = machine.getIp();

                if (BaseUrl == null || BaseUrl.isEmpty()) {
                    resource = deployunit.getProtocal() + "://" + testserver + ":" + deployunit.getPort() + api.getPath();
                } else {
                    if (BaseUrl.startsWith("/")) {
                        resource = deployunit.getProtocal() + "://" + testserver + ":" + deployunit.getPort() + BaseUrl + api.getPath();
                    } else {
                        resource = deployunit.getProtocal() + "://" + testserver + ":" + deployunit.getPort() + "/" + BaseUrl + api.getPath();
                    }
                    TestPlanCaseController.log.info("GetJmeterPerformanceCaseData resource ip is:"+resource);
                }
            } else {
                testserver = macdepunit.getDomain();
                if (BaseUrl == null || BaseUrl.isEmpty()) {
                    resource = deployunit.getProtocal() + "://" + testserver + api.getPath();
                } else {
                    if (BaseUrl.startsWith("/")) {
                        resource = deployunit.getProtocal() + "://" + testserver + BaseUrl + api.getPath();

                    } else {
                        resource = deployunit.getProtocal() + "://" + testserver + "/" + BaseUrl + api.getPath();
                    }
                }
                TestPlanCaseController.log.info("GetJmeterPerformanceCaseData resource domain is:"+resource);
            }
            jmeterPerformanceObject.setDeployunitvisittype(macdepunit.getVisittype());
            jmeterPerformanceObject.setResource(resource.trim());
        } else {
            throw new Exception("???????????????????????????API????????????????????????????????????????????????");
        }

        List<ApicasesAssert> apicasesAssertList = apicasesAssertService.findAssertbycaseid(dispatch.getTestcaseid().toString());
        if (apicasesAssertList.size() > 0) {
            String ExpectJson = JSON.toJSONString(apicasesAssertList);
            jmeterPerformanceObject.setExpect(ExpectJson);
        } else {
            jmeterPerformanceObject.setExpect("");
        }

        jmeterPerformanceObject.setMysqlurl(url.trim());
        jmeterPerformanceObject.setMysqlusername(username.trim());
        jmeterPerformanceObject.setMysqlpassword(password.trim());
        return jmeterPerformanceObject;
    }

    public JmeterPerformanceObject GetJmeterPerformanceCaseRequestData(JmeterPerformanceObject jmeterPerformanceObject, Dispatch dispatch, Api api) throws Exception {
        String PlanID = String.valueOf(jmeterPerformanceObject.getTestplanid());
        String BatchName = jmeterPerformanceObject.getBatchname();
        String RequestContentType = jmeterPerformanceObject.getRequestcontenttype();
        long Caseid=jmeterPerformanceObject.getCaseid();
        Apicases apicases= apicasesService.getBy("id",Caseid);
        long projectid=apicases.getProjectid();

        List<ApiCasedata> apiCasedataList = apiCasedataService.getcasedatabycaseid(dispatch.getTestcaseid());

        TestPlanCaseController.log.info("GetJmeterPerformanceCaseRequestData :" + PlanID + " BatchName:" + BatchName);
        List<TestvariablesValue> testvariablesValueList = testvariablesValueService.gettvlist(Long.parseLong(PlanID), BatchName, "??????");

        List<TestvariablesValue> DBtestvariablesValueList = testvariablesValueService.gettvlist(Long.parseLong(PlanID), BatchName, "?????????");

        TestPlanCaseController.log.info("gettvlist size :" + testvariablesValueList.size());

        HashMap<String, String> InterFaceMap = new HashMap<>();
        for (TestvariablesValue te : testvariablesValueList) {
            InterFaceMap.put(te.getVariablesname(), te.getVariablesvalue());
            TestPlanCaseController.log.info("???????????????"+te.getVariablesname()+" ???????????????: "+te.getVariablesvalue());
        }

        HashMap<String, String> DBMap = new HashMap<>();
        for (TestvariablesValue te : DBtestvariablesValueList) {
            DBMap.put(te.getVariablesname(), te.getVariablesvalue());
        }

        Condition gvcon = new Condition(Globalvariables.class);
        gvcon.createCriteria().andCondition("projectid = "+projectid);
        List<Globalvariables> globalvariablesList= globalvariablesService.listByCondition(gvcon);

        HashMap<String, String> GlobalVariablesHashMap = new HashMap<>();
        for (Globalvariables va : globalvariablesList) {
            GlobalVariablesHashMap.put(va.getKeyname(), va.getKeyvalue());
        }


        //1.Url??????????????????
        String RequestUrl = jmeterPerformanceObject.getResource();
        for (String VariableName : InterFaceMap.keySet()) {
            String UseVariableName = "<" + VariableName + ">";
            if (RequestUrl.contains(UseVariableName)) {
                String VariableValue = InterFaceMap.get(VariableName);
                RequestUrl = RequestUrl.replace(UseVariableName, VariableValue);
            }
        }
        //2.Url?????????????????????
        for (String VariableName : DBMap.keySet()) {
            String UseVariableName = "<<" + VariableName + ">>";
            if (RequestUrl.contains(UseVariableName)) {
                String VariableValue = DBMap.get(VariableName);
                RequestUrl = RequestUrl.replace(UseVariableName, VariableValue);
            }
        }

        //3.????????????Url??????
        for (Globalvariables variables : globalvariablesList) {
            String VariableName = "$" + variables.getKeyname() + "$";
            if (RequestUrl.contains(VariableName)) {
                Object VariableValue = variables.getKeyvalue();
                RequestUrl = RequestUrl.replace(VariableName, VariableValue.toString());
            }
        }

        jmeterPerformanceObject.setResource(RequestUrl);
        //String Caseid=dispatch.getTestcaseid().toString();
        HashMap<String, Object> HeaderMap = new HashMap<>();
        HashMap<String, Object> ParamsMap = new HashMap<>();
        HashMap<String, Object> BodyMap = new HashMap<>();
        String PostData = "";
        HashMap<String, String> globalheaderParamsHashMap = new HashMap<>();
        Map<String,Object>params=new HashMap<>();
        params.put("executeplanid",dispatch.getExecplanid());
        List<Globalheaderuse> globalheaderuseList = globalheaderuseService.searchheaderbyepid(params);
        for (ApiCasedata apiCasedata : apiCasedataList) {
            String ParamName = apiCasedata.getApiparam();
            String Paramvalue = apiCasedata.getApiparamvalue();
            String DataType = apiCasedata.getParamstype();
            if (apiCasedata.getPropertytype().equalsIgnoreCase("Params")) {
                Object Result = Paramvalue;
                if ((Paramvalue.contains("<") && Paramvalue.contains(">")) || (Paramvalue.contains("<<") && Paramvalue.contains(">>")) || (Paramvalue.contains("[") && Paramvalue.contains("]")) || (Paramvalue.contains("$") && Paramvalue.contains("$"))) {
                    Result = GetVaraibaleValue(Paramvalue, InterFaceMap, DBMap,GlobalVariablesHashMap,projectid);
                }
                Object LastObjectValue = GetDataByType(Result.toString(), DataType);
                ParamsMap.put(ParamName, LastObjectValue);
            }
            if (apiCasedata.getPropertytype().equalsIgnoreCase("Header")) {
                globalheaderParamsHashMap.put(ParamName,Paramvalue);
                TestPlanCaseController.log.info("Header???????????????"+ParamName+" Header??????Value: "+Paramvalue);
                //?????????????????????header???k,v,??????????????????header????????????????????????
//                for (Globalheaderuse globalheaderuse :globalheaderuseList) {
//                    Map<String, Object> headeridparams=new HashMap<>();
//                    headeridparams.put("globalheaderid",globalheaderuse.getGlobalheaderid());
//                    TestPlanCaseController.log.info("??????Header??????--globalheadername???"+globalheaderuse.getGlobalheadername());
//                    globalheaderParamsList= globalheaderParamsService.findGlobalheaderParamsWithName(headeridparams);
//                    for (GlobalheaderParams globalheaderParams : globalheaderParamsList) {
//                        TestPlanCaseController.log.info("??????Header??????globalheaderParams???"+globalheaderParams.getKeyname()+" globalheaderparamvalue:"+globalheaderParams.getKeyvalue());
//                        globalheaderParamsHashMap.put(globalheaderParams.getKeyname(), globalheaderParams.getKeyvalue());
//                    }
//                }
//                Object Result = Paramvalue;
//                if ((Paramvalue.contains("<") && Paramvalue.contains(">")) || (Paramvalue.contains("<<") && Paramvalue.contains(">>")) || (Paramvalue.contains("[") && Paramvalue.contains("]")) || (Paramvalue.contains("$") && Paramvalue.contains("$"))) {
//                    Result = GetVaraibaleValue(Paramvalue, InterFaceMap, DBMap,GlobalVariablesHashMap);
//                }
//                HeaderMap.put(ParamName, Result);
//                TestPlanCaseController.log.info("??????Header?????????"+ParamName+" ??????Header???Value: "+Result);
            }
            if (apiCasedata.getPropertytype().equalsIgnoreCase("Body")) {
                if (RequestContentType.equalsIgnoreCase("Form??????")) {
                    Object Result = Paramvalue;
                    if ((Paramvalue.contains("<") && Paramvalue.contains(">")) || (Paramvalue.contains("<<") && Paramvalue.contains(">>")) || (Paramvalue.contains("[") && Paramvalue.contains("]")) || (Paramvalue.contains("$") && Paramvalue.contains("$"))) {
                        Result = GetVaraibaleValue(Paramvalue, InterFaceMap, DBMap,GlobalVariablesHashMap,projectid);
                    }
                    Object LastObjectValue = GetDataByType(Result.toString(), DataType);
                    BodyMap.put(ParamName, LastObjectValue);
                } else {
                    PostData = Paramvalue;
                    //??????????????????
                    for (String VariableName : InterFaceMap.keySet()) {
                        String UseVariableName = "<" + VariableName + ">";
                        if (PostData.contains(UseVariableName)) {
                            String VariableValue = InterFaceMap.get(VariableName);
                            PostData = PostData.replace(UseVariableName, VariableValue);
                        }
                    }
                    //?????????????????????
                    for (String VariableName : DBMap.keySet()) {
                        String UseVariableName = "<<" + VariableName + ">>";
                        if (PostData.contains(UseVariableName)) {
                            String VariableValue = DBMap.get(VariableName);
                            PostData = PostData.replace(UseVariableName, VariableValue);
                        }
                    }

                    //??????????????????
                    for (Globalvariables variables : globalvariablesList) {
                        String VariableName = "$" + variables.getKeyname() + "$";
                        if (PostData.contains(VariableName)) {
                            Object VariableValue = GlobalVariablesHashMap.get(variables.getKeyname());
                            PostData = PostData.replace(VariableName, VariableValue.toString());
                        }
                    }
                }
            }
        }

        //?????????????????????header???k,v,??????????????????header????????????????????????
        for (Globalheaderuse globalheaderuse :globalheaderuseList) {
            Map<String, Object> headeridparams=new HashMap<>();
            headeridparams.put("globalheaderid",globalheaderuse.getGlobalheaderid());
            TestPlanCaseController.log.info("??????Header??????--globalheadername???"+globalheaderuse.getGlobalheadername());
            List<GlobalheaderParams>globalheaderParamsList= globalheaderParamsService.findGlobalheaderParamsWithName(headeridparams);
            for (GlobalheaderParams globalheaderParams : globalheaderParamsList) {
                globalheaderParamsHashMap.put(globalheaderParams.getKeyname(), globalheaderParams.getKeyvalue());
                TestPlanCaseController.log.info("??????Header??????globalheaderParams???"+globalheaderParams.getKeyname()+" globalheaderparamvalue:"+globalheaderParams.getKeyvalue());
            }
        }
        for (String ParamName:globalheaderParamsHashMap.keySet()) {
            String Paramvalue=globalheaderParamsHashMap.get(ParamName);
            Object Result = Paramvalue;
            if ((Paramvalue.contains("<") && Paramvalue.contains(">")) || (Paramvalue.contains("<<") && Paramvalue.contains(">>")) || (Paramvalue.contains("[") && Paramvalue.contains("]")) || (Paramvalue.contains("$") && Paramvalue.contains("$"))) {
                Result = GetVaraibaleValue(Paramvalue, InterFaceMap, DBMap,GlobalVariablesHashMap,projectid);
            }
            HeaderMap.put(ParamName, Result);
            TestPlanCaseController.log.info("??????Header?????????"+ParamName+" ??????Header???Value: "+Result);
        }

        if (HeaderMap.size() > 0) {
            jmeterPerformanceObject.setHeadjson(JSON.toJSONString(HeaderMap));
        } else {
            jmeterPerformanceObject.setHeadjson("");
        }
        if (ParamsMap.size() > 0) {
            jmeterPerformanceObject.setParamsjson(JSON.toJSONString(ParamsMap));
        } else {
            jmeterPerformanceObject.setParamsjson("");
        }
        if (BodyMap.size() > 0) {
            jmeterPerformanceObject.setBodyjson(JSON.toJSONString(BodyMap));
        } else {
            jmeterPerformanceObject.setBodyjson("");
        }

        jmeterPerformanceObject.setPostdata(PostData);

        //??????????????????json
        Condition rdcon = new Condition(Variables.class);
        rdcon.createCriteria().andCondition("projectid = "+projectid);
        List<Variables> variablesList= variablesService.listByCondition(rdcon);
        String variablesjson = "";
        if (variablesList.size() > 0) {
            variablesjson = JSON.toJSONString(variablesList);
        }
        jmeterPerformanceObject.setRadomvariablejson(variablesjson);

        //??????
        List<ApicasesAssert> apicasesAssertList = apicasesAssertService.findAssertbycaseid(dispatch.getTestcaseid().toString());
        if (apicasesAssertList.size() > 0) {
            String ExpectJson = JSON.toJSONString(apicasesAssertList);
            jmeterPerformanceObject.setExpect(ExpectJson);
        } else {
            jmeterPerformanceObject.setExpect("");
        }
        return jmeterPerformanceObject;
    }

    //?????????????????????
    private boolean GetSubOrNot(HashMap<String, String> VariablesMap, String Value, String prefix, String profix) {
        boolean flag = false;
        for (String Key : VariablesMap.keySet()) {
            String ActualValue = prefix + Key + profix;
            if (Value.contains(ActualValue)) {
                String LeftValue = Value.replace(ActualValue, "");
                if (LeftValue.length() > 0) {
                    //???????????????
                    return true;
                } else {
                    return false;
                }
            }
        }
        return flag;
    }

    private Object GetVaraibaleValue(String Value, HashMap<String, String> InterfaceMap, HashMap<String, String> DBMap,HashMap<String, String> globalvariablesMap,long projectid) throws Exception {
        Object ObjectValue = Value;
        boolean exist = false; //????????????Value??????????????????false???????????????????????????????????????

        //???????????????????????????
        for (String interfacevariablesName : InterfaceMap.keySet()) {
            boolean flag = GetSubOrNot(InterfaceMap, Value, "<", ">");
            if (Value.contains("<" + interfacevariablesName + ">")) {
                exist = true;
                String ActualValue = InterfaceMap.get(interfacevariablesName);
                if (flag) {
                    //???????????????????????????
                    Value = Value.replace("<" + interfacevariablesName + ">", ActualValue);
                    ObjectValue = Value;
                } else {
                    //?????????????????????????????????,?????????????????????????????????
                    Condition tvcon = new Condition(Testvariables.class);
                    tvcon.createCriteria().andCondition("projectid = "+projectid).andCondition("testvariablesname= '"+interfacevariablesName+"'");
                    List<Testvariables> variablesList= testvariablesService.listByCondition(tvcon);
                    Testvariables testvariables =variablesList.get(0);// testvariablesService.getBy("testvariablesname", interfacevariablesName);//  testMysqlHelp.GetVariablesDataType(interfacevariablesName);
                    if (testvariables == null) {
                        ObjectValue = "??????????????????" + Value + "?????????????????????????????????????????????-?????????????????????????????????????????????????????????";
                    } else {
                        ObjectValue = GetDataByType(ActualValue, testvariables.getValuetype());
                    }
                }
            }
        }
        //??????????????????????????????
        for (String DBvariablesName : DBMap.keySet()) {
            boolean flag = GetSubOrNot(DBMap, Value, "<<", ">>");
            if (Value.contains("<<" + DBvariablesName + ">>")) {
                exist = true;
                String ActualValue = DBMap.get(DBvariablesName);
                if (flag) {
                    //???????????????????????????
                    Value = Value.replace("<<" + DBvariablesName + ">>", ActualValue);
                    ObjectValue = Value;
                } else {
                    //?????????????????????????????????,?????????????????????????????????
                    Condition dbcon = new Condition(Dbvariables.class);
                    dbcon.createCriteria().andCondition("projectid = "+projectid).andCondition("dbvariablesname= '"+DBvariablesName+"'");
                    List<Dbvariables> variablesList= dbvariablesService.listByCondition(dbcon);
                    Dbvariables dbvariables =variablesList.get(0);// dbvariablesService.getBy("dbvariablesname", DBvariablesName);
                    if (dbvariables == null) {
                        ObjectValue = "??????????????????" + Value + " ?????????????????????-???????????????????????????????????????";
                    } else {
                        ObjectValue = GetDataByType(ActualValue, dbvariables.getValuetype());
                    }
                }
            }
        }
        //???????????????????????????
        for (String variables : globalvariablesMap.keySet()) {
            boolean flag = GetSubOrNot(globalvariablesMap, Value, "$", "$");
            if (Value.contains("$" + variables + "$")) {
                exist = true;
                if (flag) {
                    Object GlobalVariableValue = globalvariablesMap.get(variables);
                    Value = Value.replace("$" + variables + "$", GlobalVariableValue.toString());
                    ObjectValue = Value;
                } else {
                    ObjectValue = globalvariablesMap.get(variables);
                }
            }
        }
        if (!exist) {
            throw new Exception("???????????????????????????????????????" + Value + " ???????????????????????????????????????????????????????????????????????????????????????");
        }
        return ObjectValue;
    }


//    private HashMap<String, Object> GetHeaderFromTestPlanParam(HashMap<String, Object> HeaderMap, Dispatch dispatch, HashMap<String, String> InterfaceMap, HashMap<String, String> DBMap) throws Exception {
//        HashMap<String, Object> resultmap = new HashMap<>();
//        //List<ExecuteplanParams> executeplanHeaderParamList = executeplanParamsService.getParamsbyepid(dispatch.getExecplanid(), "Header");
//        Map<String,Object>params=new HashMap<>();
//        params.put("executeplanid",dispatch.getExecplanid());
//        List<Globalheaderuse> globalheaderuseList = globalheaderuseService.searchheaderbyepid(params);
//        List<GlobalheaderParams> globalheaderParamsList=new ArrayList<>();
//        HashMap<String, String> globalheaderParamsHashMap = new HashMap<>();
//
//        for (String HeaderName:HeaderMap.keySet()) {
//            globalheaderParamsHashMap.put(HeaderName,HeaderMap.get(HeaderName).toString());
//        }
//
//        //?????????????????????header???k,v,??????????????????header????????????????????????
//        for (Globalheaderuse globalheaderuse :globalheaderuseList) {
////            Condition con=new Condition(GlobalheaderParams.class);
////            con.createCriteria().andCondition("globalheaderid = " + globalheaderuse.getGlobalheaderid());
//            Map<String, Object> headeridparams=new HashMap<>();
//            params.put("globalheaderid",globalheaderuse.getGlobalheaderid());
//            globalheaderParamsList= globalheaderParamsService.findGlobalheaderParamsWithName(headeridparams);
//            for (GlobalheaderParams globalheaderParams : globalheaderParamsList) {
//                if (!globalheaderParamsHashMap.containsKey(globalheaderParams.getKeyname())) {
//                    globalheaderParamsHashMap.put(globalheaderParams.getKeyname(), globalheaderParams.getKeyvalue());
//                }
//            }
//        }
//        for (String ParamName : globalheaderParamsHashMap.keySet()) {
//            String ParamValue = globalheaderParamsHashMap.get(ParamName);
//            Object Result = ParamValue;
//            if ((ParamValue.contains("<") && ParamValue.contains(">")) || (ParamValue.contains("<<") && ParamValue.contains(">>")) || (ParamValue.contains("[") && ParamValue.contains("]"))) {
//                Result = GetVaraibaleValue(ParamValue, InterfaceMap, DBMap);
//            }
//            resultmap.put(ParamName, Result);
//        }
//        return HeaderMap;
//    }

    public JmeterPerformanceObject GetJmeterPerformance(Dispatch dispatch) throws Exception {
        Apicases apicases = apicasesService.getBy("id", dispatch.getTestcaseid());
        JmeterPerformanceObject jmeterPerformanceObject = GetJmeterPerformanceCaseData(dispatch);
        jmeterPerformanceObject.setProjectid(apicases.getProjectid());
        Api api = apiService.getBy("id", apicases.getApiid());
        jmeterPerformanceObject = GetJmeterPerformanceCaseRequestData(jmeterPerformanceObject, dispatch, api);
        return jmeterPerformanceObject;
    }

    public void JmeterClassNotExist(Dispatch dis, String jmeterclassname, String casename) {
        // ????????????????????????jmeter-class??????????????????????????????????????????????????????
        ApicasesReport ar = new ApicasesReport();
        ar.setTestplanid(dis.getExecplanid());
        ar.setCaseid(dis.getTestcaseid());
        ar.setCasename(dis.getTestcasename());
        ar.setErrorinfo("????????????-???????????????" + casename + " |????????????????????????jmeter-class??????" + jmeterclassname + " ?????????????????????????????????");
        ar.setBatchname(dis.getBatchname());
        ar.setExpect(dis.getExpect());
        ar.setStatus("??????");
        ar.setSlaverid(dis.getSlaverid());
        ar.setRuntime(new Long(0));
        Long planid = dis.getExecplanid();

        apicasesReportPerformanceMapper.addcasereport(ar);
        //epservice.updatetestplanstatus(planid, "fail");
        //PerformanceDispatchScheduleTask.log.info("????????????-????????????????????????jmeter-class???......." + jmeterclassname);
    }

    public boolean JmeterClassExist(String jmeterclassname, String JmeterPath) {
        String JmeterExtJarPath = JmeterPath.replace("bin", "lib");
        String JarPath = JmeterExtJarPath + "/ext/api-jmeter-autotest-1.0.jar";
        boolean flag = false;
        try {
            //???????????????????????????????????????????????????????????????????????????File??????
            File f = new File(JarPath);
            URL url1 = f.toURI().toURL();
            URLClassLoader myClassLoader = new URLClassLoader(new URL[]{url1}, Thread.currentThread().getContextClassLoader());

            //??????jarFile???JarEntry??????????????????
            JarFile jar = new JarFile(JarPath);
            //??????zip?????????????????????
            Enumeration<JarEntry> enumFiles = jar.entries();
            JarEntry entry;

            //??????????????????????????????????????????
            while (enumFiles.hasMoreElements()) {
                entry = (JarEntry) enumFiles.nextElement();
                if (entry.getName().indexOf("META-INF") < 0) {
                    String classFullName = entry.getName();
                    if (classFullName.indexOf(".class") > 0) {
                        //????????????.class
                        String className = classFullName.substring(0, classFullName.length() - 6).replace("/", ".");
                        if (className.equals(jmeterclassname)) {
                            flag = true;
                        }
                        //????????????
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public void FunJmeterClassNotExist(Dispatch dis, String jmeterclassname, String casename) {
        // ????????????????????????jmeter-class??????????????????????????????????????????????????????
        ApicasesReport ar = new ApicasesReport();
        ar.setTestplanid(dis.getExecplanid());
        ar.setCaseid(dis.getTestcaseid());
        ar.setCasename(dis.getTestcasename());
        ar.setErrorinfo("????????????-???????????????" + casename + " |????????????????????????jmeter-class??????" + jmeterclassname + " ?????????????????????????????????");
        ar.setBatchname(dis.getBatchname());
        ar.setExpect(dis.getExpect());
        ar.setStatus("??????");
        ar.setRuntime(new Long(0));
        Long planid = dis.getExecplanid();

        apicasereportservice.addcasereport(ar);
        epservice.updatetestplanstatus(planid, "fail");
        TestPlanCaseController.log.info("????????????-????????????????????????jmeter-class???......." + jmeterclassname);
    }

    //????????????????????????
    private Object GetDataByType(String Data, String ValueType) {
        Object Result = new Object();
        if (ValueType.equalsIgnoreCase("Number")) {
            try {
                Result = Long.parseLong(Data);
            } catch (Exception ex) {
                Result = "????????????" + Data + " ?????????????????????????????????";
            }
        }
        if (ValueType.equalsIgnoreCase("Json")) {
            try {
                Result = JSON.parse(Data);
            } catch (Exception ex) {
                Result = "????????????" + Data + " ?????????????????????????????????";
            }
        }
        if (ValueType.equalsIgnoreCase("String") || ValueType.isEmpty()) {
            Result = Data;
        }
        if (ValueType.equalsIgnoreCase("Array")) {
            String[] Array = Data.split(",");
            Result = Array;
        }
        if (ValueType.equalsIgnoreCase("Bool")) {
            try {
                Result = Boolean.parseBoolean(Data);
            } catch (Exception ex) {
                Result = "????????????" + Data + " ?????????????????????????????????";
            }
        }
        return Result;
    }


    private Object GetVariablesDataType(String Value, String PlanId, String BatchName, String Caseid) throws Exception {
        Object Result = "";
        Value = Value.substring(1);

        Testvariables testvariables = testvariablesService.getBy("testvariablesname", Value);
        if (testvariables == null) {
            throw new Exception("??????????????????" + Value + "?????????????????????????????????????????????-????????????????????????????????????");
        }
        String ValueType = testvariables.getValuetype();

        Condition con = new Condition(ApicasesVariables.class);
        con.createCriteria().andCondition("variablesname = '" + Value.replace("'", "''") + "'").andCondition(" caseid = " + Caseid);
        List<ApicasesVariables> apicasesVariablesList = apicasesVariablesService.listByCondition(con);
        if (apicasesVariablesList.size() == 0) {
            throw new Exception("??????????????????" + Value + "?????????????????????????????????????????????-?????????????????????????????????????????????????????????");
        }
        //??????????????????????????????$??????????????????????????????????????????????????????????????????
        TestvariablesValue testvariablesValue = testvariablesValueService.gettestvariablesvalue(Long.parseLong(PlanId), Long.parseLong(Caseid), Value, BatchName);
        if (testvariablesValue != null) {
            String VariablesNameValue = testvariablesValue.getVariablesvalue();
            Result = GetDataByType(VariablesNameValue, ValueType);
        } else {
            throw new Exception("??????????????????" + Value + "??????????????????????????????-???????????????????????????????????????");
        }
        return Result;
    }

    //????????????????????????
    public List<List<Dispatch>> FunctionDispatch(int Jmeternums, List<Dispatch> dispatchList) {
        if (dispatchList.size() < Jmeternums) {
            Jmeternums = dispatchList.size();
        }
        List<List<Dispatch>> LastDispatchList = new ArrayList<List<Dispatch>>();
        List<Dispatch> splitdispatchList;
        int sizemode = (dispatchList.size()) / Jmeternums;
        int sizeleft = (dispatchList.size()) % Jmeternums;
        int j = 0;
        int x = 0;
        for (int i = 0; i < Jmeternums; i++) {
            splitdispatchList = new ArrayList<Dispatch>();
            for (j = x; j < (sizemode + x); j++) {
                Dispatch dis = dispatchList.get(j);
                splitdispatchList.add(dis);
            }
            x = j;
            LastDispatchList.add(splitdispatchList);
        }
        if (sizeleft != 0) {
            for (int y = 1; y < sizeleft + 1; y++) {
                Dispatch dis = dispatchList.get(dispatchList.size() - y);
                LastDispatchList.get(LastDispatchList.size() - 1).add(dis);
            }
        }
        return LastDispatchList;
    }

    public int GetJmeterProcess(String DictionaryCode, String DicType) {
        List<Dictionary> slavermaxfunthreaddic = dictionaryMapper.findDicNameValueWithCode(DictionaryCode);

        int JmeterProcess = 1;
        //????????????????????????????????????
        if (slavermaxfunthreaddic.size() == 0) {
            TestPlanCaseController.log.info("????????????-??????????????????" + DicType + "slaver????????????jmerter????????????????????????1");
        } else {
            String slavermaxthread = slavermaxfunthreaddic.get(0).getDicitmevalue();
            try {
                JmeterProcess = Integer.valueOf(slavermaxthread);
            } catch (Exception ex) {
                TestPlanCaseController.log.error("????????????-????????????????????????" + DicType + "slaver????????????jmerter????????????????????????1");
            }
            TestPlanCaseController.log.info("????????????-???????????????slaver????????????jmerter???????????????????????????????????????" + slavermaxthread);
        }
        return JmeterProcess;
    }

    public HashMap<String, List<Dispatch>> GetProtocolDispatch(List<Dispatch> dispatchList) {
//    public  List<Dispatch> GetProtocolDispatch(List<Dispatch> dispatchList) {
        List<Dispatch> dispatchResultList = new ArrayList<>();
        HashMap<Long, List<Dispatch>> GroupDispatch = new HashMap<Long, List<Dispatch>>();

        //????????????id???????????????????????????
        for (Dispatch dispatch : dispatchList) {
            Long planid = dispatch.getExecplanid();
            if (!GroupDispatch.containsKey(planid)) {
                List<Dispatch> dispatchListtmp = new ArrayList<>();
                dispatchListtmp.add(dispatch);
                GroupDispatch.put(planid, dispatchListtmp);
            } else {
                GroupDispatch.get(planid).add(dispatch);
            }
        }
        for (Long planid : GroupDispatch.keySet()) {
            dispatchResultList = GroupDispatch.get(planid);
            break;
        }

//        //???????????????????????????
//        HashMap<String, List<Dispatch>> DeployUnitGroupDispatch = new HashMap<String, List<Dispatch>>();
//        for (Dispatch dispatch : dispatchResultList) {
//            String DeployUnit = dispatch.getDeployunitname();
//            if (!DeployUnitGroupDispatch.containsKey(DeployUnit)) {
//                List<Dispatch> dispatchListtmp = new ArrayList<>();
//                dispatchListtmp.add(dispatch);
//                DeployUnitGroupDispatch.put(DeployUnit, dispatchListtmp);
//            } else {
//                DeployUnitGroupDispatch.get(DeployUnit).add(dispatch);
//            }
//        }
//
//        //??????????????????
        HashMap<String, List<Dispatch>> ProtocolGroupDispatch = new HashMap<String, List<Dispatch>>();
//
//        for (String DeployUnit : DeployUnitGroupDispatch.keySet()) {
//            Deployunit deployunit = deployunitService.findDeployNameValueWithCode(DeployUnit);
//            String Protocal = deployunit.getProtocal();
//            if (Protocal.equalsIgnoreCase("http") || Protocal.equalsIgnoreCase("https")) {
//                ProtocolGroupDispatch = MergeCaseList(ProtocolGroupDispatch, DeployUnitGroupDispatch, DeployUnit, "http");
//            }
//            if (Protocal.equalsIgnoreCase("rpc")) {
//                ProtocolGroupDispatch = MergeCaseList(ProtocolGroupDispatch, DeployUnitGroupDispatch, DeployUnit, "rpc");
//            }
//        }
        ProtocolGroupDispatch.put("http", dispatchResultList);
        return ProtocolGroupDispatch;
    }

    public HashMap<String, List<Dispatch>> MergeCaseList(HashMap<String, List<Dispatch>> ProtocolGroupDispatch, HashMap<String, List<Dispatch>> DeployUnitGroupDispatch, String DeployUnit, String Protocol) {
        HashMap<String, List<Dispatch>> ProtocolGroupResultDispatch = ProtocolGroupDispatch;
        if (!ProtocolGroupResultDispatch.containsKey(Protocol)) {
            List<Dispatch> dispatchListtmp = new ArrayList<>();
            for (Dispatch dis : DeployUnitGroupDispatch.get(DeployUnit)) {
                dispatchListtmp.add(dis);
            }
            ProtocolGroupResultDispatch.put(Protocol, dispatchListtmp);
        } else {
            for (Dispatch dis : DeployUnitGroupDispatch.get(DeployUnit)) {
                ProtocolGroupResultDispatch.get(Protocol).add(dis);
            }
        }
        return ProtocolGroupResultDispatch;
    }


}

