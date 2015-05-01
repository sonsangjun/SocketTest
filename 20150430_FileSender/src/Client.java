
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;


public class Client {
	final int intervalTime = 5000;
	SharedData shared;
	int portNum;
	int fileSizeIndex;
	String fileName;
	String ServerIP;
	
	Socket fileSizeSocket;
	Socket fileStreamSocket;
	ClientCheckingThread CCT;
	ClientThread CT;
	MonitorThread Monitor;
	ArrayList<Thread> threadList;
	
	
	Client(int portNum, String fileName, int fileSizeIndex,String ServerIP)
	{
		this.portNum = portNum;
		this.fileName = fileName;
		this.fileSizeIndex = fileSizeIndex;		
		this.ServerIP = ServerIP;
		threadList = new ArrayList<Thread>();
		shared = new SharedData(0,fileSizeIndex);
	}
	
	public void mainClient()
	{
		System.out.println("파일을 받기위해 서버와 연결을 시도합니다.");
		try {
			fileSizeSocket = new Socket(ServerIP, portNum);
			fileStreamSocket = new Socket(ServerIP, portNum+1);
				
			CCT = new ClientCheckingThread(fileSizeSocket, shared, fileSizeIndex);
			CT = new ClientThread(fileStreamSocket, shared, fileName);
			threadList.add(CCT);
			threadList.add(CT);
			Monitor = new MonitorThread(threadList, intervalTime);
			
			CCT.start();
			CT.start();
			Monitor.start();
			
			System.out.println("파일을 받기위해 스레드를 시작합니다.");
		} catch (IOException e) {
			System.out.println("IO예외 발생"+e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
