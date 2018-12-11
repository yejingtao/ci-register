package com.mgtv.autoplug.aliclient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mgtv.autoplug.aliclient.manage.AliyunAutoScalModify;
import com.mgtv.autoplug.aliclient.util.ScalingImageModifyResponse;
import com.mgtv.autoplug.aliclient.util.ScalingImageModifyStatus;

import net.sf.json.JSONObject;

@RestController
public class AliyunAutoImageController {
	
	@Autowired
	private AliyunAutoScalModify aliyunAutoScalModify;
	
	@RequestMapping(value="/updateScalImage", method=RequestMethod.POST)
	public String updateScalImage(@RequestParam(required=true) String intraIp,@RequestParam(required=true) String autoScalId, @RequestParam(required=true) String imagePrefix) {
		aliyunAutoScalModify.updateScalingImageByIntraIp(intraIp, autoScalId, imagePrefix);
		return "OK";
	}
	
	@RequestMapping(value="/scalImageStatus", method=RequestMethod.GET)
	public String fetchScalImage(@RequestParam(required=true) String intraIp) {
		ScalingImageModifyResponse response = aliyunAutoScalModify.getTaskStatus(intraIp);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("status", response==null?ScalingImageModifyStatus.Status_Fail.getName():response.getStatus().getName());
		jsonObject.put("progress", response==null?ScalingImageModifyResponse.Init_Progress:response.getProgress());
		jsonObject.put("message",  response==null?"Nothing from this IP":response.getMessage());
		return jsonObject.toString();
	}

}
