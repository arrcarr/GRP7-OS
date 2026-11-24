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
    //It is an array list because it can easily grow in size unlike a regular array
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
        //Initialize the total number of processes by getting the length of the 
        //PID array, which was obtained from the PNP_GUI rows. Setting n with the
        //length of arrival, burst, and priority would've also worked.
        int n = pid.length;
        
        //Initialize the needed metrics per process
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
//            referring to the array index
            int chosenIndex = -1;

            // This for loop scans all processes to find the best eligible process at 'currentTime'.
            for (int i = 0; i < n; i++) {
//                Checks if the process is NOT YET DONE, and if the arrival time is less than or equal to the current time
                if (!isDone[i]/*if isDone[i] = false, flips it to true. if the index related to the process is 
                        done, skip this entire section and i gets incremented*/  && arrival[i] <= currentTime) {
//                    Default index value is -1, so if the chosenIndex is equal to -1, it means that there is no selected process yet.
                    if (chosenIndex == -1) {
                        chosenIndex = i; // First eligible process found. i.e. The process has had either a 0 arrival time or the CPU idled enough to reach a process that had a later arrival time.
                        //It could also mean the latest index to finish
                        //if this set, repeat for loop because there is nothing after this
                        continue;
                        
                    } else {
                        // Rule 1: Lower priority number wins
                        //if both priorities are equal, jump to Rule 2
                        //If priority[i] is bigger than priority[chosenIndex], repeat the for loop which increments i, because there is nothing after this
                        if (priority[i] < priority[chosenIndex]) {
                            chosenIndex = i;
                            //after this, repeat the loop
                            continue;
                        } 
                        // Rule 2: Tie in priority: Earlier arrival time wins
                        else if (priority[i] == priority[chosenIndex]) { 
                            //if arrival[i] > arrival[chosenIndex], repeat the for loop
                            if (arrival[i] < arrival[chosenIndex]) {
                                chosenIndex = i;
                                continue;
                            } 
                            // Rule 3: Tie in arrival: Lower PID wins
                            else if (arrival[i] == arrival[chosenIndex] && pid[i] < pid[chosenIndex]) {
                                chosenIndex = i;
                                continue;
                            }
                        }
                    }
                }
            }

            // If no process has arrived yet, CPU stays IDLE for 1 time unit. The variable chosenIndex equaling to -1 means that there is no actively chosen proces yet
            if (chosenIndex == -1) {
                addGanttBlock("IDLE", currentTime, currentTime + 1);
                currentTime++;
                totalIdleTime++;
            } else {
                // If Run the chosen process completely (Non-Preemptive)
//                set start and end time for the process of the current index
                int startTime = currentTime;
                int endTime = startTime + burst[chosenIndex];

                //Pass the process data to the gantt chart maker
                addGanttBlock("P" + pid[chosenIndex], startTime, endTime);

                // Compute metrics using project formulas
                completionTime[chosenIndex] = endTime;
                turnaroundTime[chosenIndex] = completionTime[chosenIndex] - arrival[chosenIndex];
                waitingTime[chosenIndex] = turnaroundTime[chosenIndex] - burst[chosenIndex];
                responseTime[chosenIndex] = startTime - arrival[chosenIndex]; 

                //add up total metrics for the averages later.
                sumWait += waitingTime[chosenIndex];
                sumTurnaround += turnaroundTime[chosenIndex];
                sumResponse += responseTime[chosenIndex];

                //Declare a process as done, increment the count for completed processes which will eventually end the while loop, and declare the current time as the end time
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
//        Checks if the ganttBlocks array list is not empty
        if (!ganttBlocks.isEmpty()) {
//            Grabs the most recently added gantt block
            String last = ganttBlocks.get(ganttBlocks.size() - 1);
            String[] parts = last.split(",");
//           Check if the label of the last added gantt block and the label that was entered into this method is the same AND it checks if the  end time of the last added gantt block is equal to the "start" variable passed in this method. 
            if (parts[0].equals(label) && Integer.parseInt(parts[2]) == start) {
                //ganttBlocks.size() - 1 is necessary since array list uses 0 indexing. So if ganttBlock size is 4, the last index is 3.
                //Changes the arrayList string. Before going into this program,
                //If the cpu idled for 2 seconds and this is the second time it idled consecutively, it would change the the arrayList content from ["IDLE,0,1"] to ["IDLE,0,2"]
                ganttBlocks.set(ganttBlocks.size() - 1, label + "," + parts[1] + "," + end);
                return;
            }
        }
        //If the previous block is different from the current (i.e. IDLE and then P1),add new block to the array list.
        ganttBlocks.add(label + "," + start + "," + end);
    }
}