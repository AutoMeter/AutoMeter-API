package com.zoctan.api.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zoctan.api.core.response.Result;
import com.zoctan.api.core.response.ResultGenerator;
import com.zoctan.api.dto.FunctionCaseSandF;
import com.zoctan.api.dto.PerformanceCaseStatis;
import com.zoctan.api.dto.PerformanceSlaverStatics;
import com.zoctan.api.entity.*;
import com.zoctan.api.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.entity.Condition;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author SeasonFan
 * @date 2020/12/14
 */
@Slf4j
@RestController
@RequestMapping("/apicases/report/performance")
public class ApicasesReportPerformanceController {
    @Resource
    private ApicasesReportPerformanceService apicasesReportPerformanceService;

    @Resource
    private RouteperformancereportService routeperformancereportService;
    @Resource
    private ExecuteplanTestcaseService executeplanTestcaseService;

    @Resource
    private ApicasesService apicasesService;
    @Resource
    private ApicasesPerformancestatisticsService apicasesPerformancestatisticsService;

    @Resource
    private PerformancereportsourceService performancereportsourceService;
    @Resource
    private DispatchService dispatchService;


    @PostMapping
    public Result add(@RequestBody ApicasesReportPerformance apicasesReportPerformance) {
        apicasesReportPerformanceService.save(apicasesReportPerformance);
        return ResultGenerator.genOkResult();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        apicasesReportPerformanceService.deleteById(id);
        return ResultGenerator.genOkResult();
    }

    @PatchMapping
    public Result update(@RequestBody ApicasesReportPerformance apicasesReportPerformance) {
        apicasesReportPerformanceService.update(apicasesReportPerformance);
        return ResultGenerator.genOkResult();
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        ApicasesReportPerformance apicasesReportPerformance = apicasesReportPerformanceService.getById(id);
        return ResultGenerator.genOkResult(apicasesReportPerformance);
    }

    @GetMapping
    public Result list(@RequestParam(defaultValue = "0") Integer page,
                       @RequestParam(defaultValue = "0") Integer size) {
        PageHelper.startPage(page, size);
        List<ApicasesReportPerformance> list = apicasesReportPerformanceService.listallresult();
        PageInfo<ApicasesReportPerformance> pageInfo = PageInfo.of(list);
        return ResultGenerator.genOkResult(pageInfo);
    }

    /**
     * ???????????????
     */
    @PostMapping("/search")
    public Result search(@RequestBody final Map<String, Object> param) {
        Integer page= Integer.parseInt(param.get("page").toString());
        Integer size= Integer.parseInt(param.get("size").toString());
        long planid=Long.parseLong(param.get("executeplanid").toString());
        long projectid=Long.parseLong(param.get("projectid").toString());
        String batchname=param.get("batchname").toString();

        Routeperformancereport routeperformancereport= routeperformancereportService.getBy("executeplanid",planid);
        List<ApicasesReportPerformance> list=new ArrayList<>();
        if(routeperformancereport!=null)
        {
            String TableName=routeperformancereport.getTablename();
            PageHelper.startPage(page, size);
            list = this.apicasesReportPerformanceService.finddynamicresult(planid,batchname,TableName,projectid);
        }
        final PageInfo<ApicasesReportPerformance> pageInfo = new PageInfo<>(list);
        return ResultGenerator.genOkResult(pageInfo);
    }


    /**
     * ????????????tps??????
     */
    @PostMapping("/getperformancecasestatics")
    public Result getperformancecasestatics(@RequestBody final Map<String, Object> param) {
        Integer page= Integer.parseInt(param.get("page").toString());
        Integer size= Integer.parseInt(param.get("size").toString());
        long planid=Long.parseLong(param.get("executeplanid").toString());
        long projectid=Long.parseLong(param.get("projectid").toString());
        String batchname=param.get("batchname").toString();
        PageHelper.startPage(page, size);
        List<ApicasesPerformancestatistics>apicasesPerformancestatisticsList = apicasesPerformancestatisticsService.getresultbyidandname(planid,batchname,projectid);
        final PageInfo<ApicasesPerformancestatistics> pageInfo = new PageInfo<>(apicasesPerformancestatisticsList);
        return ResultGenerator.genOkResult(pageInfo);
    }


