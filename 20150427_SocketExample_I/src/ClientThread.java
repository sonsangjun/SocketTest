import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;


public class ClientThread extends Thread{
	String serverIP;
	int portNum;
	int interval;
	
	Socket socket;
	BufferedInputStream streamInput;
	FileOutputStream fileOutput;
	
	public ClientThread(String serverIP, int portNum,int interval) {
		this.serverIP = serverIP;
		this.portNum = portNum;
		this.interval = interval;
	}
	
	public void run()
	{
		System.out.println("클라이언트 시작합니다.");
		
		try {
			System.out.println( (interval)+"초후 서버와 연결을 시도합니다.");
			Thread.sleep(interval);
			
			socket = new Socket(serverIP,portNum);
			
		} catch (UnknownHostException e) {
			System.out.println("그런 호스트 없답니다."+e.getMessage());
			return ;
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
			return ;
		} catch (InterruptedException e) {
			System.out.println("인터럽트 예외"+e.getMessage());
			return ;
		}
		
		//연결 성공
		try {
			streamInput = new BufferedInputStream(socket.getInputStream());
			fileOutput = new FileOutputStream("receive.jpg");
			
			//파일의 크기가 509KB일때는 서버측에서 broken pipe 에러가 떳지만,
			//파일크기가 1KB미만일때는 이상없이 파일전송이 완료되었다.
			//파일크기가 7KB까지 한번에 이상없이 보냄을 확인하였다.
			//파일크기가 25KB인 경우 전송은 받으나 클라이언트 단에서 잘려버리네... 해결방법을 찾아봐야겠다.
			//파일전송에 있어 파일사이즈에 따른 분할전송이 필요한데, 어느 사이즈가 적정선인지 확인해야겠당...
			byte[] filestream=null ;
			while(true)
			{
				if(streamInput.available() > 0)	//이부분이 문제일거 같다. 파일사이즈를 보내줄수 있다면 미리 받아서 버퍼 사이즈를 결정하는 것도 한방법일듯..
				{
					filestream = new byte[streamInput.available()];
					streamInput.read(filestream);
					break;
				}
			}
			
			
			Thread.sleep(interval);
			System.out.println("버퍼에 있는 파일 크기 "+streamInput.available());
			System.out.println("filestream배열크기"+filestream.length);
			fileOutput.write(filestream);

			System.out.println(interval+"초 후에 버퍼 닫습니다.");
			
			Thread.sleep(interval);
			
			streamInput.close();
			
			System.out.println("파일아웃풋 닫습니다.");
			fileOutput.close();
			
			System.out.println("클라이언트 소켓 닫습니다.");
			socket.close();			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	
}
