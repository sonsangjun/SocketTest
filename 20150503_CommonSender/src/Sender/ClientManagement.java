package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;


//서버 스레드의 정적 배열리스트에서 관리하기 편하게끔 클래스에 모아놨다.
public class ClientManagement {
	boolean cameraUsed = false;			//비사용중이면 false 초기값은 사용중이 아니므로 false
	boolean voiceUsed = false;			//사용중이라면 true
	
	boolean cameraTransCeive = true;	//송수신 실패하면 false
	boolean voiceTransCeive = true;		
	
	
	int clientID;		//클라이언트를 구분하기 위해 
	String clientIP;	//클라이언트 IP
	
	String joinRoom;
	SignalData signal;
	
	Socket eventSocket;
	Socket cameraSocket;
	Socket voiceSocket;
	BufferedInputStream eventInput;
	BufferedOutputStream eventOutput;

	
	
	public ClientManagement(int clientID, String clientIP, String joinRoom, SignalData signal, Socket eventSocket, Socket cameraSocket, Socket voiceSocket, BufferedInputStream eventInput, BufferedOutputStream eventOutput)
	{
		this.clientID = clientID;
		this.clientIP = clientIP;
		this.joinRoom = joinRoom;
		this.signal = signal;
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.voiceSocket = voiceSocket;		
		this.eventInput = eventInput;
		this.eventOutput = eventOutput;
	}
}
