package client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

public class ClientGui extends JFrame{
	private static final long serialVersionUID = 1L;
	
	SocialClient client = null;
	DefaultListModel<String> friendsModel;
	DefaultListModel<String> groupsModel;
	DefaultListModel<String> chatModel;
  	DefaultListModel<String> messagesModel;
	JList<String> friendsList;
	JList<String> groupsList;
	JList<String> chatList;
  	JList<String> messagesList;
  	JLabel labelChatName;
  	
	private void initialize(){
		//inizializing ChatName (title)
		labelChatName = new JLabel(getClient().getUsername());
		//inizializing FriendList
		friendsModel = new DefaultListModel<String>();
		friendsList = new JList<String>(friendsModel);
		updateFriends();
		//inizializing GroupList
		groupsModel = new DefaultListModel<String>();
		groupsList = new JList<String>(groupsModel);
		updateGroups();
		//inizializing ChatList
		chatModel = new DefaultListModel<String>();
		chatList = new JList<String>(chatModel);
      	//inizializing MessagesList
      	messagesModel = new DefaultListModel<String>();
      	messagesList = new JList<String>(messagesModel);
	}
	
	public synchronized void updateFriends(){
		//gets an updated friend list from the client and adds a tag to each friend according to its status
		friendsModel = new DefaultListModel<String>();
		for(String friend : getClient().friends.keySet()){
			Boolean status = getClient().friends.get(friend);
			if(status)
				friendsModel.addElement("[Online] " + friend);
			else
				friendsModel.addElement("[Offline] " + friend);
		}
		friendsList.setModel(friendsModel);
	}
	
	public synchronized void updateGroups(){
		//gets and updated version of the group list and adds a tag according to user's role in the group
		groupsModel = new DefaultListModel<String>();
		for(String group : getClient().groupRole.keySet())
			groupsModel.addElement(getClient().groupRole.get(group) + " " + group);
		groupsList.setModel(groupsModel);
	}
	
	public synchronized void updateChats(String name){
		//adds the tag (new) close to a new message
		if(!chatModel.contains(name) && !chatModel.contains(name + " (new)")){
			chatModel.addElement(name);
			chatList.setModel(chatModel);
		}
		return;
	}
  
  	public void showMessages(String name){
      	//check if name is a friend or a group
      	if(getClient().friendMessages.containsKey(name)){
      		ArrayList<String> friendMessages = getClient().friendMessages.get(name);
          	//adding friendMessages to messagesModel
          	messagesModel = new DefaultListModel<String>();
          	for(String msg : friendMessages)
            	messagesModel.addElement(msg); 
          	messagesList.setModel(messagesModel);
        //name is a group
      	}else if(getClient().groupMessages.containsKey(name)){
      		ArrayList<String> groupMessages = getClient().groupMessages.get(name);
  			messagesModel = new DefaultListModel<String>();
          	if(groupMessages != null){
          		for(String msg : groupMessages)
          			messagesModel.addElement(msg);
          	}
          	try{
          		messagesList.setModel(messagesModel);
          	}catch(IllegalArgumentException e){}
      	}
    }
  	
  	public void unreadMessages(String name){
  		System.out.println("[GUI] I have unreadMessages(" + name + ")");
  		//if no chat selected or already talking to name, show this chat
  		if(chatModel.isEmpty() || labelChatName.getText().compareTo(name)==0){
  			showMessages(name);
  			return;
  		}
  		//name's chat not opened yet
		int index = chatModel.indexOf(name);
		if(index != -1) //can't find name, so i already have a new message from name
			chatModel.setElementAt(name + " (new)", index);
    }
  	
