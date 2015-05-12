package com.nd.qa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SurfaceCollector {

	public static void main(String[] args) {
		
		runFpsCollector();
			
	}
	
	private static final String GET_FPS_COMMAND = "dumpsys SurfaceFlinger --latency";
	private static final String CLEAR_FPS_COMMAND = "dumpsys SurfaceFlinger --latency-clear";
	// com.android.settings/com.android.settings.Settings
	private static final String WINDOW="com.nd.pad.smarthome/com.nd.launcher.core.launcher.Launcher";
	private static final String SURFACEFLINGER_SEPARATOR="\r\r\n";

	private static  String Contacts_Window ="com.nd.pad.contacts/com.nd.pad.contacts.MainActivity";
	private static  String Email_Window ="com.android.email/com.android.email.activity.Welcome";
	
	private static  String Camera_Window ="com.nd.pad.smartcamera/com.android.camera.CameraLauncher";
	private static  String Gallery_Window ="com.android.gallery3d/com.android.gallery3d.app.GalleryActivity";
	private static  String Browser_Window ="com.nd.pad.browser/.ui.activities.MainActivity";
	private static String SurfaceView = "SurfaceView";
	private static String Video_Window ="com.softwinner.fireplayer/com.softwinner.fireplayer.ui.VideoPlayerActivity";
	
	
	public static void runFpsCollector(String window){
		long lasttime =0;
		if(clearSurfaceFlingerLatencyData()){
			while(true){
				long start =System.currentTimeMillis();
				String result = getCommandResult(window);
				String[] resultArray = result.split(SURFACEFLINGER_SEPARATOR);
				long refresh_period = 0;
				try{
					refresh_period = Long.parseLong(resultArray[0]);
				}catch(Exception e){
					System.out.println("resultArray[0]:"+resultArray[0]);
				}
				if(refresh_period!=0){
					ArrayList<Long> data = getSurfaceFlingerTime(resultArray, lasttime);
					if(data.size()>3){
						lasttime =calculateResults(refresh_period, data);
					}else{
						System.out.println("no fps data");
					}
					
				}
			}
		}else{
			System.out.println("not support !");
		}
	}
	
	public static void runFpsCollector(){
		
		if(clearSurfaceFlingerLatencyData()){
			long lasttime =0;
			while(true){
				
				String windowInfo = AdbTools.getCommandResult("adb shell \"dumpsys activity |grep mResumedActivity\"",100);
				String window = getWindowName(windowInfo);
				if(!window.equals("")){
					String result = getCommandResult(window);
					String[] resultArray = result.split(SURFACEFLINGER_SEPARATOR);
					long refresh_period = 0;
					try{
						refresh_period = Long.parseLong(resultArray[0]);
					}catch(Exception e){
						System.out.println("resultArray[0]:"+resultArray[0]);
					}
					if(refresh_period!=0){
						ArrayList<Long> data = getSurfaceFlingerTime(resultArray, lasttime);
						if(data.size()>3){
							lasttime =calculateResults(refresh_period, data);
						}else{
							System.out.println("jank:0;fps:0");
						}
						
					}
				}else{
					System.out.println("current winodw not found");
				}
			}
			
		}else{
			System.out.println("not support !");
		}
		
		
	}
	
	public static String getWindowName(String info){
		String window = "";
		if(info!=null&&info.contains("mResumedActivity")){
			String[] windowInfoToArray = info.split(" ");
			for(String s:windowInfoToArray){
				if(s.contains("/")){
					if(s.contains("/.")){
						String[] strArray = s.split("/");
						window = strArray[0]+"/"+strArray[0]+strArray[1];
					}else{
						window = s;
					}
				}
			}
		}else{
			System.out.println("window not found");
		}
		return window;
	}
	
	public static boolean clearSurfaceFlingerLatencyData(){
		boolean isSupport = false;
		Process mProcess = null;
		try {
			mProcess = Runtime.getRuntime().exec("adb shell "+CLEAR_FPS_COMMAND);
			InputStream is = mProcess.getInputStream();
			InputStream error = mProcess.getErrorStream();
			byte[] buffer_is = new byte[4*1024];
			byte[] buffer_error = new byte[4*1024];
			int index_is = is.read(buffer_is);
			int index_error = error.read(buffer_error);
			if(index_is==-1&&index_error==-1){
				isSupport = true;
			}else{
				System.out.println(new String(Arrays.copyOf(buffer_error, index_error)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isSupport;
	}
	
	public static String getCommandResult(String window){
		return AdbTools.getAdbShellCommandResult(GET_FPS_COMMAND+" " +window);
	}
	
	
	public static ArrayList<Long> getSurfaceFlingerTime(String[] result,long lasttime){
		ArrayList<Long> timestamp = new ArrayList<>();
		if(result.length>128){
			return timestamp;
		}
		for(int i=1;i<result.length;i++){
			String[] u = result[i].split("\t");
			if(u.length!=3){
				System.out.println("b["+i+"]:"+result[i]);
				continue;
			}
			long time =Long.parseLong(u[1]);		
			//time为Long.MAX_VALUE时说明数据异常
			if(time==0||time==Long.MAX_VALUE||time<=lasttime){
				continue;
			}
			timestamp.add(time);
		}
		return timestamp;
	}
	
	public static SurfaceFlingerData getSurfaceFlingerDataSubtraction(ArrayList<Long> data,long refresh_period,boolean isLargeThanHalf){
		SurfaceFlingerData mSurfaceFlingerData = new SurfaceFlingerData();
		ArrayList<Long> timestamp = new ArrayList<Long>();
		ArrayList<Double> timestampRate = new ArrayList<Double>();
		for(int i=0;i<data.size()-1;i++){
			long tamp = data.get(i+1)-data.get(i);
			if(isLargeThanHalf){
				//如果2帧间隔时间小于刷新时间的一半则是无效帧
				if(tamp*1.0/refresh_period>=0.5){
					timestamp.add(tamp);
					timestampRate.add(tamp*1.0/refresh_period);
				}
			}else{
				timestamp.add(tamp);
				timestampRate.add(tamp*1.0/refresh_period);
			}
			
		}
		mSurfaceFlingerData.setTimestamps(timestamp);
		mSurfaceFlingerData.setTimestampsRate(timestampRate);
		return mSurfaceFlingerData;
	}
	
	public static SurfaceFlingerData getSurfaceFlingerDataSubtraction(ArrayList<Long> data,long refresh_period){
		SurfaceFlingerData mSurfaceFlingerData = new SurfaceFlingerData();
		ArrayList<Long> timestamp = new ArrayList<Long>();
		ArrayList<Double> timestampRate = new ArrayList<Double>();
		for(int i=0;i<data.size()-1;i++){
			long tamp = data.get(i+1)-data.get(i);
			timestamp.add(tamp);
			timestampRate.add(tamp*1.0/refresh_period);
			
		}
		mSurfaceFlingerData.setTimestamps(timestamp);
		mSurfaceFlingerData.setTimestampsRate(timestampRate);
		return mSurfaceFlingerData;
	}
	
	public static long calculateResults(long refresh_period,ArrayList<Long> timestamps){
		
		int frameCount = timestamps.size();
//		System.out.println("framecount:"+frameCount);
		long lasttime =timestamps.get(frameCount-1);
		long second = lasttime-timestamps.get(0);
//		System.out.println("second:"+second/1e9);
		
		SurfaceFlingerData mSurfaceFlingerData = getSurfaceFlingerDataSubtraction(timestamps,refresh_period);
		ArrayList<Long> frameLengths = mSurfaceFlingerData.getTimestamps();
		ArrayList<Double>  normalized_frame_lengths = mSurfaceFlingerData.getTimestampsRate();
		if(frameLengths.size()<frameCount-1){
			frameCount = frameLengths.size()+1;
		}
		mSurfaceFlingerData = getSurfaceFlingerDataSubtraction(frameLengths, refresh_period);
		ArrayList<Long> frameChanges = mSurfaceFlingerData.getTimestamps();
		ArrayList<Double> normalizedChanges = mSurfaceFlingerData.getTimestampsRate();
		int jank =0;
		/*帧延迟上限*/
		int pause_threshold =20;
		for(double d:normalizedChanges){
			long roundD = Math.round(d); 
			if(roundD>0 && roundD<20){
				jank++;
			}
			
		}
		System.out.println();
		int fps = (int)Math.round((frameCount-1)*1e9/second);
		System.out.println("jank:"+jank+";fps:"+fps);
		return lasttime;
	}
}
