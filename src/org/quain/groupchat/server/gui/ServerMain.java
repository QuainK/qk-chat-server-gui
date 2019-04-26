package org.quain.groupchat.server.gui;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ServerMain {
	// 定义相关的参数,端口,存储Socket连接的集合,ServerSocket对象,线程池
	public static int PORT = 2333;
	public static int maxConnect = 30;
	public static List<Socket> mList = new ArrayList<Socket>();
	public static ServerSocket server = null;
	public static ExecutorService myExecutorService = null;

	public static String serverMsgToSend;
	public static String[] userMsgToSend = new String[maxConnect];
	public static InetAddress userAddress[] = new InetAddress[maxConnect];
	public static Date userLoginDate[] = new Date[30];
	public static boolean serverRunFlag = false;

	public static void main(String[] args) {
		new ServerMain();
	}

	public ServerMain() {
		try {
			server = new ServerSocket(PORT);
			serverRunFlag = true;
			myExecutorService = Executors.newCachedThreadPool();
			ServerGUI.newLogLine = "服务器：服务器运行中，监听端口" + PORT;
			System.out.println("***" + getCurrentTimeString() + ServerGUI.newLogLine);
			ServerGUI.refreshLog();
			Socket client = null;
			while (serverRunFlag) {
				client = server.accept();
				String clientAddressString = client.getInetAddress().toString();
				/*
				 * 不是localhost或者127.0.0.1才可以添加一个socket，
				 * 这样也会使得运行于电脑的手机模拟器无法正常连接，因为它们使用localhost即127.0.0.1
				 */
				if (!(clientAddressString.equals("localhost") || clientAddressString.equals("/127.0.0.1"))) {
					mList.add(client);
					myExecutorService.execute(new Service(client));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class Service implements Runnable {
		private Socket socket;
		private BufferedReader in = null;

		public Service(Socket socket) {
			this.socket = socket;
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				// 0 序号
				ServerGUI.data[mList.size() - 1][0] = String.valueOf(mList.size() - 1);
				// 1 状态
				ServerGUI.data[mList.size() - 1][1] = "已连接";
				// 2 IP地址
				userAddress[mList.size() - 1] = this.socket.getInetAddress();
				ServerGUI.data[mList.size() - 1][2] = String.valueOf(this.socket.getInetAddress());
				// 3 用户名
				ServerGUI.data[mList.size() - 1][3] = String.valueOf(in.readLine());
				// 4 登录时间
				userLoginDate[mList.size() - 1] = new Date();
				ServerGUI.data[mList.size() - 1][4] = getCurrentTimeString();
				// 5 在线时长
				ServerGUI.data[mList.size() - 1][5] = "0天0时0分0秒";
				serverMsgToSend = "用户" + ServerGUI.data[mList.size() - 1][3] + "加入群聊，当前" + mList.size() + "人在线";
				this.sendMsg(-1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			String userMsgReceived = "";
			try {
				while (serverRunFlag) {
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					if ((socket != null) && (userMsgReceived = in.readLine()) != null) {
						for (int i = 0; i < userAddress.length; ++i) {
							if (userAddress[i].equals(this.socket.getInetAddress())) {
								userMsgToSend[i] = userMsgReceived;
								this.sendMsg(i);
								break;
							}
						}
					} else {
						mList.remove(socket);
						for (int i = 0; i < userAddress.length; ++i) {
							if (userAddress[i].equals(this.socket.getInetAddress())) {
								serverMsgToSend = "服务器：用户" + ServerGUI.data[i][3].toString() + "退出群聊，当前" + mList.size()
										+ "人在线";
								userAddress[i] = null;
								userLoginDate[i] = null;
								for (int j = 0; j < 6; j++) {
									ServerGUI.data[i][j] = "";
								}
								break;
							}
						}
						in.close();
						socket.close();
						ServerGUI.newLogLine = serverMsgToSend;
						ServerGUI.refreshLog();
						this.sendMsg(-1);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 为连接上服务端的每个客户端发送信息
		public void sendMsg(int msgCreatorIndex) {
			int num = mList.size();

			for (int index = 0; index < num; index++) {
				Socket mSocket = mList.get(index);
				PrintWriter pout = null;
				try {
					pout = new PrintWriter(
							new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), "UTF-8")), true);
					if (msgCreatorIndex == -1) {
						pout.println("***" + getCurrentTimeString() + "服务器：" + serverMsgToSend);
						ServerGUI.newLogLine = "服务器：" + serverMsgToSend;
					} else {
						pout.println("***" + getCurrentTimeString() + ServerGUI.data[msgCreatorIndex][3] + "："
								+ userMsgToSend[msgCreatorIndex]);
						ServerGUI.newLogLine = ServerGUI.data[msgCreatorIndex][3] + "："
								+ userMsgToSend[msgCreatorIndex];
					}
					pout.flush();
					System.out.println("***" + getCurrentTimeString() + ServerGUI.newLogLine);
					ServerGUI.refreshLog();
				} catch (IOException e) {
					e.printStackTrace();
					ServerGUI.newLogLine = getCurrentTimeString() + "服务器：群发服务器/用户消息失败";
					System.out.println("***" + getCurrentTimeString() + ServerGUI.newLogLine);
					ServerGUI.refreshLog();
				}
			}

		}

	}

	// 获取当前时间
	public static String getCurrentTimeString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String now = sdf.format(new Date());
		return now;
	}

	public static void stopServer() {
		Thread stopServerSubThread = new Thread(new Runnable() {
			@Override
			public void run() {
				serverRunFlag = false;
				ServerGUI.newLogLine = "服务器：正在断开所有客户端连接并关闭服务器";
				System.out.println("***" + getCurrentTimeString() + ServerGUI.newLogLine);
				ServerGUI.refreshLog();
				ServerGUI.btn_stop.setText("正在关闭");
				ServerGUI.btn_stop.setEnabled(false);
				try {
					Socket tmpSocket = new Socket("localhost", PORT);
					Thread.sleep(100);
					tmpSocket.close();
					server.close();
					server = null;
					ServerGUI.btn_stop.setText("关闭服务");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		stopServerSubThread.run();
	}

}