  	private void groupMenu(String groupName, String role){
  		// setting up popup menu
		JPopupMenu groupMenu = new JPopupMenu();
		groupMenu.setBounds(-965, -367, 159, 62);
		addPopup(groupsList, groupMenu);
		JMenuItem menuShowMembers = new JMenuItem("Show Members");
		groupMenu.add(menuShowMembers);
		menuShowMembers.addActionListener(e -> {
			ArrayList<String> members = getClient().getMembers(groupName);
			JOptionPane.showMessageDialog(null, members.toArray(), "Members", JOptionPane.PLAIN_MESSAGE);
		});
  		//check if user is admin of the selected group (actions: show members, write message, delete group)
		if(role.contains("admin")){
			JMenuItem menuWriteMessage = new JMenuItem("Write Message");
			groupMenu.add(menuWriteMessage);
			menuWriteMessage.addActionListener(e -> {
				updateChats(groupName);
				//switch messages, showing the groupName's ones
          		labelChatName.setText(groupName);
          		showMessages(groupName);
			});
			JMenuItem menuDeleteGroup = new JMenuItem("Delete Group");
			groupMenu.add(menuDeleteGroup);
			menuDeleteGroup.addActionListener(e -> {
				getClient().deleteGroup(groupName);
			});
		//check if user is already a member of the selected group (actions: show members, write message)
		}else if(role.contains("member")){ 
			JMenuItem menuWriteMessage = new JMenuItem("Write Message");
			groupMenu.add(menuWriteMessage);
			menuWriteMessage.addActionListener(e -> {
				updateChats(groupName);
				//switch messages, showing the groupName's ones
          		labelChatName.setText(groupName);
          		showMessages(groupName);
			});
		//user is not yet a member (actions: show members, join group)
		}else{ 
			JMenuItem menuJoinGroup = new JMenuItem("Join Group");
			groupMenu.add(menuJoinGroup);
			menuJoinGroup.addActionListener(e -> {
				boolean joined = getClient().joinGroup(groupName);
				if(!joined)
					printMessage("Can't join " + groupName + "!");
			});
		}
  	}
	
	public ClientGui(){
		this.setSize(400, 200);
		this.setTitle("SocialGossip Client");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLayout(null);
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setVisible(true);
		this.addWindowListener(new WindowAdapter(){
			//if client closed without logout
            public void windowClosing(WindowEvent e){
            	if(getClient() != null){
            		getClient().logout();
            	}
            	System.exit(0);
            }
        });
		this.switchPanel(loginGui());
	}
	
	// ---------- AUX ----------
	public void setClient(SocialClient client){
		this.client = client;
	}
	
	public SocialClient getClient(){
		return this.client;
	}
	
	public void printMessage(String message){
		JOptionPane.showMessageDialog(this, message);
	}
	
	public void printNotification(String message){
		JOptionPane optionPane = new JOptionPane(new JLabel(message, JLabel.CENTER)); 
		JDialog dialog = optionPane.createDialog(this, "Social Notification");
		dialog.setModal(false);
		dialog.setVisible(true);
	}
	
	public void searchResult(String[] result){
		//shows a list with both groups and users containing the searched string in their name
		String selected = (String) JOptionPane.showInputDialog(null,
				"Please choose one:", "Search Result",
				JOptionPane.PLAIN_MESSAGE, null,
				result, result[0]);
		if(selected != null){
			String[] split = selected.split(" ");
			String name = split[1];
			//add the selected user or join the selected group
			if(selected.contains("user")){
				Boolean added = client.addFriend(name);
				if(!added)
					printMessage("Can't add " + name + "!");
			}else if(selected.contains("group")){
				Boolean joined = client.joinGroup(name);
				if(!joined)
					printMessage("Can't join " + name + "!");
			}
		}
		
	}
	
	// ---------- PANELS -----------
	private void switchPanel(JPanel panel){
		this.setContentPane(panel);
		switch(panel.getName()){
			case "LOGIN":{
				this.setSize(280, 220);
				this.revalidate();
				break;
			}
			
			case "CHAT":{
				this.setSize(600, 425);
				this.revalidate();
				break;
			}
			
			default:{
				break;
			}
		}
	}
	
