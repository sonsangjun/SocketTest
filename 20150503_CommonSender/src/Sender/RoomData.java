package Sender;

//이렇게 하면 동기화 블록이 완성된다. 결국 인캡슐 해야하네.
//서버스레드에서 초기화 해주어야 할게 있다.
public class RoomData implements java.io.Serializable
{
	final String unname = new String("unname");
	String roomName;
	ClientManage clientManage = new ClientManage();
	
	public RoomData(String roomName) {
		this.roomName = new String(roomName);
	}
}	
