/*	메소드 목록
	 * 	public ServerThread(Socket eventSocket,	Socket cameraSocket,Socket voiceSocket, String fileName, int waitTime)
	 * 		└ 생성자
	 * 	public boolean transClientID(int assigncedClientID)
	 * 		└ 클라이언트에게 서버에서 할당된 ID 전송
	 *  public boolean makeRoom(String roomName)
	 *  	└ 방만들기
	 *  public boolean checkingJoinRoom()
	 *  	└ 빈방체크하기
	 *  public boolean joinRoom(String roomName)
	 *  	└ 방 입장하기
	 *  public boolean exitRoom(String roomName)
	 *  	└ 방 나가기
	 * 	public boolean exitServer()
	 * 		└ 서버 나갈때 마무리
	 * 	public void run()
	 * 		└ 서버 동작부분(이부분을 작성하면 서버 스레드 동작에 반영된다.)
	 * 
	 * 	테스트 메소드 부분(각 로마자 숫자는 클라이언트 테스트 메소드와 매핑된다.)
	 * 	public void test_II()
	 * 		└ 서버와 클라이언트 송수신 테스트( 송수신테스트 횟수는 무한이다.)
	 * 	public void test_III()
	 * 		└ 서버와 클라이언트 방 만들기 및 데이터 스트림 전송
	 */
	
//서버에서 ClientManageList를 일일이 뒤지려면 오래걸리므로 파일송수신 스레드를 돌리기전에 뿌려야하는 명단을 서버에게 추려서 주도록하자.
//방 자체도 String이 아닌 String 와 해당 방의 사람 수를 포함한 클래스로 선언하면 관리하기 더 쉬울듯.
//서버에 존재하는 방이름과 각종 스트림은 정적 ArrayList로 관리한다.
//20150510_ArrayList 동기화 문제로 List와 collections을 이용해 해결한다. (Collections.syncronizedList)

//20150511_서버는 항상 입력대기 상태이므로 input에 대한 스레드만 있으면 된다.
//대신 데이터 스트림 전송시에만 관한 스레드 하나를 만들자.


