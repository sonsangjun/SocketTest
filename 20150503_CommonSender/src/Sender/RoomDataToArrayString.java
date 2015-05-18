package Sender;

import java.util.ArrayList;


//객체 직렬화 전송을 위해 만들었다.
public class RoomDataToArrayString implements java.io.Serializable{
	ArrayList<String> wantList;
	ArrayList<Integer> wantJoinNumber;	//이건 방목록 보낼때만 유효
	public RoomDataToArrayString(ArrayList<String> wantList, ArrayList<Integer> wantJoinNumber) {
		this.wantList = wantList;
		this.wantJoinNumber = wantJoinNumber;
	}
}
