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
 */

public class Top {
	static final boolean _Server =false;			//코드가 서버로 작동하는 경우 true
	static final String fileName = "Test.jpeg";	//카메라 프리뷰 이미지 전송이 파일 전송과 비슷하므로 
	
	
	public static void main(String[] args) {
		//final String ServerIP = "221.156.9.145";	//외부에서 테스트할때 IP
		final String ServerIP = "192.168.0.3";
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
 * 	┌────────────────────────── Server,Client에 사용되는 나머지 클래스들 관계
 *  │
 *	├─	SharedData
 *	├─	SignalData	─	BufferedExceptionProcessingThread
 *	├─	FileSizeChecking
 *(나중에 추가)

 * 
 */
 
