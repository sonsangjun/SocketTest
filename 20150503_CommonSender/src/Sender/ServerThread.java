package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerThread extends Thread {
	final String unname = new String("unname");	//방에 참여안했을때, unname으로 할당
		
	int waitTime;
	int fileSizeIndex;
	String fileName;
	
	Socket eventSocket;					//Socket부터
	Socket cameraSocket;
	Socket voiceSocket;
	
	BufferedInputStream eventInput;
	BufferedInputStream cameraInput;
	BufferedInputStream voiceInput;
	BufferedOutputStream eventOutput;
	BufferedOutputStream cameraOutput;
	BufferedOutputStream voiceOutput;	//Buffered까지 위치정보, 카메라 프리뷰, 음성을 위해 각각 포트에 대응해 스트림을 할당했다.
	
	ClientManagement clientManagement;	//정적 배열리스트가 관리의 편의를 위해 선언
	SignalData signal;
	
	//방 자체도 String이 아닌 String 와 해당 방의 사람 수를 포함한 클래스로 선언하면 관리하기 더 쉬울듯.
	//서버에 존재하는 방이름과 각종 스트림은 정적 ArrayList로 관리한다.
	static ArrayList<ClientManagement> clientManagementList = new ArrayList<ClientManagement>();
	static ArrayList<String> joinRoomList = new ArrayList<String>();
	static int assignedClientID = 0;
	
	public ServerThread(Socket eventSocket,	Socket cameraSocket,Socket voiceSocket,	int waitTime, int fileSizeIndex) 
	{
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
			
		this.waitTime = waitTime;
		this.fileSizeIndex = fileSizeIndex;
	}
	
	//테스트 서버에 대응하는 생성자
	public ServerThread(Socket eventSocket,	Socket cameraSocket,Socket voiceSocket, String fileName, int waitTime, int fileSizeIndex)
	{
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
		
		this.fileName = fileName;		
		this.waitTime = waitTime;
		this.fileSizeIndex = fileSizeIndex;
	}
	
	//할당된 ID번호를 클라이언트에게 전송
	public boolean transClientID(int assignedClientID)
	{
		if(signal.toRequest())
		{
			if(signal.toDoRequest(signal.byteReceive))			//신호는 무조건 명령이다. 바이트신호 받는걸 요청한다. => 바이트 받아랏
			{
				IntegerToByteArray convert = new IntegerToByteArray();
				byte[] ByteArrayID = new byte[fileSizeIndex];
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
		for(String S:joinRoomList)
		{
			if(S.equals(roomName))
			{
				return false;
			}
		}
		joinRoomList.add(roomName);
		return true;		
	}
	
	//빈방 삭제하기
	public void checkingJoinRoom()
	{
		for(String S:joinRoomList)
		{
			if(S.equals(unname))	//unname은 기본이므로 삭제하면 안된다.
				continue;
			boolean flag = false;	//방에 사람이 없다면 false
			for(ClientManagement C:clientManagementList)
			{
				if(joinRoomList.equals(C.joinRoom))
				{
					flag = true;
					break;
				}					
			}
			if(!flag)
				joinRoomList.remove(S);
		}		
	}
	
	//방 출입
	public void joinRoom(String roomName)
	{
		clientManagement.joinRoom = new String(roomName);		
	}
	
	public void exitRoom(String roomName)
	{
		clientManagement.joinRoom = new String(unname);		
	}
	
	//클라이언트 연결종료
	public boolean exitServer()
	{
		try {
			eventSocket.close();
			cameraSocket.close();
			voiceSocket.close();
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
		
		signal = new SignalData(eventSocket, waitTime);	//소켓연결후 시그널과 연결
		signal.initial();
		
		assignedClientID++;
		if(!transClientID(assignedClientID))
			return ;
		
		clientManagement = new ClientManagement(assignedClientID, new String(eventSocket.getInetAddress().getHostName()), unname, signal, eventInput, cameraInput, voiceInput, eventOutput, cameraOutput, voiceOutput);
		//여기부터 서버에서 사용될 메소드 호출
		//막 들어온 클라이언트는 방에 참여하기 전까지 clientManagement는 정적배열리스트에 추가하지 않는다.
		
		test_II();
		
		
		//이 윗부분사이에 서버 코드를 작성하면 된다.
		//클라이언트와 연결종료한다.
		exitServer();		
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
