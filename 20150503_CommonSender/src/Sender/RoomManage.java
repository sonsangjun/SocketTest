package Sender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.lang.Thread.State;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

//방 및 사용자를 관리한다.
//서버에 직접 선언하는 것보단, 클래스로 관리하여 인스턴스 하는게 더 나아보여서 만들었다.
//이 클래스 안의 메소드는 ServerThread에서 동작한다.
public class RoomManage {
	ValueCollections value = new ValueCollections();
	boolean Used = false;		//클라이언트 단의 버튼 클릭에 의한 중복 호출을 막기위해
	String yourName;			
	int clientID;				
	List<RoomData> roomDataList;
	SignalData signal;

	Socket broadCastSocket;
	Socket eventSocket;
	Socket cameraSocket;
	Socket voiceSocket;
	
	SocketBroadCastUsed socketBroadCastUsed;
	SocketBroadCastThread socketBroadCastThread;
	
	//서버용 생성자
	public RoomManage(String yourName, int clientID, Socket broadCastSocket, Socket eventSocket, Socket cameraSocket, Socket voiceSocket, List<RoomData> roomDataList, SignalData signal, SocketBroadCastUsed socketBroadCastUsed, SocketBroadCastThread socketBroadCastThread) {
		this.yourName = new String(yourName);
		this.clientID = clientID;
		this.broadCastSocket = broadCastSocket;
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
		
		this.roomDataList = roomDataList;
		this.signal = signal;
		
		this.socketBroadCastUsed = socketBroadCastUsed;
		this.socketBroadCastThread = socketBroadCastThread;
	}
	
	//클라이언트용 생성자
	public RoomManage(String yourName, int clientID, Socket eventSocket,SignalData signal)
	{
		this.yourName = new String(yourName);
		this.clientID = clientID;
		this.eventSocket = eventSocket;
		this.signal = signal;		
	}

