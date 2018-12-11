package com.mgtv.autoplug.aliclient.manage;

import com.mgtv.autoplug.aliclient.util.ScalingImageModifyResponse;

public interface AliyunAutoScalModify {
	
	/**
	 * 验证是否可执行, 最终一致性，该接口暂时可以不开放
	 * @param intraIp
	 * @return
	 */
	boolean validateModifyRequest(String intraIp);
	
	/**
	 * 核心方法，根据实例ID更新伸缩组的镜像配置
	 * @param intraIp
	 * @param scalingId
	 */
	void updateScalingImageByIntraIp(String intraIp, String scalingId, String imagePrefix);
	
	/**
	 * 获取任务进度,主要获取status和progress
	 * @param intraIp
	 * @return
	 */
	ScalingImageModifyResponse getTaskStatus(String intraIp);
	
}
