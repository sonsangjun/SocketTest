package Sender;

import java.net.Socket;

public class ByteArrayTransCeiverRule {
	int counter = 0;				//파일 분할 횟수
	int fileSize = 0;				//클라이언트간 통신을 위해 선언(파일사이즈)	
	int extra = 0;					//파일의 마지막 분할부분의 남은부분(인덱스경계초과 예외 방지)
	int fileUnitSize = 8192;		//파일 분할 단위는 8KByte
	
	//데이터 스트림 전송전에 초기화 시켜야 한다.
	SocketEventUsed socketEventUsed;	//signal사용중인지 체크
	SocketCameraUsed socketCameraUsed;	//장치 사용중인지 체크
	SocketVoiceUsed socketVoiceUsed;	//송수신할 데이터스트림을 포함하고 있다.(받는경우 전송된 데이터스트림, 이런경우 byte[]만 선언해서 넘겨주면 된다.)
	
	Socket cameraSocket;
	Socket voiceSocket;
	
	RoomData roomData	=	null;				//클라이언트는 null이다.
	SignalData signal;
	int clientID;
	boolean transCeive;				//서버일경우 이 값은 뭐든상관없다.
									//Trans(송신)(업로드)는 true, Ceive(수신)(다운로드)는 false
	
	boolean CameraVoice = false;	//Camera 는 true,		Voice는 false;
	
	
	
	
	public void Calc(int fileSize)
	{
		counter = fileSize/fileUnitSize;
		extra = fileSize%fileUnitSize;		
	}
}