	//
	//서버측, 서버는 여러 명령에 대해 응답을 기다리므로 메소드 안에 toResponse를 내장하지 않는다.
	//이름 입력 ( 서버에도 이름을 남겨야 하기 때문에 String으로 반환한다.)
	public String writeYourName()
	{
		String tempName = null;
		try {
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
			tempName = inputReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			signal.toDoResponse(signal.wrong);
			return null;
		}	
		if(tempName == null)
		{
			signal.toDoResponse(signal.wrong);
			return null;
		}
		synchronized (roomDataList) {
			if(roomDataList.get(0).clientManage.clientID.indexOf(this.clientID) > -1)	//대기실에 이름이 있을경우 서버에게 바로 이름전송
			{
				signal.toDoResponse(signal.writeYourName);
				this.yourName = new String(tempName);
				return tempName;			
			}			
		}
		signal.toDoResponse(signal.wrong);	//대기실이 아니면 이름을 바꿀 수 없다.
		return null;			
	}
	//이름 입력 끝
	
	
	//방 만들기
	public boolean makeRoom()
	{
		String roomName = null;

		try {
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
			roomName = inputReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			signal.toDoResponse(signal.wrong);
			return false;
		}	
		
		synchronized (roomDataList) {
			if(roomName.equals(roomDataList.get(0).unname))		//unname은 쓸수없다.
			{
				signal.toDoResponse(signal.wrong);
				return false;						
			}
			for(RoomData R:roomDataList)
			{
				if(R.roomName.equals(value.unname))	//대기실은 패스(unname은 대기실)
					continue;
				
				else if(R.clientManage.clientID.indexOf(this.clientID) > -1)	//방에 이미 참여했으면 false
				{
					signal.toDoResponse(signal.wrong);
					return false;
				}
				else if(R.roomName.equals(roomName))	//방명이 같으면 false
				{
					signal.toDoResponse(signal.wrong);
					return false;
				}
			}
			
			
			System.out.println(roomName+" 이 추가되었습니다.");
			roomDataList.add(new RoomData(roomName));
			int index = roomDataList.size()-1;
			
			//대기실에 clientID가 있으면 삭제
			//항상 느끼는 거지만, 오류는 내가 만든다. equal을 잘못써서 오류찾는데 2시간을 헤매다니.
			int indexClient = roomDataList.get(0).clientManage.clientID.indexOf(this.clientID);
			if(roomDataList.get(0).clientManage.clientID.get(indexClient).equals(this.clientID))
			{
				System.out.println("대기실의 아이디는 삭제합니다.");
				roomDataList.get(0).clientManage.clientID.remove(indexClient);
			}		
			
			//BroadCastSocket은 아웃풋만 필요하고, BroadCastThread 특성상 이렇게 처리하는게 편하다.
			try {
				roomDataList.get(index).clientManage.broadCast.add(new BufferedWriter(new OutputStreamWriter(broadCastSocket.getOutputStream())));
			} catch (IOException e) {
				e.printStackTrace();				
				roomDataList.remove(roomDataList.size()-1);
				signal.toDoResponse(signal.wrong);		//방 만드는중 예외 발생함을 알림.
				return false;
			}
			roomDataList.get(index).clientManage.yourName.add(yourName);
			roomDataList.get(index).clientManage.clientID.add(this.clientID);
			roomDataList.get(index).clientManage.eventSocket.add(eventSocket);
			roomDataList.get(index).clientManage.cameraSocket.add(cameraSocket);
			roomDataList.get(index).clientManage.voiceSocket.add(voiceSocket);
			roomDataList.get(index).clientManage.latitude.add(value.basicLatitude);
			roomDataList.get(index).clientManage.longitude.add(value.BasicLongitude);
			
			//BroadCastThread 생성(makeRoom)
			synchronized (socketBroadCastUsed) {
				socketBroadCastUsed.broadCastKill = true;
			}
			while(true)
			{
				if((socketBroadCastThread == null) || (socketBroadCastThread.getState() == State.TERMINATED) )
				{
					socketBroadCastThread = new SocketBroadCastThread(roomDataList.get(index), socketBroadCastUsed);
					socketBroadCastThread.start();					
					synchronized (socketBroadCastUsed) {
						socketBroadCastUsed.init();
					}
					break;
				}
			}
			//BroadCastThread 생성끝(makeRoom)
		}

		signal.toDoResponse(signal.makeRoom);
		return true;		
	}
	//방 만들기 끝
	
	//빈방 삭제하기
	public boolean delEmptyRoom()
	{
		synchronized (roomDataList){
			for(RoomData R:roomDataList)
			{
				if(R.roomName.equals(value.unname))	//unname은 대기실이다.
					continue;
				
				if(R.clientManage.clientID.size() < 1)
				{					 	
					roomDataList.remove(R);
					return true;					
				}
			}			
		}			
		return false;
	}
	//빈방 삭제하기 끝
	
