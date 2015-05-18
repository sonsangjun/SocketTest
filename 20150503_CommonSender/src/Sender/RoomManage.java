package Sender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//방 및 사용자를 관리한다.
//서버에 직접 선언하는 것보단, 클래스로 관리하여 인스턴스 하는게 더 나아보여서 만들었다.
//이 클래스 안의 메소드는 ServerThread에서 동작한다.
public class RoomManage {
	ValueCollections value = new ValueCollections();
	boolean Used = false;		//클라이언트 단의 버튼 클릭에 의한 중복 호출을 막기위해
	String yourName;			
	int clientID;				
	List<RoomData> roomData;
	SignalData signal;
	
	Socket broadCastSocket;
	Socket eventSocket;
	Socket cameraSocket;
	Socket voiceSocket;
	
	//클라이언트는 eventSocket, yourName, ClientID, signal만 있으면 된다. (나머지는 모두 null)
	public RoomManage(String yourName, int clientID, Socket broadCastSocket, Socket eventSocket, Socket cameraSocket, Socket voiceSocket, List<RoomData> roomData, SignalData signal) {
		this.yourName = new String(yourName);
		this.clientID = clientID;
		this.broadCastSocket = broadCastSocket;
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;
		
		this.roomData = roomData;
		this.signal = signal;
	}

