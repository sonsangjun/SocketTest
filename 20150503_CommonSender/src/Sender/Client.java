package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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


//안드로이드 코딩하는데 참고할 것.
//예제 클라이언트 내용
/*	기본 동작
* (방 관리 및 채팅 기준)
* 
* 클라이언트				<---->			서버(서버측 코딩은 신경쓸 필요가 없습니다.)
* toDoRequest						toDoResponse(신호 받고 signal.response신호 보냄)
* 								(방에 참가하지 않았을 경우 signal.wrong)
* 
*┌────────────────────이 부분은 roomManage.clientRequest에서 이루어 지기 때문에 사용자는 신경 쓸 필요가 없습니다.─────────────────
*│ eventOuput.write					inputReader(클라가 보내준 방목록, 채팅을 받음)
*│(방이름이나 채팅 목록)
*│ 
*│ receiveSignaltoByte(명령어 받음)		toDoResponse(명령어 보냄)
*│ (상대방의 신호를 받기 기다리는 메소드 입니다.)	(상대방에게 신호를 보내는 메소드 입니다.)
*└─────────────────────────────────────────────────────────────────────────────────────────────────────────
* 
* 해당기능을 가진 함수는 이 클래스 내에
* clientTerminate(); 
* 에 있습니다. find로 찾아서 확인해주세요.

*/

public class Client extends Thread {
	ValueCollections value = new ValueCollections();
	
	int portNum = value.portNum;
	int waitTime = value.waitTime;
	String ServerIP = value.ServerIP;
	int clientID;
	byte[] cameraByteArray = null;		//카메라 프리뷰 바이트 배열
	byte[] voiceByteArray = null;		//음성 바이트 배열
	String roomName = new String(value.unname);
	String yourName = new String(value.yourName);
	
	
	SignalData signal;
	ByteArrayTransCeiver byteArrayTransCeiver;	//데이터 스트림 송수신 역할
	ByteArrayTransCeiverRule cameraTransCeiver;	//카메라 프리뷰 데이터 송수신
	ByteArrayTransCeiverRule voiceTransCeiver;	//음성 데이터 송수신
	IntegerToByteArray integerToByteArray;
	
	Socket pushSocket;
	Socket broadCastSocket;
	Socket eventSocket;
	Socket cameraSocket;
	Socket voiceSocket;
	
	BufferedInputStream eventInput;
	BufferedOutputStream eventOutput;
		
	
	SocketBroadCastThread socketBroadCastThread = null;
	SocketPushThread socketPushThread = null;
	
	SocketPushUsed socketPushUsed = new SocketPushUsed();
	SocketBroadCastUsed socketBroadCastUsed = new SocketBroadCastUsed();	
	SocketEventUsed socketEventUsed = new SocketEventUsed();	
	SocketCameraUsed socketCameraUsed = new SocketCameraUsed();	
	SocketVoiceUsed socketVoiceUsed = new SocketVoiceUsed();
	
