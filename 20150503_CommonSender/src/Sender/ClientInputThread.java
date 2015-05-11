package Sender;

import java.io.BufferedInputStream;
import java.net.Socket;

//20150511_클라이언트가 input을 받아야 할 경우가 생기기 때문에 스레드 선언
public class ClientInputThread {
	Socket socket;
	SignalData signal;
	
	public ClientInputThread(Socket socket, SignalData signal,Client) {
		// TODO Auto-generated constructor stub
		this.socket = socket;
		this.signal = signal;
	}
	
	public void run()
	{
		while(true)
		{
			if()
			
		}
	}
}
