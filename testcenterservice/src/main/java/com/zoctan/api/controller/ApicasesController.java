package com.zoctan.api.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zoctan.api.core.response.Result;
import com.zoctan.api.core.response.ResultGenerator;
import com.zoctan.api.core.service.*;
import com.zoctan.api.dto.*;
import com.zoctan.api.entity.*;
import com.zoctan.api.service.*;
import com.zoctan.api.util.RadomVariables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.entity.Condition;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author Zoctan
 * @date 2020/09/11
 */
@Slf4j
@RestController
@RequestMapping("/apicases")
public class ApicasesController {
    @Resource
    private ApicasesService apicasesService;
    @Autowired
    private ApiCasedataService apiCasedataService;
    @Autowired
    private EnviromentService enviromentService;
    @Autowired
    private ApicasesAssertService apicasesAssertService;
    @Autowired(required = false)
    private ApiService apiService;
    @Autowired(required = false)
    private DeployunitService deployunitService;
    @Autowired(required = false)
    private MacdepunitService macdepunitService;
    @Autowired(required = false)
    private MachineService machineService;
    @Autowired(required = false)
    private ApiParamsService apiParamsService;
    @Autowired(required = false)
    private TestconditionService testconditionService;
    @Autowired(required = false)
    private ConditionApiService conditionApiService;
    @Autowired(required = false)
    private ConditionDbService conditionDbService;
    @Autowired(required = false)
    private ConditionScriptService conditionScriptService;

    @Autowired(required = false)
    private ConditionDelayService conditionDelayService;
    @Autowired(required = false)
    private ExecuteplanTestcaseService executeplanTestcaseService;
    @Resource
    private ConditionOrderService conditionOrderService;
    @Value("${spring.conditionserver.serverurl}")
    private String conditionserver;
    @Autowired(required = false)
    private VariablesService variablesService;
    @Autowired(required = false)
    private TestvariablesService testvariablesService;

    @Autowired(required = false)
    private DbvariablesService dbvariablesService;

    @Autowired(required = false)
    private ApicasesDebugConditionService apicasesDebugConditionService;

    @Autowired(required = false)
    private GlobalheaderParamsService globalheaderParamsService;


    @Autowired(required = false)
    private GlobalvariablesService globalvariablesService;

    @PostMapping
    public Result add(@RequestBody Apicases apicases) {
        Condition con = new Condition(Apicases.class);
        con.createCriteria().andCondition("projectid = " + apicases.getProjectid())
                .andCondition("deployunitname = '" + apicases.getDeployunitname() + "'")
                .andCondition("apiname = '" + apicases.getApiname() + "'").andCondition("casename = '" + apicases.getCasename().replace("'", "''") + "'");
        if (apicasesService.ifexist(con) > 0) {
            return ResultGenerator.genFailedResult("??????????????????");
        } else {
            apicasesService.save(apicases);
            //????????????????????????
            Long apiid = apicases.getApiid();
            Api api = apiService.getById(apiid);
            String RequestContentType = api.getRequestcontenttype();
            Map<String, Object> params = new HashMap<>();
            params.put("apiid", apiid);
            List<ApiParams> apiParamsList = apiParamsService.getApiParamsbyapiid(params);
            List<ApiCasedata> apiCasedataList = new ArrayList<>();
            for (ApiParams apiParams : apiParamsList) {
                if (apiParams.getPropertytype().equalsIgnoreCase("Header") || apiParams.getPropertytype().equalsIgnoreCase("Params")) {
                    ApiCasedata apiCasedata = GetApiCaseData(apicases, apiParams);
                    apiCasedataList.add(apiCasedata);
                } else {
                    if (RequestContentType.equalsIgnoreCase("Form??????")) {
                        ApiCasedata apiCasedata = GetApiCaseData(apicases, apiParams);
                        apiCasedataList.add(apiCasedata);
                    } else {
                        if (apiParams.getKeytype().equalsIgnoreCase(RequestContentType)) {
                            ApiCasedata apiCasedata = GetApiCaseData(apicases, apiParams);
                            apiCasedataList.add(apiCasedata);
                        }
                    }
                }
            }
            if (apiCasedataList.size() > 0) {
                apiCasedataService.save(apiCasedataList);
            }
            long casecount=api.getCasecounts();
            api.setCasecounts(casecount+1);
            apiService.updateApi(api);
            return ResultGenerator.genOkResult();
        }
    }

    private ApiCasedata GetApiCaseData(Apicases apicases, ApiParams apiParams) {
        ApiCasedata apiCasedata = new ApiCasedata();
        apiCasedata.setCaseid(apicases.getId());
        apiCasedata.setCasename(apicases.getCasename());
        apiCasedata.setPropertytype(apiParams.getPropertytype());
        if (apiParams.getKeydefaultvalue().equalsIgnoreCase("NoForm")) {
            apiCasedata.setApiparam("Body");
            apiCasedata.setApiparamvalue(apiParams.getKeyname());
        } else {
            apiCasedata.setApiparam(apiParams.getKeyname());
            apiCasedata.setApiparamvalue(apiParams.getKeydefaultvalue());
        }
        apiCasedata.setParamstype(apiParams.getKeytype());
        apiCasedata.setMemo("");
        apiCasedata.setCreateTime(new Date());
        apiCasedata.setLastmodifyTime(new Date());
        return apiCasedata;
    }

