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
	
	private static final String SURFACEFLINGER_SEPARATOR="\r\r\n";

	
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
//				System.out.println(window);
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
	/**
	 * 从信息中解析出完整的window 名称
	 * @param info 使用adb shell \"dumpsys activity |grep mResumedActivity\"命令返回的信息
	 * @return windowName
	 */
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
	
	/**
	 * 清除当前SurfaceFlinger Latency数据，并返回是支持该命令
	 * @return 如果支持返回true，否则返回false
	 */
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
	
	/**
	 * SurfaceFlinger数据转换成Long数组
	 * @param result
	 * @param lasttime
	 * @return
	 */
	public static ArrayList<Long> getSurfaceFlingerTime(String[] result,long lasttime){
		ArrayList<Long> timestamp = new ArrayList<>();
		//部分Pad可能存在返回结果大于128的错误数据
		if(result.length>128){
			result = Arrays.copyOf(result, 128);
		}
		for(int i=1;i<result.length;i++){
			String[] u = result[i].split("\t");
			if(u.length!=3){
				System.out.println("b["+i+"]:"+result[i]);
				continue;
			}
			long time =Long.parseLong(u[1]);		
			//time为Long.MAX_VALUE时说明数据异常，该数据可能因不同设备而不同，可以使用{GET_FPS_COMMAND}命令查看
			if(time==0||time==Long.MAX_VALUE||time<=lasttime){
				continue;
			}
			timestamp.add(time);
		}
		return timestamp;
	}
	/**
	 * 返回SurfaceFlinger相邻两行数据差的数组和相邻两行数据差与帧刷新时间的比值数组
	 * @param data SurfaceFlinger数据
	 * @param refresh_period 帧刷新时间
	 * @param isLargeThanHalf 是否需要过滤无效帧即刷新间隔时间低于帧刷新时间一半
	 * @return
	 */
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
	
	/**
	 * 返回SurfaceFlinger相邻两行数据差的数组和相邻两行数据差与帧刷新时间的比值数组
	 * @param data SurfaceFlinger数据
	 * @param refresh_period 帧刷新时间
	 * @return
	 */
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
	
	/**
	 * 计算帧率和卡帧数
	 * @param refresh_period 帧刷新时间
	 * @param timestamps SurfaceFlinger数据（三列的中间列）
	 * @return 返回本次计算最后一帧的时间，用于下一次计算。
	 */
	public static long calculateResults(long refresh_period,ArrayList<Long> timestamps){
		
		int frameCount = timestamps.size();
		long lasttime =timestamps.get(frameCount-1);
		long second = lasttime-timestamps.get(0);
		
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
