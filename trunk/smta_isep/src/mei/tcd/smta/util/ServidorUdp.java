package mei.tcd.smta.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class ServidorUdp  extends  Thread {
	String ipRemoto="";
	int portaRemota=0;
	InetAddress local;
	DatagramSocket s;
	public ServidorUdp()
	{
		
		
	}
	public ServidorUdp(String ipremoto, int portaremota)
	{
		ipRemoto=ipremoto;
		portaRemota=portaremota;
		
		try {
			s = new DatagramSocket();
			local = InetAddress.getByName(ipRemoto);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void CloseUdp()
	{
		s.close();
	}
	
    
	
	
	
//	String messageStr="Hello Android!";
//	int server_port = 12345;
//	DatagramSocket s = new DatagramSocket();
//	InetAddress local = InetAddress.getByName("192.168.1.102");
//	int msg_length=messageStr.length();
//	byte[] message = messageStr.getBytes();
//	DatagramPacket p = new DatagramPacket(message, msg_length,local,server_port);
//	s.send(p);
	
	public void send(String... mensagens) {
		try {
			
					//log.d("StringUDP",mensagens);
					//for (String mensagem : mensagens) {
					//	Log.d("StringUDP",mensagem);
			StringBuilder buffer = new StringBuilder();
			for(int  i =0; i < mensagens.length; i++)
			{
			    for (String str: mensagens)
			    {  
			    	buffer.append(str.toString());
			    	buffer.append("\r\n");
			    }

			}
			String buffersend = buffer.toString();
						int msg_length=buffersend.toString().length();
						//ByteArrayOutputStream bStream = new ByteArrayOutputStream();
						byte[] message = buffersend.toString().getBytes();
						DatagramPacket p = new DatagramPacket(message, msg_length,local,portaRemota);
						s.send(p);
					//}
				
			//}
			//s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
