package Sender;

import java.net.Socket;

public class ServerThread extends Thread {
	int waitTime;
	String fileName;
	Socket socket;
	ReceptionThread reception;
	TransMissionThread	transMission;
	
	public ServerThread(Socket socket,int waitTime) 
	{
		this.socket = socket;		
	}
	
	//테스트 서버에 대응하는 생성자
	public ServerThread(Socket socket, String fileName, int waitTime)
	{
		this.fileName = fileName;		
		this.socket = socket;
	}
		
	public void run()
	{
		while(true)
		{
			reception = new ReceptionThread();
			reception.start();
			
		}
		
	}
}
