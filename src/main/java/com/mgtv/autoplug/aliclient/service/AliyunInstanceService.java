package com.mgtv.autoplug.aliclient.service;

import java.util.List;

import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;

public interface AliyunInstanceService {
	
	// 通过基准实例的内网ip查询对应实例id
	List<String> getInstanceIdByIntraIp(String intraIp) throws ServerException, ClientException;
	
	// 使用实例id创建一个新镜像，记录镜像id
	String createImageByInstanceId(String instanceId, String imageName) throws ServerException, ClientException;

	// 通过镜像id查询镜像状态
	String[] getImageStatusById(String imageId, String status) throws ServerException, ClientException;			
}
