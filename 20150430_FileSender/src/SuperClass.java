/*2015.04.30 FileTransfer
 * 서로 주고 받는 신호를 중간 변환단계 없이 바이트 배열로만 주고 받게 해야겠다.
 * 20150501 예상대로 주고 받는 신호를 바이트 배열로 함으로서 중간 변환단계가 없어 
 * 서로 신호를 못받던 문제를 해결했다.
 * 이제는 용량이 큰 파일을 보내는 문제에 도전해야겠다.
 * 30KB정도 용량도 잘린다...
 */
public class SuperClass {
	
	public static void main(String[] args) {
		final boolean _Server = false;
		//final boolean _Server = true;
		final int portNum = 9000;
		final int fileSizeIndex = 3;
		final String fileName = "test.jpeg";
		final String ServerIP = "192.168.0.3";
		
		Server server;
		Client client;
		if(_Server)
		{
			server = new Server(portNum, fileName, fileSizeIndex);
			server.mainServer();			
		}
		else
		{
			client = new Client(portNum, fileName, fileSizeIndex, ServerIP);
			client.mainClient();
		}
	}
}
