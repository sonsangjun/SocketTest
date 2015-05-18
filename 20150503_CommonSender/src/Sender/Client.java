package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.sound.midi.VoiceStatus;

//20150511
//클라이언트는 요청 뿐만 아니라 위치 정보등을 받아야 하므로 input , output에 대한 스레드를 만들어야 한다.
//고민... Client스레드 안에 input , output에 대한 스레드를 만들고, 각 스레드는 Busywaiting을 통해 서로 락을 걸면 되겠다.
//그러다가 데드락 걸리지는 않겠지.
//clientsRequest(String command)─┐ 요거
//클라이언트의 요청에 대한 메소드 구현하고, 이 메소드는 while(true) 걸어두고, 이 메소드 호출전에 input에 대한 스레드를 생성하자.

/* 20150506 메소드 목록
 * Client									생성자
 * public boolean receiveClientID()			서버로부터 ID받음
 * public void run()						스레드 코어
 * public void test_I()						test_로마숫자 는 테스트 메소드
 */

public class Client extends Thread {
	ValueCollections value = new ValueCollections();
	
	int portNum = value.portNum;
	int waitTime = value.waitTime;
	String ServerIP = value.ServerIP;
	int clientID;
	byte[] cameraByteArray;		//카메라 프리뷰 바이트 배열
	byte[] voiceByteArray;		//음성 바이트 배열
	String roomName;
	
	
	SignalData signal;
	ByteArrayTransCevierRule shared;		//데이터 스트림 송수신 역할
	IntegerToByteArray integerToByteArray;
	
	Socket broadCastSocket;
	Socket eventSocket;
	Socket cameraSocket;
	Socket voiceSocket;
	
	BufferedInputStream eventInput;
	BufferedOutputStream eventOutput;
	
	SocketBroadCastUsed socketBroadCastUsed = new SocketBroadCastUsed();
	SocketEventUsed socketEventUsed = new SocketEventUsed();
	SocketCameraUsed socketCameraUsed = new SocketCameraUsed();
	SocketVoiceUsed socketVoiceUsed = new SocketVoiceUsed();
	
	RoomData roomData;			//방 목록을 받아오기 위해 선언
	RoomManage roomManage;		//방 관리하는 클래스
	
	//정적변수로 유틸리티 패키지에 ArrayList 선언하고, 타입은 사용자가 선언 한 클래스 . 클래스 안에는 스트림 입출력및 참여한 방번호를 넣어면 된다.
	//그러면 파일 송수신, 위치 정보 송수신등을 쉽게 관리할 수 있다.
	//그외 정적변수로 방 목록을 담는 ArrayList 필요할듯 방을 만들거나 참여할때 참고해야하므로...
	//클라이언트가 데이터 스트림을 받기위해서는 9001포트와 9002번 포트에 대한 입력대기 스레드를 만들어 놔야할듯
	
	public Client(byte[] cameraByteArray, byte[] voiceByteArray)
	{
		this.cameraByteArray = cameraByteArray;
		this.voiceByteArray = voiceByteArray;
	}
		
	public void run()
	{		
		try {
			System.out.println("서버 연결중");
			broadCastSocket = new Socket(ServerIP, portNum-1);
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
		roomName = new String(value.unname);
		
		
		//-----------------------------------------------------------------------------
		//여기부터 클라이언트에서 작동될 메소드 호출(todo)
		//나중에 안드로이드를 통해 통신을 한다면... ClientSharedData에서 안드로이드와 이 스레드간 공유변수를 추가로
		//선언해야 될지도 모른다. -> 안드로이드에 핸들러를 이용하라고 적혀있다. 
		//안드로이드 앱을 만들어 봐야겠다. 거기에 맞게 스레드를 제작하자. 여기 클라이언트는 일단 시험용.
		
		
		clientTerminate();
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		//여기까지 클라이언트에서 작성가능한 부분
		//-----------------------------------------------------------------------------		
		exitClient();
	}
	
	//클라이언트 아이디를 서버로부터 받는다.
	public boolean receiveClientID()
	{
		IntegerToByteArray clientID = new IntegerToByteArray();
		if(signal.toAccept(signal.byteReceive))
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
		System.out.println("Server로부터 ID할당받기를 실패했습니다.");
		return false;
	}
	
	//서버와 연결종료
	public boolean exitClient()
	{
		try {
			broadCastSocket.close();
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
	
	//클라이언트가 서버에게 요청하는 부분
	//방과 관련된게 아니면 null입력
	//이 함수 호출하기 전에 ClientSharedData.inputUsed = true;로 바꾸고 호출끝나면 false로바꿈
	

	
	
	
	
	
	
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
			if(signal.toDoRequest(signal.request))
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
	
	public void clientTerminate()
	{
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		String value = null;
		RoomDataToArray roomDataToArray;
		synchronized (roomManage) {
			roomManage = new RoomManage("", clientID, null, eventSocket, null, null, null, signal);
		}
		
		while(true)
		{
			System.out.println("명령을 입력하세요.");
			System.out.println(signal.signalByteToString(signal.makeRoom)+" 방만들기\t"+signal.signalByteToString(signal.joinRoom)+"방참여\t "+signal.signalByteToString(signal.exitRoom)+" 방나가기\t"+signal.signalByteToString(signal.exitServer)+" 나가기");
			try {
				value = inputReader.readLine();
			} catch (IOException e) {
				System.out.println("입력에러");
				e.printStackTrace();
				continue;
			}			
			if(value.equals(signal.signalByteToString(signal.makeRoom)) || value.equals(signal.signalByteToString(signal.joinRoom)) || value.equals(signal.signalByteToString(signal.exitRoom)))
			{
				socketEventUsed.socketEventUsed = true;
				System.out.println("방 이름을 입력하세요.");
				try {
					value = inputReader.readLine();
				} catch (IOException e) {
					System.out.println("방 입력하는 중 예외 발생");
					e.printStackTrace();
					socketEventUsed.socketEventUsed = false;
					continue;
				}
				if(value.equals(signal.signalByteToString(signal.makeRoom)))
				{
					if(roomManage.clientsRequest(signal.makeRoom, value, roomName, null))
					{
						socketEventUsed.socketEventUsed = false;
						continue;					
					}
				}
				else if(value.equals(signal.signalByteToString(signal.joinRoom)))
				{
					if(roomManage.clientsRequest(signal.joinRoom, value, roomName, null))
					{
						socketEventUsed.socketEventUsed = false;
						continue;					
					}
				}
				else if(value.equals(signal.signalByteToString(signal.exitRoom)))
				{
					if(roomManage.clientsRequest(signal.exitRoom, null, roomName, null))
					{
						socketEventUsed.socketEventUsed = false;
						continue;
					}
				}				
			}
			else
			{
				System.out.println("클라이언트를 종료합니다.");
				exitClient();
				break;
			}			
		}
	}
}