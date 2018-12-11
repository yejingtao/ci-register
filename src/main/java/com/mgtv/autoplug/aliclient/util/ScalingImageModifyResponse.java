package com.mgtv.autoplug.aliclient.util;

public class ScalingImageModifyResponse {
	
	public final static String Init_Progress = "0%";
	
	public final static String Full_Progress = "100%";
	
	private ScalingImageModifyStatus status;
	
	private String imageId;
	
	private String progress;
	
	private String message;

	public ScalingImageModifyStatus getStatus() {
		return status;
	}

	public void setStatus(ScalingImageModifyStatus status) {
		this.status = status;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getProgress() {
		return progress;
	}

	public void setProgress(String progress) {
		this.progress = progress;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	
	
}
