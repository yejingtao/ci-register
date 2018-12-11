package com.mgtv.autoplug.aliclient.util;

public enum ScalingImageModifyStatus {
	
	Status_Success("Successful"),
	Status_Running("Running"),
	Status_Fail("Failed");
	
	private String name;
    
    private ScalingImageModifyStatus(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }


}
