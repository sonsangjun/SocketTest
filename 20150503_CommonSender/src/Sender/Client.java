package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends Thread {
	int portNum;
	int waitTime;
	int fileSizeIndex;
	
	String ServerIP;
	Socket socket;
	SignalData signal;
	SharedData shared;
	FileSizeChecking fileSizeChecking;
	
	BufferedInputStream packetInput;
	BufferedOutputStream packetOutput;
	
	//정적변수로 유틸리티 패키지에 ArrayList 선언하고, 타입은 사용자가 선언 한 클래스 . 클래스 안에는 스트림 입출력및 참여한 방번호를 넣어면 된다.
	//그러면 파일 송수신, 위치 정보 송수신등을 쉽게 관리할 수 있다.
	//그외 정적변수로 방 목록을 담는 ArrayList 필요할듯 방을 만들거나 참여할때 참고해야하므로...
	
	public Client(String ServerIP, int portNum,int waitTime, int fileSizeIndex)
	{
		this.ServerIP = ServerIP;		
		this.portNum = portNum;
		this.waitTime = waitTime;
		this.fileSizeIndex = fileSizeIndex;
		
	}
	
	public void run()
	{		
		try {
			System.out.println("서버 연결중");
			socket = new Socket(ServerIP, portNum);
		} catch (IOException e) {
			System.out.println("서버로 연결중 예외");
			e.printStackTrace();
			return ;
		}
		try {
			packetInput = new BufferedInputStream(socket.getInputStream());
			packetOutput = new BufferedOutputStream(socket.getOutputStream());
		} catch (IOException e1) {
			System.out.println(this.getName()+"스트림 예외");
			e1.printStackTrace();
			return;
		}
		
		signal = new SignalData(socket, waitTime);	//소켓연결후 시그널과 연결
		signal.initial();
		//여기부터 클라이언트에서 작동될 메소드 호출

		test_II();
	}
	
	
	
	
	//Test메소드들
	public void test_I()
	{
		System.out.println("서버와 연결되었습니다.");
		while(true)
		{
			//원래대로면 방 만들던가 참여하던가 선택해야지( 테스트 이므로 제낀다. )
			System.out.println("신호 테스트");
			if(signal.toRequest())
			{
				System.out.println("서버와 통신을 성공했습니다.");
				System.out.println("서버와 연결을 종료합니다.");
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println("Client스레드 종료중에 예외");
					e.printStackTrace();
					return ;
				}
				return ;
			}
			else
			{
				System.out.println("서버와 통신을 실패했습니다.");
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println("Client스레드 종료중에 예외");
					e.printStackTrace();
					return ;
				}
				return;				
			}
		}	
	}
	
	public void test_II()
	{
		System.out.println("서버와 연결되었습니다.");
		System.out.println("서버 연속 신호 송수신 테스트");
		
		while(true)
		{
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(signal.toRequest())
				System.out.println("통신성공");
			else
			{
				System.out.println("통신실패");
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println("Client스레드 test_II메소드 종료중에 예외");
					e.printStackTrace();
				}				
				return ;
			}
		}
	}	
}