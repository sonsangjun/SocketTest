package Sender;

import java.util.List;

//방 목록을 보낼때와 , 방안의 클라이언트의 위치정보 보낼때
//두 경우에만 쓰인다.
//객체 직렬화 전송을 위해 만들었다.
public class RoomDataToArray implements java.io.Serializable{
	//static final long serialVersionUID = 100;
	public boolean checking = false;		//클라가 서버에서 받은 위치정보를 확인했는지 여부 (true 했다.)
	
	List<String> wantList = null;
	List<Integer> wantJoinNumber = null;	//이건 방목록 보낼때만 유효
	
	List<Integer>clientID = null;
	List<Double> latitude = null;
	List<Double> longitude = null;
	
	public RoomDataToArray(List<String> wantList, List<Integer> wantJoinNumber) {
		this.wantList = wantList;
		this.wantJoinNumber = wantJoinNumber;
	}
	
	public RoomDataToArray(List<Integer> clientID, List<Double> latitude, List<Double> longitude)
	{
		this.clientID = clientID;
		this.latitude = latitude;
		this.longitude = longitude;		
	}
}
