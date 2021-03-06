package com.yzx.web.controller.admin;

import com.yzx.model.Account;
import com.yzx.model.BlackList;
import com.yzx.model.BookOrder;
import com.yzx.model.admin.Log;
import com.yzx.service.AccountService;
import com.yzx.service.BlackListService;
import com.yzx.service.BookOrderService;
import com.yzx.service.admin.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("admin/blackList")
public class BlackListController {

    @Autowired
    private BlackListService blackListService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private BookOrderService bookOrderService;
    @Autowired
    private LogService logService;

    @RequestMapping(value = "list",method = RequestMethod.GET)
    public String list(){
        return "admin/blackList/list";
    }

    @RequestMapping("add")
    @ResponseBody
    public Map<String,String> add(int accountId) throws ParseException {
        Map<String,String> ret=new HashMap<>();
        Account account=accountService.findAccountById(accountId);
        List<BookOrder> bookOrders=bookOrderService.findBookOrdersByAccountId(accountId);
        if(blackListService.findBlackListByAccountId(accountId)!=null){
            ret.put("type","error");
            ret.put("msg","添加失败 该用户已经在黑名单");
        }else if(bookOrders!=null && bookOrders.size()>0){
            ret.put("type","error");
            ret.put("msg","添加失败 该用户还有未完成的定单");
            logService.addLog(Log.ACCOUNT,"黑名单","手机号"+account.getPhoneNum()+"被永久加入黑名单失败，该用户还有未完成的定单");
        }else{
            blackListService.doInBreakListBySumBreakTimes(accountId);
            account.setStatus(0);
            accountService.eidtAccount(account);
            logService.addLog(Log.ACCOUNT,"黑名单","手机号"+account.getPhoneNum()+"被永久加入黑名单");
            ret.put("type","success");
        }
        return ret;
    }

    @RequestMapping("findBlackListByAccountId")
    @ResponseBody
    public BlackList findBlackListByAccountId(int accountId) {
        return blackListService.findBlackListByAccountId(accountId);
    }

    @RequestMapping("delete")
    @ResponseBody
    public Map<String,String> delete(int [] accountId) throws ParseException {
        Map<String,String> ret=new HashMap<>();
        for(int i=0;i<accountId.length;i++){
            BlackList blackList=blackListService.findBlackListByAccountId(accountId[i]);
            SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd");
            Date not=dateFormat.parse("1970-01-01");
            if(blackList.getOutTime().compareTo(not)==0){
                ret.put("type", "error");
                ret.put("msg", "删除停止 存在用户永久冻结");
                logService.addLog(Log.ACCOUNT,"黑名单","手机号"+accountService.findAccountById(blackList.getAccountId()).getPhoneNum()+"被移出黑名单失败，已永久加黑");
                return ret;
            }
            if(blackListService.deleteBlackList(blackList.getId())<=0) {
                ret.put("type", "error");
                ret.put("msg", "删除出错 请联系管理员");
                logService.addLog(Log.SYSTEM,"删除失败","删除手机号"+accountService.findAccountById(blackList.getAccountId()).getPhoneNum()+"时，操作个数小于1");
                return ret;
            }
            Account account=accountService.findAccountById(accountId[i]);
            account.setMonthBreakTimes(0);
            account.setStatus(1);
            accountService.eidtAccount(account);
        }
        ret.put("type","success");
        return ret;
    }

//    @RequestMapping(value="list",method = RequestMethod.POST)//搜索的时候的参数名
//    @ResponseBody
//    public Map<String,Object> findList(Page page,
//                                       @RequestParam(value = "name",defaultValue = "",required = false)String name,
//                                       @RequestParam(value = "realName",defaultValue = "",required = false)String realName,
//                                       @RequestParam(value = "phoneNum",defaultValue = "",required = false)String phoneNum,
//                                       @RequestParam(value = "idCard",defaultValue = "",required = false)String idCard
//    ) {
//        Map<String,Object> ret=new HashMap<>();
//        Map<String,Object> queryMap=new HashMap<>();
//
//        queryMap.put("name",name);
//        queryMap.put("realName",realName);
//        queryMap.put("phoneNum",phoneNum);
//        queryMap.put("idCard",idCard);
//        queryMap.put("pageSize",page.getRows());
//        queryMap.put("offset",page.getOffset());
//        System.out.println(queryMap);
//
//        ret.put("rows",blackListService.findList(queryMap));
//        ret.put("total",blackListService.getTotal(queryMap));
//        return ret;
//    }

    @RequestMapping("findblackListById")
    @ResponseBody
    public BlackList findblackListById(int id){
        return blackListService.findBlackListById(id);
    }

}
