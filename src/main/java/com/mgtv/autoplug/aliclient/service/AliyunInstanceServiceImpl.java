package com.mgtv.autoplug.aliclient.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.mgtv.autoplug.aliclient.util.CommonRequestFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Service
public class AliyunInstanceServiceImpl implements AliyunInstanceService{

	public final Logger logger = LoggerFactory.getLogger(AliyunInstanceServiceImpl.class);

	public final static String ECS_DOMAIN = "ecs.aliyuncs.com";

	public final static String ECS_VERSION = "2014-05-26";

	@Autowired
	private IAcsClient client;

	// 通过基准实例的内网ip查询对应实例id
	@Override
	public List<String> getInstanceIdByIntraIp(String intraIp) throws ServerException, ClientException {

		List<String> instanceIdList = new ArrayList<String>();

		CommonRequest request = CommonRequestFactory.createRequest(ECS_DOMAIN, ECS_VERSION, "DescribeInstances");
		request.putQueryParameter("PrivateIpAddresses", String.format("[\"%s\"]", intraIp));
		request.putQueryParameter("InstanceNetworkType", "vpc");
		CommonResponse response = client.getCommonResponse(request);

		// 处理返回值
		int returnStatus = response.getHttpStatus();
		logger.info(String.format("Describe Instance %s return status %d", intraIp, returnStatus));
		logger.debug(response.getData());

		if (returnStatus == 200) {
			// 解析返回内容
			JSONObject rootObject = JSONObject.fromObject(response.getData());
			JSONArray jsonArray = JSONArray
					.fromObject(JSONObject.fromObject(rootObject.get("Instances")).get("Instance"));
			if (jsonArray != null && jsonArray.size() > 0) {
				for (int i = 0; i < jsonArray.size(); i++) {
					instanceIdList.add(jsonArray.getJSONObject(i).getString("InstanceId"));
				}
			}
		}

		return instanceIdList;
	}

	// 使用实例id创建一个新镜像，记录镜像id
	@Override
	public String createImageByInstanceId(String instanceId, String imageName) throws ServerException, ClientException {

		String newImageId = null;

		CommonRequest request = CommonRequestFactory.createRequest(ECS_DOMAIN, ECS_VERSION, "CreateImage");
		request.putQueryParameter("InstanceId", instanceId);
		request.putQueryParameter("ImageName", imageName);
		CommonResponse response = client.getCommonResponse(request);

		// 处理返回值
		int returnStatus = response.getHttpStatus();
		logger.info(String.format("CreateImage by InstanceId %s return status %d", instanceId, returnStatus));
		logger.debug(response.getData());

		if (returnStatus == 200) {
			// 解析返回内容
			JSONObject rootObject = JSONObject.fromObject(response.getData());
			newImageId = rootObject.getString("ImageId");
		}

		return newImageId;
	}

	// 通过镜像id查询镜像状态
	@Override
	public String[] getImageStatusById(String imageId, String status) throws ServerException, ClientException {

		String[] imageInfo = null;

		CommonRequest request = CommonRequestFactory.createRequest(ECS_DOMAIN, ECS_VERSION, "DescribeImages");
		request.putQueryParameter("ImageId", imageId);
		if(status!=null) {
			//阿里云次接口，默认只查可用的镜像
			request.putQueryParameter("Status", status);
		}
		CommonResponse response = client.getCommonResponse(request);

		// 处理返回值
		int returnStatus = response.getHttpStatus();
		logger.info(String.format("Describe Image by imageId %s return status %d", imageId, returnStatus));
		logger.debug(response.getData());

		// 解析返回内容
		JSONObject rootObject = JSONObject.fromObject(response.getData());
		JSONArray jsonArray = JSONArray.fromObject(JSONObject.fromObject(rootObject.get("Images")).get("Image"));
		if (jsonArray != null && jsonArray.size() == 1) {
			imageInfo = new String[2];
			imageInfo[0] = jsonArray.getJSONObject(0).getString("Status");
			imageInfo[1] = jsonArray.getJSONObject(0).getString("Progress");
		}

		return imageInfo;
	}

}
