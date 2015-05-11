package Sender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//이렇게 하면 동기화 블록이 완성된다. 결국 인캡슐 해야하네.
//서버스레드에서 초기화 해주어야 할게 있다.
public class RoomManagement implements java.io.Serializable
{
	List<String> joinRoomName = Collections.synchronizedList(new ArrayList<String>());
	List<Integer> joinNumber = Collections.synchronizedList(new ArrayList<Integer>());
}	
