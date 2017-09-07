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
    protected Set<File> m_files = new HashSet<>();
    protected Set<File> m_tmpFiles = new HashSet<>();
    protected JList<File> m_fileList = new JList<>();
    protected JTextField m_txtClassName = new JTextField();
    private final JButton m_btnSearch = new JButton("Search");
    private final JButton m_btnClear = new JButton("Clear");
    private final JPanel m_pnlMain = new JPanel();
    // private Object mutex = new Object();
    Map<String, Collection<String>> m_classMap = new HashMap<>();
    Map<String, Collection<String>> m_fileMap = new HashMap<>();
    ListCellRenderer<Object> m_myRenderer = new DefaultListCellRenderer() {
        {
            noFocusBorder = new EmptyBorder(1, 1, 1, 7);
        }

        @Override
        public Component getListCellRendererComponent(final JList<?> list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);
            if (value instanceof File) {
                setText(((File) value).getName());
            } else if (value instanceof ZipFile) {
                setText(((ZipFile) value).getName());
            } else if (value instanceof String) {
                setText((String) value);
            }
            return this;
        }
    };

    private final JFileChooser m_jlstChooser = new JFileChooser();

    private final JFileChooser m_jarChooser = new JFileChooser();
    private final JFileChooser m_dirChooser = new JFileChooser();

    protected void doExit() {
        if (JOptionPane.showConfirmDialog(this,
                "Are you sure you wish to exit?", "Exit",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    public SearchJars() {
        this.getContentPane().add(m_pnlMain);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                doExit();
            }
        });
        final GroupLayout layout = new GroupLayout(m_pnlMain);
        // m_pnlMain.setBorder(new EmptyBorder(20,7,7,7));
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        m_pnlMain.setLayout(layout);
        setupMenuBar();
        this.setTitle("Search Jars");
        m_dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        m_fileList.setLayoutOrientation(JList.VERTICAL_WRAP);
        m_fileList.setCellRenderer(m_myRenderer);
        m_fileList.setVisibleRowCount(-1);
        final JScrollPane listScrl = new JScrollPane(m_fileList);

        // m_pnlMain.add(listScrl,BorderLayout.CENTER);
        layout.setHorizontalGroup(layout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(listScrl)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(m_txtClassName).addComponent(m_btnSearch)
                        .addComponent(m_btnClear)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addComponent(listScrl)
                .addGroup(layout
                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(m_txtClassName, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(m_btnSearch, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(m_btnClear, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        // queryPanel.setLayout(queryLayout);
        // queryLayout.setAutoCreateGaps(true);
        // queryLayout.setAutoCreateContainerGaps(true);
        // queryLayout.setHorizontalGroup(queryLayout.createSequentialGroup()
        // .addComponent(m_txtClassName)
        // .addComponent(m_btnSearch)
        // .addComponent(m_btnClear));
        // queryLayout.setVerticalGroup(queryLayout.createSequentialGroup()
        // .addGroup(queryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        // .addComponent(m_txtClassName)
        // .addComponent(m_btnSearch)
        // .addComponent(m_btnClear)));
        // queryPanel.add(m_txtClassName,BorderLayout.CENTER);
        // JPanel buttonPanel = new JPanel();
        // buttonPanel.setLayout(queryLayout);
        // buttonPanel.add(m_btnSearch,BorderLayout.LINE_START);
        // buttonPanel.add(m_btnClear,BorderLayout.LINE_END);
        m_btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (JOptionPane.showConfirmDialog(m_txtClassName.getParent(),
                        "Are you sure you want to clear the selected jar files?",
                        "Clear",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    clear();
                }

            }
        });
        // queryPanel.add(buttonPanel ,BorderLayout.LINE_END);
        m_btnSearch.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                String searchString = m_txtClassName.getText();
                if (searchString == null || searchString.length() == 0) {
                    JOptionPane.showMessageDialog(
                            m_txtClassName.getParent().getParent(),
                            "please enter a class name to search for");
                    return;
                }
                searchString = searchString.replace('/', '.');
                if (searchString.endsWith(".class")) {
                    searchString = searchString.substring(0,
                            searchString.length() - ".class".length());
                }
                searchString = searchString.toUpperCase();
                final Collection<String> containingJarFiles = new ArrayList<>();
                final Collection<String> jarFiles = m_classMap
                        .get(searchString);
                if (jarFiles != null) {
                    containingJarFiles.addAll(jarFiles);
                }
                if (containingJarFiles == null
                        || containingJarFiles.size() == 0) {
                    JOptionPane.showMessageDialog(
                            m_txtClassName.getParent().getParent(),
                            "no class \"" + m_txtClassName.getText() + '\"');
                    return;
                }
                final JList<?> jlist = new JList<>(
                        containingJarFiles.toArray());
                final JScrollPane scrl = new JScrollPane(jlist);
                jlist.setCellRenderer(m_myRenderer);
                JOptionPane.showMessageDialog(
                        m_txtClassName.getParent().getParent(), scrl,
                        "Classes Containing the class: " + searchString,
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        // m_pnlMain.add(queryPanel,BorderLayout.PAGE_END);
    }

    private void setupMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        this.getRootPane().setDefaultButton(m_btnSearch);
        this.getContentPane().add(menuBar, BorderLayout.PAGE_START);
        final JMenuItem openItem = new JMenuItem("Add Jars", 'A');
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                chooseJars();
            }
        });
        fileMenu.add(openItem);
        final JMenuItem addDirectoryItem = new JMenuItem("Add Directory", 'D');
        addDirectoryItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                chooseDirectories();
            }
        });
        fileMenu.add(addDirectoryItem);
        final JMenuItem loadJarList = new JMenuItem("Load Jars From File", 'L');
        loadJarList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadJars();
            }
        });
        fileMenu.add(loadJarList);
        final JMenuItem saveJarList = new JMenuItem("Save jars To File", 'S');
        saveJarList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                saveJars();
            }

        });
        fileMenu.add(saveJarList);
        final JMenuItem exit = new JMenuItem("Exit", 'E');
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                doExit();
            }
        });
        fileMenu.addSeparator();
        fileMenu.add(exit);
    }

    protected void saveJars() {
        m_jlstChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return (!pathname.isHidden() && (pathname.isDirectory()
                        || pathname.getName().toUpperCase().endsWith(".JLST")));
            }

            @Override
            public String getDescription() {
                return "Jar List (JLST) files";
            }
        });
        m_jlstChooser.setMultiSelectionEnabled(false);
        if (m_jlstChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File jarListFile;
            jarListFile = m_jlstChooser.getSelectedFile();
            writeJarsListToFile(jarListFile);
        }
    }

    private void writeJarsListToFile(final File jarListFile) {
        try {
            final BufferedWriter writer = new BufferedWriter(
                    new FileWriter(jarListFile));
            for (final Iterator<File> it = m_files.iterator(); it.hasNext();) {
                final File file = it.next();
                writer.write(file.getAbsolutePath());
                writer.newLine();
            }
            writer.close();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void loadJars() {
        m_jlstChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return (!pathname.isHidden() && (pathname.isDirectory()
                        || pathname.getName().toUpperCase().endsWith(".JLST")));
            }

            @Override
            public String getDescription() {
                return "Jar List (JLST) files";
            }
        });
        m_jlstChooser.setMultiSelectionEnabled(false);
        if (m_jlstChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File jarListFile;
            jarListFile = m_jlstChooser.getSelectedFile();
            loadJarsListFromFile(jarListFile);
        }
    }

    /**
     * @param jarListFile
     */
    private void loadJarsListFromFile(final File jarListFile) {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(jarListFile))) {

            final ArrayList<ZipFile> fileList = new ArrayList<>(128);
            for (String line = reader.readLine(); line != null; line = reader
                    .readLine()) {
                File jarFile;
                jarFile = new File(line);
                if (!m_files.contains(jarFile)) {
                    m_files.add(jarFile);
                    fileList.add(new ZipFile(jarFile));
                }
            }
            addClasses(fileList);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private void addClasses(final ArrayList<ZipFile> fileList) {
        if (fileList == null || fileList.size() == 0) {
            return;
        }
        final JProgressBar progressBar = new JProgressBar(0, fileList.size());
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        final JDialog progressDialog = new JDialog(this);
        final GroupLayout groupLayout = new GroupLayout(
                progressDialog.getContentPane());
        progressDialog.getContentPane().setLayout(groupLayout);
        progressDialog.setSize(200, 125);
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup().addComponent(progressBar));
        groupLayout.setVerticalGroup(
                groupLayout.createSequentialGroup().addComponent(progressBar));
        groupLayout.setAutoCreateContainerGaps(true);
        progressDialog.setModalityType(ModalityType.MODELESS);
        final SwingWorker<Void, Integer> task = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i < fileList.size(); i++) {
                    addClasses(fileList.get(i));
                    final int j = i;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setValue(j);
                        }
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {

                        m_fileList.setModel(new DefaultListModel<File>() {

                            List<File> files = new ArrayList<>();
                            {
                                for (final Iterator<File> it = m_files
                                        .iterator(); it.hasNext();) {
                                    files.add(it.next());
                                }
                                Collections.sort(files, new Comparator<File>() {
                                    @Override
                                    public int compare(final File o1,
                                            final File o2) {
                                        return o1.getName()
                                                .compareTo(o2.getName());
                                    }
                                });
                            }

                            @Override
                            public File getElementAt(final int index) {
                                return files.get(index);
                            }

                            @Override
                            public int getSize() {
                                return m_files.size();
                            }
                        });
                        progressDialog.dispose();
                    }
                });

            }
        };
        task.execute();
        progressDialog.setVisible(true);
    }

    protected void addClasses(final ZipFile jarFile) {
        addClasses(jarFile, new File(jarFile.getName()).getPath());
    }

    /**
     * @param jarFile
     */
    private void addClasses(final ZipFile jarFile, final String strFileName) {
        try {
            for (final Enumeration<? extends ZipEntry> zipEntries = jarFile
                    .entries(); zipEntries.hasMoreElements();) {
                final ZipEntry zipEntry = zipEntries.nextElement();
                final String strFilePath = zipEntry.getName();
                final String upCaseName = strFilePath.toUpperCase();
                if (upCaseName.endsWith(".JAR") || upCaseName.endsWith(".EAR")
                        || upCaseName.endsWith(".JAR")
                        || upCaseName.endsWith(".ZIP")) {
                    final byte[] buf = new byte[(int) zipEntry.getSize()];
                    File tempJarFile = null;
                    try {
                        final BufferedInputStream bis = new BufferedInputStream(
                                jarFile.getInputStream(zipEntry));
                        bis.read(buf);
                        bis.close();
                        // String newFileName =
                        // fileName.substring(fileName.lastIndexOf(File.separator)
                        // + 1,
                        // fileName.length());
                        final String newFileName = strFilePath.substring(
                                strFilePath.lastIndexOf('/') + 1,
                                strFilePath.lastIndexOf('.'));
                        final String newFileSuffix = strFilePath.substring(
                                strFilePath.lastIndexOf('.'),
                                strFilePath.length());
                        tempJarFile = File.createTempFile(newFileName,
                                newFileSuffix);
                        final FileOutputStream writer = new FileOutputStream(
                                tempJarFile);
                        writer.write(buf);
                        writer.flush();
                        writer.close();
                        addClasses(new JarFile(tempJarFile), strFileName + "::"
                                + newFileName + newFileSuffix);
                    } catch (final IOException e) {
                        System.err.println(
                                jarFile.getName() + "::" + zipEntry.getName());
                        e.printStackTrace();
                    } finally {
                        if (tempJarFile != null && tempJarFile.exists()) {
                            tempJarFile.delete();
                        }
                    }
                }
                addClass(strFileName, strFilePath, upCaseName);
            }
        } finally {
            try {
                jarFile.close();
            } catch (final IOException exp) {
                exp.printStackTrace();
            }
        }
    }

    private void addClass(final String jarFile, String strFilePath,
            final String name) {
        final String upCaseName = name.toUpperCase();
        if (upCaseName.endsWith(".CLASS")) {
            int i;
            while ((i = strFilePath.lastIndexOf('.')) > -1) {
                strFilePath = strFilePath.substring(0, i);
                strFilePath = strFilePath.replace('/', '.');
                strFilePath = strFilePath.toUpperCase();

                addFileToClass(jarFile, strFilePath, m_classMap);
                addFileToClass(strFilePath, jarFile, m_fileMap);
                final String strFileName = strFilePath
                        .substring(strFilePath.lastIndexOf('.') + 1);
                addFileToClass(jarFile, strFileName, m_classMap);

                // String strFilePackage=strFilePath;
                // do{
                // strFilePackage=strFilePackage.substring(0,
                // strFilePackage.lastIndexOf('.'));
                // Collection<String>
                // packageZipFiles=m_classMap.get(strFilePackage);
                // if(packageZipFiles==null){
                // packageZipFiles=new HashSet<String>();
                // m_classMap.put(strFilePackage, packageZipFiles);
                // }
                // packageZipFiles.add(jarFile);
                // }while(strFilePackage.contains("."));
            }
        }
    }

    private void addFileToClass(final String jarFile, final String strFilePath,
            final Map<String, Collection<String>> map) {
        Collection<String> pathZipFiles = map.get(strFilePath);
        if (pathZipFiles == null) {
            pathZipFiles = new HashSet<>();
            map.put(strFilePath, pathZipFiles);
        }
        pathZipFiles.add(jarFile);
    }

    protected void chooseJars() {
        try {
            m_jarChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(final File pathname) {
                    return (!pathname.isHidden() && (pathname.isDirectory()
                            || pathname.getName().toUpperCase().endsWith(".JAR")
                            || pathname.getName().toUpperCase().endsWith(".EAR")
                            || pathname.getName().toUpperCase().endsWith(".WAR")
                            || pathname.getName().toUpperCase()
                                    .endsWith(".ZIP"))
                            && (!m_files.contains(pathname)));
                }

                @Override
                public String getDescription() {
                    return "ZIP files";
                }
            });
            m_jarChooser.setMultiSelectionEnabled(true);
            if (m_jarChooser
                    .showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                final File[] files = m_jarChooser.getSelectedFiles();

                addClassesFromJars(Arrays.asList(files));
                m_fileList.setModel(new AbstractListModel<File>() {
                    List<File> files = new ArrayList<>();
                    {
                        for (final Iterator<File> it = m_files.iterator(); it
                                .hasNext();) {
                            files.add(it.next());
                        }
                        Collections.sort(files, new Comparator<File>() {
                            @Override
                            public int compare(final File o1, final File o2) {
                                final int upper = o1.getName().toUpperCase()
                                        .compareTo(o2.getName().toUpperCase());
                                if (upper == 0) {
                                    return o1.getName().compareTo(o2.getName());
                                } else {
                                    return upper;
                                }
                            }
                        });
                    }

                    @Override
                    public File getElementAt(final int index) {
                        return files.get(index);
                    }

                    @Override
                    public int getSize() {
                        return m_files.size();
                    }
                });
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private void addClassesFromJars(final List<File> files) throws IOException {
        final ArrayList<ZipFile> fileList = new ArrayList<>(files.size());
        for (final File file : files) {
            if (!m_files.contains(file)) {
                m_files.add(file);
                try {
                    fileList.add(new ZipFile(file));
                } catch (final ZipException zipe) {
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
        if (m_dirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File[] selectedDirectories = m_dirChooser.getSelectedFiles();
            if (selectedDirectories == null
                    || selectedDirectories.length == 0) {
                return;
            }
            final List<File> lstSelectedDirectories = new ArrayList<>(
                    selectedDirectories.length);
            for (final File directory : selectedDirectories) {
                if (!directory.exists() || !directory.isDirectory()) {
                    return;
                }
                lstSelectedDirectories.add(directory);
            }
            try {
                addClassesFromJars(chooseDirectories(lstSelectedDirectories));
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private List<File> chooseDirectories(final List<File> selectedDirectories) {
        final ArrayList<File> fileList = new ArrayList<>();
        for (final File selectedDirectory : selectedDirectories) {
            fileList.addAll(chooseDirectories(selectedDirectory));
        }
        return fileList;
    }

    private List<File> chooseDirectories(final File selectedDirectory) {
        final List<File> directories = Arrays
                .asList(selectedDirectory.listFiles(new java.io.FileFilter() {
                    @Override
                    public boolean accept(final File pathname) {
                        return pathname.isDirectory();
                    }
                }));
        final List<File> listOfJars = chooseDirectories(directories);
        final File[] jarFiles = selectedDirectory
                .listFiles(new java.io.FileFilter() {
                    @Override
                    public boolean accept(final File p_fPath) {
                        final String strFName = p_fPath.getName();

                        final int iExtensionIndex = strFName.lastIndexOf('.');
                        if (iExtensionIndex < 0) {
                            return false;
                        }
                        final String extension = strFName
                                .substring(iExtensionIndex).toUpperCase();
                        return p_fPath.isFile() && (extension.equals(".EAR")
                                || extension.equals(".WAR")
                                || extension.equals(".JAR")
                                || extension.equals(".ZIP"));
                    }
                });
        listOfJars.addAll(Arrays.asList(jarFiles));
        return listOfJars;
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        if (args.length > 0) {

        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                createAndShowGui();
            }
        });
    }

    protected static void createAndShowGui() {
        final JFrame appFrame = new SearchJars();
        appFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        appFrame.setPreferredSize(new Dimension(800, 600));
        appFrame.pack();
        appFrame.setVisible(true);
    }

    protected void clear() {
        m_fileList.getSelectedValues();
        m_fileList.setModel(new DefaultListModel<>());
        m_classMap.clear();
        m_files.clear();
    }
}
