package Sender;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

//[transmission송신] [reception수신]
/* Date		20150503 
 * Title 	CommonSender(범용전송기(?))	
 * by SonSangJun
 * 20150430 에 만들었던 파일 전송기능은 두 스레드간의 통신문제 때문에 
 * 수정을 하고 싶지만, 건들기가 영 좋지 않아서 일단 킵
 * 그리고 안드로이드 통신 부분도 짜야해서 범용으로 쓸 수 있게 만드는게 목표다.
 * 
 * 신호 송수신은 signalData에서 처리하도록 하고, 서버와 클라이언트는 그 클래스의 메소드를 호출하도록 한다.
 * BufferedExceptionProcessingThread는 소켓 버퍼의 read()에 의한 블락을 막기위해 만든 스레드.
 * 
 * 20150506 테스트 메소드에 로마자 번호를 붙인다. 각 클라이언트와 서버의 테스트 메소드는 로마자 번호로 대응된다. 
 * 
 * 카메라 프리뷰 이미지와, 음성, 위치정보를 보내는 포트를 다르게 해야할거 같다. 같은 소켓으로 두면
 * 엉망이 될거 같다. 위치정보는 크기가 작지만, 나머지는 크기에 다른 포트로 하고, 스레드도 각각 필요할때만 돌리자.
 * 9000포트는 위치정보겸, 각종 이벤트 뿌리기 (XX님이 화면 공유를 시작합니다.)
 * 9001포트는 카메라 프리뷰 이미지
 * 9002포트는 음성 
 * 
 * 서버 스레드에서 각 클라이언트 관리를 위해서 스트림 포트와 참여한 방번호를 기억하는 클래스를 하나 선언하고, 
 * 각 클라이언트에 대한 인스턴스를 만들어 서버스레드의 정적 배열리스트에 추가해 관리하도록 계획.
 */

/*서버와 클라이언트 작동방식( 방 관리 기준 )
 * 		client	<──────────────────────────────> server
 * 
 * 1.	client가 signal.toDoRequest(명령어) 보냄
 * 
 * 2.	Server가 signal.receiveSignalToByteArray()로 신호 받고
 * 		signal.toDoResponse로 확인 신호 보냄(만약 이 메소드가 false 반환하면 연결이 끊긴 것임)
 * 
 * 3.	client가  roomManage.clientsRequest(명령어)를 호출한다.
 * 
 * 4.	Server는 roomManage.(방 관리 명령어. ex)roomManage.makeRoom()같은거) 를 호출하여
 * 		client와 server가 통신한다.( 이때, client가 방이름을 입력할 경우도 있다.)
 * 
 * 5.	서로 통신이 끝나면 결과값을 콘솔에 출력하고 client와 Server는 다시 대기상태가 된다.
 * 	
 * 다른것도 이것과 다르지 않다.
 */

public class Top {
	public static void main(String[] args) {
		ValueCollections value = new ValueCollections();
		
		if(value._Server)
		{
			System.out.println("서버 시작");
			Server server = new Server();
			server.mainServer();
		}
		else
		{
			//카메라 프리뷰 이미지가 없어서 파일로부터 이미지를 읽어들입니다.
			//tempFile은 이미지 파일이 담길 바이트 배열.
			byte[] tempFile;
			FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(value.fileName);
				tempFile = new byte[inputStream.available()];
				inputStream.read(tempFile);
				inputStream.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("파일 찾을 수 없음.");
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("IO에러");
				e.printStackTrace();
				return;
			}
			//이미지 파일을 tempFile에 담았습니다. (이미지 파일명은 ValueCollections.java참고)
			//								(이미지 파일 위치는 프로젝트 폴더안에 넣으세요.)
			
						
			System.out.println("클라이언트 시작");
			Client client = new Client(tempFile, null);
			client.start();
		}
	}
}
/*						 	 Top───────ValueCollections
 *							  │
 * 			┌─────────────────┴──────────────────┐
 * 		 Server						 		  Client
 * 			│						    ┌────────┤
 * 		ServerThread────────────────────┼SocketBroadCastThread
 *			│						    │SocketCameraThread─┬──	SocketCameraUsed	
 *			│							└SocketVoiceThread──┴──	ByteArrayTransCeiver───────ByteArrayTransCevierRule
 *			│									 └ SocketVoiceUsed
 *	(List<RoomData> roomDataList) 		
 * 	┌────────────────────────────────────────────── Server,Client에 사용되는 나머지 클래스들 관계
 *  │				
 *	├─	SharedData
 *	├─	SignalData
 *	├─	IntegerToByteArray
 *	├─	
 *	├─	
 *  │
 *  ├─	LocationManage────┐		   							
 *	├─	RoomManage───── RoomData ───────────────┬── ClientManage
 *	│											└── RoomDataToArray
 *	├─	SocketBroadCastUsed	//─┐				
 *	├─	SocketCameraUsed	// ├─	이게 필요한 클래스에 
 *	├─	SocketVoiceUsed		// │	이 세 클래스는 종속적이다.
 *	├─	SocketEventUsed		//─┘	소켓을 사용한다면, 꼭 이클래스 값을 true시켜라. 안그러면 꼬인다.
 *(나중에 추가)
 * 
 * 클라이언트 ID는 static int로 접속시 해당 변수에 ++ 된 값을 할당한다.
 */
 
