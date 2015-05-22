package Sender;

//음성 소켓이 사용중인지 체크
public class SocketVoiceUsed {
	public boolean socketVoiceThreadKill = false;
	public boolean socketVoiceUsed = false;
	public byte[] message = null;			//음성 전송을 위해 선언
}
