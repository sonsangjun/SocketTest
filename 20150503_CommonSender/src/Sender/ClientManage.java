package Sender;

import java.io.BufferedWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//서버 스레드의 정적 배열리스트에서 관리하기 편하게끔 클래스에 모아놨다.
//RoomData클래스에 종속된다.
public class ClientManage {
	List<String> yourName 				= Collections.synchronizedList(new ArrayList<>());
	List<Integer> clientID				= Collections.synchronizedList(new ArrayList<>());
	List<BufferedWriter> broadCast		= Collections.synchronizedList(new ArrayList<>());
	List<Socket> eventSocket 			= Collections.synchronizedList(new ArrayList<>());
	List<Socket> cameraSocket			= Collections.synchronizedList(new ArrayList<>());
	List<Socket> voiceSocket			= Collections.synchronizedList(new ArrayList<>());
	
}
