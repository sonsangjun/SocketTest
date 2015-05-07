package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


/* 20150506 메소드 목록
 * Client									생성자
 * public boolean receiveClientID()			서버로부터 ID받음
 * public void run()						스레드 코어
 * public void test_I()						test_로마숫자 는 테스트 메소드
 */

public class Client extends Thread {
	int portNum;
	int waitTime;
	int clientID;
	
	String ServerIP;
	SignalData signal;
	ByteArrayTransCevierRule shared;
	IntegerToByteArray integerToByteArray;
	
	Socket eventSocket;
	Socket cameraSocket;
	Socket voiceSocket;
	
	BufferedInputStream eventInput;
	BufferedOutputStream eventOutput;
	
	//정적변수로 유틸리티 패키지에 ArrayList 선언하고, 타입은 사용자가 선언 한 클래스 . 클래스 안에는 스트림 입출력및 참여한 방번호를 넣어면 된다.
	//그러면 파일 송수신, 위치 정보 송수신등을 쉽게 관리할 수 있다.
	//그외 정적변수로 방 목록을 담는 ArrayList 필요할듯 방을 만들거나 참여할때 참고해야하므로...
	
	public Client(String ServerIP, int portNum,int waitTime)
	{
		this.ServerIP = ServerIP;		
		this.portNum = portNum;
		this.waitTime = waitTime;		
	}
	
	//클라이언트 아이디를 서버로부터 받는다.
	public boolean receiveClientID()
	{
		IntegerToByteArray clientID = new IntegerToByteArray();
		if(signal.toResponse(signal.request))
		{
			if(signal.toResponse(signal.byteReceive))
			{
				byte[] receiveID = new byte[clientID.fileSizeIndex];
				try {
					eventInput.read(receiveID);
					this.clientID = clientID.getInt(receiveID);
					System.out.println("서버로부터 ID를 할당받았습니다. ID : "+this.clientID);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}
		System.out.println("Server로부터 ID할당받기를 실패했습니다.");
		return false;
	}
	
	//서버와 연결종료
	public boolean exitClient()
	{
		try {
			eventSocket.close();
			cameraSocket.close();
			voiceSocket.close();
			return true;
		} catch (IOException e) {
			System.out.println("exitClient도중 예외발생");
			e.printStackTrace();
			return false;
		}
	}
	
	public void run()
	{		
		try {
			System.out.println("서버 연결중");
			eventSocket = new Socket(ServerIP, portNum);
			cameraSocket = new Socket(ServerIP, portNum+1);
			voiceSocket = new Socket(ServerIP, portNum+2);
			
		} catch (IOException e) {
			System.out.println("서버로 연결중 예외");
			e.printStackTrace();
			return ;
		}
		try {
			eventInput = new BufferedInputStream(eventSocket.getInputStream());
			eventOutput = new BufferedOutputStream(eventSocket.getOutputStream());
		} catch (IOException e1) {
			System.out.println(this.getName()+"스트림 예외");
			e1.printStackTrace();
			return;
		}
		
		signal = new SignalData(eventSocket);	//소켓연결후 시그널과 연결
		signal.initial();
		
		if(!receiveClientID())
			return ;
		
		//여기부터 클라이언트에서 작동될 메소드 호출

		test_II();
		
		
		
		
		
		//여기까지 클라이언트에서 작성가능한 부분
		exitClient();
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
					eventSocket.close();
					cameraSocket.close();
					voiceSocket.close();
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
					eventSocket.close();
					cameraSocket.close();
					voiceSocket.close();
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
				System.out.println(eventSocket.getInetAddress().getHostName()+"통신성공");
			else
			{
				System.out.println(eventSocket.getInetAddress().getHostName()+"통신실패");
				try {
					eventSocket.close();
					cameraSocket.close();
					voiceSocket.close();
				} catch (IOException e) {
					System.out.println("Client스레드 test_II메소드 종료중에 예외");
					e.printStackTrace();
				}				
				return ;
			}
		}
	}	
}