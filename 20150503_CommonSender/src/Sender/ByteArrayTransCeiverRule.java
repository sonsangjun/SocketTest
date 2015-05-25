package Sender;

import java.net.Socket;

public class ByteArrayTransCeiverRule {
	int counter = 0;				//파일 분할 횟수
	int fileSize = 0;				//클라이언트간 통신을 위해 선언(파일사이즈)	
	int extra = 0;					//파일의 마지막 분할부분의 남은부분(인덱스경계초과 예외 방지)
	int fileUnitSize = 8192;		//파일 분할 단위는 8KByte
	
	//데이터 스트림 전송전에 초기화 시켜야 한다.
	SocketCameraUsed socketCameraUsed;	//장치 사용중인지 체크
	SocketVoiceUsed socketVoiceUsed;	//송수신할 데이터스트림을 포함하고 있다.(받는경우 전송된 데이터스트림, 아마 for문으로 일일이 다 집어넣어야할듯.)
	SocketPushUsed socketPushUsed;
	
	Socket cameraSocket;
	Socket voiceSocket;
	Socket pushSocket;
	
	RoomData roomData	=	null;		//클라이언트는 null이다.
	int clientID;
	boolean transCeive = true;			//서버일경우 이 값은 뭐든상관없다.
										//Trans(송신)(업로드)는 true, Ceive(수신)(다운로드)는 false
	
	boolean cameraVoice = false;		//Camera 는 true,		Voice는 false;
	
	//클라이언트
		//카메라 부분
		public ByteArrayTransCeiverRule(boolean transCeive, SocketCameraUsed socketCameraUsed, Socket cameraSocket) 
		{	this.cameraVoice = true;
			this.transCeive = transCeive;
			this.cameraSocket = cameraSocket;
			this.socketCameraUsed = socketCameraUsed;
		}
		
		//음성(두번째 boolean cameraVoice값에 상관없이 cameraVoice = false. 그냥 오버로딩할때 구분시켜놓을려고 끼워넣은 매개변수)
		public ByteArrayTransCeiverRule(boolean transCeive,boolean cameraVoice, SocketVoiceUsed socketVoiceUsed, Socket voiceSocket)
		{
			this.cameraVoice = false;
			this.transCeive = transCeive;
			this.voiceSocket = voiceSocket;
			this.socketVoiceUsed = socketVoiceUsed;		
		}
	//클라이언트 끝
	
	
	//서버
		//카메라
		public ByteArrayTransCeiverRule(int clientID, SocketCameraUsed socketCameraUsed, SocketPushUsed socketPushUsed, RoomData roomData)
		{
			int index = roomData.clientManage.clientID.indexOf(clientID);
			this.clientID = clientID;
			this.cameraVoice = true;
			this.socketCameraUsed = socketCameraUsed;
			this.cameraSocket = roomData.clientManage.cameraSocket.get(index);
			this.pushSocket = roomData.clientManage.pushSocket.get(index);
			this.socketPushUsed = socketPushUsed;
			this.roomData = roomData;
		}
		
		//음성
		public ByteArrayTransCeiverRule(int clientID, boolean cameraVoice, SocketVoiceUsed socketVoiceUsed,  SocketPushUsed socketPushUsed, RoomData roomData) 
		{
			int index = roomData.clientManage.clientID.indexOf(clientID);
			this.clientID = clientID;
			this.cameraVoice = false;
			this.socketVoiceUsed = socketVoiceUsed;
			this.voiceSocket = roomData.clientManage.voiceSocket.get(index);
			this.pushSocket = roomData.clientManage.pushSocket.get(index);
			this.socketPushUsed = socketPushUsed;
			this.roomData = roomData;		
		}	
	//서버 끝
	
	public void Calc(int fileSize)
	{
		counter = fileSize/fileUnitSize;
		extra = fileSize%fileUnitSize;		
	}
}