    @PostMapping("/copycases")
    public Result copycases(@RequestBody final Map<String, Object> param) {
        String sourcecaseid = param.get("sourcecaseid").toString();
        String sourcedeployunitid = param.get("sourcedeployunitid").toString();
        String sourcedeployunitname = param.get("sourcedeployunitname").toString();
        String newcasename = param.get("newcasename").toString();

        Condition con = new Condition(Apicases.class);
        con.createCriteria().andCondition("deployunitid = " + sourcedeployunitid)
                .andCondition("casename = '" + newcasename + "'");
        if (apicasesService.ifexist(con) > 0) {
            return ResultGenerator.genFailedResult(sourcedeployunitname + "???????????????????????????");
        } else {
            Apicases Sourcecase = apicasesService.getBy("id", Long.parseLong(sourcecaseid));
            if (Sourcecase != null) {
                long Apiid = Sourcecase.getApiid();
                Condition apcasedatacon = new Condition(Apicases.class);
                apcasedatacon.createCriteria().andCondition("caseid = " + Long.parseLong(sourcecaseid));
                List<ApiCasedata> SourceApicasedataList = apiCasedataService.listByCondition(apcasedatacon);
                //????????????
                Sourcecase.setDeployunitid(Long.parseLong(sourcedeployunitid));
                Sourcecase.setDeployunitname(sourcedeployunitname);
                Sourcecase.setCasename(newcasename);
                Sourcecase.setCreateTime(new Date());
                Sourcecase.setLastmodifyTime(new Date());
                Sourcecase.setId(null);
                apicasesService.save(Sourcecase);
                Long NewCaseId = Sourcecase.getId();
                //??????????????????
                for (ApiCasedata apiCasedata : SourceApicasedataList) {
                    apiCasedata.setCaseid(NewCaseId);
                    apiCasedata.setId(null);
                    apiCasedata.setCasename(newcasename);
                    apiCasedata.setCreateTime(new Date());
                    apiCasedata.setLastmodifyTime(new Date());
                    apiCasedataService.save(apiCasedata);
                }
                //????????????
                Condition AssertDataCondition = new Condition(ApicasesAssert.class);
                AssertDataCondition.createCriteria().andCondition("caseid = " + Long.parseLong(sourcecaseid));
                List<ApicasesAssert> SourceAssertdataList = apicasesAssertService.listByCondition(AssertDataCondition);
                for (ApicasesAssert apicasesAssert : SourceAssertdataList) {
                    apicasesAssert.setCaseid(NewCaseId);
                    apicasesAssert.setId(null);
                    apicasesAssertService.save(apicasesAssert);
                }

                //??????????????????
                Condition ParentCondition = new Condition(Testcondition.class);
                ParentCondition.createCriteria().andCondition("objectid = " + sourcecaseid)
                        .andCondition("objecttype='" + "????????????'");
                List<Testcondition> testconditionList = testconditionService.listByCondition(ParentCondition);
                for (Testcondition SourceParentCondition : testconditionList) {
                    long SourceConditionID = SourceParentCondition.getId();
                    String DestinationConditionName = SourceParentCondition.getConditionname() + "-????????????";
                    SourceParentCondition.setObjectid(NewCaseId);
                    SourceParentCondition.setConditionname(DestinationConditionName);
                    SourceParentCondition.setObjectname(newcasename);
                    SourceParentCondition.setApiid(Apiid);
                    SourceParentCondition.setDeployunitid(Long.parseLong(sourcedeployunitid));
                    SourceParentCondition.setDeployunitname(sourcedeployunitname);
                    SourceParentCondition.setId(null);
                    testconditionService.save(SourceParentCondition);
                    long DestinationConditionID = SourceParentCondition.getId();
                    SubCondition(SourceConditionID, DestinationConditionID, DestinationConditionName, "case");
                }
                //????????????????????????
//                ApicasesDebugCondition apicasesDebugCondition = apicasesDebugConditionService.getBy("caseid", Long.parseLong(sourcecaseid));
//                if (apicasesDebugCondition != null) {
//                    apicasesDebugCondition.setId(null);
//                    apicasesDebugCondition.setCaseid(NewCaseId);
//                    apicasesDebugCondition.setCasename(newcasename);
//                    apicasesDebugConditionService.save(apicasesDebugCondition);
//                }
                //api????????????1
                Api api= apiService.getById(Apiid);
                long casecount=api.getCasecounts();
                api.setCasecounts(casecount+1);
                apiService.updateApi(api);
            }
            return ResultGenerator.genOkResult();
        }
    }


    //??????????????????????????????
    @PostMapping("/copydeployunitcases")
    public Result copydeployunitcases(@RequestBody final Map<String, Object> param) {
        Long sourcedeployunitid = Long.parseLong(param.get("sourcedeployunitid").toString());
        String sourcedeployunitname = param.get("sourcedeployunitname").toString();
        Long destinationdeployunitid = Long.parseLong(param.get("destinationdeployunitid").toString());
        String destinationdeployunitname = param.get("destinationdeployunitname").toString();

        if (sourcedeployunitid.equals(destinationdeployunitid)) {
            return ResultGenerator.genFailedResult("??????????????????????????????????????????????????????????????????????????????????????????");
        } else {
            //?????????????????????
            long DebugDesConditionID = 0;
            String DestiConditionName = "";
            Condition apicasedebugcon = new Condition(ApicasesDebugCondition.class);
            apicasedebugcon.createCriteria().andCondition("deployunitid = " + sourcedeployunitid);
            List<ApicasesDebugCondition> apicasesDebugConditionList = apicasesDebugConditionService.listByCondition(apicasedebugcon);
            if (apicasesDebugConditionList.size() > 0) {
                long SourceConditionID = apicasesDebugConditionList.get(0).getConditionid();
                Testcondition testcondition = testconditionService.getBy("id", SourceConditionID);
                if (testcondition != null) {
                    Testcondition NewCondition = testcondition;
                    DestiConditionName = testcondition.getConditionname() + "-???????????????";
                    NewCondition.setConditionname(DestiConditionName);
                    NewCondition.setId(null);
                    testconditionService.save(NewCondition);
                    DebugDesConditionID = NewCondition.getId();
                    SubCondition(SourceConditionID, DebugDesConditionID, DestiConditionName, "deployunit");
                }
            }

            Condition apicon = new Condition(Api.class);
            apicon.createCriteria().andCondition("deployunitid = " + sourcedeployunitid);
            if (apiService.ifexist(apicon) == 0) {
                return ResultGenerator.genFailedResult(sourcedeployunitname + "???????????????API??????????????????????????????????????????API");
            } else {
                List<Api> SourceapiList = apiService.listByCondition(apicon);
                for (Api SourceApi : SourceapiList) {
                    long SourceApiid = SourceApi.getId();
                    //1.??????api
                    Api DestinationApi = SourceApi;
                    DestinationApi.setDeployunitname(destinationdeployunitname);
                    DestinationApi.setDeployunitid(destinationdeployunitid);
                    DestinationApi.setId(null);
                    DestinationApi.setCasecounts(new Long(1));
                    apiService.save(DestinationApi);
                    long DestinationApiid = DestinationApi.getId();

                    //2.??????api??????
                    Condition apiparamcon = new Condition(ApiParams.class);
                    apiparamcon.createCriteria().andCondition("apiid = " + SourceApiid);
                    List<ApiParams> apiParamsList = apiParamsService.listByCondition(apiparamcon);
                    for (ApiParams SourceParam : apiParamsList) {
                        ApiParams DestinationParam = SourceParam;
                        DestinationParam.setApiid(DestinationApiid);
                        DestinationParam.setDeployunitname(destinationdeployunitname);
                        DestinationParam.setDeployunitid(destinationdeployunitid);
                        DestinationParam.setId(null);
                        apiParamsService.save(DestinationParam);
                    }

                    //3.??????api??????
                    Condition apicasecon = new Condition(Apicases.class);
                    apicasecon.createCriteria().andCondition("apiid = " + SourceApiid);
                    List<Apicases> apicasesList = apicasesService.listByCondition(apicasecon);
                    for (Apicases SourceCase : apicasesList) {
                        long SourceCaseID = SourceCase.getId();
                        Apicases DesitionApicase = SourceCase;
                        DesitionApicase.setDeployunitname(destinationdeployunitname);
                        DesitionApicase.setDeployunitid(destinationdeployunitid);
                        DesitionApicase.setId(null);
                        apicasesService.save(DesitionApicase);
                        long DestinationCaseID = DesitionApicase.getId();

                        //????????????????????????
//                        Condition casedebugcon = new Condition(ApicasesDebugCondition.class);
//                        casedebugcon.createCriteria().andCondition("caseid = " + SourceCaseID);
//                        ApicasesDebugCondition apicasesDebugCondition = apicasesDebugConditionService.getBy("caseid", SourceCaseID);
//                        if (apicasesDebugCondition != null) {
//                            ApicasesDebugCondition DestiapicasesDebugCondition = apicasesDebugCondition;
//                            DestiapicasesDebugCondition.setId(null);
//                            DestiapicasesDebugCondition.setCaseid(DestinationCaseID);
//                            DestiapicasesDebugCondition.setDeployunitid(destinationdeployunitid);
//                            DestiapicasesDebugCondition.setDeployunitname(destinationdeployunitname);
//                            DestiapicasesDebugCondition.setConditionid(DebugDesConditionID);
//                            DestiapicasesDebugCondition.setConditionname(DestiConditionName);
//                            apicasesDebugConditionService.save(DestiapicasesDebugCondition);
//                        }


                        //4.??????????????????
                        Condition apicasedatacon = new Condition(ApiCasedata.class);
                        apicasedatacon.createCriteria().andCondition("caseid = " + SourceCaseID);
                        List<ApiCasedata> apiCasedataList = apiCasedataService.listByCondition(apicasedatacon);
                        for (ApiCasedata SourceCaseData : apiCasedataList) {
                            ApiCasedata DestinationData = SourceCaseData;
                            DestinationData.setCaseid(DestinationCaseID);
                            DestinationData.setId(null);
                            apiCasedataService.save(DestinationData);
                        }

                        //5.??????????????????
                        Condition CaseAssertCondition = new Condition(ApicasesAssert.class);
                        CaseAssertCondition.createCriteria().andCondition("caseid = " + SourceCaseID);
                        List<ApicasesAssert> SourceAssertdataList = apicasesAssertService.listByCondition(CaseAssertCondition);
                        for (ApicasesAssert apicasesAssert : SourceAssertdataList) {
                            apicasesAssert.setCaseid(DestinationCaseID);
                            apicasesAssert.setId(null);
                            apicasesAssertService.save(apicasesAssert);
                        }

                        //6.?????????????????????
                        Condition ParentSubCondition = new Condition(Testcondition.class);
                        ParentSubCondition.createCriteria().andCondition("objectid = " + SourceCaseID)
                                .andCondition("objecttype='" + "????????????'");
                        List<Testcondition> testconditionList = testconditionService.listByCondition(ParentSubCondition);
                        for (Testcondition SourceParentCondition : testconditionList) {
                            long SourceConditionID = SourceParentCondition.getId();
                            String DestinationConditionName = SourceParentCondition.getConditionname() + "-???????????????";
                            SourceParentCondition.setObjectid(DestinationCaseID);
                            SourceParentCondition.setConditionname(DestinationConditionName);
                            SourceParentCondition.setApiid(DestinationApiid);
                            SourceParentCondition.setDeployunitid(destinationdeployunitid);
                            SourceParentCondition.setDeployunitname(destinationdeployunitname);
                            SourceParentCondition.setId(null);
                            testconditionService.save(SourceParentCondition);
                            long DestinationConditionID = SourceParentCondition.getId();
                            SubCondition(SourceConditionID, DestinationConditionID, DestinationConditionName, "deployunit");
                        }
                    }
                }
                return ResultGenerator.genOkResult();
            }
        }
    }