	//
	//서버측, 서버는 여러 명령에 대해 응답을 기다리므로 메소드 안에 toResponse를 내장하지 않는다.
	//방 만들기
	public boolean makeRoom(String roomName)
	{
		synchronized (roomData) {
			if(roomName.equals(roomData.get(0).unname))		//unname은 쓸수없다.
				return false;						
			for(RoomData R:roomData)
			{
				if(R.clientManage.clientID.indexOf(this.clientID) > -1)	//방에 이미 참여했으면 false
					return false;
				else if(R.roomName.equals(roomName))	//방명이 같으면 false
					return false;
			}
			
			System.out.println(roomName+" 이 추가되었습니다.");
			roomData.add(new RoomData(roomName));
			int index = roomData.size()-1;
			roomData.get(index).clientManage.yourName.add(yourName);
			roomData.get(index).clientManage.clientID.add(this.clientID);
			roomData.get(index).clientManage.eventSocket.add(eventSocket);
			roomData.get(index).clientManage.cameraSocket.add(cameraSocket);
			roomData.get(index).clientManage.voiceSocket.add(voiceSocket);
			
			//BroadCastSocket은 아웃풋만 필요하고, BroadCastThread 특성상 이렇게 처리하는게 편하다.
			try {
				roomData.get(index).clientManage.broadCast.add(new BufferedWriter(new OutputStreamWriter(broadCastSocket.getOutputStream())));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
		return true;		
	}
	
	//빈방 삭제하기
	public boolean delEmptyRoom()
	{
		synchronized (roomData){
			for(RoomData R:roomData)
			{
				if(R.clientManage.clientID.size() < 1)
				{					 	
					roomData.remove(R);
					return true;					
				}
			}			
		}			
		return false;
	}
	
	//방 출입
	public boolean joinRoom(String roomName)
	{	
		synchronized (roomData) {
			for(RoomData R:roomData)
			{
				if(R.roomName.equals(roomName))	//방이 존재하는 경우
				{
					System.out.println("ClientID : "+clientID+" 가 "+roomName+" 에 참여했습니다.");
					R.clientManage.yourName.add(yourName);
					R.clientManage.clientID.add(this.clientID);
					R.clientManage.eventSocket.add(eventSocket);
					R.clientManage.cameraSocket.add(cameraSocket);
					R.clientManage.voiceSocket.add(voiceSocket);
					
					//BroadCastSocket은 아웃풋만 필요하고, BroadCastThread 특성상 이렇게 처리하는게 편하다.
					try {
						R.clientManage.broadCast.add(new BufferedWriter(new OutputStreamWriter(broadCastSocket.getOutputStream())));
					} catch (IOException e) {
						e.printStackTrace();
					}
					return true;
				}				
			}
		}
		System.out.println("ClientID : "+this.clientID+" 가 "+roomName+" 방을 참여하려 시도했으나 실패, 없는 방인거 같습니다.");
		return false;
	}
	
	//만약 ServerThread.exitServer 메소드를 호출하려면 exitRoom를 먼저 호출해야 아래 ArrayList가 엉키지 않는다. 
	public boolean exitRoom(String roomName)
	{
		synchronized (roomData) {
			for(RoomData R:roomData)
			{
				if(R.roomName.equals(roomName))
				{
					System.out.println("ClientID : "+clientID+" 가 "+roomName+" 을 나갔습니다.");
					int index = R.clientManage.clientID.indexOf(this.clientID);
					R.clientManage.yourName.remove(index);
					R.clientManage.clientID.remove(index);
					R.clientManage.broadCast.remove(index);
					R.clientManage.eventSocket.remove(index);
					R.clientManage.cameraSocket.remove(index);
					R.clientManage.voiceSocket.remove(index);
					return true;					
				}
			}
		}
		System.out.println("ClientID : "+this.clientID+" 가 참여했다는 "+roomName+" 방은 없습니다.");
		return false;
	}
	
	//방 목록 보내는 부분
	//클라이언트 단에도 이것과 대응되도록 메소드 선언한다.
	public boolean roomListSender()
	{
		ObjectOutputStream objectOutput = null;
		RoomDataToArrayString result;
		ArrayList<String> wantList = new ArrayList<>();
		ArrayList<Integer> wantJoinNumber = new ArrayList<>();
		
		//방 목록을 roomList에 집어넣는다.
		for(RoomData R: roomData)
		{
			wantList.add(R.roomName);
			wantJoinNumber.add(R.clientManage.clientID.size());
		}
		
		//뽑아낸 방목록과 방에 참가한 인원을 result 객체에 집어넣는다.
		result = new RoomDataToArrayString(wantList, wantJoinNumber);
		
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
			if(signal.toDoResponse(signal.roomList))	//원격측에서 전달을 받았다 오바하면
			{
				System.out.println(this.clientID+" 에게 방 목록을 성공적으로 전송했습니다.");
				return true;
			}			
			
		}
		return false;		
	}
	
	public boolean roomListReceiver(RoomDataToArrayString result)
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
				result = (RoomDataToArrayString) objectInput.readObject();
			} catch (ClassNotFoundException | IOException e) {
				System.out.println("result 객체를 받는중 예외");
				e.printStackTrace();
				return false;
			}
			System.out.println("ClientID : "+this.clientID+"가 방목록 받는데 성공했습니다.");
			return true;
		}
		
		System.out.println("ClientID : "+this.clientID+"가 방목록 받는데 실패했습니다.");
		return false;
		
	}

	
	//클라이언트가 방 관련하여 요청하는 부분
	//서버단은 직접 신호 받기를 대기하므로 이런 메소드는 만들지 않는다.
	//command는 시그널, wantRoomName은 command하고 싶은 방이름, roomName은 너가 가지고 있는 방 이름의 변수(client객체의 roomName 변수)
	public boolean clientsRequest(byte[] command,String wantRoomName, String roomName, RoomDataToArrayString result)
	{
		this.Used = true;
		BufferedWriter eventOutput;
		try {
			eventOutput = new BufferedWriter(new OutputStreamWriter(eventSocket.getOutputStream()));
		} catch (IOException e1) {
			System.out.println("clientRequest에서 예외발생");
			e1.printStackTrace();
			this.Used = false;
			return false;
		}
		
		
		if(signal.toDoRequest(command))
		{
			if(signal.signalChecking(command, signal.makeRoom) || signal.signalChecking(command, signal.joinRoom) || signal.signalChecking(command, signal.exitRoom))
			{
				try {
					eventOutput.write(wantRoomName);
					eventOutput.newLine();
					eventOutput.flush();
				} catch (IOException e) {
					System.out.println(command+" 도중 예외 발생");
					e.printStackTrace();
					this.Used = false;
					return false;
				}
				
				if(signal.toAccept(signal.makeRoom))
				{
					roomName = new String(wantRoomName);
					this.Used = false;
					return true;
				}
				else if(signal.toAccept(signal.signalStringToByte("joinRoom")))
				{
					roomName = new String(wantRoomName);
					this.Used = false;
					return true;
				}
				else if(signal.toAccept(signal.signalStringToByte("exitRoom")))
				{
					roomName = new String(value.unname);
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
