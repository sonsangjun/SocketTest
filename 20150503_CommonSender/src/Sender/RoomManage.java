package Sender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
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
	
	//클라이언트는 eventSocket, yourName, ClientID, signal만 있으면 된다.
	public RoomManage(String yourName, int clientID, Socket broadCastSocket, Socket eventSocket, Socket cameraSocket, Socket voiceSocket, List<RoomData> roomDataList, SignalData signal) {
		this.yourName = new String(yourName);
		this.clientID = clientID;
		this.broadCastSocket = broadCastSocket;
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
		
		this.roomDataList = roomDataList;
		this.signal = signal;
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
	//방 만들기
	public boolean makeRoom()
	{
		String roomName = null;
		if(signal.toAccept(signal.makeRoom))
		{
			try {
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
				roomName = inputReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				signal.toDoResponse(signal.wrong);
				return false;
			}
		}
		else
		{
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
			
			if(roomDataList.get(0).clientManage.clientID.equals(this.clientID))
				roomDataList.get(0).clientManage.clientID.remove(this.clientID);
			
			
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
		}		
		signal.toDoResponse(signal.makeRoom);
		return true;		
	}
	
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
	
	//방 출입
	public boolean joinRoom()
	{	
		String roomName = null;
		if(signal.toAccept(signal.joinRoom))
		{
			try {
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
				roomName = inputReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				signal.toDoResponse(signal.wrong);
				return false;
			}
		}
		else
		{
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
					
					System.out.println("ClientID : "+clientID+" 가 "+roomName+" 에 참여했습니다.");	
					synchronized (roomDataList) {
						if(roomDataList.get(0).clientManage.clientID.equals(this.clientID))
							roomDataList.get(0).clientManage.clientID.remove(this.clientID);	
					}
					signal.toDoResponse(signal.joinRoom);	
					return true;
				}				
			}
		}		
		System.out.println("ClientID : "+this.clientID+" 가 "+roomName+" 방을 참여하려 시도했으나 실패, 없는 방인거 같습니다.");
		signal.toDoResponse(signal.wrong);
		return false;
	}
	
	//만약 ServerThread.exitServer 메소드를 호출하려면 exitRoom를 먼저 호출해야 아래 ArrayList가 엉키지 않는다. 
	public boolean exitRoom()
	{
		String roomName = null;
		if(signal.toAccept(signal.exitRoom))
		{
			try {
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
				roomName = inputReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			signal.toDoResponse(signal.wrong);
			return false;
		}
			
		
		synchronized (roomDataList) {
			for(RoomData R:roomDataList)
			{
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
					signal.toDoResponse(signal.exitRoom);	//방을 나갔다는 확인 신호를 보냄
					System.out.println("ClientID : "+clientID+" 가 "+roomName+" 을 나갔습니다.");
					return true;					
				}
			}
		}
		System.out.println("ClientID : "+this.clientID+" 가 참여했다는 "+roomName+" 방은 없습니다.");
		signal.toDoResponse(signal.wrong);
		return false;
	}
	
	//클라이언트가 예기치 못하게 연결이 끊어진 경우 호출
	public boolean byForceExitRoom()
	{
		synchronized (roomDataList) {
			for(RoomData R:roomDataList)
			{
				if(R.roomName.equals(value.unname))
					continue;
				
				else if(R.clientManage.clientID.equals(this.clientID))
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
					signal.toDoResponse(signal.exitRoom);	//방을 나갔다는 확인 신호를 보냄
					System.out.println("ClientID : "+clientID+" 연결이 끊어져 강제로 종료했습니다.");
					return true;					
				}
			}
		}
		
		System.out.println("ClientID : "+this.clientID+" 방에 참가하지 않았습니다. 그래도 강제 종료합니다.");
		return false;
	}
	
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
		
		//룸리스트에 대한 응답을 받으면
		if(signal.toAccept(signal.roomList))
		{
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
				System.out.println(this.clientID+" 에게 방 목록을 성공적으로 전송했습니다.");
				return true;
			}			
		}
		return false;		
	}
	
	//클라이언트(방 목록을 받으므로)
	public boolean roomListReceiver(RoomDataToArray result)
	{
		ObjectInputStream objectInput = null;
		
		try {
			objectInput = new ObjectInputStream(eventSocket.getInputStream());
		} catch (IOException e) {
			System.out.println("ObjectInputStream 열기 실패");
			e.printStackTrace();
			return false;
		}
		
		
		if(signal.toDoRequest(signal.roomList))	//룸 리스트를 요청한다.를 서버가 확인했으면 true
		{
			try {
				result = (RoomDataToArray) objectInput.readObject();
			} catch (ClassNotFoundException | IOException e) {
				System.out.println("result 객체를 받는중 예외");
				e.printStackTrace();
				return false;
			}
			if(signal.toDoResponse(signal.roomList))	//방 목록을 받았다고 신호보냄
			{
				System.out.println("ClientID : "+this.clientID+" 방목록을 받았습니다.");
				return true;	
			}
		}
		System.out.println("ClientID : "+this.clientID+" 방목록 받는데 실패했습니다.");
		return false;
		
	}

	//클라이언트에서 방관련 메소드 호출전에 꼭 SocketEventUsed = true 로 설정하여 블록시키자.
	//클라이언트가 방 관련하여 요청하는 부분
	//서버단은 직접 신호 받기를 대기하므로 이런 메소드는 만들지 않는다.
	//command는 시그널, wantRoomName은 command하고 싶은 방이름, roomName은 너가 가지고 있는 방 이름의 변수(client객체의 roomName 변수)
	public boolean clientsRequest(byte[] command,String wantRoomName, String roomName, RoomDataToArray result)
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
		
		
		if(signal.toDoRequest(command))
		{
			if(signal.signalChecking(command, signal.makeRoom) || signal.signalChecking(command, signal.joinRoom) || signal.signalChecking(command, signal.exitRoom))
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
				
				if(signal.toCatchResponse(signal.makeRoom))
				{
					roomName = new String(wantRoomName);
					System.out.println("방을 만들었습니다.");
					System.out.println("방명은 "+roomName);
					this.Used = false;
					return true;
				}
				else if(signal.toCatchResponse(signal.signalStringToByte("joinRoom")))
				{
					roomName = new String(wantRoomName);
					System.out.println("방에 참가했습니다.");
					System.out.println("방명은 "+roomName);
					this.Used = false;
					return true;
				}
				else if(signal.toCatchResponse(signal.signalStringToByte("exitRoom")))
				{
					roomName = new String(value.unname);
					System.out.println("방을 나갔습니다.");
					this.Used = false;
				}
				else
				{
					if(command.equals("makeRoom"))
						System.out.println("이미 방이 있는거 같습니다.");
					else if(command.equals("joinRoom"))
						System.out.println("방에 참여할수 없습니다.");
					else if(command.equals("exitRoom"))
						System.out.println("방을 나갈 수 없습니다.");
					this.Used = false;
					return false;
				}					
			}
			else if(command.equals("roomList"))
			{
				if(roomListReceiver(result))	//this.roomListReceiver(방목록)이다.
				{
					System.out.println("방목록을 받아오지 못했습니다.");
					this.Used = false;
					return true;
				}
				
				System.out.println("방 목록을 받아왔습니다.");
				this.Used = false;
				return true;
			}			
		}
		System.out.println("서버에게 요청할 수 없습니다.");
		this.Used = false;
		return false;
	}		
}