    private void SubCondition(long SourceConditionID, long DestinationConditionID, String DestinationConditionName, String CopyType) {
        long subconditionapiid = 0;
        long subconditiondbid = 0;
        long subconditionscriptid = 0;
        long subconditiondelayid = 0;

        //???????????????????????????
        Condition APISubCondition = new Condition(ConditionApi.class);
        APISubCondition.createCriteria().andCondition("conditionid = " + SourceConditionID);
        List<ConditionApi> conditionApiList = conditionApiService.listByCondition(APISubCondition);
        for (ConditionApi SourceConditionApi : conditionApiList) {
            SourceConditionApi.setId(null);
            SourceConditionApi.setSubconditionname(SourceConditionApi.getSubconditionname() + "-??????");
            SourceConditionApi.setConditionname(DestinationConditionName);
            SourceConditionApi.setConditionid(DestinationConditionID);
            conditionApiService.save(SourceConditionApi);
            subconditionapiid = SourceConditionApi.getId();
        }

        //??????????????????????????????
        Condition DBSubCondition = new Condition(ConditionDb.class);
        DBSubCondition.createCriteria().andCondition("conditionid = " + SourceConditionID);
        List<ConditionDb> conditionDbList = conditionDbService.listByCondition(DBSubCondition);
        for (ConditionDb SourceConditionDB : conditionDbList) {
            SourceConditionDB.setId(null);
            SourceConditionDB.setConditionid(DestinationConditionID);
            SourceConditionDB.setConditionname(DestinationConditionName);
            SourceConditionDB.setSubconditionname(SourceConditionDB.getSubconditionname() + "-??????");
            conditionDbService.save(SourceConditionDB);
            subconditiondbid = SourceConditionDB.getId();
        }

        //???????????????????????????
        Condition ScriptSubCondition = new Condition(ConditionScript.class);
        ScriptSubCondition.createCriteria().andCondition("conditionid = " + SourceConditionID);
        List<ConditionScript> conditionScriptList = conditionScriptService.listByCondition(ScriptSubCondition);
        for (ConditionScript SourceConditionScript : conditionScriptList) {
            SourceConditionScript.setId(null);
            SourceConditionScript.setConditionid(DestinationConditionID);
            SourceConditionScript.setConditionname(DestinationConditionName);
            SourceConditionScript.setSubconditionname(SourceConditionScript.getSubconditionname() + "-??????");
            conditionScriptService.save(SourceConditionScript);
            subconditionscriptid = SourceConditionScript.getId();
        }

        //???????????????????????????
        Condition DelaySubCondition = new Condition(ConditionDelay.class);
        DelaySubCondition.createCriteria().andCondition("conditionid = " + SourceConditionID);
        List<ConditionDelay> conditionDelayList = conditionDelayService.listByCondition(DelaySubCondition);
        for (ConditionDelay SourceConditionDelay : conditionDelayList) {
            SourceConditionDelay.setId(null);
            SourceConditionDelay.setConditionid(DestinationConditionID);
            SourceConditionDelay.setConditionname(DestinationConditionName);
            SourceConditionDelay.setSubconditionname(SourceConditionDelay.getSubconditionname() + "-??????");
            conditionDelayService.save(SourceConditionDelay);
            subconditiondelayid = SourceConditionDelay.getId();
        }

        Condition OrderCondition = new Condition(ConditionOrder.class);
        OrderCondition.createCriteria().andCondition("conditionid = " + SourceConditionID);
        List<ConditionOrder> conditionOrderList = conditionOrderService.listByCondition(OrderCondition);
        for (ConditionOrder SourceConditionOrder : conditionOrderList) {
            SourceConditionOrder.setId(null);
            SourceConditionOrder.setConditionname(DestinationConditionName);
            SourceConditionOrder.setConditionid(DestinationConditionID);
            if (SourceConditionOrder.getSubconditiontype().equalsIgnoreCase("??????")) {
                SourceConditionOrder.setSubconditionid(subconditionapiid);
            }
            if (SourceConditionOrder.getSubconditiontype().equalsIgnoreCase("?????????")) {
                SourceConditionOrder.setSubconditionid(subconditiondbid);
            }
            if (SourceConditionOrder.getSubconditiontype().equalsIgnoreCase("??????")) {
                SourceConditionOrder.setSubconditionid(subconditionscriptid);
            }
            if (SourceConditionOrder.getSubconditiontype().equalsIgnoreCase("??????")) {
                SourceConditionOrder.setSubconditionid(subconditiondelayid);
            }
            if (CopyType.equalsIgnoreCase("case")) {
                SourceConditionOrder.setSubconditionname(SourceConditionOrder.getSubconditionname() + "-????????????");
            } else {
                SourceConditionOrder.setSubconditionname(SourceConditionOrder.getSubconditionname() + "-???????????????");
            }
            conditionOrderService.save(SourceConditionOrder);
        }
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        Apicases apicases=apicasesService.getById(id);
        apicasesService.deleteById(id);
        //?????????????????????
        apiCasedataService.deletcasedatabyid(id);
        //??????????????????
        Condition caseassertcon = new Condition(ApicasesAssert.class);
        caseassertcon.createCriteria().andCondition("caseid = " + id);
        apicasesAssertService.deleteByCondition(caseassertcon);
        //??????????????????????????????
        Condition con = new Condition(Testcondition.class);
        con.createCriteria().andCondition("objecttype = '????????????'").andCondition("objectid = " + id).andCondition("conditiontype = '" + "????????????'");
        List<Testcondition> testconditionList = testconditionService.listByCondition(con);
        if (testconditionList.size() > 0) {
            Long ConditionID = testconditionList.get(0).getId();
            conditionApiService.deleteBy("conditionid", ConditionID);
            conditionDbService.deleteBy("conditionid", ConditionID);
            conditionScriptService.deleteBy("conditionid", ConditionID);
            conditionDelayService.deleteBy("conditionid", ConditionID);
            conditionOrderService.deleteBy("conditionid", ConditionID);
            testconditionService.deleteByCondition(con);
        }
        //??????????????????????????????
        executeplanTestcaseService.removetestcase(id);
        //??????????????????????????????
        long ConditionID = 0;
        ApicasesDebugCondition apicasesDebugCondition = apicasesDebugConditionService.getBy("caseid", id);
        if (apicasesDebugCondition != null) {
            ConditionID = apicasesDebugCondition.getConditionid();
        }
        apicasesDebugConditionService.deleteBy("caseid", id);
        Condition ApicasesDebugCondition = new Condition(ApicasesDebugCondition.class);
        ApicasesDebugCondition.createCriteria().andCondition("conditionid = " + ConditionID);
        List<ApicasesDebugCondition> apicasesDebugConditionList = apicasesDebugConditionService.listByCondition(ApicasesDebugCondition);
        if (apicasesDebugConditionList.size() == 0) {
            testconditionService.deleteBy("id", ConditionID);
        }

        Api api= apiService.getById(apicases.getApiid());
        long casecount=api.getCasecounts();
        api.setCasecounts(casecount-1);
        apiService.updateApi(api);
        return ResultGenerator.genOkResult();
    }


