package client;

import javax.swing.SwingUtilities;

public class ClientMain implements Runnable{

	public static void main(String[] args){
		SwingUtilities.invokeLater(new ClientMain());
	}
	
	@Override
	public void run(){
		ClientGui cgui = new ClientGui();
		cgui.setVisible(true);
	}
	
}
