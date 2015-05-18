package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

//서버,클라이언트 스레드와 독립적으로 돌아감 .
//크기가 큰 카메라프리뷰나 음성 송수신에 사용.


/* 20150506 메소드 목록
 * public ByteArrayTransCeiverThread(int clientID, boolean transCeive, boolean CameraVoice, byte[] fileByteArray, ArrayList<ClientManagement> clientManagementList, ClientManagement clientManagement)
 * 	└ 생성자
 * public void run()
 * 	└ 코어
 * 	public void clientTrans()
 * 	└ 클라이언트가 서버에게 데이터스트림 보냄
 * 	public void clientReceive()
 * 	└ 클라이언트가 서버에게 데이터스트림 받음.
 * 	public void serverTransCeive()
 * 	└ 서버가 클라이언트에게 받은 데이터스트림을 그외 다른 클라이언트에게 뿌림.
 * 
 * 	public void usedChecking(boolean used)
 * 	└ 카메라,보이스 전송 소켓을 사용하는지 여부 체크 (사용하면 true)
 * 	└ 사용됨을 표시해야 클라이언트간 송수신 경쟁이 일어나지 않는다. (혼돈...) 
 * 	public void transCeiveFailed()			
 * 	└ 송수신 실패를 클라이언트 매니지먼트에 기록한다. (실패하면 false기록)
 * 	public void transCeiveSuccess()			
 * 	└ 송수신 성공을 클라이언트 매니지먼트에 기록한다. (성공하면 true 기록)
 * 
 *	public void toClientUsedChecking(ClientManagement clientManagement, boolean used)
 * 	└ 내용은 usedChecking와 같다. 이건 static ClientManagementList에 있는걸 건들기 위해 선언했다.
 *	public void toClientTransCeiveFailed(ClientManagement clientManagement)			
 * 	└ TransCeiveFailed과 같다. 상동
 *	public void toClientTransCeiveSuccess(ClientManagement clientManagement)
 *	└ TransCeiveSuccess와 같다. 상동			
 */
public class ByteArrayTransCeiverThread {
	SocketCameraUsed socketCameraUsed;		//장치 사용중인지 체크
	SocketVoiceUsed socketVoiceUsed;
	
	Socket targetSocket; 					//데이터 스트림에 이용할 소켓이다.
	RoomData roomData;						//클라이언트는 null이다.
	
	boolean transCeive;						//Trans(송신)(업로드)는 true, Ceive(수신)(다운로드)는 false
	boolean CameraVoice;					//Camera 는 true,		Voice는 false;
	byte[] fileByteArray;
	int clientID;						
	
	SignalData signal;
	ByteArrayTransCevierRule byteArrayTransCeiverRule; 
	
	BufferedInputStream input;
	BufferedOutputStream output;
	IntegerToByteArray integerToByteArray;
	
	
	
	/* 바이트 배열 전송포트중
	 * 9001은 카메라 프리뷰 포트
	 * 9002는 음성 포트이다. 참고할것
	 */
	
	//데이트 스트림 전송 스레드는 9000번 포트로부터 신호를 받아서 생성되는 스레드이다. 이놈 자체가 데이터 스트림 요청입력을 대기하지 않는다.
	//다시말하지만, roomData == null은 클라이언트이다.
	public ByteArrayTransCeiverThread(SocketCameraUsed socketCameraUsed, SocketVoiceUsed socketVoiceUsed, Socket targetSocket, RoomData roomData, int clientID, boolean transCeive, boolean CameraVoice, byte[] fileByteArray) {
		this.socketCameraUsed = socketCameraUsed;
		this.socketVoiceUsed = socketVoiceUsed;
		
		this.targetSocket = targetSocket;
		this.roomData = roomData;
		this.clientID = clientID;
		this.transCeive = transCeive;						//서버일경우 이 값은 뭐든상관없다.
		this.CameraVoice = CameraVoice;						//프리뷰 이미지 전송이면 true 아니면 false
		this.fileByteArray = fileByteArray;					//전송할 데이터스트림(받는경우 전송된 데이터스트림, 이런경우 byte[]만 선언해서 넘겨주면 된다.)
	}
	
