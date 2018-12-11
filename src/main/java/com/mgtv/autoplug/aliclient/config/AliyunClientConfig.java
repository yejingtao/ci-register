package com.mgtv.autoplug.aliclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;

@Configuration
public class AliyunClientConfig {

	@Value("${accessKeyID}")
	private String accessKeyID;
	
	@Value("${accessKeySecret}")
	private String accessKeySecret;

	@Bean
	public IAcsClient client() {
		System.out.println("Init client");
		return new DefaultAcsClient(DefaultProfile.getProfile("cn-beijing", accessKeyID, accessKeySecret));
	}

}
