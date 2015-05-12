package com.nd.qa;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ShellExec {

	private static String TAG = ShellExec.class.getSimpleName();
	private static boolean DEBUG=true;
	private static ShellExec shellExec;
	private FileDescriptor fileDescriptor ;
	private InputStream inputStream;
	private OutputStream outputStream;
	public InputStream errorStream;
	public static String cmd;
	private Process mProcess;
	
	private ShellExec() throws Exception{

		mProcess = Runtime.getRuntime().exec("cmd");
        
        inputStream = mProcess.getInputStream();
        outputStream = mProcess.getOutputStream();
        errorStream = mProcess.getErrorStream();
        
	}
	
	public static ShellExec getInstance() throws Exception{
		if(shellExec==null){
			synchronized (ShellExec.class) {
				if(shellExec==null){
					shellExec = new ShellExec();
				}
			}
		}
		return shellExec;
	}


	/**
	 * 获取进程输入进行操控
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		return inputStream;
	}


	/**
	 * 获取进程输出进行操控
	 * @return
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}
	/**
	 * 运行shell命令
	 * @param command 完整命令
	 */
	public void sendCommand(String command){
		try{
			command = command+" \n";
			outputStream.write(command.getBytes("utf-8"));
			outputStream.flush();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	
	public void close() {
		
		try {
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