	// ---------- LOGIN GUI -----------
	private JPanel loginGui(){
		JPanel panel = new JPanel();
		panel.setName("LOGIN");
		panel.setLayout(null);
		
		JLabel welcomeTag = new JLabel("SocialGossip");
		welcomeTag.setHorizontalAlignment(SwingConstants.CENTER);
		welcomeTag.setFont(new Font("Lobster Two", Font.BOLD | Font.ITALIC, 28));
		welcomeTag.setBounds(12, 10, 263, 30);
		panel.add(welcomeTag);
		
		JPanel loginPanel = new JPanel();
		loginPanel.setBounds(15, 40, 240, 145);
		panel.add(loginPanel);
		loginPanel.setLayout(null);
		
		JPanel usernamePanel = new JPanel();
		usernamePanel.setBounds(5, 17, 230, 25);
		loginPanel.add(usernamePanel);
		usernamePanel.setLayout(null);
		
		JLabel userLabel = new JLabel("Username");
		userLabel.setBounds(0, 0, 80, 25);
		usernamePanel.add(userLabel);

		JTextField userText = new JTextField(20);
		userText.setBounds(100, 0, 129, 25);
		usernamePanel.add(userText);

		JPanel passwordPanel = new JPanel();
		passwordPanel.setBounds(5, 47, 230, 25);
		loginPanel.add(passwordPanel);
		passwordPanel.setLayout(null);

		JLabel passwordLabel = new JLabel("Password");
		passwordLabel.setBounds(0, 0, 80, 25);
		passwordPanel.add(passwordLabel);

		JPasswordField passwordText = new JPasswordField(20);
		passwordText.setBounds(100, 0, 129, 25);
		passwordPanel.add(passwordText);
		
		JPanel languagePanel = new JPanel();
		languagePanel.setBounds(5, 77, 230, 25);
		loginPanel.add(languagePanel);
		languagePanel.setLayout(null);
		
		JLabel languageLabel = new JLabel("Language");
		languageLabel.setBounds(0, 0, 80, 25);
		languagePanel.add(languageLabel);
		
		JComboBox<String> languageBox = new JComboBox<String>();
		languageBox.setBounds(100, 0, 129, 24);
		languagePanel.add(languageBox);
		//setting up a list of languages
		languageBox.setModel(new DefaultComboBoxModel<String>(
				new String[] {"en", "it", "es", "de", "fr", "ru", "ro"}));
		
		JButton registerButton = new JButton("Register");
		registerButton.setFont(new Font("Dialog", Font.BOLD, 12));
		registerButton.setBounds(5, 112, 100, 25);
		loginPanel.add(registerButton);
		registerButton.addActionListener(e -> {
			String username = userText.getText();
			String password = String.valueOf(passwordText.getPassword());
			String language = String.valueOf(languageBox.getSelectedItem());
			if(username.length()==0 || username.contains(" ") || password.length()==0){
				printMessage("Insert Username and Password.");
			}else{
				SocialClient client = new SocialClient(username, password, language);
				if(client.register()){
					if(client.login(this)){
						setClient(client);
						switchPanel(chatGui());
						printNotification("Welcome, " + username + "!");
					}
				}else
					printMessage("Registration failed.");
			}
		});
		
		JButton loginButton = new JButton("Login");
		loginButton.setBounds(135, 112, 100, 25);
		loginPanel.add(loginButton);
		loginButton.addActionListener(ev -> {
			String username = userText.getText();
			String password = String.valueOf(passwordText.getPassword());
			String language = String.valueOf(languageBox.getSelectedItem());
			if(username.length()==0 | password.length()==0){
				printMessage("Insert Username and Password.");
			}else{
				SocialClient client = new SocialClient(username, password, language);
				if(client.login(this)){
					setClient(client);
					switchPanel(chatGui());
					printNotification("Welcome back, " + username + "!");
				}else
					printMessage("Login failed.");
			}
		});
		
		return panel;
	}
	
