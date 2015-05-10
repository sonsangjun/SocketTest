package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;


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
	//클라이언트가 데이터 스트림을 받기위해서는 9001포트와 9002번 포트에 대한 입력대기 스레드를 만들어 놔야할듯
	
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
		
		//여기부터 클라이언트에서 작동될 메소드 호출(todo)

		test_II();
		
		
		
		
		
		//여기까지 클라이언트에서 작성가능한 부분
		exitClient();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	//!!테스트 메소드!!
	//!!Test 메소드!!
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
	
	/*	파일전송 및 방 만들기 테스트
	 * 	클라이언트의 요청을 받아 방을 만들고
	 * 	데이터 스트림을 서버에 전송한다.
	 * 	스트림을 전송받은 서버는 방에 참여한 다른 클라이언트에게 파일을 전송한다.
	 * 	전송을 마치면 연결을 종료한다.
	 * 
	 * 	방명은 : testRoom
	 * 	파일을 받는 입장은 방을 들어간다.
	 * 	파일을 보낸 입장은 방을 만든다.
	 * 
	 */
	
	public void test_III()
	{
		Scanner scan = new Scanner(System.in);
		int res=0;
		
		System.out.println("파일을 받는 입장이면  1\n파일을 보낸 입장이면 2");
		res=scan.nextInt();
		
		if(1==res)
		{
			
		}
		else if(2==res)
		{
			
		}
		
		scan.close();
	}
	
	
}