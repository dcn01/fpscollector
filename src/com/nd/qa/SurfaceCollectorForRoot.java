package com.nd.qa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




public class SurfaceCollectorForRoot {
	
	private ShellExec shellExec;
	private long old_Time;
	private long current_Time;
	private int old_FPSIndex;
	
	private OutputStream os;
	private InputStream is;
	private InputStream error;
	private boolean flag =true;
	
	
	public static void main(String[] args) {
		
		String helpInfo = "FPS collect tool version 1.0 \n"
				+ "-d	-fps default collector \n"
				+ "-root	-another fps collector,pad need root \n"
				+ "-h	-show help info";
		if(args.length>0){
			switch (args[0].toLowerCase()) {
			case "-root":
				SurfaceCollectorForRoot root = new SurfaceCollectorForRoot();
				try {
					root.startFPSProcess();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "-d":
				SurfaceCollector.runFpsCollector();
				break;
			case "-h":
			default:
				
				System.out.println(helpInfo);
				break;
			}
		}else{
			System.out.println(helpInfo);
		}
		
		

		
	}
	

	
	/**
	 * 获取service call SurfaceFlinger 1013命令的FPS的index的16进制值
	 * @param content 运行命令后返回的结果
	 * @return
	 */
	public String getFPSIndex(String content){
		String res="";
		if(content.contains("Parcel")){
			Pattern pattern = Pattern.compile("(?<=\\()\\w+");
			Matcher matcher = pattern.matcher(content);
			matcher.find();
			try{
				res = matcher.group().trim();	
			}catch(java.lang.IllegalStateException e){
				e.printStackTrace();
			}
			
		}
		return res;
	}

	Thread osthread = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(flag){
				try {
					shellExec.sendCommand("adb shell service call SurfaceFlinger 1013");
			
					if(old_Time==0){
						old_Time = System.currentTimeMillis();
					}else{
						current_Time = System.currentTimeMillis();
					}
					
					Thread.sleep(1000);
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		
		}
	});
	
	
	Thread isThread = new Thread(new Runnable() {

		@Override
		public void run() {
			byte[] buffer = new byte[4 * 1024];
			int index = 0;
			while (flag) {
				try {
					index = is.read(buffer);
					if (index == -1) {
						return;
					}
					if (index == 0) {
						continue;
					}
					byte[] info = Arrays.copyOf(buffer, index);

					String infoString = new String(info);
//					System.out.println("read info:" + infoString);

					if (infoString.contains("Parcel")) {
						if (old_FPSIndex == 0) {
							old_FPSIndex = Integer.parseInt(
									getFPSIndex(infoString), 16);
						} else {
							int current_FPSIndex = Integer.parseInt(
									getFPSIndex(infoString), 16);
							long timeIndex =current_Time - old_Time;
							long fps_r = Math.round((current_FPSIndex - old_FPSIndex)
									* 1000.0 / timeIndex);
							old_FPSIndex = current_FPSIndex;
							old_Time = current_Time;
							System.out.println("fps:" + fps_r);
						}

					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}
	});
	
	Thread eThread = new Thread(new Runnable() {

		@Override
		public void run() {
			byte[] buffer = new byte[4 * 1024];
			int index = 0;
			while (flag) {
				try {
					index = error.read(buffer);
					if (index == -1) {
						return;
					}
					if (index == 0) {
						continue;
					}
					byte[] info = Arrays.copyOf(buffer, index);

					String infoString = new String(info);
//					System.out.println("read error info:" + infoString);
					if(infoString.contains("error")){
						flag = false;
						System.out.println(infoString);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}
	});

	public void startFPSProcess() throws Exception{
		shellExec = ShellExec.getInstance();
		os = shellExec.getOutputStream();
		is = shellExec.getInputStream();
		error = shellExec.errorStream;
		shellExec.sendCommand("adb root");
		shellExec.sendCommand("adb shell dumpsys SurfaceFlinger --latency");
		osthread.start();
		isThread.start();
		eThread.start();
	}
}
