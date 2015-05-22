package Sender;

//카메라 소켓이 사용중인지 체크
public class SocketCameraUsed {
	public boolean socketCameraUsed = false;
	public boolean socketCameraThreadKill = false;
	public byte[] message = null;			//카메라 프리뷰 사진 전송을 위해 선언
}
