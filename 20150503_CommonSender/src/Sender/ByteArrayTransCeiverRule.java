package Sender;

import java.net.Socket;

public class ByteArrayTransCeiverRule {
	int counter = 0;				//파일 분할 횟수
	int fileSize = 0;				//클라이언트간 통신을 위해 선언(파일사이즈)	
	int extra = 0;					//파일의 마지막 분할부분의 남은부분(인덱스경계초과 예외 방지)
	int fileUnitSize = 8192;		//파일 분할 단위는 8KByte
	
	//데이터 스트림 전송전에 초기화 시켜야 한다.
	SocketEventUsed socketEventUsed;
	SocketCameraUsed socketCameraUsed;
	SocketVoiceUsed socketVoiceUsed;
	
	Socket cameraSocket;
	Socket voiceSocket;
	
	RoomData roomData;
	SignalData signal;
	int clientID;
	boolean transCeive;			//서버일경우 이 값은 뭐든상관없다.
	boolean CameraVoice;		//프리뷰 이미지 전송이면 true 아니면 false
	
	
	
	
	public void Calc(int fileSize)
	{
		counter = fileSize/fileUnitSize;
		extra = fileSize%fileUnitSize;		
	}
}