	RoomDataToArray myLocation;
	RoomDataToArray locationList = null;
	
	
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
			pushSocket = new Socket(ServerIP, portNum-2);
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
			//기본적인 신호를 주고 받는 소켓의 버퍼스트림을 연다. 이걸 열어야 신호를 주고 받는다.
			eventInput = new BufferedInputStream(eventSocket.getInputStream());
			eventOutput = new BufferedOutputStream(eventSocket.getOutputStream());
		} catch (IOException e1) {
			System.out.println(this.getName()+"스트림 예외");
			e1.printStackTrace();
			return;
		}
		
		//아까 연 버퍼스트림을 신호 담당 클래스에 준다.
		signal = new SignalData(eventSocket);	//소켓연결후 시그널과 연결
		signal.initial();
		
		
		//서버로부터 할당받은 아이디를 받는다. 못받으면 클라이언트 연결 종료
		if(!receiveClientID())
		{
			System.out.println("예기치 못한 오류 발생");
			System.out.println("클라이언트를 종료합니다.");
			exitClient();
			return ;			
		}
			
		
		//서버에서 날라오는 메시지 받기 시작
		socketBroadCastThread = new SocketBroadCastThread(broadCastSocket, socketBroadCastUsed);
		socketBroadCastThread.start();
		
		//데이터 입력이 필요한 카메라, 음성, 위치는 스레드를 만들어 입력받기를 기다림(Push)(맨 앞 매개변수가 false면 데이터 스트림을 받는다.)
		ByteArrayTransCeiverRule cameraTrans = new ByteArrayTransCeiverRule(false, socketCameraUsed, cameraSocket);
		ByteArrayTransCeiverRule voiceTrans = new ByteArrayTransCeiverRule(false, false, socketVoiceUsed, voiceSocket);
		socketPushThread = new SocketPushThread(pushSocket,socketPushUsed,cameraTrans,voiceTrans);
		socketPushThread.start();

		
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
			pushSocket.close();
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
					pushSocket.close();
					broadCastSocket.close();
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
	
	public void clientTerminate()
	{
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in)); //명령어를 입력하는 부분
		String valueString = null;	//명령어
		RoomDataToArray roomDataToArray = null;	//방 목록을 저장하는 변수
		roomManage = new RoomManage(" ", clientID, eventSocket, signal);	//방 관리 클래스
		
		//위치 정보 제공을 위해 초기화 (collections.synchronizedList는 스레드간 데이터 동시 접근할때 발생하는 예외를 미리 방지하기 위해서)
		List<Integer>myClientID = Collections.synchronizedList(new ArrayList<Integer>());
		List<Double> myLatitude = Collections.synchronizedList(new ArrayList<Double>());
		List<Double> myLongtitude = Collections.synchronizedList(new ArrayList<Double>());
		myClientID.add(this.clientID);
		myLatitude.add(value.basicLatitude);
		myLongtitude.add(value.BasicLongitude);
		
		myLocation = new RoomDataToArray(myClientID,myLatitude,myLongtitude);
		
		List<Integer>ClientIDList = Collections.synchronizedList(new ArrayList<Integer>());
		List<Double> LatitudeList = Collections.synchronizedList(new ArrayList<Double>());
		List<Double> LongtitudeList = Collections.synchronizedList(new ArrayList<Double>());
		
		locationList = new RoomDataToArray(ClientIDList, LatitudeList, LongtitudeList);
		
		LocationManage locationManage = new LocationManage(eventSocket, socketEventUsed, myLocation, locationList);
		locationManage.start();
		//위치 정보 제공을 위해 초기화 끝
		
		
		//클라이언트가 명령을 받고 수행하는 부분		
		while(true)
		{
			//카메라 음성 보내기 전에 클래스 인스턴스 화
			//이 클래스를 매개변수로 넘겨야 하기 때문에 무조건 선언해줘야 한다.
			this.cameraTransCeiver = new ByteArrayTransCeiverRule(true, socketCameraUsed, cameraSocket);
			this.voiceTransCeiver = new ByteArrayTransCeiverRule(true, false, socketVoiceUsed, voiceSocket);
			
			System.out.println(value.upLine);
			System.out.println("명령을 입력하세요.");
			System.out.println(signal.signalByteToString(signal.makeRoom)+" 방만들기\t"+signal.signalByteToString(signal.joinRoom)+"방참여\t "+signal.signalByteToString(signal.exitRoom)+" 방나가기\t"+signal.signalByteToString(signal.exitServer)+" 나가기");
			System.out.println(signal.signalByteToString(signal.roomList)+" 방 목록\t"+signal.signalByteToString(signal.writeYourName)+" 내이름 바꾸기\t "+signal.signalByteToString(signal.camera)+" 카메라 보내기\t"+signal.signalByteToString(signal.voice)+" 음성보내기");
			System.out.println(signal.signalByteToString(signal.locationList)+" 위치 현황");
			System.out.println(value.downLine);
			
			System.out.printf("["+this.roomName+"]["+this.clientID+"]("+this.yourName+")>");
			try {
				valueString = inputReader.readLine();	//안드로이드라면 직접 value를 입력해 스레드에게 갖다주는 식으로 변형하면 될듯.
			} catch (IOException e) {
				System.out.println("입력에러");
				e.printStackTrace();
				continue;
			}					
			
			if(eventSocket.isClosed())
			{
				System.out.println("연결을 종료합니다.");
				break;
			}
				
			
			//!!!!
			//이벤트 소켓 사용중이면 continue;
			//(아래 블록에서 이벤트 소켓의 boolean 값을 자주 바꾸는데, 이건 방 참여와 방 만들기 버튼을 동시에 누르는 행위등등, 동시 접근을 막기 위해서)
			if(socketEventUsed.socketEventUsed)
			{
				System.out.println("EventSocket이 사용중입니다.");
				continue;
			}
				
			
			//명령어 잘못 입력은 채팅으로
			//여기만 toDoRequest(talk)로 따로 씀.
			//명령어 입력창에서 talk쓰면 연결이 종료됨. talk가 signal.talk로 인식되기 때문. 안드로이드 개발때 아래 if문처럼 처리바람.
			if(signal.signalStringToByte(valueString) == null || valueString.equals(signal.signalByteToString(signal.talk)))
			{
				signal.signalReCount(true);	//장난칠경우 3회까지 봐줌
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;
				}				
				if(signal.toDoRequest(signal.talk))
				{
					if(roomManage.clientsRequest(signal.talk, valueString, null))
					{
						synchronized (socketEventUsed) {
							socketEventUsed.socketEventUsed = false;
						}						
						continue;					
					}				
				}
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = false;	
				}	
				
				if(signal.signalReCount(false))	//3회 이상 장난 칠경우 종료 시켜버림.
					System.out.println("채팅 실패");						
				else
				{
					System.out.println("채팅 실패");						
					continue;					
				}								
			}
			//명령어 잘못 입력은 채팅으로 끝				
						
			//////////////////////////////////////////////////////////////////////////////
			//테스트(무작위 위치 정보 제공)														//
			synchronized (myLocation) {													//
				myLocation.latitude.set(0, Math.random()*100);							//
				myLocation.longitude.set(0, Math.random()*100);							//
			}																			//
			//////////////////////////////////////////////////////////////////////////////
			
			//위치 현황 보기는 서버에게 요청할 필요가 없으므로 여기서 걸림.
			if(valueString.equals(signal.signalByteToString(signal.locationList)))
			{
				if(locationList == null)
					System.out.println("위치 현황이 없습니다.");
				else
				{
					System.out.println(value.upLine+"\nClientID \t latitude \t longtitude\n"+value.downLine);
					synchronized (locationList) {
						for(int i=0; i<locationList.clientID.size(); i++)
							System.out.println(locationList.clientID.get(i)+" \t "+locationList.latitude.get(i)+" \t "+locationList.longitude.get(i));
					}
					System.out.println(value.downLine);
				}
				continue;
			}
			
			//클라이언트 테스트 이므로 데이터가 없을땐, continue한다. 실제 안드로이드에서는 데이터를 바로 넣어줘야 한다.
			//////////////////////////////////////////////////////////////////////////////
			if(valueString.equals(signal.signalByteToString(signal.camera)) || valueString.equals(signal.signalByteToString(signal.voice)))
			{
				if(cameraByteArray == null || voiceByteArray == null)
				{
					System.out.println("보내려는 데이터가 없습니다.");
					continue;
				}				
			}				
			//////////////////////////////////////////////////////////////////////////////
			
			
			//명령 보내기(서버 측에서 신호를 reponse, wait, wrong 세가지로 보내기 때문에 구분을 위해)
			//요청(toDoRequest)가 아닌 toDoResponse(원래는 응답)로 신호를 보내고 receiveSignalToByteArray로 받는다.
			if(signal.toDoResponse(signal.signalStringToByte(valueString)))
			{
				byte[] tempSignal = new byte[signal.signalSize];
				tempSignal = signal.receiveSignalToByteArray();
				if(signal.signalChecking(tempSignal, signal.response))
					System.out.println("서버가 명령을 확인했습니다.");
				else if(signal.signalChecking(tempSignal, signal.wait))
				{
					System.out.println("서버가 바쁩니다. 잠시후에 다시 명령을 내려주세요.");
					continue;
				}					
				else 
				{
					System.out.println("올바르지 않은 명령입니다.");
					continue;
				}
					
			}
			else
			{
				System.out.println("연결을 종료합니다.");
				break;
				//서버에서 날라오는 메시지 받기 끝내기
				//연결이 종료되면 브로드캐스트는 죽는다. (예외 처리로 죽임 broadCastThread의 111.line)			
			}
			
			
			//방 만들기 및 참가
			//방 만들거나 참가후에 this객체의 roomName을 바꿔줘야 한다.
			if(valueString.equals(signal.signalByteToString(signal.makeRoom)) || valueString.equals(signal.signalByteToString(signal.joinRoom)))
			{
				String inputRoomName = new String(" ");
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;	
				}
				
				System.out.println("방 이름을 입력하세요.");
				try {
					inputRoomName = inputReader.readLine();
					System.out.println("입력한 방이름은 "+inputRoomName);
				} catch (IOException e) {
					System.out.println("방 입력하는 중 예외 발생");
					e.printStackTrace();
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;	
					}
					continue;
				}
				if(valueString.equals(signal.signalByteToString(signal.makeRoom)))
				{
					if(roomManage.clientsRequest(signal.makeRoom, inputRoomName, null))
					{
						synchronized (socketEventUsed) {
							socketEventUsed.socketEventUsed = false;	
						}
						this.roomName = new String(inputRoomName);
						System.out.println("만든 방이름은 "+inputRoomName);							
						continue;					
					}
				}
				else if(valueString.equals(signal.signalByteToString(signal.joinRoom)))
				{
					if(roomManage.clientsRequest(signal.joinRoom, inputRoomName, null))
					{
						synchronized (socketEventUsed) {
							socketEventUsed.socketEventUsed = false;	
						}
						this.roomName = new String(inputRoomName);
						System.out.println("참가한 방이름은 "+inputRoomName);							
						continue;					
					}
				}							
			}
			//방 만들기 및 참가 끝
			
			
			//방 나가기
			//방 나간후 this객체의 roomName을 바꿔줘야 한다.
			else if (valueString.equals(signal.signalByteToString(signal.exitRoom)))
			{
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;	
				}
				if(roomManage.clientsRequest(signal.exitRoom, this.roomName,  null))
				{
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;	
					}
					this.roomName = new String(this.value.unname);						
				}
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = false;	
				}
				continue;
			}
			//방 나가기 끝
			
			
			//방 목록 요청
			else if(valueString.equals(signal.signalByteToString(signal.roomList)))
			{
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;	
				}
				roomDataToArray = new RoomDataToArray(null, null);
				if(roomManage.clientsRequest(signal.roomList, null, roomDataToArray))
				{						
					System.out.println("방 목록을 받아왔습니다.\n"+value.upLine);
					System.out.println("방이름\t참가인원");
					for(int i=0; i<roomDataToArray.wantList.size(); i++)
						System.out.println(roomDataToArray.wantList.get(i)+"\t"+roomDataToArray.wantJoinNumber.get(i));
					System.out.println(value.downLine);
				}	
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = false;	
				}
				continue;
			}
			//방 목록 요청 끝
			
			
			//이름 바꾸기
			else if(valueString.equals(signal.signalByteToString(signal.writeYourName)))
			{
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;	
				}
				
				String inputRoomName = new String(" ");
				System.out.println("이름을 입력하세요.");
				try {
					inputRoomName = inputReader.readLine();
					System.out.println("입력한 이름은 "+inputRoomName);
				} catch (IOException e) {
					System.out.println("입력하는 중 예외 발생");
					e.printStackTrace();
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;	
					}
					continue;
				}
				
				if(roomManage.clientsRequest(signal.writeYourName, inputRoomName, null))
				{
					this.yourName = new String(inputRoomName);
					synchronized (socketEventUsed) {
						socketEventUsed.socketEventUsed = false;	
					}
				}
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = false;	
				}
				continue;
			}
			//이름 바꾸기 끝
			
			
			//카메라 프리뷰 전송
			else if(valueString.equals(signal.signalByteToString(signal.camera)))
			{
				//데이터 스트림 전송에선 event를 자동으로 컨트롤 한다.
				//프리뷰 이미지를 넣는 코드(message에 이미지 데이터 스트림이 담긴다.)
				//원래대로라면 해당 클래스에서 이미지를 바로 받아야 하지만, Top클래스에서 배열을 받아옵니다.

					
				synchronized (socketCameraUsed) {
					socketCameraUsed.message = cameraByteArray;
				}
				System.out.println("카메라 프리뷰 전송합니다. 사이즈는 :"+ socketCameraUsed.message.length+"Byte");
				
				ByteArrayTransCeiver byteArrayTransCeiver = new ByteArrayTransCeiver(cameraTransCeiver);
				if(byteArrayTransCeiver.TransCeiver() != null)
					System.out.println("카메라 프리뷰 전송 성공");			
				else
					System.out.println("카메라 프리뷰 전송 실패");
				//재승이 요청으로 날코드로 한줄 추가(대응되는 코드는 SocketPushThread의 서버측 signal.toDoResponse(signal.camera)
				SignalData cameraSignal = new SignalData(cameraSocket);
				cameraSignal.initial();
				if(cameraSignal.toCatchResponse(signal.camera))
					System.out.println("Client ID : "+this.clientID+" 서버가 모든 클라이언트에게 사진을 전송했습니다.");
			}
			else if(valueString.equals(signal.signalByteToString(signal.voice)))
			{
				synchronized (socketVoiceUsed) {
					socketVoiceUsed.message = voiceByteArray;
				}
				System.out.println("음성 전송합니다. 크기는 : "+socketVoiceUsed.message.length+"Byte");
				
				ByteArrayTransCeiver byteArrayTransCeiver = new ByteArrayTransCeiver(voiceTransCeiver);
				if(byteArrayTransCeiver.TransCeiver() != null)
					System.out.println("음성 전송 성공");
				else
					System.out.println("음성 전송 실패");
			}

			
			//종료
			else if(valueString.equals(signal.signalByteToString(signal.exitServer)))
			{
				System.out.println("클라이언트를 종료합니다.");
				exitClient();
				return ;
			}
			//종료 끝
		
			synchronized (socketEventUsed) {
				socketEventUsed.socketEventUsed = false;	
			}
		}
	}
}