	//방 들어가기
	public boolean joinRoom()
	{	
		String roomName = null;
		try {
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
			roomName = inputReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			signal.toDoResponse(signal.wrong);
			return false;
		}

		
		synchronized (roomDataList) {
			
			if(roomName.equals(roomDataList.get(0).unname))		//unname에 참여할 수 없다.
			{
				signal.toDoResponse(signal.wrong);
				return false;						
			}
			for(RoomData R:roomDataList)
			{
				if(R.roomName.equals(value.unname))
					continue;
				else if(R.clientManage.clientID.indexOf(this.clientID) > -1)	//방에 이미 참여했으면 false
				{
					signal.toDoResponse(signal.wrong);
					return false;
				}
			}
				
			for(RoomData R:roomDataList)
			{
				if(R.roomName.equals(roomName))	//방이 존재하는 경우
				{
					//대기실에 clientID가 있으면 삭제
					int indexClient = roomDataList.get(0).clientManage.clientID.indexOf(this.clientID);
					if(roomDataList.get(0).clientManage.clientID.get(indexClient).equals(this.clientID))
					{
						System.out.println("대기실의 아이디는 삭제합니다.");
						roomDataList.get(0).clientManage.clientID.remove(indexClient);
					}		
					
					int tempSize = R.clientManage.broadCast.size();
					//BroadCastSocket은 아웃풋만 필요하고, BroadCastThread 특성상 이렇게 처리하는게 편하다.
					try {
						R.clientManage.broadCast.add(new BufferedWriter(new OutputStreamWriter(broadCastSocket.getOutputStream())));
					} catch (IOException e) {
						e.printStackTrace();
						if( tempSize != R.clientManage.broadCast.size())	//올바르지 않은 브로드캐스트소켓이 들어갔을경우
							R.clientManage.broadCast.remove(R.clientManage.broadCast.size()-1);
						signal.toDoResponse(signal.wrong);		//방 출입에 예외가 발생함을 알림.
						return false;
					}
					R.clientManage.yourName.add(yourName);
					R.clientManage.clientID.add(this.clientID);
					R.clientManage.eventSocket.add(eventSocket);
					R.clientManage.cameraSocket.add(cameraSocket);
					R.clientManage.voiceSocket.add(voiceSocket);
					R.clientManage.latitude.add(value.basicLatitude);
					R.clientManage.longitude.add(value.BasicLongitude);
					
					System.out.println("Client ID : "+clientID+" 가 "+roomName+" 에 참여했습니다.");	
					
					//대기실에 대기하는 ClientID 삭제
					synchronized (roomDataList) {
						if(roomDataList.get(0).clientManage.clientID.equals(this.clientID))
							roomDataList.get(0).clientManage.clientID.remove(this.clientID);	
					}
					
					//BroadCastThread 생성(joinRoom)
					synchronized (socketBroadCastUsed) {
						socketBroadCastUsed.broadCastKill = true;
					}
					while(true)
					{
						if( (socketBroadCastThread == null) || (socketBroadCastThread.getState() == State.TERMINATED) )
						{
							socketBroadCastThread = new SocketBroadCastThread(R, socketBroadCastUsed);
							socketBroadCastThread.start();
							synchronized (socketBroadCastUsed) {
								socketBroadCastUsed.init();
								socketBroadCastUsed.message = new String("Client ID : "+this.clientID+" 방에 참여했습니다.");								
							}
							break;
						}
					}
					//BroadCastThread 생성끝(joinRoom)
					
					signal.toDoResponse(signal.joinRoom);	
					return true;
				}				
			}
		}		
		System.out.println("Client ID : "+this.clientID+" 가 "+roomName+" 방을 참여하려 시도했으나 실패, 없는 방인거 같습니다.");
		signal.toDoResponse(signal.wrong);
		return false;
	}
	//방 들어가기 끝
	
