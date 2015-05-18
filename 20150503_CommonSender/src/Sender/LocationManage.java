package Sender;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class LocationManage {
	RoomData roomData = null;
	Socket eventSocket = null;
	RoomDataToArray LocationList = null;
	
	//서버측 생성자
	public LocationManage(Socket eventSocket, RoomData roomData) {
		this.eventSocket = eventSocket;
		this.roomData = roomData;
	}
	
	//클라이언트측 생성자
	public LocationManage(Socket eventSocket)
	{
		this.eventSocket = eventSocket;
	}
	
	
	
	//클라가 서버에게 주기적으로 보내는 위치정보
	//클라측
	public boolean sender(int clientID, double latitude, double longitude)
	{
		ObjectOutputStream outputLocation = null;
		RoomDataToArray myLocation = new RoomDataToArray(new ArrayList<Integer>(), new ArrayList<Double>(), new ArrayList<Double>());
			
		myLocation.clientID.add(clientID);
		myLocation.latitude.add(latitude);
		myLocation.longitude.add(longitude);
		try {
			outputLocation = new ObjectOutputStream(eventSocket.getOutputStream());
			outputLocation.writeObject(myLocation);
			outputLocation.flush();			
		} catch (IOException e) {
			System.out.println("서버에게 위치정보 전송 실패");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	//서버측
	public RoomDataToArray receiver()
	{
		ObjectInputStream inputLocation = null;
		RoomDataToArray clientLocation = new RoomDataToArray(null,null,null);
		try {
			inputLocation = new ObjectInputStream(eventSocket.getInputStream());
			clientLocation = (RoomDataToArray) inputLocation.readObject();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("클라이언트로부터 위치정보 못받음");
			e.printStackTrace();
			return null;
		}
		return clientLocation;		
	}	
	
	//방에 참가한 인원의 위치 정보를 요청하는 경우
	//보내는자. 서버가 호출하는 메소드
	public boolean listSender()
	{
		ObjectOutputStream outputLocationList = null;
		RoomDataToArray locationList = new RoomDataToArray(roomData.clientManage.clientID, roomData.clientManage.latitude, roomData.clientManage.longitude);
		try {
			outputLocationList.writeObject(locationList);
			outputLocationList.flush();
		} catch (IOException e) {
			System.out.println(roomData.roomName+" 방의 위치 정보 전송 못함");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	//받는자. 클라이언트가 호출하는 메소드
	public RoomDataToArray listReceiver()
	{
		ObjectInputStream inputLocationList = null;
		RoomDataToArray locationList = null;
		try {
			inputLocationList = new ObjectInputStream(eventSocket.getInputStream());
			locationList = (RoomDataToArray) inputLocationList.readObject();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return locationList;		
	}
}
