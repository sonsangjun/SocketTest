import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Scanner;


public class ServerControl extends Thread{
	final String bye = "bye";
	ServerSocket serversocket;
	
	public ServerControl(ServerSocket serversocket) {
		this.serversocket = serversocket;
	}
	
	public void run()
	{ 
		Scanner input = new Scanner(System.in);
		
		while(true)
		{
			System.out.println("연결 대기 중인 서버를 중지시키려면 bye를 입력하세요.");
			String response = input.nextLine();	
			if(response.equals(bye))
			{
				System.out.println("서버를 중지합니다.");
				try {
					serversocket.close();	//서버소켓을 레퍼런스로 Server관련 이 스레드 포함 두개에 뿌렸다. 
					input.close();			//어차피 서버종료엔 동기화가 필요없으므로 바로 닫아버린다.
					return ;
				} catch (IOException e) {
					System.out.println("서버 중지를 실패했습니다. 사유 : "+ e.getMessage());
				}
			}	
		}
	}
}
