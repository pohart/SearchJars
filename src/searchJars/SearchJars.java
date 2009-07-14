package searchJars;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

public class SearchJars extends JFrame {
    protected Set<File> m_files=new HashSet<File>();
    protected Set<File> m_tmpFiles=new HashSet<File>();
    protected JList m_fileList = new JList();
    protected JTextField m_txtClassName=new JTextField();
    private JButton m_btnSearch = new JButton("Search"); 
    private JButton m_btnClear = new JButton("Clear");
    private JPanel m_pnlMain=new JPanel();
    private Object mutex = new Object();
    Map<String,Collection<String>> m_classMap = new HashMap<String,Collection<String>>(); 
    Map<String,Collection<String>> m_fileMap = new HashMap<String,Collection<String>>();
    ListCellRenderer m_myRenderer = new DefaultListCellRenderer(){
	    {
		noFocusBorder=new EmptyBorder(1,1,1,7);
	    }

	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if(value instanceof File)
		    setText(((File)value).getName());
		else if(value instanceof ZipFile)
		    setText(((ZipFile)value).getName());
		else if(value instanceof String)
		    setText((String)value);
		return this;
	    }};

    private JFileChooser m_jlstChooser = new JFileChooser();

    private JFileChooser m_jarChooser = new JFileChooser();
    private JFileChooser m_dirChooser = new JFileChooser();
    
    protected void doExit(){
	if(JOptionPane.showConfirmDialog(this,"Are you sure you wish to exit?","Exit",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
	    System.exit(0);
	}
    }

  
    public SearchJars(){
	this.getContentPane().add(m_pnlMain);
	this.addWindowListener(new WindowAdapter(){
		public void windowClosing(WindowEvent e){
		    doExit();
		}
	    });
	GroupLayout layout=new GroupLayout(m_pnlMain);
	//    m_pnlMain.setBorder(new EmptyBorder(20,7,7,7));
	layout.setAutoCreateContainerGaps(true);
	layout.setAutoCreateGaps(true);
	m_pnlMain.setLayout(layout);
	setupMenuBar();
	this.setTitle("Search Jars");
        m_dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	m_fileList.setLayoutOrientation(JList.VERTICAL_WRAP);
	m_fileList.setCellRenderer(m_myRenderer);
	m_fileList.setVisibleRowCount(-1);
	JScrollPane listScrl=new JScrollPane(m_fileList);
    
    
	//    m_pnlMain.add(listScrl,BorderLayout.CENTER);
	layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				  .addComponent(listScrl)
				  .addGroup(layout.createSequentialGroup()
					    .addComponent(m_txtClassName)
					    .addComponent(m_btnSearch)
					    .addComponent(m_btnClear)));
	layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(listScrl)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					  .addComponent(m_txtClassName,GroupLayout.DEFAULT_SIZE,GroupLayout.DEFAULT_SIZE,Short.MAX_VALUE)
					  .addComponent(m_btnSearch,GroupLayout.DEFAULT_SIZE,GroupLayout.DEFAULT_SIZE,Short.MAX_VALUE)
					  .addComponent(m_btnClear,GroupLayout.DEFAULT_SIZE,GroupLayout.DEFAULT_SIZE,Short.MAX_VALUE)));
	//    queryPanel.setLayout(queryLayout);
	//    queryLayout.setAutoCreateGaps(true);
	//    queryLayout.setAutoCreateContainerGaps(true);
	//    queryLayout.setHorizontalGroup(queryLayout.createSequentialGroup()
	//                                   .addComponent(m_txtClassName)
	//                                   .addComponent(m_btnSearch)
	//                                   .addComponent(m_btnClear));
	//    queryLayout.setVerticalGroup(queryLayout.createSequentialGroup()
	//                                 .addGroup(queryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
	//                                           .addComponent(m_txtClassName)
	//                                           .addComponent(m_btnSearch)
	//                                           .addComponent(m_btnClear)));
	//    queryPanel.add(m_txtClassName,BorderLayout.CENTER);
	//    JPanel buttonPanel = new JPanel();
	//    buttonPanel.setLayout(queryLayout);
	//    buttonPanel.add(m_btnSearch,BorderLayout.LINE_START);
	//    buttonPanel.add(m_btnClear,BorderLayout.LINE_END);
	m_btnClear.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    if(JOptionPane.showConfirmDialog(m_txtClassName.getParent(),
						     "Are you sure you want to clear the selected jar files?", 
						     "Clear",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
			clear();
		    }
        
		}
	    });
	//    queryPanel.add(buttonPanel ,BorderLayout.LINE_END);
	m_btnSearch.addActionListener(new ActionListener(){

		public void actionPerformed(ActionEvent e) {
		    String searchString=m_txtClassName.getText();
		    if(searchString==null || searchString.length()==0){
			JOptionPane.showMessageDialog(m_txtClassName.getParent().getParent(), "please enter a class name to search for");
			return;
		    }
		    searchString=searchString.replace('/','.');
		    if(searchString.endsWith(".class")){
			searchString=searchString.substring(0,searchString.length()-".class".length());
		    }
		    searchString= searchString.toUpperCase();
		    Collection<String> containingJarFiles=new ArrayList<String>();
		    Collection<String> jarFiles = m_classMap.get(searchString);
		    if(jarFiles != null){
			containingJarFiles.addAll(jarFiles);
		    }
		    if(containingJarFiles==null || containingJarFiles.size()==0){
			JOptionPane.showMessageDialog(m_txtClassName.getParent().getParent(),
			                              "no class \"" + m_txtClassName.getText() + '\"');
			return;
		    }
		    JList jlist = new JList(containingJarFiles.toArray());
		    JScrollPane scrl=new JScrollPane(jlist);
		    jlist.setCellRenderer(m_myRenderer);
		    JOptionPane.showMessageDialog(m_txtClassName.getParent().getParent(),scrl,
						  "Classes Containing the class: " + searchString,
						  JOptionPane.INFORMATION_MESSAGE);
		}});
	//    m_pnlMain.add(queryPanel,BorderLayout.PAGE_END);
    }


    private void setupMenuBar() {
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu=new JMenu("File");
	menuBar.add(fileMenu);
	this.getRootPane().setDefaultButton(m_btnSearch);
	this.getContentPane().add(menuBar,BorderLayout.PAGE_START);
	JMenuItem openItem = new JMenuItem("Add Jars",'A');
	openItem.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    chooseJars();
		}
	    });
        fileMenu.add(openItem);
	JMenuItem addDirectoryItem = new JMenuItem("Add Directory",'D');
	addDirectoryItem.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    chooseDirectories();
		}
	    });
	fileMenu.add(addDirectoryItem);
	JMenuItem loadJarList = new JMenuItem("Load Jars From File",'L');
	loadJarList.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    loadJars();
		}
	    });
	fileMenu.add(loadJarList);
	JMenuItem saveJarList = new JMenuItem("Save jars To File",'S');
	saveJarList.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    saveJars();
		}
      
	    });
	fileMenu.add(saveJarList);
	JMenuItem exit = new JMenuItem("Exit",'E');
	exit.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    doExit();
		}
	    });
	fileMenu.addSeparator();
	fileMenu.add(exit);
    }
  
    protected void saveJars() {
	m_jlstChooser.setFileFilter(new FileFilter(){
		@Override
		    public boolean accept(File pathname) {
		    return (!pathname.isHidden() &&
			    (pathname.isDirectory() 
			     || pathname.getName().toUpperCase().endsWith(".JLST")));
		}
		@Override
		    public String getDescription() {
		    return "Jar List (JLST) files";
		}
	    });
	m_jlstChooser.setMultiSelectionEnabled(false);
	if(m_jlstChooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
	    File jarListFile;
	    jarListFile=m_jlstChooser.getSelectedFile();
	    writeJarsListToFile(jarListFile);
	}
    }
  
    private void writeJarsListToFile(File jarListFile) {
	try{
	    BufferedWriter writer = new BufferedWriter(new FileWriter(jarListFile));
	    for(Iterator<File> it=m_files.iterator(); it.hasNext();){
		File file=it.next();
		writer.write(file.getAbsolutePath());
		writer.newLine();
	    }
	    writer.close();
	}catch (IOException ex){
	    ex.printStackTrace();
	}
    }
  
    protected void loadJars() {
	m_jlstChooser.setFileFilter(new FileFilter(){
		@Override
		    public boolean accept(File pathname) {
		    return (!pathname.isHidden() &&
			    (pathname.isDirectory() 
			     || pathname.getName().toUpperCase().endsWith(".JLST")));
		}
		@Override
		    public String getDescription() {
		    return "Jar List (JLST) files";
		}
	    });
	m_jlstChooser.setMultiSelectionEnabled(false);
	if(m_jlstChooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
	    File jarListFile;
	    jarListFile=m_jlstChooser.getSelectedFile();
	    loadJarsListFromFile(jarListFile);
	}
    }

    /**
     * @param jarListFile
     */
    private void loadJarsListFromFile(File jarListFile) {
	try{
	    BufferedReader reader = new BufferedReader(new FileReader(jarListFile));
	    ArrayList<ZipFile> fileList = new ArrayList<ZipFile>(128);
	    for(String line=reader.readLine(); line != null;line=reader.readLine()){
		File jarFile;
		jarFile= new File(line);
		if(!m_files.contains(jarFile)){
		    m_files.add(jarFile);
		    fileList.add(new ZipFile(jarFile));
		}
	    }
	    addClasses(fileList);
	}catch(IOException ex){
	    ex.printStackTrace();
	}
    }


    private void addClasses(final ArrayList<ZipFile> fileList) {
	if(fileList==null || fileList.size()==0){
	    return;
	}
	final JProgressBar progressBar = new JProgressBar(0,fileList.size());
	progressBar.setValue(0);
	progressBar.setStringPainted(true);
	final JLabel progressLabel = new JLabel("importing " + fileList.size() + " files.");
	final JDialog progressDialog = new JDialog(this);
	final GroupLayout groupLayout = new GroupLayout(progressDialog.getContentPane());
	progressDialog.getContentPane().setLayout(groupLayout);
	progressDialog.setSize(200,125);
	groupLayout.setHorizontalGroup(groupLayout.createParallelGroup()
	                               .addComponent(progressBar));
	groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
	                             .addComponent(progressBar));
	groupLayout.setAutoCreateContainerGaps(true);
	progressDialog.setModalityType(ModalityType.MODELESS);
	SwingWorker<Void,Integer> task =new SwingWorker<Void,Integer>(){

	  @Override
	  protected Void doInBackground() throws Exception {
	    for(int i=0; i < fileList.size(); i++){
	      addClasses(fileList.get(i));
	      final int j=i;
	      SwingUtilities.invokeLater(new Runnable(){
	        public void run(){
	          progressBar.setValue(j);
	        }});
	    }
	    return null;
	  }

	  @Override
	  protected void done(){
	    SwingUtilities.invokeLater(new Runnable(){
	      public void run(){
      	          
	        m_fileList.setModel(new DefaultListModel(){
                
	          List<File> files = new ArrayList<File>();
	          {
	            for(Iterator<File>it=m_files.iterator();it.hasNext();){
	              files.add(it.next());
	            }
	            Collections.sort(files,new Comparator<File>(){
	              public int compare(File o1, File o2) {
	                return o1.getName().compareTo(o2.getName());
	              }});
	          }
	          public Object getElementAt(int index) {
	            return files.get(index);
	          }
      
	          public int getSize() {
	            return m_files.size();
	          }});
	        progressDialog.dispose();
	      }});

	  }
	};
	task.execute();
	progressDialog.setVisible(true);
    }

    protected void addClasses(ZipFile jarFile){
	addClasses(jarFile,new File(jarFile.getName()).getPath());
    }
    /**
     * @param jarFile
     */
    private void addClasses(ZipFile jarFile,String strFileName) {
	try{
	    for (Enumeration<? extends ZipEntry> zipEntries = jarFile.entries(); zipEntries.hasMoreElements();){
		ZipEntry zipEntry = zipEntries.nextElement();
		String strFilePath = zipEntry.getName();
		String upCaseName = strFilePath.toUpperCase();
		if (upCaseName.endsWith(".JAR") || upCaseName.endsWith(".EAR") || upCaseName.endsWith(".JAR") || upCaseName.endsWith(".ZIP")){
		    byte[] buf = new byte[(int) zipEntry.getSize()];
		    File tempJarFile=null;
		    try {
			final BufferedInputStream bis = new BufferedInputStream(jarFile.getInputStream(zipEntry));
			bis.read(buf);
			bis.close();
			//          String newFileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1,
			//                                                           fileName.length());
			String newFileName = strFilePath.substring(strFilePath.lastIndexOf('/') + 1, strFilePath.lastIndexOf('.'));
			String newFileSuffix=strFilePath.substring(strFilePath.lastIndexOf('.'),strFilePath.length());
			tempJarFile = File.createTempFile(newFileName, newFileSuffix);
			FileOutputStream writer = new FileOutputStream(tempJarFile);
			writer.write(buf);
			writer.flush();
			writer.close();
			addClasses(new JarFile(tempJarFile),strFileName +"::" +  newFileName + newFileSuffix);
		    } catch (IOException e) {
		        System.err.println(jarFile.getName() + "::" + zipEntry.getName());
			e.printStackTrace();
		    }finally{
			if(tempJarFile!= null && tempJarFile.exists()){
			    tempJarFile.delete();
			}
		    }
		}
		addClass(strFileName, strFilePath, upCaseName);
	    }
	}finally{
	    try {
		jarFile.close();
	    } catch (IOException exp) {
		exp.printStackTrace();
	    }
	}
    }

    private void addClass(String jarFile, String strFilePath, String name) {
	String upCaseName=name.toUpperCase();
	if(upCaseName.endsWith(".CLASS")){
	    int i;
	    while((i=strFilePath.lastIndexOf('.'))>-1){
		strFilePath=strFilePath.substring(0,i);
		strFilePath=strFilePath.replace('/','.');
		strFilePath=strFilePath.toUpperCase();
		
                addFileToClass(jarFile, strFilePath, m_classMap);
                addFileToClass(strFilePath,jarFile, m_fileMap);
		String strFileName = strFilePath.substring(strFilePath.lastIndexOf('.')+1);
		addFileToClass(jarFile,strFileName,m_classMap);
       
		//       String strFilePackage=strFilePath;
		//       do{
		//         strFilePackage=strFilePackage.substring(0, strFilePackage.lastIndexOf('.'));
		//         Collection<String> packageZipFiles=m_classMap.get(strFilePackage);
		//         if(packageZipFiles==null){
		//           packageZipFiles=new HashSet<String>();
		//           m_classMap.put(strFilePackage, packageZipFiles);
		//         }
		//         packageZipFiles.add(jarFile);
		//       }while(strFilePackage.contains("."));
	    }
	}
    }


    private void addFileToClass(String jarFile, String strFilePath, final Map<String, Collection<String>> map)
    {
      Collection<String> pathZipFiles=map.get(strFilePath);
      if(pathZipFiles==null){
          pathZipFiles=new HashSet<String>();
          map.put(strFilePath,pathZipFiles);
      }
      pathZipFiles.add(jarFile);
    }
    protected void chooseJars() {
	try{
	    m_jarChooser.setFileFilter(new FileFilter(){
		    @Override
			public boolean accept(File pathname) {
			return (!pathname.isHidden() 
				&& (pathname.isDirectory() 
				    || pathname.getName().toUpperCase().endsWith(".JAR")
				    || pathname.getName().toUpperCase().endsWith(".EAR") 
				    || pathname.getName().toUpperCase().endsWith(".WAR") 
				    || pathname.getName().toUpperCase().endsWith(".ZIP"))
				&& (!m_files.contains(pathname)));
		    }
		    @Override
			public String getDescription() {
			return "ZIP files";
		    }
		});
	    m_jarChooser.setMultiSelectionEnabled(true);
	    if(m_jarChooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
		File[] files=m_jarChooser.getSelectedFiles();
		
		addClassesFromJars(Arrays.asList(files));
		m_fileList.setModel(new AbstractListModel(){
			List<File> files=new ArrayList<File>();
			{
			    for(Iterator<File>it=m_files.iterator();it.hasNext();){
				files.add(it.next());
			    }
			    Collections.sort(files,new Comparator<File>(){
				    public int compare(File o1, File o2) {
					int upper=o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
					if(upper==0){
					    return o1.getName().compareTo(o2.getName());                
					}else{
					    return upper;
					}
				    }});
			}
			public Object getElementAt(int index) {
			    return files.get(index);
			}

			public int getSize() {
			    return m_files.size();
			}});
	    }
	}catch(IOException ex){
	    ex.printStackTrace();
	}
    }


    private void addClassesFromJars(List<File> files) throws IOException
    {
      ArrayList<ZipFile> fileList = new ArrayList<ZipFile>(files.size());
      for(File file: files){
          if(!m_files.contains(file)){
            m_files.add(file);
            try{
              fileList.add(new ZipFile(file));
            }catch(ZipException zipe){
              System.err.println(file.getAbsolutePath());
              zipe.printStackTrace();
            }
          }
      }

      addClasses(fileList);
    }
  
    public void chooseDirectories() {
      m_dirChooser.setMultiSelectionEnabled(true);
      m_dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if(m_dirChooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
        File[] selectedDirectories=m_dirChooser.getSelectedFiles();
        if(selectedDirectories==null || selectedDirectories.length==0){ return; }
        List<File> lstSelectedDirectories= new ArrayList(selectedDirectories.length);
        for(File directory : selectedDirectories){
          if(!directory.exists() || !directory.isDirectory()){
            return;
          }
          lstSelectedDirectories.add(directory);
        }
        try{
          addClassesFromJars(chooseDirectories(lstSelectedDirectories));
        }catch(IOException ex){
          ex.printStackTrace();
        }
      }
    }


    private List<File> chooseDirectories(List<File> selectedDirectories)
    {
      ArrayList<File> fileList=new ArrayList<File>();
      for (File selectedDirectory : selectedDirectories){
        fileList.addAll(chooseDirectories(selectedDirectory));
      }
      return fileList;
    }


    private List<File> chooseDirectories(final File selectedDirectory)
    {
        List<File> directories = Arrays.asList(selectedDirectory.listFiles(new java.io.FileFilter(){
          public boolean accept(File pathname){
            return pathname.isDirectory();
          }}));
        List<File> listOfJars=chooseDirectories(directories);
        File[] jarFiles=selectedDirectory.listFiles(new java.io.FileFilter(){
          public boolean accept(File p_fPath){
            final String strFName = p_fPath.getName();
            
            final int iExtensionIndex = strFName.lastIndexOf('.');
            if(iExtensionIndex<0){return false;}
            final String extension = strFName.substring(iExtensionIndex).toUpperCase();
            return p_fPath.isFile()
                && (extension.equals(".EAR")
                ||extension.equals(".WAR")
                ||extension.equals(".JAR")
                ||extension.equals(".ZIP"));
          }});
        listOfJars.addAll(Arrays.asList(jarFiles));
        return listOfJars;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	if (args.length > 0){
	
	}
	SwingUtilities.invokeLater(new Runnable(){

		public void run() {
		    createAndShowGui();
		}});
    }
    protected static void createAndShowGui() {
	JFrame appFrame = new SearchJars();
	appFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	appFrame.setPreferredSize(new Dimension(800,600));
	appFrame.pack();
	appFrame.setVisible(true);
    }

    protected void clear(){
        Object[] selection=m_fileList.getSelectedValues();
	m_fileList.setModel(new DefaultListModel());
	m_classMap.clear();
	m_files.clear();
    }
}
