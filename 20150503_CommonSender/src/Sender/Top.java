package Sender;

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

public class Top {
	static final boolean _Server =false;			//코드가 서버로 작동하는 경우 true
	static final String fileName = "Test.jpeg";	//카메라 프리뷰 이미지 전송이 파일 전송과 비슷하므로 
	
	
	
	public static void main(String[] args) {
		final String ServerIP = "221.156.9.145";	//외부에서 테스트할때 IP
		//final String ServerIP = "192.168.0.3";
		final int waitTime = 2000;
		final int portNum = 9000;
		
		if(_Server)
		{
			System.out.println("서버 시작");
			Server server = new Server(waitTime, portNum);
			server.mainServer();
		}
		else
		{
			System.out.println("클라이언트 시작");
			Client client = new Client(ServerIP, portNum, waitTime);
			client.start();
		}
	}
}
/*				 Top
 * 				  │
 * 			┌─────┴─────┐
 * 		 Server		Client
 * 			│			
 * 		ServerThread
 *  		│
 *  	ClientManagement		(ClientManagement클래스는 서버스레드 정적배열리스트를 위해 선언된 클래스)	
 * 	┌────────────────────────── Server,Client에 사용되는 나머지 클래스들 관계
 *  │				
 *	├─	SharedData
 *	├─	SignalData
 *	├─	IntegerToByteArray
 *	├─	ByteArrayTransCeiverThread
 *(나중에 추가)
 * 
 */
 
