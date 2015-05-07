package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerThread extends Thread {
	final String unname = new String("unname");	//방에 참여안했을때, unname으로 할당
		
	int waitTime;
	String fileName;
	
	Socket eventSocket;					//Socket부터
	Socket cameraSocket;
	Socket voiceSocket;
	
	BufferedInputStream eventInput;
	BufferedOutputStream eventOutput;
	
	ClientManagement clientManagement;	//정적 배열리스트가 관리의 편의를 위해 선언
	RoomManagement roomManagement;		//방 관리
	SignalData signal;
	
	//서버에서 ClientManageList를 일일이 뒤지려면 오래걸리므로 파일송수신 스레드를 돌리기전에 뿌려야하는 명단을 서버에게 추려서 주도록하자.
	//방 자체도 String이 아닌 String 와 해당 방의 사람 수를 포함한 클래스로 선언하면 관리하기 더 쉬울듯.
	//서버에 존재하는 방이름과 각종 스트림은 정적 ArrayList로 관리한다.
	static ArrayList<ClientManagement> clientManagementList = new ArrayList<ClientManagement>();
	static ArrayList<RoomManagement> joinRoomList = new ArrayList<RoomManagement>();
	static int assignedClientID = 0;
	
	public ServerThread(Socket eventSocket,	Socket cameraSocket,Socket voiceSocket,	int waitTime) 
	{
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
			
		this.waitTime = waitTime;
	}
	
	//테스트 서버에 대응하는 생성자
	public ServerThread(Socket eventSocket,	Socket cameraSocket,Socket voiceSocket, String fileName, int waitTime)
	{
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
		
		this.fileName = fileName;		
		this.waitTime = waitTime;
	}
	
	//할당된 ID번호를 클라이언트에게 전송
	public boolean transClientID(int assignedClientID)
	{
		if(signal.toRequest())
		{
			if(signal.toDoRequest(signal.byteReceive))			//신호는 무조건 명령이다. 바이트신호 받는걸 요청한다. => 바이트 받아랏
			{
				IntegerToByteArray convert = new IntegerToByteArray();
				byte[] ByteArrayID = new byte[convert.fileSizeIndex];
				try {
					convert.initialByteArray(ByteArrayID);
					convert.getBytes(assignedClientID, ByteArrayID);
					eventOutput.write(ByteArrayID);
					eventOutput.flush();					//flush(); 꼭 넣자 이것때문에 블락되는경우가 있다.
					return true;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
		System.out.println("클라이언트에게 ID할당하는데 실패했습니다.");
		return false;				
	}
	
	//방 만들기
	public boolean makeRoom(String roomName)
	{
		if(roomName.equals(unname))
			return false;				//unname은 쓸수없다.
		for(RoomManagement R:joinRoomList)
		{
			if(R.roomName.equals(roomName))
			{
				return false;
			}
		}
		System.out.println(roomName+" 이 추가되었습니다.");
		clientManagement.joinRoom = new String(roomName);
		
		synchronized (joinRoomList) {
			joinRoomList.add(new RoomManagement(roomName));	
		}		
		return true;		
	}
	
	//빈방 삭제하기
	public boolean checkingJoinRoom()
	{
		for(RoomManagement R:joinRoomList)
		{
			if(R.joinNumber <= 0)
			{
				synchronized (joinRoomList) {
					System.out.println(R.roomName+" 삭제되었습니다.");
					joinRoomList.remove(R);		//방 삭제
				}				
				return true;					
			}
		}
		return false;
	}
	
	//방 출입
	public boolean joinRoom(String roomName)
	{	
		
		for(RoomManagement R:joinRoomList)
		{
			if(R.equals(roomName))
			{				
				R.joinNumber++;
				clientManagement.joinRoom = new String(roomName);
				return true;
			}
		}
		System.out.println(clientManagement.clientID+" 가 "+roomName+" 방에 참여하지 못했습니다.");
		return false;
	}
	
	public boolean exitRoom(String roomName)
	{
		for(RoomManagement R:joinRoomList)
		{
			if(R.equals(roomName))
			{				
				synchronized (R) {
					R.joinNumber--;
					clientManagement.joinRoom = new String(unname);					
				}				
				return true;
			}
		}
		return false;
	}
	
	//클라이언트 연결종료
	//클라이언트에 대한 수많은 서버 스레드가 두 클래스에 접근시 예측이 어려움으로 동기화 작성
	public boolean exitServer()
	{
		try {
			eventSocket.close();
			cameraSocket.close();
			voiceSocket.close();
			
			for(RoomManagement R:joinRoomList)
			{
				if(clientManagement.joinRoom.equals(R.roomName))
				{
					synchronized (R) {
						R.joinNumber--;						
					}
					break;
				}
			}
			clientManagementList.remove(clientManagement);						
			return true;
		} catch (IOException e) {
			System.out.println("exitServer도중 예외발생");
			e.printStackTrace();
			return false;
		}
	}
	
	//스레드 코어
	public void run()
	{
		System.out.println(eventSocket.getInetAddress().getHostName()+"과 연결되었습니다.");
		
		try {
			eventInput = new BufferedInputStream(eventSocket.getInputStream());
			eventOutput = new BufferedOutputStream(eventSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("버퍼 예외");
			e.printStackTrace();
			try {
				System.out.println("ServerThread의 IO예외 발생으로 소켓을 닫습니다.");
				eventSocket.close();
				cameraSocket.close();
				voiceSocket.close();
			} catch (IOException e1) {
				System.out.println("버퍼 및 소켓 닫기 예외 스레드 종료");
				e1.printStackTrace();
				return ;
			}		
		}
		
		signal = new SignalData(eventSocket);	//소켓연결후 시그널과 연결
		signal.initial();
		
		assignedClientID++;
		if(!transClientID(assignedClientID))
			return ;
		
		clientManagement = new ClientManagement(assignedClientID, new String(eventSocket.getInetAddress().getHostName()), unname, signal, eventSocket, cameraSocket, voiceSocket, eventInput, eventOutput);
		
		//여기부터 서버에서 사용될 메소드 호출
		//막 들어온 클라이언트는 방에 참여하기 전까지 clientManagement는 정적배열리스트에 추가하지 않는다.
		
		test_II();		
		//이 윗부분사이에 서버 코드를 작성하면 된다.
		//클라이언트와 연결종료한다.
		exitServer();
		checkingJoinRoom();
	}
	
	
	
	
	
	
	
	
	//테스트 메소드
	public void test_I()
	{
		while(true)
		{
			if(signal.toResponse(signal.request))
			{
				System.out.println(this.getName()+"스레드 클라이언트 연결 종료");
				try {
					eventSocket.close();
					cameraSocket.close();
					voiceSocket.close();
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
				System.out.println(eventSocket.getInetAddress().getHostName()+"와 통신성공");
			else
			{
				System.out.println(eventSocket.getInetAddress().getHostName()+"와 통신실패");
				try {
					eventSocket.close();
					cameraSocket.close();
					voiceSocket.close();
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
