package com.lontri.lighttherapy.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;
import org.springframework.scheduling.annotation.Async;

public class SocketCmdUtil {
	public static String socketIp = "127.0.0.1";
	public static Integer socketPort = 3366;
	public static String receiveMsg = "";
	@Async  // 让 Spring 线程池管理异步任务，防止阻塞主线程
	public static void send(String message,String IP,int port){
		if(StringUtil.isEmpty(message))return;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try(Socket socket = new Socket(IP,port)){
					System.out.println("connect success");
					PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),false);//初始化缓冲区，及换行符
					writer.write(message);
					writer.println();//发送一个换行符，相当于发送一个空行，表示消息结束
					writer.flush();
					socket.shutdownOutput(); 
					
					InputStream inputStream = socket.getInputStream();  
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));  
					String recMsg = null;  
					while ((recMsg = bufferedReader.readLine()) != null) {
						receiveMsg += recMsg + "\r\n";
						Logger.getLogger("SocketCmdUtil.send").info("收到服务端信息：" + recMsg);
					}  
	
					inputStream.close();
					writer.close();
				}catch(UnknownHostException e){
					//System.out.println("unknow host");
					Logger.getLogger("SocketCmdUtil.send").error(e);
				}catch(IOException e){
					//System.out.println("io ecxeption");
					Logger.getLogger("SocketCmdUtil.send").error(e);
				}catch(Exception e){
					//System.out.println("connect failed");
					Logger.getLogger("SocketCmdUtil.send").error(e);
				}

			}
		});
		t.start();
	}
	public static void send(String message){
		send(message,socketIp,socketPort);
	}
}