    /**
     * ?????????????????????
     */
    @PostMapping("/getperformanceallstatics")
    public Result getperformanceallstatics(@RequestBody final Map<String, Object> param) {
        long planid=Long.parseLong(param.get("executeplanid").toString());
        Long batchid = Long.parseLong(param.get("batchid").toString());

        List<PerformanceCaseStatis> performanceCaseStatisList=new ArrayList<>();
        PerformanceCaseStatis performanceCaseStatis=new PerformanceCaseStatis();

        Condition dispatchccon = new Condition(Dispatch.class);
        dispatchccon.createCriteria().andCondition("execplanid = " + planid)
                .andCondition("batchid = " + batchid);
        List<Dispatch> dispatchList = dispatchService.listByCondition(dispatchccon);

//        List<ExecuteplanTestcase> executeplanTestcaseList = executeplanTestcaseService.getplancasesbyplanid(planid);
        performanceCaseStatis.setCaseNum(dispatchList.size());

        long threadnums=0;
        long loopnums=0;
        long totalrunnuns=0;
        for (Dispatch dispatch:dispatchList) {
            long caseid=dispatch.getTestcaseid();
            totalrunnuns=totalrunnuns+dispatch.getLoops()*dispatch.getThreadnum();
            Apicases apicases= apicasesService.getBy("id",caseid);
            if(apicases!=null)
            {
                threadnums=threadnums+apicases.getThreadnum();
                loopnums=loopnums+apicases.getLoops();
            }
        }
        ApicasesReportPerformanceController.log.info("??????????????????"+totalrunnuns);
        performanceCaseStatis.setThreadnums(threadnums);
        performanceCaseStatis.setLoops(loopnums);
        performanceCaseStatis.setSlavernums(dispatchList.size());

        Condition prscon=new Condition(Performancereportsource.class);
        prscon.createCriteria().andCondition("planid = " + planid ).andCondition("batchid = " + batchid);
        List<Performancereportsource>performancereportsourceList= performancereportsourceService.listByCondition(prscon);
        long totalpassnums=0;
        double costtime=0;
        for (Performancereportsource per:performancereportsourceList) {
            totalpassnums=totalpassnums+per.getTotalcasepassnums();
            ApicasesReportPerformanceController.log.info("???????????????"+totalpassnums+"-"+per.getTotalcasepassnums());
            costtime=costtime+per.getRuntime();
        }
        ApicasesReportPerformanceController.log.info("??????????????????"+totalpassnums);
        long totalfailnums=totalrunnuns-totalpassnums;
        ApicasesReportPerformanceController.log.info("??????????????????"+totalfailnums);
        performanceCaseStatis.setExecCaseNums(totalrunnuns);
        performanceCaseStatis.setSuccessCaseNums(totalpassnums);
        performanceCaseStatis.setFailCaseNums(totalfailnums);
        DecimalFormat decimalFormat=new DecimalFormat("#.00");
        performanceCaseStatis.setCosttime(decimalFormat.format(costtime));

        performanceCaseStatisList.add(performanceCaseStatis);
        return ResultGenerator.genOkResult(performanceCaseStatisList);
    }

    /**
     * ???????????????????????????
     */
    @PostMapping("/getperformanceslaverstatics")
    public Result getperformanceslaverstatics(@RequestBody final Map<String, Object> param) {
        long planid=Long.parseLong(param.get("executeplanid").toString());
        Long batchid = Long.parseLong(param.get("batchid").toString());

        List<PerformanceSlaverStatics> performanceSlaverStaticsList=new ArrayList<>();
        Condition con=new Condition(Dispatch.class);
        con.createCriteria().andCondition("execplanid = " + planid ).andCondition("batchid = " + batchid);
        List<Dispatch>dispatchList= dispatchService.listByCondition(con);

        for (Dispatch dis:dispatchList) {
            PerformanceSlaverStatics performanceSlaverStatics=new PerformanceSlaverStatics();
            performanceSlaverStatics.setSlaverName(dis.getSlavername());
            performanceSlaverStatics.setThreadnums(dis.getThreadnum());
            performanceSlaverStatics.setLoops(dis.getLoops());
            long slaverid=dis.getSlaverid();
            long caseid=dis.getTestcaseid();
            Condition perfcon=new Condition(Performancereportsource.class);
            perfcon.createCriteria().andCondition("planid = " + planid ).andCondition("batchid = " + batchid )
            .andCondition("slaverid = " + slaverid ).andCondition("caseid = " + caseid );
            List<Performancereportsource>performancereportsourceList= performancereportsourceService.listByCondition(perfcon);
            ApicasesReportPerformanceController.log.info("?????????????????????"+"-"+performancereportsourceList.size());
            if(performancereportsourceList.size()>0)
            {
                Performancereportsource performancereportsource= performancereportsourceList.get(0);
                ApicasesReportPerformanceController.log.info("????????????????????????"+"-"+performancereportsource.getTotalcasenums());
                performanceSlaverStatics.setCaseNum(performancereportsource.getTotalcasenums());
                performanceSlaverStatics.setSuccessCaseNums(performancereportsource.getTotalcasepassnums());
                ApicasesReportPerformanceController.log.info("??????????????????????????????"+"-"+performancereportsource.getTotalcasepassnums());
                performanceSlaverStatics.setFailCaseNums(performancereportsource.getTotalcasefailnums());
                ApicasesReportPerformanceController.log.info("??????????????????????????????"+"-"+performancereportsource.getTotalcasefailnums());
                performanceSlaverStatics.setCosttime(performancereportsource.getRuntime());
            }
            else
            {
                performanceSlaverStatics.setFailCaseNums(dis.getLoops()*dis.getThreadnum());
            }
            performanceSlaverStaticsList.add(performanceSlaverStatics);
        }
        return ResultGenerator.genOkResult(performanceSlaverStaticsList);
    }

