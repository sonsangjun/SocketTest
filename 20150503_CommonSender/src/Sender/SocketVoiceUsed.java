package Sender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//음성 소켓이 사용중인지 체크
public class SocketVoiceUsed {
	public boolean socketVoiceThreadKill = false;
	public boolean socketVoiceUsed = false;
	public byte[] message = null;
}
