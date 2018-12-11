package com.mgtv.autoplug.aliclient.util;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.http.MethodType;

public class CommonRequestFactory {
	
	public final static String REGIONID = "cn-beijing";

	
	public static CommonRequest createRequest(MethodType methodType, String domain, String version, String action, String regionId) {
		CommonRequest request = new CommonRequest();
		request.setMethod(methodType);
        request.setDomain(domain);
        request.setVersion(version);
        request.setAction(action);
        request.putQueryParameter("RegionId", regionId);
        return request;
	}
	
	public static CommonRequest createRequest(String domain,String version,String action) {
		return createRequest(MethodType.POST,domain,version,action,REGIONID);
	}

}
