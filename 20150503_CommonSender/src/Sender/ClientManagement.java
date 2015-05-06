package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;


//서버 스레드의 정적 배열리스트에서 관리하기 편하게끔 클래스에 모아놨다.
public class ClientManagement {
	
	int clientID;		//클라이언트를 구분하기 위해 
	String clientIP;	//클라이언트 IP
	
	String joinRoom;
	SignalData signal;
	
	BufferedInputStream eventInput;
	BufferedInputStream cameraInput;
	BufferedInputStream voiceInput;
	BufferedOutputStream eventOutput;
	BufferedOutputStream cameraOutput;
	BufferedOutputStream voiceOutput;
	
	
	public ClientManagement(int clientID, String clientIP, String joinRoom, SignalData signal, BufferedInputStream eventInput, BufferedInputStream cameraInput, BufferedInputStream voiceInput, BufferedOutputStream eventOutput,	BufferedOutputStream cameraOutput, BufferedOutputStream voiceOutput)
	{
		this.clientID = clientID;
		this.clientIP = clientIP;
		this.joinRoom = joinRoom;
		this.signal = signal;
		this.eventInput = eventInput;
		this.cameraInput = cameraInput;
		this.voiceInput = voiceInput;
		this.eventOutput = eventOutput;
		this.cameraOutput = cameraOutput;
		this.voiceOutput = voiceOutput;	
	}
}
