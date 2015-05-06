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
		this.waitTime = waitTime;
	}
	
	//테스트 서버에 대응하는 생성자
	public ServerThread(Socket socket, String fileName, int waitTime)
	{
		this.fileName = fileName;		
		this.socket = socket;
		this.waitTime = waitTime;
	}
		
	public void run()
	{
		System.out.println(socket.getInetAddress().getHostName()+"과 연결되었습니다.");
		signal = new SignalData(socket,waitTime);
		
		try {
			packetInput = new BufferedInputStream(socket.getInputStream());
			packetOutput = new BufferedOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("버퍼 예외");
			e.printStackTrace();
			try {
				System.out.println("ServerThread의 IO예외 발생으로 소켓을 닫습니다.");
				socket.close();
			} catch (IOException e1) {
				System.out.println("버퍼 및 소켓 닫기 예외 스레드 종료");
				e1.printStackTrace();
				return ;
			}		
		}
		test_II();
	}
	
	public void test_I()
	{
		while(true)
		{
			if(signal.toResponse(signal.request))
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
			else
			{
				System.out.println("통신이 정상적으로 이루어지지 않았습니다.");
				return ;
			}
		}
	}
	
	public void test_II()
	{
		while(true)
		{
			if(signal.toResponse(signal.request))
				System.out.println(socket.getInetAddress().getHostName()+"와 통신성공");
			else
			{
				System.out.println(socket.getInetAddress().getHostName()+"와 통신실패");
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
		}
	}
}
