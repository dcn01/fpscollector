package com.nd.qa;

import java.util.ArrayList;

public class SurfaceFlingerData {

	public ArrayList<Long> timestamps;//SurfaceFlinger前后一帧数据的差值
	public ArrayList<Double> timestampsRate;//SurfaceFlinger前后一帧数据的差值与刷新时间的比值
	public ArrayList<Long> getTimestamps() {
		return timestamps;
	}
	public void setTimestamps(ArrayList<Long> timestamps) {
		this.timestamps = timestamps;
	}
	public ArrayList<Double> getTimestampsRate() {
		return timestampsRate;
	}
	public void setTimestampsRate(ArrayList<Double> timestampsRate) {
		this.timestampsRate = timestampsRate;
	}
	
}
