package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerThread extends Thread {
	int waitTime;
	String fileName;
	Socket socket;
	SignalData signal;
	
	BufferedInputStream packetInput;
	BufferedOutputStream packetOutput;
	
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
		signal = new SignalData(waitTime);		
		
		try {
			packetInput = new BufferedInputStream(socket.getInputStream());
			packetOutput = new BufferedOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("버퍼 예외");
			e.printStackTrace();
			try {
				socket.close();
			} catch (IOException e1) {
				System.out.println("버퍼 및 소켓 닫기 예외 스레드 종료");
				e1.printStackTrace();
				return ;
			}		
		}
	}
	
	public void test()
	{
		while(true)
		{
			byte[] signalByte = new byte[signal.signalSize];
			try {
				packetInput.read(signalByte);
			} catch (IOException e1) {
				signalByte = null;
				e1.printStackTrace();
			}
			
			if(packetInput == null)
			{
				System.out.println(this.getName()+"스레드 클라이언트 연결 종료");
				try {
					packetInput.close();
					packetOutput.close();
					socket.close();
				} catch (IOException e) {
					System.out.println("클라이언트 연결 종료중 IO예외 ");
					e.printStackTrace();
					return ;
				}
				break;
			}
			signal.toResponse(packetInput, packetOutput, signal.request);
		}
	}
}
