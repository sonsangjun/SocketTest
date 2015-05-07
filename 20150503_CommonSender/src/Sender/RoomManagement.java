package Sender;

public class RoomManagement {
	String roomName;
	int joinNumber;
	public RoomManagement(String roomName) {
		this.roomName = roomName;
		joinNumber = 1;	//방이 만들어지는건 참여자가 한명이상 있으므로 최소값 1 부여		
	}
}
