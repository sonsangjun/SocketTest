package Sender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//음성 소켓이 사용중인지 체크
public class SocketVoiceUsed {
	public boolean socketVoiceThreadKill = false;
	public boolean socketVoiceUsed = false;
	public List<FileByteArrayClass> message = Collections.synchronizedList(new ArrayList<FileByteArrayClass>()); //배열을 그대로 갖다 붙이면 소멸되어서 클래스 씌움
}
