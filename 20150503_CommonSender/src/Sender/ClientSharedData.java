package Sender;

//write는 상관없는데, input의 경우 사용중을 구별해야할듯.
//일단 만들고 쓸모가 없으면 버린다.
public class ClientSharedData {
	boolean inputUsed = false;		//input을 누군가 사용중이면 true
	boolean ClientSession = true;	//클라이언트가 살아있으면 true
}
