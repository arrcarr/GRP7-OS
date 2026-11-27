package grp7.grp7_os;

/**
 * Algorithm: Shortest Seek Time First (SSTF) - Disk Scheduling
 * Made By Tzar Lance Jester C. Fuentes
 */
public class SSTF_Scheduler {

    //the order the requests were served 
    public int[] executionOrder;

    //head movement for each step 
    public int[] seekDistance;

    //sum of all seekDistance values = total head movement
    public int totalSeekTime;

    public void calculate(int initialHead, int[] requests) {
        int n = requests.length;
        executionOrder = new int[n];
        seekDistance = new int[n];
        totalSeekTime = 0;

        // isDone[i] = true means requests[i] was already served.
        // (We use this instead of deleting from the array.)
        boolean[] isDone = new boolean[n];

        int currentHead = initialHead;

        // One loop round = one request served, so we repeat n times.
        for (int step = 0; step < n; step++) {

            // STEP 1: find the closest request that is still pending.
            int chosenIndex = -1; 

            for (int i = 0; i < n; i++) {
                if (!isDone[i]) {
                    if (chosenIndex == -1) {
                        chosenIndex = i; // first pending request becomes the candidate
                    } else if (Math.abs(requests[i] - currentHead)
                            < Math.abs(requests[chosenIndex] - currentHead)) {
                       
                        chosenIndex = i;
                    }
                }
            }

            // STEP 2: move the head to the chosen request and record the move.
            int distance = Math.abs(requests[chosenIndex] - currentHead);

            executionOrder[step] = requests[chosenIndex];
            seekDistance[step] = distance;
            totalSeekTime += distance;

            // STEP 3: mark it as served; the head is now at that cylinder.
            isDone[chosenIndex] = true;
            currentHead = requests[chosenIndex];
        }
    }
}
