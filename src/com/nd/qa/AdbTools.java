package com.nd.qa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class AdbTools {

	/**
	 * 执行Runtime.exec()并返回结果
	 * @param cmd 命令
	 * @return 返回命令结果，否则返回NULL
	 */
	public static String getCommandResult(String cmd){
		return getCommandResult(cmd, 500);
	}
	
	/**
	 * 执行Runtime.exec()并返回结果
	 * @param cmd 命令
	 * @param millis 等待返回结果的时间，单位毫秒
	 * @return
	 */
	public static String getCommandResult(String cmd,long millis ){
		StringBuilder mStringBuilder = new StringBuilder();
		Process mProcess = null;
		try {
			mProcess = Runtime.getRuntime().exec(cmd);
			InputStream mInputStream = mProcess.getInputStream();
			byte[] buffer = new byte[4*1024];
			/*进入等待，保证结果在收集前返回*/
			Thread.sleep(millis);
				
			while(true){
				int index = mInputStream.read(buffer);
				if(index<1){
					return mStringBuilder.toString();
				}
				mStringBuilder.append(new String(Arrays.copyOf(buffer, index)));
				if(index<4*1024){
					mInputStream.close();
					return mStringBuilder.toString();
				}
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 执行并返回adb命令结果
	 * @param cmd adb命令，例如pull、push
	 * @return
	 */
	public static String getAdbCommandResult(String cmd){
		return getCommandResult("adb "+cmd);
	}
	
	/**
	 * 执行并返回adb shell 命令结果
	 * @param cmd adb shell命令，例如dumpsys
	 * @return
	 */
	public static String getAdbShellCommandResult(String cmd){
		return getAdbCommandResult("shell "+cmd);
	}
}