	//방 나가기
	//만약 ServerThread.exitServer 메소드를 호출하려면 exitRoom를 먼저 호출해야 아래 ArrayList가 엉키지 않는다. 
	public boolean exitRoom()
	{
		String roomName = null;
		try {
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
			roomName = inputReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
			
		System.out.println("Client ID : "+this.clientID+" "+roomName+" 에 대한 exitRoom 요청");
		synchronized (roomDataList) {
			for(RoomData R:roomDataList)
			{
				if(R.roomName.equals(value.unname))	//대기실에 있으면 나갈이유가 없다.
					continue;
				if(R.roomName.equals(roomName))
				{
					int index = R.clientManage.clientID.indexOf(this.clientID);
					R.clientManage.yourName.remove(index);
					R.clientManage.clientID.remove(index);
					R.clientManage.broadCast.remove(index);
					R.clientManage.eventSocket.remove(index);
					R.clientManage.cameraSocket.remove(index);
					R.clientManage.voiceSocket.remove(index);
					R.clientManage.latitude.remove(index);
					R.clientManage.longitude.remove(index);
			
					//BroadCastThread 제거(exitRoom)
					synchronized (socketBroadCastUsed) {
						socketBroadCastUsed.message = new String("Client ID : "+this.clientID+" 방을 나갔습니다.");	
					}
					
					try {
						Thread.sleep(value.waitTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return false;
					}
					
					synchronized (socketBroadCastUsed) {
						socketBroadCastUsed.broadCastKill = true;
					}
					while(true)
					{
						if( socketBroadCastThread == null || socketBroadCastThread.getState() == State.TERMINATED )
						{
							socketBroadCastThread = null;
							synchronized (socketBroadCastUsed) {
								socketBroadCastUsed.init();
							}								
							break;
						}															
					}						
				
					//BroadCastThread 제거(exitRoom)					
					signal.toDoResponse(signal.exitRoom);	//방을 나갔다는 확인 신호를 보냄
					roomDataList.get(0).clientManage.clientID.add(this.clientID);	//방을 나간 클라이언트는 대기실에 입성.					
					System.out.println("Client ID : "+clientID+" 방 : "+roomName+" 나갔습니다.");
					return true;					
				}
			}
		}		
		signal.toDoResponse(signal.wrong);
		return false;
	}
	//방 나가기 끝
	
	//예기치 못한 연결 해제
	//클라이언트가 예기치 못하게 연결이 끊어진 경우 호출
	public boolean byForceExitRoom()
	{
		synchronized (roomDataList) {
			for(RoomData R:roomDataList)
			{				
				int indexClient = R.clientManage.clientID.indexOf(this.clientID);
				if(indexClient < 0)	//해당 방에 대상 클라이언트가 없다.
					continue;
			
				if(R.roomName.equals(value.unname))
				{
					R.clientManage.clientID.remove(indexClient);
					System.out.println("Client ID : "+clientID+" 연결이 끊어져 종료했습니다.(대기실)");
					return true;
											
				}
				int index = R.clientManage.clientID.indexOf(this.clientID);
				R.clientManage.yourName.remove(index);
				R.clientManage.clientID.remove(index);
				R.clientManage.broadCast.remove(index);
				R.clientManage.eventSocket.remove(index);
				R.clientManage.cameraSocket.remove(index);
				R.clientManage.voiceSocket.remove(index);
				R.clientManage.latitude.remove(index);
				R.clientManage.longitude.remove(index);
				signal.toDoResponse(signal.exitRoom);	//방을 나갔다는 확인 신호를 보냄
				
				
				//BroadCastThread 제거(byForceExitRoom)
				synchronized (socketBroadCastUsed) {
					socketBroadCastUsed.message = new String("Client ID : "+this.clientID+" 연결이 끊겼습니다.");	
				}				
				try {
					Thread.sleep(value.waitTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				
				synchronized (socketBroadCastUsed) {
					socketBroadCastUsed.broadCastKill = true;
				}
				while(true)
				{
					if( socketBroadCastThread == null || socketBroadCastThread.getState() == State.TERMINATED )
					{
						socketBroadCastThread = null;
						synchronized (socketBroadCastUsed) {
							socketBroadCastUsed.init();
						}								
						break;
					}															
				}				
				//BroadCastThread 제거(byForceExitRoom)
				
				System.out.println("Client ID : "+clientID+" 연결이 끊어져 종료했습니다.");
				return true;					
			}
		}
		return false;
	}
	//예기지 못한 연결 해제 끝
	
	//방 목록 보내는 부분
	//클라이언트 단에도 이것과 대응되도록 메소드 선언한다.
	public boolean roomListSender()
	{
		ObjectOutputStream objectOutput = null;
		RoomDataToArray result;
		ArrayList<String> wantList = new ArrayList<>();
		ArrayList<Integer> wantJoinNumber = new ArrayList<>();
		
		//방 목록을 roomList에 집어넣는다.
		for(RoomData R: roomDataList)
		{
			if(R.roomName.equals(value.unname))	//대기실은 방목록에 추가하지 않는다.
				continue;
			wantList.add(R.roomName);
			wantJoinNumber.add(R.clientManage.clientID.size());
		}
		
		//뽑아낸 방목록과 방에 참가한 인원을 result 객체에 집어넣는다.
		result = new RoomDataToArray(wantList, wantJoinNumber);
		
		//객체 전송을 위해 object아웃풋을 연다.
		try {
			objectOutput = new ObjectOutputStream(eventSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("roomListing()의 ObjectOutput에서 예외");
			e.printStackTrace();
			return false;
		}
		
		//클라이언트에게 방 목록을 보낸다.
		try {
			objectOutput.writeObject(result);
			objectOutput.flush();
		} catch (IOException e) {
			System.out.println("roomList 객체 전송중 예외");
			e.printStackTrace();
			return false;
		}
		if(signal.toCatchResponse(signal.roomList))	//원격측에서 받았는지 확인(확인은 안함)
		{
			System.out.println("Client ID : " +this.clientID+" 에게 방 목록을 성공적으로 전송했습니다.");
			return true;
		}			

		return false;		
	}
	//클라이언트에게 방 목록 보내는 부분 끝
	
	//채팅
	public String talk()
	{
		String talkString = null;

		try {
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
			talkString = inputReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			signal.toDoResponse(signal.wrong);
			return null;
		}
		
		if(talkString != null)
		{
			synchronized (socketBroadCastUsed) {
				socketBroadCastUsed.message = new String("Client ID : "+this.clientID+" : <"+this.yourName+">"+talkString);
			}
			signal.toDoResponse(signal.talk);
			return talkString;
		}
		else
		{
			signal.toDoResponse(signal.wrong);
			return null;
		}
			
				
	}
	
	//채팅 끝
	//서버측 끝
	//--------------------
	
	
	
	
	
	//--------------------
	//클라이언트 측
	//클라이언트(방 목록을 받으므로)
	//객체를 받으므로 객체가 사라지기 전에  객체리턴
	public boolean roomListReceiver(RoomDataToArray result)
	{
		ObjectInputStream objectInput = null;
		RoomDataToArray temp = null;
		
		try {
			objectInput = new ObjectInputStream(eventSocket.getInputStream());
		} catch (IOException e) {
			System.out.println("ObjectInputStream 열기 실패");
			e.printStackTrace();
			return false;
		}
		

		try {
			temp = (RoomDataToArray) objectInput.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("result 객체를 받는중 예외");
			e.printStackTrace();
			return false;
		}	
		
		if(signal.toDoResponse(signal.roomList))	//방 목록을 받았다고
		{
			if(temp == null)
			{
				System.out.println("방 목록이 없습니다.");
				return false;
			}
			result.wantList = temp.wantList;				//그냥 객체를 대입하면 해당 클래스 메소드 호출 끝나면 사라져 버려서...
			result.wantJoinNumber = temp.wantJoinNumber;	//직접 클래스의 필드에 대입한다.
			return true;
		}
		else
		{
			System.out.println("방 목록을 받아오지 못했습니다.");
			return false;
		}
				
	}
	//클라이언트 끝(방 목록을 받으므로)

	//클라이언트 요청
	//클라이언트에서 방관련 메소드 호출전에 꼭 SocketEventUsed = true 로 설정하여 블록시키자.
	//클라이언트가 방 관련하여 요청하는 부분
	//서버단은 직접 신호 받기를 대기하므로 이런 메소드는 만들지 않는다.
	//command는 시그널, wantRoomName은 command하고 싶은 방이름, roomName은 너가 가지고 있는 방 이름의 변수(client객체의 roomName 변수)
	public boolean clientsRequest(byte[] command,String wantRoomName, RoomDataToArray result)
	{
		BufferedWriter eventOutput;
		try {
			eventOutput = new BufferedWriter(new OutputStreamWriter(eventSocket.getOutputStream()));
		} catch (IOException e1) {
			System.out.println("clientRequest에서 예외발생");
			e1.printStackTrace();
			this.Used = false;
			return false;
		}
		
		if(!this.Used)
			this.Used = true;
		
		//방 관리
		if(signal.signalChecking(command, signal.makeRoom) || signal.signalChecking(command, signal.joinRoom) || signal.signalChecking(command, signal.exitRoom))
		{
			//방 이름 서버에게 보냄
			try {
				Thread.sleep(value.waitTime);	//서버측 스레드의 버퍼 생성시간이 늦어질수도 있으므로.
				eventOutput.write(wantRoomName);
				eventOutput.newLine();
				eventOutput.flush();
			} catch (IOException | InterruptedException e) {
				System.out.println(command+" 도중 예외 발생");
				e.printStackTrace();
				this.Used = false;
				return false;
			}
			byte[] receiveSignal = signal.receiveSignalToByteArray();	//서버에게 잘 받았다는 연락을 받아야 함
			
			//명령에 따른 수행
			if(signal.signalChecking(receiveSignal, signal.makeRoom))
			{
				System.out.println("방을 만들었습니다.");
				this.Used = false;
				return true;
			}
			else if(signal.signalChecking(receiveSignal, signal.joinRoom))
			{					
				System.out.println("방에 참가했습니다.");
				this.Used = false;
				return true;
			}
			else if(signal.signalChecking(receiveSignal, signal.exitRoom))
			{
				System.out.println("방을 나갔습니다.");
				this.Used = false;
				return true;
			}
			else
			{
				System.out.println("서버에서 "+signal.signalByteToString(command)+"명령을 수행할 수 없습니다.");
				this.Used = false;
				return false;
			}
			//명령에 따른 수행 끝
		}
		//방 관리 끝
				
		//채팅
		else if(signal.signalChecking(command, signal.talk))
		{
			//채팅을 서버에게 보냄
			try {
				Thread.sleep(value.waitTime);	//서버측 스레드의 버퍼 생성시간이 늦어질수도 있으므로.
				eventOutput.write(wantRoomName);
				eventOutput.newLine();
				eventOutput.flush();
			} catch (IOException | InterruptedException e) {
				System.out.println(command+" 도중 예외 발생");
				e.printStackTrace();
				this.Used = false;
				return false;
			}
			byte[] receiveSignal = signal.receiveSignalToByteArray();	//서버에게 잘 받았다는 연락을 받아야 함
			
			if(signal.signalChecking(receiveSignal, signal.talk))
			{
				this.Used = false;
				return true;				
			}
			else
			{
				System.out.println("서버에서 "+signal.signalByteToString(command)+"명령을 수행할 수 없습니다.");
				this.Used = false;
				return false;
			}
			

		}
		//채팅 끝
		
		
		//이름 바꾸기
		else if(signal.signalChecking(command, signal.writeYourName))
		{
			try {
				Thread.sleep(value.waitTime);	//서버측 스레드의 버퍼 생성시간이 늦어질수도 있으므로.
				eventOutput.write(wantRoomName);
				eventOutput.newLine();
				eventOutput.flush();
			} catch (IOException | InterruptedException e) {
				System.out.println(command+" 도중 예외 발생");
				e.printStackTrace();
				this.Used = false;
				return false;
			}
			
			byte[] receiveSignal = signal.receiveSignalToByteArray();	//서버에게 잘 받았다는 연락을 받아야 함
			
			if(signal.signalChecking(receiveSignal, signal.writeYourName))
			{
				System.out.println("당신의 이름은 이제 "+wantRoomName+" 입니다.");
				this.Used = false;
				return true;
			}
			else
			{
				this.Used = false;
				return false;
			}	
		}	
		//이름 바꾸기 끝
		
		//방 목록 받기
		else if(signal.signalChecking(command, signal.roomList))
		{
			if(!roomListReceiver(result))
			{
				this.Used = false;
				return false;
			}
			this.Used = false;
			return true;
		}			
		//방 목록 받기 끝
		
		System.out.println("서버에게 요청할 수 없습니다.");
		this.Used = false;
		return false;
	}		
	//클라이언트 요청 끝
}