    @PostMapping("/removebatchapicase")
    public Result removebatchapicase(@RequestBody List<Apicases> apicasesList) {
        for (Apicases apicases : apicasesList) {
            long id = apicases.getId();
            apicasesService.deleteById(id);
            //?????????????????????
            apiCasedataService.deletcasedatabyid(id);
            //??????????????????
            Condition caseassertcon = new Condition(ApicasesAssert.class);
            caseassertcon.createCriteria().andCondition("caseid = " + id);
            apicasesAssertService.deleteByCondition(caseassertcon);
            //??????????????????????????????
            Condition con = new Condition(Testcondition.class);
            con.createCriteria().andCondition("objecttype = '????????????'").andCondition("objectid = " + id).andCondition("conditiontype = '" + "????????????'");
            List<Testcondition> testconditionList = testconditionService.listByCondition(con);
            if (testconditionList.size() > 0) {
                Long ConditionID = testconditionList.get(0).getId();
                conditionApiService.deleteBy("conditionid", ConditionID);
                conditionDbService.deleteBy("conditionid", ConditionID);
                conditionScriptService.deleteBy("conditionid", ConditionID);
                conditionDelayService.deleteBy("conditionid", ConditionID);
                conditionOrderService.deleteBy("conditionid", ConditionID);
                testconditionService.deleteByCondition(con);
            }
            //??????????????????????????????
            executeplanTestcaseService.removetestcase(id);
            //??????????????????????????????
            long ConditionID = 0;
            ApicasesDebugCondition apicasesDebugCondition = apicasesDebugConditionService.getBy("caseid", id);
            if (apicasesDebugCondition != null) {
                ConditionID = apicasesDebugCondition.getConditionid();
            }
            apicasesDebugConditionService.deleteBy("caseid", id);
            Condition ApicasesDebugCondition = new Condition(ApicasesDebugCondition.class);
            ApicasesDebugCondition.createCriteria().andCondition("conditionid = " + ConditionID);
            List<ApicasesDebugCondition> apicasesDebugConditionList = apicasesDebugConditionService.listByCondition(ApicasesDebugCondition);
            if (apicasesDebugConditionList.size() == 0) {
                testconditionService.deleteBy("id", ConditionID);
            }
            Api api= apiService.getById(apicases.getApiid());
            long casecount=api.getCasecounts();
            api.setCasecounts(casecount-1);
            apiService.updateApi(api);
        }
        return ResultGenerator.genOkResult();
    }


    @PatchMapping
    public Result update(@RequestBody Apicases apicases) {
        apicasesService.update(apicases);
        return ResultGenerator.genOkResult();
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        Apicases apicases = apicasesService.getById(id);
        return ResultGenerator.genOkResult(apicases);
    }

    @GetMapping("/getcasenum")
    public Result getcasenum(@RequestParam String casetype, @RequestParam long projectid) {
        Integer apicasesnum = apicasesService.getcasenum(casetype, projectid);
        return ResultGenerator.genOkResult(apicasesnum);
    }

    @GetMapping("/getperformancecasenum")
    public Result getperformancecasenum(@RequestParam String casetype, @RequestParam long projectid) {
        Integer apicasesnum = apicasesService.getcasenum(casetype, projectid);
        return ResultGenerator.genOkResult(apicasesnum);
    }

    @GetMapping
    public Result list(@RequestParam(defaultValue = "0") Integer page,
                       @RequestParam(defaultValue = "0") Integer size) {
        PageHelper.startPage(page, size);
        List<Apicases> list = apicasesService.listAll();
        PageInfo<Apicases> pageInfo = PageInfo.of(list);
        return ResultGenerator.genOkResult(pageInfo);
    }

    @GetMapping("/getstaticsdeployunitcases")
    public Result getstaticsdeployunitcases(@RequestParam long projectid) {
        List<Apicases> list = apicasesService.getstaticsdeployunitcases(projectid);
        List<StaticsDataForPie> result = new ArrayList<>();
        for (Apicases ac : list) {
            StaticsDataForPie staticsDataForPie = new StaticsDataForPie();
            staticsDataForPie.setValue(ac.getApiid());
            staticsDataForPie.setName(ac.getDeployunitname());
            result.add(staticsDataForPie);
        }
        return ResultGenerator.genOkResult(result);
    }

    /**
     * ?????????????????????
     */
    @PutMapping("/detail")
    public Result updateDeploy(@RequestBody final Apicases apicases) {
        if (apicasesService.forupdateifexist(apicases).size() > 0) {
            return ResultGenerator.genFailedResult("??????????????????");
        } else {
            this.apicasesService.updateApicase(apicases);
            //?????????????????????????????????????????????????????????
            testconditionService.updatecasename(apicases.getId(), "????????????", apicases.getCasename());
            conditionApiService.updatecasename(apicases.getId(), apicases.getCasename());
            return ResultGenerator.genOkResult();
        }
    }

