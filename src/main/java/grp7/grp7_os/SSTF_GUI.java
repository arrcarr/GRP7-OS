package grp7.grp7_os;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * GUI for SSTF Disk Scheduling.
 */
public class SSTF_GUI extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SSTF_GUI.class.getName());

    DefaultTableModel stepsModel; // the step-by-step table

    public SSTF_GUI() {
        initComponents();
        stepsModel = (DefaultTableModel) stepsTable.getModel();
    }

    // Builds all the components and puts them on the window.
    private void initComponents() {

        // ---------- Header (same look as PNP_GUI) ----------
        Header = new JPanel();
        Header.setBackground(new Color(204, 204, 204));
        JLabel title = new JLabel("Shortest Seek Time First (SSTF)", JLabel.CENTER);
        title.setFont(new java.awt.Font("Dialog", 0, 36));
        Header.setLayout(new BorderLayout());
        Header.add(title, BorderLayout.CENTER);
        Header.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // ---------- Input row: head, max cylinder, requests, buttons ----------
        headField = new JTextField("53", 5);
        maxField = new JTextField("200", 5);
        requestsField = new JTextField( "98, 183, 37, 122, 14, 124, 65, 67", 28);
        
        // Above are some Example Values

        calculate = new JButton("Calculate");
        calculate.addActionListener(this::calculateActionPerformed);
        clear = new JButton("Clear");
        clear.addActionListener(this::clearActionPerformed);

        jPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 12));
        jPanel1.setBackground(new Color(153, 255, 255));
        jPanel1.add(new JLabel("Initial Head:"));
        jPanel1.add(headField);
        jPanel1.add(new JLabel("Max Cylinder:"));
        jPanel1.add(maxField);
        jPanel1.add(new JLabel("Requests (comma separated):"));
        jPanel1.add(requestsField);
        jPanel1.add(calculate);
        jPanel1.add(clear);

        // Header on top of the input row
        JPanel north = new JPanel(new BorderLayout());
        north.add(Header, BorderLayout.NORTH);
        north.add(jPanel1, BorderLayout.CENTER);

        // ---------- Graph (centre) ----------
        graph = new SSTF_Graph();

        // ---------- Step-by-step table (right of the graph) ----------
        stepsTable = new JTable();
        stepsTable.setModel(new DefaultTableModel(
            new Object[][]{},
            new String[]{"Step", "From", "To", "Seek Distance"}
        ) {
            // The user can't edit result cells
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });
        jScrollPane1 = new JScrollPane(stepsTable);
        jScrollPane1.setPreferredSize(new Dimension(340, 300));

        // ---------- Summary (bottom): the four required metrics ----------
        initialHeadOut = makeOutputField();
        queueOut = makeOutputField();
        sequenceOut = makeOutputField();
        totalOut = makeOutputField();

        JPanel summary = new JPanel(new GridLayout(4, 2, 5, 5));
        summary.setBorder(BorderFactory.createTitledBorder("Results"));
        summary.add(new JLabel("Initial Head Position:"));
        summary.add(initialHeadOut);
        summary.add(new JLabel("Request Queue:"));
        summary.add(queueOut);
        summary.add(new JLabel("Execution Sequence:"));
        summary.add(sequenceOut);
        summary.add(new JLabel("Total Seek Time (Head Movement):"));
        summary.add(totalOut);

        // ---------- Put everything on the window ----------
        getContentPane().setLayout(new BorderLayout(5, 5));
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(graph, BorderLayout.CENTER);
        getContentPane().add(jScrollPane1, BorderLayout.EAST);
        getContentPane().add(summary, BorderLayout.SOUTH);

        // DISPOSE_ON_CLOSE: closing this window does NOT close the whole
        // program, so the Main Menu stays open. (EXIT_ON_CLOSE would quit everything.)
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null); // centre on screen
    }

    // Small helper: a read-only text box, so long text can still be scrolled.
    private JTextField makeOutputField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        return field;
    }

    // ------------------------------------------------------------------
    // Calculate button
    // ------------------------------------------------------------------
    private void calculateActionPerformed(java.awt.event.ActionEvent evt) {

        int head;
        int maxCylinder;
        int[] requests;

        // STEP 1: read the text boxes and turn the text into numbers.
        try {
            head = Integer.parseInt(headField.getText().trim());
            maxCylinder = Integer.parseInt(maxField.getText().trim());

            // split("[,\\s]+") cuts the text at commas and/or spaces
            String[] parts = requestsField.getText().trim().split("[,\\s]+");
            requests = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                requests[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            // anything that is not a whole number (including empty boxes)
            JOptionPane.showMessageDialog(this, "All fields must contain valid whole numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // STEP 2: check that the numbers make sense for the disk.
        if (maxCylinder <= 0) {
            JOptionPane.showMessageDialog(this, "Max Cylinder must be greater than 0.", "Invalid Range", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (head < 0 || head > maxCylinder) {
            JOptionPane.showMessageDialog(this, "Initial Head must be between 0 and " + maxCylinder + ".", "Invalid Range", JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (int i = 0; i < requests.length; i++) {
            if (requests[i] < 0 || requests[i] > maxCylinder) {
                JOptionPane.showMessageDialog(this, "Request " + requests[i] + " is outside the disk range (0 to " + maxCylinder + ").", "Invalid Range", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // STEP 3: run the algorithm (all the logic lives in SSTF_Scheduler).
        SSTF_Scheduler sched = new SSTF_Scheduler();
        sched.calculate(head, requests);

        // STEP 4: show the step-by-step table.
        stepsModel.setRowCount(0);
        int from = head;
        for (int i = 0; i < sched.executionOrder.length; i++) {
            stepsModel.addRow(new Object[]{
                i + 1,
                from,
                sched.executionOrder[i],
                sched.seekDistance[i]
            });
            from = sched.executionOrder[i]; // next step starts where this one ended
        }

        // STEP 5: show the four required metrics.
        initialHeadOut.setText(String.valueOf(head));
        queueOut.setText(arrayToText(requests, ", "));
        sequenceOut.setText(head + " -> " + arrayToText(sched.executionOrder, " -> "));
        totalOut.setText(String.valueOf(sched.totalSeekTime));

        // STEP 6: draw the graph.
        graph.setData(head, sched.executionOrder, maxCylinder);
    }

    // ------------------------------------------------------------------
    // Clear button
    // ------------------------------------------------------------------
    private void clearActionPerformed(java.awt.event.ActionEvent evt) {
        headField.setText("");
        maxField.setText("");
        requestsField.setText("");
        stepsModel.setRowCount(0);
        initialHeadOut.setText("");
        queueOut.setText("");
        sequenceOut.setText("");
        totalOut.setText("");
        graph.clear();
    }

    // Turns {98, 183, 37} into "98, 183, 37" (separator is chosen by the caller).
    private String arrayToText(int[] values, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

   
    //Main method to run the GUI
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new SSTF_GUI().setVisible(true));
    }

    // Variables declaration
    private JPanel Header;
    private JPanel jPanel1;
    private JButton calculate;
    private JButton clear;
    private JTextField headField;
    private JTextField maxField;
    private JTextField requestsField;
    private JTextField initialHeadOut;
    private JTextField queueOut;
    private JTextField sequenceOut;
    private JTextField totalOut;
    private JTable stepsTable;
    private JScrollPane jScrollPane1;
    private SSTF_Graph graph;
    // End of variables declaration
}
