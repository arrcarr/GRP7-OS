package grp7.grp7_os;


import java.util.ArrayList;

/**
 * Author: Rafael Francis P. Carrillo
 * Algorithm: Priority Scheduling (Non-Preemptive)
 * Conventions: Lower priority number = higher priority. No aging.
 * Ties broken by: 1. Higher Priority (smaller number) -> 2. Earlier Arrival -> 3. Lower PID.
 */
public class PNP_Scheduler {

    // Stores Gantt chart blocks as strings: "PID,StartTime,EndTime" (e.g., "P1,0,5" or "IDLE,5,7")
    public ArrayList<String> ganttBlocks = new ArrayList<>();

    // Per-process output arrays (parallel to input arrays)
    public int[] completionTime;
    public int[] turnaroundTime;
    public int[] waitingTime;
    public int[] responseTime;

    // Summary metrics
    public double avgWaitTime;
    public double avgTurnaroundTime;
    public double avgResponseTime;
    public double cpuUtilization;
    public double throughput;

    public void calculate(int[] pid, int[] arrival, int[] burst, int[] priority) {
        int n = pid.length;
        completionTime = new int[n];
        turnaroundTime = new int[n];
        waitingTime = new int[n];
        responseTime = new int[n];
        ganttBlocks.clear();

        boolean[] isDone = new boolean[n];
        int completedCount = 0;
        int currentTime = 0;
        int totalIdleTime = 0;

        double sumWait = 0, sumTurnaround = 0, sumResponse = 0;

        // Loop until all processes are finished
        while (completedCount < n) {
            int chosenIndex = -1;

            // Scan all processes to find the best eligible one at 'currentTime'
            for (int i = 0; i < n; i++) {
                if (!isDone[i] && arrival[i] <= currentTime) {
                    if (chosenIndex == -1) {
                        chosenIndex = i; // First eligible process found
                    } else {
                        // Rule 1: Lower priority number wins
                        if (priority[i] < priority[chosenIndex]) {
                            chosenIndex = i;
                        } 
                        // Rule 2: Tie in priority: Earlier arrival time wins
                        else if (priority[i] == priority[chosenIndex]) {
                            if (arrival[i] < arrival[chosenIndex]) {
                                chosenIndex = i;
                            } 
                            // Rule 3: Tie in arrival: Lower PID wins
                            else if (arrival[i] == arrival[chosenIndex] && pid[i] < pid[chosenIndex]) {
                                chosenIndex = i;
                            }
                        }
                    }
                }
            }

            // If no process has arrived yet, CPU stays IDLE for 1 time unit
            if (chosenIndex == -1) {
                addGanttBlock("IDLE", currentTime, currentTime + 1);
                currentTime++;
                totalIdleTime++;
            } else {
                // Run the chosen process completely (Non-Preemptive)
                int startTime = currentTime;
                int endTime = startTime + burst[chosenIndex];

                addGanttBlock("P" + pid[chosenIndex], startTime, endTime);

                // Compute metrics using project formulas
                completionTime[chosenIndex] = endTime;
                turnaroundTime[chosenIndex] = completionTime[chosenIndex] - arrival[chosenIndex];
                waitingTime[chosenIndex] = turnaroundTime[chosenIndex] - burst[chosenIndex];
                responseTime[chosenIndex] = startTime - arrival[chosenIndex]; 

                sumWait += waitingTime[chosenIndex];
                sumTurnaround += turnaroundTime[chosenIndex];
                sumResponse += responseTime[chosenIndex];

                isDone[chosenIndex] = true;
                completedCount++;
                currentTime = endTime;
            }
        }

        // Compute final averages and system metrics
        avgWaitTime = sumWait / n;
        avgTurnaroundTime = sumTurnaround / n;
        avgResponseTime = sumResponse / n;

        int totalTime = currentTime;
        int busyTime = totalTime - totalIdleTime;
        cpuUtilization = ((double) busyTime / totalTime) * 100.0;
        throughput = (double) n / totalTime;
    }

    // Helper to merge consecutive IDLE ticks into one clean block (e.g., IDLE from 0 to 3)
    private void addGanttBlock(String label, int start, int end) {
        if (!ganttBlocks.isEmpty()) {
            String last = ganttBlocks.get(ganttBlocks.size() - 1);
            String[] parts = last.split(",");
            if (parts[0].equals(label) && Integer.parseInt(parts[2]) == start) {
                ganttBlocks.set(ganttBlocks.size() - 1, label + "," + parts[1] + "," + end);
                return;
            }
        }
        ganttBlocks.add(label + "," + start + "," + end);
    }
}