package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerThread extends Thread {
	ValueCollections value = new ValueCollections();
	
	int clientID;
	String unname = value.unname;
	String roomName = new String(unname);
	String yourName = new String(unname);
	RoomData roomData = null;
		
	Socket pushSocket;
	Socket broadCastSocket;
	Socket eventSocket;					//Socket부터
	Socket cameraSocket;
	Socket voiceSocket;
	
	BufferedInputStream eventInput;
	BufferedOutputStream eventOutput;

	SignalData signal;
	SocketBroadCastThread socketBroadCastThread = null;
	ByteArrayTransCeiver byteArrayTransCeiver;
	RoomManage roomManage = null;
	LocationManage locationManage = null;
	
	
	SocketPushUsed socketPushUsed = new SocketPushUsed();
	SocketBroadCastUsed socketBroadCastUsed = new SocketBroadCastUsed();
	SocketEventUsed socketEventUsed = new SocketEventUsed();
	SocketCameraUsed socketCameraUsed = new SocketCameraUsed();
	SocketVoiceUsed socketVoiceUsed = new SocketVoiceUsed();
	
	static List<RoomData> roomDataList = Collections.synchronizedList(new ArrayList<RoomData>());
	static int assignedClientID = 0;
	
	public ServerThread(Socket pushSocket, Socket broadCastSocket, Socket eventSocket,	Socket cameraSocket,Socket voiceSocket) 
	{
		this.pushSocket = pushSocket;
		this.broadCastSocket = broadCastSocket;
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
	}
	
	//!!핵심!!
	//!!핵심!!
	//스레드 코어(이 부분을 작성해야 서버 스레드가 돌아간다.)
		public void run()
		{			
			if(roomDataList.size() < 1)
			{
				synchronized (roomDataList) {
					roomDataList.add(new RoomData(unname));
					roomDataList.get(0).clientManage.clientID = Collections.synchronizedList(new ArrayList<Integer>());
				}				
			}
				
			
			try {
				eventInput = new BufferedInputStream(eventSocket.getInputStream());
				eventOutput = new BufferedOutputStream(eventSocket.getOutputStream());				
			} catch (IOException e) {
				System.out.println("버퍼 예외");
				e.printStackTrace();
				try {
					System.out.println("ServerThread의 IO예외 발생으로 소켓을 닫습니다.");
					broadCastSocket.close();
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
			
			synchronized (ServerThread.class) {		//정적변수는 클래스에 하나뿐이므로 클래스에 대해 동기화 한다. (this를 쓰면 ServerThread 각각 인스턴스객체들 사이에 동기화가 안된다.)
				this.clientID = ++assignedClientID;
			}
			System.out.println(value.upLine);
			System.out.println("할당해준 Client ID 는 "+this.clientID+" 입니다.");
			System.out.println(value.downLine);
			
			if(!transClientID(this.clientID))
				return ;
			
			System.out.println(value.upLine);
			synchronized (roomDataList) {
				roomDataList.get(0).clientManage.clientID.add(this.clientID);
				System.out.println("대기실에 현재 "+roomDataList.get(0).clientManage.clientID.size()+" 있습니다.");
				int totalLogin = 0;
				roomManage = new RoomManage(yourName, this.clientID, pushSocket ,broadCastSocket, eventSocket, cameraSocket, voiceSocket, roomDataList, signal, socketBroadCastUsed, socketBroadCastThread);
				
				System.out.println("현재 방 현황 ");				
				for(RoomData R:roomDataList)
				{
					System.out.println(R.roomName+" : "+R.clientManage.clientID.size()+" 명");
					totalLogin += R.clientManage.clientID.size();
				}
									
				
				System.out.println("총 접속자 ["+totalLogin+"]");
			}
			System.out.println(value.downLine);
			//여기부터 서버에서 사용될 메소드 호출
			//막 들어온 클라이언트는 방에 참여하기 전까지 clientManagement는 정적배열리스트에 추가하지 않는다.
			//-----------------------------------------
			//(to do)
			
			standard();
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			//-----------------------------------------
			//이 윗부분사이에 서버 코드를 작성하면 된다.
			//클라이언트와 연결종료한다.
		}
	
	
	
	//할당된 ID번호를 클라이언트에게 전송
	public boolean transClientID(int assignedClientID)
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
		System.out.println("클라이언트에게 ID할당하는데 실패했습니다.");
		System.out.println(pushSocket.getInetAddress().getHostAddress()+"스레드를 종료합니다.");
		exitServer();
		return false;				
	}
	
	
	
	//클라이언트 연결종료
	//클라이언트에 대한 수많은 서버 스레드가 두 클래스에 접근시 예측이 어려움으로 동기화 작성
	//클라이언트와 연결이 끊어지면 서버스레드는 이 메소드를 호출시킨다. 서버가 망가지지 않는이상 스레드가 중간에 죽는일은 없을테니
	//이 스레드 호출전에 RoomManage.exitRoom() 부터 호출해야한다.
	public boolean exitServer()
	{
		System.out.println("Client ID : "+this.clientID+" 연결을 종료합니다.");
		try {
			broadCastSocket.close();
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
	
	//----------
	//----------
	//정식서버메소드
	public void standard()
	{
		System.out.println("Client ID : "+this.clientID+" 접속했습니다.");
			
		//서버 시작
		while(true)
		{
			byte[] receiveSignal = new byte[signal.signalSize];
			receiveSignal = signal.receiveSignalToByteArray();		//신호 받기를 대기한다.
						
			System.out.println("Client ID : "+this.clientID+" 에게 받은 신호 "+signal.signalByteToString(receiveSignal));
			
			//소켓이 닫히면 연결종료
			if(eventSocket.isClosed())
			{
				System.out.println("Client ID : "+this.clientID+" EventSocket닫힘");
				break;
			}
				
			
			//event소켓이 사용중이면 대기
			if(socketEventUsed.socketEventUsed)
			{
				if(signal.toDoResponse(signal.wait))
					continue;
				else
					break;
			}
			
			//방 목록전송
			//방에 참가 안하면 데이터나 위치 전송불가
			if(signal.signalChecking(receiveSignal, signal.roomList))
			{
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				if(roomManage.roomListSender())
				{
					System.out.println("Client ID : "+this.clientID+" 에게 방 목록을 전송했습니다.");
					continue;
				}
				else
				{
					System.out.println("Client ID : "+this.clientID+" 에게 방 목록 전송을 실패했습니다.");
					continue;
				}
			}
			//방 목록전송 끝
			
			
			//방 만들기
			else if(signal.signalChecking(receiveSignal, signal.makeRoom))
			{
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				if(roomManage.makeRoom())
				{
					System.out.println("Client ID : "+this.clientID+" 방을 만들었습니다.");
					
					//만든 방의 정보를 현재 스레드 변수에 기록함.
					synchronized (roomDataList) {
						for(RoomData R:roomDataList)
						{
							if(R.roomName.equals(value.unname))	//대기실빼고
								continue;
							if(R.clientManage.clientID.indexOf(this.clientID) > -1)
							{								
								System.out.println("방명은 "+R.roomName+" 입니다.");
								this.roomName = new String(R.roomName);		
								this.roomData = R;
								break;
							}
						}						
					}//여기까지 synchronized(roomDataList) 블록
					continue;
				}
				else
				{
					System.out.println("Client ID : "+this.clientID+" 방 만들기를 실패했습니다.");
					continue;
				}
			}
			//방 만들기 끝
			
			
			//방 참여하기
			else if(signal.signalChecking(receiveSignal, signal.joinRoom))
			{
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				if(roomManage.joinRoom())
				{					
					//참여한 방의 정보를 현재 스레드 변수에 기록함.
					synchronized (roomDataList) {
						for(RoomData R:roomDataList)
						{
							if(R.roomName.equals(value.unname))	//대기실빼고
								continue;
							if(R.clientManage.clientID.indexOf(this.clientID) > -1)
							{
								System.out.println("방명은 "+R.roomName+" 입니다.");
								this.roomName = new String(R.roomName);
								this.roomData = R;
								break;								
							}
						}						
					}//여기까지 synchronized(roomDataList) 블록
					continue;
					
				}
				else
				{
					System.out.println("Client ID : "+this.clientID+" 방에 들어가지 못했습니다.");
					continue;
				}
			}
			//방 참여하기 끝
			
			
			//방 나가기
			else if(signal.signalChecking(receiveSignal, signal.exitRoom))
			{
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				if(roomManage.exitRoom())
				{
					System.out.println("Client ID : "+this.clientID+" 방을 나갔습니다.");	
					this.roomData = null;
					if(roomManage.delEmptyRoom())
						System.out.println("Client ID : "+this.clientID+" 빈방을 정리했습니다.");
					
					//exitRoom 이후에 join이나 make할것 이므로 번거롭지 않게 미리 만들어둔다.
					synchronized (roomDataList) {
						roomManage = new RoomManage(yourName, this.clientID, pushSocket,broadCastSocket, eventSocket, cameraSocket, voiceSocket, roomDataList, signal, socketBroadCastUsed, socketBroadCastThread);
					}
					//roomManage 선언 끝
					
					continue;
				}
				else
				{
					System.out.println("Client ID : "+this.clientID+" 방을 못 나갔습니다.(?)");
					if(roomManage.delEmptyRoom())
						System.out.println("Client ID : "+"빈방을 정리했습니다.");
					continue;
				}
			}	
			//방 나가기 끝
			
			
			//이름 바꾸기
			else if(signal.signalChecking(receiveSignal, signal.writeYourName))
			{
				String tempName = null;
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				tempName = roomManage.writeYourName();
				if(tempName != null)
				{
					this.yourName = new String(tempName);
					System.out.println("Client ID : "+this.clientID+" 의 이름은 이제 "+this.yourName+" 입니다.");
					continue;
				}
				System.out.println("Client ID : "+this.clientID+" 이름 바꾸는데 문제 생김.");
				continue;
			}
			//이름 바꾸기 끝
			
			
			//연결종료 요청
			else if(signal.signalChecking(receiveSignal, signal.exitServer))
			{
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))
					break;
				//신호 응답 끝
				break;
			}				
			//연결종료 요청 끝
			
			
			//대기실에 있는경우 continue;
			if(roomName.equals(unname))
			{
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				//방에 참가하지 않았는데 올바른 명령을 실행하지 않으므로 wrong을 보낸다.
				if(!signal.toDoResponse(signal.wrong))	break;
				//신호 응답 끝
				continue;
			}					
			//대기실에 있는경우 continue; 끝
			
			
			//채팅
			if(signal.signalChecking(receiveSignal, signal.talk))
			{
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				String temp = roomManage.talk();	//서버도 보기 위해 반환값을 String으로 한다.
				if(temp != null)
				{
					System.out.println("Client ID : "+this.clientID+" : "+temp+"(채팅)");
					continue;
				}
				else
				{
					System.out.println("Client ID : "+this.clientID+" 채팅 실패");
					continue;
				}			
			}			
			//채팅 끝
			
			
			//위치는 해당스레드에서 socketEventUsed를 직접수정한다.
			//위치는 카메라,음성 송수신 방식과 다르다. 위치는 클라이언트가 일정 주기마다 요청한다.
			//또한, 일정 주기마다 클라이언트가 위치 정보를 보낸다.
			//위치 받기
			if(signal.signalChecking(receiveSignal, signal.location))
			{
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;
				}
				//만약 방을 안만들었다면 wrong을 보낸다. 서버측 LocationManage에서 null예외 발생해서.
				if(roomData == null)
				{
					signal.toDoResponse(signal.wrong);
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;
					}
					continue;
				}
				
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝			
				
				locationManage = new LocationManage(eventSocket, roomData);
				if(locationManage.serverReceiver() != null)
				{
					int index = roomData.clientManage.clientID.indexOf(this.clientID);
					System.out.println("Client ID : "+this.clientID+"의 현재 위치는 "+roomData.clientManage.latitude.get(index)+" , "+roomData.clientManage.longitude.get(index)+" 입니다.");
				}
				else
					System.out.println("Client ID : "+this.clientID+" 의 현재 위치는 알 수 없습니다.");
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = false;
				}
				continue;
			}
			//위치 받기 끝
			
			
			//각각 정보를 클라에게 받아 다른 클라에게 뿌리는 역할
			//위치목록 보내기
			if(signal.signalChecking(receiveSignal, signal.locationList))
			{
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;
				}
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				//만약 방을 안만들었다면 wrong을 보낸다. 서버측 LocationManage에서 null예외 발생해서.
				if(roomData == null)
				{
					signal.toDoResponse(signal.wrong);
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;
					}
					continue;
				}
					
				
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝	
				
				locationManage = new LocationManage(eventSocket, roomData);
				if(locationManage.serverListSender())
					System.out.println("Client ID : "+this.clientID+" 위치 목록을 보냈습니다.");
				else
					System.out.println("Client ID : "+this.clientID+" 위치 목록 전송 실패했습니다.");
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = false;
				}
				continue;				
			}
			//위치 목록 보내기 끝
			
			
			//카메라 시작
			else if(signal.signalChecking(receiveSignal, signal.camera))
			{
				//카메라가 이미 사용중이라면 wrong 반환
				if(socketCameraUsed.socketCameraUsed)
				{		
					signal.toDoResponse(signal.wrong);
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;
					}
					continue;
				}
				
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				synchronized (socketCameraUsed) {				//서버에서 직접 락을 건다.	false는 따로 넣지않는다. 
					socketCameraUsed.socketCameraUsed = true;	//나중에 스레드에서 락을 알아서 풀기 때문에 상관없다.					
				}			
				
				synchronized (socketBroadCastUsed) {
					socketBroadCastUsed.message= new String("Client ID : "+this.clientID+" 카메라 이미지 전송합니다.");
				}	
				
				//여기에 선언한다. 
				ByteArrayTransCeiverRule byteArrayTransCeiverRule;
				byteArrayTransCeiverRule = new ByteArrayTransCeiverRule(clientID, socketCameraUsed, socketPushUsed, roomData);
				
				//데이터 스트림 전송전에 초기화 시켜야 한다.								
				SocketPushThread thread = new SocketPushThread(true, byteArrayTransCeiverRule);
				thread.start();
				continue;
			}
			//카메라 끝
			
			
			//음성 시작
			else if(signal.signalChecking(receiveSignal, signal.voice))
			{
				//음성이 사용중이면 wrong 반환
				if(socketVoiceUsed.socketVoiceUsed)
				{
					signal.toDoResponse(signal.wrong);
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;
					}
					continue;
				}
				
				//신호를 받았다는 응답을 보낸다. 아래 메소드가 false를 반환하면 클라와 연결이 끊긴것이다.
				if(!signal.toDoResponse(signal.response))	break;
				//신호 응답 끝
				
				synchronized (socketVoiceUsed) {			//서버에서 직접 락을 건다.	false는 따로 넣지않는다. 					
					socketVoiceUsed.socketVoiceUsed = true;	//나중에 스레드에서 락을 알아서 풀기 때문에 상관없다.						
				}
				
				synchronized (socketBroadCastUsed) {
					socketBroadCastUsed.message= new String("Client ID : "+this.clientID+" 무전 전송합니다.");
				}	
				
				//여기에 선언한다. 
				ByteArrayTransCeiverRule byteArrayTransCeiverRule;
				byteArrayTransCeiverRule = new ByteArrayTransCeiverRule(clientID, false, socketVoiceUsed, socketPushUsed, roomData);
				
				//데이터 스트림 전송전에 초기화 시켜야 한다.								
				SocketPushThread thread = new SocketPushThread(true, byteArrayTransCeiverRule);
				thread.start();
				continue;
				
			}		
			//음성끝
			//각각 정보를 클라에게 받아 다른 클라에게 뿌리는 역할 끝
			
			
			//클라이언트와 연결이 끊어진 경우
			else	
			{
				synchronized (socketBroadCastUsed) {
					socketBroadCastUsed.message= new String("Client ID : "+this.clientID+" 연결이 끊어졌습니다.");
				}				
				break;
			}
		}
		//서버 끝
		
		
		//서버 종료
		System.out.println("Client ID : "+this.clientID+" 대한 연결이 끊어졌습니다.");
		roomManage.byForceExitRoom();
		roomManage.delEmptyRoom();
		exitServer();
		System.out.println("Client ID : "+this.clientID+" 대한 연결을 마칩니다.");
		//서버 종료 끝
	}
	
	
	
	
	
	
	
	
	
	
	//!!Test 메소드!!
	//!!테스트 메소드!!	
	public void test_II()
	{
		while(true)
		{
			if(signal.toAccept(signal.request))
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