    /**
     * ????????????????????????
     */
    @PostMapping("/getperformanceCaseSandF")
    public Result getperformanceCaseSandF(@RequestBody final Map<String, Object> param) {
        if (param.get("batchid").toString().isEmpty() || param.get("executeplanid").toString().isEmpty()) {
            return ResultGenerator.genFailedResult("????????????????????????????????????");
        } else {
            Long executeplanid = Long.parseLong(param.get("executeplanid").toString());
            Long batchid = Long.parseLong(param.get("batchid").toString());


            Condition discon=new Condition(Dispatch.class);
            discon.createCriteria().andCondition("execplanid = " + executeplanid ).andCondition("batchid = " + batchid);
            List<Dispatch>dispatchList= dispatchService.listByCondition(discon);

            long totalrunnums=0;
            for (Dispatch dis:dispatchList) {
                totalrunnums=totalrunnums+dis.getThreadnum()*dis.getLoops();
            }

            List<FunctionCaseSandF> functionCaseSandFList = new ArrayList<>();
            Condition con=new Condition(Performancereportsource.class);
            con.createCriteria().andCondition("planid = " + executeplanid ).andCondition("batchid = " + batchid );
            List<Performancereportsource>performancereportsourceList= performancereportsourceService.listByCondition(con);
            long totalsuccess=0;
            for (Performancereportsource per:performancereportsourceList) {
                ApicasesReportPerformanceController.log.info("???????????????????????????"+"-"+per.getTotalcasepassnums());
                totalsuccess=totalsuccess+per.getTotalcasepassnums();
                ApicasesReportPerformanceController.log.info("??????????????????????????????"+"-"+totalsuccess);
            }
            long totalfail=totalrunnums-totalsuccess;
            ApicasesReportPerformanceController.log.info("??????????????????????????????"+"-"+totalfail);
            FunctionCaseSandF functionCaseSandF = new FunctionCaseSandF();
            functionCaseSandF.setName("?????????");
            functionCaseSandF.setValue(totalsuccess);
            functionCaseSandFList.add(functionCaseSandF);

            FunctionCaseSandF functionCaseSandFail = new FunctionCaseSandF();
            functionCaseSandFail.setName("?????????");
            functionCaseSandFail.setValue(totalfail);
            functionCaseSandFList.add(functionCaseSandFail);
            return ResultGenerator.genOkResult(functionCaseSandFList);
        }
    }


    @PostMapping("/findApicasereportWithNameandStatus")
    public Result findApicasereportWithNameandStatus(@RequestBody final Map<String, Object> param) {
        Integer page = Integer.parseInt(param.get("page").toString());
        Integer size = Integer.parseInt(param.get("size").toString());
        Long executeplanid = Long.parseLong(param.get("executeplanid").toString());
        String status = param.get("caseststus").toString();
        String batchname = param.get("batchname").toString();

        Routeperformancereport routeperformancereport= routeperformancereportService.getBy("executeplanid",executeplanid);
        List<ApicasesReportPerformance> list=new ArrayList<>();
        if(routeperformancereport!=null)
        {
            String TableName=routeperformancereport.getTablename();
            PageHelper.startPage(page, size);
            list = this.apicasesReportPerformanceService.finddynamicresultbystatus(executeplanid,batchname,TableName,status);
        }
        final PageInfo<ApicasesReportPerformance> pageInfo = new PageInfo<>(list);
        return ResultGenerator.genOkResult(pageInfo);
    }
}
