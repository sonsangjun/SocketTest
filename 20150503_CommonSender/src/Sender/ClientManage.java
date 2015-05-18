package Sender;

import java.io.BufferedWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//서버 스레드의 정적 배열리스트에서 관리하기 편하게끔 클래스에 모아놨다.
//RoomData클래스에 종속된다.
//라즈베리파이 javac는 new ArrayList<>(); 만쓰면 양립할수 없는 타입이란 에러 뜬다. 자바컴파일러 버젼이 낮은건지...
public class ClientManage {
	List<String> yourName 				= Collections.synchronizedList(new ArrayList<String>());
	List<Integer> clientID				= Collections.synchronizedList(new ArrayList<Integer>());
	List<BufferedWriter> broadCast		= Collections.synchronizedList(new ArrayList<BufferedWriter>());
	List<Socket> eventSocket 			= Collections.synchronizedList(new ArrayList<Socket>());
	List<Socket> cameraSocket			= Collections.synchronizedList(new ArrayList<Socket>());
	List<Socket> voiceSocket			= Collections.synchronizedList(new ArrayList<Socket>());
	List<Double> latitude				= Collections.synchronizedList(new ArrayList<Double>());	//위도
	List<Double> longitude				= Collections.synchronizedList(new ArrayList<Double>());	//경도
	
}
