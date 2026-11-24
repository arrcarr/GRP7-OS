package grp7.grp7_os;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class MLQ_GUI extends JFrame {

    private final Runnable onClose;
    private final JSpinner queue1Quantum = new JSpinner(new SpinnerNumberModel(2, 1, Integer.MAX_VALUE, 1));
    private final JSpinner queue2Quantum = new JSpinner(new SpinnerNumberModel(2, 1, Integer.MAX_VALUE, 1));
    private final DefaultTableModel processModel = new DefaultTableModel(
            new Object[]{"PID", "Arrival Time", "Burst Time", "Queue"}, 0) {
        private final Class<?>[] columnTypes = {
            String.class, Integer.class, Integer.class, Integer.class
        };

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnTypes[columnIndex];
        }
    };
    private final DefaultTableModel resultModel = new DefaultTableModel(
            new Object[]{"Completion Time", "Turnaround Time", "Waiting Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel summaryModel = new DefaultTableModel(
            new Object[]{"Avg. Turnaround Time", "Avg. Waiting Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable processTable = new JTable(processModel);
    private final JTable resultTable = new JTable(resultModel);
    private final JTable summaryTable = new JTable(summaryModel);
    private final JTable ganttTable = new JTable(new DefaultTableModel());

    public MLQ_GUI() {
        this(null);
    }

    public MLQ_GUI(Runnable onClose) {
        this.onClose = onClose;
        setTitle("Multilevel Queue Scheduling");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1360, 960);
        setLocationByPlatform(true);
        buildInterface();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (MLQ_GUI.this.onClose != null) {
                    MLQ_GUI.this.onClose.run();
                }
            }
        });
    }

    private void buildInterface() {
        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setContentPane(content);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(204, 204, 204));
        JLabel title = new JLabel("Multilevel Queue Scheduling", JLabel.CENTER);
        title.setFont(new Font("Dialog", Font.PLAIN, 36));
        header.add(title, BorderLayout.CENTER);
        header.setPreferredSize(new Dimension(100, 100));

        JPanel queueSettings = new JPanel(new BorderLayout(6, 4));
        queueSettings.setBorder(BorderFactory.createTitledBorder("Queue Configuration"));
        JPanel fixedQueues = new JPanel(new GridLayout(3, 1, 4, 4));
        JPanel queue1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queue1.add(new JLabel("Queue 1 — RR, Quantum:"));
        queue1.add(queue1Quantum);
        JPanel queue2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queue2.add(new JLabel("Queue 2 — RR, Quantum:"));
        queue2.add(queue2Quantum);
        JPanel queue3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queue3.add(new JLabel("Queue 3 — FCFS"));
        fixedQueues.add(queue1);
        fixedQueues.add(queue2);
        fixedQueues.add(queue3);
        queueSettings.add(fixedQueues, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(header, BorderLayout.NORTH);
        top.add(queueSettings, BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(6, 6));
        JPanel processTables = new JPanel(new GridLayout(1, 2, 6, 6));
        processTables.add(createTablePanel("Input Processes", processTable));
        processTables.add(createTablePanel("Per-Process Results", resultTable));
        center.add(processTables, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(6, 6));
        lower.setPreferredSize(new Dimension(100, 260));
        lower.add(createTablePanel("Summary Metrics", summaryTable), BorderLayout.NORTH);
        lower.add(createTablePanel("Gantt Chart", ganttTable), BorderLayout.CENTER);
        center.add(lower, BorderLayout.SOUTH);
        content.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 16));
        actions.setBackground(new Color(153, 255, 255));
        JButton addRow = new JButton("Add Row");
        addRow.addActionListener(event -> processModel.addRow(new Object[]{
            "P" + (processModel.getRowCount() + 1), 0, 1, 1
        }));
        JButton deleteRow = new JButton("Delete Row");
        deleteRow.addActionListener(event -> deleteSelectedRow());
        JButton calculate = new JButton("Calculate");
        calculate.addActionListener(event -> calculateSchedule());
        JButton clear = new JButton("Clear");
        clear.addActionListener(event -> clearTables());
        actions.add(addRow);
        actions.add(deleteRow);
        actions.add(calculate);
        actions.add(clear);
        content.add(actions, BorderLayout.SOUTH);

        configureTable(processTable, true);
        configureTable(resultTable, false);
        configureTable(summaryTable, false);
        configureTable(ganttTable, false);
    }

    private JPanel createTablePanel(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void configureTable(JTable table, boolean editable) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 18));
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        if (!editable) {
            DefaultTableCellRenderer center = new DefaultTableCellRenderer();
            center.setHorizontalAlignment(JLabel.CENTER);
            for (int column = 0; column < table.getColumnCount(); column++) {
                table.getColumnModel().getColumn(column).setCellRenderer(center);
            }
        }
    }

    private void calculateSchedule() {
        if (processTable.isEditing() && !processTable.getCellEditor().stopCellEditing()) {
            return;
        }
        if (processModel.getRowCount() == 0) {
            showInputError("Please add at least one process.");
            return;
        }

        try {
            List<MLQ_Scheduler.ProcessInput> processes = new ArrayList<>();
            for (int row = 0; row < processModel.getRowCount(); row++) {
                processes.add(new MLQ_Scheduler.ProcessInput(
                        cellText(row, 0),
                        Integer.parseInt(cellText(row, 1)),
                        Integer.parseInt(cellText(row, 2)),
                        Integer.parseInt(cellText(row, 3))));
            }

            MLQ_Scheduler.ScheduleResult result = new MLQ_Scheduler().calculate(
                    processes,
                    (Integer) queue1Quantum.getValue(),
                    (Integer) queue2Quantum.getValue());
            resultModel.setRowCount(0);
            for (MLQ_Scheduler.ProcessResult process : result.processes()) {
                resultModel.addRow(new Object[]{
                    process.completionTime(),
                    process.turnaroundTime(),
                    process.waitingTime()
                });
            }
            summaryModel.setRowCount(0);
            summaryModel.addRow(new Object[]{
                roundToTwoDecimals(result.averageTurnaroundTime()),
                roundToTwoDecimals(result.averageWaitingTime())
            });
            renderGanttChart(result.ganttBlocks());
        } catch (NumberFormatException exception) {
            showInputError("All process fields must contain valid whole numbers.");
        } catch (IllegalArgumentException | ArithmeticException exception) {
            showInputError(exception.getMessage());
        }
    }

    private String cellText(int row, int column) {
        Object value = processModel.getValueAt(row, column);
        if (value == null) {
            throw new NumberFormatException("Missing process value.");
        }
        return value.toString().trim();
    }

    private void renderGanttChart(List<MLQ_Scheduler.GanttBlock> blocks) {
        DefaultTableModel model = new DefaultTableModel();
        Object[] timeRow = new Object[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            MLQ_Scheduler.GanttBlock block = blocks.get(i);
            model.addColumn(block.label());
            timeRow[i] = block.startTime() + " - " + block.endTime();
        }
        model.addRow(timeRow);
        ganttTable.setModel(model);
        configureTable(ganttTable, false);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void deleteSelectedRow() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow < 0) {
            showInputError("Select a process row to delete.");
            return;
        }
        processModel.removeRow(processTable.convertRowIndexToModel(selectedRow));
    }

    private void clearTables() {
        processModel.setRowCount(0);
        resultModel.setRowCount(0);
        summaryModel.setRowCount(0);
        ganttTable.setModel(new DefaultTableModel());
    }

    private void showInputError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException exception) {
            java.util.logging.Logger.getLogger(MLQ_GUI.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Unable to set Nimbus look and feel.", exception);
        }
        SwingUtilities.invokeLater(() -> new MLQ_GUI().setVisible(true));
    }
}