    /**
     * ???????????????
     */
    @PostMapping("/search")
    public Result search(@RequestBody final Map<String, Object> param) {
        Integer page = Integer.parseInt(param.get("page").toString());
        Integer size = Integer.parseInt(param.get("size").toString());
        PageHelper.startPage(page, size);
        final List<Apicases> list = this.apicasesService.findApiCaseWithName(param);
        final PageInfo<Apicases> pageInfo = new PageInfo<>(list);
        return ResultGenerator.genOkResult(pageInfo);
    }

    /**
     * ???????????????
     */
    @PostMapping("/searchleftcase")
    public Result searchleftcase(@RequestBody final Map<String, Object> param) {
        Integer page = Integer.parseInt(param.get("page").toString());
        Integer size = Integer.parseInt(param.get("size").toString());
        PageHelper.startPage(page, size);
        final List<Apicases> list = this.apicasesService.findApiCaseleft(param);
        final PageInfo<Apicases> pageInfo = new PageInfo<>(list);
        return ResultGenerator.genOkResult(pageInfo);
    }

    /**
     * ???????????????id????????????
     */
    @PostMapping("/getcasebydeployunitid")
    public Result getcasebydeployunitid(@RequestBody final Map<String, Object> param) {
        String deployunitid = param.get("sourcedeployunitid").toString();
        final List<Apicases> list = this.apicasesService.getcasebydeployunitid(Long.parseLong(deployunitid));
        return ResultGenerator.genOkResult(list);
    }

    /**
     * ???????????????
     */
    @PostMapping("/searchbyname")
    public Result searchbyname(@RequestBody final Map<String, Object> param) {
        long deployunitid = Long.parseLong(param.get("deployunitid").toString());
        long apiid = Long.parseLong(param.get("apiid").toString());
        final List<Apicases> list = this.apicasesService.getapicasebyName(deployunitid, apiid);
        return ResultGenerator.genOkResult(list);
    }

    private String getSubConditionRespone(String Url, String params, HttpHeader header) throws Exception {
        //??????API??????
        TestHttp testHttp = new TestHttp();
        header.addParam("Content-Type", "application/json;charset=utf-8");
        TestResponeData testResponeData = testHttp.doService("http", "", Url, header, new HttpParamers(), params, "POST", "", 30000);
        String Respone = testResponeData.getResponeContent();
        if (Respone.contains("??????????????????")) {
            JSONObject object = JSON.parseObject(Respone);
            throw new Exception(object.getString("message"));
        }
        return Respone;
    }


    private ConditionResult FixCondition(List<Testcondition> testconditionList, Map<String, Object> param, long Caseid, String objecttype) throws Exception {
        //List<Testcondition> testconditionList = testconditionService.GetConditionByPlanIDAndConditionType(Caseid, "????????????", objecttype);
        ConditionResult conditionResult = new ConditionResult();
        String APIRespone = "";
        String DBRespone = "";
        conditionResult.setAPIRespone(APIRespone);
        conditionResult.setDBRespone(DBRespone);
        if (testconditionList.size() > 0) {
            String ScriptConditionServerurl = conditionserver + "/testcondition/execcasecondition/script";
            String DBConditionServerurl = conditionserver + "/testcondition/execcasecondition/db";
            String APIConditionServerurl = conditionserver + "/testcondition/execcasecondition/api";

            Long ConditionID = testconditionList.get(0).getId();
            Map<String, Object> conditionmap = new HashMap<>();
            conditionmap.put("conditionid", ConditionID);
            List<ConditionOrder> conditionOrderList = conditionOrderService.findconditionorderWithid(conditionmap);
            param.put("ConditionID", ConditionID);

            HttpHeader header = new HttpHeader();
            try {
                if (conditionOrderList.size() > 0) {
                    for (ConditionOrder conditionOrder : conditionOrderList) {
                        param.put("dbvariablesvalue", DBRespone);
                        String params = JSON.toJSONString(param);
                        if (conditionOrder.getSubconditiontype().equals("??????")) {
                            ApicasesController.log.info("????????????????????????????????????????????????????????????" + params);
                            APIRespone = getSubConditionRespone(APIConditionServerurl, params, header);
                        }
                        if (conditionOrder.getSubconditiontype().equals("?????????")) {
                            DBRespone = getSubConditionRespone(DBConditionServerurl, params, header);
                            param.put("dbvariablesvalue", DBRespone);
                        }
                        if (conditionOrder.getSubconditiontype().equals("??????")) {
                            getSubConditionRespone(ScriptConditionServerurl, params, header);
                        }
                    }
                } else {
                    String params = JSON.toJSONString(param);
                    Condition dbcon = new Condition(ConditionDb.class);
                    dbcon.createCriteria().andCondition("conditionid=" + ConditionID);
                    List<ConditionDb> conditionDbList = conditionDbService.listByCondition(dbcon);
                    if (conditionDbList.size() > 0) {
                        ApicasesController.log.info("????????????????????????????????????????????????????????????????????????" + params);
                        DBRespone = getSubConditionRespone(DBConditionServerurl, params, header);
                    }
                    param.put("dbvariablesvalue", DBRespone);
                    ApicasesController.log.info("??????????????????????????????????????????????????????????????????" + DBRespone);
                    Condition apicon = new Condition(ConditionApi.class);
                    apicon.createCriteria().andCondition("conditionid=" + ConditionID);
                    List<ConditionApi> conditionApiList = conditionApiService.listByCondition(apicon);
                    if (conditionApiList.size() > 0) {
                        params = JSON.toJSONString(param);
                        ApicasesController.log.info("?????????????????????????????????????????????????????????????????????" + params);
                        APIRespone = getSubConditionRespone(APIConditionServerurl, params, header);
                    }

                    Condition scriptcon = new Condition(ConditionScript.class);
                    scriptcon.createCriteria().andCondition("conditionid=" + ConditionID);
                    List<ConditionScript> conditionScriptList = conditionScriptService.listByCondition(scriptcon);

                    if (conditionScriptList.size() > 0) {
                        ApicasesController.log.info("?????????????????????????????????????????????????????????????????????" + params);
                        getSubConditionRespone(ScriptConditionServerurl, params, header);
                    }
                }
                conditionResult.setAPIRespone(APIRespone);
                conditionResult.setDBRespone(DBRespone);
            } catch (Exception ex) {
                if (ex.getMessage().contains("Connection refused")) {
                    throw new Exception("???????????????????????????????????????ConditionService?????????????????????");
                } else {
                    throw new Exception(ex.getMessage());
                }
            }
        }
        return conditionResult;
    }

    private HashMap<String, String> GetResponeMap(String Respone, HashMap<String, String> ResponeMap) throws Exception {
        if (!Respone.isEmpty()) {
            try {
                JSONObject jsonObject = JSON.parseObject(Respone);
                for (Map.Entry<String, Object> objectEntry : jsonObject.getJSONObject("data").entrySet()) {
                    String key = objectEntry.getKey();
                    String value = objectEntry.getValue().toString();
                    ResponeMap.put(key, value);
                }
            } catch (Exception ex) {
                throw new Exception("?????????????????????????????????" + Respone);
            }
        }
        return ResponeMap;
    }

