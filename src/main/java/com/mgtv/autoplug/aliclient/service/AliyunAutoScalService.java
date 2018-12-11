package com.mgtv.autoplug.aliclient.service;

import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;

public interface AliyunAutoScalService {
	
	// 通过伸缩组id查询到生效中的伸缩配置，记录ID
	String getAutoScalingConfigById(String scalingId) throws ServerException, ClientException;
	
	// 修改伸缩配置，指向新的镜像
	void updateScalingImageById(String configId, String imageId) throws ServerException, ClientException;
	
	


}