	public void run()
	{
		//전송전에 스트림 초기화
		signal = new SignalData(targetSocket);
		try {
			input = new BufferedInputStream(targetSocket.getInputStream());
			output = new BufferedOutputStream(targetSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("바이트배열 전송전 초기화 실패");
			e.printStackTrace();
			return ;
		}			
		
		integerToByteArray = new IntegerToByteArray();
				
		//송수신이 클라이언트인가 서버인가 판단
		if(roomData == null)
		{
			if(transCeive)
				clientTrans();
			else
				clientReceive();		
		}
		else 
		{
			serverTransCeive();
		}			
	}
	
	//현재 아래 짠 코드는 카메라 프리뷰 전송때 오버헤드가 크다... 이점을 어떻게 해야할지...
	//클라이언트가 서버에게 전송
	public void clientTrans()
	{		
		usedChecking(true);
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		integerToByteArray.initialByteArray(fileSize);
		if(signal.toDoRequest(signal.byteReceive))
		{
			integerToByteArray.getBytes(fileByteArray.length,fileSize);		
			try {
				output.write(fileSize);
				output.flush();
				if(signal.toAccept(signal.byteSize))
				{
					byteArrayTransCeiverRule.Calc(fileByteArray.length);
					int counter = 0;
					int unitSize = byteArrayTransCeiverRule.fileUnitSize;
					int maxCounter = byteArrayTransCeiverRule.counter;
						
					while(true)
					{
						if(counter >= maxCounter)
						{
							if(counter*unitSize == fileByteArray.length)
							{
								usedChecking(false);
								return ;									
							}
							if(signal.toDoRequest(signal.byteSend))
							{
								output.write(fileByteArray, counter*unitSize, byteArrayTransCeiverRule.extra);
								output.flush();								
								usedChecking(false);
								return ;
							}								
						}
						
						else if(signal.toDoRequest(signal.byteSend))
						{
							output.write(fileByteArray, counter*unitSize, unitSize);
							output.flush();
							counter++;
						}
					}						
				}
				
			} catch (IOException e) {
				System.out.println("clientTrans() output.write(fileSize) 예외");
				e.printStackTrace();
				usedChecking(false);
				return ;
			}	
		}
		
		usedChecking(false);
		return ;		
	}
	
	public void clientReceive()
	{
		usedChecking(true);
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		int intFileSize;
		integerToByteArray.initialByteArray(fileByteArray);
		
		if(signal.toAccept(signal.byteReceive))
		{	
			try {
				input.read(fileSize);
				intFileSize = integerToByteArray.getInt(fileSize);
				fileByteArray = new byte[intFileSize];		
				if(!signal.toDoResponse(signal.byteSize))
				{
					System.out.println("byte 받았다는 확인을 보내는 중 서버와 연결 예외 발생");					
					usedChecking(false);
					return ;						
				}
				
				byteArrayTransCeiverRule.Calc(intFileSize);
				int counter=0;
				int unitSize = byteArrayTransCeiverRule.fileUnitSize;
				int maxCounter = byteArrayTransCeiverRule.counter;
				
				while(true)
				{
					if(counter >= maxCounter)
					{
						if(counter*unitSize == fileByteArray.length)
						{
							usedChecking(false);
							System.out.println("서버로 전송을 완료했습니다.");
							break;
						}
						
						else if(signal.toAccept(signal.byteSend))
						{
							input.read(fileByteArray, unitSize, byteArrayTransCeiverRule.extra);
							usedChecking(false);
							System.out.println("서버로 전송을 완료했습니다.");
							break;								
						}							
					}
					else if(signal.toAccept(signal.byteSend))
					{
						input.read(fileByteArray, counter*unitSize, unitSize);
						counter++;
					}						
				}
			} catch (IOException e) {
				System.out.println("서버가 요청을 하지 않습니다.");
				e.printStackTrace();
				usedChecking(false);
				return ;
			}							
		}
	}

	
	//서버가 클라이언트에게 받아 데이터 스트림을 전송한 클라이언트를 제외하고 방여 참여중인 다른 클라이언트에게 데이터 스트림 전송.
	public void serverTransCeive()
	{
		usedChecking(true);
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		int intFileSize;
		integerToByteArray.initialByteArray(fileSize);
		
		if(signal.toAccept(signal.byteReceive))
		{	
			try {
				input.read(fileSize);
				intFileSize = integerToByteArray.getInt(fileSize);
				fileByteArray = new byte[intFileSize];		
				if(!signal.toDoResponse(signal.byteSize))
				{
					System.out.println("클라이언트에게 byte사이즈 받았다는 응답을 보내지 못했습니다.");
					usedChecking(false);
					return ;						
				}
				
				byteArrayTransCeiverRule.Calc(intFileSize);
				int counter=0;
				int unitSize = byteArrayTransCeiverRule.fileUnitSize;
				int maxCounter = byteArrayTransCeiverRule.counter;
				
				while(true)
				{
					if(counter >= maxCounter)
					{
						if(counter*unitSize == fileByteArray.length)
						{							
							System.out.println(roomData.roomName+"에 참가한 "+this.clientID+"의 데이터 스트림을 받았습니다.");
							System.out.println("받은 스트림의 크기는 "+intFileSize+"Byte 입니다.");
							break;
						}
						
						if(signal.toAccept(signal.byteSend))
						{
							input.read(fileByteArray, unitSize, byteArrayTransCeiverRule.extra);
							System.out.println(roomData.roomName+"에 참가한 "+this.clientID+"의 데이터 스트림을 받았습니다.");
							System.out.println("받은 스트림의 크기는 "+intFileSize+"Byte 입니다.");
							break;								
						}							
					}
					else if(signal.toAccept(signal.byteSend))
					{
						input.read(fileByteArray, counter*unitSize, unitSize);
						counter++;
					}						
				}
			} catch (IOException e) {
				System.out.println("클라이언트에게서 정상적으로 데이터를 못받았습니다.");
				e.printStackTrace();
				usedChecking(false);
				return ;
			}							
		}
	
		//여기까지는 클라에게 서버가 파일 받는거고(정확히는 데이터스트림)
		//아래부터는 서버가 클라이언트에게 뿌린다.		
		while(true)
		{			
			//다른클라이언트에게 전송을 위해 반복문 선언
			for(int i=0; i<roomData.clientManage.clientID.size(); i++)
			{
				//다른 클라이언트와 송수신을 위해 버퍼스트림 선언
				BufferedOutputStream outputOtherClient;
				SignalData signalOtherClient = null;
				
				//자기 자신이면 통과
				if(roomData.clientManage.clientID.get(i) == this.clientID)
					continue;
				
				//카메라인지 소리인지 구분해 전송 소켓 열기(초기화)
				if(this.CameraVoice)
				{
					try {
						outputOtherClient = new BufferedOutputStream(roomData.clientManage.cameraSocket.get(i).getOutputStream());
						signalOtherClient = new SignalData(roomData.clientManage.cameraSocket.get(i));
						
					} catch (IOException e) {
						System.out.println("["+roomData.clientManage.clientID.get(i)+"] 에게 프리뷰 전송실패");
						e.printStackTrace();
						continue;	//전송실패하면 다음 클라이언트에게 전송
					}					
				}
				else
				{
					try {
						outputOtherClient = new BufferedOutputStream(roomData.clientManage.voiceSocket.get(i).getOutputStream());
						signalOtherClient = new SignalData(roomData.clientManage.voiceSocket.get(i));
						
					} catch (IOException e) {
						System.out.println("["+roomData.clientManage.clientID.get(i)+"] 에게 보이스 전송실패");
						e.printStackTrace();
						continue;
					}					
				}
						
				//클라이언트에게 준비하라고 요청 및 데이터 스트림 전송
				if(signalOtherClient.toDoRequest(signal.byteReceive))
				{
					try {
						outputOtherClient.write(fileSize);
						outputOtherClient.flush();
						
						if(signalOtherClient.toAccept(signal.byteSize))
						{
							int counter = 0;
							int unitSize = byteArrayTransCeiverRule.fileUnitSize;
							int maxCounter = byteArrayTransCeiverRule.counter;
							while(true)
							{
								if(counter >= maxCounter)
								{
									if(counter*unitSize == fileByteArray.length)
									{										
										System.out.println(roomData.roomName+"에 참가한"+roomData.clientManage.clientID.get(i)+"에게 데이터 스트림을 보냈습니다.");
										System.out.println("보낸 스트림의 크기는 "+byteArrayTransCeiverRule.fileSize+"Byte 입니다.");
										continue;
									}
									else if(signalOtherClient.toDoRequest(signal.byteSend))
									{
										outputOtherClient.write(fileByteArray, counter*unitSize, byteArrayTransCeiverRule.extra);
										outputOtherClient.flush();
										System.out.println(roomData.roomName+"에 참가한"+roomData.clientManage.clientID.get(i)+"에게 데이터 스트림을 보냈습니다.");
										System.out.println("보낸 스트림의 크기는 "+byteArrayTransCeiverRule.fileSize+"Byte 입니다.");											
										continue;
									}
								}
								else if(signalOtherClient.toDoRequest(signal.byteSend))
								{
									outputOtherClient.write(fileByteArray, counter*unitSize, unitSize);
									output.flush();
									counter++;										
								}
							}
						}
					} catch (IOException e) {
						System.out.println("["+roomData.clientManage.clientID.get(i)+"] 에게 전송실패");
						e.printStackTrace();						
						continue;
					}						
				}									//실패할경우 if문을 빠져나온다.
				continue;							//실패하면 다음 클라이언트에게 전송한다.
			}
			//for문이 끝나면 리턴한다.
			System.out.println(roomData.roomName+" 방의 클라이언트에게 데이터를 전송했습니다.");
			usedChecking(false);
			return ;
		}
	}
	
	//간단한 체크 함수
	public void usedChecking(boolean used)	//사용하면 true,	안사용하면 false
	{		
		if(this.CameraVoice)
		{
			synchronized (socketCameraUsed) {
				socketCameraUsed.socketCameraUsed = used;
			}			
		}					
		else
		{
			synchronized (socketVoiceUsed) {
				socketVoiceUsed.socketVoiceUsed = used;
			}			
		}
	}
}