    /**
     * ????????????
     */
    @PostMapping("/runtest")
    public Result runtest(@RequestBody final Map<String, Object> param) {
        String enviromentid = param.get("enviromentid").toString();
        Long Caseid = Long.parseLong(param.get("caseid").toString());
        Long conditionid = Long.parseLong(param.get("conditionid").toString());
        Long globalheaderid = Long.parseLong(param.get("globalheaderid").toString());
        Long projectid = Long.parseLong(param.get("projectid").toString());
        boolean prixflag = Boolean.parseBoolean(param.get("prixflag").toString());
        HashMap<String, String> ParamsValuesMap = new HashMap<>();
        HashMap<String, String> DBParamsValuesMap = new HashMap<>();

        String APIRespone = "";
        String DBRespone = "";
        if (conditionid != 0) {
            //????????????????????????????????????
            ConditionResult conditionResult = new ConditionResult();
            try {
                Condition con = new Condition(Testcondition.class);
                con.createCriteria().andCondition("id = " + conditionid);
                List<Testcondition> testconditionList = testconditionService.listByCondition(con);
                param.put("apivariablesvalues",APIRespone);
                conditionResult = FixCondition(testconditionList, param, Caseid, "????????????");
                APIRespone = conditionResult.getAPIRespone();
                ApicasesController.log.info("????????????????????????????????????????????????????????????????????????" + APIRespone);
                ParamsValuesMap = GetResponeMap(APIRespone, ParamsValuesMap);
                DBRespone = conditionResult.getDBRespone();
                ApicasesController.log.info("????????????????????????????????????????????????????????????????????????" + DBRespone);
                DBParamsValuesMap = GetResponeMap(DBRespone, DBParamsValuesMap);
            } catch (Exception exception) {
                return ResultGenerator.genFailedResult(exception.getMessage());
            }
        }

        //??????????????????
        ConditionResult CaseconditionResult = new ConditionResult();
        String CaseAPIRespone = "";
        String CaseDBRespone = "";
        try {
            List<Testcondition> testconditionList = testconditionService.GetConditionByPlanIDAndConditionType(Caseid, "????????????", "????????????");
            param.put("apivariablesvalues",APIRespone);
            CaseconditionResult = FixCondition(testconditionList, param, Caseid, "????????????");
            CaseAPIRespone = CaseconditionResult.getAPIRespone();
            ApicasesController.log.info("??????????????????????????????????????????????????????????????????" + CaseAPIRespone);
            ParamsValuesMap = GetResponeMap(CaseAPIRespone, ParamsValuesMap);
            CaseDBRespone = CaseconditionResult.getDBRespone();
            ApicasesController.log.info("?????????????????????????????????????????????????????????????????????" + CaseDBRespone);
            DBParamsValuesMap = GetResponeMap(CaseDBRespone, DBParamsValuesMap);
        } catch (Exception exception) {
            return ResultGenerator.genFailedResult(exception.getMessage());
        }

        Apicases apicases = apicasesService.getBy("id", Caseid);
        if (apicases == null) {
            return ResultGenerator.genFailedResult("???????????????????????????????????????????????????");
        }
        Long Apiid = apicases.getApiid();
        Api api = apiService.getBy("id", Apiid);
        if (api == null) {
            return ResultGenerator.genFailedResult("???????????????API???????????????????????????????????????");
        }
        String Method = api.getVisittype();
        String ApiStyle = api.getApistyle();
        Deployunit deployunit = deployunitService.getBy("id", api.getDeployunitid());
        if (deployunit == null) {
            return ResultGenerator.genFailedResult("???????????????API?????????????????????????????????????????????????????????");
        }
        String Protocal = deployunit.getProtocal();
        String BaseUrl = deployunit.getBaseurl();
        Macdepunit macdepunit = macdepunitService.getmacdepbyenvidanddepid(Long.parseLong(enviromentid), deployunit.getId());
        if (macdepunit != null) {
            String testserver = "";
            String resource = "";
            if (macdepunit.getVisittype().equals("ip")) {
                Long MachineID = macdepunit.getMachineid();
                Machine machine = machineService.getBy("id", MachineID);
                if (machine == null) {
                    return ResultGenerator.genFailedResult("??????????????????????????????????????????????????????????????????");
                }
                Enviroment enviroment = enviromentService.getBy("id", Long.parseLong(enviromentid));
                if (enviroment == null) {
                    return ResultGenerator.genFailedResult("??????????????????????????????????????????????????????????????????");
                }
                testserver = machine.getIp();
                if (BaseUrl == null || BaseUrl.isEmpty()) {
                    resource = deployunit.getProtocal() + "://" + testserver + ":" + deployunit.getPort() + api.getPath();
                } else {
                    if (BaseUrl.startsWith("/")) {
                        resource = deployunit.getProtocal() + "://" + testserver + ":" + deployunit.getPort() + BaseUrl + api.getPath();
                    } else {
                        resource = deployunit.getProtocal() + "://" + testserver + ":" + deployunit.getPort() + "/" + BaseUrl + api.getPath();
                    }
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
            }

            Condition rdcon = new Condition(Variables.class);
            rdcon.createCriteria().andCondition("projectid = " + projectid);
            List<Variables> variablesList = variablesService.listByCondition(rdcon);
            HashMap<String, String> RadomHashMap = new HashMap<>();
            for (Variables va : variablesList) {
                RadomHashMap.put(va.getVariablesname(), va.getVariablestype());
            }
            ApicasesController.log.info("????????????????????????????????????resource Url???" + resource);

            //url??????????????????
            //1.??????????????????
            for (Variables variables : variablesList) {
                String VariableName = "[" + variables.getVariablesname() + "]";
                if (resource.contains(VariableName)) {
                    Object VariableValue = GetRadomValue(variables.getVariablesname());
                    resource = resource.replace(VariableName, VariableValue.toString());
                }
            }
            //2.??????????????????
            for (String Interfacevariables : ParamsValuesMap.keySet()) {
                String UseInterfacevariables = "<" + Interfacevariables + ">";
                if (resource.contains(UseInterfacevariables)) {
                    Object VariableValue = ParamsValuesMap.get(Interfacevariables);// GetVariablesObjectValues("$" +Interfacevariables, ParamsValuesMap);
                    resource = resource.replace(UseInterfacevariables, VariableValue.toString());
                }
            }

            //3.?????????????????????
            for (String DBvariables : DBParamsValuesMap.keySet()) {
                String UseDBvariables = "<<" + DBvariables + ">>";
                if (resource.contains(UseDBvariables)) {
                    Object VariableValue = DBParamsValuesMap.get(DBvariables);
                    resource = resource.replace(UseDBvariables, VariableValue.toString());
                }
            }

            //4.??????????????????
            Condition gvcon = new Condition(Globalvariables.class);
            gvcon.createCriteria().andCondition("projectid = " + projectid);
            List<Globalvariables> globalvariablesList = globalvariablesService.listByCondition(gvcon);

            HashMap<String, String> GlobalVariablesHashMap = new HashMap<>();
            for (Globalvariables va : globalvariablesList) {
                GlobalVariablesHashMap.put(va.getKeyname(), va.getKeyvalue());
            }
            for (Globalvariables variables : globalvariablesList) {
                String VariableName = "$" + variables.getKeyname() + "$";
                if (resource.contains(VariableName)) {
                    Object VariableValue = variables.getKeyvalue();
                    resource = resource.replace(VariableName, VariableValue.toString());
                }
            }
            ApicasesController.log.info("????????????????????????????????????resource Url???" + resource);

            List<ApiCasedata> HeaderApiCasedataList = apiCasedataService.getparamvaluebycaseidandtype(Caseid, "Header");
            List<ApiCasedata> ParamsApiCasedataList = apiCasedataService.getparamvaluebycaseidandtype(Caseid, "Params");
            List<ApiCasedata> BodyApiCasedataList = apiCasedataService.getparamvaluebycaseidandtype(Caseid, "Body");

            //????????????Header
            Condition con = new Condition(GlobalheaderParams.class);
            con.createCriteria().andCondition("globalheaderid = " + globalheaderid);
            List<GlobalheaderParams> globalheaderParamsList = globalheaderParamsService.listByCondition(con);
            String requestcontenttype = api.getRequestcontenttype();

            //Header?????????
            HttpHeader header = new HttpHeader();
            try {
                header = GetHttpHeader(globalheaderParamsList, HeaderApiCasedataList, ParamsValuesMap, RadomHashMap, DBParamsValuesMap, GlobalVariablesHashMap,projectid);
            } catch (Exception exception) {
                return ResultGenerator.genFailedResult(exception.getMessage());
            }
            header = AddHeaderByRequestContentType(header, requestcontenttype);

            //???????????????
            HttpParamers paramers = new HttpParamers();
            try {
                paramers = GetHttpParamers(ParamsApiCasedataList, ParamsValuesMap, RadomHashMap, DBParamsValuesMap, GlobalVariablesHashMap,projectid);
            } catch (Exception exception) {
                return ResultGenerator.genFailedResult(exception.getMessage());
            }

            //Body?????????
            String PostData = "";
            HttpParamers Bodyparamers = new HttpParamers();
            if (requestcontenttype.equalsIgnoreCase("Form??????")) {
                try {
                    Bodyparamers = GetHttpParamers(BodyApiCasedataList, ParamsValuesMap, RadomHashMap, DBParamsValuesMap, GlobalVariablesHashMap,projectid);
                } catch (Exception exception) {
                    return ResultGenerator.genFailedResult(exception.getMessage());
                }
                if (Bodyparamers.getParams().size() > 0) {
                    PostData = Bodyparamers.getQueryString();
                }
            } else {
                for (ApiCasedata Paramdata : BodyApiCasedataList) {
                    //Body????????????????????????
                    PostData = Paramdata.getApiparamvalue();
                    //1.??????????????????
                    for (Variables variables : variablesList) {
                        String VariableName = "[" + variables.getVariablesname() + "]";
                        if (PostData.contains(VariableName)) {
                            Object VariableValue = GetRadomValue(variables.getVariablesname());
                            PostData = PostData.replace(VariableName, VariableValue.toString());
                        }
                    }
                    //2.??????????????????
                    for (String Interfacevariables : ParamsValuesMap.keySet()) {
                        String UseInterfacevariables = "<" + Interfacevariables + ">";
                        if (PostData.contains(UseInterfacevariables)) {
                            Object VariableValue = ParamsValuesMap.get(Interfacevariables);// GetVariablesObjectValues("$" +Interfacevariables, ParamsValuesMap);
                            PostData = PostData.replace(UseInterfacevariables, VariableValue.toString());
                        }
                    }
                    //3.?????????????????????
                    for (String DBvariables : DBParamsValuesMap.keySet()) {
                        String UseDBvariables = "<<" + DBvariables + ">>";
                        if (PostData.contains(UseDBvariables)) {
                            Object VariableValue = DBParamsValuesMap.get(DBvariables);
                            PostData = PostData.replace(UseDBvariables, VariableValue.toString());
                        }
                    }
                    //4.??????????????????
                    for (Globalvariables variables : globalvariablesList) {
                        String VariableName = "$" + variables.getKeyname() + "$";
                        if (PostData.contains(VariableName)) {
                            Object VariableValue = GlobalVariablesHashMap.get(variables.getKeyname());
                            PostData = PostData.replace(VariableName, VariableValue.toString());
                        }
                    }
                }
            }
            try {
                long Start = new Date().getTime();
                TestHttp testHttp = new TestHttp();
                String VisitType = api.getVisittype();
                TestResponeData respon = testHttp.doService(Protocal, ApiStyle, resource, header, paramers, PostData, VisitType, requestcontenttype, 2000);
                long End = new Date().getTime();
                long CostTime = End - Start;
                respon.setResponeTime(CostTime);
                ResponeGeneral responeGeneral = new ResponeGeneral();
                responeGeneral.setApistyle(ApiStyle);
                responeGeneral.setPostData(PostData);
                responeGeneral.setMethod(Method);
                responeGeneral.setProtocal(Protocal);
                responeGeneral.setUrl(respon.getRequestUrl());
                List<RequestHead> requestHeadList = new ArrayList<>();
                for (String Key : header.getParams().keySet()) {
                    RequestHead requestHead = new RequestHead();
                    requestHead.setKeyName(Key);
                    requestHead.setKeyValue(header.getParams().get(Key).toString());
                    requestHeadList.add(requestHead);
                }
                List<RequestParams> requestParamsList = new ArrayList<>();
                for (String Key : paramers.getParams().keySet()) {
                    RequestParams requestParams = new RequestParams();
                    requestParams.setKeyName(Key);
                    requestParams.setKeyValue(paramers.getParams().get(Key).toString());
                    requestParamsList.add(requestParams);
                }
                respon.setRequestHeadList(requestHeadList);
                respon.setRequestParamsList(requestParamsList);
                respon.setResponeGeneral(responeGeneral);
                return ResultGenerator.genOkResult(respon);

            } catch (Exception exception) {
                String ExceptionMess = exception.getMessage();
                if (ExceptionMess.contains("Illegal character in path at")) {
                    ExceptionMess = "Url????????????????????????????????????????????????????????????????????????????????????" + exception.getMessage();
                }
                return ResultGenerator.genFailedResult(ExceptionMess);
            }
        } else {
            return ResultGenerator.genFailedResult("??????????????????????????????API??????????????????????????????????????????????????????");
        }
    }


    //??????HttpHeader
    private HttpHeader GetHttpHeader(List<GlobalheaderParams> globalheaderParamsList, List<ApiCasedata> HeaderApiCasedataList, HashMap<String, String> ParamsValuesMap, HashMap<String, String> RadomMap, HashMap<String, String> DBMap, HashMap<String, String> GlobalVariablesHashMap,long projectid) throws Exception {
        HashMap<String, String> globalheaderParamsHashMap = new HashMap<>();
        for (ApiCasedata Headdata : HeaderApiCasedataList) {
            if (!globalheaderParamsHashMap.containsKey(Headdata.getApiparam())) {
                globalheaderParamsHashMap.put(Headdata.getApiparam(), Headdata.getApiparamvalue());
            }
        }
        //??????Header??????????????????????????????????????????
        for (GlobalheaderParams ghp : globalheaderParamsList) {
            globalheaderParamsHashMap.put(ghp.getKeyname(), ghp.getKeyvalue());
        }
        HttpHeader header = new HttpHeader();
        for (String HeaderName : globalheaderParamsHashMap.keySet()) {
            String HeaderValue = globalheaderParamsHashMap.get(HeaderName);
            Object Result = HeaderValue;
            if ((HeaderValue.contains("<") && HeaderValue.contains(">")) || (HeaderValue.contains("<<") && HeaderValue.contains(">>")) || (HeaderValue.contains("[") && HeaderValue.contains("]")) || (HeaderValue.contains("$") && HeaderValue.contains("$"))) {
                try {
                    Result = GetVaraibaleValue(HeaderValue, RadomMap, ParamsValuesMap, DBMap, GlobalVariablesHashMap,projectid);
                } catch (Exception ex) {
                    throw new Exception("???????????????Header???????????????" + HeaderName + "-?????????????????????" + ex.getMessage());
                }
            }
            header.addParam(HeaderName, Result);
        }
        return header;
    }

    //??????HttpParams
    private HttpParamers GetHttpParamers(List<ApiCasedata> ParamsApiCasedataList, HashMap<String, String> ParamsValuesMap, HashMap<String, String> RadomMap, HashMap<String, String> DBMap, HashMap<String, String> GlobalVariablesHashMap,long projectid) throws Exception {
        HttpParamers paramers = new HttpParamers();
        for (ApiCasedata Paramdata : ParamsApiCasedataList) {
            String ParamName = Paramdata.getApiparam();
            String ParamValue = Paramdata.getApiparamvalue();
            String DataType = Paramdata.getParamstype();
            Object ObjectResult = ParamValue;
            if ((ParamValue.contains("<") && ParamValue.contains(">")) || (ParamValue.contains("<<") && ParamValue.contains(">>")) || (ParamValue.contains("[") && ParamValue.contains("]")) || (ParamValue.contains("$") && ParamValue.contains("$"))) {
                try {
                    ObjectResult = GetVaraibaleValue(ParamValue, RadomMap, ParamsValuesMap, DBMap, GlobalVariablesHashMap,projectid);
                } catch (Exception ex) {
                    throw new Exception("???????????????Params??????Body???????????????" + ParamName + "-?????????????????????" + ex.getMessage());
                }
            }
            Object Result = GetDataByType(ObjectResult.toString(), DataType);
            paramers.addParam(ParamName, Result);
        }
        return paramers;
    }

    private Object GetVaraibaleValue(String Value, HashMap<String, String> RadomMap, HashMap<String, String> InterfaceMap, HashMap<String, String> DBMap, HashMap<String, String> globalvariablesMap,long projectid) throws Exception {
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
                    Dbvariables dbvariables =variablesList.get(0);// dbvariablesService.getBy("dbvariablesname", DBvariablesName);//  testMysqlHelp.GetVariablesDataType(interfacevariablesName);
                    if (dbvariables == null) {
                        ObjectValue = "??????????????????" + Value + "?????????????????????????????????????????????-?????????????????????????????????????????????????????????";
                    } else {
                        ObjectValue = GetDataByType(ActualValue, dbvariables.getValuetype());
                    }
                }
            }
        }
        //???????????????????????????
        for (String variables : RadomMap.keySet()) {
            boolean flag = GetSubOrNot(RadomMap, Value, "[", "]");
            if (Value.contains("[" + variables + "]")) {
                exist = true;
                if (flag) {
                    Object RadomValue = GetRadomValue(variables);
                    Value = Value.replace("[" + variables + "]", RadomValue.toString());
                    ObjectValue = Value;
                } else {
                    ObjectValue = GetRadomValue(variables);
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
            throw new Exception(Value + " ????????????????????????????????????1.???????????????????????????2.??????????????????????????????????????????????????????????????????");
        }
        return ObjectValue;
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

    //?????????????????????
    private Object GetRadomValue(String Value) {
        Object Result = Value;
        String FunctionName = Value;
        List<Variables> variablesList = variablesService.listAll();
        for (Variables variables : variablesList) {
            if (variables.getVariablesname().equalsIgnoreCase(FunctionName)) {
                String Params = variables.getVariablecondition();
                String Variablestype = variables.getVariablestype();
                RadomVariables radomVariables = new RadomVariables();
                if (Variablestype.equalsIgnoreCase("???????????????")) {
                    try {
                        Integer length = Integer.parseInt(Params);
                        Result = radomVariables.GetRadmomStr(length);
                    } catch (Exception ex) {
                        Result = "????????????GetRadmomStr???????????????????????????????????????????????????????????????????????????";
                    }
                }
                if (Variablestype.equalsIgnoreCase("????????????")) {
                    String ParamsArray[] = Params.split(",");
                    if (ParamsArray.length < 2) {
                        Result = "????????????GetRadmomStr?????????????????????????????????????????????????????????";
                    } else {
                        try {
                            Long Start = Long.parseLong(ParamsArray[0]);
                            Long End = Long.parseLong(ParamsArray[1]);
                            Result = radomVariables.GetRadmomNum(Start, End);
                        } catch (Exception exception) {
                            Result = "????????????GetRadmomNum???????????????????????????????????????????????????????????????";
                        }
                    }
                }
                if (Variablestype.equalsIgnoreCase("????????????")) {
                    String ParamsArray[] = Params.split(",");
                    if (ParamsArray.length < 2) {
                        Result = "????????????GetRadmomStr?????????????????????????????????????????????????????????";
                    } else {
                        try {
                            Long Start = Long.parseLong(ParamsArray[0]);
                            Long End = Long.parseLong(ParamsArray[1]);
                            Result = radomVariables.GetRadmomDouble(Start, End);
                        } catch (Exception exception) {
                            Result = "????????????GetRadmomNum???????????????????????????????????????????????????????????????";
                        }
                    }
                }
                if (Variablestype.equalsIgnoreCase("Guid")) {
                    Result = radomVariables.GetGuid();
                }
                if (Variablestype.equalsIgnoreCase("??????IP")) {
                    Result = radomVariables.GetRadmonIP();
                }
                if (Variablestype.equalsIgnoreCase("????????????")) {
                    Result = radomVariables.GetCurrentTime();
                }
                if (Variablestype.equalsIgnoreCase("????????????")) {
                    Result = radomVariables.GetCurrentDate(Params);
                }
                if (Variablestype.equalsIgnoreCase("???????????????")) {
                    Result = radomVariables.GetCurrentTimeMillis();
                }
            }
        }
        return Result;
    }

    //????????????????????????
    private Object GetDataByType(String Data, String ValueType) throws Exception {
        Object Result = new Object();
        if (ValueType.equalsIgnoreCase("Number")) {
            try {
                Result = Long.parseLong(Data);
            } catch (Exception ex) {
                Result = "?????????  " + Data + " ????????????Number???????????????????????????";
                throw new Exception(Result.toString());
            }
        }
        if (ValueType.equalsIgnoreCase("Json")) {
            try {
                Result = JSON.parse(Data);
            } catch (Exception ex) {
                Result = "?????????  " + Data + " ??????Json???????????????????????????";
                throw new Exception(Result.toString());
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
                Result = "?????????  " + Data + " ????????????Bool???????????????????????????";
                throw new Exception(Result.toString());
            }
        }
        return Result;
    }

    //??????????????????????????????header
    private HttpHeader AddHeaderByRequestContentType(HttpHeader httpHeader, String RequestContentType) {
        if (RequestContentType.equalsIgnoreCase("json")) {
            httpHeader.addParam("Content-Type", "application/json;charset=utf-8");
        }
        if (RequestContentType.equalsIgnoreCase("xml")) {
            httpHeader.addParam("Content-Type", "application/xml;charset=utf-8");
        }
        if (RequestContentType.equalsIgnoreCase("Form??????")) {
            httpHeader.addParam("Content-Type", "application/x-www-form-urlencoded");
        }
        return httpHeader;
    }
}
