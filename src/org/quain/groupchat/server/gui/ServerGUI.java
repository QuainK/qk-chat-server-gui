package org.quain.groupchat.server.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class ServerGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	// 得到显示器屏幕的宽高
	int width = Toolkit.getDefaultToolkit().getScreenSize().width;
	int height = Toolkit.getDefaultToolkit().getScreenSize().height;
	// 定义窗体的宽高
	int windowsWidth = 1200;
	int windowsHeight = 600;

	Thread timeSubThread;// 计时子线程
	Thread serverSubThread;// 服务子线程

	Date startDate;
	Date runDate;

	public static StringBuilder sb = new StringBuilder("");
	public static String newLogLine = "";

	static JFrame frame = new JFrame("QKGroupChat-Server-GUI 1.0.0 Designed by QuainK (电子1603钱坤)");// 初始化框架，窗口标题
	static Container container = frame.getContentPane();// 初始化容器
	static JPanel pnl_basic = new JPanel();// 初始化面板
	static JPanel pnl_time = new JPanel();
	static JPanel pnl_log = new JPanel();
	static JPanel pnl_list = new JPanel();

	File fileName;

	static Font myfont = new Font("黑体", Font.PLAIN, 15);

	static JLabel lab_info = new JLabel("基本配置");
	static JLabel lab_state = new JLabel("状态：关闭");
	static JLabel lab_port = new JLabel("端口：");
	static JTextField txt_port = new JTextField("2333");
	static JLabel lab_maxConnect = new JLabel("最大连接数：");
	static JTextField txt_maxConnect = new JTextField("30");
	static JButton btn_start = new JButton("开始服务");
	static JButton btn_stop = new JButton("停止服务");

	static JLabel lab_time = new JLabel("时间统计");
	static JLabel lab_startTime = new JLabel("开启服务时间：当前状态不可用");
	static JLabel lab_runTime = new JLabel("持续运行时长：当前状态不可用");
	static JLabel lab_currentTime = new JLabel("当前系统时间：正在获取");

	public static JLabel lab_log = new JLabel("运行日志");
	public static JButton btn_exportLog = new JButton("导出日志");
	public static JTextArea txt_log = new JTextArea("");

	public static JLabel lab_list = new JLabel("连接列表");
	public static String[][] data = new String[30][6];
	public static String[] dataTitle = {"序号", "状态", "IP", "用户名", "登录时间", "在线时长"};
	public static JTable table = new JTable(data, dataTitle);

	public static void main(String[] args) {
		ServerGUI myServerGUI = new ServerGUI();// 创建一个非静态类对象
		myServerGUI.createSeverGUI();// 开始渲染GUI
	}

	public void adaptWindows(JFrame frame) {
		// 设置窗体可见
		frame.setVisible(true);
		// 设置窗体位置和大小
		frame.setBounds((width - windowsWidth) / 2, (height - windowsHeight) / 2, windowsWidth, windowsHeight);
		frame.setResizable(false);// 窗体是否可调大小
		frame.setLocationRelativeTo(null);

		try {// JFrame风格转换成当前系统风格(Win10)
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createSeverGUI() {
		adaptWindows(frame);// 窗口调整
		container.setLayout(null);

		lab_info.setBounds(800, 30, 300, 20);
		lab_state.setBounds(800, 60, 100, 20);
		btn_start.setBounds(1000, 60, 100, 25);
		btn_stop.setBounds(1000, 90, 100, 25);
		lab_port.setBounds(800, 90, 50, 20);
		txt_port.setBounds(850, 90, 100, 25);
		lab_maxConnect.setBounds(800, 120, 100, 20);
		txt_maxConnect.setBounds(900, 120, 50, 25);
		lab_info.setHorizontalAlignment(JTextField.CENTER);
		btn_start.setEnabled(true);
		btn_stop.setEnabled(false);
		container.setLayout(null);
		container.add(lab_info);
		container.add(lab_state);
		container.add(lab_port);
		container.add(txt_port);
		// container.add(lab_maxConnect);
		// container.add(txt_maxConnect);
		container.add(btn_start);
		container.add(btn_stop);

		lab_time.setBounds(800, 150, 400, 20);
		lab_startTime.setBounds(800, 210, 400, 20);
		lab_runTime.setBounds(800, 240, 400, 20);
		lab_currentTime.setBounds(800, 180, 400, 20);
		lab_time.setHorizontalAlignment(JTextField.CENTER);
		container.add(lab_time);
		container.add(lab_startTime);
		container.add(lab_runTime);
		container.add(lab_currentTime);

		pnl_log.setLayout(null);
		lab_log.setBounds(100, 30, 600, 20);
		btn_exportLog.setBounds(600, 30, 100, 25);
		txt_log.setBounds(100, 60, 600, 200);
		JScrollPane scrollpane_log = new JScrollPane(txt_log);
		scrollpane_log.setBounds(100, 60, 600, 200);
		lab_log.setHorizontalAlignment(JTextField.CENTER);
		container.add(lab_log);
		container.add(btn_exportLog);
		container.add(scrollpane_log);

		pnl_list.setLayout(null);
		lab_list.setBounds(100, 270, 1000, 20);
		lab_list.setHorizontalAlignment(JTextField.CENTER);
		container.add(lab_list);
		table.setBounds(100, 300, 1000, 200);
		JScrollPane scrollpane_list = new JScrollPane(table);
		scrollpane_list.setBounds(100, 300, 1000, 200);
		for (int row = 0; row < 30; row++) {
			for (int col = 0; col < 6; col++) {
				data[row][col] = "";
			}
		}
		table.setEnabled(false);
		container.add(scrollpane_list);
		container.add(pnl_basic);
		container.add(pnl_time);
		container.add(pnl_log);
		container.add(pnl_list);
		container.setVisible(true);

		frame.setVisible(true);
		newLogLine = "服务器：欢迎使用，点击“启动服务器”按钮开始服务";
		refreshLog();

		setGlobalFont(container.getComponents());// 设置全局字体

		timeSubThread = new Thread(new Runnable() {// 计时子线程
			@Override
			public void run() {
				try {
					while (true) {
						lab_currentTime.setText("当前系统时间：" + getCurrentTimeString());
						if (ServerMain.serverRunFlag) {
							lab_runTime.setText("持续运行时长：" + calculateConstantTimeString(startDate, new Date()));
							for (int i = 0; i < ServerMain.mList.size(); i++) {
								data[i][5] = calculateConstantTimeString(ServerMain.userLoginDate[i], new Date());
//								System.out.println(data[i][5]);
							}
						}
						refreshList();// 更新列表
						Thread.sleep(1000);// 线程休眠
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		timeSubThread.start();
		frame.addWindowListener(new WindowAdapter() {// 窗口监听
			@Override
			public void windowClosing(WindowEvent e) {
				// Object[] options = { "退出服务器GUI", "返回" }; // 自定义按钮上的文字
				// int m = JOptionPane.showOptionDialog(null,
				// "确定退出服务器GUI吗？", "准备退出", JOptionPane.YES_NO_OPTION,
				// JOptionPane.QUESTION_MESSAGE, null, options,
				// options[0]);
				// if (m == 0) {
				// System.exit(0);
				// }else{
				// adaptWindows(frame);// 窗口调整
				// }
				exportLog();
				System.exit(0);
			}
		});

		btn_start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btn_start.setEnabled(false);
				btn_stop.setEnabled(true);
				lab_state.setText("状态：运行");
				ServerMain.PORT = Integer.parseInt(txt_port.getText());
				ServerGUI.newLogLine = "服务器：成功启动服务器";
				ServerGUI.refreshLog();
				lab_runTime.setText("持续运行时长：0天0时0分0秒");
				txt_port.setEnabled(false);
				txt_maxConnect.setEnabled(false);
				serverSubThread = new Thread(new Runnable() {// 主服务子线程
					@Override
					public void run() {
						try {
							startDate = new Date();
							lab_startTime.setText("开启服务时间：" + getCurrentTimeString());
							new ServerMain();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				serverSubThread.start();
			}
		});
		btn_stop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ServerMain.myExecutorService.shutdown();
					ServerMain.serverRunFlag = false;
					ServerMain.stopServer();
					if (!ServerMain.myExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
						// 超时的时候向线程池中所有的线程发出中断(interrupted)。
						ServerMain.myExecutorService.shutdownNow();
					}
				} catch (InterruptedException exception) {
					// awaitTermination方法被中断的时候也中止线程池中全部的线程的执行。
					System.out.println("awaitTermination interrupted: " + exception);
					ServerMain.myExecutorService.shutdownNow();
				}
				lab_state.setText("状态：停止");
				lab_startTime.setText(lab_startTime.getText() + "(上次运行)");
				lab_runTime.setText(lab_runTime.getText() + "(上次运行)");
				newLogLine = "服务器：成功关闭服务器";
				refreshLog();
				for (int row = 0; row < 30; row++) {
					for (int col = 0; col < 6; col++) {
						data[row][col] = "";
					}
				}
				txt_port.setEnabled(true);
				txt_maxConnect.setEnabled(true);
				btn_start.setEnabled(true);
				btn_stop.setEnabled(false);
			}
		});

		btn_exportLog.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				exportLog();
				JOptionPane.showMessageDialog(null, "输出日志成功，位于当前程序路径：\n" + fileName.getAbsolutePath(), "成功",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}

	private static void setGlobalFont(Component[] obj) {
		for (int i = 0; i < obj.length; i++) {
			if (obj[i] instanceof JPanel) {
				JPanel panel = (JPanel) obj[i];
				setGlobalFont(panel.getComponents());
			} else if (obj[i] instanceof Component) {
				obj[i].setFont(myfont);
			}
		}

	}

	public static void refreshLog() {
		sb.append("***" + getCurrentTimeString() + newLogLine + "\n");
		txt_log.setText(sb.toString());
	}

	public static String getCurrentTimeString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String now = sdf.format(new Date());
		return now;
	}

	public static void refreshList() {
		// table.repaint();
		table.validate();
		table.updateUI();
	}

	public String calculateConstantTimeString(Date BeginningDate, Date CompletedDate) {
		long constantSec = 0;
		long oneDaySec = 24 * 60 * 60;
		long oneHourSec = 60 * 60;
		long oneMinSec = 60;
		// 经过的秒数
		constantSec = (CompletedDate.getTime() - BeginningDate.getTime()) / 1000;
		long day = constantSec / oneDaySec; // 计算差多少天[0,∞)
		long hour = constantSec % oneDaySec / oneHourSec;// 减去天数后，计算差多少小时[0,23]
		long min = constantSec % oneDaySec % oneHourSec / oneMinSec; // 减去小时后，计算差多少分钟[0,59]
		long sec = constantSec % oneDaySec % oneHourSec % oneMinSec; // 减去分钟后，计算差多少秒[0,59]
		return day + "天" + hour + "时" + min + "分" + sec + "秒";
	}

	public void exportLog() {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			String now = sdf.format(new Date());
			fileName = new File("QKGroupChatServerLog" + now + ".log");
			FileWriter fileOutput = new FileWriter(fileName);
			fileOutput.write(txt_log.getText());
			fileOutput.flush();
			fileOutput.close();

		} catch (Exception e1) {

		}
	}
}