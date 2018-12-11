package com.mgtv.autoplug.aliclient.manage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.mgtv.autoplug.aliclient.service.AliyunAutoScalService;
import com.mgtv.autoplug.aliclient.service.AliyunInstanceService;
import com.mgtv.autoplug.aliclient.util.ScalingImageModifyResponse;
import com.mgtv.autoplug.aliclient.util.ScalingImageModifyStatus;

@Service
public class AliyunAutoScalModifyImpl implements AliyunAutoScalModify{

	public final Logger logger = LoggerFactory.getLogger(AliyunAutoScalModifyImpl.class);

	@Autowired
	private AliyunAutoScalService aliyunAutoScalService;

	@Autowired
	private AliyunInstanceService aliyunInstanceService;

	private ExecutorService threadPool = Executors.newCachedThreadPool();

	private Map<String, ScalingImageModifyResponse> tasksMap = new HashMap<String, ScalingImageModifyResponse>();

	private final static String Instance_Error_NONE = "Can not find instance by ip";

	private final static String Image_Error_NONE = "Can not find image by id";
	
	private final static String Image_Error_RETRY = "DescribeImages API error";

	private final static String AutoScal_Error_NONE = "Can not find scaling configuration by scalingId";

	private final static String Image_Status_Creating = "Creating"; // 镜像正在创建中
	private final static String Image_Status_Waiting = "Waiting"; // 多任务排队中
	private final static String Image_Status_Available = "Available"; // 您可以使用的镜像
	private final static String Image_Status_UnAvailable = "UnAvailable"; // 您不能使用的镜像
	private final static String Image_Status_CreateFailed = "CreateFailed"; // 创建失败的镜像
	
	private final static String Image_Status_Full = "Creating,Waiting,Available,UnAvailable,CreateFailed";
	
	@Override
	public ScalingImageModifyResponse getTaskStatus(String intraIp) {
		return tasksMap.get(intraIp);
	}

	// 这个要加锁
	private synchronized void setTaskStatus(String intraIp, ScalingImageModifyStatus status, String message,
			String progress) {
		ScalingImageModifyResponse thisResponse = tasksMap.get(intraIp);
		if (thisResponse == null) {
			thisResponse = new ScalingImageModifyResponse();
		}
		if (status != null) {
			thisResponse.setStatus(status);
		}
		if (message != null) {
			thisResponse.setMessage(message);
		}
		if (progress != null) {
			thisResponse.setProgress(progress);
		}
		tasksMap.put(intraIp, thisResponse);
	}

	// 验证是否可执行
	@Override
	public boolean validateModifyRequest(String intraIp) {
		if (getTaskStatus(intraIp) != null
				&& getTaskStatus(intraIp).getStatus() == ScalingImageModifyStatus.Status_Running) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 核心方法，根据实例ID更新伸缩组的镜像配置
	 * 
	 * @param intraIp
	 * @param scalingId
	 * @return
	 */
	@Override
	public void updateScalingImageByIntraIp(String intraIp, String scalingId, String imagePrefix) {
		// 验证是否重复执行
		// 只有验证通过才可以更新镜像
		logger.info(String.format("Get image modify request, parameters: %s %s", intraIp,scalingId));
		if (validateModifyRequest(intraIp)) {
			// 初始化
			setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Running, null,
					ScalingImageModifyResponse.Init_Progress);

			// 线程池异步操作
			threadPool.execute(new Runnable() {
				public void run() {
					try {
						List<String> instanceIds = aliyunInstanceService.getInstanceIdByIntraIp(intraIp);

						// 如果没找到实例id或者多个实例id，均失败
						if (instanceIds.size() != 1) {
							logger.error(String.format("Instance list size is not 1: %d", instanceIds.size()));
							setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Fail, Instance_Error_NONE, null);
							// 提前结束
							return;
						}

						// 通过伸缩组id查询到生效中的伸缩配置
						String configId = aliyunAutoScalService.getAutoScalingConfigById(scalingId);
						if (configId == null) {
							logger.error(String.format("Can not find scaling configuration by scalingId %s", scalingId));
							// 如果没找到，报错并提前结束
							setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Fail, AutoScal_Error_NONE, null);
							// 提前结束
							return;
						}
						
						
						// 找到实例后，构造镜像
						DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
						String imageName = imagePrefix + "-" + format.format(new Date());
						String newImageId = aliyunInstanceService.createImageByInstanceId(instanceIds.get(0),imageName);

						// 监控镜像制作进度，并维护progress
						// 最多等待120min
						int maxWaitTimes = 180;
						int errorMax = 3;
						
						while (maxWaitTimes > 0) {
							String[] imageInfo = null;
							try {
								imageInfo = aliyunInstanceService.getImageStatusById(newImageId,Image_Status_Full);
							}catch(Exception e) {
								//实战中发现，阿里云的API并不稳定，查询镜像失败后增加3次重试
								e.printStackTrace();
								logger.error(e.getMessage());
								try {
									// 60s更新一次状态
									Thread.sleep(60000);
								} catch (InterruptedException ie) {
									ie.printStackTrace();
								}
								errorMax = errorMax - 1;
								if(errorMax>=0) {
									continue;
								}else {
									// 如果多次重试还是失败，直接结束
									setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Fail, Image_Error_RETRY, null);
									logger.error(String.format("DescribeImages API error by id %s", newImageId));
									return;
								}
								
							}
														
							if (imageInfo == null) {
								// 如果没找到镜像，可能平台内部错误，报错退出
								setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Fail, Image_Error_NONE, null);
								logger.error(String.format("Can not find new image by id %s", newImageId));
								return;
							} else if (Image_Status_UnAvailable.equals(imageInfo[0])
									|| Image_Status_CreateFailed.equals(imageInfo[0])) {
								// 如果镜像制作的有问题，可能平台内部错误，报错退出
								setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Fail, imageInfo[0], null);
								logger.error(String.format("Image create error %s", imageInfo[0]));
								return;
							} else if (Image_Status_Waiting.equals(imageInfo[0])
									|| Image_Status_Creating.equals(imageInfo[0])) {
								// 如果多任务排队或者镜像制作中，设置progress
								setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Running, null, imageInfo[1]);
								logger.info(String.format("Status: %s Process: %s", imageInfo[0],imageInfo[1]));
								// 不能退出
							} else if (Image_Status_Available.equals(imageInfo[0])) {
								// 如果镜像制作完毕，跳出while循环
								setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Running, null,
										ScalingImageModifyResponse.Full_Progress);
								logger.info(String.format("Image create success by id %s !", newImageId));
								break;
							}

							try {
								// 60s更新一次状态
								Thread.sleep(60000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							maxWaitTimes--;
						}

						// 修改伸缩配置，指向新的镜像
						aliyunAutoScalService.updateScalingImageById(configId, newImageId);

						// 执行结束，成功退出
						setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Success, null, null);

					} catch (ServerException e) {
						e.printStackTrace();
						setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Fail, e.getMessage(), null);
						logger.error(e.getMessage());
					} catch (ClientException e) {
						e.printStackTrace();
						setTaskStatus(intraIp, ScalingImageModifyStatus.Status_Fail, e.getMessage(), null);
						logger.error(e.getMessage());
					}
				}
			});
		}
	}
}
