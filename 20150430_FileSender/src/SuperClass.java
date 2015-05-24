/*2015.04.30 FileTransfer
 * 서로 주고 받는 신호를 중간 변환단계 없이 바이트 배열로만 주고 받게 해야겠다.
 * 20150501 예상대로 주고 받는 신호를 바이트 배열로 함으로서 중간 변환단계가 없어 
 * 서로 신호를 못받던 문제를 해결했다.
 * 이제는 용량이 큰 파일을 보내는 문제에 도전해야겠다.
 * 30KB정도 용량도 잘린다...
 * 8KB는 잘리지 않는다. 이걸 기준으로 분할 전송해보자.
 * 
 * 파일 송수신 시에 분할전송 하면 CheckingThread가 분할된것을 계속 검사해줘야 겠다.
 * 작은파일은 정상적으로 되는데(140KB) 큰파일은(509KB) 시도조차 안된다. 분할횟수 계산중의 타이밍문제인듯.
 * 20150502 500KB상당의 파일전송은 잘된다. 하지만, 가끔 안된다. (4번중 1번)
 * 그렇다면 매번 OK신호를 받지말고 파일전송이 완료된 마지막에서만 OK신호를 받고 종료해보자. TCP는 신뢰가 높으니
 * 신뢰성을 이용한다 생각하기.
 * 
 * 20150502 파일 받는것은 가능한데, 가끔 notify() <-> wait()가 안먹는다. 서버에서 클라이언트 신호를 받아 notify()를 했음에도 ST측에서 wait()를 풀지못해 락에 걸린 경우가 있다.
 * 스레드 우선순위 때문인지 (monitor스레드가 있으므로 가능)(하지만, wait()가 없는데 굳이 그쪽에 notify()신호를 보낼까)
 * 일단 wait()에 최대대기시간을 주어 해결했다.
 * notify()를 쓰기보단 busy loop이 더 좋은데, Cpu부담은 busy loop이 더 커서 문제네...
 * 
 * 20150502. 여기까지만 코드짜고 마무리.
 * 소켓 간의 통신은 문제가 없는데 정작 스레드간 통신이 발목을 잡는다. 이점 꽤 문제다...
 * 이걸 감안해서 코드를 다시 짜봐야겠다.
 * 
 */
public class SuperClass {
	
	public static void main(String[] args) {
		final int unitSize = 8192;		//파일 스트림 분할 전송 8KB/1회
		final boolean _Server = false;
		//final boolean _Server = true;
		final int portNum = 9000;
		final int fileSizeIndex = 4;
		final String fileName = "test.jpeg";
		final String ServerIP = "192.168.0.3";
		
		Server server;
		Client client;
		if(_Server)
		{
			server = new Server(portNum, fileName, fileSizeIndex,unitSize);
			server.mainServer();			
		}
		else
		{
			client = new Client(portNum, fileName, fileSizeIndex, ServerIP,unitSize);
			client.mainClient();
		}
	}
}
