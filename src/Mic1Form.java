import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class Mic1Form extends JFrame
{
    private JPanel panel1;
    private JLabel MAR;
    private JLabel MARLabel;
    private JLabel MDRLabel;
    private JLabel MBRLabel;
    private JLabel PCLabel;
    private JLabel SPLabel;
    private JLabel LVLabel;
    private JLabel HLabel;
    private JLabel OPCLabel;
    private JLabel CPPLabel;
    private JTextField MARField;
    private JTextField MBRField;
    private JTextField HField;
    private JTextField OPCField;
    private JTextField TOSField;
    private JTextField CPPField;
    private JTextField PCField;
    private JTextField SPField;
    private JTextField LVField;
    private JTextField MDRField;
    private JButton cycleButton;
    private JButton importMICButton;
    private JButton importIJVMButton;
    private JLabel MIC1FileLabel;
    private JLabel IJVMFileLabel;
    private JLabel TOS;
    private JButton assembleJASButton;
    private JButton runButton;
    private JButton assembleMALButton;

    public Mic1Form()
    {
        setSize(400,500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(panel1);
        setResizable(false);

        cycleButton.setEnabled(false);
        runButton.setEnabled(false);
        cycleButton.addActionListener(e -> Main.cycle());
        runButton.addActionListener(e -> {
            while(Main.cycle() != 0xFF); //Fino a che il programma non arriva all'istruzione HALT
        });

        importMICButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            fileChooser.setFileFilter(new FileNameExtensionFilter("MIC1 File", "mic1"));

            int returnVal = fileChooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                if(!Main.setMIC1File(fileChooser.getSelectedFile()))
                    JOptionPane.showMessageDialog(null, "Il file mic1 non è valido.", "Error", JOptionPane.ERROR_MESSAGE);

                MIC1FileLabel.setText("MIC1: " + ((Main.MIC1File != null)?Main.MIC1File.getName():""));
                checkIfCanCycle();
            }
        });
        importIJVMButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            fileChooser.setFileFilter(new FileNameExtensionFilter("IJVM File", "ijvm"));

            int returnVal = fileChooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                if(!Main.setIJVMFile(fileChooser.getSelectedFile()))
                    JOptionPane.showMessageDialog(null, "Il file ijvm non è valido.", "Error",  JOptionPane.ERROR_MESSAGE);

                IJVMFileLabel.setText("IJVM: " + ((Main.IJVMFile != null)?Main.IJVMFile.getName():""));
                checkIfCanCycle();
            }
        });
        assembleJASButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            fileChooser.setFileFilter(new FileNameExtensionFilter("JAS File", "jas"));

            int returnVal = fileChooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION)
            {

                String fileIn = fileChooser.getSelectedFile().getPath();
                String fileOut = fileIn.replaceFirst("[.][^.\\/\\\\]+$", ".ijvm");
                IJVMAssembler assembler = new IJVMAssembler(fileIn, fileOut);
                try
                {
                    assembler.parse();
                    assembler.assemble();

                    if(!Main.setIJVMFile(new File(fileOut)))
                        JOptionPane.showMessageDialog(null, "Il file jas non è valido.", "Error",  JOptionPane.ERROR_MESSAGE);

                    IJVMFileLabel.setText("IJVM: " + ((Main.IJVMFile != null)?Main.IJVMFile.getName():""));
                    checkIfCanCycle();
                }
                catch (IJVMException f)
                {
                    JOptionPane.showMessageDialog(null, +f.line+": "+f.error, "Error",  JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        assembleMALButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            fileChooser.setFileFilter(new FileNameExtensionFilter("MAL File", "mal"));

            int returnVal = fileChooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION)
            {

                String fileIn = fileChooser.getSelectedFile().getPath();
                String fileOut = fileIn.replaceFirst("[.][^.\\/\\\\]+$", ".mic1");
                MIC1Assembler assembler = new MIC1Assembler(fileIn, fileOut);
                try
                {
                    assembler.parse();
                    assembler.assemble();

                    if(!Main.setMIC1File(new File(fileOut)))
                        JOptionPane.showMessageDialog(null, "Il file mal non è valido.", "Error",  JOptionPane.ERROR_MESSAGE);

                    MIC1FileLabel.setText("MIC1: " + ((Main.MIC1File != null)?Main.MIC1File.getName():""));
                    checkIfCanCycle();
                }
                catch (MIC1Exception f)
                {
                    JOptionPane.showMessageDialog(null, f.line+": "+f.error, "Error",  JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public void checkIfCanCycle()
    {
        Main.resetRegisters();
        runButton.setEnabled(Main.IJVMFile != null && Main.MIC1File != null);
        cycleButton.setEnabled(Main.IJVMFile != null && Main.MIC1File != null);
    }

    public void setRegister(String register, String value)
    {
        JTextField choice;
        switch(register)
        {
            case "MAR": choice = MARField;
                break;
            case "MDR": choice = MDRField;
                break;
            case "PC": choice = PCField;
                break;
            case "MBR": choice = MBRField;
                break;
            case "SP": choice = SPField;
                break;
            case "LV": choice = LVField;
                break;
            case "CPP": choice = CPPField;
                break;
            case "TOS": choice = TOSField;
                break;
            case "OPC": choice = OPCField;
                break;
            case "H": choice = HField;
                break;
            default:
                return;
        }

        choice.setText(value);
    }
}
