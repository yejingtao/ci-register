package com.mgtv.autoplug.aliclient.service;

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
public class AliyunAutoScalServiceImpl implements AliyunAutoScalService{

	public final Logger logger = LoggerFactory.getLogger(AliyunAutoScalServiceImpl.class);

	public final static String ESS_DOMAIN = "ess.aliyuncs.com";

	public final static String ESS_VERSION = "2014-08-28";

	@Autowired
	private IAcsClient client;

	// 通过伸缩组id查询到生效中的伸缩配置，记录ID
	@Override
	public String getAutoScalingConfigById(String scalingId) throws ServerException, ClientException {

		String scalingConfigurationId = null;

		CommonRequest request = CommonRequestFactory.createRequest(ESS_DOMAIN, ESS_VERSION,
				"DescribeScalingGroups");
		request.putQueryParameter("ScalingGroupId.1", scalingId);
		CommonResponse response = client.getCommonResponse(request);

		// 处理返回值
		int returnStatus = response.getHttpStatus();
		logger.info(String.format("Describe ScalingConfiguration by scalingId %s return status %d", scalingId,
				returnStatus));
		logger.debug(response.getData());

		// 解析返回内容
		JSONObject rootObject = JSONObject.fromObject(response.getData());
		JSONArray jsonArray = JSONArray
				.fromObject(JSONObject.fromObject(rootObject.get("ScalingGroups")).get("ScalingGroup"));
		if (jsonArray != null && jsonArray.size() == 1) {
			scalingConfigurationId = jsonArray.getJSONObject(0).getString("ActiveScalingConfigurationId");
		}

		return scalingConfigurationId;
	}

	// 修改伸缩配置，指向新的镜像
	@Override
	public void updateScalingImageById(String configId, String imageId) throws ServerException, ClientException {

		CommonRequest request = CommonRequestFactory.createRequest(ESS_DOMAIN, ESS_VERSION,
				"ModifyScalingConfiguration");
		request.putQueryParameter("ScalingConfigurationId", configId);
		request.putQueryParameter("ImageId", imageId);
		CommonResponse response = client.getCommonResponse(request);

		// 处理返回值
		int returnStatus = response.getHttpStatus();
		logger.info(String.format("Modify ScalingConfiguration image by configId %s and ImageId %s return status %d",
				configId, imageId, returnStatus));
		logger.debug(response.getData());
	}
}
