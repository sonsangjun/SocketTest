package Sender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class SocketBroadCastThread extends Thread{ 
	ValueCollections value = new ValueCollections();
	RoomData roomData = null;
	Socket socket = null;
	SocketBroadCastUsed socketBroadCastUsed;
	
	//서버인경우
	public SocketBroadCastThread(RoomData roomData,SocketBroadCastUsed socketBroadCastUsed) {
		this.roomData = roomData;
		this.socketBroadCastUsed = socketBroadCastUsed;
	}
	
	//클라이언트인 경우 (안드로이드인 경우, 텍스트를 받아 출력하고 싶다면 생성자 매개변수를 하나더 추가한다. 레퍼런스로 주면 값 변경시 바로 적용된다.)
	//아니면 socketBroadCastUsed.message를 이용하자.
	public SocketBroadCastThread(Socket socket, SocketBroadCastUsed socketBroadCastUsed) {
		this.socket = socket;
		this.socketBroadCastUsed = socketBroadCastUsed;
	}
	@Override
	public void run() {
		if(socket == null)				//서버인 경우
		{
			while(true)
			{													
				if(socketBroadCastUsed.broadCastKill)
				{
					synchronized (socketBroadCastUsed) {
						socketBroadCastUsed.broadCastKill = false;	
					}					
					break;
				}
				
				else if(socketBroadCastUsed.message.equals(socketBroadCastUsed.nullString))
				{
					try {
						Thread.sleep(value.waitTime);
						continue;
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				}
				
				
				//문자열이 계시는 경우 전 소켓에 날려준다.
				synchronized (roomData) {
					String tempMessage;
					synchronized (socketBroadCastUsed) {
						tempMessage = new String(socketBroadCastUsed.message);
					}
					for(BufferedWriter B: roomData.clientManage.broadCast)
					{					
						try {
							B.write(tempMessage);
							B.newLine();
							B.flush();			
						} catch (java.net.SocketException e) {
							System.out.println("SocketBroadCastThread.run() (메시지 보내는 도중) SocketException발생 : "+e.getMessage());
							continue;
						} catch (IOException e) {
							System.out.println("SocketBroadCastThread.run() (메시지 보내는 도중) IOException발생 : "+e.getMessage());					
							continue;
						}
					}
				}
				
				//메시지를 보냈으므로 없앤다.
				synchronized (socketBroadCastUsed) {
					socketBroadCastUsed.message = new String(socketBroadCastUsed.nullString);
				}
			}
		}
		
		else if(roomData == null)		//클라이언트인 경우
		{			
			String temp = new String(socketBroadCastUsed.nullString);
			try {
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				while(true)
				{
					
					synchronized (socketBroadCastUsed) {
						if(socketBroadCastUsed.broadCastKill)
						{
							socketBroadCastUsed.broadCastKill = false;
							break;
						}						
					}					
					
					temp = inputReader.readLine();	//temp가 null이라는건 연결이 끊겼음을 의미.(연결된 소켓에서 아무것도 들어온게 없으니)
					if(temp.equals(socketBroadCastUsed.nullString))
					{
						Thread.sleep(value.waitTime);
						continue;						
					}
					else
					{
						synchronized (socketBroadCastUsed) {
							socketBroadCastUsed.message = new String(temp);
							System.out.println(temp);
							temp = new String(socketBroadCastUsed.nullString);
						}						
					}						
				}								
			} catch (IOException | InterruptedException e) {
				System.out.println("SocketBroadCastThread.run() (클라이언트 메소드) IO or Interrupted Exception발생 : "+e.getMessage());
				e.printStackTrace();
				return ;
			} catch (java.lang.NullPointerException e) {
				System.out.println("SocketBroadCastThread.run() (클라이언트 메소드) NullPointerException발생 : "+e.getMessage());
				e.printStackTrace();
				return ;
			}
		}		
	}
}
