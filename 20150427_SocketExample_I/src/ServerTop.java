import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class ServerTop {
	int portNum;
	int interval;
	ServerSocket serversocket = null;
	Socket socket = null;
	
	ServerControl serverCtrl;
	ServerThread serverthread;
	MonitorThread Monitor; 
	
		
	ServerTop(int interval,int portNum)
	{
		this.interval = interval;
	}
	
	public void mainServer()
	{
		try {
			System.out.println("서버 초기화 중");
			serversocket = new ServerSocket(portNum);
			serverCtrl = new ServerControl(serversocket);
			serverthread = new ServerThread(serversocket,portNum);
			
			ArrayList<Thread> threadList = new ArrayList<Thread>();
			threadList.add(serverCtrl);
			threadList.add(serverthread);
			Monitor = new MonitorThread(threadList, interval);		
			
			Monitor.start();
			serverCtrl.start();
			serverthread.start();
			
				
		} catch (IOException e) {
			System.out.println("포트 열기 예외 :"+e.getMessage());
		}		
	}
}
