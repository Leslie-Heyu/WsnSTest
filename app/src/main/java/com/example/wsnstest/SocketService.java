package com.example.wsnstest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.http.util.EncodingUtils;
import com.example.wsnstest.R;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

public class SocketService extends Service {
	public static Socket socket;
	public static DataInputStream in;
	public static DataOutputStream out;
	public static boolean bSocketflag;
	public static String strMessageFor;
	byte[] recvbuffer = new byte[1024];

	RecvThread thread;

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public SocketService getService() {
			return SocketService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		
		Log.i("onCreate1", "onCreate");
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.detectDiskReads().detectDiskWrites().detectNetwork()
		.penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
				.build());

		bSocketflag = false;
		strMessageFor = "ActivityMain";

		String stripaddr = "";
		String strport = "";
		if (fileIsExists("Settings.ini")) {
			String strSettings = readFile("Settings.ini");
			String[] strs = strSettings.split(";");
			for (int i = 0; i < strs.length; i++) {
				if (readSettingName(strs[i]).equals("ipaddress")) {
					stripaddr = readSettingValue(strs[i]).toString();
					Log.i("stripaddr1", stripaddr);
				} else if (readSettingName(strs[i]).equals("port")) {
					strport = readSettingValue(strs[i]).toString();
					Log.i("strport1", strport);
				}
			}
		}
		try {
			socket = new Socket();
			socket.setSoTimeout(100);
			SocketAddress isa = new InetSocketAddress(stripaddr,
					Integer.parseInt(strport));
			socket.connect(isa, 100);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			bSocketflag = true;
			Toast.makeText(getApplicationContext(), R.string.connect_success,
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			bSocketflag = false;
			Toast.makeText(getApplicationContext(), R.string.connect_failed,
					Toast.LENGTH_SHORT).show();
		}

		thread = new RecvThread();
		thread.start();
	}

	@Override
	public void onDestroy() {
		if (bSocketflag) {
			try {
				in.close();
				out.close();
				socket.close();
			} catch (IOException e) {
			}
		}
		super.onDestroy();
	}

	class RecvThread extends Thread {
		public void run() {
			while (true) {
				if (bSocketflag) {
					try {
						int iCount = in.read(recvbuffer);
						if (iCount != -1) {
							String str = new String(recvbuffer, 0, iCount);
							sendMsgtoActivty(str);
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}

	public void reConnectSocket(String stripaddr, int iport) {
		if (bSocketflag) {
			try {
				in.close();
				out.close();
				socket.close();
			} catch (IOException e) {
			}
		}
		try {
			socket = new Socket();
			socket.setSoTimeout(100);
			SocketAddress isa = new InetSocketAddress(stripaddr, iport);
			socket.connect(isa, 100);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			bSocketflag = true;
			Toast.makeText(getApplicationContext(), R.string.connect_success,
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			bSocketflag = false;
			Toast.makeText(getApplicationContext(), R.string.connect_failed,
					Toast.LENGTH_SHORT).show();
		}
	}

	public void sendMsgtoActivty(String msg) {
		Intent intent = new Intent(strMessageFor);
		intent.putExtra("msg", msg);
		sendBroadcast(intent);
	}

	public void socketSend(String strBuff) {
		if (bSocketflag) {
			try {
				out.writeBytes(strBuff);
			} catch (IOException e) {
			}
		} else {
			Toast.makeText(SocketService.this, R.string.connect_please, Toast.LENGTH_SHORT)
					.show();
		}
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

	protected int char2int(char c) {
		if (c >= '0' && c <= '9')
			return (int) (c - '0');
		else if (c <= 'f' && c >= 'a')
			return (int) (c - 'a' + 10);
		else if (c <= 'F' && c >= 'A')
			return (int) (c - 'A' + 10);
		return -1;
	}
}