	// ----------- CHAT GUI ----------
	private JPanel chatGui(){
		initialize();
		this.setTitle("SocialGossip - " + this.getClient().getUsername());
		JPanel panel = new JPanel();
		panel.setName("CHAT");
		panel.setLayout(null);
	
		JPanel searchPanel = new JPanel();
		searchPanel.setBounds(0, 0, 200, 35);
		panel.add(searchPanel);
		searchPanel.setLayout(null);
		
		JTextField textSearch = new JTextField();
		textSearch.setBounds(5, 7, 115, 23);
		textSearch.setFont(new Font("Dialog", Font.PLAIN, 13));
		textSearch.setText("search...");
		//make default text disappear on click
		textSearch.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent evt){
				if(evt.getButton() == MouseEvent.BUTTON1){
					JTextField field = (JTextField) evt.getSource();
					if(field.getText().compareTo("search...") == 0)
						field.setText(null);
				}
			}
		});
		searchPanel.add(textSearch);
		textSearch.setColumns(10);
		
		JButton buttonSearch = new JButton("Search");
		buttonSearch.setFont(new Font("Dialog", Font.BOLD, 11));
		buttonSearch.setBounds(120, 7, 80, 22);
		searchPanel.add(buttonSearch);
		buttonSearch.addActionListener(e -> {
			String name = textSearch.getText();
			if(name != null && name.length() != 0 && !name.contains("search...") && !name.contains(" ")){
				String[] result = getClient().searchFor(name);
				if(result.length>0)
					searchResult(result);
				else
					printMessage("Can't find " + name + "!");
			}
		});
		
		JTabbedPane listsPanel = new JTabbedPane(JTabbedPane.TOP);
		listsPanel.setFont(new Font("Dialog", Font.BOLD, 11));
		listsPanel.setBounds(0, 35, 200, 365);
		panel.add(listsPanel);
		
		JPanel chatsPanel = new JPanel();
		listsPanel.addTab("Chat", null, chatsPanel, null);
		chatsPanel.setLayout(null);
		
		JScrollPane chatsScrollPane = new JScrollPane();
		chatsScrollPane.setBounds(0, 0, 195, 339);
		chatsPanel.add(chatsScrollPane);
		
		chatsScrollPane.setViewportView(chatList);
		chatList.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(255, 255, 255)));
		chatList.setFont(new Font("Loma", Font.PLAIN, 16));
		chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chatList.addMouseListener(new MouseAdapter(){
			//listen for click to open the selected chat and show its messages
			public void mouseClicked(MouseEvent evt){
				if(evt.getButton() == MouseEvent.BUTTON1){
					@SuppressWarnings("unchecked")
					JList<String> list = (JList<String>) evt.getSource();
					if(!list.isSelectionEmpty()){
						//maybe contains "name (new)", so i have to take only the name
						String[] line = list.getSelectedValue().split(" "); 
						String friendName = line[0];
						//switch tab to chats and switch messages
						listsPanel.setSelectedIndex(0);
						labelChatName.setText(friendName);
						showMessages(friendName);
						int index = list.getSelectedIndex();
						if(index >= 0)
							chatModel.setElementAt(friendName, index);
					}
				}
			}
		});
		
		JPanel groupsPanel = new JPanel();
		listsPanel.addTab("Groups", null, groupsPanel, null);
		groupsPanel.setLayout(null);
		
		JScrollPane groupsScrollPane = new JScrollPane();
		groupsScrollPane.setBounds(0, 0, 195, 309);
		groupsPanel.add(groupsScrollPane);
		
		groupsScrollPane.setViewportView(groupsList);
		groupsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupsList.setFont(new Font("Loma", Font.PLAIN, 16));
		groupsList.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(255, 255, 255)));
		groupsList.addMouseListener(new MouseAdapter(){
			//listen for right-click to open menu
			public void mouseClicked(MouseEvent evt){
				@SuppressWarnings("unchecked")
				JList<String> list = (JList<String>) evt.getSource();
				int index = list.locationToIndex(evt.getPoint());
	            list.setSelectedIndex(index);
				if(!list.isSelectionEmpty()){
					String[] line = list.getSelectedValue().split(" "); 
					String role = line[0];
					String groupName = line[1];
					groupMenu(groupName, role);
				}
			}
		});
		
		JButton buttonCreate = new JButton("Create");
		buttonCreate.setBounds(0, 312, 96, 25);
		buttonCreate.addActionListener(e -> {
			//displaying a window where the user can write the new group's name
			String groupName = (String) JOptionPane.showInputDialog("Please input a name for the group:");
			if(groupName != null && !groupName.contains(" ")){
				boolean created = getClient().createGroup(groupName);
				if(created)
					printMessage("Group created successfully!");
				else
					printMessage("Can't create the group!");
			}else{
				printMessage("You can't use that name!");
			}
		});
		groupsPanel.add(buttonCreate);
		
		JButton buttonShow = new JButton("Update");
		buttonShow.setBounds(97, 312, 96, 25);
		buttonShow.addActionListener(e -> {
			//ask the client to get all the existent groups
			boolean result = getClient().getAllGroups();
			if(result){
				//update the displayed groups
				groupsModel = new DefaultListModel<String>();
				groupsList.setModel(groupsModel);
				updateGroups();
			}
		});
		groupsPanel.add(buttonShow);
		
		JPanel friendsPanel = new JPanel();
		listsPanel.addTab("Friends", null, friendsPanel, null);
		friendsPanel.setLayout(null);
		
		JScrollPane friendsScrollPane = new JScrollPane();
		friendsScrollPane.setBounds(0, 0, 195, 339);
		friendsPanel.add(friendsScrollPane);
		
		friendsScrollPane.setViewportView(friendsList);
		friendsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		friendsList.setFont(new Font("Loma", Font.PLAIN, 16));
		friendsList.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(255, 255, 255)));
		friendsList.addMouseListener(new MouseAdapter(){
			//listen for click to open chat with selected friend
			public void mouseClicked(MouseEvent evt){
				if(evt.getButton() == MouseEvent.BUTTON1){
					@SuppressWarnings("unchecked")
					JList<String> list = (JList<String>) evt.getSource();
					String selected = list.getSelectedValue();
					String[] split = selected.split(" ");
					String friendName = split[1];
					if(friendName != null){
						Boolean friendStatus = getClient().friends.get(friendName);
						//add friend to active chats
						if(friendStatus){
							updateChats(friendName);
							//switch tab to chats and switch messages
	                  		labelChatName.setText(friendName);
	                  		listsPanel.setSelectedIndex(0);;
	                  		showMessages(friendName);
						}else
							printNotification("Your friend " + friendName + " is offline!");
					}
				}
			}
		});
		
		JPanel chatPanel = new JPanel();
		chatPanel.setBounds(200, 0, 400, 400);
		panel.add(chatPanel);
		chatPanel.setLayout(null);
		
		labelChatName.setBounds(5, 7, 390, 30);
		labelChatName.setHorizontalAlignment(SwingConstants.CENTER);
		labelChatName.setFont(new Font("Lobster Two", Font.PLAIN, 24));
		chatPanel.add(labelChatName);
		
		JTextField textMessage = new JTextField();
		textMessage.setBounds(76, 370, 249, 26);
		textMessage.setText("message...");
		//clears the textbox on click
		textMessage.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent evt){
				if(evt.getButton() == MouseEvent.BUTTON1){
					JTextField field = (JTextField) evt.getSource();
					if(field.getText().compareTo("message...") == 0)
						field.setText(null);
				}
			}
		});
		chatPanel.add(textMessage);
		textMessage.setColumns(10);
		
		JButton buttonSend = new JButton("Send");
		buttonSend.setBounds(325, 370, 70, 25);
		chatPanel.add(buttonSend);
      	buttonSend.addActionListener(ev -> {
        	String name = labelChatName.getText();
        	String text = textMessage.getText();
          	//check if different from user's name -> otherwise that means that no chat is open
          	if(getClient().getUsername().compareTo(name) != 0
              && text != null && text.compareTo(" Message...") != 0
              && name != null){
          		//check if friend or group
          		if(getClient().friendMessages.containsKey(name)){
          			boolean sent = getClient().sendMessage(name, text);
          			 if(sent){
          				//showing the new message
                       	messagesModel.addElement("[" + getClient().getUsername() + "]: " + text);
                       	textMessage.setText(null);
                     }else
                     	printNotification("Failed to send message to " + name + "!");
          		}else if(getClient().groupMessages.containsKey(name)){
          			boolean sent = getClient().sendGroupMessage(name, text);
          			if(sent)
                      	textMessage.setText(null);
                    else
                    	printNotification("Failed to send message to " + name + "!");
          		}
          		
                
            }
        });
		
		JButton buttonFile = new JButton("File");
		buttonFile.setBounds(5, 370, 70, 25);
		buttonFile.addActionListener(ev -> {
			//check if you're talking to a friend
			if(labelChatName.getText().compareTo(getClient().getUsername()) != 0){
				JFileChooser fileChooser = new JFileChooser();
				int action = fileChooser.showOpenDialog(this);
				//if user has chosen a file
				if(action == JFileChooser.APPROVE_OPTION){
					File file = fileChooser.getSelectedFile();
					String friendName = labelChatName.getText();
					String fileName = file.getAbsolutePath();
					boolean sent = getClient().requestSendFile(friendName, fileName);
					if(sent)
						System.out.println("[GUI] File request ok: " + fileName);
					else
						printMessage("Failed to transfer file.");
				}
			}else
				printMessage("You can't send a file to yourself!");
		});
		chatPanel.add(buttonFile);
		
		JScrollPane messagesScrollPane = new JScrollPane();
		messagesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		messagesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		messagesScrollPane.setBounds(5, 37, 390, 330);
		chatPanel.add(messagesScrollPane);
		
		messagesList.setLayoutOrientation(JList.VERTICAL);
		messagesList.setValueIsAdjusting(true);
		messagesList.setVisibleRowCount(0);
		messagesList.setBorder(null);
		messagesScrollPane.setViewportView(messagesList);
		
		return panel;
	}
	
	private static void addPopup(Component component, final JPopupMenu popup){
		component.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
	
}
