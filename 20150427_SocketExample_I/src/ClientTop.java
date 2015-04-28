import java.net.Socket;
import java.util.ArrayList;


public class ClientTop {
	String serverIP;
	int portNum;
	int interval;
	
	Socket socket;
	ClientTop(String serverIP, int portNum, int interval)
	{
		this.serverIP = serverIP;
		this.portNum = portNum;
		this.interval = interval;		
	}
	
	public void mainCilent()
	{
		System.out.println("클라이언트 초기화 중");
		ClientThread client = new ClientThread(serverIP, portNum, interval);
		ArrayList<Thread> threadList = new ArrayList<Thread>();
		threadList.add(client);
		
		MonitorThread Monitor = new MonitorThread(threadList, interval);
		client.start();
		Monitor.start();
	}
}
