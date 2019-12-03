package com.example.wsnstest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.util.EncodingUtils;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WsnSTest extends Activity {

	private static final String MYTAG = "WsnSTest";

	private static final int DIALOG_CONNECT = 0;

	private SocketService myservice;
	private ServiceMsgReceiver myServiceMsg;

	EditText editIpaddress, editPort;

	static final int MAXLEN = 2048;
	static final byte HEAD = 0x02;
	private int TimeFlag = 1;
	static final int DELAY = 150;

	private boolean bLock;
	private byte iRecDataLen;
	private byte[] byteRecBuff = new byte[MAXLEN];

	byte[] bytesTemp = new byte[2];
	TextView txt;
	TextView txt2;
	TextView txt3;
	Button btn;
	boolean btnn=false;

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			myservice = ((SocketService.LocalBinder) service).getService();
			SocketService.strMessageFor = MYTAG;

			// yanglei test
			// myservice.bSocketflag = true;
			myservice.socketSend("4C4B0104");

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			myservice = null;
		}
	};

	public class ServiceMsgReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MYTAG)) {
				String msg = intent.getStringExtra("msg");

				// yang test
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
				// Log.i("cvtech", MYTAG);

				// 缓存数据
				ReceiveData(HexString2Bytes(msg), msg.length() / 2);
			}
		}
	}

	private Timer mTimer;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				// 处理数据
				ScanData();

				break;
			}
			super.handleMessage(msg);
		}
	};

	private void stopTimer() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
	}

	public void initWidgetInfo() {
		myServiceMsg = new ServiceMsgReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(MYTAG);
		registerReceiver(myServiceMsg, filter);

		if (!fileIsExists("Settings.ini")) {
			writeFile("Settings.ini", "ipaddress=192.168.1.5;port=2012;");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wsns_test);
		txt = (TextView)findViewById(R.id.light);
		txt2 = (TextView)findViewById(R.id.temp);
		txt3 = (TextView)findViewById(R.id.water);
		btn = (Button)findViewById(R.id.alarm);
		btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 自动生成的方法存根
				if(btnn){
					
				}else{
					ring();
				}
			}
		});
		initWidgetInfo();
		// findviews();

		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Message message = new Message();
				message.what = TimeFlag;
				handler.sendMessage(message);
			}
		}, DELAY, DELAY);

		bindService(new Intent(this, SocketService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_wsn_stest, menu);
		return true;
	}

	@Override
	public void onResume() {
		SocketService.strMessageFor = MYTAG;
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO 自动生成的方法存根
		stopTimer();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		stopTimer();
		super.onDestroy();
	}

	public void onClick_Event(View v) {
		byte[] bytesSend;
		switch (v.getId()) {
		case R.id.main_shutdown:
			new AlertDialog.Builder(WsnSTest.this)
					.setIcon(R.drawable.ic_home_small)
					.setTitle(R.string.app_exit)
					.setMessage("确认退出吗？")
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialoginterface, int i) {
									stopTimer();
									finish();
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialoginterface, int i) {
								}
							}).show();
			break;
		case R.id.main_btn3:
			showDialog(DIALOG_CONNECT);
			break;

		default:
			break;
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(WsnSTest.this)
					.setIcon(R.drawable.ic_home_small)
					.setTitle(R.string.app_exit)
					.setMessage("确认退出吗？")
					.setPositiveButton("确认",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialoginterface, int i) {
									finish();
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialoginterface, int i) {
								}
							}).show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_CONNECT:
			View DialogView = LayoutInflater.from(this).inflate(
					R.layout.connect_settings, null);
			editIpaddress = (EditText) DialogView
					.findViewById(R.id.edit_ipaddress);
			editPort = (EditText) DialogView.findViewById(R.id.edit_port);

			dialog = new AlertDialog.Builder(WsnSTest.this)
					.setIcon(R.drawable.ic_home_small)
					.setTitle(R.string.app_settings)
					.setView(DialogView)
					.setPositiveButton("连接",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialoginterface, int i) {
									if (fileIsExists("Settings.ini")) {
										String strSettings = readFile("Settings.ini");
										String[] strs = strSettings.split(";");
										String stripaddr = "";
										String strport = "";
										String strTemp = "";
										for (int j = 0; j < strs.length; j++) {
											if (readSettingName(strs[j])
													.equals("ipaddress")) {
												stripaddr = editIpaddress
														.getText().toString();
												strs[j] = "ipaddress="
														+ stripaddr;
											} else if (readSettingName(strs[j])
													.equals("port")) {
												strport = editPort.getText()
														.toString();
												strs[j] = "port=" + strport;
											}
										}
										for (int j = 0; j < strs.length; j++) {
											strTemp += strs[j] + ";";
										}
										writeFile("Settings.ini", strTemp);

										myservice.reConnectSocket(stripaddr,
												Integer.parseInt(strport));
									}
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialoginterface, int i) {
								}
							}).create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		switch (id) {
		case DIALOG_CONNECT:
			if (fileIsExists("Settings.ini")) {
				String strSettings = readFile("Settings.ini");
				String[] strs = strSettings.split(";");
				for (int i = 0; i < strs.length; i++) {
					if (readSettingName(strs[i]).equals("ipaddress")) {
						editIpaddress.setText(readSettingValue(strs[i])
								.toString());
					} else if (readSettingName(strs[i]).equals("port")) {
						editPort.setText(readSettingValue(strs[i]).toString());
					}
				}
			}
			break;
		}
		super.onPrepareDialog(id, dialog, args);
	}

	public boolean fileIsExists(String fileName) {
		try {
			// 一定使用绝对路径
			File f = new File("/data/data/com.example.wsnstest/files/"
					+ fileName);
			if (!f.exists()) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void writeFile(String fileName, String strWrite) {
		try {
			FileOutputStream fout = openFileOutput(fileName, MODE_PRIVATE);
			byte[] bytes = strWrite.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String readFile(String fileName) {
		String res = "";
		try {
			FileInputStream fin = openFileInput(fileName);
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			res = EncodingUtils.getString(buffer, "UTF-8");
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public String readSettingName(String str) {
		return str.substring(0, str.indexOf("="));
	}

	public String readSettingValue(String str) {
		return str.substring(str.indexOf("=") + 1, str.length());
	}

	public static byte uniteBytes(byte src0, byte src1) {
		byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 }))
				.byteValue();
		_b0 = (byte) (_b0 << 4);
		byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 }))
				.byteValue();
		byte ret = (byte) (_b0 ^ _b1);
		return ret;
	}

	private static byte[] HexString2Bytes(String src) {
		byte[] ret = new byte[src.length() / 2];
		byte[] tmp = src.getBytes();
		for (int i = 0; i < tmp.length / 2; i++) {
			ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
		}
		return ret;
	}

	private static String Bytes2HexString(byte[] bytes) {
		String ret = "";
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(bytes[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			ret += hex.toUpperCase();
		}
		return ret;
	}

	private void ReceiveData(byte[] bRecData, int iSize) {
		if (bLock == false) {
			bLock = true;
			if (iRecDataLen + iSize <= MAXLEN) {
				for (int i = 0; i < iSize; i++) {
					byteRecBuff[iRecDataLen + i] = bRecData[i];
				}
				iRecDataLen += iSize;
			}
			bLock = false;
		}
	}

	private byte SummationCheck(byte[] BytesData) {
		int i, uSum = BytesData[1];
		for (i = 2; i < BytesData.length - 1; i++) {
			uSum ^= BytesData[i];
		}
		return (byte) uSum;
	}

	private void DateDispose(byte[] Packet) {
		if (SummationCheck(Packet) == Packet[Packet.length - 1]) {
			Log.i("...WsnSTest DateDispose...", Bytes2HexString(Packet));
			int type=Packet[10] & 0x3f;
			if(type==2){
				byte sun=(byte)(Packet[11]);
				txt.setText(String.valueOf(sun));
				if(sun>=200) {
					ring();
				}
			}else if(type==00) {
				byte tempe=(byte)(Packet[11]);
				byte water=(byte)(Packet[12]);
				txt2.setText(String.valueOf(tempe));
				txt3.setText(String.valueOf(water));
				if(tempe>=25 || water >=50) {
					ring();
				}
			}

		}
	}

	private void ScanData() {
		if (bLock == false) {
			bLock = true;
			if (iRecDataLen > 0) {
				int iFirstPosition = 0;
				while (iFirstPosition < iRecDataLen) {
					// 判断包头
					if (byteRecBuff[iFirstPosition] == HEAD) {
						int PacketLen = (int) byteRecBuff[iFirstPosition + 1] + 3;
						// 判断数据包是否完整
						if (PacketLen <= iRecDataLen - iFirstPosition) {
							// 读出一个数据包
							byte[] Packet = new byte[PacketLen];
							int i;
							for (i = 0; i < PacketLen; i++) {
								Packet[i] = byteRecBuff[iFirstPosition + i];
							}
							// 数据包处理
							DateDispose(Packet);
							// 缓冲区删除已读部分
							for (i = 0; i < iRecDataLen - iFirstPosition
									- PacketLen; i++) {
								byteRecBuff[i] = byteRecBuff[iFirstPosition
										+ PacketLen + i];
							}
							iRecDataLen -= PacketLen + iFirstPosition;
						}
						bLock = false;
						return;
					} else {
						iFirstPosition++;
					}
				}
				iRecDataLen = 0;
			}
			bLock = false;
		}
	}
	
public void ring() {
		byte[] bytesSendz = new byte[13];
		bytesSendz[0] = (byte) 0x02;
		bytesSendz[1] = (byte) 0x0a;
		bytesSendz[2] = (byte) 0xb8;
		bytesSendz[3] = (byte) 0x47;
		bytesSendz[4] = (byte) 0xf1;
		bytesSendz[5] = (byte) bytesTemp[0];
		bytesSendz[6] = (byte) bytesTemp[1];
		bytesSendz[7] = (byte) 0x01;
		bytesSendz[8] = (byte) 0x10;
		bytesSendz[9] = (byte) 0x03;
		bytesSendz[10] = (byte) 0x00;
		bytesSendz[11] = (byte) 0x6F;
		bytesSendz[12] = SummationCheck(bytesSendz);
		myservice.socketSend(Bytes2HexString(bytesSendz));
   }
}